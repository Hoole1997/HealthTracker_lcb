package net.corekit.monetize.ads.topon

import android.content.Context
import android.view.ViewGroup
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativeEventListener
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.NativeAd
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * TopOn 原生广告控制器
 */
class TopOnNativeAdController private constructor() {

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

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnNativeId()) {
            AdLogger.d("[$TAG] 原生广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "原生广告 ID 未配置"))
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

    private suspend fun loadAd(context: Context): AdResult<Unit> = 
        suspendCancellableCoroutine { continuation ->
            val adUnitId = BuildConfig.TOPON_NATIVE_ID
            val startTime = System.currentTimeMillis()
            
            AdLogger.d("[$TAG] 开始加载原生广告, ID: %s", adUnitId)
            
            val ad = TUNative(context, adUnitId, object : TUNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    cachedNativeAd = nativeAd?.nativeAd
                    AdLogger.d("[$TAG] ✅ 原生广告加载成功, 耗时: %d ms", loadTime)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onNativeAdLoadFail(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ 原生广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Failure(AdException(
                            parseErrorCode(error?.code),
                            error?.desc ?: "加载失败"
                        )))
                    }
                }
            })
            
            nativeAd = ad
            ad.makeAdRequest()
        }

    fun getCachedNativeAd(): NativeAd? = cachedNativeAd

    /**
     * 将广告渲染到容器中
     * 支持模板渲染和自渲染两种模式
     * @param style 可选的布局样式（默认 STANDARD）
     */
    fun renderToContainer(
        context: Context, 
        container: ViewGroup,
        style: net.corekit.monetize.ui.NativeAdStyle = net.corekit.monetize.ui.NativeAdStyle.STANDARD
    ): Boolean {
        val nativeAd = cachedNativeAd ?: return false
        
        try {
            container.removeAllViews()
            
            // 1. 创建 TUNativeAdView
            val nativeAdView = TUNativeAdView(context)
            
            // 2. 设置事件监听
            nativeAd.setNativeEventListener(object : TUNativeEventListener {
                override fun onAdImpressed(view: TUNativeAdView?, info: TUAdInfo?) {
                    AdLogger.d("[$TAG] TopOn 原生广告已展示")
                    cachedEcpm = parseEcpm(info?.ecpmLevel)
                }

                override fun onAdClicked(view: TUNativeAdView?, info: TUAdInfo?) {
                    AdLogger.d("[$TAG] TopOn 原生广告被点击")
                }

                override fun onAdVideoStart(view: TUNativeAdView?) {}
                override fun onAdVideoEnd(view: TUNativeAdView?) {}
                override fun onAdVideoProgress(view: TUNativeAdView?, progress: Int) {}
            })
            
            // 3. 判断渲染模式
            if (nativeAd.isNativeExpress) {
                // 模板渲染
                AdLogger.d("[$TAG] 使用模板渲染 (样式: %s)", style.description)
                nativeAd.renderAdContainer(nativeAdView, null)
                nativeAd.prepare(nativeAdView, null)
            } else {
                // 自渲染
                AdLogger.d("[$TAG] 使用自渲染 (样式: %s)", style.description)
                
                // 使用 TopOn 专用布局
                val layoutResId = style.getTopOnLayout()
                val adView = android.view.LayoutInflater.from(context)
                    .inflate(layoutResId, null) as android.view.ViewGroup
                
                // 获取素材
                val material = nativeAd.adMaterial
                
                // 绑定数据到自定义布局
                val titleView = adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_tv_title)
                val descView = adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_tv_description)
                val ctaView = adView.findViewById<android.widget.TextView>(net.corekit.monetize.R.id.ads_btn_cta)
                val iconView = adView.findViewById<android.widget.ImageView>(net.corekit.monetize.R.id.ads_iv_icon)
                
                titleView?.text = material?.title ?: "Ad"
                descView?.text = material?.descriptionText ?: ""
                ctaView?.text = material?.callToActionText ?: "Install"
                
                // 使用 Glide 加载图标
                material?.iconImageUrl?.let { iconUrl ->
                    com.bumptech.glide.Glide.with(context)
                        .load(iconUrl)
                        .into(iconView)
                }
                
                // 创建 TUNativePrepareInfo
                val prepareInfo = com.thinkup.nativead.api.TUNativePrepareInfo()
                titleView?.let { prepareInfo.titleView = it }
                descView?.let { prepareInfo.descView = it }
                ctaView?.let { prepareInfo.ctaView = it }
                iconView?.let { prepareInfo.iconView = it }
                
                // 添加可点击视图
                val clickViews = mutableListOf<android.view.View>()
                titleView?.let { clickViews.add(it) }
                ctaView?.let { clickViews.add(it) }
                iconView?.let { clickViews.add(it) }
                prepareInfo.clickViewList = clickViews
                
                // 渲染
                nativeAd.renderAdContainer(nativeAdView, adView)
                nativeAd.prepare(nativeAdView, prepareInfo)
            }
            
            container.addView(nativeAdView)
            AdLogger.d("[$TAG] TopOn 原生广告渲染成功")
            return true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] TopOn 原生广告渲染失败", e)
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
        nativeAd = null
        cachedNativeAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }
}
