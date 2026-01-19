package net.corekit.monetize.ads.pangle

import android.content.Context
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Pangle 全屏原生广告控制器
 */
class PangleFullScreenNativeAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("pangle_fn_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_fn_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("pangle_fn_load_fail_count", 0)

    companion object {
        private const val TAG = "PangleFullNative"

        @Volatile
        private var instance: PangleFullScreenNativeAdController? = null

        fun getInstance(): PangleFullScreenNativeAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleFullScreenNativeAdController().also { instance = it }
            }
        }
    }

    private var cachedAd: PAGNativeAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasPangleFullNativeId()) {
            AdLogger.d("[$TAG] 全屏原生广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdException(
                    AdException.ERROR_INVALID_REQUEST,
                    "全屏原生广告 ID 未配置"
                )
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
        totalLoadCount++
        val adUnitId = BuildConfig.PANGLE_FULL_NATIVE_ID
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载全屏原生广告, ID: %s", adUnitId)

            PAGNativeAd.loadAd(adUnitId, PAGNativeRequest(), object : PAGNativeAdLoadListener {
                override fun onAdLoaded(ad: PAGNativeAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    cachedAd = ad
                    loadTimestamp = System.currentTimeMillis()
                    cachedEcpm = try {
                        ad.mediaExtraInfo?.get("price")?.toString()?.toDoubleOrNull() ?: 0.0
                    } catch (e: Exception) {
                        0.0
                    }

                    AdLogger.d("[$TAG] ✅ 全屏原生广告加载成功, 耗时: %d ms, ecpm: %.6f", loadTime, cachedEcpm)

                    val adnName = ad.pagRevenueInfo?.winEcpm?.adnName
                    val loadedSource = if (adnName.isNullOrEmpty()) "Pangle" else adnName

                    totalLoadSucCount++
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

                override fun onError(code: Int, message: String?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ 全屏原生广告加载失败, 耗时: %d ms, code: %d, message: %s",
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

    fun getCachedAd(): PAGNativeAd? = cachedAd

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

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "Pangle", "ad_format" to "FullNative")
        data.putAll(params)
        ReportDataManager.reportData(eventName, data)
    }
}
