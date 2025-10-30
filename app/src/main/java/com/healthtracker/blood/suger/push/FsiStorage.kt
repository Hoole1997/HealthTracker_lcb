package com.healthtracker.blood.suger.push

import com.google.android.gms.common.wrappers.Wrappers.packageManager
import com.healthtracker.blood.suger.App
import com.healthtracker.framework.util.SpUtils


private const val KEY_TRIGGER_COUNT = "fsi_trigger_count"
private const val KEY_LAST_TRIGGER_TIME = "fsi_last_trigger_time"
private const val KEY_LAST_ACTIVE_TIME = "fsi_last_active_time"
private const val KEY_LAST_CLICK_NOTIFY_TIME = "last_click_notify_time"

/**
 * 获取触发次数
 */
fun getTriggerCount(): Int {
    return SpUtils.getInt(KEY_TRIGGER_COUNT, 0)
}

/**
 * 获取最后触发时间
 */
fun getLastTriggerTime(): Long {
    return SpUtils.getLong(KEY_LAST_TRIGGER_TIME, 0L)
}

/**
 * 记录触发
 */
fun recordTrigger() {
    SpUtils.putInt(KEY_TRIGGER_COUNT, getTriggerCount() + 1)
    SpUtils.putLong(KEY_LAST_TRIGGER_TIME, System.currentTimeMillis())
}

/**
 * 重置触发记录（测试用）
 */
fun reset() {
    SpUtils.remove(KEY_TRIGGER_COUNT)
    SpUtils.remove(KEY_LAST_TRIGGER_TIME)
    SpUtils.remove(KEY_LAST_ACTIVE_TIME)
}

// ==================== 活跃时间相关 ====================

/**
 * 记录最后活跃时间（用户关闭首页时调用）
 */
fun recordLastActiveTime() {
    SpUtils.putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis())
}

/**
 * 获取最后活跃时间
 * @return 最后活跃时间戳，如果从未记录返回 0
 */
fun getLastActiveTime(): Long {
    return SpUtils.getLong(KEY_LAST_ACTIVE_TIME, 0L)
}


fun recordLastClickNotifyTime() = SpUtils.putLong(KEY_LAST_CLICK_NOTIFY_TIME, System.currentTimeMillis())

fun hasClickNotify() = SpUtils.getLong(KEY_LAST_CLICK_NOTIFY_TIME,0L) > 0L

// ==================== 安装时间相关 ====================

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