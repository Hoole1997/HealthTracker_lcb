package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleAppOpenAdController
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnSplashAdController

/**
 * 开屏两层预加载管理器
 * (包含展示时竞价逻辑)
 */
object SplashTwoLayerPreloadManager {

    private const val TAG = "SplashTwoLayerPreload"
    private const val PRELOAD_TIMEOUT_MS = 15000L
    private const val SHOW_TIMEOUT_MS = 60000L

    suspend fun preloadAll(context: Context) = coroutineScope {
        // 根据竞价模式决定预加载内容
        val biddingMode = BiddingConfigManager.getSceneBiddingMode("splash")
        val isTwoLayer = biddingMode == "two_layer"
        val modeCn = if (isTwoLayer) "两层" else "单层"

        AdLogger.d("[$TAG] 开始${modeCn}预加载")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        jobs += async { AppOpenPreloadManager.preloadAll(context) }
        
        if (isTwoLayer) {
            jobs += async { InterstitialPreloadManager.preloadAll(context) }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] ${modeCn}预加载完成 (Mode: $biddingMode)")
    }

    suspend fun performTwoLayerBidding(context: Context): FinalBidResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val platformResults = mutableListOf<PlatformBidResult>()
        
        // 获取开屏场景的竞价超时时间
        val timeoutMillis = BiddingConfigManager.getBiddingTimeoutMs("splash")
        
        // 获取开屏场景的竞价模式
        val biddingMode = BiddingConfigManager.getSceneBiddingMode("splash")
        val isTwoLayer = biddingMode == "two_layer"
        val modeCn = if (isTwoLayer) "两层" else "单层"
        
        AdLogger.d("[$TAG] ============ 开始${modeCn}竞价 ============")
        AdLogger.d("[$TAG] 竞价模式: $biddingMode")

        // 并行执行竞价（优化：避免串行等待导致耗时翻倍）
        val splashDeferred = async { AppOpenPreloadManager.performBidding(context, timeoutMillis) }
        
        // 仅在 Two-Layer 模式下请求插屏
        val interstitialDeferred = if (isTwoLayer) {
            async { InterstitialPreloadManager.performBidding(context, timeoutMillis) }
        } else {
            null
        }
        
        // 等待两个竞价结果
        val splashWinner = splashDeferred.await()
        val interstitialWinner = interstitialDeferred?.await()
        
        splashWinner?.let { platformResults.add(it) }
        interstitialWinner?.let { platformResults.add(it) }
        
        val biddingTime = System.currentTimeMillis() - startTime
        
        if (platformResults.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的广告参与竞价")
            FinalBidResult.failed(biddingTime)
        } else {
        val finalWinner = platformResults.maxByOrNull { it.ecpm }
        AdLogger.d("[$TAG] ============ ${modeCn}竞价结束 ============")
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
            AdException(AdException.ERROR_NOT_LOADED, "No winning ad")
        )
        
        AdLogger.d("[$TAG] Show winning ad: %s - %s", winner.platform.name, winner.winnerType.name)
        
        // 等待权限授权完成（避免权限流程未结束就弹广告）
        AdsManager.Controllers.appOpen.awaitPermissionReady()
        
        return withTimeoutOrNull(SHOW_TIMEOUT_MS) {
            val showResult = when (winner.winnerType) {
                BiddingAdType.SPLASH -> showSplashAd(activity, container, winner.platform, onDismiss) {
                    PlatformFrequencyManager.recordShow(winner.platform, winner.winnerType)
                }
                BiddingAdType.INTERSTITIAL -> {
                    val result = showInterstitialAd(activity, winner.platform, onDismiss)
                    if (result is AdResult.Success) {
                        PlatformFrequencyManager.recordShow(winner.platform, winner.winnerType)
                    }
                    result
                }
                else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Unsupported ad type"))
            }
            
            showResult
        } ?: AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "Ad show timeout"))
    }

    private suspend fun showSplashAd(
        activity: Activity,
        container: ViewGroup?,
        platform: BiddingPlatform,
        onDismiss: (() -> Unit)?,
        onShow: () -> Unit
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                val showResult = AdsManager.Controllers.appOpen.displayAd(activity, "bidding", onShow = onShow, onLoaded = { _ -> })
                onDismiss?.invoke()
                showResult
            }
            BiddingPlatform.PANGLE -> {
                val result = PangleAppOpenAdController.getInstance().showAd(activity, onDismiss = onDismiss, onShow = onShow)
                if (result is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE, net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH)
                }
                result
            }
            BiddingPlatform.TOPON -> {
                if (container != null) {
                    val result = TopOnSplashAdController.getInstance().showAd(activity, container, null, onShow, onDismiss)
                    if (result is AdResult.Success) {
                        net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.TOPON, net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH)
                    }
                    result
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
                val result = PangleInterstitialAdController.getInstance().showAd(activity, onDismiss)
                if (result is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE, net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL)
                }
                result
            }
            BiddingPlatform.TOPON -> {
                val result = TopOnInterstitialAdController.getInstance().showAd(activity, onDismiss)
                if (result is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.TOPON, net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL)
                }
                result
            }
        }
    }

    fun hasReadyAd(): Boolean {
        return AppOpenPreloadManager.hasReadyAd() || InterstitialPreloadManager.hasReadyAd()
    }
}
