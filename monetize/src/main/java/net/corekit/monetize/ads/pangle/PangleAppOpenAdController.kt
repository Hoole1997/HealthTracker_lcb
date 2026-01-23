package net.corekit.monetize.ads.pangle

import android.app.Activity
import android.content.Context
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAd
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdInteractionListener
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenAdLoadListener
import com.bytedance.sdk.openadsdk.api.open.PAGAppOpenRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
 * Pangle 开屏广告控制器
 */
class PangleAppOpenAdController private constructor() {

    // 累积统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pangle_sp_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_sp_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("pangle_sp_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("pangle_sp_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("pangle_sp_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("pangle_sp_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("pangle_sp_click_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("pangle_sp_close_count", 0)

    // 当前广告展示位置
    private var currentPosition: String = ""
    private var currentAdSource: String = "Pangle"

    companion object {
        private const val TAG = "PangleAppOpen"

        @Volatile
        private var instance: PangleAppOpenAdController? = null

        fun getInstance(): PangleAppOpenAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleAppOpenAdController().also { instance = it }
            }
        }
    }

    private var cachedAd: PAGAppOpenAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 4 * 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasPangleSplashId()) {
            AdLogger.d("[$TAG] 开屏广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.SPLASH_AD_ID_NOT_CONFIGURED.toAdException()
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

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
     * @param timeoutMillis 超时时间（毫秒）
     * @return 广告加载结果
     */
    suspend fun waitForAd(timeoutMillis: Long): AdResult<Unit> {
        val deferred = synchronized(this) {
            // 如果已有缓存，直接返回成功
            if (hasValidCache()) {
                return@synchronized CompletableDeferred(AdResult.Success(Unit))
            }
            // 如果正在加载，返回当前的 deferred
            loadingDeferred
        }

        if (deferred == null) {
            return AdResult.Failure(
                AdException(
                    AdException.ERROR_NOT_LOADED,
                    "没有正在进行的加载请求且无缓存"
                )
            )
        }

        return try {
            withTimeoutOrNull(timeoutMillis) {
                deferred.await()
            } ?: AdResult.Failure(AdErrorCode.AD_LOAD_TIMEOUT.toAdException())
        } catch (e: Exception) {
            AdResult.Failure(AdErrorCode.AD_LOAD_INTERRUPTED.toAdException(e))
        }
    }

    private suspend fun loadAd(context: Context): AdResult<Unit> {
        val adUnitId = BuildConfig.PANGLE_SPLASH_ID
        
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.PANGLE, BiddingAdType.SPLASH)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.PANGLE, BiddingAdType.SPLASH)
            AdLogger.w("[$TAG] 加载跳过 | 平台: Pangle | 类型: Splash | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Pangle"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        // 创建新的 deferred
        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        // 累积加载次数统计
        totalLoadCount++

        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )

        return try {
            suspendCancellableCoroutine { continuation ->
                val startTime = System.currentTimeMillis()

                AdLogger.d("[$TAG] 开始加载开屏广告, ID: %s", adUnitId)

                PAGAppOpenAd.loadAd(
                    adUnitId,
                    PAGAppOpenRequest(),
                    object : PAGAppOpenAdLoadListener {
                        override fun onAdLoaded(ad: PAGAppOpenAd) {
                            val loadTime = System.currentTimeMillis() - startTime
                            cachedAd = ad
                            loadTimestamp = System.currentTimeMillis()
                            cachedEcpm = try {
                                // 优先使用官方推荐的 pagRevenueInfo API
                                (ad.pagRevenueInfo?.winEcpm?.revenue as? Number)?.toDouble()
                                    ?: ad.mediaExtraInfo?.get("price")?.toString()?.toDoubleOrNull()
                                    ?: 0.0
                            } catch (e: Exception) {
                                0.0
                            }
                            AdLogger.d(
                                "[$TAG] ✅ 开屏广告加载成功, 耗时: %d ms, eCPM: %.6f USD",
                                loadTime,
                                cachedEcpm
                            )

                            totalLoadSucCount++
                            reportAdData(
                                eventName = "ad_loaded",
                                params = mapOf(
                                    "ad_unit_name" to adUnitId,
                                    "number" to totalLoadSucCount,
                                    "ad_source" to "Pangle",
                                    "pass_time" to ceil(loadTime / 1000.0).toInt()
                                )
                            )
                            FpuController.onAdFill("SP")

                            // 完成 deferred
                            deferred.complete(AdResult.Success(Unit))
                            synchronized(this@PangleAppOpenAdController) {
                                if (loadingDeferred == deferred) {
                                    loadingDeferred = null
                                }
                            }

                            if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                        }

                        override fun onError(code: Int, message: String?) {
                            val loadTime = System.currentTimeMillis() - startTime
                            AdLogger.e(
                                "[$TAG] ❌ 开屏广告加载失败, 耗时: %d ms, code: %d, message: %s",
                                loadTime,
                                code,
                                message
                            )

                            totalLoadFailCount++
                            reportAdData(
                                eventName = "ad_load_fail",
                                params = mapOf(
                                    "ad_unit_name" to adUnitId,
                                    "number" to totalLoadFailCount,
                                    "ad_source" to "Pangle",
                                    "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                    "reason" to (message ?: "code=$code")
                                )
                            )

                            // 失败 deferred
                            deferred.complete(
                                AdResult.Failure(
                                    AdException(
                                        code,
                                        message ?: "加载失败"
                                    )
                                )
                            )
                            synchronized(this@PangleAppOpenAdController) {
                                if (loadingDeferred == deferred) {
                                    loadingDeferred = null
                                }
                            }

                            if (continuation.isActive) continuation.resume(
                                AdResult.Failure(
                                    AdException(code, message ?: "加载失败")
                                )
                            )
                        }
                    })
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(createAdException("加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) {
                    loadingDeferred = null
                }
            }
            throw e
        }
    }

    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(0, message, cause)
    }

    suspend fun showAd(
        activity: Activity,
        position: String = "",
        onLoaded: ((Boolean) -> Unit)? = null,
        onShow: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = cachedAd
        val adUnitId = BuildConfig.PANGLE_SPLASH_ID
        currentPosition = position

        val adnName = ad?.pagRevenueInfo?.winEcpm?.adnName
        currentAdSource = if (adnName.isNullOrEmpty()) "Pangle" else adnName

        // 累积触发统计
        totalShowTriggerCount++
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.PANGLE, BiddingAdType.SPLASH)) {
            totalShowFailCount++
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )
            onLoaded?.invoke(false)
            if (continuation.isActive) continuation.resume(
                AdResult.Failure(AdErrorCode.AD_SHOW_FAILED.toAdException("platform_frequency_limit"))
            )
            return@suspendCancellableCoroutine
        }

        if (ad == null || !hasValidCache()) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            totalShowFailCount++
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "没有可用的缓存广告"
                )
            )
            onLoaded?.invoke(false)
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

        onLoaded?.invoke(true)
        AdLogger.d("[$TAG] 准备展示开屏广告")

        ad.setAdInteractionListener(object : PAGAppOpenAdInteractionListener {
            override fun onAdShowed() {
                AdLogger.d("[$TAG] 开屏广告已展示")
                totalShowCount++
                onShow?.invoke()

                val ecpmMicros = (cachedEcpm * 1_000_000).toLong()

                reportAdData(
                    eventName = "ad_impression",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to currentPosition,
                        "number" to totalShowCount,
                        "ad_source" to currentAdSource,
                        "value" to cachedEcpm,
                        "currency" to "USD"
                    )
                )

                val adRevenueData = RevenueAdData(
                    revenue = RevenueInfo(
                        value = cachedEcpm,
                        currencyCode = "USD"
                    ),
                    adRevenueNetwork = "Pangle",
                    adRevenueUnit = adUnitId,
                    adRevenuePlacement = currentPosition,
                    adFormat = "Splash"
                )
                RevenueAdManager.reportAdRevenue(adRevenueData)

                IpuController.onAdImpression("SP", ecpmMicros)
                RpuController.onAdRevenue("SP", ecpmMicros)
            }

            override fun onAdClicked() {
                AdLogger.d("[$TAG] 开屏广告被点击")
                totalClickCount++
                AdConfigManager.getAppOpenConfig().recordClick()
                PlatformFrequencyManager.recordClick(BiddingPlatform.PANGLE, BiddingAdType.SPLASH)
                reportAdData(
                    eventName = "ad_click",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to currentPosition,
                        "number" to totalClickCount,
                        "ad_source" to currentAdSource,
                        "value" to cachedEcpm,
                        "currency" to "USD"
                    )
                )
            }

            override fun onAdDismissed() {
                AdLogger.d("[$TAG] 开屏广告已关闭")
                totalCloseCount++
                reportAdData(
                    eventName = "ad_close",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to currentPosition,
                        "number" to totalCloseCount,
                        "ad_source" to "Pangle",
                        "value" to cachedEcpm,
                        "currency" to "USD"
                    )
                )
                clearCache()
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })

        ad.show(activity)
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedEcpm else 0.0

    fun hasValidCache(): Boolean {
        if (cachedAd == null) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        cachedAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    /**
     * 通用数据上报函数
     */
    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Pangle",
            "ad_format" to "Splash"
        )
        data.putAll(params)

        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }
}
