package com.daily.health.manager.work

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.daily.health.manager.TokenUploadCtrl.uploadToken
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logw
import java.util.concurrent.TimeUnit


object HealthWorkTask {
    @JvmOverloads
    fun start(context: Context, tag: String, repeatInterval: Long = 15, repeatIntervalTimeUnit: TimeUnit = TimeUnit.MINUTES) {
        WorkManager.getInstance(context).apply {
            cancelAllWorkByTag(tag)
            enqueue(PeriodicWorkRequest.Builder(HealthWorker::class.java, repeatInterval, repeatIntervalTimeUnit).addTag(tag).build())
        }
        initUser()
    }

    private fun initUser() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                if (BuildState.debug) "[Buss] Fetching FCM registration token failed: ${task.exception}".logw(
                    "Worker"
                )
                return@OnCompleteListener
            }

            val token = task.result
            uploadToken(token)
        })

    }

    fun registerReceiver(context: Context) {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_SCREEN_ON)
        context.registerReceiver(ScreenUnlockReceiver(), filter)
    }
}