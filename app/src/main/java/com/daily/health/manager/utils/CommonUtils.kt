package com.daily.health.manager.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.PowerManager
import android.os.Process
import com.bytedance.sdk.openadsdk.activity.TTAdActivity
import com.bytedance.sdk.openadsdk.activity.TTAppOpenAdActivity
import com.facebook.ads.AudienceNetworkActivity
import com.google.android.libraries.ads.mobile.sdk.common.AdActivity
import com.daily.health.manager.constants.KEY_APP_FIRST_START_TIME
import com.daily.health.manager.constants.KEY_APP_OPEN_TIMES
import com.daily.health.manager.constants.KEY_APP_START_TIME
import com.daily.health.manager.face.act.GuideScreen
import com.daily.health.manager.face.act.SplashScreen
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logi
import com.healthtracker.framework.util.SpUtils
import net.corekit.monetize.ui.AdmobFullScreenNativeAdActivity
import java.util.Calendar
import java.util.Date


// 第一次启动时间
fun onAppStart() {
    if (!SpUtils.contain(KEY_APP_START_TIME)) {
        SpUtils.putLong(KEY_APP_START_TIME, System.currentTimeMillis())
    }

    runCatching {
        val firstStart = SpUtils.getLong(KEY_APP_FIRST_START_TIME, 0L)
        if (firstStart == 0L) {
            SpUtils.putLong(KEY_APP_FIRST_START_TIME, System.currentTimeMillis())
        }
        SpUtils.putInt(KEY_APP_OPEN_TIMES,getOpenTimes() + 1)
    }
}

/**
 * 获取app打开次数(启动页被打开次数)
 */
fun getOpenTimes() = SpUtils.getInt(KEY_APP_OPEN_TIMES,0)



fun getCurProcessName(context: Context): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val processName = Application.getProcessName()
            if (!processName.isNullOrEmpty()) {
                return processName
            }
        } catch (ignored: Throwable) {
        }
    }
    try {
        val pid = Process.myPid()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (appProcess in activityManager.runningAppProcesses) {
            if (appProcess != null && appProcess.pid == pid) {
                return appProcess.processName
            }
        }
    } catch (ignored: Exception) {
    }
    return null
}

fun getScreenSize(context: Context): Point {
    val x: Int = context.resources.displayMetrics.widthPixels
    val y: Int = context.resources.displayMetrics.heightPixels
    return Point(x, y)
}

fun isInteractive(context: Context) = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive

fun isSameDay(time: Long, time2: Long, tag: String) = Calendar.getInstance().let {
    val date1 = getDateInt(it, time)
    val date2 = getDateInt(it, time2)
    if (BuildState.debug) "isSameDay [$tag] $date1 $date2".logi("Util")
    date1 == date2
}

private fun getDateInt(calendar: Calendar, time: Long): Int {
    calendar.timeInMillis = time
    return calendar[Calendar.YEAR] * 10000 + (calendar[Calendar.MONTH] + 1) * 100 + calendar[Calendar.DAY_OF_MONTH]
}

fun isExcludePage(lastVisibleActivity: Activity?) =
    lastVisibleActivity is SplashScreen ||
            lastVisibleActivity is GuideScreen


fun isAdPage(activity: Activity?) = activity?.run {
    this.javaClass in adClasses
}?: run {
    false
}

val adClasses = arrayOf(
    AdActivity::class.java,
    AdmobFullScreenNativeAdActivity::class.java,
    AudienceNetworkActivity::class.java,
    TTAppOpenAdActivity::class.java,
    TTAdActivity::class.java,
    GuideScreen::class.java,
    sg.bigo.ads.api.AdActivity::class.java,
)

fun getTodayStart(): Date {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}
