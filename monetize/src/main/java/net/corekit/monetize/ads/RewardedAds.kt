package net.corekit.monetize.ads


import android.app.Activity
import android.content.Context
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.remax.bill.ads.report.IpuController
import com.remax.bill.ads.report.RpuController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ui.dialog.ADLoadingDialog
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * 激励广告控制器（支持缓存池）
 */
class RewardedAds private constructor() {

    companion object {
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2

        @Volatile
        private var INSTANCE: RewardedAds? = null

        fun getInstance(): RewardedAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardedAds().also { INSTANCE = it }
            }
        }
    }

    private data class CachedRewardedAd(
        val ad: RewardedAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > 1 * 60 * 60 * 1000L
        }
    }

    private val cacheLock = Any()
    private val adCachePool = mutableListOf<CachedRewardedAd>()
    private val maxCacheSizePerAdUnit = DEFAULT_CACHE_SIZE_PER_AD_UNIT

    private var totalTriggerCount by DataStoreIntDelegate("reward_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("reward_show_count", 0)
    private var totalFailCount by DataStoreIntDelegate("reward_fail_count", 0)
    private var totalRewardCount by DataStoreIntDelegate("reward_reward_count", 0)
    private var totalClickCount by DataStoreIntDelegate("reward_click_count", 0)

    private var currentAdValue: AdValue? = null

    /**
     * 预加载激励广告
     */
    suspend fun load(context: Context, adUnitId: String? = null): AdResult<Unit> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID

        if (isCacheFull(finalAdUnitId)) {
            AdLogger.d(
                "激励广告缓存已满，广告位ID: %s，当前缓存: %d/%d",
                finalAdUnitId,
                getCachedAdCount(finalAdUnitId),
                maxCacheSizePerAdUnit
            )
            return AdResult.Success(Unit)
        }

        return try {
            val rewardedAd = withContext(Dispatchers.Main) {
                loadInternal(context.applicationContext, finalAdUnitId)
            }
            if (rewardedAd != null) {
                synchronized(cacheLock) {
                    adCachePool.add(CachedRewardedAd(rewardedAd, finalAdUnitId))
                    val currentCount = getCachedAdCount(finalAdUnitId)
                    AdLogger.d(
                        "激励广告加载成功并缓存，广告位ID: %s，缓存数量: %d/%d",
                        finalAdUnitId,
                        currentCount,
                        maxCacheSizePerAdUnit
                    )
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("激励广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("激励广告加载异常", e)
            AdResult.Failure(createAdException("加载异常: ${e.message}", e))
        }
    }

    /**
     * 展示激励广告
     */
    suspend fun show(
        activity: Activity,
        adUnitId: String? = null
    ): AdResult<RewardOutcome> = withContext(Dispatchers.Main) {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID

        totalTriggerCount++
        reportAdData(
            "ad_reward_trigger",
            mapOf(
                "ad_unit_name" to finalAdUnitId,
                "number" to totalTriggerCount
            )
        )

        try {
            var cachedAd = getCachedAd(finalAdUnitId)
            var loadingShown = false

            if (cachedAd == null) {
                loadingShown = true
                ADLoadingDialog.show(activity)
                when (val loadResult = load(activity, finalAdUnitId)) {
                    is AdResult.Failure -> {
                        ADLoadingDialog.hide()
                        totalFailCount++
                        return@withContext AdResult.Failure(loadResult.error)
                    }

                    else -> {
                        cachedAd = getCachedAd(finalAdUnitId)
                    }
                }
                ADLoadingDialog.hide()
            }

            val adHolder = cachedAd
            if (adHolder == null) {
                if (!loadingShown) {
                    ADLoadingDialog.hide()
                }
                totalFailCount++
                val error = createAdException("激励广告缓存为空")
                return@withContext AdResult.Failure(error)
            }

            suspendCancellableCoroutine<AdResult<RewardOutcome>> { continuation ->
                var rewardItem: RewardItem? = null

                adHolder.ad.adEventCallback = object : RewardedAdEventCallback {
                    override fun onAdPaid(value: AdValue) {
                        super.onAdPaid(value)
                        currentAdValue = value
                        AdLogger.d(
                            "激励广告收益回调: value=%d, currency=%s",
                            value.valueMicros,
                            value.currencyCode
                        )
                        // 上报收益
                        reportRevenue(adHolder.ad, finalAdUnitId, value)

                        // 补充 ad_impression 事件并路由到 ThinkingData
                        reportAdData(
                            "ad_impression",
                            mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                                "number" to totalShowCount,
                                "ad_source" to (adHolder.ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "value" to (value.valueMicros / 1_000_000.0),
                                "currency" to value.currencyCode
                            )
                        )

                        // 触发 Ipu / Rpu 钩子
                        IpuController.onAdImpression("RW", value.valueMicros)
                        RpuController.onAdRevenue("RW", value.valueMicros)
                    }
                    override fun onAdShowedFullScreenContent() {
                        totalShowCount++
                        AdLogger.d("激励广告展示成功，总展示次数: %d", totalShowCount)
                        reportAdData(
                            "ad_reward_show",
                            mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "number" to totalShowCount
                            )
                        )
                    }

                    override fun onAdDismissedFullScreenContent() {
                        val outcome = RewardOutcome(
                            rewarded = rewardItem != null,
                            rewardType = rewardItem?.type,
                            rewardAmount = rewardItem?.amount
                        )
                        reportAdData(
                            "ad_reward_close",
                            mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "reward_granted" to outcome.rewarded
                            )
                        )
                        if (!continuation.isCompleted) {
                            continuation.resume(AdResult.Success(outcome))
                        }
                        adHolder.ad.adEventCallback = null

                        // 自动拉起下一次预加载
                        if (!isCacheFull(finalAdUnitId)) {
                            AdLogger.d("激励广告尝试补充缓存，广告位ID: %s", finalAdUnitId)
                            PreloadController.preload(activity)
                        }
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        totalFailCount++
                        AdLogger.e("激励广告展示失败: %s", fullScreenContentError.message)
                        val error = createAdException("展示失败: ${fullScreenContentError.message}")
                        reportAdData(
                            "ad_reward_show_fail",
                            mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "reason" to fullScreenContentError.message,
                                "code" to fullScreenContentError.code
                            )
                        )
                        if (!continuation.isCompleted) {
                            continuation.resume(AdResult.Failure(error))
                        }
                        adHolder.ad.adEventCallback = null
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        AdLogger.d("激励广告曝光完成")
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        AdLogger.d("原生广告被点击")

                        // 累积点击统计
                        totalClickCount++
                        AdLogger.d("原生广告累积点击次数: $totalClickCount")

                        AdConfigManager.getNativeConfig().recordClick()

                        reportAdData(
                            eventName = "ad_click",
                            params = mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                                "number" to totalClickCount,
                                "ad_source" to (adHolder.ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                "currency" to (currentAdValue?.currencyCode ?: "")
                            )
                        )
                    }
                }

                try {
                    adHolder.ad.show(activity) { item ->
                        rewardItem = item
                        totalRewardCount++
                        reportAdData(
                            "ad_reward_earned",
                            mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "number" to totalRewardCount,
                                "type" to item.type,
                                "amount" to item.amount
                            )
                        )
                    }
                } catch (e: Exception) {
                    totalFailCount++
                    val error = createAdException("展示异常: ${e.message}", e)
                    if (!continuation.isCompleted) {
                        continuation.resume(AdResult.Failure(error))
                    }

                }

                continuation.invokeOnCancellation {
                    adHolder.ad.adEventCallback = null
                }
            }
        } catch (e: Exception) {
            ADLoadingDialog.hide()
            totalFailCount++
            val error = createAdException("展示异常: ${e.message}", e)
            AdResult.Failure(error)
        }
    }

    /**
     * 当前指定广告位是否有可用缓存
     */
    fun isReady(adUnitId: String? = null): Boolean {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID
        return getCachedAdCount(finalAdUnitId) > 0
    }

    /**
     * 清空缓存
     */
    fun release() {
        synchronized(cacheLock) { adCachePool.clear() }
        AdLogger.d("激励广告缓存已清理")
    }

    private suspend fun loadInternal(context: Context, adUnitId: String): RewardedAd? =
        suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            val adRequest = AdRequest.Builder(adUnitId)
                .build()

            RewardedAd.load(
                adRequest,
                object : AdLoadCallback<RewardedAd> {


                    override fun onAdLoaded(ad: RewardedAd) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.d("激励广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                        reportAdData(
                            "ad_reward_loaded",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            )
                        )
                        continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        AdLogger.w("激励广告加载失败: %s", adError.message)
                        reportAdData(
                            "ad_reward_load_fail",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "reason" to adError.message,
                                "code" to adError.code
                            )
                        )
                        continuation.resume(null)
                    }
                }
            )
        }

    private fun getCachedAd(adUnitId: String): CachedRewardedAd? {
        synchronized(cacheLock) {
            val index = adCachePool.indexOfFirst { it.adUnitId == adUnitId && !it.isExpired() }
            return if (index != -1) {
                adCachePool.removeAt(index)
            } else {
                null
            }
        }
    }

    private fun getCachedAdCount(adUnitId: String): Int {
        synchronized(cacheLock) {
            // 同步移除过期广告
            adCachePool.removeAll { it.adUnitId == adUnitId && it.isExpired() }
            return adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
        }
    }

    private fun isCacheFull(adUnitId: String): Boolean {
        return getCachedAdCount(adUnitId) >= maxCacheSizePerAdUnit
    }

    private fun reportRevenue(rewardedAd: RewardedAd, adUnitId: String,adValue: AdValue) {
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = rewardedAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = rewardedAd.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "Rewarded"
        )

        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d(
            "激励广告收益已上报，广告位ID: %s, 收益(微元): %d %s",
            adUnitId,
            adValue.valueMicros,
            adValue.currencyCode
        )
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to "Rewarded"
        )
        data.putAll(params)
        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }

    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(
            code = 0,
            message = message,
            cause = cause
        )
    }

    data class RewardOutcome(
        val rewarded: Boolean,
        val rewardType: String? = null,
        val rewardAmount: Int? = null
    )
}
