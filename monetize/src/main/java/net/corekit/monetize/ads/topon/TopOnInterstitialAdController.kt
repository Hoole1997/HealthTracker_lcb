package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import com.thinkup.interstitial.api.TUInterstitial
import com.thinkup.interstitial.api.TUInterstitialListener
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
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * TopOn 插页广告控制器
 */
class TopOnInterstitialAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("topon_it_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_it_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("topon_it_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("topon_it_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("topon_it_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_it_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("topon_it_click_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("topon_it_close_count", 0)
    private var currentPosition: String = ""
    private var currentAdSource: String = "TopOn"

    companion object {
        private const val TAG = "TopOnInterstitial"

        @Volatile
        private var instance: TopOnInterstitialAdController? = null

        fun getInstance(): TopOnInterstitialAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnInterstitialAdController().also { instance = it }
            }
        }
    }

    private var interstitialAd: TUInterstitial? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
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

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnInterstitialId()) {
            AdLogger.d("[$TAG] 插页广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.INTERSTITIAL_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.TOPON_INTERSTITIAL_ID
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val startTime = System.currentTimeMillis()

                AdLogger.d("[$TAG] 开始加载插页广告, ID: %s", adUnitId)

                val ad = TUInterstitial(context, adUnitId)
                interstitialAd = ad

                ad.setAdListener(object : TUInterstitialListener {
                    override fun onInterstitialAdLoaded() {
                        val loadTime = System.currentTimeMillis() - startTime
                        loadTimestamp = System.currentTimeMillis()
                        
                        // 尝试使用 checkValidAdCaches 获取 eCPM
                        cachedEcpm = try {
                            ad.checkValidAdCaches()?.firstOrNull()?.publisherRevenue?.toDouble() ?: 0.0
                        } catch (e: Exception) { 0.0 }
                        
                        AdLogger.d("[$TAG] ✅ 插页广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)

                        totalLoadSucCount++
                        
                        // 尝试获取加载成功的广告源
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
                        FpuController.onAdFill("IT")

                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@TopOnInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }

                    override fun onInterstitialAdLoadFail(error: AdError?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e(
                            "[$TAG] ❌ 插页广告加载失败, 耗时: %d ms, error: %s",
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
                        synchronized(this@TopOnInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) {
                            continuation.resume(
                                AdResult.Failure(
                                    AdException(
                                        parseErrorCode(error?.code),
                                        error?.desc ?: "加载失败"
                                    )
                                )
                            )
                        }
                    }

                    override fun onInterstitialAdClicked(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 插页广告被点击")
                        totalClickCount++
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

                    override fun onInterstitialAdShow(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 插页广告已展示")
                        cachedEcpm = parseEcpm(info?.ecpmLevel)
                        
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
                                adFormat = "Interstitial"
                            )
                        )
                        IpuController.onAdImpression("IT", ecpmMicros)
                        RpuController.onAdRevenue("IT", ecpmMicros)
                    }

                    override fun onInterstitialAdClose(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 插页广告已关闭")
                        totalCloseCount++
                        reportAdData(
                            "ad_close",
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

                    override fun onInterstitialAdVideoStart(info: TUAdInfo?) {}
                    override fun onInterstitialAdVideoEnd(info: TUAdInfo?) {}
                    override fun onInterstitialAdVideoError(error: AdError?) {}
                })

                ad.load()
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(AdException(0, "加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) loadingDeferred = null
            }
            throw e
        }
    }

    suspend fun showAd(
        activity: Activity,
        onDismiss: (() -> Unit)? = null,
        position: String = ""
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = interstitialAd
        val adUnitId = BuildConfig.TOPON_INTERSTITIAL_ID
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

        AdLogger.d("[$TAG] 准备展示插页广告")

        ad.setAdListener(object : TUInterstitialListener {
            override fun onInterstitialAdLoaded() {}
            override fun onInterstitialAdLoadFail(error: AdError?) {}
            override fun onInterstitialAdClicked(info: TUAdInfo?) {}
            override fun onInterstitialAdShow(info: TUAdInfo?) {}
            override fun onInterstitialAdVideoStart(info: TUAdInfo?) {}
            override fun onInterstitialAdVideoEnd(info: TUAdInfo?) {}
            override fun onInterstitialAdVideoError(error: AdError?) {}

            override fun onInterstitialAdClose(info: TUAdInfo?) {
                AdLogger.d("[$TAG] 插页广告已关闭")
                clearCache()
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })

        ad.show(activity)
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
        val ad = interstitialAd ?: return false
        if (!ad.isAdReady) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        interstitialAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data =
            mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "Interstitial")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }
}
