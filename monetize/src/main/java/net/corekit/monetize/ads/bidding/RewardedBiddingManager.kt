package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleRewardedAdController
import net.corekit.monetize.ads.topon.TopOnRewardedAdController

/**
 * 激励广告竞价管理器
 */
object RewardedBiddingManager {

    private const val TAG = "RewardedBidding"
    private const val PRELOAD_TIMEOUT_MS = 15000L

    private val admobController get() = RewardedAds.getInstance()

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        
        AdLogger.d("[$TAG] 开始并行预加载激励广告")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey())) {
            jobs += async { 
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    admobController.load(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    PangleRewardedAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    TopOnRewardedAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 激励广告预加载完成")
    }

    suspend fun performBidding(context: Context): PlatformBidResult? {
        val controller = BiddingPlatformController
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        AdLogger.d("[$TAG] 开始执行激励广告竞价")
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = admobController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            if (admobController.hasCachedAd()) {
                results.add(BiddingPlatform.ADMOB to ecpm)
                AdLogger.d("[$TAG] AdMob eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = PangleRewardedAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            if (PangleRewardedAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.PANGLE to ecpm)
                AdLogger.d("[$TAG] Pangle eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = TopOnRewardedAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            if (TopOnRewardedAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.TOPON to ecpm)
                AdLogger.d("[$TAG] TopOn eCPM: %.6f USD", ecpm)
            }
        }
        
        if (results.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的激励广告参与竞价")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        AdLogger.d("[$TAG] ✅ 竞价胜出: %s, eCPM: %.6f USD", winner.first.name, winner.second)
        
        return PlatformBidResult(
            platform = winner.first,
            winnerType = BiddingAdType.REWARDED,
            ecpm = winner.second
        )
    }

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey()) 
            && admobController.hasCachedAd()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())
            && PangleRewardedAdController.getInstance().hasValidCache()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())
            && TopOnRewardedAdController.getInstance().hasValidCache()) {
            return true
        }
        
        return false
    }
}
