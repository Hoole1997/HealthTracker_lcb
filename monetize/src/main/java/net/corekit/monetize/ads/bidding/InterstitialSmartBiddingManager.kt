package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.content.Context
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnInterstitialAdController
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ui.AdmobFullScreenNativeAdActivity
import net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdActivity
import net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdActivity

/**
 * 插页广告智能竞价管理器
 * 
 * 提供独立插页广告场景的多平台竞价支持。
 * 实现 "预加载 + 等待 + 竞价 + 展示" 流程。
 */
object InterstitialSmartBiddingManager {

    private const val TAG = "InterstitialSmartBidding"

    /**
     * 智能竞价并展示插页广告
     * 
     * @param activity Activity 上下文
     * @param position 广告位置标识
     * @return 广告展示结果
     */
    suspend fun smartBidAndShow(
        activity: Activity,
        position: String
    ): AdResult<Unit> {
        // 确保竞价配置已初始化
        BiddingConfigManager.ensureInitialized(activity)

        // 检查是否启用多平台竞价
        if (!BiddingPlatformController.isMultiPlatformBiddingEnabled()) {
            AdLogger.d("[$TAG] 多平台竞价未启用，使用 AdMob 直接展示")
            return InterstitialAds.getInstance().displayAd(activity, position)
        }

        return multiPlatformBidAndShow(activity, position)
    }

    /**
     * 执行多平台竞价流程
     */
    private suspend fun multiPlatformBidAndShow(
        activity: Activity,
        position: String
    ): AdResult<Unit> {
        AdLogger.d("[$TAG] ========== 开始插页广告多平台竞价 ==========")

        // 获取插页广告场景的竞价超时时间
        val timeoutMillis = BiddingConfigManager.getBiddingTimeoutMs("interstitial")

        // 1. 并行预加载
        InterstitialPreloadManager.preloadAll(activity)

        // 2. 执行竞价（内部已包含等待逻辑）
        val bidResult = InterstitialPreloadManager.performBidding(activity, timeoutMillis)

        if (bidResult == null) {
            AdLogger.w("[$TAG] 多平台竞价失败，没有可用广告")
            return AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "多平台竞价失败"))
        }

        AdLogger.d("[$TAG] 竞价胜出: %s, eCPM: %.6f USD", bidResult.platform.name, bidResult.ecpm)

        // 3. 展示胜出平台的广告
        return showWinnerAd(activity, bidResult, position)
    }

    /**
     * 检查是否需要展示全屏原生广告
     * 触发条件：每展示 N 个插页广告后展示一次全屏原生
     */
    private fun checkNeedShowFullNative(): Boolean {
        val interval = AdConfigManager.getFullscreenNativeAfterInterstitialCount()
        val todayShowInter = AdConfigManager.getInterstitialConfig().getDailyShowCount()
        val needShow = interval > 0 && todayShowInter > 0 && todayShowInter % interval == 0
        AdLogger.d("[$TAG] 全屏原生触发检查: 今日插页展示=$todayShowInter, 间隔=$interval, 需要展示=$needShow")
        return needShow
    }

    /**
     * 展示竞价胜出的广告
     */
    private suspend fun showWinnerAd(
        activity: Activity,
        bidResult: PlatformBidResult,
        position: String
    ): AdResult<Unit> {
        // 检查是否需要展示全屏原生广告（在任何平台插页展示前）
        if (checkNeedShowFullNative() && FullScreenNativeBiddingManager.hasAnyReadyAd()) {
            AdLogger.d("[$TAG] 触发全屏原生竞价展示")
            return showFullNativeThenInterstitial(activity, bidResult, position)
        }

        val result = when (bidResult.platform) {
            BiddingPlatform.ADMOB -> {
                AdLogger.d("[$TAG] 展示 AdMob 插页广告")
                // 已在 showWinnerAd 中检查过全屏原生，避免重复检查
                InterstitialAds.getInstance().displayAd(activity, position, ignoreFullNative = true)
            }
            BiddingPlatform.PANGLE -> {
                AdLogger.d("[$TAG] 展示 Pangle 插页广告")
                val showResult = PangleInterstitialAdController.getInstance().showAd(activity)
                if (showResult is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE, net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL)
                }
                showResult
            }
            BiddingPlatform.TOPON -> {
                AdLogger.d("[$TAG] 展示 TopOn 插页广告")
                val showResult = TopOnInterstitialAdController.getInstance().showAd(activity)
                if (showResult is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.TOPON, net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL)
                }
                showResult
            }
        }
        
        // Record platform frequency on successful show
        if (result is AdResult.Success) {
            PlatformFrequencyManager.recordShow(bidResult.platform, BiddingAdType.INTERSTITIAL)
        }
        
        return result
    }

    /**
     * 先展示全屏原生广告竞价胜出者，然后展示插页广告
     */
    private suspend fun showFullNativeThenInterstitial(
        activity: Activity,
        interstitialBidResult: PlatformBidResult,
        position: String
    ): AdResult<Unit> {
        // 1. 执行全屏原生竞价
        val fullNativeWinner = FullScreenNativeBiddingManager.bidding(activity)
        AdLogger.d("[$TAG] 全屏原生竞价胜出: ${fullNativeWinner.name}")

        // 2. 根据胜出平台启动对应的全屏原生广告 Activity（内部会展示插页）
        return when (fullNativeWinner) {
            BiddingWinner.ADMOB -> {
                if (FullNativeAds.getInstance().checkCachedAdAvailable()) {
                    AdLogger.d("[$TAG] 展示 AdMob 全屏原生 + 插页")
                    AdmobFullScreenNativeAdActivity.start(activity, position, showInterstitial = true)
                } else {
                    AdLogger.w("[$TAG] AdMob 全屏原生无缓存，回退展示插页")
                    showInterstitialOnly(activity, interstitialBidResult, position)
                }
            }
            BiddingWinner.PANGLE -> {
                if (net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdController.getInstance().hasValidCache()) {
                    AdLogger.d("[$TAG] 展示 Pangle 全屏原生 + 插页")
                    PangleFullScreenNativeAdActivity.start(activity, position, showInterstitial = true)
                } else {
                    AdLogger.w("[$TAG] Pangle 全屏原生无缓存，回退展示插页")
                    showInterstitialOnly(activity, interstitialBidResult, position)
                }
            }
            BiddingWinner.TOPON -> {
                if (net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdController.getInstance().hasCachedAd()) {
                    AdLogger.d("[$TAG] 展示 TopOn 全屏原生 + 插页")
                    TopOnFullScreenNativeAdActivity.start(activity, position, showInterstitial = true)
                } else {
                    AdLogger.w("[$TAG] TopOn 全屏原生无缓存，回退展示插页")
                    showInterstitialOnly(activity, interstitialBidResult, position)
                }
            }
        }
    }

    /**
     * 仅展示插页广告（不触发全屏原生）
     */
    private suspend fun showInterstitialOnly(
        activity: Activity,
        bidResult: PlatformBidResult,
        position: String
    ): AdResult<Unit> {
        val result = when (bidResult.platform) {
            BiddingPlatform.ADMOB -> {
                AdLogger.d("[$TAG] 展示 AdMob 插页广告")
                InterstitialAds.getInstance().displayAd(activity, position, ignoreFullNative = true)
            }
            BiddingPlatform.PANGLE -> {
                AdLogger.d("[$TAG] 展示 Pangle 插页广告")
                val showResult = PangleInterstitialAdController.getInstance().showAd(activity)
                if (showResult is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, BiddingWinner.PANGLE, BiddingAdType.INTERSTITIAL)
                }
                showResult
            }
            BiddingPlatform.TOPON -> {
                AdLogger.d("[$TAG] 展示 TopOn 插页广告")
                val showResult = TopOnInterstitialAdController.getInstance().showAd(activity)
                if (showResult is AdResult.Success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, BiddingWinner.TOPON, BiddingAdType.INTERSTITIAL)
                }
                showResult
            }
        }
        
        if (result is AdResult.Success) {
            PlatformFrequencyManager.recordShow(bidResult.platform, BiddingAdType.INTERSTITIAL)
        }
        
        return result
    }
}
