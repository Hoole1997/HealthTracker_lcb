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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.daily.health.manager.utils.showNativeAd
import com.android.common.bill.ads.log.AdLogger
import net.corekit.monetize.ui.NativeAdStyle

/**
 * 原生广告 Compose 容器。请求绑定组合生命周期，并与普通 View 共用动态开关。
 */
@Composable
fun NativeAdContainer(
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    modifier: Modifier = Modifier
) {
    var isLoaded by remember(position) { mutableStateOf(false) }
    // 使用变量持有 FrameLayout 实例，在 LaunchedEffect 中直接操作
    var containerView by remember { mutableStateOf<FrameLayout?>(null) }

    // 独立于重组的加载逻辑
    LaunchedEffect(position, style, containerView) {
        val container = containerView ?: return@LaunchedEffect
        AdLogger.d("[NativeAdContainer] 开始加载流程 | Position: $position")
        
        isLoaded = showNativeAd(
            container = container,
            style = style.toRemaxStyleType(),
            position = position
        )

        AdLogger.d("[NativeAdContainer] 最终结果 | 是否展示: $isLoaded")
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
