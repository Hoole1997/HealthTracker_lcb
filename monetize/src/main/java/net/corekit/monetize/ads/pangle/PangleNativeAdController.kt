package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest
import com.healthtracker.framework.ext.visible
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.R
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Pangle 原生广告控制器
 */
class PangleNativeAdController private constructor() {

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
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "原生广告 ID 未配置"))
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

    private suspend fun loadAd(context: Context): AdResult<Unit> =
        suspendCancellableCoroutine { continuation ->
            val adUnitId = BuildConfig.PANGLE_NATIVE_ID
            val startTime = System.currentTimeMillis()

            AdLogger.d("[$TAG] 开始加载原生广告, ID: %s", adUnitId)

            PAGNativeAd.loadAd(adUnitId, PAGNativeRequest(), object : PAGNativeAdLoadListener {
                override fun onAdLoaded(ad: PAGNativeAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    cachedAd = ad
                    loadTimestamp = System.currentTimeMillis()
                    cachedEcpm = try {
                        ad.mediaExtraInfo?.get("price")?.toString()?.toDoubleOrNull() ?: 0.0
                    } catch (e: Exception) { 0.0 }

                    AdLogger.d("[$TAG] ✅ 原生广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onError(code: Int, message: String?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ 原生广告加载失败, 耗时: %d ms, code: %d, message: %s", loadTime, code, message)
                    if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(code, message ?: "加载失败")))
                }
            })
        }

    /**
     * 获取缓存的广告用于渲染
     */
    fun getCachedAd(): PAGNativeAd? = cachedAd

    /**
     * 将广告渲染到容器中
     * 使用 PAGViewBinder 模式注册视图交互
     * @param style 可选的布局样式（默认 STANDARD）
     */
    fun renderToContainer(
        context: Context,
        container: ViewGroup,
        style: net.corekit.monetize.ui.NativeAdStyle = net.corekit.monetize.ui.NativeAdStyle.STANDARD
    ): Boolean {
        val ad = cachedAd ?: return false
        val data = ad.nativeAdData ?: return false

        try {
            container.removeAllViews()

            // 1. 使用 Pangle 专用布局
            val layoutResId = style.getPangleLayout()
            val adView = android.view.LayoutInflater.from(context)
                .inflate(layoutResId, container, false) as android.view.ViewGroup

            // 2. 绑定广告数据
            val titleView = adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_tv_title)
            val descView = adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_tv_description)
            val ctaView = adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_btn_cta)
            val iconView = adView.findViewById<android.widget.ImageView>(net.corekit.monetize.R.id.ads_iv_icon)
            val logoContainer = adView.findViewById<FrameLayout>(net.corekit.monetize.R.id.fl_ad_logo)
            val mediaContainer = adView.findViewById<FrameLayout>(net.corekit.monetize.R.id.fl_ad_media)
            
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

            mediaContainer?.let {container ->
                container.removeAllViews()
                data.mediaView?.let {mediaView ->
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
            
            AdLogger.d("[$TAG] Pangle 原生广告渲染成功 (样式: %s)", style.description)
            return true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] Pangle 原生广告渲染失败", e)
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
}
