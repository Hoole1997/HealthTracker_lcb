package com.healthtracker.earthquake.push

import android.content.Context

/**
 * 地震推送本地存储：记录低震级推送的上次展示时间。
 */
object EarthquakePushStorage {
    private const val PREFS = "earthquake_push_prefs"
    private const val KEY_LAST_LOW_PUSH_AT = "last_low_push_at"

    fun getLastLowSeverityPushTime(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_LOW_PUSH_AT, 0L)
    }

    fun updateLastLowSeverityPushTime(context: Context, timeMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_LOW_PUSH_AT, timeMillis)
            .apply()
    }
}