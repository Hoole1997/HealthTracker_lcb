package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.NativeAds
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleNativeAdController
import net.corekit.monetize.ads.topon.TopOnNativeAdController
import net.corekit.monetize.ui.NativeAdStyle

/**
 * 原生广告智能竞价管理器
 * 
 * 提供原生广告场景的多平台竞价支持。
 * 实现 "并行加载 + 竞价 + 展示" 流程。
 */
object NativeSmartBiddingManager {

    private const val TAG = "NativeSmartBidding"

    /**
     * 智能竞价并展示原生广告
     * 
     * @param context Context 上下文
     * @param container 广告容器
     * @param position 广告位置标识
     * @param style 广告样式
     * @param onClick 点击回调
     * @return 是否成功展示广告
     */
    suspend fun smartBidAndShow(
        context: Context,
        container: ViewGroup,
        position: String,
        style: NativeAdStyle = NativeAdStyle.STANDARD,
        onClick: (() -> Unit)? = null
    ): Boolean {
        // 确保竞价配置已初始化
        BiddingConfigManager.ensureInitialized(context)

        // 检查是否启用多平台竞价
        if (!BiddingPlatformController.isMultiPlatformBiddingEnabled()) {
            AdLogger.d("[$TAG] 多平台竞价未启用，使用 AdMob 直接展示")
            return NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick)
        }

        return multiPlatformBidAndShow(context, container, position, style, onClick)
    }

    /**
     * 执行多平台竞价流程
     */
    private suspend fun multiPlatformBidAndShow(
        context: Context,
        container: ViewGroup,
        position: String,
        style: NativeAdStyle,
        onClick: (() -> Unit)?
    ): Boolean {
        AdLogger.d("[$TAG] ========== 开始原生广告多平台竞价 ==========")

        val controller = BiddingPlatformController
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())
        // TopOn 原生广告仅在 STANDARD 样式时参与竞价（因为 TopOn 需要在加载时指定尺寸，不同尺寸需要不同缓存，简化为仅支持 Normal）
        val isStandardStyle = style == NativeAdStyle.STANDARD
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey()) && isStandardStyle
        
        if (!isStandardStyle) {
            AdLogger.d("[$TAG] 非 STANDARD 样式 (%s)，TopOn Native 不参与竞价", style.description)
        }

        // 1. 并行加载各平台广告
        val (admobResult, pangleResult, toponResult) = coroutineScope {
            val admobDeferred = if (admobEnabled) async {
                AdLogger.d("[$TAG] 加载 AdMob Native...")
                runCatching { 
                    NativeAds.getInstance().loadInAdvance(context)
                    AdResult.Success(Unit) 
                }.getOrElse { AdResult.Failure(net.corekit.monetize.ads.AdException(-1, it.message ?: "加载失败")) }
            } else null

            val pangleDeferred = if (pangleEnabled) async {
                AdLogger.d("[$TAG] 加载 Pangle Native...")
                PangleNativeAdController.getInstance().preloadAd(context)
            } else null

            val toponDeferred = if (toponEnabled) async {
                AdLogger.d("[$TAG] 加载 TopOn Native...")
                TopOnNativeAdController.getInstance().preloadAd(context)
            } else null

            Triple(
                admobDeferred?.await(),
                pangleDeferred?.await(),
                toponDeferred?.await()
            )
        }

        // 2. 执行竞价（传入 style 参数，用于判断 TopOn 是否参与）
        val winner = NativePreloadManager.performBidding(
            admobLoadResult = admobResult,
            pangleLoadResult = pangleResult,
            toponLoadResult = toponResult,
            style = style
        )

        AdLogger.d("[$TAG] 竞价胜出: %s", winner.name)

        // 3. 展示胜出平台的广告
        return showWinnerAd(context, container, position, style, winner, onClick)
    }

    /**
     * 展示竞价胜出的广告
     */
    private suspend fun showWinnerAd(
        context: Context,
        container: ViewGroup,
        position: String,
        style: NativeAdStyle,
        winner: BiddingWinner,
        onClick: (() -> Unit)?
    ): Boolean {
        return when (winner) {
            BiddingWinner.ADMOB -> {
                AdLogger.d("[$TAG] 展示 AdMob Native 广告")
                NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick, bypassBidding = true)
            }
            BiddingWinner.PANGLE -> {
                AdLogger.d("[$TAG] 展示 Pangle Native 广告")
                val success = PangleNativeAdController.getInstance().renderToContainer(context, container, style)
                if (success) {
                    AdLogger.d("[$TAG] Pangle Native 渲染成功")
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(context, net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE, net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE)
                    true
                } else {
                    AdLogger.w("[$TAG] Pangle Native 渲染失败，回退到 AdMob")
                    NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick)
                }
            }
            BiddingWinner.TOPON -> {
                AdLogger.d("[$TAG] 展示 TopOn Native 广告")
                val success = TopOnNativeAdController.getInstance().renderToContainer(context, container, style)
                if (success) {
                    AdLogger.d("[$TAG] TopOn Native 渲染成功")
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(context, net.corekit.monetize.ads.bidding.BiddingWinner.TOPON, net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE)
                    true
                } else {
                    AdLogger.w("[$TAG] TopOn Native 渲染失败，回退到 AdMob")
                    NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick)
                }
            }
        }
    }
}
