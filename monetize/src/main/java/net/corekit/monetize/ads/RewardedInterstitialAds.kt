package net.corekit.monetize.ads

import android.app.Activity
import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
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
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil
import net.corekit.monetize.ui.dialog.ADLoadingDialog
import net.corekit.monetize.util.PositionGet
import kotlin.coroutines.resume
import kotlin.math.ceil

class RewardedInterstitialAds private constructor() {

    companion object {
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2

        @Volatile
        private var INSTANCE: RewardedInterstitialAds? = null

        fun getInstance(): RewardedInterstitialAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardedInterstitialAds().also { INSTANCE = it }
            }
        }
    }

    private data class CachedRewardedInterstitialAd(
        val ad: RewardedInterstitialAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > 1 * 60 * 60 * 1000L
        }
    }

    private val cacheLock = Any()
    private val adCachePool = mutableListOf<CachedRewardedInterstitialAd>()
    private val maxCacheSizePerAdUnit = DEFAULT_CACHE_SIZE_PER_AD_UNIT

    private var totalTriggerCount by DataStoreIntDelegate("rvi_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("rvi_show_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("rvi_load_fail_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("rvi_load_suc_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("rvi_show_fail_count", 0)
    private var totalRewardCount by DataStoreIntDelegate("rvi_reward_count", 0)
    private var totalClickCount by DataStoreIntDelegate("rvi_click_count", 0)
    private var totalLoadCount by DataStoreIntDelegate("rvi_load_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("rvi_close_count", 0)

    private var currentAdValue: AdValue? = null
    
    // 插页激励广告是否正在显示的标识
    private var isShowing: Boolean = false

    suspend fun loadInAdvance(context: Context, adUnitId: String? = null): AdResult<Unit> {
        AdsManager.awaitInitialized()
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID

        if (isCacheFull(finalAdUnitId)) {
            AdLogger.d(
                "插页激励广告缓存已满，广告位ID: %s，当前缓存: %d/%d",
                finalAdUnitId,
                getCachedAdCount(finalAdUnitId),
                maxCacheSizePerAdUnit
            )
            return AdResult.Success(Unit)
        }

        return try {
            reportAdData(
                eventName = "ad_start_load",
                params = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "number" to totalLoadCount
                )
            )

            val ad = loadInternal(context.applicationContext, finalAdUnitId)
            if (ad != null) {
                synchronized(cacheLock) {
                    adCachePool.add(CachedRewardedInterstitialAd(ad, finalAdUnitId))
                    val currentCount = getCachedAdCount(finalAdUnitId)
                    AdLogger.d(
                        "插页激励广告加载成功并缓存，广告位ID: %s，缓存数量: %d/%d",
                        finalAdUnitId,
                        currentCount,
                        maxCacheSizePerAdUnit
                    )
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("插页激励广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("插页激励广告加载异常", e)
            AdResult.Failure(createAdException("加载异常: ${e.message}", e))
        }
    }

    suspend fun displayAd(activity: Activity, adUnitId: String? = null): AdResult<RewardedAds.RewardOutcome> {
        AdsManager.awaitInitialized()
        
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID

        val position = PositionGet.get()

        // 每次开始展示前清理状态，避免收益/展示状态污染
        currentAdValue = null
        
        // 检查是否正在显示
        if (isShowing) {
            AdLogger.w("插页激励广告正在显示中，忽略本次展示请求")
            return AdResult.Failure(createAdException("广告正在显示中"))
        }
        
        // 检查 Activity 生命周期
        if (activity.isFinishing || activity.isDestroyed) {
            AdLogger.w("插页激励广告展示失败：Activity 已销毁")
            return AdResult.Failure(createAdException("Activity is finishing or destroyed"))
        }

        totalTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to position,
                "number" to totalTriggerCount
            )
        )

        return try {
            var cachedAd = getCachedAd(finalAdUnitId)
            var loadingShown = false

            if (cachedAd == null) {
                loadingShown = true
                ADLoadingDialog.show(activity)
                loadInAdvance(activity, finalAdUnitId)
                cachedAd = getCachedAd(finalAdUnitId)
            }

            if (loadingShown) {
                ADLoadingDialog.hide()
            }

            val adHolder = cachedAd
            if (adHolder == null) {
                return AdResult.Failure(createAdException("插页激励广告缓存为空"))
            }

            showAdInternal(activity, adHolder.ad, finalAdUnitId, position)
        } catch (e: Exception) {
            AdLogger.e("显示插页激励广告异常", e)
            AdResult.Failure(createAdException("显示广告异常: ${e.message}", e))
        } finally {
            ADLoadingDialog.hide()
        }
    }

    fun peekCachedAd(adUnitId: String? = null): RewardedInterstitialAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID
        synchronized(cacheLock) {
            adCachePool.removeAll { it.adUnitId == finalAdUnitId && it.isExpired() }
            return adCachePool.firstOrNull { it.adUnitId == finalAdUnitId && !it.isExpired() }?.ad
        }
    }

    fun hasCachedAd(adUnitId: String? = null): Boolean {
        return peekCachedAd(adUnitId) != null
    }

    suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID
        val cachedAd = peekCachedAd(finalAdUnitId)
        if (cachedAd == null) {
            AdLogger.w("[竞价] 获取插页激励广告价格失败：缓存为空")
            return null
        }

        val adValue = withContext(Dispatchers.Default) {
            AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd)
        }

        return if (adValue != null) {
            val price = adValue.valueMicros / 1_000_000.0
            AdLogger.d("[竞价] 获取插页激励广告价格成功: %.6f %s (精度: %s)", price, adValue.currencyCode, adValue.precisionType)
            price
        } else {
            AdLogger.w("[竞价] 获取插页激励广告价格失败：反射获取AdValue为空")
            null
        }
    }

    private suspend fun loadInternal(context: Context, adUnitId: String): RewardedInterstitialAd? =
        suspendCancellableCoroutine { continuation ->
            totalLoadCount++
            val startTime = System.currentTimeMillis()
            val adRequest = AdRequest.Builder(adUnitId).build()

            RewardedInterstitialAd.load(
                adRequest,
                object : AdLoadCallback<RewardedInterstitialAd> {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        if (!continuation.isActive) {
                            return
                        }
                        totalLoadSucCount++
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.d("插页激励广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                        reportAdData(
                            "ad_loaded",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadSucCount,
                                "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            )
                        )
                        FpuController.onAdFill("RVI")
                        continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (!continuation.isActive) {
                            return
                        }
                        totalLoadFailCount++
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.w("插页激励广告加载失败: %s", adError.message)
                        reportAdData(
                            "ad_load_fail",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadFailCount,
                                "ad_source" to (adError.responseInfo?.loadedAdSourceResponseInfo?.name.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                "reason" to adError.message,
                                "code" to adError.code
                            )
                        )
                        continuation.resume(null)
                    }
                }
            )
        }

    private suspend fun showAdInternal(
        activity: Activity,
        ad: RewardedInterstitialAd,
        adUnitId: String,
        position: String
    ): AdResult<RewardedAds.RewardOutcome> {
        return suspendCancellableCoroutine { continuation ->
            var rewardItem: RewardItem? = null

            ad.adEventCallback = object : RewardedInterstitialAdEventCallback {
                override fun onAdPaid(value: AdValue) {
                    super.onAdPaid(value)
                    currentAdValue = value
                    AdLogger.d("插页激励广告收益回调: value=%d, currency=%s", value.valueMicros, value.currencyCode)

                    reportRevenue(ad, adUnitId, value)

                    reportAdData(
                        "ad_impression",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalShowCount,
                            "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (value.valueMicros / 1_000_000.0),
                            "currency" to value.currencyCode
                        )
                    )

                    IpuController.onAdImpression("RVI", value.valueMicros)
                    RpuController.onAdRevenue("RVI", value.valueMicros)
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    isShowing = true
                    totalShowCount++
                    AdLogger.d("插页激励广告开始显示，总展示次数: %d", totalShowCount)
                }

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    totalCloseCount++

                    val outcome = RewardedAds.RewardOutcome(
                        rewarded = rewardItem != null,
                        rewardType = rewardItem?.type,
                        rewardAmount = rewardItem?.amount
                    )

                    reportAdData(
                        "ad_close",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalCloseCount,
                            "reward_granted" to outcome.rewarded,
                            "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                    // 重置展示状态
                    isShowing = false
                    currentAdValue = null
                    
                    val result = AdResult.Success(outcome)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                    ad.adEventCallback = null
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                    totalShowFailCount++

                    AdLogger.w("插页激励广告显示失败: %s", fullScreenContentError.message)
                    reportAdData(
                        "ad_show_fail",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "reason" to fullScreenContentError.message,
                            "code" to fullScreenContentError.code,
                            "number" to totalShowFailCount
                        )
                    )

                    // 失败路径兜底复位状态
                    isShowing = false
                    currentAdValue = null
                    
                    val result = AdResult.Failure(createAdException("show failed: ${fullScreenContentError.message}"))
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                    ad.adEventCallback = null
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    AdLogger.d("插页激励广告曝光完成")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    totalClickCount++
                    AdLogger.d("插页激励广告被点击，总点击次数: %d", totalClickCount)

                    reportAdData(
                        "ad_click",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalClickCount,
                            "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )
                }
            }

            ad.show(activity) { item ->
                rewardItem = item
                totalRewardCount++
                reportAdData(
                    "ad_reward_earned",
                    mapOf(
                        "ad_unit_name" to adUnitId,
                        "number" to totalRewardCount,
                        "type" to item.type,
                        "amount" to item.amount,
                        "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty())
                    )
                )
            }
        }
    }

    private fun getCachedAd(adUnitId: String): CachedRewardedInterstitialAd? {
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
            adCachePool.removeAll { it.adUnitId == adUnitId && it.isExpired() }
            return adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
        }
    }

    private fun isCacheFull(adUnitId: String): Boolean {
        return getCachedAdCount(adUnitId) >= maxCacheSizePerAdUnit
    }

    private fun reportRevenue(ad: RewardedInterstitialAd, adUnitId: String, adValue: AdValue) {
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = ad.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "RewardedInterstitial"
        )

        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d(
            "插页激励广告收益已上报，广告位ID: %s, 收益(微元): %d %s",
            adUnitId,
            adValue.valueMicros,
            adValue.currencyCode
        )
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to "RewardedInterstitial"
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
}
