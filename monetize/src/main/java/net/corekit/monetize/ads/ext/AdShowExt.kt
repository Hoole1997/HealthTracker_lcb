package net.corekit.monetize.ads.ext

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.RewardedInterstitialAds
import net.corekit.monetize.ads.bidding.*
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.*
import net.corekit.monetize.ads.topon.*

/**
 * 广告统一展示扩展
 * 
 * 提供基于竞价结果的统一广告展示接口
 */
object AdShowExt {

    private const val TAG = "AdShowExt"

    /**
     * 根据竞价结果展示胜出广告
     */
    suspend fun showWinnerAd(
        activity: Activity,
        result: PlatformBidResult,
        container: ViewGroup? = null,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> {
        AdLogger.d("[$TAG] 展示广告: %s - %s", result.platform.name, result.winnerType.name)
        
        return when (result.winnerType) {
            BiddingAdType.SPLASH -> showSplashAd(activity, result.platform, container, onDismiss)
            BiddingAdType.INTERSTITIAL -> showInterstitialAd(activity, result.platform, onDismiss)
            BiddingAdType.REWARDED -> showRewardedAd(activity, result.platform, onRewardEarned, onDismiss)
            BiddingAdType.REWARDED_INTERSTITIAL -> showRewardedInterstitialAd(activity, onRewardEarned, onDismiss)
            BiddingAdType.NATIVE -> showNativeAd(activity, result.platform, container, onDismiss)
            BiddingAdType.FULL_NATIVE -> showFullNativeAd(activity, result.platform, container, onDismiss)
            BiddingAdType.BANNER -> showBannerAd(activity, result.platform, container, onDismiss)
        }
    }

    // ============ 开屏广告 ============

    private suspend fun showSplashAd(
        activity: Activity,
        platform: BiddingPlatform,
        container: ViewGroup?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                AdsManager.Controllers.appOpen.displayAd(activity, "bidding") { _ -> }
                onDismiss?.invoke()
                AdResult.Success(Unit)
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

    // ============ 插页广告 ============

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

    // ============ 激励广告 ============

    private suspend fun showRewardedAd(
        activity: Activity,
        platform: BiddingPlatform,
        onRewardEarned: ((Boolean) -> Unit)?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                RewardedAds.getInstance().show(activity, "bidding")
                onDismiss?.invoke()
                AdResult.Success(Unit)
            }
            BiddingPlatform.PANGLE -> {
                PangleRewardedAdController.getInstance().showAd(activity, onRewardEarned, onDismiss)
            }
            BiddingPlatform.TOPON -> {
                TopOnRewardedAdController.getInstance().showAd(activity, onRewardEarned, onDismiss)
            }
        }
    }

    // ============ 插页激励广告 ============

    private suspend fun showRewardedInterstitialAd(
        activity: Activity,
        onRewardEarned: ((Boolean) -> Unit)?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        // 插页激励广告仅 AdMob 支持
        RewardedInterstitialAds.getInstance().displayAd(activity, "bidding")
        onDismiss?.invoke()
        return AdResult.Success(Unit)
    }

    // ============ 原生广告 ============

    private suspend fun showNativeAd(
        activity: Activity,
        platform: BiddingPlatform,
        container: ViewGroup?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        if (container == null) {
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "原生广告需要 container"))
        }
        
        // 注意：Pangle/TopOn 原生广告需要特定 UI 渲染，建议直接使用各平台控制器
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                AdsManager.Controllers.native.displayAdInView(activity, container, "bidding")
                onDismiss?.invoke()
                AdResult.Success(Unit)
            }
            BiddingPlatform.PANGLE -> {
                // Pangle 原生广告需要使用 getCachedAd() 获取广告对象后手动渲染
                AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "请使用 PangleNativeAdController.getCachedAd() 获取广告对象"))
            }
            BiddingPlatform.TOPON -> {
                // TopOn 原生广告需要使用 getCachedNativeAd() 获取广告对象后手动渲染
                AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "请使用 TopOnNativeAdController.getCachedNativeAd() 获取广告对象"))
            }
        }
    }

    // ============ 全屏原生广告 ============

    private suspend fun showFullNativeAd(
        activity: Activity,
        platform: BiddingPlatform,
        container: ViewGroup?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        if (container == null) {
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "全屏原生广告需要 container"))
        }
        
        // 注意：Pangle/TopOn 全屏原生广告需要特定 UI 渲染，建议直接使用各平台控制器
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                // activity 作为 Context，还需要一个 LifecycleOwner
                val lifecycleOwner = activity as? androidx.lifecycle.LifecycleOwner
                if (lifecycleOwner != null) {
                    AdsManager.Controllers.fullScreenNative.displayAdInView(activity, container, lifecycleOwner, "bidding")
                    onDismiss?.invoke()
                    AdResult.Success(Unit)
                } else {
                    AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Activity 不是 LifecycleOwner"))
                }
            }
            BiddingPlatform.PANGLE -> {
                AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "请使用 PangleFullScreenNativeAdController.getCachedAd() 获取广告对象"))
            }
            BiddingPlatform.TOPON -> {
                AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "请使用 TopOnFullScreenNativeAdController.getCachedNativeAd() 获取广告对象"))
            }
        }
    }

    // ============ Banner 广告 ============

    private suspend fun showBannerAd(
        activity: Activity,
        platform: BiddingPlatform,
        container: ViewGroup?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        if (container == null) {
            return AdResult.Failure(AdException(AdException.ERROR_INVALID_REQUEST, "Banner 广告需要 container"))
        }
        
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                AdsManager.Controllers.banner.displayAd(activity, container, "bidding")
                onDismiss?.invoke()
                AdResult.Success(Unit)
            }
            BiddingPlatform.PANGLE -> {
                val success = PangleBannerAdController.getInstance().renderToContainer(container)
                if (success) {
                    onDismiss?.invoke()
                    AdResult.Success(Unit)
                } else {
                    AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "Pangle Banner 广告未加载"))
                }
            }
            BiddingPlatform.TOPON -> {
                val success = TopOnBannerAdController.getInstance().renderToContainer(container)
                if (success) {
                    onDismiss?.invoke()
                    AdResult.Success(Unit)
                } else {
                    AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "TopOn Banner 广告未加载"))
                }
            }
        }
    }
}
