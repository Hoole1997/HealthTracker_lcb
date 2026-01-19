package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.healthtracker.framework.ext.dp2px
import com.healthtracker.framework.util.ScreenUtil
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativeEventListener
import com.thinkup.nativead.api.TUNativeNetworkListener
import net.corekit.monetize.ads.AdErrorCode
import com.thinkup.nativead.api.NativeAd
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import com.thinkup.core.api.TUAdConst
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * TopOn 原生广告控制器
 */
class TopOnNativeAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("topon_na_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_na_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("topon_na_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("topon_na_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("topon_na_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_na_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("topon_na_click_count", 0)
    private var currentPosition: String = ""
    private var currentAdSource: String = "TopOn"

    companion object {
        private const val TAG = "TopOnNative"

        @Volatile
        private var instance: TopOnNativeAdController? = null

        fun getInstance(): TopOnNativeAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnNativeAdController().also { instance = it }
            }
        }
    }

    private var nativeAd: TUNative? = null
    private var cachedNativeAd: NativeAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L
    private val nativeAdView = ToponNativeAdView()
    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnNativeId()) {
            AdLogger.d("[$TAG] 原生广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.NATIVE_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.TOPON_NATIVE_ID
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载原生广告, ID: %s", adUnitId)

            val ad = TUNative(context, adUnitId, object : TUNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    cachedNativeAd = nativeAd?.nativeAd
                    // 在加载成功后立即获取 eCPM，用于竞价决策
                    cachedEcpm = try {
                        nativeAd?.checkValidAdCaches()?.firstOrNull()?.publisherRevenue?.toDouble()
                            ?: 0.0
                    } catch (e: Exception) {
                        0.0
                    }
                    AdLogger.d(
                        "[$TAG] ✅ 原生广告加载成功, 耗时: %d ms, eCPM: %.6f USD",
                        loadTime,
                        cachedEcpm
                    )
                    totalLoadSucCount++
                    // 尝试获取加载成功的广告源
                    val networkName = nativeAd?.checkValidAdCaches()?.firstOrNull()?.networkName
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
                    FpuController.onAdFill("NA")
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onNativeAdLoadFail(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ 原生广告加载失败, 耗时: %d ms, error: %s",
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
            ad.setLocalExtra(getAdSize(context))
            ad.makeAdRequest()
        }
    }

    fun getCachedNativeAd(): NativeAd? = cachedNativeAd

    private fun getAdSize(context: Context): Map<String, Int> {
        val widthPixels = ScreenUtil.screenWidth()

        return mutableMapOf<String, Int>().apply {
            this[TUAdConst.KEY.AD_WIDTH] = widthPixels
            this[TUAdConst.KEY.AD_HEIGHT] = (widthPixels / 4f).toInt()
        }
    }

    /**
     * 将广告渲染到容器中
     * 支持模板渲染和自渲染两种模式
     * @param style 可选的布局样式（默认 STANDARD）
     */
    fun renderToContainer(
        context: Context,
        container: ViewGroup,
        style: net.corekit.monetize.ui.NativeAdStyle = net.corekit.monetize.ui.NativeAdStyle.STANDARD,
        position: String = ""
    ): Boolean {
        val adUnitId = BuildConfig.TOPON_NATIVE_ID
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

        val nativeAd = cachedNativeAd
        if (nativeAd == null) {
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
            return false
        }

        val activityContext = getActivityContext(context)
        if (activityContext == null) {
            AdLogger.w("[$TAG] 无法获取 Activity Context，尝试使用原始 Context")
        }
        val renderContext = activityContext ?: context

        try {
            container.removeAllViews()

            nativeAd.setNativeEventListener(object : TUNativeEventListener {
                override fun onAdImpressed(view: TUNativeAdView?, info: TUAdInfo?) {
                    AdLogger.d("[$TAG] TopOn 原生广告已展示")
                    cachedEcpm = parseEcpm(info?.ecpmLevel)
                    
                    // 获取展示的广告源
                    currentAdSource = info?.networkName ?: "TopOn"
                    
                    totalShowCount++
                    val ecpmMicros = (cachedEcpm * 1_000_000).toLong()
                    reportAdData(
                        "ad_impression",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
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
                            adRevenuePlacement = position,
                            adFormat = "Native"
                        )
                    )
                    IpuController.onAdImpression("NA", ecpmMicros)
                    RpuController.onAdRevenue("NA", ecpmMicros)
                }

                override fun onAdClicked(view: TUNativeAdView?, info: TUAdInfo?) {
                    AdLogger.d("[$TAG] TopOn 原生广告被点击")
                    totalClickCount++
                    reportAdData(
                        "ad_click",
                        mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalClickCount,
                            "ad_source" to currentAdSource,
                            "value" to cachedEcpm,
                            "currency" to "USD"
                        )
                    )
                }

                override fun onAdVideoStart(view: TUNativeAdView?) {}
                override fun onAdVideoEnd(view: TUNativeAdView?) {}
                override fun onAdVideoProgress(view: TUNativeAdView?, progress: Int) {}
            })

            AdLogger.d("[$TAG] 使用自渲染 (样式: %s)", style.description)

            nativeAdView.bindNativeAdToContainer(context, container, nativeAd, style)
            AdLogger.d("[$TAG] TopOn 原生广告渲染成功")
            return true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] TopOn 原生广告渲染失败", e)
            totalShowFailCount++
            reportAdData(
                "ad_show_fail",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to (e.message ?: "render_exception")
                )
            )
        }

        return false
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
        if (cachedNativeAd == null) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        // TopOn NativeAd SDK 无显式 destroy 方法，置空引用让 GC 回收
        nativeAd = null
        cachedNativeAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
        AdLogger.d("[$TAG] 原生广告缓存已清理")
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "Native")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }

    /**
     * 从 Context 中获取 Activity
     * Pangle 等广告平台要求使用 Activity Context 创建广告视图
     */
    private fun getActivityContext(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return ctx
            }
            ctx = ctx.baseContext
        }
        return null
    }
}
