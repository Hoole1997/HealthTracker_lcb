package com.daily.health.manager.push

import com.daily.health.manager.App

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
