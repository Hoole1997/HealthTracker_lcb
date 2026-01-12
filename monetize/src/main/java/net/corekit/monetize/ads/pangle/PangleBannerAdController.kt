package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Pangle Banner 广告控制器
 */
class PangleBannerAdController private constructor() {

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
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "Banner 广告 ID 未配置"))
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
            val adUnitId = BuildConfig.PANGLE_BANNER_ID
            val startTime = System.currentTimeMillis()
            
            AdLogger.d("[$TAG] 开始加载 Banner 广告, ID: %s", adUnitId)
            
            val request = PAGBannerRequest(PAGBannerSize.BANNER_W_320_H_50)
            
            PAGBannerAd.loadAd(adUnitId, request, object : PAGBannerAdLoadListener {
                override fun onAdLoaded(ad: PAGBannerAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    cachedAd = ad
                    loadTimestamp = System.currentTimeMillis()
                    cachedEcpm = try {
                        ad.mediaExtraInfo?.get("price")?.toString()?.toDoubleOrNull() ?: 0.0
                    } catch (e: Exception) { 0.0 }
                    
                    AdLogger.d("[$TAG] ✅ Banner 广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onError(code: Int, message: String?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ Banner 广告加载失败, 耗时: %d ms, code: %d, message: %s", loadTime, code, message)
                    if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(code, message ?: "加载失败")))
                }
            })
        }

    fun renderToContainer(container: ViewGroup): Boolean {
        val ad = cachedAd ?: return false
        
        try {
            val bannerView = ad.bannerView
            if (bannerView != null) {
                container.removeAllViews()
                container.addView(bannerView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ))
                AdLogger.d("[$TAG] Banner 广告已渲染到容器")
                return true
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 渲染 Banner 广告失败", e)
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
