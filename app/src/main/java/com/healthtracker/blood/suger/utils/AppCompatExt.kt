package com.healthtracker.blood.suger.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.BannerAds
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ads.InterstitialAds
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
                    container.isVisible = true
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

fun FragmentActivity.loadNative(
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

            val success = NativeAds.getInstance().displayAdInView(
                context = container.context,
                container = container,
                style = style,
                onClick = onClick
            )

            if (success) {
                container.visibility = View.VISIBLE
                call.invoke(true)
            } else {
                container.visibility = View.GONE
                call.invoke(false)
            }
        } catch (e: Exception) {
            container.visibility = View.GONE
            call.invoke(false)
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
