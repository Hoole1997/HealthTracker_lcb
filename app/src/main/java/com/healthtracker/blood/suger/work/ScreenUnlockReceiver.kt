package com.healthtracker.blood.suger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd

class ScreenUnlockReceiver : BroadcastReceiver() {
    companion object {
       private const val  TAG = "ScreenUnlockReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action) {
//            val currentTimeMillis = System.currentTimeMillis()
//            val triggerTime = lastTriggerTime
//            lastTriggerTime = currentTimeMillis
//
//            if (currentTimeMillis - triggerTime < 60 * 1000) {
//                return
//            }

            // 在这里调度WorkManager任务
            if(BuildState.debug) "ScreenUnlockReceiver onReceive".logd(TAG)
            HealthWorkTask.scheduleScanTask(context)
        }
    }
}