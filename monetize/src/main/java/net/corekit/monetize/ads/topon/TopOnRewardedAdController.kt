package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import com.thinkup.rewardvideo.api.TURewardVideoAd
import com.thinkup.rewardvideo.api.TURewardVideoListener
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdErrorCode
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * TopOn 激励广告控制器
 */
class TopOnRewardedAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("topon_rw_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_rw_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("topon_rw_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("topon_rw_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("topon_rw_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_rw_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("topon_rw_click_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("topon_rw_close_count", 0)
    private var currentPosition: String = ""

    companion object {
        private const val TAG = "TopOnRewarded"

        @Volatile
        private var instance: TopOnRewardedAdController? = null

        fun getInstance(): TopOnRewardedAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnRewardedAdController().also { instance = it }
            }
        }
    }

    private var rewardedAd: TURewardVideoAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnRewardedId()) {
            AdLogger.d("[$TAG] 激励广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.REWARDED_AD_ID_NOT_CONFIGURED.toAdException()
            )
        }

        if (!TopOnManager.isReady()) {
            val initResult = TopOnManager.initialize(context)
            if (initResult is AdResult.Failure) return initResult
        }

        if (hasValidCache()) {
            AdLogger.d("[$TAG] 已有有效缓存，跳过加载")
            return AdResult.Success(Unit)
        }

        if (!isLoading.compareAndSet(false, true)) {
            AdLogger.d("[$TAG] 正在加载中，跳过重复请求")
            return AdResult.Success(Unit)
        }

        return try {
            loadAd(context)
        } finally {
            isLoading.set(false)
        }
    }

    private suspend fun loadAd(context: Context): AdResult<Unit> {
        totalLoadCount++
        val adUnitId = BuildConfig.TOPON_REWARDED_ID
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载激励广告, ID: %s", adUnitId)

            val ad = TURewardVideoAd(context, adUnitId)
            rewardedAd = ad

            ad.setAdListener(object : TURewardVideoListener {
                override fun onRewardedVideoAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    
                    // 尝试使用 checkValidAdCaches 获取 eCPM
                    cachedEcpm = try {
                        ad.checkValidAdCaches()?.firstOrNull()?.publisherRevenue?.toDouble() ?: 0.0
                    } catch (e: Exception) { 0.0 }
                    
                    AdLogger.d("[$TAG] ✅ 激励广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)
                    
                    totalLoadSucCount++
                    reportAdData(
                        "ad_loaded",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to "TopOn",
                            "pass_time" to ceil(loadTime / 1000.0).toInt()
                        )
                    )
                    FpuController.onAdFill("RW")
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onRewardedVideoAdFailed(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ 激励广告加载失败, 耗时: %d ms, error: %s",
                        loadTime,
                        error?.fullErrorInfo
                    )
                    totalLoadFailCount++
                    reportAdData(
                        "ad_load_fail",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadFailCount,
                            "ad_source" to "TopOn",
                            "pass_time" to ceil(loadTime / 1000.0).toInt(),
                            "reason" to (error?.desc ?: "code=${error?.code}")
                        )
                    )
                    if (continuation.isActive) continuation.resume(
                        AdResult.Failure(
                            AdException(
                                error?.code?.toIntOrNull() ?: AdException.ERROR_INTERNAL,
                                error?.desc ?: "加载失败"
                            )
                        )
                    )
                }

                override fun onRewardedVideoAdPlayStart(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告开始播放")
                    cachedEcpm = parseEcpm(info?.ecpmLevel)
                    totalShowCount++
                    val ecpmMicros = (cachedEcpm * 1_000_000).toLong()
                    reportAdData(
                        "ad_impression",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to currentPosition,
                            "number" to totalShowCount,
                            "ad_source" to "TopOn",
                            "value" to cachedEcpm,
                            "currency" to "USD"
                        )
                    )
                    RevenueAdManager.reportAdRevenue(
                        RevenueAdData(
                            revenue = RevenueInfo(
                                value = cachedEcpm,
                                currencyCode = "USD"
                            ),
                            adRevenueNetwork = "TopOn",
                            adRevenueUnit = adUnitId,
                            adRevenuePlacement = currentPosition,
                            adFormat = "Rewarded"
                        )
                    )
                    IpuController.onAdImpression("RW", ecpmMicros)
                    RpuController.onAdRevenue("RW", ecpmMicros)
                }

                override fun onRewardedVideoAdPlayEnd(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告播放结束")
                }

                override fun onRewardedVideoAdPlayFailed(error: AdError?, info: TUAdInfo?) {
                    AdLogger.e("[$TAG] 激励广告播放失败: %s", error?.fullErrorInfo)
                    totalShowFailCount++
                    reportAdData(
                        "ad_show_fail",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to currentPosition,
                            "number" to totalShowFailCount,
                            "reason" to (error?.fullErrorInfo ?: "play_failed")
                        )
                    )
                }

                override fun onRewardedVideoAdClosed(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告已关闭")
                    totalCloseCount++
                    reportAdData(
                        "ad_close",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to currentPosition,
                            "number" to totalCloseCount,
                            "ad_source" to "TopOn",
                            "value" to cachedEcpm,
                            "currency" to "USD"
                        )
                    )
                }

                override fun onRewardedVideoAdPlayClicked(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告被点击")
                    totalClickCount++
                    reportAdData(
                        "ad_click",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to currentPosition,
                            "number" to totalClickCount,
                            "ad_source" to "TopOn",
                            "value" to cachedEcpm,
                            "currency" to "USD"
                        )
                    )
                }

                override fun onReward(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 用户获得奖励")
                }
            })

            ad.load()
        }
    }

    suspend fun showAd(
        activity: Activity,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        position: String = ""
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = rewardedAd
        val adUnitId = BuildConfig.TOPON_REWARDED_ID
        currentPosition = position

        totalShowTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to adUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        if (ad == null || !ad.isAdReady) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            totalShowFailCount++
            reportAdData(
                "ad_show_fail",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "没有可用的缓存广告"
                )
            )
            onRewardEarned?.invoke(false)
            if (continuation.isActive) continuation.resume(
                AdResult.Failure(
                    AdException(
                        AdException.ERROR_NOT_LOADED,
                        "没有可用的缓存广告"
                    )
                )
            )
            return@suspendCancellableCoroutine
        }

        var hasEarnedReward = false
        AdLogger.d("[$TAG] 准备展示激励广告")

        ad.setAdListener(object : TURewardVideoListener {
            override fun onRewardedVideoAdLoaded() {}
            override fun onRewardedVideoAdFailed(error: AdError?) {}
            override fun onRewardedVideoAdPlayStart(info: TUAdInfo?) {}
            override fun onRewardedVideoAdPlayEnd(info: TUAdInfo?) {}
            override fun onRewardedVideoAdPlayFailed(error: AdError?, info: TUAdInfo?) {}
            override fun onRewardedVideoAdPlayClicked(info: TUAdInfo?) {}

            override fun onReward(info: TUAdInfo?) {
                hasEarnedReward = true
                AdLogger.d("[$TAG] 用户获得奖励")
            }

            override fun onRewardedVideoAdClosed(info: TUAdInfo?) {
                AdLogger.d("[$TAG] 激励广告已关闭, 是否获得奖励: %s", hasEarnedReward)
                clearCache()
                onRewardEarned?.invoke(hasEarnedReward)
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })

        ad.show(activity)
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedEcpm else 0.0

    fun hasValidCache(): Boolean {
        val ad = rewardedAd ?: return false
        if (!ad.isAdReady) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        rewardedAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    private fun parseEcpm(ecpmLevel: Any?): Double {
        return when (ecpmLevel) {
            is Number -> ecpmLevel.toDouble()
            is String -> ecpmLevel.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "Rewarded")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }
}
