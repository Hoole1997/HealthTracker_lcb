package com.daily.health.manager.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.daily.health.manager.helper.NotificationHelper
import com.daily.health.manager.strategy.PushScenario
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd

class ScreenUnlockReceiver : BroadcastReceiver() {
    companion object {
       private const val  TAG = "ScreenUnlockReceiver"
    }
    override fun onReceive(context: Context, intent: Intent) {
        if (!NotificationFeatureSwitch.notificationsEnabled) {
            return
        }
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            // 在这里调度WorkManager任务
            if(BuildState.debug) "ScreenUnlockReceiver onReceive".logd(TAG)

            if (BuildState.debug) {
                "Screen unlock detected, triggering push".logd(TAG)
            }
            // 使用 goAsync() 处理异步操作
            // 这会给 BroadcastReceiver 额外的时间完成协程操作
            val pendingResult = goAsync()
            try {
                NotificationHelper.show(PushScenario.UNLOCK)

            }catch (e: Throwable){
                e.printStackTrace()
            }finally {
                pendingResult.finish()
            }

        }
    }


}
