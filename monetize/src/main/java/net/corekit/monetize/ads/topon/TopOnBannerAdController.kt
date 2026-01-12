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
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * TopOn Banner 广告控制器
 */
class TopOnBannerAdController private constructor() {

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
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "Banner 广告 ID 未配置"))
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
            val adUnitId = BuildConfig.TOPON_BANNER_ID
            val startTime = System.currentTimeMillis()
            
            AdLogger.d("[$TAG] 开始加载 Banner 广告, ID: %s", adUnitId)
            
            val view = TUBannerView(context).apply {  }
            view.setPlacementId(adUnitId)
            bannerView = view
            val displayMetrics = context.resources.displayMetrics
            val adWidth = displayMetrics.widthPixels
            val adHeight = (60 * displayMetrics.density).toInt()
            view.setBannerAdListener(object : TUBannerListener {
                override fun onBannerLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    AdLogger.d("[$TAG] ✅ Banner 广告加载成功, 耗时: %d ms", loadTime)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onBannerFailed(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ Banner 广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Failure(AdException(
                            parseErrorCode(error?.code),
                            error?.desc ?: "加载失败"
                        )))
                    }
                }

                override fun onBannerClicked(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] Banner 广告被点击")
                }

                override fun onBannerShow(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] Banner 广告已展示")
                    cachedEcpm = parseEcpm(info?.ecpmLevel)
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

    private fun getAdSize(context: Context): Map<String, Int>{
        val widthPixels = ScreenUtil.screenWidth()
        val density = Resources.getSystem().displayMetrics.density
        val adWidth = (widthPixels / density).toInt()
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context,adWidth)

        return mutableMapOf<String,Int>().apply {
            this[TUAdConst.KEY.AD_WIDTH] = adWidth
            this[TUAdConst.KEY.AD_HEIGHT] = (60 * density).toInt()
        }
    }

    fun renderToContainer(container: ViewGroup): Boolean {
        val view = bannerView ?: return false
        
        try {
            (view.parent as? ViewGroup)?.removeView(view)
            
            container.removeAllViews()
            container.addView(view, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ))
            AdLogger.d("[$TAG] Banner 广告已渲染到容器")
            return true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 渲染 Banner 广告失败", e)
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
}
