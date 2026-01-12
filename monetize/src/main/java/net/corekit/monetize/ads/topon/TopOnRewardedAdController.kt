package net.corekit.monetize.ads.topon

import android.app.Activity
import android.content.Context
import com.thinkup.rewardvideo.api.TURewardVideoAd
import com.thinkup.rewardvideo.api.TURewardVideoListener
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
 * TopOn 激励广告控制器
 */
class TopOnRewardedAdController private constructor() {

    companion object {
        private const val TAG = "TopOnRewarded"
        
        @Volatile
        private var instance: TopOnRewardedAdController? = null
        
        fun getInstance(): TopOnRewardedAdController {
            return instance ?: synchronized(this) {
                instance ?: TopOnRewardedAdController().also { instance = it }
            }
        }
    }

    private var rewardedAd: TURewardVideoAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasTopOnRewardedId()) {
            AdLogger.d("[$TAG] 激励广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "激励广告 ID 未配置"))
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
            val adUnitId = BuildConfig.TOPON_REWARDED_ID
            val startTime = System.currentTimeMillis()
            
            AdLogger.d("[$TAG] 开始加载激励广告, ID: %s", adUnitId)
            
            val ad = TURewardVideoAd(context, adUnitId)
            rewardedAd = ad
            
            ad.setAdListener(object : TURewardVideoListener {
                override fun onRewardedVideoAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    loadTimestamp = System.currentTimeMillis()
                    AdLogger.d("[$TAG] ✅ 激励广告加载成功, 耗时: %d ms", loadTime)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onRewardedVideoAdFailed(error: AdError?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ 激励广告加载失败, 耗时: %d ms, error: %s", loadTime, error?.fullErrorInfo)
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Failure(AdException(
                            error?.code?.toIntOrNull() ?: AdException.ERROR_INTERNAL,
                            error?.desc ?: "加载失败"
                        )))
                    }
                }

                override fun onRewardedVideoAdPlayStart(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告开始播放")
                    cachedEcpm = parseEcpm(info?.ecpmLevel)
                }

                override fun onRewardedVideoAdPlayEnd(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告播放结束")
                }

                override fun onRewardedVideoAdPlayFailed(error: AdError?, info: TUAdInfo?) {
                    AdLogger.e("[$TAG] 激励广告播放失败: %s", error?.fullErrorInfo)
                }

                override fun onRewardedVideoAdClosed(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告已关闭")
                }

                override fun onRewardedVideoAdPlayClicked(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 激励广告被点击")
                }

                override fun onReward(info: TUAdInfo?) {
                    AdLogger.d("[$TAG] 用户获得奖励")
                }
            })
            
            ad.load()
        }

    suspend fun showAd(
        activity: Activity,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = rewardedAd
        
        if (ad == null || !ad.isAdReady) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            onRewardEarned?.invoke(false)
            if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "没有可用的缓存广告")))
            return@suspendCancellableCoroutine
        }
        
        var hasEarnedReward = false
        AdLogger.d("[$TAG] 准备展示激励广告")
        
        ad.setAdListener(object : TURewardVideoListener {
            override fun onRewardedVideoAdLoaded() {}
            override fun onRewardedVideoAdFailed(error: AdError?) {}
            override fun onRewardedVideoAdPlayStart(info: TUAdInfo?) {}
            override fun onRewardedVideoAdPlayEnd(info: TUAdInfo?) {}
            override fun onRewardedVideoAdPlayFailed(error: AdError?, info: TUAdInfo?) {}
            override fun onRewardedVideoAdPlayClicked(info: TUAdInfo?) {}
            
            override fun onReward(info: TUAdInfo?) {
                hasEarnedReward = true
                AdLogger.d("[$TAG] 用户获得奖励")
            }
            
            override fun onRewardedVideoAdClosed(info: TUAdInfo?) {
                AdLogger.d("[$TAG] 激励广告已关闭, 是否获得奖励: %s", hasEarnedReward)
                clearCache()
                onRewardEarned?.invoke(hasEarnedReward)
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }
        })
        
        ad.show(activity)
    }

    fun getEcpm(): Double = if (hasValidCache()) cachedEcpm else 0.0

    fun hasValidCache(): Boolean {
        val ad = rewardedAd ?: return false
        if (!ad.isAdReady) return false
        return (System.currentTimeMillis() - loadTimestamp) < cacheExpireTime
    }

    fun clearCache() {
        rewardedAd = null
        cachedEcpm = 0.0
        loadTimestamp = 0
    }

    private fun parseEcpm(ecpmLevel: Any?): Double {
        return when (ecpmLevel) {
            is Number -> ecpmLevel.toDouble()
            is String -> ecpmLevel.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }
}
