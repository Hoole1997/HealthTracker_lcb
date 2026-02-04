package com.daily.health.manager.face.compose.ad

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.bidding.NativeSmartBiddingManager
import net.corekit.monetize.ui.NativeAdStyle

/**
 * 原生广告 Compose 容器
 * 封装了从加载到展示的完整逻辑，支持加载失败自动塌陷
 * 参考 AppCompatExt.kt 中的相关逻辑实现
 *
 * @param position 广告埋点位置标识
 * @param style 广告样式 (如 NativeAdStyle.CARD_7)
 * @param modifier 外部修饰符
 */
@Composable
fun NativeAdContainer(
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLoaded) Modifier.wrapContentHeight() else Modifier.height(0.dp)),
        update = { container: FrameLayout ->
            // 避免重复加载
            if (container.childCount == 0) {
                val lifecycleOwner = context as? LifecycleOwner
                lifecycleOwner?.lifecycleScope?.launch {
                    try {
                        // 使用 container.context 确保类型为 android.content.Context
                        val success = NativeSmartBiddingManager.smartBidAndShow(
                            context = container.context,
                            container = container,
                            position = position,
                            style = style
                        )
                        isLoaded = success
                    } catch (e: Exception) {
                        isLoaded = false
                    }
                }
            }
        }
    )
}
