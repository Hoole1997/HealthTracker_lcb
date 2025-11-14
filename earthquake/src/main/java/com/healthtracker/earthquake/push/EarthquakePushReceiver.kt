package com.healthtracker.earthquake.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * 处理闹钟触发：入队执行 Worker，并重新调度下一次同一时段任务。
 */
class EarthquakePushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val hour = intent?.getIntExtra("time_hour", -1) ?: -1

        val request = OneTimeWorkRequestBuilder<EarthquakePushWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            EarthquakePushWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )

        if (hour == 8 || hour == 17) {
            EarthquakePushScheduler.scheduleNext(context, hour)
        }
    }
}