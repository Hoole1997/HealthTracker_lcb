package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logi
import com.healthtracker.framework.ext.logw
import com.thinkup.rewardvideo.api.TURewardVideoAd
import com.thinkup.rewardvideo.api.TURewardVideoListener
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import com.thinkup.core.api.TUAdRevenueListener
import kotlinx.coroutines.CancellableContinuation
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
import net.corekit.monetize.ads.bidding.BiddingAdType
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.bidding.BiddingWinner
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
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
    private var currentAdSource: String = "TopOn"

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
    private val adUnitId: String get() = BuildConfig.TOPON_REWARDED_ID

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
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.TOPON, BiddingAdType.REWARDED)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.TOPON, BiddingAdType.REWARDED)
            AdLogger.w("[$TAG] 加载跳过 | 平台: TopOn | 类型: Rewarded | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "TopOn"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        totalLoadCount++
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载激励广告, ID: %s", adUnitId)

            val ad = TURewardVideoAd(context, adUnitId)
            rewardedAd = ad

            ad.setAdListener(LoadAdListener(startTime, continuation))

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

        AdLogger.d("[$TAG] 准备展示激励广告")

        val listener = ShowAdListener(continuation, onRewardEarned, onDismiss)
        ad.setAdListener(listener)
        ad.setAdRevenueListener(listener)

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


    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "Rewarded")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }

    /**
     * 加载阶段监听器 - 处理加载成功/失败埋点，展示相关回调空实现
     */
    private inner class LoadAdListener(
        private val startTime: Long,
        private val continuation: CancellableContinuation<AdResult<Unit>>
    ) : TURewardVideoListener {
        
        override fun onRewardedVideoAdLoaded() {
            loadTimestamp = System.currentTimeMillis()
            val loadTime = loadTimestamp - startTime
            val ad = rewardedAd ?: return
            
            cachedEcpm = try {
                ad.checkValidAdCaches()?.firstOrNull()?.publisherRevenue?.toDouble() ?: 0.0
            } catch (e: Exception) { 0.0 }
            
            AdLogger.d("[$TAG] ✅ 激励广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)
            totalLoadSucCount++
            
            val networkName = ad.checkValidAdCaches()?.firstOrNull()?.networkName
            val loadedSource = if (networkName.isNullOrEmpty()) "TopOn" else networkName

            reportAdData(
                "ad_loaded",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "number" to totalLoadSucCount,
                    "ad_source" to loadedSource,
                    "pass_time" to ceil(loadTime / 1000.0).toInt()
                )
            )
            FpuController.onAdFill("RW")
            if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
        }

        override fun onRewardedVideoAdFailed(error: AdError?) {
            val loadTime = System.currentTimeMillis() - startTime
            AdLogger.e("[$TAG] ❌ 激励广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)
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

        // 展示相关回调 - 空实现，由 ShowAdListener 处理
        override fun onRewardedVideoAdPlayStart(info: TUAdInfo?) {}
        override fun onRewardedVideoAdPlayEnd(info: TUAdInfo?) {}
        override fun onRewardedVideoAdPlayFailed(error: AdError?, info: TUAdInfo?) {}
        override fun onRewardedVideoAdPlayClicked(info: TUAdInfo?) {}
        override fun onRewardedVideoAdClosed(info: TUAdInfo?) {}
        override fun onReward(info: TUAdInfo?) {}
    }

    /**
     * 展示阶段监听器 - 处理展示/点击/关闭/奖励埋点，加载相关回调空实现
     */
    private inner class ShowAdListener(
        private val continuation: CancellableContinuation<AdResult<Unit>>,
        private val onRewardEarned: ((Boolean) -> Unit)?,
        private val onDismiss: (() -> Unit)?
    ) : TURewardVideoListener, TUAdRevenueListener {
        
        private var hasEarnedReward = false

        // 加载相关回调 - 空实现，由 LoadAdListener 处理
        override fun onRewardedVideoAdLoaded() {}
        override fun onRewardedVideoAdFailed(error: AdError?) {}

        override fun onRewardedVideoAdPlayStart(info: TUAdInfo?) {
            AdLogger.d("[$TAG] 激励广告开始播放")
            cachedEcpm = info?.publisherRevenue ?: info?.ecpm ?: 0.0
            currentAdSource = info?.networkName ?: "TopOn"
            
            totalShowCount++
            PlatformFrequencyManager.recordShow(BiddingWinner.TOPON, BiddingAdType.REWARDED)
            AdConfigManager.getRewardedConfig().recordShow()
            reportAdData(
                "ad_impression",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalShowCount,
                    "ad_source" to currentAdSource,
                    "currency" to "USD"
                )
            )

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

        override fun onRewardedVideoAdPlayClicked(info: TUAdInfo?) {
            AdLogger.d("[$TAG] 激励广告被点击")
            totalClickCount++
            AdConfigManager.getRewardedConfig().recordClick()
            PlatformFrequencyManager.recordClick(BiddingPlatform.TOPON, BiddingAdType.REWARDED)
            reportAdData(
                "ad_click",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalClickCount,
                    "ad_source" to currentAdSource,
                    "currency" to "USD"
                )
            )
        }

        override fun onReward(info: TUAdInfo?) {
            hasEarnedReward = true
            AdLogger.d("[$TAG] 用户获得奖励")
        }

        override fun onRewardedVideoAdClosed(info: TUAdInfo?) {
            AdLogger.d("[$TAG] 激励广告已关闭, 是否获得奖励: %s", hasEarnedReward)
            totalCloseCount++
            reportAdData(
                "ad_close",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalCloseCount,
                    "ad_source" to currentAdSource,
                    "currency" to "USD"
                )
            )
            clearCache()
            onRewardEarned?.invoke(hasEarnedReward)
            onDismiss?.invoke()
            if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
        }

        override fun onAdRevenuePaid(adInfo: TUAdInfo?) {
            val revenueValue = adInfo?.publisherRevenue ?: 0.0
            val ecpmUsd =  revenueValue.toLong()
            "Topon 激励上报 ecpm = $ecpmUsd".logd("###############")
            RevenueAdManager.reportAdRevenue(
                RevenueAdData(
                    revenue = RevenueInfo(
                        value = revenueValue,
                        currencyCode = "USD"
                    ),
                    adRevenueNetwork = currentAdSource,
                    adRevenueUnit = adUnitId,
                    adRevenuePlacement = currentPosition,
                    adFormat = "Rewarded"
                )
            )
            //Topon返回的是美元，转成long类型后永远都是 0
            IpuController.onAdImpression("RW", ecpmUsd)
            RpuController.onAdRevenue("RW", ecpmUsd)
        }
    }
}
