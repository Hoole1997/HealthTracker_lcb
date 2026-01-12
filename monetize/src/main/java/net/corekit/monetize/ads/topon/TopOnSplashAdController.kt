package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.thinkup.splashad.api.TUSplashAd
import com.thinkup.splashad.api.TUSplashAdExtraInfo
import com.thinkup.splashad.api.TUSplashAdListener
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * TopOn 开屏广告控制器
 */
class TopOnSplashAdController private constructor() {

    companion object {
        private const val TAG = "TopOnSplash"
        
        @Volatile
        private var instance: TopOnSplashAdController? = null
        
        fun getInstance(): TopOnSplashAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnSplashAdController().also { instance = it }
            }
        }
    }

    private var splashAd: TUSplashAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 4 * 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnSplashId()) {
            AdLogger.d("[$TAG] 开屏广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "开屏广告 ID 未配置"))
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

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
     * @param timeoutMillis 超时时间（毫秒）
     * @return 广告加载结果
     */
    suspend fun waitForAd(timeoutMillis: Long): AdResult<Unit> {
        val deferred = synchronized(this) {
            // 如果已有缓存，直接返回成功
            if (hasValidCache()) {
                return@synchronized CompletableDeferred(AdResult.Success(Unit))
            }
            // 如果正在加载，返回当前的 deferred
            loadingDeferred
        }

        if (deferred == null) {
            return AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "没有正在进行的加载请求且无缓存"))
        }

        return try {
            withTimeoutOrNull(timeoutMillis) {
                deferred.await()
            } ?: AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "等待广告加载超时"))
        } catch (e: Exception) {
            AdResult.Failure(createAdException("等待被中断", e))
        }
    }

    private suspend fun loadAd(context: Context): AdResult<Unit> {
        // 创建新的 deferred
        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val adUnitId = BuildConfig.TOPON_SPLASH_ID
                val startTime = System.currentTimeMillis()
                
                AdLogger.d("[$TAG] 开始加载开屏广告, ID: %s", adUnitId)
                
                val ad = TUSplashAd(context, adUnitId, object : TUSplashAdListener {
                    override fun onAdLoaded(isTimeout: Boolean) {
                        val loadTime = System.currentTimeMillis() - startTime
                        loadTimestamp = System.currentTimeMillis()
                        AdLogger.d("[$TAG] ✅ 开屏广告加载成功, 耗时: %d ms, isTimeout: %s", loadTime, isTimeout)
                        
                        // 完成 deferred
                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@TopOnSplashAdController) {
                            if (loadingDeferred == deferred) {
                                loadingDeferred = null
                            }
                        }

                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }
    
                    override fun onAdLoadTimeout() {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("[$TAG] ❌ 开屏广告加载超时, 耗时: %d ms", loadTime)
                        
                        // 失败 deferred
                        deferred.complete(AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "加载超时")))
                        synchronized(this@TopOnSplashAdController) {
                            if (loadingDeferred == deferred) {
                                loadingDeferred = null
                            }
                        }

                        if (continuation.isActive) {
                            continuation.resume(AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "加载超时")))
                        }
                    }
    
                    override fun onNoAdError(error: AdError?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("[$TAG] ❌ 开屏广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)
                        
                        // 失败 deferred
                        deferred.complete(AdResult.Failure(AdException(
                            parseErrorCode(error?.code),
                            error?.desc ?: "加载失败"
                        )))
                        synchronized(this@TopOnSplashAdController) {
                            if (loadingDeferred == deferred) {
                                loadingDeferred = null
                            }
                        }

                        if (continuation.isActive) {
                            continuation.resume(AdResult.Failure(AdException(
                                parseErrorCode(error?.code),
                                error?.desc ?: "加载失败"
                            )))
                        }
                    }
    
                    override fun onAdShow(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 开屏广告已展示")
                        cachedEcpm = parseEcpm(info?.ecpmLevel)
                    }
    
                    override fun onAdClick(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 开屏广告被点击")
                    }
    
                    override fun onAdDismiss(info: TUAdInfo?, extraInfo: TUSplashAdExtraInfo?) {
                        AdLogger.d("[$TAG] 开屏广告已关闭")
                    }
                }, 5000)
                
                splashAd = ad
                ad.loadAd()
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(createAdException("加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) {
                    loadingDeferred = null
                }
            }
            throw e
        }
    }

    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(0, message, cause)
    }

    suspend fun showAd(
        activity: Activity,
        container: ViewGroup,
        onLoaded: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = splashAd
        
        if (ad == null || !ad.isAdReady) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            onLoaded?.invoke(false)
            if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "没有可用的缓存广告")))
            return@suspendCancellableCoroutine
        }
        
        onLoaded?.invoke(true)
        AdLogger.d("[$TAG] 准备展示开屏广告")
        
        ad.setAdListener(object : TUSplashAdListener {
            override fun onAdLoaded(isTimeout: Boolean) {}
            override fun onAdLoadTimeout() {}
            override fun onNoAdError(error: AdError?) {}
            override fun onAdShow(info: TUAdInfo?) {}
            override fun onAdClick(info: TUAdInfo?) {}
            
            override fun onAdDismiss(info: TUAdInfo?, extraInfo: TUSplashAdExtraInfo?) {
                AdLogger.d("[$TAG] 开屏广告已关闭")
                clearCache()
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })
        
        ad.show(activity, container)
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
        val ad = splashAd ?: return false
        if (!ad.isAdReady) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        splashAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }
}
