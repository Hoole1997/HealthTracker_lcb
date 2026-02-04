package com.daily.health.manager.face.compose.ad

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.corekit.monetize.ads.NativeAds
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.NativeSmartBiddingManager
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ui.NativeAdStyle

/**
 * 原生广告 Compose 容器 (针对重组冲突优化版)
 * 
 * 优化点：
 * 1. 使用 LaunchedEffect 确保加载逻辑与 Compose 生命周期绑定，且仅触发一次。
 * 2. 增加明确的调试日志输出。
 * 3. 增加竞价配置强制初始化检查。
 * 4. 增加竞价失败后的 AdMob 自动兜底。
 */
@Composable
fun NativeAdContainer(
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }
    // 使用变量持有 FrameLayout 实例，在 LaunchedEffect 中直接操作
    var containerView by remember { mutableStateOf<FrameLayout?>(null) }

    // 独立于重组的加载逻辑
    LaunchedEffect(position) {
        val container = containerView ?: return@LaunchedEffect
        AdLogger.d("[NativeAdContainer] 开始加载流程 | Position: $position")
        
        try {
            // 1. 强制初始化配置
            BiddingConfigManager.ensureInitialized(context)
            
            // 打印竞价状态便于调试
            val isBiddingEnabled = BiddingPlatformController.isMultiPlatformBiddingEnabled()
            AdLogger.d("[NativeAdContainer] 判定流程 | 竞价启用: $isBiddingEnabled")

            // 2. 尝试竞价加载
            var success = NativeSmartBiddingManager.smartBidAndShow(
                context = context,
                container = container,
                position = position,
                style = style
            )
            
            // 3. 竞价失败/未启用时的 AdMob 兜底逻辑
            if (!success) {
                AdLogger.d("[NativeAdContainer] 竞价未成功/未命中，尝试执行原生兜底展示")
                success = NativeAds.getInstance().displayAdInView(
                    context = context,
                    container = container,
                    position = position,
                    style = style
                )
            }
            
            isLoaded = success
            AdLogger.d("[NativeAdContainer] 最终结果 | 是否展示: $isLoaded")
        } catch (e: Exception) {
            AdLogger.e("[NativeAdContainer] 加载过程发生异常", e)
            isLoaded = false
        }
    }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                containerView = this
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLoaded) Modifier.wrapContentHeight() else Modifier.height(0.dp)),
        update = {
            // 这里不再放耗时加载逻辑，专注同步 View 状态
        }
    )
}
