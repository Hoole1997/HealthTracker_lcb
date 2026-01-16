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
import com.thinkup.nativead.api.NativeAd
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import com.thinkup.core.api.TUAdConst
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
    private val nativeAdView = ToponNativeAdView()
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
            ad.setLocalExtra(getAdSize(context))
            ad.makeAdRequest()
        }

    fun getCachedNativeAd(): NativeAd? = cachedNativeAd

    private fun getAdSize(context: Context): Map<String, Int>{
        val widthPixels = ScreenUtil.screenWidth()

        return mutableMapOf<String,Int>().apply {
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
        style: net.corekit.monetize.ui.NativeAdStyle = net.corekit.monetize.ui.NativeAdStyle.STANDARD
    ): Boolean {
        val nativeAd = cachedNativeAd ?: return false
        
        // 修复: 确保使用 Activity Context (Pangle 等平台要求)
        val activityContext = getActivityContext(context)
        if (activityContext == null) {
            AdLogger.w("[$TAG] 无法获取 Activity Context，尝试使用原始 Context")
        }
        val renderContext = activityContext ?: context
        
        try {
            container.removeAllViews()
            
            // 1. 创建 TUNativeAdView (使用 Activity Context)
//            val nativeAdView = TUNativeAdView(renderContext)
            
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
            
            // 3. 始终使用自渲染（避免模板渲染高度不可控问题）
            AdLogger.d("[$TAG] 使用自渲染 (样式: %s)", style.description)

            nativeAdView.bindNativeAdToContainer(context, container, nativeAd, style)
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
