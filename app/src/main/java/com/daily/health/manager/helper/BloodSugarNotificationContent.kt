package com.daily.health.manager.helper

import com.daily.health.manager.R

/**
 * 血糖通知内容管理器
 * 管理 7 种血糖测量场景的通知文案配置
 */
object BloodSugarNotificationContent {
    
    // ========== 场景 ID 常量 ==========
    const val SCENE_FASTING_AM = "fasting_am"         // 早餐前空腹
    const val SCENE_POST_BREAKFAST = "post_breakfast" // 早餐后 2 小时
    const val SCENE_PRE_LUNCH = "pre_lunch"           // 午餐前
    const val SCENE_POST_LUNCH = "post_lunch"         // 午餐后 2 小时
    const val SCENE_PRE_DINNER = "pre_dinner"         // 晚餐前
    const val SCENE_POST_DINNER = "post_dinner"       // 晚餐后 2 小时
    const val SCENE_BEDTIME = "bedtime"               // 睡前

    /**
     * 获取预设闹钟配置
     * @return List of (sceneId, hour, minute)
     */
    fun getPresetConfigs(): List<Triple<String, Int, Int>> = listOf(
        Triple(SCENE_FASTING_AM, 7, 30),
        Triple(SCENE_POST_BREAKFAST, 9, 30),
        Triple(SCENE_PRE_LUNCH, 11, 30),
        Triple(SCENE_POST_LUNCH, 14, 0),
        Triple(SCENE_PRE_DINNER, 18, 0),
        Triple(SCENE_POST_DINNER, 20, 30),
        Triple(SCENE_BEDTIME, 23, 0),
    )

    /**
     * 根据场景 ID 获取通知标题资源 ID
     * 复用现有血糖测量状态文案
     */
    fun getTitleResId(sceneId: String?): Int = when (sceneId) {
        SCENE_FASTING_AM -> R.string.ht_blood_sugar_status_fasting
        SCENE_POST_BREAKFAST, SCENE_POST_LUNCH, SCENE_POST_DINNER -> 
            R.string.ht_blood_sugar_status_two_hours_after_meal
        SCENE_PRE_LUNCH, SCENE_PRE_DINNER -> R.string.ht_blood_sugar_status_before_meal
        SCENE_BEDTIME -> R.string.ht_blood_sugar_status_bedtime
        else -> R.string.ht_alarm_blood_sugar_content // 默认文案
    }

    /**
     * 根据场景 ID 获取通知描述资源 ID
     * 使用新增的场景差异化描述文案
     */
    fun getDescResId(sceneId: String?): Int = when (sceneId) {
        SCENE_FASTING_AM -> R.string.ht_bs_notify_desc_fasting_am
        SCENE_POST_BREAKFAST -> R.string.ht_bs_notify_desc_post_breakfast
        SCENE_PRE_LUNCH -> R.string.ht_bs_notify_desc_pre_lunch
        SCENE_POST_LUNCH -> R.string.ht_bs_notify_desc_post_lunch
        SCENE_PRE_DINNER -> R.string.ht_bs_notify_desc_pre_dinner
        SCENE_POST_DINNER -> R.string.ht_bs_notify_desc_post_dinner
        SCENE_BEDTIME -> R.string.ht_bs_notify_desc_bedtime
        else -> R.string.ht_alarm_blood_sugar_content
    }

    /**
     * 获取通知按钮文案资源 ID
     */
    fun getButtonResId(): Int = R.string.ht_bs_notify_btn_check
}
