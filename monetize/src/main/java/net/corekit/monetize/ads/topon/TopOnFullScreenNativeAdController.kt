package net.corekit.monetize.ads.topon

import android.content.Context
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.NativeAd
import com.thinkup.core.api.AdError
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
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
                AdException(
                    AdException.ERROR_INVALID_REQUEST,
                    "全屏原生广告 ID 未配置"
                )
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
                    reportAdData(
                        "ad_loaded",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to "TopOn",
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
        nativeAd = null
        cachedNativeAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "FullNative")
        data.putAll(params)
        ReportDataManager.reportData(eventName, data)
    }
}
