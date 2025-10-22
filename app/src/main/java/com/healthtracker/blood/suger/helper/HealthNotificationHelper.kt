package com.healthtracker.blood.suger.helper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康服务通知辅助类
 * 负责创建和管理常驻通知
 */
@Singleton
class HealthNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager
) {

    companion object {
        private const val TAG = "HealthNotificationHelper"
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * 创建通知渠道
     * Android 8.0+ 需要
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    HealthServiceConstants.CHANNEL_ID_HEALTH_SERVICE,
                    context.getString(R.string.health_service_channel_name),
                    NotificationManager.IMPORTANCE_LOW  // 低重要性，不打扰用户
                ).apply {
                    description = context.getString(R.string.health_service_channel_description)
                    setShowBadge(false)  // 不显示角标
                    enableVibration(false)  // 不震动
                    setSound(null, null)  // 无声音
                }

                notificationManager.createNotificationChannel(channel)
                "Health service notification channel created".logd(TAG)

            } catch (e: Exception) {
                "Failed to create notification channel: ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 构建常驻通知
     */
    fun buildNotification(): Notification {
        // 创建自定义通知布局
        val remoteViews = RemoteViews(
            context.packageName,
            R.layout.layout_resident_notify
        )

        // 设置三个区域的点击事件
        remoteViews.setOnClickPendingIntent(
            R.id.ll_bs,
            createClickPendingIntent(HealthServiceConstants.ACTION_BLOOD_SUGAR)
        )

        remoteViews.setOnClickPendingIntent(
            R.id.ll_bp,
            createClickPendingIntent(HealthServiceConstants.ACTION_BLOOD_PRESSURE)
        )

        remoteViews.setOnClickPendingIntent(
            R.id.ll_hr,
            createClickPendingIntent(HealthServiceConstants.ACTION_HEART_RATE)
        )

        // 构建通知
        return NotificationCompat.Builder(context, HealthServiceConstants.CHANNEL_ID_HEALTH_SERVICE)
            .setSmallIcon(R.drawable.ic_notification_bs)  // 使用项目中的通知图标
            .setCustomContentView(remoteViews)  // 设置自定义布局
            .setCustomBigContentView(remoteViews)  // 展开后也使用相同布局
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // 高优先级
            .setOngoing(true)  // 常驻通知，不可滑动删除
            .setAutoCancel(false)  // 不自动取消
            .setShowWhen(false)  // 不显示时间
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // 锁屏可见
            .build()
    }

    /**
     * 创建点击事件的PendingIntent
     * 直接启动 SplashActivity，不再通过 BroadcastReceiver 中转
     * 这样可以避免 Android 10+ 从后台启动 Activity 的限制
     */
    private fun createClickPendingIntent(action: String): PendingIntent {
        // 将内部 action 映射到对应的参数值
        val actionValue = when(action) {
            HealthServiceConstants.ACTION_BLOOD_SUGAR -> HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR
            HealthServiceConstants.ACTION_BLOOD_PRESSURE -> HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE
            HealthServiceConstants.ACTION_HEART_RATE -> HealthServiceConstants.ACTION_VALUE_HEART_RATE
            else -> null
        }

        // 直接创建启动 SplashActivity 的 Intent
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(HealthServiceConstants.EXTRA_NOTIFICATION_ACTION, actionValue)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // 使用 getActivity 而不是 getBroadcast，符合 Android 10+ 要求
        return PendingIntent.getActivity(
            context,
            action.hashCode(),  // 使用action的hashCode作为requestCode确保唯一性
            intent,
            flags
        )
    }
}
