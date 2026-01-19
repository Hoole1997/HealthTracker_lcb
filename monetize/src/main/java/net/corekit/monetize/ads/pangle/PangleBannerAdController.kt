package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize
import kotlinx.coroutines.suspendCancellableCoroutine
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
 * Pangle Banner 广告控制器
 */
class PangleBannerAdController private constructor() {

    // 累积统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pangle_ba_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_ba_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("pangle_ba_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("pangle_ba_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("pangle_ba_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("pangle_ba_show_fail_count", 0)

    private var currentPosition: String = ""

    companion object {
        private const val TAG = "PangleBanner"

        @Volatile
        private var instance: PangleBannerAdController? = null

        fun getInstance(): PangleBannerAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleBannerAdController().also { instance = it }
            }
        }
    }

    private var cachedAd: PAGBannerAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 30 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasPangleBannerId()) {
            AdLogger.d("[$TAG] Banner 广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.BANNER_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.PANGLE_BANNER_ID

        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载 Banner 广告, ID: %s", adUnitId)

            val request = PAGBannerRequest(PAGBannerSize.BANNER_W_320_H_50)

            PAGBannerAd.loadAd(adUnitId, request, object : PAGBannerAdLoadListener {
                override fun onAdLoaded(ad: PAGBannerAd) {
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
                        "[$TAG] ✅ Banner 广告加载成功, 耗时: %d ms, eCPM: %.6f USD",
                        loadTime,
                        cachedEcpm
                    )

                    totalLoadSucCount++
                    reportAdData(
                        "ad_loaded", mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to "Pangle",
                            "pass_time" to ceil(loadTime / 1000.0).toInt()
                        )
                    )
                    FpuController.onAdFill("BA")

                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onError(code: Int, message: String?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ Banner 广告加载失败, 耗时: %d ms, code: %d, message: %s",
                        loadTime,
                        code,
                        message
                    )

                    totalLoadFailCount++
                    reportAdData(
                        "ad_load_fail", mapOf(
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

    fun renderToContainer(container: ViewGroup, position: String = ""): Boolean {
        val ad = cachedAd ?: return false
        val adUnitId = BuildConfig.PANGLE_BANNER_ID
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

        try {
            val bannerView = ad.bannerView
            if (bannerView != null) {
                container.removeAllViews()
                container.addView(
                    bannerView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                totalShowCount++
                val ecpmMicros = (cachedEcpm * 1_000_000).toLong()

                reportAdData(
                    "ad_impression", mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to currentPosition,
                        "number" to totalShowCount,
                        "ad_source" to "Pangle",
                        "value" to cachedEcpm,
                        "currency" to "USD"
                    )
                )

                RevenueAdManager.reportAdRevenue(
                    RevenueAdData(
                        revenue = RevenueInfo(value = cachedEcpm, currencyCode = "USD"),
                        adRevenueNetwork = "Pangle",
                        adRevenueUnit = adUnitId,
                        adRevenuePlacement = currentPosition,
                        adFormat = "Banner"
                    )
                )

                IpuController.onAdImpression("BA", ecpmMicros)
                RpuController.onAdRevenue("BA", ecpmMicros)

                AdLogger.d("[$TAG] Banner 广告已渲染到容器")
                return true
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 渲染 Banner 广告失败", e)
            totalShowFailCount++
            reportAdData(
                "ad_show_fail", mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalShowFailCount,
                    "reason" to (e.message ?: "渲染异常")
                )
            )
        }

        return false
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

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "Pangle", "ad_format" to "Banner")
        data.putAll(params)
        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }
}
