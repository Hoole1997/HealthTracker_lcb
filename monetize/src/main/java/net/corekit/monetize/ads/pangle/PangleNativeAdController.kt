package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener
import net.corekit.monetize.ads.AdErrorCode
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest
import com.healthtracker.framework.ext.visible
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.R
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.bidding.BiddingAdType
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Pangle 原生广告控制器
 */
class PangleNativeAdController private constructor() {

    // 累积统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pangle_na_load_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("pangle_na_load_suc_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("pangle_na_load_fail_count", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("pangle_na_show_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("pangle_na_show_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("pangle_na_show_fail_count", 0)
    private var totalClickCount by DataStoreIntDelegate("pangle_na_click_count", 0)

    // 当前广告展示位置
    private var currentPosition: String = ""
    // 当前广告源 (默认 Pangle)
    private var currentAdSource: String = "Pangle"

    companion object {
        private const val TAG = "PangleNative"

        @Volatile
        private var instance: PangleNativeAdController? = null

        fun getInstance(): PangleNativeAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleNativeAdController().also { instance = it }
            }
        }
    }

    private var cachedAd: PAGNativeAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasPangleNativeId()) {
            AdLogger.d("[$TAG] 原生广告 ID 未配置，跳过加载")
            return AdResult.Failure(
                AdErrorCode.NATIVE_AD_ID_NOT_CONFIGURED.toAdException()
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
        val adUnitId = BuildConfig.PANGLE_NATIVE_ID
        
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.PANGLE, BiddingAdType.NATIVE)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.PANGLE, BiddingAdType.NATIVE)
            AdLogger.w("[$TAG] 加载跳过 | 平台: Pangle | 类型: Native | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Pangle"
            ))
            return AdResult.Failure(AdErrorCode.AD_LOAD_SKIPPED.toAdException(reason ?: "frequency_limit"))
        }
        
        // 累积加载次数统计
        totalLoadCount++

        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载原生广告, ID: %s", adUnitId)

            PAGNativeAd.loadAd(adUnitId, PAGNativeRequest(), object : PAGNativeAdLoadListener {
                override fun onAdLoaded(ad: PAGNativeAd) {
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
                        "[$TAG] ✅ 原生广告加载成功, 耗时: %d ms, eCPM: %.6f USD",
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
                    FpuController.onAdFill("NA")

                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onError(code: Int, message: String?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "[$TAG] ❌ 原生广告加载失败, 耗时: %d ms, code: %d, message: %s",
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

    /**
     * 获取缓存的广告用于渲染
     */
    fun getCachedAd(): PAGNativeAd? = cachedAd

    /**
     * 将广告渲染到容器中
     * 使用 PAGViewBinder 模式注册视图交互
     * @param position 广告位置标识
     * @param style 可选的布局样式（默认 STANDARD）
     */
    fun renderToContainer(
        context: Context,
        container: ViewGroup,
        style: net.corekit.monetize.ui.NativeAdStyle = net.corekit.monetize.ui.NativeAdStyle.STANDARD,
        position: String = ""
    ): Boolean {
        val ad = cachedAd ?: return false
        val data = ad.nativeAdData ?: return false
        val adUnitId = BuildConfig.PANGLE_NATIVE_ID
        currentPosition = position
        
        // 获取真实广告源 (尝试从 winEcpm 中获取)
        // 注意：如果编译失败，说明 SDK 版本不包含此字段，回退到 "Pangle"
        val adnName = ad.pagRevenueInfo?.winEcpm?.adnName
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

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.PANGLE, BiddingAdType.NATIVE)) {
            totalShowFailCount++
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )
            return false
        }

        try {
            container.removeAllViews()

            // 1. 使用 Pangle 专用布局
            val layoutResId = style.getPangleLayout()
            val adView = android.view.LayoutInflater.from(context)
                .inflate(layoutResId, container, false) as android.view.ViewGroup

            // 2. 绑定广告数据
            val titleView =
                adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_tv_title)
            val descView =
                adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_tv_description)
            val ctaView =
                adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_btn_cta)
            val iconView =
                adView.findViewById<android.widget.ImageView>(net.corekit.monetize.R.id.ads_iv_icon)
            val logoContainer =
                adView.findViewById<FrameLayout>(net.corekit.monetize.R.id.fl_ad_logo)
            val mediaContainer =
                adView.findViewById<FrameLayout>(net.corekit.monetize.R.id.fl_ad_media)

            titleView?.text = data.title ?: "Ad"
            descView?.text = data.description ?: ""
            ctaView?.text = data.buttonText ?: "Install"

            // 3. 使用 Glide 加载图标
            data.icon?.let { icon ->
                com.bumptech.glide.Glide.with(context)
                    .load(icon.imageUrl)
                    .into(iconView)
            }

            // 4. 处理 Pangle Ad Logo（合规要求）
            logoContainer?.let { container ->
                container.removeAllViews()
                data.adLogoView?.let { logoView ->
                    container.addView(logoView)
                    container.visible()
                }
            }

            mediaContainer?.let { container ->
                container.removeAllViews()
                data.mediaView?.let { mediaView ->
                    container.addView(mediaView)
                    container.visible()
                }

            }

            // 5. 添加到容器
            container.addView(adView)

            // 6. 构建 PAGViewBinder（关键步骤！）
            val binder = com.bytedance.sdk.openadsdk.api.nativeAd.PAGViewBinder.Builder(container)
                .titleTextView(titleView)
                .descriptionTextView(descView)
                .iconImageView(iconView)
                .logoViewGroup(logoContainer)  // 修复: 添加 Logo 容器绑定
                .mediaContentViewGroup(mediaContainer)
                .build()

            // 7. 准备可点击视图列表（包含所有可交互元素）
            val clickViews = java.util.ArrayList<android.view.View>().apply {
                titleView?.let { add(it) }
                descView?.let { add(it) }  // 修复: 添加描述区域到点击列表
                ctaView?.let { add(it) }
                iconView?.let { add(it) }
                mediaContainer?.let { add(it) }
            }

            // 8. 注册视图交互
            ad.registerViewForInteraction(
                binder,
                clickViews,
                null
            )

            // 上报展示事件
            totalShowCount++
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

            val adRevenueData = RevenueAdData(
                revenue = RevenueInfo(
                    value = cachedEcpm,
                    currencyCode = "USD"
                ),
                adRevenueNetwork = "Pangle",
                adRevenueUnit = adUnitId,
                adRevenuePlacement = currentPosition,
                adFormat = "Native"
            )
            RevenueAdManager.reportAdRevenue(adRevenueData)

            IpuController.onAdImpression("NA", ecpmMicros)
            RpuController.onAdRevenue("NA", ecpmMicros)

            AdLogger.d("[$TAG] Pangle 原生广告渲染成功 (样式: %s)", style.description)
            return true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] Pangle 原生广告渲染失败", e)
            totalShowFailCount++
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to adUnitId,
                    "position" to currentPosition,
                    "number" to totalShowFailCount,
                    "reason" to (e.message ?: "渲染异常")
                )
            )
        }

        return false
    }

    /**
     * 上报点击事件（供外部调用）
     */
    fun reportClick() {
        totalClickCount++
        AdConfigManager.getNativeConfig().recordClick()
        PlatformFrequencyManager.recordClick(BiddingPlatform.PANGLE, BiddingAdType.NATIVE)
        reportAdData(
            eventName = "ad_click",
            params = mapOf(
                "ad_unit_name" to BuildConfig.PANGLE_NATIVE_ID,
                "position" to currentPosition,
                "number" to totalClickCount,
                "ad_source" to currentAdSource,
                "value" to cachedEcpm,
                "currency" to "USD"
            )
        )
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedEcpm else 0.0

    fun hasValidCache(): Boolean {
        if (cachedAd == null) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        // Pangle PAGNativeAd SDK 无显式 destroy 方法，置空引用让 GC 回收
        cachedAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
        AdLogger.d("[$TAG] Pangle 原生广告缓存已清理")
    }

    /**
     * 通用数据上报函数
     */
    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Pangle",
            "ad_format" to "Native"
        )
        data.putAll(params)

        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }
}
