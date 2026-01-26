package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.view.ViewGroup
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.BannerAds
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleBannerAdController
import net.corekit.monetize.ads.topon.TopOnBannerAdController
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager

/**
 * Banner 广告智能竞价管理器
 * 
 * 提供 Banner 广告场景的多平台竞价支持。
 * 实现 "并行加载 + 竞价 + 展示" 流程。
 */
object BannerSmartBiddingManager {

    private const val TAG = "BannerSmartBidding"

    /**
     * 智能竞价并展示 Banner 广告
     * 
     * @param activity Activity 上下文
     * @param container 广告容器
     * @param position 广告位置标识
     * @param onClick 点击回调
     * @param onClose 关闭回调
     * @return 广告展示结果，Boolean 表示是否为折叠式广告
     */
    suspend fun smartBidAndShow(
        activity: Activity,
        container: ViewGroup,
        position: String,
        onClick: (() -> Unit)? = null,
        onClose: (() -> Unit)? = null
    ): AdResult<Boolean> {
        // 确保竞价配置已初始化
        BiddingConfigManager.ensureInitialized(activity)

        // 检查是否启用多平台竞价
        if (!BiddingPlatformController.isMultiPlatformBiddingEnabled()) {
            AdLogger.d("[$TAG] 多平台竞价未启用，使用 AdMob 直接展示")
            return BannerAds.getInstance().displayAd(activity, container, position, onClick = onClick, onClose = onClose)
        }

        return multiPlatformBidAndShow(activity, container, position, onClick, onClose)
    }

    /**
     * 执行多平台竞价流程
     */
    private suspend fun multiPlatformBidAndShow(
        activity: Activity,
        container: ViewGroup,
        position: String,
        onClick: (() -> Unit)?,
        onClose: (() -> Unit)?
    ): AdResult<Boolean> {
        AdLogger.d("[$TAG] ========== 开始 Banner 广告多平台竞价 ==========")

        val controller = BiddingPlatformController
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.BANNER.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.BANNER.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.BANNER.toConfigKey())

        // 1. 并行加载各平台广告
        val (admobResult, pangleResult, toponResult) = coroutineScope {
            val admobDeferred = if (admobEnabled) async {
                AdLogger.d("[$TAG] 加载 AdMob Banner...")
                BannerAds.getInstance().loadInAdvance(activity)
            } else null

            val pangleDeferred = if (pangleEnabled) async {
                AdLogger.d("[$TAG] 加载 Pangle Banner...")
                PangleBannerAdController.getInstance().preloadAd(activity)
            } else null

            val toponDeferred = if (toponEnabled) async {
                AdLogger.d("[$TAG] 加载 TopOn Banner...")
                TopOnBannerAdController.getInstance().preloadAd(activity)
            } else null

            Triple(
                admobDeferred?.await(),
                pangleDeferred?.await(),
                toponDeferred?.await()
            )
        }

        // 2. 执行竞价
        val winner = BannerPreloadManager.performBidding(
            context = activity,
            admobLoadResult = admobResult,
            pangleLoadResult = pangleResult,
            toponLoadResult = toponResult
        )

        AdLogger.d("[$TAG] 竞价胜出: %s", winner.name)

        // 3. 展示胜出平台的广告
        return showWinnerAd(activity, container, position, winner, onClick, onClose)
    }

    /**
     * 展示竞价胜出的广告
     */
    private suspend fun showWinnerAd(
        activity: Activity,
        container: ViewGroup,
        position: String,
        winner: BiddingWinner,
        onClick: (() -> Unit)?,
        onClose: (() -> Unit)?
    ): AdResult<Boolean> {
        val result = when (winner) {
            BiddingWinner.ADMOB -> {
                AdLogger.d("[$TAG] 展示 AdMob Banner 广告")
                BannerAds.getInstance().displayAd(activity, container, position, onClick = onClick, onClose = onClose)
            }
            BiddingWinner.PANGLE -> {
                AdLogger.d("[$TAG] 展示 Pangle Banner 广告 | 位置: %s", position)
                val success = PangleBannerAdController.getInstance().renderToContainer(container, position)
                if (success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE, net.corekit.monetize.ads.bidding.BiddingAdType.BANNER)
                    AdResult.Success(false) // Pangle Banner 非折叠式
                } else {
                    AdLogger.w("[$TAG] Pangle Banner 渲染失败，回退到 AdMob")
                    BannerAds.getInstance().displayAd(activity, container, position, onClick = onClick, onClose = onClose)
                }
            }
            BiddingWinner.TOPON -> {
                AdLogger.d("[$TAG] 展示 TopOn Banner 广告 | 位置: %s", position)
                val success = TopOnBannerAdController.getInstance().renderToContainer(container, position)
                if (success) {
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.TOPON, net.corekit.monetize.ads.bidding.BiddingAdType.BANNER)
                    AdResult.Success(false) // TopOn Banner 非折叠式
                } else {
                    AdLogger.w("[$TAG] TopOn Banner 渲染失败，回退到 AdMob")
                    BannerAds.getInstance().displayAd(activity, container, position, onClick = onClick, onClose = onClose)
                }
            }
        }
        
        // Record platform frequency on successful show
        if (result is AdResult.Success) {
            PlatformFrequencyManager.recordShow(winner.toBiddingPlatform(), BiddingAdType.BANNER)
        }
        
        return result
    }
}
