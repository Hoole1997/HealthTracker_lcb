package com.healthtracker.blood.suger.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.hasOreo

open class BaseNotificationHelper(private val context: Context) {
    companion object{
        private const val TAG = "BaseNotificationHelper"
        const val CHANNEL_NAME_GENERAL = "recovery_single"
        const val CHANNEL_NAME_GENERAL_SILENT = "recovery_loop"
        const val CHANNEL_ID_GENERAL = "general_notification"
        const val CHANNEL_ID_GENERAL_SILENT = "general_silent_notification"

        const val CHANNEL_NAME_RESIDENT = "recovery_resident"
        const val CHANNEL_ID_RESIDENT = "resident_notification"
    }

    protected val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    // 渠道创建标志，使用线程安全的懒加载模式
    @Volatile
    private var isChannelCreated = false



    /**
     * 创建通知渠道（懒加载初始化 + 缓存）
     * Android 8.0+ 需要
     */
    private fun createNotificationChannel() {
        if (hasOreo()) {
            try {

                // 常驻通知通道
                val residentChannel = NotificationChannel(
                    HealthServiceConstants.CHANNEL_ID_HEALTH_SERVICE,
                    context.getString(R.string.health_service_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.health_service_channel_description)
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
                // 普通通知通道
                val generalChannel = NotificationChannel(
                    CHANNEL_ID_GENERAL, CHANNEL_NAME_GENERAL, NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "recovery_single"
                    setShowBadge(true)
                    enableLights(false)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                // 静音通知通道
                val silentChannel = NotificationChannel(
                    CHANNEL_ID_GENERAL_SILENT,
                    CHANNEL_NAME_GENERAL_SILENT,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = " recovery_loop "
                    setShowBadge(true)
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)  // 设置为静音
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannels(listOf(silentChannel,generalChannel,residentChannel))
                "Custom notification channel created".logd(TAG)
            } catch (e: Exception) {
                "Failed to create notification channel: ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 确保通知渠道已创建（线程安全的双重检查锁定）
     */
    protected fun ensureChannelCreated() {
        if (!isChannelCreated) {
            synchronized(this) {
                if (!isChannelCreated) {
                    createNotificationChannel()
                    isChannelCreated = true
                }
            }
        }
    }
}