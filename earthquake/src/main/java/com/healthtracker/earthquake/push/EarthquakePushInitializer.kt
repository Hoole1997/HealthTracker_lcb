package com.healthtracker.earthquake.push

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * 初始化入口：在宿主App合适的时机调用以开始每日两次定时任务。
 */
object EarthquakePushInitializer {
    @JvmOverloads
    fun init(context: Context, intervalHours: Long? = null) {
        intervalHours?.let { EarthquakePushConfig.updateIntervalHours(it) }
        EarthquakePushScheduler.scheduleDailyChecks(context)

        // 初始化后立即入队一次检查任务，避免用户等待下一次定时触发
//        val request = OneTimeWorkRequestBuilder<EarthquakePushWorker>().build()
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            EarthquakePushWorker.WORK_NAME,
//            ExistingWorkPolicy.APPEND_OR_REPLACE,
//            request
//        )
    }
}