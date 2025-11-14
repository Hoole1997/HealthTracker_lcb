package com.healthtracker.earthquake.push

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * 每日两次（8:00、17:00）定时触发地震查询的调度器。
 * 使用 AlarmManager 的精确闹钟以尽量贴近指定时间。
 */
object EarthquakePushScheduler {

    // 其他项目使用需要替换ACTION
    private const val ACTION = "com.healthtracker.earthquake.ACTION_EARTHQUAKE_CHECK"
    private const val REQ_8 = 800
    private const val REQ_17 = 1700

    fun scheduleDailyChecks(context: Context) {
        scheduleNext(context, 8)
        scheduleNext(context, 17)
    }

    fun cancel(context: Context) {
        cancelHour(context, 8)
        cancelHour(context, 17)
    }

    fun scheduleNext(context: Context, hour: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = computeNextTriggerTime(hour)
        val pi = pendingIntent(context, hour)

        // Android 12+（S）对精确闹钟增加了权限限制：
        // 如果没有 SCHEDULE_EXACT_ALARM/USE_EXACT_ALARM 权限，则回退为非精确闹钟，避免崩溃。
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val canExact = am.canScheduleExactAlarms()
                if (canExact) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                    }
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        } catch (_: SecurityException) {
            // 权限不足时，使用非精确闹钟作为兜底，保证功能不崩溃
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelHour(context: Context, hour: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, hour))
    }

    private fun pendingIntent(context: Context, hour: Int): PendingIntent {
        val intent = Intent(context, EarthquakePushReceiver::class.java).apply {
            action = ACTION
            putExtra("time_hour", hour)
        }
        val reqCode = if (hour == 8) REQ_8 else REQ_17
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, reqCode, intent, flags)
    }

    private fun computeNextTriggerTime(hour: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        val now = System.currentTimeMillis()
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.timeInMillis
    }
}