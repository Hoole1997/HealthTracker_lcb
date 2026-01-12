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

        // 获取超时配置
        val config = BiddingPlatformController.getCurrentChannelConfig()
        val timeoutSeconds = config?.biddingTimeoutSeconds ?: 10
        val timeoutMillis = timeoutSeconds * 1000L

        // 1. 并行预加载
        InterstitialBiddingManager.preloadAll(activity)

        // 2. 执行竞价（内部已包含等待逻辑）
        val bidResult = InterstitialBiddingManager.performBidding(activity, timeoutMillis)

        if (bidResult == null) {
            AdLogger.w("[$TAG] 多平台竞价失败，没有可用广告")
            return AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "多平台竞价失败"))
        }

        AdLogger.d("[$TAG] 竞价胜出: %s, eCPM: %.6f USD", bidResult.platform.name, bidResult.ecpm)

        // 3. 展示胜出平台的广告
        return showWinnerAd(activity, bidResult, position)
    }

    /**
     * 展示竞价胜出的广告
     */
    private suspend fun showWinnerAd(
        activity: Activity,
        bidResult: PlatformBidResult,
        position: String
    ): AdResult<Unit> {
        return when (bidResult.platform) {
            BiddingPlatform.ADMOB -> {
                AdLogger.d("[$TAG] 展示 AdMob 插页广告")
                InterstitialAds.getInstance().displayAd(activity, position)
            }
            BiddingPlatform.PANGLE -> {
                AdLogger.d("[$TAG] 展示 Pangle 插页广告")
                PangleInterstitialAdController.getInstance().showAd(activity)
            }
            BiddingPlatform.TOPON -> {
                AdLogger.d("[$TAG] 展示 TopOn 插页广告")
                TopOnInterstitialAdController.getInstance().showAd(activity)
            }
        }
    }
}
