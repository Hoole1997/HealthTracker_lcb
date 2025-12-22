package com.healthtracker.blood.suger.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.strategy.LoopPushManager
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import org.koin.core.context.GlobalContext

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

    override fun onReceive(context: Context, intent: Intent) {
        val loopPushManager = runCatching { GlobalContext.get().get<LoopPushManager>() }.getOrNull()
        if (loopPushManager == null) {
            "Koin not ready, skipping notification action".logw(PushOrchestrator.TAG)
            return
        }

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
            ACTION_NOTIFICATION_CLICKED -> handleNotificationClicked(loopPushManager, notificationId)
            ACTION_NOTIFICATION_DISMISSED -> handleNotificationDismissed(loopPushManager, notificationId)
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
    private fun handleNotificationClicked(loopPushManager: LoopPushManager, notificationId: Int) {
        if (BuildState.debug) {
            "Notification clicked: notificationId=$notificationId".logd(PushOrchestrator.TAG)
        }
        // 1. 停止 Loop 推送
        loopPushManager.stopLoopPush(notificationId, "clicked")
    }

    /**
     * 处理通知划掉事件
     *
     * 流程：
     * 1. 停止对应的 Loop 推送
     */
    private fun handleNotificationDismissed(loopPushManager: LoopPushManager, notificationId: Int) {
        if (BuildState.debug) {
            "Notification dismissed: notificationId=$notificationId".logd(PushOrchestrator.TAG)
        }

        // 停止 Loop 推送
        loopPushManager.stopLoopPush(notificationId, "dismissed")

        // 显式取消通知（应对点击 Close 按钮的情况）
        NotificationManagerCompat.from(App.INSTANCE).cancel(notificationId)
    }
}
