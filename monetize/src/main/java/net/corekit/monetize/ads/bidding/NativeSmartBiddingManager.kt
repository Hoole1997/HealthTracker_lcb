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
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
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
            AdLogger.logD(TAG, "多平台竞价未启用 | 使用 AdMob 直接展示")
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
        AdLogger.logD(TAG, "开始多平台竞价 | 位置: %s | 样式: %s", position, style.description)

        val controller = BiddingPlatformController
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())
        // TopOn 原生广告仅在 STANDARD 样式时参与竞价（因为 TopOn 需要在加载时指定尺寸，不同尺寸需要不同缓存，简化为仅支持 Normal）
        val isStandardStyle = style == NativeAdStyle.STANDARD
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey()) && isStandardStyle
        
        if (!isStandardStyle) {
            AdLogger.logD(TAG, "TopOn 不参与竞价 | 原因: 非 STANDARD 样式 (%s)", style.description)
        }

        // 1. 并行加载各平台广告
        val (admobResult, pangleResult, toponResult) = coroutineScope {
            val admobDeferred = if (admobEnabled) async {
                AdLogger.logD(TAG, "加载中 | 平台: AdMob")
                runCatching { 
                    NativeAds.getInstance().loadInAdvance(context)
                    AdResult.Success(Unit) 
                }.getOrElse { AdResult.Failure(net.corekit.monetize.ads.AdException(-1, it.message ?: "加载失败")) }
            } else null

            val pangleDeferred = if (pangleEnabled) async {
                AdLogger.logD(TAG, "加载中 | 平台: Pangle")
                PangleNativeAdController.getInstance().preloadAd(context)
            } else null

            val toponDeferred = if (toponEnabled) async {
                AdLogger.logD(TAG, "加载中 | 平台: TopOn")
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
            style = style,
            admobEnabled = admobEnabled,
            pangleEnabled = pangleEnabled,
            toponEnabled = toponEnabled
        )

        // 如果没有平台参与竞价，返回 false 不展示广告
        if (winner == null) {
            AdLogger.logW(TAG, "竞价失败 | 无可用平台参与竞价，不展示广告")
            return false
        }

        AdLogger.logD(TAG, "竞价胜出 | 平台: %s", winner.name)

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
        val success = when (winner) {
            BiddingWinner.ADMOB -> {
                AdLogger.logD(TAG, "展示广告 | 平台: AdMob")
                NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick, bypassBidding = true)
            }
            BiddingWinner.PANGLE -> {
                AdLogger.logD(TAG, "展示广告 | 平台: Pangle")
                val renderSuccess = PangleNativeAdController.getInstance().renderToContainer(context, container, style, position)
                if (renderSuccess) {
                    AdLogger.logD(TAG, "渲染成功 | 平台: Pangle")
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(context, net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE, net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE)
                    true
                } else {
                    AdLogger.logW(TAG, "渲染失败 | 平台: Pangle | 回退到 AdMob")
                    NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick, bypassBidding = true)
                }
            }
            BiddingWinner.TOPON -> {
                AdLogger.logD(TAG, "展示广告 | 平台: TopOn")
                val renderSuccess = TopOnNativeAdController.getInstance().renderToContainer(context, container, style, position)
                if (renderSuccess) {
                    AdLogger.logD(TAG, "渲染成功 | 平台: TopOn")
                    net.corekit.monetize.ads.PreloadController.preloadPlatformAdType(context, net.corekit.monetize.ads.bidding.BiddingWinner.TOPON, net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE)
                    true
                } else {
                    AdLogger.logW(TAG, "渲染失败 | 平台: TopOn | 回退到 AdMob")
                    NativeAds.getInstance().displayAdInView(context, container, position, style, onClick = onClick, bypassBidding = true)
                }
            }
        }
        
        // Record platform frequency on successful show
        if (success) {
            PlatformFrequencyManager.recordShow(winner.toBiddingPlatform(), BiddingAdType.NATIVE)
        }
        
        return success
    }
}
