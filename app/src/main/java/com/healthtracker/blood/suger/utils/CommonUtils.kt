package com.healthtracker.blood.suger.utils

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.PowerManager
import android.os.Process
import com.healthtracker.blood.suger.constants.KEY_APP_FIRST_START_TIME
import com.healthtracker.blood.suger.constants.KEY_APP_OPEN_TIMES
import com.healthtracker.blood.suger.constants.KEY_APP_START_TIME
import com.healthtracker.framework.util.SpUtils


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

fun isFirstOpen() = getOpenTimes() == 1

//判断是否无损音乐
fun isLossless(format:String) = format == "flac" || format == "ape" || format == "wav"


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