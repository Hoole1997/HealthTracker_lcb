package net.corekit.monetize.ads.topon

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.NativeAd
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdErrorCode
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.interceptor.GlobalAdSwitchInterceptor
import net.corekit.monetize.ads.interceptor.InterceptorChain
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * TopOn 全屏原生广告控制器
 */
class TopOnFullScreenNativeAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("topon_fn_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_fn_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("topon_fn_load_fail_count", 0)

    companion object {
        private const val TAG = "TopOnFullNative"

        @Volatile
        private var instance: TopOnFullScreenNativeAdController? = null

        fun getInstance(): TopOnFullScreenNativeAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnFullScreenNativeAdController().also { instance = it }
            }
        }
    }

    private var nativeAd: TUNative? = null
    private var cachedNativeAd: NativeAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnFullNativeId()) {
            AdLogger.d("[$TAG] 全屏原生广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.FULL_NATIVE_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.TOPON_FULL_NATIVE_ID
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载全屏原生广告, ID: %s", adUnitId)

            val ad = TUNative(context, adUnitId, object : TUNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    cachedNativeAd = nativeAd?.nativeAd
                    AdLogger.d("[$TAG] ✅ 全屏原生广告加载成功, 耗时: %d ms", loadTime)
                    totalLoadSucCount++
                    
                    // 尝试获取加载成功的广告源
                    // 修复：此时 ad 变量在对象构造中不可用，使用 nativeAd 属性
                    val validAdCache = nativeAd?.checkValidAdCaches()?.firstOrNull()
                    val networkName = validAdCache?.networkName
                    val loadedSource = if (networkName.isNullOrEmpty()) "TopOn" else networkName
                    
                    // 获取并缓存 eCPM
                    // TopOn ecpm is Double in recent SDKs or we assume it is correct type matching the field in SDK
                    val ecpmValue = validAdCache?.ecpm
                    cachedEcpm = try {
                         ecpmValue?.toDouble() ?: 0.0
                    } catch (e: Exception) {
                         // compatible if it's string
                         ecpmValue.toString().toDoubleOrNull() ?: 0.0
                    }
                    AdLogger.d("[$TAG] TopOn eCPM: %.6f USD", cachedEcpm)

                    reportAdData(
                        "ad_loaded",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to loadedSource,
                            "pass_time" to ceil(loadTime / 1000.0).toInt()
                        )
                    )
                    FpuController.onAdFill("FN")
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onNativeAdLoadFail(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ 全屏原生广告加载失败, 耗时: %d ms, error: %s",
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
                                parseErrorCode(error?.code),
                                error?.desc ?: "加载失败"
                            )
                        )
                    )
                }
            })

            nativeAd = ad
            ad.makeAdRequest()
        }
    }

    fun getCachedNativeAd(): NativeAd? = cachedNativeAd

    private fun parseErrorCode(code: String?): Int {
        return code?.toIntOrNull() ?: AdException.ERROR_INTERNAL
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedEcpm else 0.0

    fun hasValidCache(): Boolean {
        if (cachedNativeAd == null) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        // TopOn NativeAd SDK 无显式 destroy 方法，置空引用让 GC 回收
        nativeAd = null
        cachedNativeAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
        AdLogger.d("[$TAG] 全屏原生广告缓存已清理")
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "FullNative")
        data.putAll(params)
        ReportDataManager.reportData(eventName, data)
    }

    private var totalShowTriggerCount by DataStoreIntDelegate("topon_fn_show_trigger_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_fn_show_fail_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("topon_fn_close_count", 0)
    private var isShowing = false
    private val nativeAdView = ToponFullScreenNativeAdView()

    private val interceptorChain = InterceptorChain(
        listOf(GlobalAdSwitchInterceptor())
    )

    fun closeEvent(
        adUnitId: String = "",
        adSource: String? = "TopOn",
        valueUsd: Double? = null,
        currencyCode: String? = null
    ) {
        isShowing = false
        totalCloseCount++
        val params: Map<String, Any> = mapOf(
            "ad_unit_name" to adUnitId,
            "position" to "",
            "number" to totalCloseCount,
            "ad_source" to (adSource ?: "TopOn"),
            "value" to (valueUsd ?: 0.0),
            "currency" to (currencyCode ?: "USD")
        )
        reportAdData(
            eventName = "ad_close",
            params = params
        )
    }

    suspend fun showAdInContainer(
        context: Context,
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        adUnitId: String? = null
    ): AdResult<Unit> {
        val finalAdUnitId = adUnitId ?: BuildConfig.TOPON_FULL_NATIVE_ID

        totalShowTriggerCount++
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to "",
                "number" to totalShowTriggerCount
            )
        )

        when (val interceptResult = interceptorChain.intercept(context, AdConfigManager.getFullscreenNativeConfig())) {
            is AdResult.Failure -> {
                totalShowFailCount++
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to "",
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message.orEmpty()
                    )
                )
                return AdResult.Failure(interceptResult.error)
            }
            else -> Unit
        }

        return try {
            val nativeAd = cachedNativeAd
            if (nativeAd == null || !hasValidCache()) {
                totalShowFailCount++
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to "",
                        "number" to totalShowFailCount,
                        "reason" to "no_valid_cache"
                    )
                )
                return AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "TopOn 全屏原生广告无可用缓存"))
            }

            val bindSuccess = nativeAdView.bindFullScreenNativeAdToContainer(
                context = context,
                container = container,
                nativeAd = nativeAd,
                lifecycleOwner = lifecycleOwner
            )

            if (bindSuccess) {
                isShowing = true
                clearCache()
                AdResult.Success(Unit)
            } else {
                totalShowFailCount++
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to "",
                        "number" to totalShowFailCount,
                        "reason" to "bind_failed"
                    )
                )
                AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "TopOn 全屏原生广告绑定失败"))
            }
        } catch (e: Exception) {
            totalShowFailCount++
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to "",
                    "number" to totalShowFailCount,
                    "reason" to (e.message ?: "unknown")
                )
            )
            AdResult.Failure(AdException(AdException.ERROR_INTERNAL, e.message ?: "展示异常"))
        }
    }
}
