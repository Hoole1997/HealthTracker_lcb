package net.corekit.monetize.ads.topon

import android.content.Context
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.NativeAd
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
 * TopOn 全屏原生广告控制器
 */
class TopOnFullScreenNativeAdController private constructor() {

    companion object {
        private const val TAG = "TopOnFullNative"
        
        @Volatile
        private var instance: TopOnFullScreenNativeAdController? = null
        
        fun getInstance(): TopOnFullScreenNativeAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnFullScreenNativeAdController().also { instance = it }
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
        if (!AdIdHelper.hasTopOnFullNativeId()) {
            AdLogger.d("[$TAG] 全屏原生广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "全屏原生广告 ID 未配置"))
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
            val adUnitId = BuildConfig.TOPON_FULL_NATIVE_ID
            val startTime = System.currentTimeMillis()
            
            AdLogger.d("[$TAG] 开始加载全屏原生广告, ID: %s", adUnitId)
            
            val ad = TUNative(context, adUnitId, object : TUNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    cachedNativeAd = nativeAd?.nativeAd
                    AdLogger.d("[$TAG] ✅ 全屏原生广告加载成功, 耗时: %d ms", loadTime)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onNativeAdLoadFail(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ 全屏原生广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)
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

    private fun parseErrorCode(code: String?): Int {
        return code?.toIntOrNull() ?: AdException.ERROR_INTERNAL
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
