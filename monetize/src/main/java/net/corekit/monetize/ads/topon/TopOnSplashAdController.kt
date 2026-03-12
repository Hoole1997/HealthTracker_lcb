package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.thinkup.splashad.api.TUSplashAd
import com.thinkup.splashad.api.TUSplashAdExtraInfo
import com.thinkup.splashad.api.TUSplashAdListener
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
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
 * TopOn 开屏广告控制器
 */
class TopOnSplashAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("topon_sp_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_sp_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("topon_sp_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("topon_sp_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("topon_sp_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_sp_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("topon_sp_click_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("topon_sp_close_count", 0)
    private var currentPosition: String = "Splash"
    private var currentAdSource: String = "TopOn"

    companion object {
        private const val TAG = "TopOnSplash"

        @Volatile
        private var instance: TopOnSplashAdController? = null

        fun getInstance(): TopOnSplashAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnSplashAdController().also { instance = it }
            }
        }
    }

    private var splashAd: TUSplashAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 4 * 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnSplashId()) {
            AdLogger.d("[$TAG] 开屏广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.SPLASH_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.TOPON_SPLASH_ID
        
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.TOPON, BiddingAdType.SPLASH)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.TOPON, BiddingAdType.SPLASH)
            AdLogger.w("[$TAG] 加载跳过 | 平台: TopOn | 类型: Splash | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "TopOn"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        totalLoadCount++
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val startTime = System.currentTimeMillis()

                AdLogger.d("[$TAG] 开始加载开屏广告, ID: %s", adUnitId)

                var currentAd: TUSplashAd? = null
                val listener = object : TUSplashAdListener {
                    override fun onAdLoaded(isTimeout: Boolean) {
                        val loadTime = System.currentTimeMillis() - startTime
                        loadTimestamp = System.currentTimeMillis()
                        
                        // 尝试使用 checkValidAdCaches 获取 eCPM
                        cachedEcpm = try {
                            currentAd?.checkValidAdCaches()?.firstOrNull()?.publisherRevenue?.toDouble() ?: 0.0
                        } catch (e: Exception) { 0.0 }

                        AdLogger.d(
                            "[$TAG] ✅ 开屏广告加载成功, 耗时: %d ms, eCPM: %.6f USD, isTimeout: %s",
                            loadTime,
                            cachedEcpm,
                            isTimeout
                        )

                        totalLoadSucCount++
                        
                        // 尝试获取加载成功的广告源
                        val networkName = currentAd?.checkValidAdCaches()?.firstOrNull()?.networkName
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
                        FpuController.onAdFill("SP")

                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@TopOnSplashAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }
                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }

                    override fun onAdLoadTimeout() {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("[$TAG] ❌ 开屏广告加载超时, 耗时: %d ms", loadTime)

                        totalLoadFailCount++
                        reportAdData(
                            "ad_load_fail",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadFailCount,
                                "ad_source" to "TopOn",
                                "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                "reason" to "加载超时"
                            )
                        )

                        deferred.complete(
                            AdResult.Failure(
                                AdException(
                                    AdException.ERROR_TIMEOUT,
                                    "加载超时"
                                )
                            )
                        )
                        synchronized(this@TopOnSplashAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }
                        if (continuation.isActive) continuation.resume(
                            AdResult.Failure(
                                AdException(
                                    AdException.ERROR_TIMEOUT,
                                    "加载超时"
                                )
                            )
                        )
                    }

                    override fun onNoAdError(error: AdError?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e(
                            "[$TAG] ❌ 开屏广告加载失败, 耗时: %d ms, error: %s",
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

                        deferred.complete(
                            AdResult.Failure(
                                AdException(
                                    parseErrorCode(error?.code),
                                    error?.desc ?: "加载失败"
                                )
                            )
                        )
                        synchronized(this@TopOnSplashAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }
                        if (continuation.isActive) continuation.resume(
                            AdResult.Failure(
                                AdException(
                                    parseErrorCode(error?.code),
                                    error?.desc ?: "加载失败"
                                )
                            )
                        )
                    }

                    override fun onAdShow(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 开屏广告已展示")
                        cachedEcpm = info?.publisherRevenue ?: info?.ecpm ?: 0.0
                        currentAdSource = info?.networkName ?: "TopOn"
                        
                        totalShowCount++
                        val ecpmMicros = (cachedEcpm * 1_000_000).toLong()
                        reportAdData(
                            "ad_impression",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalShowCount,
                                "ad_source" to currentAdSource,
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
                                adRevenueNetwork = currentAdSource,
                                adRevenueUnit = adUnitId,
                                adRevenuePlacement = currentPosition,
                                adFormat = "Splash"
                            )
                        )
                        IpuController.onAdImpression("SP", ecpmMicros)
                        RpuController.onAdRevenue("SP", ecpmMicros)
                    }

                    override fun onAdClick(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 开屏广告被点击")
                        totalClickCount++
                        AdConfigManager.getAppOpenConfig().recordClick()
                        PlatformFrequencyManager.recordClick(BiddingPlatform.TOPON, BiddingAdType.SPLASH)
                        reportAdData(
                            "ad_click",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalClickCount,
                                "ad_source" to currentAdSource,
                                "value" to cachedEcpm,
                                "currency" to "USD"
                            )
                        )
                    }

                    override fun onAdDismiss(info: TUAdInfo?, extraInfo: TUSplashAdExtraInfo?) {
                        AdLogger.d("[$TAG] 开屏广告已关闭")
                        totalCloseCount++
                        reportAdData(
                            "ad_dismiss",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalCloseCount,
                                "ad_source" to currentAdSource,
                                "value" to cachedEcpm,
                                "currency" to "USD"
                            )
                        )
                    }
                }

                val ad = TUSplashAd(context, adUnitId, listener, 5000)
                currentAd = ad
                splashAd = ad
                ad.loadAd()
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(createAdException("加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) loadingDeferred = null
            }
            throw e
        }
    }

    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(0, message, cause)
    }

    suspend fun showAd(
        activity: Activity,
        container: ViewGroup,
        onLoaded: ((Boolean) -> Unit)? = null,
        onShow: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = splashAd
        val adUnitId = BuildConfig.TOPON_SPLASH_ID

        totalShowTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to adUnitId,
                "position" to currentPosition,
                "number" to totalShowTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.TOPON, BiddingAdType.SPLASH)) {
            totalShowFailCount++
            reportAdData(
                "ad_show_error",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
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

        if (ad == null || !ad.isAdReady) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            totalShowFailCount++
            reportAdData(
                "ad_show_error",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
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

        ad.setAdListener(object : TUSplashAdListener {
            override fun onAdLoaded(isTimeout: Boolean) {}
            override fun onAdLoadTimeout() {}
            override fun onNoAdError(error: AdError?) {}
            override fun onAdShow(info: TUAdInfo?) {
                AdLogger.d("[$TAG] 开屏广告已展示")
                onShow?.invoke()
            }
            override fun onAdClick(info: TUAdInfo?) {}

            override fun onAdDismiss(info: TUAdInfo?, extraInfo: TUSplashAdExtraInfo?) {
                AdLogger.d("[$TAG] 开屏广告已关闭")
                clearCache()
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })

        ad.show(activity, container)
    }

    private fun parseErrorCode(code: String?): Int {
        return code?.toIntOrNull() ?: AdException.ERROR_INTERNAL
    }

    private fun parseEcpm(ecpmLevel: Any?): Double {
        return when (ecpmLevel) {
            is Number -> ecpmLevel.toDouble()
            is String -> ecpmLevel.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedEcpm else 0.0

    fun hasValidCache(): Boolean {
        val ad = splashAd ?: return false
        if (!ad.isAdReady) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        splashAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "Splash")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }
}
