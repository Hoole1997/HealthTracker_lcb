package com.healthtracker.blood.suger.work

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


object HealthWorkTask {


    @JvmOverloads
    fun start(context: Context, tag: String, repeatInterval: Long = 15, repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES) {
        WorkManager.getInstance(context).apply {
            cancelAllWorkByTag(tag)
            enqueue(PeriodicWorkRequest.Builder(PeriodicScanWorker::class.java, repeatInterval, repeatIntervalTimeUnit).addTag(tag).build())
        }
    }

    fun registerReceiver(context: Context) {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        context.registerReceiver(ScreenUnlockReceiver(), filter)
    }
}