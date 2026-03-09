package com.daily.health.manager.face.compose

import com.daily.health.manager.R
import com.daily.health.manager.data.entity.AlarmRecord

/**
 * 通知预览卡片的动态数据
 *
 * @param contentResId 通知文案 string resource ID
 * @param decorIconResId 右侧拟物图标 mipmap resource ID
 * @param buttonTextResId 模拟按钮文案 string resource ID
 * @param descriptionResId 弹窗副标题 string resource ID（动态场景文案）
 */
data class NotificationPreviewData(
    val contentResId: Int,
    val decorIconResId: Int,
    val buttonTextResId: Int = R.string.ht_record_now,
    val descriptionResId: Int = R.string.ht_notification_grant_permissions_des
)

/**
 * 根据闹钟类型获取通知预览卡片数据
 *
 * @param alarmType AlarmRecord.TYPE_* 常量
 * @return 对应类型的通知预览数据，未知类型返回默认血糖数据
 */
fun getNotificationPreviewData(alarmType: Int): NotificationPreviewData {
    return when (alarmType) {
        AlarmRecord.TYPE_BLOOD_SUGAR -> NotificationPreviewData(
            contentResId = R.string.ht_alarm_blood_sugar_content,
            decorIconResId = R.mipmap.ht_home_card_bs,
            descriptionResId = R.string.ht_notification_des_blood_sugar
        )
        AlarmRecord.TYPE_BLOOD_PRESSURE -> NotificationPreviewData(
            contentResId = R.string.ht_alarm_blood_pressure_content,
            decorIconResId = R.mipmap.ht_home_card_bp,
            descriptionResId = R.string.ht_notification_des_blood_pressure
        )
        AlarmRecord.TYPE_HEART_RATE -> NotificationPreviewData(
            contentResId = R.string.ht_alarm_heart_rate_content,
            decorIconResId = R.mipmap.ht_home_hero_heart,
            descriptionResId = R.string.ht_notification_des_heart_rate
        )
        AlarmRecord.TYPE_BMI -> NotificationPreviewData(
            contentResId = R.string.ht_alarm_bmi_content,
            decorIconResId = R.mipmap.ht_home_card_weight,
            descriptionResId = R.string.ht_notification_des_bmi
        )
        AlarmRecord.TYPE_CHOLESTEROL -> NotificationPreviewData(
            contentResId = R.string.ht_alarm_cholesterol_content,
            decorIconResId = R.mipmap.ht_home_card_cholesterol,
            descriptionResId = R.string.ht_notification_des_cholesterol
        )
        else -> NotificationPreviewData(
            contentResId = R.string.ht_alarm_general_content,
            decorIconResId = R.mipmap.ic_notification_req_ring, // 暂用血糖图标，后续替换
            descriptionResId = R.string.ht_notification_des_general
        )
    }
}
