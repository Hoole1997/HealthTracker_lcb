package com.daily.health.manager.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.daily.health.manager.App
import com.daily.health.manager.alarm.PermissionManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.BannerAds
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.NativeAdAutoRefreshManager
import net.corekit.monetize.ads.NativeAds
import net.corekit.monetize.ads.RewardBiddingManager
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ui.NativeAdStyle
import okio.AsyncTimeout.Companion.condition


fun FragmentActivity.safeLaunch(afterInvoke: () -> Unit) {
    if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        afterInvoke.invoke()
    }
}

fun FragmentActivity.loadBanner(
    container: ViewGroup,
    condition: () -> Boolean = { true },
    onClose: (() -> Unit)? = null,
    call: (Boolean) -> Unit = {},

) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }

            when (val result =
                BannerAds.getInstance().displayAd(this@loadBanner, container, onClick = {
                    App.INSTANCE.isClickAdLeave = true

                }, onClose = onClose)) {
                is AdResult.Success -> {
                    val canShow = condition.invoke()
                    container.isVisible = canShow
                    if (!canShow) {
                        call.invoke(false)
                        return@launch
                    }
                    if(result.data){
                        if(BuildState.debug) "折叠式广告，先不回调成功".logd(PermissionManager.TAG)
                    }else{
                        if(BuildState.debug) "非折叠式广告，回调成功".logd(PermissionManager.TAG)
                        call.invoke(true)
                    }

                }

                is AdResult.Failure -> {
                    container.isVisible = false
                    call.invoke(false)
                }

                AdResult.Loading -> {

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
    style: NativeAdStyle = NativeAdStyle.STANDARD,
    enableAutoRefresh: Boolean = true,
    condition: () -> Boolean = { true },
    onClick: () -> Unit = {App.INSTANCE.isClickAdLeave = true},
    call: (Boolean) -> Unit = {}
) {
    loadNativeWithManager(container, style, enableAutoRefresh, condition, onClick) { success, _ ->
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

            val success = NativeAds.getInstance().displayAdInView(
                context = container.context,
                container = container,
                style = style,
                onClick = onClick
            )

            if (success) {
                container.visibility = View.VISIBLE
                
                // 如果启用自动刷新，创建并启动刷新管理器
                val refreshManager = if (enableAutoRefresh) {
                    NativeAdAutoRefreshManager(
                        container = container,
                        style = style,
                        lifecycleOwner = this@loadNativeWithManager,
                        onRefresh = {
                            // 刷新时重新加载广告
                            NativeAds.getInstance().displayAdInView(
                                context = container.context,
                                container = container,
                                style = style,
                                onClick = onClick
                            )
                        },
                        onClick = onClick
                    ).also { it.startRefreshTimer() }
                } else {
                    null
                }
                
                // 通过回调传递 refreshManager，解决返回值无效问题
                call.invoke(true, refreshManager)
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

            val success = FullNativeAds.getInstance().displayAdInView(
                context = container.context,
                container = container,
                this@loadFullNative,
            )

            when(success){
                is AdResult.Success -> {
                    container.visibility = View.VISIBLE
                    call.invoke(true)
                }
                is AdResult.Failure -> {
                    container.visibility = View.GONE
                    call.invoke(false)
                }
                AdResult.Loading -> {

                }

            }
        } catch (e: Exception) {
            container.visibility = View.GONE
            call.invoke(false)
        }
    }
}


fun FragmentActivity.loadInterstitial(
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

            when (val result = InterstitialAds.getInstance().displayAd(this@loadInterstitial)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }

                is AdResult.Failure -> {
                    call.invoke(false)
                }

                AdResult.Loading -> {

                }
            }

        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.loadRewardBidding(call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        try {
            // 检查竞价开关，关闭时回退到普通激励广告
            if (!AdConfigManager.isRewardBiddingEnabled()) {
                when (RewardedAds.getInstance().show(this@loadRewardBidding)) {
                    is AdResult.Success -> call.invoke(true)
                    is AdResult.Failure -> call.invoke(false)
                    AdResult.Loading -> {}
                }
                return@launch
            }

            when (val result = RewardBiddingManager.showWithBidding(this@loadRewardBidding)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }

                is AdResult.Failure -> {
                    call.invoke(false)
                }

                AdResult.Loading -> {

                }
            }
        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.loadReword(condition: () -> Boolean = { true }, call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }

            when (RewardedAds.getInstance().show(this@loadReword)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }

                is AdResult.Failure -> {
                    // result.error.message 可用于提示或上报
                    call.invoke(false)
                }

                AdResult.Loading -> {

                }
            }

        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.showInter(onComplete: () -> Unit) {
    loadInterstitial {
        onComplete.invoke()
    }
}
