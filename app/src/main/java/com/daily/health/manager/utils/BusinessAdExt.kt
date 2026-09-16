package com.daily.health.manager.utils

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.android.common.bill.ui.NativeAdStyleType
import com.android.common.bill.ui.dialog.ADLoadingDialog
import com.daily.health.manager.App
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import net.corekit.core.controller.AdSlotSwitchController
import net.corekit.monetize.ui.NativeAdStyle

/** 每次读取当前广告位开关，不缓存；缺失或配置异常均按关闭处理。 */
fun isAdSlotEnabled(position: String): Boolean =
    position.isNotBlank() && runCatching { AdSlotSwitchController.isEnabled(position) }.getOrDefault(false)

/**
 * 仅统一关闭/失败返回值，不创建协程、不排队、不额外等待。
 * SDK 内部超时也可能抛 CancellationException：只有页面协程真的取消时才中止业务回调。
 */
internal suspend fun requestBusinessAd(
    enabled: () -> Boolean,
    whenDisabled: Boolean = false,
    request: suspend () -> Boolean,
): Boolean {
    fun slotEnabled() = runCatching(enabled).getOrDefault(false)
    if (!slotEnabled()) return whenDisabled
    return try {
        val success = request()
        currentCoroutineContext().ensureActive()
        if (slotEnabled()) success else whenDisabled
    } catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        if (slotEnabled()) false else whenDisabled
    }
}

fun FragmentActivity.loadBanner(
    container: ViewGroup,
    position: String,
    call: (Boolean) -> Unit = {},
) {
    lifecycleScope.launch {
        val shown = requestBusinessAd(enabled = { isAdSlotEnabled(position) }) {
            container.isVisible = true
            AdShowExt.showBannerAd(this@loadBanner, container, position) is AdResult.Success
        }
        container.isVisible = shown
        if (!shown) container.removeAllViews()
        // 回调在异常处理之外执行，避免页面代码抛错后被重复调用。
        call(shown)
    }
}

fun FragmentActivity.loadNative(
    container: ViewGroup,
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    call: (Boolean) -> Unit = {},
) {
    lifecycleScope.launch {
        call(showNativeAd(container, position, style.toRemaxStyleType()))
    }
}

/** View、Compose 和地震模块共用一次原生请求，生命周期由各自调用方持有。 */
suspend fun showNativeAd(
    container: ViewGroup,
    position: String,
    style: NativeAdStyleType,
): Boolean {
    val shown = requestBusinessAd(enabled = { isAdSlotEnabled(position) }) {
        container.isVisible = true
        AdShowExt.showNativeAdInContainer(container.context, container, style, position)
    }
    container.isVisible = shown
    if (!shown) container.removeAllViews()
    return shown
}

/** 与 StreetHtml 一样，一次协程直接调用 SDK，关闭/失败也完成页面回调。 */
fun FragmentActivity.loadInterstitial(position: String, call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        val shown = requestBusinessAd(enabled = { isAdSlotEnabled(position) }) {
            try {
                App.fixAdBug(this@loadInterstitial)
                AdShowExt.showInterstitialAd(this@loadInterstitial, position = position) is AdResult.Success
            } finally {
                // SDK 异常或页面销毁时也收起加载层，避免透明窗口继续拦截点击。
                ADLoadingDialog.hide()
            }
        }
        call(shown)
    }
}

/** 关闭广告位直接回调；SDK 流程结束后也回调，页面在 call 中继续下一步。 */
fun FragmentActivity.loadReward(position: String, call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        val result = requestBusinessAd(enabled = { isAdSlotEnabled(position) }, whenDisabled = true) {
            try {
                App.fixAdBug(this@loadReward)
                AdShowExt.showRewardedAd(this@loadReward, position = position) is AdResult.Success
            } finally {
                ADLoadingDialog.hide()
            }
        }
        call(result)
    }
}
