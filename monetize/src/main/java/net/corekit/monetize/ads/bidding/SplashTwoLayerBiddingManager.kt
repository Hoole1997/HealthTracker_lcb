package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleAppOpenAdController
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnSplashAdController

/**
 * 开屏两层竞价管理器
 */
object SplashTwoLayerBiddingManager {

    private const val TAG = "SplashTwoLayer"
    private const val PRELOAD_TIMEOUT_MS = 15000L
    private const val SHOW_TIMEOUT_MS = 60000L

    suspend fun preloadAll(context: Context) = coroutineScope {
        AdLogger.d("[$TAG] 开始两层竞价预加载")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        jobs += async { AppOpenBiddingManager.preloadAll(context) }
        jobs += async { InterstitialBiddingManager.preloadAll(context) }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 两层竞价预加载完成")
    }

    suspend fun performTwoLayerBidding(context: Context): FinalBidResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val platformResults = mutableListOf<PlatformBidResult>()
        
        AdLogger.d("[$TAG] ============ 开始两层竞价 ============")
        
        // 获取当前渠道的竞价配置超时时间 (单位：秒 -> 毫秒)
        val config = BiddingPlatformController.getCurrentChannelConfig()
        val timeoutSeconds = config?.biddingTimeoutSeconds ?: 10 // 默认为 10秒
        val timeoutMillis = timeoutSeconds * 1000L

        // 并行执行两层竞价（优化：避免串行等待导致耗时翻倍）
        val splashDeferred = async { AppOpenBiddingManager.performBidding(context, timeoutMillis) }
        val interstitialDeferred = async { InterstitialBiddingManager.performBidding(context, timeoutMillis) }
        
        // 等待两个竞价结果
        val splashWinner = splashDeferred.await()
        val interstitialWinner = interstitialDeferred.await()
        
        splashWinner?.let { platformResults.add(it) }
        interstitialWinner?.let { platformResults.add(it) }
        
        val biddingTime = System.currentTimeMillis() - startTime
        
        if (platformResults.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的广告参与竞价")
            FinalBidResult.failed(biddingTime)
        } else {
        val finalWinner = platformResults.maxByOrNull { it.ecpm }
        AdLogger.d("[$TAG] ============ 两层竞价结束 ============")
        AdLogger.d("[$TAG] 最终胜出: %s - %s, eCPM: %.6f USD", 
            finalWinner?.platform?.name, finalWinner?.winnerType?.name, finalWinner?.ecpm ?: 0.0)
        
        FinalBidResult(
            winner = finalWinner,
            allResults = platformResults,
            biddingTimeMs = biddingTime
        )
        }
    }

    suspend fun showWinnerAd(
        activity: Activity,
        container: ViewGroup? = null,
        result: FinalBidResult,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> {
        val winner = result.winner ?: return AdResult.Failure(
            AdException(AdException.ERROR_NOT_LOADED, "没有胜出的广告")
        )
        
        AdLogger.d("[$TAG] 展示胜出广告: %s - %s", winner.platform.name, winner.winnerType.name)
        
        return withTimeoutOrNull(SHOW_TIMEOUT_MS) {
            when (winner.winnerType) {
                BiddingAdType.SPLASH -> showSplashAd(activity, container, winner.platform, onDismiss)
                BiddingAdType.INTERSTITIAL -> showInterstitialAd(activity, winner.platform, onDismiss)
                else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "不支持的广告类型"))
            }
        } ?: AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "展示广告超时"))
    }

    private suspend fun showSplashAd(
        activity: Activity,
        container: ViewGroup?,
        platform: BiddingPlatform,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                val showResult = AdsManager.Controllers.appOpen.displayAd(activity, "bidding") { _ -> }
                onDismiss?.invoke()
                showResult
            }
            BiddingPlatform.PANGLE -> {
                PangleAppOpenAdController.getInstance().showAd(activity, onDismiss = onDismiss)
            }
            BiddingPlatform.TOPON -> {
                if (container != null) {
                    TopOnSplashAdController.getInstance().showAd(activity, container, null, onDismiss)
                } else {
                    AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "TopOn 开屏广告需要 container"))
                }
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
                val showResult = AdsManager.Controllers.interstitial.displayAd(activity, "bidding")
                onDismiss?.invoke()
                showResult
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
        return AppOpenBiddingManager.hasReadyAd() || InterstitialBiddingManager.hasReadyAd()
    }
}
