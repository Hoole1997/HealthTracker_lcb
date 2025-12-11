package com.healthtracker.blood.suger.push

import com.healthtracker.blood.suger.App
import com.healthtracker.framework.util.SpUtils

private const val KEY_LAST_ACTIVE_TIME = "fsi_last_active_time"
private const val KEY_LAST_CLICK_NOTIFY_TIME = "last_click_notify_time"

/**
 * 记录最后活跃时间（用户关闭首页时调用）
 */
fun recordLastActiveTime() {
    SpUtils.putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
}

fun recordLastClickNotifyTime() = SpUtils.putLong(KEY_LAST_CLICK_NOTIFY_TIME, System.currentTimeMillis())

/**
 * 获取首次安装时间
 * @return 首次安装时间戳，失败返回 0
 */
fun getFirstInstallTime(): Long {
    return try {
        val packageInfo = App.INSTANCE.packageManager.getPackageInfo(
            App.INSTANCE.packageName,
            0
        )
        packageInfo.firstInstallTime
    } catch (e: Exception) {
        0L
    }
}
