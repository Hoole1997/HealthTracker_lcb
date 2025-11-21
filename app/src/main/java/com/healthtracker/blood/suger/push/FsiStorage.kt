package com.healthtracker.blood.suger.push

import com.google.android.gms.common.wrappers.Wrappers.packageManager
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.SpUtils


private const val KEY_TRIGGER_COUNT = "fsi_trigger_count"
private const val KEY_LAST_ACTIVE_TIME = "fsi_last_active_time"
private const val KEY_LAST_CLICK_NOTIFY_TIME = "last_click_notify_time"
private const val KEY_LAST_TRIGGER_DATE = "fsi_last_trigger_date"  // 存储最后触发日期（格式：yyyyMMdd）

/**
 * 获取当天触发次数（每天自动重置）
 */
fun getTriggerCount(): Int {
    val today = getCurrentDate()
    val lastDate = SpUtils.getString(KEY_LAST_TRIGGER_DATE, "")
    
    // 如果日期不同，说明是新的一天，重置计数
    if (lastDate != today) {
        "Fsi Reset trigger count for new day: $today".logd("FsiTriggerChecker")
        return 0
    }
    
    return SpUtils.getInt(KEY_TRIGGER_COUNT, 0)
}



/**
 * 记录触发（自动处理日期切换）
 */
fun recordTrigger() {
    val today = getCurrentDate()
    val lastDate = SpUtils.getString(KEY_LAST_TRIGGER_DATE, "")
    
    // 如果是新的一天，重置计数
    val newCount = if (lastDate != today) {
        1
    } else {
        getTriggerCount() + 1
    }
    "Fsi 增加触发次数，当前出发次数 = $newCount".logd("FsiTriggerChecker")
    
    SpUtils.putInt(KEY_TRIGGER_COUNT, newCount)
    SpUtils.putString(KEY_LAST_TRIGGER_DATE, today)
}

/**
 * 重置触发记录（测试用）
 */
fun reset() {
    SpUtils.remove(KEY_TRIGGER_COUNT)
    SpUtils.remove(KEY_LAST_ACTIVE_TIME)
    SpUtils.remove(KEY_LAST_TRIGGER_DATE)
}

/**
 * 获取当前日期（格式：yyyyMMdd）
 */
private fun getCurrentDate(): String {
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1  // 月份从0开始
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    return String.format("%04d%02d%02d", year, month, day)
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