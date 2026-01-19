package net.corekit.monetize.ads.pangle

import android.app.Activity
import android.content.Context
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest
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
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Pangle 插页广告控制器
 *
 * 管理 Pangle 插页广告的加载、展示和缓存
 */
class PangleInterstitialAdController private constructor() {

    // 累积统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pangle_iv_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_iv_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("pangle_iv_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("pangle_iv_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("pangle_iv_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("pangle_iv_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("pangle_iv_click_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("pangle_iv_close_count", 0)

    // 当前广告展示位置（用于埋点）
    private var currentPosition: String = ""
    private var currentAdSource: String = "Pangle"

    companion object {
        private const val TAG = "PangleInterstitial"

        @Volatile
        private var instance: PangleInterstitialAdController? = null

        fun getInstance(): PangleInterstitialAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleInterstitialAdController().also { instance = it }
            }
        }
    }

    // 缓存的广告
    private var cachedAd: PAGInterstitialAd? = null

    // 广告 eCPM（美分转美元）
    private var cachedEcpm: Double = 0.0

    // 加载状态
    private val isLoading = AtomicBoolean(false)

    // 广告加载时间戳（用于判断缓存是否过期）
    private var loadTimestamp: Long = 0

    // 缓存有效期（1小时）
    private val cacheExpireTime = 60 * 60 * 1000L

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
     * @param timeoutMillis 超时时间（毫秒）
     * @return 广告加载结果
     */
    suspend fun waitForAd(timeoutMillis: Long): AdResult<Unit> {
        val deferred = synchronized(this) {
            if (hasValidCache()) {
                return@synchronized CompletableDeferred(AdResult.Success(Unit))
            }
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

    /**
     * 预加载广告
     */
    suspend fun preloadAd(context: Context): AdResult<Unit> {
        // 检查广告 ID 是否有效
        if (!AdIdHelper.hasPangleInterstitialId()) {
            AdLogger.d("[$TAG] 插页广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.INTERSTITIAL_AD_ID_NOT_CONFIGURED.toAdException()
            )
        }

        // 检查 SDK 是否已初始化
        if (!PangleManager.isReady()) {
            val initResult = PangleManager.initialize(context)
            if (initResult is AdResult.Failure) {
                return initResult
            }
        }

        // 检查是否有有效缓存
        if (hasValidCache()) {
            AdLogger.d("[$TAG] 已有有效缓存，跳过加载")
            return AdResult.Success(Unit)
        }

        // 防止重复加载
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

    /**
     * 加载广告
     */
    private suspend fun loadAd(context: Context): AdResult<Unit> {
        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        // 累积加载次数统计
        totalLoadCount++
        val adUnitId = BuildConfig.PANGLE_INTERSTITIAL_ID

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

                AdLogger.d("[$TAG] 开始加载插页广告, ID: %s", adUnitId)

                val request = PAGInterstitialRequest()

                PAGInterstitialAd.loadAd(adUnitId, request, object : PAGInterstitialAdLoadListener {
                    override fun onAdLoaded(ad: PAGInterstitialAd) {
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
                            "[$TAG] ✅ 插页广告加载成功, 耗时: %d ms, eCPM: %.6f USD",
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
                        FpuController.onAdFill("IV")

                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@PangleInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }

                    override fun onError(code: Int, message: String?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e(
                            "[$TAG] ❌ 插页广告加载失败, 耗时: %d ms, code: %d, message: %s",
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

                        deferred.complete(
                            AdResult.Failure(
                                AdException(
                                    code,
                                    message ?: "加载失败"
                                )
                            )
                        )
                        synchronized(this@PangleInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

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
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(AdException(0, "加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) loadingDeferred = null
            }
            throw e
        }
    }

    /**
     * 展示广告
     */
    suspend fun showAd(
        activity: Activity,
        onDismiss: (() -> Unit)? = null,
        position: String = ""
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = cachedAd
        val adUnitId = BuildConfig.PANGLE_INTERSTITIAL_ID
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
            if (continuation.isActive) {
                continuation.resume(
                    AdResult.Failure(
                        AdException(
                            AdException.ERROR_NOT_LOADED,
                            "没有可用的缓存广告"
                        )
                    )
                )
            }
            return@suspendCancellableCoroutine
        }

        AdLogger.d("[$TAG] 准备展示插页广告")

        ad.setAdInteractionListener(object : PAGInterstitialAdInteractionListener {
            override fun onAdShowed() {
                AdLogger.d("[$TAG] 插页广告已展示")
                totalShowCount++

                // 在 paid 回调才上报 ad_impression，这里先上报收益
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

                // 上报收益数据
                val adRevenueData = RevenueAdData(
                    revenue = RevenueInfo(
                        value = cachedEcpm,
                        currencyCode = "USD"
                    ),
                    adRevenueNetwork = "Pangle",
                    adRevenueUnit = adUnitId,
                    adRevenuePlacement = currentPosition,
                    adFormat = "Interstitial"
                )
                RevenueAdManager.reportAdRevenue(adRevenueData)

                IpuController.onAdImpression("IV", ecpmMicros)
                RpuController.onAdRevenue("IV", ecpmMicros)
            }

            override fun onAdClicked() {
                AdLogger.d("[$TAG] 插页广告被点击")
                totalClickCount++
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
                AdLogger.d("[$TAG] 插页广告已关闭")
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
                if (continuation.isActive) {
                    continuation.resume(AdResult.Success(Unit))
                }
            }
        })

        ad.show(activity)
    }

    /**
     * 获取缓存广告的 eCPM
     */
    fun getEcpm(): Double {
        return if (hasValidCache()) cachedEcpm else 0.0
    }

    /**
     * 检查是否有有效缓存
     */
    fun hasValidCache(): Boolean {
        if (cachedAd == null) return false
        // 检查缓存是否过期
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    /**
     * 清除缓存
     */
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
            "ad_format" to "Interstitial"
        )
        data.putAll(params)

        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }
}
