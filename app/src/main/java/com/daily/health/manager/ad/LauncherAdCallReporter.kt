package com.daily.health.manager.ad

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.daily.health.manager.App

/**
 * 将应用侧的广告调用统一同步给 Launcher SDK。
 */
object LauncherAdCallReporter {
    const val TYPE_REWARDED = 1
    const val TYPE_INTERSTITIAL = 2
    const val TYPE_SPLASH = 3
    const val TYPE_NATIVE = 4
    const val TYPE_BANNER = 5

    fun report(context: Context, position: String, adType: Int) {
        val activity = context.findActivity() ?: return
        runCatching {
            App.INSTANCE.showAd(activity, position, adType)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
