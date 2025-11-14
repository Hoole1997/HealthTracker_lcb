package com.healthtracker.earthquake.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.earthquake.EarthquakeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 定时查询USGS接口并按规则发送通知：
 * 1) 每天8:00和17:00触发（由Alarm/Receiver调度）
 * 2) >= M5.0 立即推送（高震级）
 * 3) < M5.0 仅在上次低震级推送超过 Earthquake_interval_time 后推送
 */
class EarthquakePushWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val api = EarthquakeApi()
            val props = api.fetchYesterdayTopProperties()
            if (props == null || props.mag == null) {
                return@withContext Result.success()
            }

            val mag = props.mag!!
            val helper = EarthquakeNotificationHelper(applicationContext)

            if (mag >= 5.0) {
                // 高震级：立即发送
                helper.notifyHighSeverity(props)
            } else {
                // 低震级：检查间隔
                val last = EarthquakePushStorage.getLastLowSeverityPushTime(applicationContext)
                val now = System.currentTimeMillis()
                val intervalOk = (now - last) >= EarthquakePushConfig.intervalMillis
                if (intervalOk) {
                    helper.notifyLowSeverity(props)
                    EarthquakePushStorage.updateLastLowSeverityPushTime(applicationContext, now)
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "earthquake_push_work"
    }
}