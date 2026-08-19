package com.daily.health.manager.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import com.daily.health.manager.App
import com.daily.health.manager.alarm.PermissionManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.NativeAdAutoRefreshManager
import net.corekit.monetize.ui.NativeAdStyle


fun FragmentActivity.safeLaunch(afterInvoke: () -> Unit) {
    if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        afterInvoke.invoke()
    }
}

fun FragmentActivity.loadBanner(
    container: ViewGroup,
    position: String,
    condition: () -> Boolean = { true },
    onClose: (() -> Unit)? = null,
    call: (Boolean) -> Unit = {},

) {
    lifecycleScope.launch {
        try {
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }

            when (AdShowExt.showBannerAd(this@loadBanner, container, position)) {
                is AdResult.Success -> {
                    val canShow = condition.invoke()
                    container.isVisible = canShow
                    if (!canShow) {
                        call.invoke(false)
                        return@launch
                    }
                    if(BuildState.debug) "Banner 展示成功".logd(PermissionManager.TAG)
                    call.invoke(true)

                }

                is AdResult.Failure -> {
                    container.isVisible = false
                    call.invoke(false)
                }
            }

        } catch (e: Exception) {
            container.isVisible = false
            call.invoke(false)
        }
    }
}

/**
 * 加载原生广告（向后兼容版本）
 * @param container 广告容器
 * @param style 广告样式
 * @param enableAutoRefresh 是否启用自动刷新，默认为true
 * @param condition 条件判断
 * @param onClick 点击回调
 * @param call 结果回调
 */
fun FragmentActivity.loadNative(
    container: ViewGroup,
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    enableAutoRefresh: Boolean = true,
    condition: () -> Boolean = { true },
    onClick: () -> Unit = {App.INSTANCE.isClickAdLeave = true},
    call: (Boolean) -> Unit = {}
) {
    loadNativeWithManager(container, position, style, enableAutoRefresh, condition, onClick) { success, _ ->
        call.invoke(success)
    }
}

/**
 * 加载原生广告（完整版，可获取刷新管理器）
 * @param container 广告容器
 * @param style 广告样式
 * @param enableAutoRefresh 是否启用自动刷新，默认为true
 * @param condition 条件判断
 * @param onClick 点击回调
 * @param call 结果回调，第一个参数为是否成功，第二个参数为刷新管理器（可用于手动控制 stop()/release()）
 */
fun FragmentActivity.loadNativeWithManager(
    container: ViewGroup,
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    enableAutoRefresh: Boolean = true,
    condition: () -> Boolean = { true },
    onClick: () -> Unit = {App.INSTANCE.isClickAdLeave = true},
    call: (success: Boolean, refreshManager: NativeAdAutoRefreshManager?) -> Unit = { _, _ -> }
) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                container.visibility = View.GONE
                call.invoke(false, null)
                return@launch
            }

            val success = AdShowExt.showNativeAdInContainer(
                context = container.context,
                container = container,
                styleType = style.toRemaxStyleType(),
                position = position
            )

            if (success) {
                container.visibility = View.VISIBLE
                call.invoke(true, null)
            } else {
                container.visibility = View.GONE
                call.invoke(false, null)
            }
        } catch (e: Exception) {
            container.visibility = View.GONE
            call.invoke(false, null)
        }
    }
}

fun FragmentActivity.loadFullNative(
    container: ViewGroup,
    position: String,
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    condition: () -> Boolean = { true },
    onClick: () -> Unit = {App.INSTANCE.isClickAdLeave = true},
    call: (Boolean) -> Unit = {}

) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                container.visibility = View.GONE
                call.invoke(false)
                return@launch
            }
            App.fixAdBug(this@loadFullNative)
            when(AdShowExt.showFullScreenNativeAdInContainer(this@loadFullNative, showInterstitial = false, position = position)){
                is AdResult.Success -> {
                    container.visibility = View.VISIBLE
                    call.invoke(true)
                }
                is AdResult.Failure -> {
                    container.visibility = View.GONE
                    call.invoke(false)
                }
            }
        } catch (e: Exception) {
            container.visibility = View.GONE
            call.invoke(false)
        }
    }
}


fun FragmentActivity.loadInterstitial(
    position: String,
    condition: () -> Boolean = { true },
    call: (Boolean) -> Unit
) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }
            App.fixAdBug(this@loadInterstitial)
            when (AdShowExt.showInterstitialAd(this@loadInterstitial, position = position)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }

                is AdResult.Failure -> {
                    call.invoke(false)
                }

            }

        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.loadRewardBidding(position: String, call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        try {
            App.fixAdBug(this@loadRewardBidding)
            when (AdShowExt.showRewardedAd(this@loadRewardBidding, position = position)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }

                is AdResult.Failure -> {
                    call.invoke(false)
                }

            }
        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.loadReword(position: String, condition: () -> Boolean = { true }, call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }
            App.fixAdBug(this@loadReword)
            when (AdShowExt.showRewardedAd(this@loadReword, position = position)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }

                is AdResult.Failure -> {
                    // result.error.message 可用于提示或上报
                    call.invoke(false)
                }

            }

        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.showInter(position: String, onComplete: () -> Unit) {
    loadInterstitial(position) {
        onComplete.invoke()
    }
}
