package com.healthtracker.earthquake.push

/**
 * 通知与渠道常量，确保高震级与低震级使用不同通知ID，避免覆盖。
 */
object EarthquakePushIds {
    const val NOTIFICATION_ID_HIGH = 100500  // >= M5.0
    const val NOTIFICATION_ID_LOW = 100499   // < M5.0

    const val CHANNEL_ID = "earthquake_alert"
    const val CHANNEL_NAME = "地震预警"
}