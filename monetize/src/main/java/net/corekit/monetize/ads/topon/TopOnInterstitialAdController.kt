package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import com.thinkup.interstitial.api.TUInterstitial
import com.thinkup.interstitial.api.TUInterstitialListener
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
 * TopOn 插页广告控制器
 */
class TopOnInterstitialAdController private constructor() {

    companion object {
        private const val TAG = "TopOnInterstitial"
        
        @Volatile
        private var instance: TopOnInterstitialAdController? = null
        
        fun getInstance(): TopOnInterstitialAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnInterstitialAdController().also { instance = it }
            }
        }
    }

    private var interstitialAd: TUInterstitial? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
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

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnInterstitialId()) {
            AdLogger.d("[$TAG] 插页广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "插页广告 ID 未配置"))
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

    private suspend fun loadAd(context: Context): AdResult<Unit> {
        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                val adUnitId = BuildConfig.TOPON_INTERSTITIAL_ID
                val startTime = System.currentTimeMillis()

                AdLogger.d("[$TAG] 开始加载插页广告, ID: %s", adUnitId)

                val ad = TUInterstitial(context, adUnitId)
                interstitialAd = ad

                ad.setAdListener(object : TUInterstitialListener {
                    override fun onInterstitialAdLoaded() {
                        val loadTime = System.currentTimeMillis() - startTime
                        loadTimestamp = System.currentTimeMillis()
                        AdLogger.d("[$TAG] ✅ 插页广告加载成功, 耗时: %d ms", loadTime)

                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@TopOnInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                    }

                    override fun onInterstitialAdLoadFail(error: AdError?) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("[$TAG] ❌ 插页广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)

                        deferred.complete(AdResult.Failure(AdException(
                            parseErrorCode(error?.code),
                            error?.desc ?: "加载失败"
                        )))
                        synchronized(this@TopOnInterstitialAdController) {
                            if (loadingDeferred == deferred) loadingDeferred = null
                        }

                        if (continuation.isActive) {
                            continuation.resume(AdResult.Failure(AdException(
                                parseErrorCode(error?.code),
                                error?.desc ?: "加载失败"
                            )))
                        }
                    }

                    override fun onInterstitialAdClicked(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 插页广告被点击")
                    }

                    override fun onInterstitialAdShow(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 插页广告已展示")
                        cachedEcpm = parseEcpm(info?.ecpmLevel)
                    }

                    override fun onInterstitialAdClose(info: TUAdInfo?) {
                        AdLogger.d("[$TAG] 插页广告已关闭")
                    }

                    override fun onInterstitialAdVideoStart(info: TUAdInfo?) {}
                    override fun onInterstitialAdVideoEnd(info: TUAdInfo?) {}
                    override fun onInterstitialAdVideoError(error: AdError?) {}
                })

                ad.load()
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(AdException(0, "加载异常", e)))
            synchronized(this) {
                if (loadingDeferred == deferred) loadingDeferred = null
            }
            throw e
        }
    }

    suspend fun showAd(
        activity: Activity,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = interstitialAd
        
        if (ad == null || !ad.isAdReady) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "没有可用的缓存广告")))
            return@suspendCancellableCoroutine
        }
        
        AdLogger.d("[$TAG] 准备展示插页广告")
        
        ad.setAdListener(object : TUInterstitialListener {
            override fun onInterstitialAdLoaded() {}
            override fun onInterstitialAdLoadFail(error: AdError?) {}
            override fun onInterstitialAdClicked(info: TUAdInfo?) {}
            override fun onInterstitialAdShow(info: TUAdInfo?) {}
            override fun onInterstitialAdVideoStart(info: TUAdInfo?) {}
            override fun onInterstitialAdVideoEnd(info: TUAdInfo?) {}
            override fun onInterstitialAdVideoError(error: AdError?) {}
            
            override fun onInterstitialAdClose(info: TUAdInfo?) {
                AdLogger.d("[$TAG] 插页广告已关闭")
                clearCache()
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })
        
        ad.show(activity)
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
        val ad = interstitialAd ?: return false
        if (!ad.isAdReady) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        interstitialAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }
}
