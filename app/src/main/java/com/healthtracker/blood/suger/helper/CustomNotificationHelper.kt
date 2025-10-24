package com.healthtracker.blood.suger.helper

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.hasOreo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自定义通知辅助类
 * 根据 PushMessage 配置构建和发送自定义通知
 */
@Singleton
class CustomNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourceMapper: NotificationResourceMapper
) {

    companion object {
        private const val TAG = "CustomNotificationHelper"
        private const val CHANNEL_ID_CUSTOM = "health_tracker_custom_push"
        private const val NOTIFICATION_ID_BASE = 20000
    }

    private val notificationManager: NotificationManager by lazy {
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
                val channel = NotificationChannel(
                    CHANNEL_ID_CUSTOM,
                    context.getString(R.string.custom_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.custom_notification_channel_description)
                    setShowBadge(true)
                    enableVibration(true)
                    enableLights(true)
                    // 启用弹出通知（Heads-up notification）
                    setBypassDnd(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                notificationManager.createNotificationChannel(channel)
                "Custom notification channel created".logd(TAG)

            } catch (e: Exception) {
                "Failed to create notification channel: ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 确保通知渠道已创建（线程安全的双重检查锁定）
     */
    private fun ensureChannelCreated() {
        if (!isChannelCreated) {
            synchronized(this) {
                if (!isChannelCreated) {
                    createNotificationChannel()
                    isChannelCreated = true
                }
            }
        }
    }

    /**
     * 显示自定义通知
     * @param pushMessage PushMessage 配置对象
     * @param isSilent 是否为静音通知（Loop推送使用）
     * @param notificationId 指定的通知ID（Loop推送复用），null则自动生成
     * @return 通知ID
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCustomNotification(
        pushMessage: PushMessage,
        isSilent: Boolean = false,
        notificationId: Int? = null
    ): Int {
        try {
            // 首次调用时创建渠道并缓存
            ensureChannelCreated()

            // 获取资源
            val notifResources = resourceMapper.getNotificationResources(pushMessage.iconType)
            val layoutResources = resourceMapper.getLayoutResources(pushMessage.iconType)

            // 映射 actionType 到 action value
            val actionValue = mapActionType(pushMessage.actionType)

            // 创建 RemoteViews
            val collapsedView = createCollapsedView(pushMessage, notifResources, layoutResources)
            val expandedView = createExpandedView(pushMessage, notifResources, layoutResources)

            // 创建点击和删除 PendingIntent
            val finalNotificationId = notificationId ?: (NOTIFICATION_ID_BASE + pushMessage.id.hashCode())
            val clickIntent = createClickPendingIntent(actionValue, finalNotificationId)
            val deleteIntent = createDeletePendingIntent(finalNotificationId)

            // 构建通知（根据 isSilent 参数决定是否静音）
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_CUSTOM)
                .setSmallIcon(notifResources.smallIcon)
                .setCustomContentView(collapsedView)
                .setCustomHeadsUpContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(clickIntent)
                .setDeleteIntent(deleteIntent)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            // 静音通知设置（Loop推送）
            if (isSilent) {
                notificationBuilder
                    .setSound(null)
                    .setVibrate(null)
                    .setDefaults(0)
                    .setOnlyAlertOnce(true)  // 关键：使用相同ID替换通知时不发声
            }

            val notification = notificationBuilder.build()

            // 发送通知
            NotificationManagerCompat.from(context).notify(finalNotificationId, notification)

            val silentTag = if (isSilent) "[Silent]" else ""
            "Custom notification shown $silentTag: ${pushMessage.title}, ID=$finalNotificationId".logd(TAG)

            return finalNotificationId

        } catch (e: Exception) {
            "Failed to show custom notification: ${e.message}".loge(TAG)
            return notificationId ?: -1
        }
    }

    /**
     * 创建折叠状态的 RemoteViews
     */
    private fun createCollapsedView(
        pushMessage: PushMessage,
        notifResources: NotificationResourceMapper.NotificationResources,
        layoutResources: NotificationResourceMapper.LayoutResources
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutResources.collapsedLayout).apply {
            // 设置背景（如果有）
            notifResources.decorIcon?.let { bg ->
                setImageViewResource(R.id.ic_bg_icon,bg)
            }

            notifResources.background?.let {
                setImageViewResource(R.id.iv_bg,it)
            }

            // 设置标题和按钮文字
            setTextViewText(R.id.tv_title, pushMessage.title)
            setTextViewText(R.id.tv_btn, pushMessage.buttonText)

            notifResources.btnTextColor?.let {
                setTextColor(R.id.tv_btn, ContextCompat.getColor(context,it))
            }

            // 药品通知特殊处理：设置 iv_type 图标
            if (pushMessage.iconType == 8) {
                notifResources.decorIcon?.let { icon ->
                    setImageViewResource(R.id.iv_type, icon)
                }
            }
        }
    }

    /**
     * 创建展开状态的 RemoteViews
     */
    private fun createExpandedView(
        pushMessage: PushMessage,
        notifResources: NotificationResourceMapper.NotificationResources,
        layoutResources: NotificationResourceMapper.LayoutResources
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutResources.expandedLayout).apply {
            // 设置背景（如果有）
            notifResources.background?.let {
                setImageViewResource(R.id.iv_bg,it)
            }

            // 设置标题、内容和按钮文字
            setTextViewText(R.id.tv_title, pushMessage.title)
            setTextViewText(R.id.tv_content, pushMessage.desc)
            setTextViewText(R.id.tv_btn, pushMessage.buttonText)

            // 设置大图标（如果有）
            notifResources.largeIcon?.let { icon ->
                setImageViewResource(R.id.iv_icon, icon)
                setViewVisibility(R.id.iv_icon, View.VISIBLE)
            } ?: run {
                setViewVisibility(R.id.iv_icon, View.GONE)
            }

            // 设置装饰图标（如果有）
            notifResources.decorIcon?.let { icon ->
                setImageViewResource(R.id.ic_bg_icon, icon)
                setViewVisibility(R.id.ic_bg_icon, View.VISIBLE)
            } ?: run {
                setViewVisibility(R.id.ic_bg_icon, View.GONE)
            }

            notifResources.btnTextColor?.let {
                setTextColor(R.id.tv_btn, ContextCompat.getColor(context,it))
            }
        }
    }

    /**
     * 创建点击事件的 PendingIntent
     * 通过 NotificationActionReceiver 处理，以支持 Loop 推送停止
     */
    private fun createClickPendingIntent(actionValue: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_CLICKED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_ACTION_VALUE, actionValue)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            flags
        )
    }

    /**
     * 创建删除事件的 PendingIntent
     * 用于处理用户划掉通知的情况
     */
    private fun createDeletePendingIntent(notificationId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_NOTIFICATION_DISMISSED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        return PendingIntent.getBroadcast(
            context,
            notificationId + 10000,  // 使用不同的 requestCode 避免冲突
            intent,
            flags
        )
    }

    /**
     * 将 PushMessage.actionType 映射到 ACTION_VALUE 常量
     */
    private fun mapActionType(actionType: Int): String {
        return when (actionType) {
            1 -> HealthServiceConstants.ACTION_VALUE_HOMEPAGE
            2 -> HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR
            3 -> HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE
            4 -> HealthServiceConstants.ACTION_VALUE_CHOLESTEROL
            5 -> HealthServiceConstants.ACTION_VALUE_BMI
            6 -> HealthServiceConstants.ACTION_VALUE_HEART_RATE
            7 -> HealthServiceConstants.ACTION_VALUE_HISTORY
            8 -> HealthServiceConstants.ACTION_VALUE_MEDICATION
            else -> {
                "Unknown actionType: $actionType, defaulting to homepage".logd(TAG)
                HealthServiceConstants.ACTION_VALUE_HOMEPAGE
            }
        }
    }
}
