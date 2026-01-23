package net.corekit.monetize.ads.topon

import android.content.Context
import android.content.res.Resources
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.healthtracker.framework.util.ScreenUtil
import com.thinkup.banner.api.TUBannerListener
import com.thinkup.banner.api.TUBannerView
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
 * TopOn Banner 广告控制器
 */
class TopOnBannerAdController private constructor() {

    private var totalLoadCount by DataStoreIntDelegate("topon_ba_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_ba_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("topon_ba_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("topon_ba_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("topon_ba_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_ba_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("topon_ba_click_count", 0)
    private var currentPosition: String = ""
    private var currentAdSource: String = "TopOn"

    companion object {
        private const val TAG = "TopOnBanner"

        @Volatile
        private var instance: TopOnBannerAdController? = null

        fun getInstance(): TopOnBannerAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnBannerAdController().also { instance = it }
            }
        }
    }

    private var bannerView: TUBannerView? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 30 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnBannerId()) {
            AdLogger.d("[$TAG] Banner 广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.BANNER_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.TOPON_BANNER_ID
        
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.TOPON, BiddingAdType.BANNER)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.TOPON, BiddingAdType.BANNER)
            AdLogger.w("[$TAG] 加载跳过 | 平台: TopOn | 类型: Banner | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "TopOn"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        totalLoadCount++
        reportAdData("ad_start_load", mapOf("ad_unit_name" to adUnitId, "number" to totalLoadCount))

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载 Banner 广告, ID: %s", adUnitId)

            val view = TUBannerView(context).apply { }
            view.setPlacementId(adUnitId)
            bannerView = view

            view.setBannerAdListener(object : TUBannerListener {
                override fun onBannerLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    
                    // 尝试使用 checkValidAdCaches 获取 eCPM
                    cachedEcpm = try {
                        view.checkValidAdCaches()?.firstOrNull()?.publisherRevenue?.toDouble() ?: 0.0
                    } catch (e: Exception) { 0.0 }
                    
                    AdLogger.d("[$TAG] ✅ Banner 广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)
                    totalLoadSucCount++
                    // 尝试获取加载成功的广告源
                    val networkName = view.checkValidAdCaches()?.firstOrNull()?.networkName
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
                    FpuController.onAdFill("BA")
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onBannerFailed(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ Banner 广告加载失败, 耗时: %d ms, error: %s",
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

                override fun onBannerClicked(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] Banner 广告被点击")
                    totalClickCount++
                    AdConfigManager.getBannerConfig().recordClick()
                    PlatformFrequencyManager.recordClick(BiddingPlatform.TOPON, BiddingAdType.BANNER)
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

                override fun onBannerShow(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] Banner 广告已展示")
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
                            adFormat = "Banner"
                        )
                    )
                    IpuController.onAdImpression("BA", ecpmMicros)
                    RpuController.onAdRevenue("BA", ecpmMicros)
                }

                override fun onBannerClose(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] Banner 广告已关闭")
                }

                override fun onBannerAutoRefreshed(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] Banner 广告自动刷新")
                }

                override fun onBannerAutoRefreshFail(error: AdError?) {
                    AdLogger.w("[$TAG] Banner 广告自动刷新失败: %s", error?.fullErrorInfo)
                }
            })
            view.setLocalExtra(getAdSize(context))
            view.loadAd()
        }
    }

    private fun getAdSize(context: Context): Map<String, Int> {
        val widthPixels = ScreenUtil.screenWidth()
        val density = Resources.getSystem().displayMetrics.density
        val adWidth = (widthPixels / density).toInt()
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)

        return mutableMapOf<String, Int>().apply {
            this[TUAdConst.KEY.AD_WIDTH] = adWidth
            this[TUAdConst.KEY.AD_HEIGHT] = (60 * density).toInt()
        }
    }

    fun renderToContainer(container: ViewGroup, position: String = ""): Boolean {
        val adUnitId = BuildConfig.TOPON_BANNER_ID
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

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.TOPON, BiddingAdType.BANNER)) {
            totalShowFailCount++
            reportAdData(
                "ad_show_fail",
                mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )
            return false
        }

        val view = bannerView
        if (view == null) {
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

        try {
            (view.parent as? ViewGroup)?.removeView(view)

            container.removeAllViews()
            container.addView(
                view, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
            AdLogger.d("[$TAG] Banner 广告已渲染到容器")
            return true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 渲染 Banner 广告失败", e)
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
        if (bannerView == null) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        bannerView?.destroy()
        bannerView = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>("ad_platform" to "TopOn", "ad_format" to "Banner")
        data.putAll(params)
        if (eventName == "ad_impression") ReportDataManager.reportDataByName(
            "ThinkingData",
            eventName,
            data
        ) else ReportDataManager.reportData(eventName, data)
    }
}
