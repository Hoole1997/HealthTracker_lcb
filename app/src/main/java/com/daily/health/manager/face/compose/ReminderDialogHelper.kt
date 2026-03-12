package com.daily.health.manager.face.compose

import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.healthtracker.framework.util.SpUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 提醒弹窗显示逻辑助手
 */
object ReminderDialogHelper {
    private const val KEY_PREFIX = "reminder_shown_"
    private const val KEY_BACK_GUIDE_TOTAL = "back_guide_total_"
    private const val KEY_BACK_GUIDE_LAST_DATE = "back_guide_last_date_"
    private const val KEY_BACK_GUIDE_GLOBAL_COUNT = "back_guide_global_count_"
    
    /**
     * 检查今天是否已显示过该类型的提醒弹窗 (原有逻辑)
     */
    fun shouldShowReminderDialog(type: Int): Boolean {
        if (!NotificationFeatureSwitch.reminderEntryEnabled) return false
        val today = getToday()
        val key = "${KEY_PREFIX}${type}_$today"
        return !SpUtils.getBoolean(key, false)
    }
    
    /**
     * 标记今天已显示过该类型的提醒弹窗 (原有逻辑)
     */
    fun markReminderDialogShown(type: Int) {
        val today = getToday()
        val key = "${KEY_PREFIX}${type}_$today"
        SpUtils.putBoolean(key, true)
    }

    /**
     * 检查是否可以显示返回引导弹窗
     * 规则:
     * 1. 该场景总上限 2 次
     * 2. 该场景每天上限 1 次
     * 3. 全局每天上限 2 次
     */
    fun canShowBackGuide(type: Int): Boolean {
        if (!NotificationFeatureSwitch.reminderEntryEnabled) return false
        val today = getToday()
        
        // 1. 全局日频控 (2次)
        val globalKey = "${KEY_BACK_GUIDE_GLOBAL_COUNT}$today"
        val globalCount = SpUtils.getInt(globalKey, 0)
        if (globalCount >= 2) return false
        
        // 2. 场景日频控 (1次)
        val lastDateKey = "${KEY_BACK_GUIDE_LAST_DATE}$type"
        val lastDate = SpUtils.getString(lastDateKey, "")
        if (lastDate == today) return false
        
        // 3. 场景总频控 (2次)
        val totalKey = "${KEY_BACK_GUIDE_TOTAL}$type"
        val totalCount = SpUtils.getInt(totalKey, 0)
        if (totalCount >= 2) return false
        
        return true
    }

    /**
     * 标记返回引导已显示
     */
    fun markBackGuideShown(type: Int) {
        val today = getToday()
        
        // 更新全局日计数
        val globalKey = "${KEY_BACK_GUIDE_GLOBAL_COUNT}$today"
        val globalCount = SpUtils.getInt(globalKey, 0)
        SpUtils.putInt(globalKey, globalCount + 1)
        
        // 更新场景日日期
        val lastDateKey = "${KEY_BACK_GUIDE_LAST_DATE}$type"
        SpUtils.putString(lastDateKey, today)
        
        // 更新场景总计数
        val totalKey = "${KEY_BACK_GUIDE_TOTAL}$type"
        val totalCount = SpUtils.getInt(totalKey, 0)
        SpUtils.putInt(totalKey, totalCount + 1)
    }

    private fun getToday(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
