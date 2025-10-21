package com.healthtracker.blood.suger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.blood.suger.utils.isInteractive
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd

class PeriodicScanWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (BuildState.debug) "PeriodicScanWorker Run".logd("Worker")

        try {
            // 只在解锁状态下触发
            if (isInteractive(applicationContext)) {
                HealthWorkTask.scheduleScanTask(applicationContext)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return Result.success()
    }
}