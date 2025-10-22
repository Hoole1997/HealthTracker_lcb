package com.healthtracker.blood.suger.service

/**
 * 健康服务相关常量
 */
object HealthServiceConstants {

    // 通知渠道ID
    const val CHANNEL_ID_HEALTH_SERVICE = "health_service_channel"

    // 通知ID
    const val NOTIFICATION_ID_HEALTH_SERVICE = 2001

    // 广播Action - 通知点击
    const val ACTION_BLOOD_SUGAR = "com.healthtracker.blood.suger.ACTION_BLOOD_SUGAR"
    const val ACTION_BLOOD_PRESSURE = "com.healthtracker.blood.suger.ACTION_BLOOD_PRESSURE"
    const val ACTION_HEART_RATE = "com.healthtracker.blood.suger.ACTION_HEART_RATE"

    // SharedPreferences 键
    const val PREF_HEALTH_SERVICE_ENABLED = "health_service_enabled"

    // Intent Extra Keys - 通知点击参数
    const val EXTRA_NOTIFICATION_ACTION = "extra_notification_action"

    // Action Values - 通知点击动作值
    const val ACTION_VALUE_BLOOD_SUGAR = "blood_sugar"
    const val ACTION_VALUE_BLOOD_PRESSURE = "blood_pressure"
    const val ACTION_VALUE_HEART_RATE = "heart_rate"
}
