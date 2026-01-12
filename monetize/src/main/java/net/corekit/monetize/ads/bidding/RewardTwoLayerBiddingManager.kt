package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.RewardedInterstitialAds
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController
import net.corekit.monetize.ads.pangle.PangleRewardedAdController
import net.corekit.monetize.ads.topon.TopOnInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnRewardedAdController

/**
 * 激励广告两层竞价管理器
 */
object RewardTwoLayerBiddingManager {

    private const val TAG = "RewardTwoLayer"
    private const val PRELOAD_TIMEOUT_MS = 15000L
    private const val SHOW_TIMEOUT_MS = 60000L

    private val rewardedInterstitialController get() = RewardedInterstitialAds.getInstance()
    private val rewardedController get() = RewardedAds.getInstance()

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        AdLogger.d("[$TAG] 开始两层竞价预加载")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        jobs += async { RewardedBiddingManager.preloadAll(context) }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    rewardedInterstitialController.loadInAdvance(context)
                    Unit
                } ?: Unit
            }
        }
        
        jobs += async { InterstitialBiddingManager.preloadAll(context) }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 两层竞价预加载完成")
    }

    suspend fun performTwoLayerBidding(context: Context): FinalBidResult {
        val controller = BiddingPlatformController
        val startTime = System.currentTimeMillis()
        val platformResults = mutableListOf<PlatformBidResult>()
        
        AdLogger.d("[$TAG] ============ 开始激励两层竞价 ============")
        
        // 第一层：各平台激励广告竞价
        val rewardedWinner = RewardedBiddingManager.performBidding(context)
        if (rewardedWinner != null) {
            platformResults.add(rewardedWinner)
        }
        
        // AdMob 插页激励广告
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())) {
            val rawEcpm = rewardedInterstitialController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            if (rewardedInterstitialController.hasCachedAd()) {
                platformResults.add(PlatformBidResult(
                    platform = BiddingPlatform.ADMOB,
                    winnerType = BiddingAdType.REWARDED_INTERSTITIAL,
                    ecpm = ecpm
                ))
                AdLogger.d("[$TAG] AdMob 插页激励 eCPM: %.6f USD", ecpm)
            }
        }
        
        // 第二层：与插页广告竞价
        val interstitialWinner = InterstitialBiddingManager.performBidding(context, PRELOAD_TIMEOUT_MS)
        if (interstitialWinner != null) {
            platformResults.add(interstitialWinner)
        }
        
        val biddingTime = System.currentTimeMillis() - startTime
        
        if (platformResults.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的广告参与竞价")
            return FinalBidResult.failed(biddingTime)
        }
        
        val finalWinner = platformResults.maxByOrNull { it.ecpm }
        AdLogger.d("[$TAG] ============ 两层竞价结束 ============")
        AdLogger.d("[$TAG] 最终胜出: %s - %s, eCPM: %.6f USD", 
            finalWinner?.platform?.name, finalWinner?.winnerType?.name, finalWinner?.ecpm ?: 0.0)
        
        return FinalBidResult(
            winner = finalWinner,
            allResults = platformResults,
            biddingTimeMs = biddingTime
        )
    }

    suspend fun showWinnerAd(
        activity: Activity,
        result: FinalBidResult,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> {
        val winner = result.winner ?: return AdResult.Failure(
            AdException(AdException.ERROR_NOT_LOADED, "没有胜出的广告")
        )
        
        AdLogger.d("[$TAG] 展示胜出广告: %s - %s", winner.platform.name, winner.winnerType.name)
        
        return withTimeoutOrNull(SHOW_TIMEOUT_MS) {
            when (winner.winnerType) {
                BiddingAdType.REWARDED -> showRewardedAd(activity, winner.platform, onRewardEarned, onDismiss)
                BiddingAdType.REWARDED_INTERSTITIAL -> {
                    val showResult = rewardedInterstitialController.displayAd(activity, "bidding")
                    if (showResult is AdResult.Success) {
                        onRewardEarned?.invoke(true) // 插页激励通常直接给奖励
                    }
                    onDismiss?.invoke()
                    
                    when (showResult) {
                        is AdResult.Success -> AdResult.Success(Unit)
                        is AdResult.Failure -> AdResult.Failure(showResult.error)
                        else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Unknown show result"))
                    }
                }
                BiddingAdType.INTERSTITIAL -> {
                    onRewardEarned?.invoke(false)
                    showInterstitialAd(activity, winner.platform, onDismiss)
                }
                else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "不支持的广告类型"))
            }
        } ?: AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "展示广告超时"))
    }

    private suspend fun showRewardedAd(
        activity: Activity,
        platform: BiddingPlatform,
        onRewardEarned: ((Boolean) -> Unit)?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                val showResult = rewardedController.show(activity, "bidding")
                if (showResult is AdResult.Success) {
                    onRewardEarned?.invoke(showResult.data.rewarded)
                }
                onDismiss?.invoke()
                
                when (showResult) {
                    is AdResult.Success -> AdResult.Success(Unit)
                    is AdResult.Failure -> AdResult.Failure(showResult.error)
                    else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Unknown show result"))
                }
            }
            BiddingPlatform.PANGLE -> {
                PangleRewardedAdController.getInstance().showAd(activity, onRewardEarned, onDismiss)
            }
            BiddingPlatform.TOPON -> {
                TopOnRewardedAdController.getInstance().showAd(activity, onRewardEarned, onDismiss)
            }
        }
    }

    private suspend fun showInterstitialAd(
        activity: Activity,
        platform: BiddingPlatform,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                AdsManager.Controllers.interstitial.displayAd(activity, "bidding")
                onDismiss?.invoke()
                AdResult.Success(Unit)
            }
            BiddingPlatform.PANGLE -> {
                PangleInterstitialAdController.getInstance().showAd(activity, onDismiss)
            }
            BiddingPlatform.TOPON -> {
                TopOnInterstitialAdController.getInstance().showAd(activity, onDismiss)
            }
        }
    }

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (RewardedBiddingManager.hasReadyAd()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())
            && rewardedInterstitialController.hasCachedAd()) return true
        
        if (InterstitialBiddingManager.hasReadyAd()) return true
        
        return false
    }
}
