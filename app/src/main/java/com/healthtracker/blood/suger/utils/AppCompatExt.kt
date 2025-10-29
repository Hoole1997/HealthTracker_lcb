package com.healthtracker.blood.suger.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.utils.loadInterstitial
import com.healthtracker.framework.ext.toastShort
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.BannerAds
import net.corekit.monetize.ads.InterstitialAds
import net.corekit.monetize.ads.NativeAds
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ui.NativeAdStyle


fun FragmentActivity.safeLaunch(afterInvoke: () -> Unit) {
    if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        afterInvoke.invoke()
    }
}

fun FragmentActivity.loadBanner(
    container: ViewGroup,
    condition: () -> Boolean = { true },
    call: (Boolean) -> Unit = {}) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }

            when (val result = BannerAds.getInstance().displayAd(this@loadBanner,container)) {
                is AdResult.Success -> {
                    container.isVisible = true
                    call.invoke(true)
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

fun FragmentActivity.loadNative(container: ViewGroup,
                                style: NativeAdStyle = NativeAdStyle.STANDARD,
                                condition: () -> Boolean = { true },
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
                style = style
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


fun FragmentActivity.loadInterstitial(condition: () -> Boolean = { true }, call: (Boolean) -> Unit) {
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

fun FragmentActivity.loadReword(condition: () -> Boolean = { true }, call: (Boolean) -> Unit) {
    lifecycleScope.launch {
        try {
            // 检查条件是否满足
            if (!condition.invoke()) {
                call.invoke(false)
                return@launch
            }

            when (val result = RewardedAds.getInstance().show(this@loadReword)) {
                is AdResult.Success -> {
                    call.invoke(true)
                }
                is AdResult.Failure -> {
                    // result.error.message 可用于提示或上报
                    call.invoke(true)
                }

                AdResult.Loading -> {

                }
            }

        } catch (e: Exception) {
            call.invoke(false)
        }
    }
}

fun FragmentActivity.showInter(onComplete:() -> Unit){
    loadInterstitial {
        onComplete.invoke()
    }
}