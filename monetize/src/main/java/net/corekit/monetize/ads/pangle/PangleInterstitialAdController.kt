package net.corekit.monetize.ads.pangle

import android.app.Activity
import android.content.Context
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAd
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdInteractionListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialAdLoadListener
import com.bytedance.sdk.openadsdk.api.interstitial.PAGInterstitialRequest
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
 * Pangle 插页广告控制器
 * 
 * 管理 Pangle 插页广告的加载、展示和缓存
 */
class PangleInterstitialAdController private constructor() {

    companion object {
        private const val TAG = "PangleInterstitial"
        
        @Volatile
        private var instance: PangleInterstitialAdController? = null
        
        fun getInstance(): PangleInterstitialAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleInterstitialAdController().also { instance = it }
            }
        }
    }

    // 缓存的广告
    private var cachedAd: PAGInterstitialAd? = null
    
    // 广告 eCPM（美分转美元）
    private var cachedEcpm: Double = 0.0
    
    // 加载状态
    private val isLoading = AtomicBoolean(false)
    
    // 广告加载时间戳（用于判断缓存是否过期）
    private var loadTimestamp: Long = 0
    
    // 缓存有效期（1小时）
    private val cacheExpireTime = 60 * 60 * 1000L

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
     * @param timeoutMillis 超时时间（毫秒）
     * @return 广告加载结果
     */
    suspend fun waitForAd(timeoutMillis: Long): AdResult<Unit> {
        val deferred = synchronized(this) {
            if (hasValidCache()) {
                return@synchronized CompletableDeferred(AdResult.Success(Unit))
            }
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
            AdResult.Failure(AdException(0, "等待被中断", e))
        }
    }

    /**
     * 预加载广告
     */
    suspend fun preloadAd(context: Context): AdResult<Unit> {
        // 检查广告 ID 是否有效
        if (!AdIdHelper.hasPangleInterstitialId()) {
            AdLogger.d("[$TAG] 插页广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "插页广告 ID 未配置"))
        }
        
        // 检查 SDK 是否已初始化
        if (!PangleManager.isReady()) {
            val initResult = PangleManager.initialize(context)
            if (initResult is AdResult.Failure) {
                return initResult
            }
        }
        
        // 检查是否有有效缓存
        if (hasValidCache()) {
            AdLogger.d("[$TAG] 已有有效缓存，跳过加载")
            return AdResult.Success(Unit)
        }
        
        // 防止重复加载
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

    /**
     * 加载广告
     */
    private suspend fun loadAd(context: Context): AdResult<Unit> {
        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val adUnitId = BuildConfig.PANGLE_INTERSTITIAL_ID
                val startTime = System.currentTimeMillis()

                AdLogger.d("[$TAG] 开始加载插页广告, ID: %s", adUnitId)

                val request = PAGInterstitialRequest()

                PAGInterstitialAd.loadAd(adUnitId, request, object : PAGInterstitialAdLoadListener {
                    override fun onAdLoaded(ad: PAGInterstitialAd) {
                        val loadTime = System.currentTimeMillis() - startTime
                        cachedAd = ad
                        loadTimestamp = System.currentTimeMillis()

                        cachedEcpm = try {
                            ad.mediaExtraInfo?.get("price")?.toString()?.toDoubleOrNull() ?: 0.0
                        } catch (e: Exception) {
                            0.0
                        }

                        AdLogger.d("[$TAG] ✅ 插页广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)

                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@PangleInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }

                    override fun onError(code: Int, message: String?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("[$TAG] ❌ 插页广告加载失败, 耗时: %d ms, code: %d, message: %s", loadTime, code, message)

                        deferred.complete(AdResult.Failure(AdException(code, message ?: "加载失败")))
                        synchronized(this@PangleInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(code, message ?: "加载失败")))
                    }
                })
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(AdException(0, "加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) loadingDeferred = null
            }
            throw e
        }
    }

    /**
     * 展示广告
     */
    suspend fun showAd(
        activity: Activity,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = cachedAd
        
        if (ad == null || !hasValidCache()) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            if (continuation.isActive) {
                continuation.resume(AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "没有可用的缓存广告")))
            }
            return@suspendCancellableCoroutine
        }
        
        AdLogger.d("[$TAG] 准备展示插页广告")
        
        ad.setAdInteractionListener(object : PAGInterstitialAdInteractionListener {
            override fun onAdShowed() {
                AdLogger.d("[$TAG] 插页广告已展示")
            }

            override fun onAdClicked() {
                AdLogger.d("[$TAG] 插页广告被点击")
            }

            override fun onAdDismissed() {
                AdLogger.d("[$TAG] 插页广告已关闭")
                clearCache()
                onDismiss?.invoke()
                if (continuation.isActive) {
                    continuation.resume(AdResult.Success(Unit))
                }
            }
        })
        
        ad.show(activity)
    }

    /**
     * 获取缓存广告的 eCPM
     */
    fun getEcpm(): Double {
        return if (hasValidCache()) cachedEcpm else 0.0
    }

    /**
     * 检查是否有有效缓存
     */
    fun hasValidCache(): Boolean {
        if (cachedAd == null) return false
        // 检查缓存是否过期
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cachedAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }
}
