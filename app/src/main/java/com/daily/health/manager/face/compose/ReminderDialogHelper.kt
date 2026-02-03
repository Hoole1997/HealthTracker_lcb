package com.daily.health.manager.face.compose

import com.healthtracker.framework.util.SpUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 提醒弹窗显示逻辑助手
 */
object ReminderDialogHelper {
    private const val KEY_PREFIX = "reminder_shown_"
    
    /**
     * 检查今天是否已显示过该类型的提醒弹窗
     */
    fun shouldShowReminderDialog(type: Int): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val key = "${KEY_PREFIX}${type}_$today"
        return !SpUtils.getBoolean(key, false)
    }
    
    /**
     * 标记今天已显示过该类型的提醒弹窗
     */
    fun markReminderDialogShown(type: Int) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val key = "${KEY_PREFIX}${type}_$today"
        SpUtils.putBoolean(key, true)
    }
}
