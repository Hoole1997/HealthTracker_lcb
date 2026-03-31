package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionCallback
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize
import com.bytedance.sdk.openadsdk.api.model.PAGErrorModel
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

    // 单次展示状态
    private var currentPosition: String = ""
    private var currentAdSource: String = "Pangle"
    private var currentEcpmValue: Double = 0.0
    private var currentCurrency: String = "USD"
    private var totalClickCount = 0
    private var totalCloseCount = 0
    private var currentContainer: ViewGroup? = null

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
        val adUnitId = BuildConfig.PANGLE_BANNER_ID
        
        // 频控前置检查
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(
            BiddingPlatform.PANGLE, 
            BiddingAdType.BANNER
        )
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(
                BiddingPlatform.PANGLE, 
                BiddingAdType.BANNER
            )
            AdLogger.w("[$TAG] 加载跳过 | 平台: Pangle | 类型: Banner | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Pangle"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        totalLoadCount++
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
                            AdException(code, message ?: "加载失败")
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
        currentContainer = container

        val adnName = ad.pagRevenueInfo?.winEcpm?.adnName
        currentAdSource = if (adnName.isNullOrEmpty()) "Pangle" else adnName

        totalShowTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to adUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(
                BiddingPlatform.PANGLE,
                BiddingAdType.BANNER
            )
        ) {
            totalShowFailCount++
            reportAdData(
                "ad_show_error", mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )
            return false
        }

        try {
            val bannerView = ad.bannerView
            if (bannerView != null) {
                ad.setAdInteractionListener(object : PAGBannerAdInteractionCallback() {
                    override fun onAdShowed() {
                        AdLogger.logD(TAG, "广告展示 | 位置: %s", currentPosition)
                        
                        val showEcpm = ad.pagRevenueInfo?.showEcpm
                        currentEcpmValue = showEcpm?.revenue?.toDoubleOrNull() ?: 0.0
                        currentCurrency = showEcpm?.currency ?: "USD"
                        currentAdSource = showEcpm?.adnName?.takeIf { it.isNotEmpty() } ?: "Pangle"
                        
                        totalShowCount++
                        AdConfigManager.getBannerConfig().recordShow()
                        
                        val ecpmMicros = (currentEcpmValue * 1_000_000).toLong()
                        
                        reportAdData(
                            eventName = "ad_impression",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalShowCount,
                                "ad_source" to currentAdSource,
                                "value" to currentEcpmValue,
                                "currency" to currentCurrency
                            )
                        )
                        
                        val adRevenueData = RevenueAdData(
                            revenue = RevenueInfo(
                                value = currentEcpmValue,
                                currencyCode = currentCurrency
                            ),
                            adRevenueNetwork = currentAdSource,
                            adRevenueUnit = adUnitId,
                            adRevenuePlacement = currentPosition,
                            adFormat = "Banner"
                        )
                        RevenueAdManager.reportAdRevenue(adRevenueData)
                        
                        IpuController.onAdImpression("BA", ecpmMicros)
                        RpuController.onAdRevenue("BA", ecpmMicros)
                    }

                    override fun onAdClicked() {
                        totalClickCount++
                        AdLogger.logD(TAG, "用户点击 | 位置: %s | 累计点击: %d", currentPosition, totalClickCount)
                        
                        AdConfigManager.getBannerConfig().recordClick()
                        PlatformFrequencyManager.recordClick(
                            BiddingPlatform.PANGLE, 
                            BiddingAdType.BANNER
                        )
                        
                        reportAdData(
                            eventName = "ad_click",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalClickCount,
                                "ad_source" to currentAdSource,
                                "value" to currentEcpmValue,
                                "currency" to currentCurrency
                            )
                        )
                        
                        if (PlatformFrequencyManager.isClickLimitReached(
                            BiddingPlatform.PANGLE, 
                            BiddingAdType.BANNER
                        )) {
                            AdLogger.logW(TAG, "点击达到配额上限，移除正在展示的广告 | 位置: %s", currentPosition)
                            removeCurrentAd()
                        }
                    }

                    override fun onAdDismissed() {
                        totalCloseCount++
                        AdLogger.logD(TAG, "广告关闭 | 位置: %s | 累计关闭: %d", currentPosition, totalCloseCount)
                        
                        reportAdData(
                            eventName = "ad_dismiss",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalCloseCount,
                                "ad_source" to currentAdSource,
                                "value" to currentEcpmValue,
                                "currency" to currentCurrency
                            )
                        )
                    }

                    override fun onAdShowFailed(error: PAGErrorModel) {
                        totalShowFailCount++
                        AdLogger.logE(TAG, "广告展示失败 | 位置: %s | code: %d | message: %s", 
                            currentPosition, error.errorCode, error.errorMessage)
                        
                        reportAdData(
                            eventName = "ad_show_error",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "position" to currentPosition,
                                "number" to totalShowFailCount,
                                "reason" to (error.errorMessage ?: "code=${error.errorCode}"),
                                "ad_source" to currentAdSource
                            )
                        )
                    }
                })

                container.removeAllViews()
                container.addView(
                    bannerView, FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    )
                )

                AdLogger.d("[$TAG] Banner 广告渲染成功 | 位置: %s", currentPosition)
                return true
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 渲染 Banner 广告失败", e)
            totalShowFailCount++
            reportAdData(
                "ad_show_error", mapOf(
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
        destroyAd()
        cachedAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
        AdLogger.d("[$TAG] Pangle Banner 广告缓存已清理")
    }

    /**
     * 移除当前展示的广告
     */
    fun removeCurrentAd() {
        try {
            currentContainer?.let { container ->
                container.removeAllViews()
                AdLogger.logD(TAG, "已移除广告视图 | 位置: %s", currentPosition)
            }
            currentContainer = null
            destroyAd()
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 移除广告视图失败", e)
        }
    }

    /**
     * 显式销毁广告对象，释放 SDK 内部资源
     */
    fun destroyAd() {
        try {
            cachedAd?.destroy()
            cachedAd = null
            AdLogger.d("[$TAG] 广告对象已显式销毁")
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 销毁广告对象失败", e)
        }
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
