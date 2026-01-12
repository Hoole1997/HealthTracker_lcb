package net.corekit.monetize.ads.pangle

import android.app.Activity
import android.content.Context
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAd
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdInteractionListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedAdLoadListener
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardedRequest
import com.bytedance.sdk.openadsdk.api.reward.PAGRewardItem
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.log.AdLogger
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Pangle 激励广告控制器
 */
class PangleRewardedAdController private constructor() {

    companion object {
        private const val TAG = "PangleRewarded"
        
        @Volatile
        private var instance: PangleRewardedAdController? = null
        
        fun getInstance(): PangleRewardedAdController {
            return instance ?: synchronized(this) {
                instance ?: PangleRewardedAdController().also { instance = it }
            }
        }
    }

    private var cachedAd: PAGRewardedAd? = null
    private var cachedEcpm: Double = 0.0
    private val isLoading = AtomicBoolean(false)
    private var loadTimestamp: Long = 0
    private val cacheExpireTime = 60 * 60 * 1000L

    suspend fun preloadAd(context: Context): AdResult<Unit> {
        if (!AdIdHelper.hasPangleRewardedId()) {
            AdLogger.d("[$TAG] 激励广告 ID 未配置，跳过加载")
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "激励广告 ID 未配置"))
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
            val adUnitId = BuildConfig.PANGLE_REWARDED_ID
            val startTime = System.currentTimeMillis()
            
            AdLogger.d("[$TAG] 开始加载激励广告, ID: %s", adUnitId)
            
            PAGRewardedAd.loadAd(adUnitId, PAGRewardedRequest(), object : PAGRewardedAdLoadListener {
                override fun onAdLoaded(ad: PAGRewardedAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    cachedAd = ad
                    loadTimestamp = System.currentTimeMillis()
                    cachedEcpm = try {
                        ad.mediaExtraInfo?.get("price")?.toString()?.toDoubleOrNull() ?: 0.0
                    } catch (e: Exception) { 0.0 }
                    
                    AdLogger.d("[$TAG] ✅ 激励广告加载成功, 耗时: %d ms, eCPM: %.6f USD", loadTime, cachedEcpm)
                    if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
                }

                override fun onError(code: Int, message: String?) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("[$TAG] ❌ 激励广告加载失败, 耗时: %d ms, code: %d, message: %s", loadTime, code, message)
                    if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(code, message ?: "加载失败")))
                }
            })
        }

    suspend fun showAd(
        activity: Activity,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val ad = cachedAd
        
        if (ad == null || !hasValidCache()) {
            AdLogger.w("[$TAG] 没有可用的缓存广告")
            onRewardEarned?.invoke(false)
            if (continuation.isActive) continuation.resume(AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "没有可用的缓存广告")))
            return@suspendCancellableCoroutine
        }
        
        var hasEarnedReward = false
        AdLogger.d("[$TAG] 准备展示激励广告")
        
        ad.setAdInteractionListener(object : PAGRewardedAdInteractionListener {
            override fun onAdShowed() { AdLogger.d("[$TAG] 激励广告已展示") }
            override fun onAdClicked() { AdLogger.d("[$TAG] 激励广告被点击") }
            
            override fun onAdDismissed() {
                AdLogger.d("[$TAG] 激励广告已关闭, 是否获得奖励: %s", hasEarnedReward)
                clearCache()
                onRewardEarned?.invoke(hasEarnedReward)
                onDismiss?.invoke()
                if (continuation.isActive) continuation.resume(AdResult.Success(Unit))
            }

            override fun onUserEarnedReward(rewardItem: PAGRewardItem) {
                hasEarnedReward = true
                AdLogger.d("[$TAG] 用户获得奖励: %s x %d", rewardItem.rewardName, rewardItem.rewardAmount)
            }

            override fun onUserEarnedRewardFail(code: Int, message: String?) {
                AdLogger.w("[$TAG] 奖励发放失败: %d - %s", code, message)
            }
        })
        
        ad.show(activity)
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
