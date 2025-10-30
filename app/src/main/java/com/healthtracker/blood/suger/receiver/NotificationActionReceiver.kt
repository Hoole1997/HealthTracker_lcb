package com.healthtracker.blood.suger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtracker.blood.suger.push.recordLastClickNotifyTime
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.strategy.LoopPushManager
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 通知动作接收器
 *
 * 职责：
 * 1. 接收通知点击事件（ACTION_NOTIFICATION_CLICKED）
 * 2. 接收通知划掉事件（ACTION_NOTIFICATION_DISMISSED）
 * 3. 停止对应的 Loop 推送
 * 4. 点击事件：停止 Loop → 启动 SplashActivity
 * 5. 划掉事件：停止 Loop
 *
 * 工作流程：
 * - 点击通知：NotificationActionReceiver → 停止 Loop → 启动 SplashActivity
 * - 划掉通知：NotificationActionReceiver → 停止 Loop
 * - 切回前台：AppForegroundObserver → 停止所有 Loop
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationActionReceiver"

        // Action 常量
        const val ACTION_NOTIFICATION_CLICKED = "com.healthtracker.NOTIFICATION_CLICKED"
        const val ACTION_NOTIFICATION_DISMISSED = "com.healthtracker.NOTIFICATION_DISMISSED"

        // Extra 常量
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_ACTION_VALUE = "action_value"
    }

    @Inject
    lateinit var loopPushManager: LoopPushManager

    override fun onReceive(context: Context, intent: Intent) {
        if (BuildState.debug) {
            "onReceive: action=${intent.action}".logd(PushOrchestrator.TAG)
        }

        // 获取通知 ID
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId == -1) {
            "Invalid notification ID: $notificationId".logw(PushOrchestrator.TAG)
            return
        }

        when (intent.action) {
            ACTION_NOTIFICATION_CLICKED -> handleNotificationClicked(notificationId)
            ACTION_NOTIFICATION_DISMISSED -> handleNotificationDismissed(notificationId)
            else -> {
                "Unknown action: ${intent.action}".logw(PushOrchestrator.TAG)
            }
        }
    }

    /**
     * 处理通知点击事件
     *
     * 流程：
     * 1. 停止对应的 Loop 推送
     * 2. 启动 SplashActivity（传递 action_value 用于导航）
     */
    private fun handleNotificationClicked(notificationId: Int) {
        if (BuildState.debug) {
            "Notification clicked: notificationId=$notificationId".logd(PushOrchestrator.TAG)
        }
        recordLastClickNotifyTime()
        // 1. 停止 Loop 推送
        loopPushManager.stopLoopPush(notificationId, "clicked")

//        // 2. 获取 action_value 用于导航
//        val actionValue = intent.getStringExtra(EXTRA_ACTION_VALUE)
//            ?: HealthServiceConstants.ACTION_VALUE_HOMEPAGE
//
//        if (BuildState.debug) {
//            "Launching SplashActivity with action: $actionValue".logd(TAG)
//        }
//
//        // 3. 启动 SplashActivity
//        val activityIntent = Intent(context, SplashActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            putExtra(HealthServiceConstants.EXTRA_NOTIFICATION_ACTION, actionValue)
//        }
//
//        try {
//            context.startActivity(activityIntent)
//        } catch (e: Exception) {
//            "Failed to start SplashActivity: ${e.message}".logw(TAG)
//        }
    }

    /**
     * 处理通知划掉事件
     *
     * 流程：
     * 1. 停止对应的 Loop 推送
     */
    private fun handleNotificationDismissed(notificationId: Int) {
        if (BuildState.debug) {
            "Notification dismissed: notificationId=$notificationId".logd(PushOrchestrator.TAG)
        }

        // 停止 Loop 推送
        loopPushManager.stopLoopPush(notificationId, "dismissed")
    }
}
