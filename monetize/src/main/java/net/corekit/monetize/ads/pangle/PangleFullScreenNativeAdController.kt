package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.bytedance.sdk.openadsdk.api.model.PAGAdEcpmInfo
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel
import com.bytedance.sdk.openadsdk.api.model.PAGRevenueInfo
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionCallback
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadCallback
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.interceptor.ClickLimitInterceptor
import net.corekit.monetize.ads.interceptor.GlobalAdSwitchInterceptor
import net.corekit.monetize.ads.interceptor.InterceptorChain
import net.corekit.monetize.ads.interceptor.ShowCountLimitInterceptor
import net.corekit.monetize.ads.interceptor.ShowIntervalLimitInterceptor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Pangle全屏原生广告控制器
 * 参考文档：https://www.pangleglobal.com/integration/android-native-ads
 */
class PangleFullScreenNativeAdController private constructor() {

    // 累积点击/展示等统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("pangle_full_native_total_clicks", 0)
    private var totalCloseCount by DataStoreIntDelegate("pangle_full_native_total_close", 0)
    private var totalLoadCount by DataStoreIntDelegate("pangle_full_native_total_loads", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_full_native_total_load_suc", 0)
    private var totalShowFailCount by DataStoreIntDelegate("pangle_full_native_total_show_fails", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("pangle_full_native_total_show_triggers", 0)
    private var totalShowCount by DataStoreIntDelegate("pangle_full_native_total_shows", 0)

    private val nativeAdView = PangleFullScreenNativeAdView()

    // 全屏原生广告是否正在显示的标识
    private var isShowing: Boolean = false

    companion object {
        private const val TAG = "PangleFullScreenNative"
        private const val AD_TIMEOUT = 1 * 60 * 60 * 1000L
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 1

        @Volatile
        private var INSTANCE: PangleFullScreenNativeAdController? = null

        fun getInstance(): PangleFullScreenNativeAdController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PangleFullScreenNativeAdController().also { INSTANCE = it }
            }
        }
    }

    private data class CachedFullScreenNativeAd(
        val ad: PAGNativeAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > AD_TIMEOUT
        }
    }

    private val adCachePool = mutableListOf<CachedFullScreenNativeAd>()
    private val interceptorChain = InterceptorChain(
        interceptors = listOf(
            GlobalAdSwitchInterceptor(),
            ShowCountLimitInterceptor(),
            ShowIntervalLimitInterceptor(),
            ClickLimitInterceptor()
        )
    )

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Pangle",
            "ad_format" to "FullNative"
        )
        data.putAll(params)
        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }

    fun closeEvent(
        adUnitId: String = "",
        adSource: String? = "Pangle",
        valueUsd: Double? = null,
        currencyCode: String? = null
    ) {
        // 设置广告不再显示标识
        isShowing = false
        totalCloseCount++
        val params: Map<String, Any> = mapOf(
            "ad_unit_name" to adUnitId,
            "position" to "",
            "number" to totalCloseCount,
            "ad_source" to (adSource ?: "Pangle"),
            "value" to (valueUsd ?: 0.0),
            "currency" to (currencyCode ?: "USD")
        )
        reportAdData(
            eventName = "ad_close",
            params = params
        )
    }

    suspend fun preloadAd(context: Context, adUnitId: String? = null): AdResult<Unit> {
        if (!GlobalAdSwitchInterceptor.isGlobalAdEnabled()) {
            return AdResult.Failure(
                AdException(
                    code = -100,
                    message = "全屏原生广告全局开关已关闭"
                )
            )
        }
        val finalAdUnitId = adUnitId ?: BuildConfig.PANGLE_FULL_NATIVE_ID
        return loadAdToCache(context, finalAdUnitId)
    }

    suspend fun showAdInContainer(
        context: Context,
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        adUnitId: String? = null
    ): AdResult<Unit> {
        val finalAdUnitId = adUnitId ?: BuildConfig.PANGLE_FULL_NATIVE_ID

        totalShowTriggerCount++
        val posParams: Map<String, Any> = mapOf(
            "ad_unit_name" to finalAdUnitId,
            "position" to "",
            "number" to totalShowTriggerCount
        )
        reportAdData(
            eventName = "ad_position",
            params = posParams
        )

        when (val interceptResult = interceptorChain.intercept(context, AdConfigManager.getFullscreenNativeConfig())) {
            is AdResult.Failure -> {
                totalShowFailCount++
                val failParams: Map<String, Any> = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to "",
                    "number" to totalShowFailCount,
                    "reason" to interceptResult.error.message.orEmpty()
                )
                reportAdData(
                    eventName = "ad_show_fail",
                    params = failParams
                )
                return AdResult.Failure(interceptResult.error)
            }
            else -> Unit
        }

        return try {
            nativeAdView.createFullScreenLoadingView(context, container)

            when (val result = getAd(context, finalAdUnitId)) {
                is AdResult.Success -> {
                    val nativeAd = result.data

                    if(!nativeAd.isReady){
                        throw IllegalArgumentException("full_native_not_ready")
                    }

                    var currentRevenueUsd: Double? = null
                    var currentCurrency: String? = null
                    var currentAdSource: String? = null
                    var currentPlacement: String? = null
                    var currentRevenueAdUnit: String? = null

                    val bindSuccess = nativeAdView.bindFullScreenNativeAdToContainer(
                        context = context,
                        container = container,
                        nativeAd = nativeAd,
                        lifecycleOwner = lifecycleOwner,
                        interactionListener = object : PAGNativeAdInteractionCallback() {
                            override fun onAdShowed() {
                                AdLogger.d("Pangle全屏原生广告开始显示")
                                val pagRevenueInfo: PAGRevenueInfo? = nativeAd.pagRevenueInfo
                                val ecpmInfo: PAGAdEcpmInfo? = pagRevenueInfo?.showEcpm
                                currentCurrency = ecpmInfo?.currency
                                currentAdSource = ecpmInfo?.adnName
                                currentPlacement = ecpmInfo?.placement
                                currentRevenueAdUnit = ecpmInfo?.adUnit
                                // Pangle 的 revenue 本身就是美元，直接使用
                                val revenueUsd = ecpmInfo?.revenue?.toDoubleOrNull() ?: 0.0
                                currentRevenueUsd = revenueUsd
                                val impressionValue = revenueUsd

                                // 设置广告正在显示标识
                                isShowing = true

                                totalShowCount++
                                AdConfigManager.getFullscreenNativeConfig().recordShow()

                                val impressionParams: Map<String, Any> = mapOf(
                                    "ad_unit_name" to finalAdUnitId,
                                    "position" to "",
                                    "number" to totalShowCount,
                                    "ad_source" to (currentAdSource ?: "Pangle"),
                                    "value" to (impressionValue?:0.0),
                                    "currency" to (currentCurrency ?: "USD")
                                )
                                reportAdData(
                                    eventName = "ad_impression",
                                    params = impressionParams
                                )

                                currentRevenueUsd?.let { revenueValue ->
                                    reportAdRevenueWithValue(
                                        adUnitId = finalAdUnitId,
                                        valueUsd = revenueValue,
                                        currencyCode = currentCurrency,
                                        adNetwork = currentAdSource,
                                        placement = currentPlacement,
                                        ecpmAdUnitId = currentRevenueAdUnit
                                    )
                                    // Pangle 的 revenue 本身就是美元，直接使用
                                    val revenueUsd = ecpmInfo?.revenue?.toDoubleOrNull()?.toLong() ?: 0L
                                    IpuController.onAdImpression("FullNa", revenueUsd)
                                    RpuController.onAdRevenue("FullNa", revenueUsd)
                                    AdLogger.d(
                                        "Pangle全屏原生收益(onShow): adUnit=%s, placement=%s, adn=%s, revenueUsd=%.4f, currency=%s",
                                        currentRevenueAdUnit ?: finalAdUnitId,
                                        currentPlacement ?: "",
                                        currentAdSource ?: "Pangle",
                                        revenueValue,
                                        currentCurrency ?: ""
                                    )
                                }
                            }

                            override fun onAdClicked() {
                                AdLogger.d("Pangle全屏原生广告被点击")
                                totalClickCount++
                                AdConfigManager.getFullscreenNativeConfig().recordClick()
                                val clickParams: Map<String, Any> = mapOf(
                                    "ad_unit_name" to finalAdUnitId,
                                    "position" to "",
                                    "number" to totalClickCount,
                                    "ad_source" to (currentAdSource ?: "Pangle"),
                                    "value" to (nativeAd.pagRevenueInfo?.showEcpm?.revenue?.toDoubleOrNull() ?: 0.0),
                                    "currency" to (currentCurrency ?: "USD")
                                )
                                reportAdData(
                                    eventName = "ad_click",
                                    params = clickParams
                                )
                            }

                            override fun onAdDismissed() {
                                AdLogger.d("Pangle全屏原生广告关闭")
                                closeEvent(
                                    adUnitId = finalAdUnitId,
                                    adSource = currentAdSource,
                                    valueUsd = currentRevenueUsd,
                                    currencyCode = currentCurrency
                                )
                            }

                            override fun onAdShowFailed(error: PAGErrorModel) {
                                super.onAdShowFailed(error)
                                totalShowFailCount++
                                AdLogger.e(
                                    "Pangle全屏原生广告显示失败: code=%d, message=%s",
                                    error.errorCode,
                                    error.errorMessage
                                )
                                reportAdData(
                                    eventName = "ad_show_fail",
                                    params = mapOf<String, Any>(
                                        "ad_unit_name" to finalAdUnitId,
                                        "position" to "",
                                        "number" to totalShowFailCount,
                                        "reason" to error.errorMessage.orEmpty(),
                                        "ad_source" to (currentAdSource ?: "Pangle")
                                    )
                                )
                            }
                        }
                    )

                    if (bindSuccess) {
                        AdResult.Success(Unit)
                    } else {
                        totalShowFailCount++
                        reportAdData(
                            eventName = "ad_show_fail",
                            params = mapOf<String, Any>(
                                "ad_unit_name" to finalAdUnitId,
                                "position" to "",
                                "number" to totalShowFailCount,
                                "reason" to "bind_failed"
                            )
                        )
                        AdResult.Failure(createAdException("广告绑定失败"))
                    }
                }
                is AdResult.Failure -> {
                    totalShowFailCount++
                    val failParams: Map<String, Any> = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to "",
                        "number" to totalShowFailCount,
                        "reason" to (result.error.message ?: "")
                    )
                    reportAdData(
                        eventName = "ad_show_fail",
                        params = failParams
                    )
                    AdResult.Failure(result.error)
                }
                AdResult.Loading -> AdResult.Loading
            }
        } catch (e: Exception) {
            totalShowFailCount++
            val failParams: Map<String, Any> = mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to "",
                "number" to totalShowFailCount,
                "reason" to e.message.orEmpty()
            )
            reportAdData(
                eventName = "ad_show_fail",
                params = failParams
            )
            AdLogger.e("Pangle全屏原生广告展示异常", e)
            AdResult.Failure(createAdException("显示异常: ${e.message}", e))
        }
    }

    private fun createInteractionListener(adUnitId: String): PAGNativeAdInteractionCallback {
        return object : PAGNativeAdInteractionCallback() {
            override fun onAdShowed() {
                AdLogger.d("Pangle全屏原生广告开始显示")
                AdConfigManager.getFullscreenNativeConfig().recordShow()
            }

            override fun onAdClicked() {
                AdLogger.d("Pangle全屏原生广告被点击")
                totalClickCount++
                AdConfigManager.getFullscreenNativeConfig().recordClick()
                reportAdData(
                    eventName = "ad_click",
                    params = mapOf<String, Any>(
                        "ad_unit_name" to adUnitId,
                        "position" to "",
                        "number" to totalClickCount,
                        "ad_source" to "Pangle",
                        "value" to 0.0,
                        "currency" to "USD"
                    )
                )
            }

            override fun onAdDismissed() {
                AdLogger.d("Pangle全屏原生广告关闭")
                closeEvent(adUnitId)
            }
        }
    }

    private suspend fun getAd(context: Context, adUnitId: String): AdResult<PAGNativeAd> {
        var cachedAd = getCachedAd(adUnitId)
        if (cachedAd == null) {
            AdLogger.d("缓存为空，立即加载Pangle全屏原生广告，广告位ID: %s", adUnitId)
            loadAdToCache(context, adUnitId)
            cachedAd = getCachedAd(adUnitId)
        }
        return if (cachedAd != null) {
            AdResult.Success(cachedAd.ad)
        } else {
            AdResult.Failure(createAdException("load ad fail"))
        }
    }

    private fun getCachedAd(adUnitId: String): CachedFullScreenNativeAd? {
        synchronized(adCachePool) {
            val index = adCachePool.indexOfFirst { it.adUnitId == adUnitId && !it.isExpired() }
            return if (index != -1) adCachePool.removeAt(index) else null
        }
    }

    private fun peekCachedAd(adUnitId: String): PAGNativeAd? {
        synchronized(adCachePool) {
            return adCachePool.firstOrNull { it.adUnitId == adUnitId && !it.isExpired() }?.ad
        }
    }

    suspend fun loadAdToCache(context: Context, adUnitId: String): AdResult<Unit> {
        return try {
            // 检查缓存是否已满（需要同步访问）
            val currentCount = synchronized(adCachePool) {
                adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
            }
            if (currentCount >= DEFAULT_CACHE_SIZE_PER_AD_UNIT) {
                AdLogger.d("广告位 %s 缓存已满", adUnitId)
                return AdResult.Success(Unit)
            }
            val ad = loadAd(context, adUnitId)
            if (ad != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedFullScreenNativeAd(ad, adUnitId))
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("load ad fail"))
            }
        } catch (e: Exception) {
            AdLogger.e("Pangle全屏原生广告缓存加载异常", e)
            AdResult.Failure(createAdException("加载异常: ${e.message}", e))
        }
    }

    /**
     * 获取缓存广告的 eCPM
     */
    fun getEcpm(adUnitId: String? = null): Double {
        synchronized(adCachePool) {
            val finalAdUnitId = adUnitId ?: BuildConfig.PANGLE_FULL_NATIVE_ID
            val cachedAd = adCachePool.firstOrNull { it.adUnitId == finalAdUnitId && !it.isExpired() }
            return cachedAd?.ad?.pagRevenueInfo?.showEcpm?.revenue?.toDoubleOrNull() ?: 0.0
        }
    }

    fun getCurrentAd(adUnitId: String? = null): PAGNativeAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.PANGLE_FULL_NATIVE_ID
        return peekCachedAd(finalAdUnitId)
    }

    private suspend fun loadAd(context: Context, adUnitId: String): PAGNativeAd? {
        totalLoadCount++
        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            val request = PAGNativeRequest(context)
            PAGNativeAd.loadAd(adUnitId, request, object : PAGNativeAdLoadCallback {
                override fun onAdLoaded(ad: PAGNativeAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.d("Pangle全屏原生广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
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
                    FpuController.onAdFill("FullNa")
                    continuation.resume(ad)
                }

                override fun onError(model: PAGErrorModel) {
                    val code = model.errorCode
                    val message = model.errorMessage
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("Pangle全屏原生广告加载失败，广告位ID: %s, 耗时: %dms, 错误码: %d, 错误信息: %s", adUnitId, loadTime, code, message)
                    reportAdData(
                        eventName = "ad_load_fail",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to "Pangle",
                            "pass_time" to ceil(loadTime / 1000.0).toInt(),
                            "reason" to message
                        )
                    )
                    continuation.resume(null)
                }
            })
        }
    }

    fun hasCachedAd(adUnitId: String? = null): Boolean {
        synchronized(adCachePool) {
            return if (adUnitId != null) {
                adCachePool.any { it.adUnitId == adUnitId && !it.isExpired() }
            } else {
                adCachePool.any { !it.isExpired() }
            }
        }
    }

    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(
            code = -1,
            message = message,
            cause = cause
        )
    }

    fun hasValidCache(adUnitId: String? = null): Boolean {
        return hasCachedAd(adUnitId)
    }

    private fun reportAdRevenueWithValue(
        adUnitId: String,
        valueUsd: Double,
        currencyCode: String?,
        adNetwork: String?,
        placement: String?,
        ecpmAdUnitId: String?
    ) {
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = valueUsd,
                currencyCode = currencyCode ?: ""
            ),
            adRevenueNetwork = adNetwork ?: "Pangle",
            adRevenueUnit = ecpmAdUnitId ?: adUnitId,
            adRevenuePlacement = placement ?: "",
            adFormat = "FullNative"
        )

        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d(
            "Pangle全屏原生广告真实收益数据已上报，广告位ID: %s, 收益: %.4f %s, adn=%s, placement=%s",
            ecpmAdUnitId ?: adUnitId,
            valueUsd,
            currencyCode ?: "",
            adNetwork ?: "Pangle",
            placement ?: ""
        )
    }

    fun destroy() {
        synchronized(adCachePool) {
            adCachePool.clear()
        }
        AdLogger.d("Pangle全屏原生广告控制器已清理")
    }

    /**
     * 获取全屏原生广告是否正在显示的状态
     * @return true 如果全屏原生广告正在显示，false 否则
     */
    fun isAdShowing(): Boolean {
        return isShowing
    }
}
