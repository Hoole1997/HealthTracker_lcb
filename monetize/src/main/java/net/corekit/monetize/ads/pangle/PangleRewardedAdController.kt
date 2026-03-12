package net.corekit.monetize.ads.pangle

import android.app.Activity
import android.content.Context
import com.bytedance.sdk.openadsdk.api.model.PAGAdEcpmInfo
import com.bytedance.sdk.openadsdk.api.model.PAGRevenueInfo
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem
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
 * Pangle 激励广告控制器
 */
class PangleRewardedAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("pangle_rv_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_rv_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("pangle_rv_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("pangle_rv_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("pangle_rv_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("pangle_rv_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("pangle_rv_click_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("pangle_rv_close_count", 0)
    private var totalRewardEarnedCount by DataStoreIntDelegate("pangle_rewarded_ad_total_reward_earned", 0)

    companion object {
        private const val TAG = "PangleRewarded"

        @Volatile
        private var instance: PangleRewardedAdController? = null

        fun getInstance(): PangleRewardedAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleRewardedAdController().also { instance = it }
            }
        }
    }

    private var cachedAd: PAGRewardedAd? = null
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasPangleRewardedId()) {
            AdLogger.d("[$TAG] 激励广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.REWARDED_AD_ID_NOT_CONFIGURED.toAdException()
            )
        }

        if (!PangleManager.isReady()) {
            val initResult = PangleManager.initialize(context)
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
        val adUnitId = BuildConfig.PANGLE_REWARDED_ID
        
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.PANGLE, BiddingAdType.REWARDED)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.PANGLE, BiddingAdType.REWARDED)
            AdLogger.w("[$TAG] 加载跳过 | 平台: Pangle | 类型: Rewarded | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Pangle"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        totalLoadCount++
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载激励广告, ID: %s", adUnitId)

            PAGRewardedAd.loadAd(
                adUnitId,
                PAGRewardedRequest(),
                object : PAGRewardedAdLoadListener {
                    override fun onAdLoaded(ad: PAGRewardedAd) {
                        val loadTime = System.currentTimeMillis() - startTime
                        cachedAd = ad
                        loadTimestamp = System.currentTimeMillis()

                        AdLogger.d(
                            "[$TAG] ✅ 激励广告加载成功, 耗时: %d ms",
                            loadTime
                        )
                        totalLoadSucCount++
                        reportAdData(
                            "ad_loaded",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadSucCount,
                                "ad_source" to "Pangle",
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            )
                        )
                        FpuController.onAdFill("RV")
                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }

                    override fun onError(code: Int, message: String?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e(
                            "[$TAG] ❌ 激励广告加载失败, 耗时: %d ms, code: %d, message: %s",
                            loadTime,
                            code,
                            message
                        )
                        totalLoadFailCount++
                        reportAdData(
                            "ad_load_fail",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadFailCount,
                                "ad_source" to "Pangle",
                                "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                "reason" to (message ?: "code=$code")
                            )
                        )
                        if (continuation.isActive) continuation.resume(
                            AdResult.Failure(
                                AdException(
                                    code,
                                    message ?: "加载失败"
                                )
                            )
                        )
                    }
                })
        }
    }

    suspend fun showAd(
        activity: Activity,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
        position: String = ""
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = cachedAd
        val adUnitId = BuildConfig.PANGLE_REWARDED_ID

        val pagRevenueInfo: PAGRevenueInfo? = ad?.pagRevenueInfo
        val ecpmInfo: PAGAdEcpmInfo? = pagRevenueInfo?.showEcpm
        val currentCurrency = ecpmInfo?.currency ?: "USD"
        val ecpmMicros = ecpmInfo?.revenue?.toDoubleOrNull() ?: 0.0
        val adnName = ad?.pagRevenueInfo?.winEcpm?.adnName
        var currentAdSource = if (adnName.isNullOrEmpty()) "Pangle" else adnName

        totalShowTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to adUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.PANGLE, BiddingAdType.REWARDED)) {
            totalShowFailCount++
            reportAdData(
                "ad_show_error",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )
            onRewardEarned?.invoke(false)
            if (continuation.isActive) continuation.resume(
                AdResult.Failure(AdErrorCode.AD_SHOW_FAILED.toAdException("platform_frequency_limit"))
            )
            return@suspendCancellableCoroutine
        }

        if (ad == null || !hasValidCache()) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            totalShowFailCount++
            reportAdData(
                "ad_show_error",
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

        ad.setAdInteractionListener(object : PAGRewardedAdInteractionListener {
            override fun onAdShowed() {
                AdLogger.d("[$TAG] 激励广告已展示")

                // Pangle 的 revenue 本身就是美元，直接使用

                totalShowCount++
                PlatformFrequencyManager.recordShow(BiddingWinner.PANGLE, BiddingAdType.REWARDED)
                AdConfigManager.getRewardedConfig().recordShow()
                reportAdData(
                    eventName = "ad_impression",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to adUnitId,
                        "number" to totalShowCount,
                        "ad_source" to currentAdSource,
                        "value" to ecpmMicros,
                        "currency" to currentCurrency
                    )
                )
                RevenueAdManager.reportAdRevenue(
                    RevenueAdData(
                        revenue = RevenueInfo(
                            value = ecpmMicros,
                            currencyCode = currentCurrency
                        ),
                        adRevenueNetwork = currentAdSource,
                        adRevenueUnit = adUnitId,
                        adRevenuePlacement = position,
                        adFormat = "Rewarded"
                    )
                )
                val revenueMicros = (ecpmMicros * 1_000_000).toLong()
                IpuController.onAdImpression("RV", revenueMicros)
                RpuController.onAdRevenue("RV", revenueMicros)
            }

            override fun onAdClicked() {
                AdLogger.d("[$TAG] 激励广告被点击")
                totalClickCount++
                AdConfigManager.getRewardedConfig().recordClick()
                PlatformFrequencyManager.recordClick(BiddingPlatform.PANGLE, BiddingAdType.REWARDED)
                reportAdData(
                    eventName = "ad_click",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to position,
                        "number" to totalClickCount,
                        "ad_source" to currentAdSource,
                        "value" to ecpmMicros,
                        "currency" to currentCurrency
                    )
                )
            }

            override fun onAdDismissed() {
                AdLogger.d("[$TAG] 激励广告已关闭, 是否获得奖励: %s", hasEarnedReward)
                totalCloseCount++
                reportAdData(
                    "ad_dismiss",
                    mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to position,
                        "number" to totalCloseCount,
                        "ad_source" to currentAdSource,
                        "value" to ecpmMicros,
                        "currency" to currentCurrency
                    )
                )
                clearCache()
                onRewardEarned?.invoke(hasEarnedReward)
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }

            override fun onUserEarnedReward(rewardItem: PAGRewardItem) {
                hasEarnedReward = true
                totalRewardEarnedCount++
                reportAdData(
                    eventName = "ad_reward_earned",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to position,
                        "number" to totalRewardEarnedCount,
                        "reward_name" to rewardItem.rewardName,
                        "reward_amount" to rewardItem.rewardAmount,
                        "ad_source" to currentAdSource
                    )
                )
                AdLogger.d(
                    "[$TAG] 用户获得奖励: %s x %d",
                    rewardItem.rewardName,
                    rewardItem.rewardAmount
                )
            }

            override fun onUserEarnedRewardFail(code: Int, message: String?) {
                AdLogger.w("[$TAG] 奖励发放失败: %d - %s", code, message)
            }
        })

        ad.show(activity)
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedAd?.pagRevenueInfo?.showEcpm?.revenue?.toDoubleOrNull() ?: 0.0 else 0.0

    fun hasValidCache(): Boolean {
        if (cachedAd == null) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        cachedAd = null
        loadTimestamp = 0
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "Pangle", "ad_format" to "Rewarded")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }
}
