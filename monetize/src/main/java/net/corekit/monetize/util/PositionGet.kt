package net.corekit.monetize.util

import android.app.Activity
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import net.corekit.monetize.ui.FullScreenNativeAdActivity

object PositionGet {
    fun get(): String{
        val activityList: MutableList<Activity?> = ActivityUtils.getActivityList()
        for (activity in activityList) {
            if (activity == null || !ActivityUtils.isActivityAlive(activity) || activity is AdActivity || activity is FullScreenNativeAdActivity) {
                continue
            }
            return activity::class.simpleName.orEmpty()
        }
        return ""
    }
}