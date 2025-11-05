package com.healthtracker.blood.suger.helper

import android.Manifest
import android.R.attr.type
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.FsiConfig
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_CONTENT
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_FROM
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_TITLE
import com.healthtracker.blood.suger.push.canUpgradeToFullScreen
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver.Companion.EXTRA_ACTION_VALUE
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.blood.suger.ui.act.FsiNotificationActivity
import com.healthtracker.blood.suger.ui.act.FsiNotificationActivity.Companion.EXTRA_PUSH_MESSAGE
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.qualifiers.ApplicationContext
import net.corekit.core.report.ReportDataManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自定义通知辅助类
 * 根据 PushMessage 配置构建和发送自定义通知
 */
@Singleton
class CustomNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourceMapper: NotificationResourceMapper,
    private val configManager: RemoteConfigManager
): BaseNotificationHelper(context) {

    companion object {
        private const val TAG = "CustomNotificationHelper"
        private const val NOTIFICATION_ID_BASE = 20000

    }




    /**
     * 显示自定义通知
     * @param pushMessage PushMessage 配置对象
     * @param isSilent 是否为静音通知（Loop推送使用）
     * @param notificationId 指定的通知ID（Loop推送复用），null则自动生成
     * @return 通知ID
     */
    @SuppressLint("FullScreenIntentPolicy")
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCustomNotification(
        pushMessage: PushMessage,
        isSilent: Boolean = false,
        notificationId: Int? = null,
        scenario: PushScenario
    ): Int {
        try {
            // 首次调用时创建渠道并缓存
            ensureChannelCreated()

            // 获取资源
            val notifResources = resourceMapper.getNotificationResources(pushMessage.iconType)
            val layoutResources = resourceMapper.getLayoutResources(pushMessage.iconType)

            // 创建 RemoteViews
            val collapsedView = createCollapsedView(pushMessage, notifResources, layoutResources)
            val expandedView = createExpandedView(pushMessage, notifResources, layoutResources)

            // 创建点击和删除 PendingIntent
            val finalNotificationId = notificationId ?: (NOTIFICATION_ID_BASE + pushMessage.id.hashCode())
            val clickIntent = createClickPendingIntent(pushMessage,finalNotificationId,scenario)
            val deleteIntent = createDeletePendingIntent(finalNotificationId)

            val channelId = if(isSilent) CHANNEL_ID_GENERAL_SILENT else CHANNEL_ID_GENERAL
            // 构建通知（根据 isSilent 参数决定是否静音）
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
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

            if((scenario == PushScenario.BACKGROUND || scenario == PushScenario.FCM) && canUpgradeToFullScreen(configManager.getConfig<FsiConfig>())){
                val fullScreenIntent = Intent(context, FsiNotificationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(EXTRA_PUSH_MESSAGE, pushMessage)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID,finalNotificationId)
                    putExtra(EXTRA_NOTIFICATION_ACTION,mapActionType(pushMessage.actionType))
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    finalNotificationId,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                notificationBuilder.setFullScreenIntent(fullScreenPendingIntent,true)
            }

            val notification = notificationBuilder.build()

            // 发送通知
            NotificationManagerCompat.from(context).notify(finalNotificationId, notification)

            val silentTag = if (isSilent) "[Silent]" else ""
            "Custom notification shown $silentTag: ${pushMessage.title}, ID=$finalNotificationId".logd(
                PushOrchestrator.TAG
            )

            return finalNotificationId

        } catch (e: Exception) {
            "Failed to show custom notification: ${e.message}".loge(PushOrchestrator.TAG)
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
    private fun createClickPendingIntent(pushMessage: PushMessage, notificationId: Int, scenario: PushScenario): PendingIntent {
        val actionType = pushMessage.actionType
        if(BuildState.debug) "createClickPendingIntent actionType = $actionType,notificationId = $notificationId".logd(PushOrchestrator.TAG)
        // 直接创建启动 SplashActivity 的 Intent
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_NOTIFICATION_ACTION,mapActionType(actionType))
            putExtra(LANDING_NOTIFICATION_TITLE,pushMessage.title)
            putExtra(LANDING_NOTIFICATION_CONTENT,pushMessage.desc)
            putExtra(LANDING_NOTIFICATION_FROM,when(scenario){
                PushScenario.FCM -> "firebase_push"
                PushScenario.BACKGROUND, PushScenario.UNLOCK -> "background_push"
                else -> "local_push"
            })
        }

        ReportDataManager.reportData(
            "Notific_Show", mapOf(
                "Notific_Type" to when (scenario) {
                    PushScenario.UNLOCK -> 1
                    PushScenario.BACKGROUND -> 1
                    PushScenario.KEEPALIVE -> 1
                    PushScenario.FCM -> 3
                    else -> 4
                },
                "Notific_Position" to 1,
                "Notific_Priority" to "PRIORITY_HIGH",
                "event_id" to "customer_general_style",
                "title" to pushMessage.title,
                "text" to pushMessage.desc,
            )
        )

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE


        // 使用 getActivity 而不是 getBroadcast，符合 Android 10+ 要求
        return PendingIntent.getActivity(
            context,
            notificationId.hashCode(),  // 使用action的hashCode作为requestCode确保唯一性
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
                "Unknown actionType: $actionType, defaulting to homepage".logd(PushOrchestrator.TAG)
                HealthServiceConstants.ACTION_VALUE_HOMEPAGE
            }
        }
    }
}
