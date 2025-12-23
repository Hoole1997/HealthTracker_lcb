package com.daily.health.manager.helper

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.config.models.PushMessage
import com.daily.health.manager.constants.LANDING_NOTIFICATION_CONTENT
import com.daily.health.manager.constants.LANDING_NOTIFICATION_FROM
import com.daily.health.manager.constants.LANDING_NOTIFICATION_TITLE
import com.daily.health.manager.constants.PUSH_CLOSE_ACTION
import com.daily.health.manager.receiver.NotificationActionReceiver
import com.daily.health.manager.service.HealthServiceConstants
import com.daily.health.manager.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION
import com.daily.health.manager.strategy.PushOrchestrator
import com.daily.health.manager.strategy.PushScenario
import com.daily.health.manager.ui.act.SplashActivity
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager

/**
 * 自定义通知辅助类
 * 根据 PushMessage 配置构建和发送自定义通知
 */
class CustomNotificationHelper(
    private val context: Context,
    private val resourceMapper: NotificationResourceMapper,
    private val configManager: RemoteConfigManager
): BaseNotificationHelper(context) {

    companion object {
        private const val TAG = "CustomNotificationHelper"
        private const val NOTIFICATION_ID_BASE = 20000

        private const val KEY_ASSISTANT_CALL_INDEX = "assistant_call_index"
        private const val ASSISTANT_TYPE_PLACEHOLDER = "[type]"

        /**
         * 将 PushMessage.actionType 映射到 ACTION_VALUE 常量
         */
        fun mapActionType(actionType: Int): String {
            return when (actionType) {
                1 -> HealthServiceConstants.ACTION_VALUE_HOMEPAGE
                2 -> HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR
                3 -> HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE
                4 -> HealthServiceConstants.ACTION_VALUE_CHOLESTEROL
                5 -> HealthServiceConstants.ACTION_VALUE_BMI
                6 -> HealthServiceConstants.ACTION_VALUE_HEART_RATE
                7 -> HealthServiceConstants.ACTION_VALUE_HISTORY
                8 -> HealthServiceConstants.ACTION_VALUE_MEDICATION
                9 -> HealthServiceConstants.ACTION_VALUE_HYDRATION
                10 -> HealthServiceConstants.ACTION_VALUE_STEPS
                else -> {
                    "Unknown actionType: $actionType, defaulting to homepage".logd(PushOrchestrator.TAG)
                    HealthServiceConstants.ACTION_VALUE_HOMEPAGE
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
    @SuppressLint("FullScreenIntentPolicy")
    fun showCustomNotification(
        pushMessage: PushMessage,
        isSilent: Boolean = false,
        notificationId: Int? = null,
        scenario: PushScenario
    ): Int {
        try {
            if(pushMessage.actionType == 8){
                return 0
            }
            // 首次调用时创建渠道并缓存
            ensureChannelCreated()

            // 获取资源
            val notifResources = resourceMapper.getNotificationResources(pushMessage.iconType)
            val layoutResources = resourceMapper.getLayoutResources(pushMessage.iconType)

            val finalNotificationId = notificationId ?: (NOTIFICATION_ID_BASE + pushMessage.id.hashCode())

            val (displayMessage, clickMessage) = prepareAssistantCallIfNeeded(pushMessage)

            // 创建点击和删除 PendingIntent（避免重复创建导致重复上报）
            val clickIntent = createClickPendingIntent(clickMessage, finalNotificationId, scenario, isSilent)
            val deleteIntent = createDeletePendingIntent(finalNotificationId)

            // 创建 RemoteViews
            val collapsedView = createCollapsedView(displayMessage, notifResources, layoutResources, clickIntent, deleteIntent)
            val expandedView = createExpandedView(displayMessage, notifResources, layoutResources, finalNotificationId, clickIntent, deleteIntent)

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

            val notification = notificationBuilder.build()

            // 发送通知
            val hasPostNotificationsPermission = if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.checkSelfPermission(
                    App.INSTANCE,
                    "android.permission.POST_NOTIFICATIONS"
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (!hasPostNotificationsPermission) return 0
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
        layoutResources: NotificationResourceMapper.LayoutResources,
        clickIntent: PendingIntent,
        deleteIntent: PendingIntent
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutResources.collapsedLayout).apply {
            if (layoutResources.collapsedLayout == R.layout.ht_layout_assistant_notify) {
                setTextViewText(R.id.tv_time, pushMessage.title)
                setOnClickPendingIntent(R.id.iv_confirm, clickIntent)
                setOnClickPendingIntent(R.id.iv_close, deleteIntent)
                return@apply
            }

            // 设置背景（如果有）
            notifResources.decorIcon?.let { bg ->
                setImageViewResource(R.id.ic_bg_icon, bg)
            }

            notifResources.background?.let {
                setImageViewResource(R.id.iv_bg, it)
            }

            // 设置标题和按钮文字
            setTextViewText(R.id.tv_title, pushMessage.title)
            setTextViewText(R.id.tv_btn, pushMessage.buttonText)

            notifResources.btnTextColor?.let {
                setTextColor(R.id.tv_btn, ContextCompat.getColor(context, it))
            }
        }
    }

    /**
     * 创建展开状态的 RemoteViews
     */
    private fun createExpandedView(
        pushMessage: PushMessage,
        notifResources: NotificationResourceMapper.NotificationResources,
        layoutResources: NotificationResourceMapper.LayoutResources,
        notificationId: Int,
        clickIntent: PendingIntent,
        deleteIntent: PendingIntent
    ): RemoteViews {
        return RemoteViews(context.packageName, layoutResources.expandedLayout).apply {
            if (layoutResources.expandedLayout == R.layout.ht_layout_assistant_notify_big) {
                setTextViewText(R.id.tv_title, pushMessage.title)
                setTextViewText(R.id.tv_content, pushMessage.desc)
                setOnClickPendingIntent(R.id.ll_answer, clickIntent)
                setOnClickPendingIntent(R.id.ll_neglect, deleteIntent)
                setOnClickPendingIntent(R.id.iv_close, deleteIntent)
                return@apply
            }

            // 设置背景（如果有）
            notifResources.background?.let {
                setImageViewResource(R.id.iv_bg, it)
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
                setTextColor(R.id.tv_btn, ContextCompat.getColor(context, it))
            }
            if (canClose()) {
                if (BuildState.debug) "添加关闭按钮响应,${SpUtils.getString(PUSH_CLOSE_ACTION)}".logd(TAG)
                // Bind close button click to delete intent
                setOnClickPendingIntent(R.id.iv_close, deleteIntent)
            }

        }
    }

    private fun prepareAssistantCallIfNeeded(pushMessage: PushMessage): Pair<PushMessage, PushMessage> {
        // 仅当 iconType 和 actionType 同时为 12 时才进入助手来电轮播逻辑
        if (pushMessage.iconType != 12 || pushMessage.actionType != 12) {
            return pushMessage to pushMessage
        }

        val lastIndex = SpUtils.getInt(KEY_ASSISTANT_CALL_INDEX, -1)
        val nextIndex = (lastIndex + 1) % 3
        SpUtils.putInt(KEY_ASSISTANT_CALL_INDEX, nextIndex)

        val typeNameRes = when (nextIndex) {
            0 -> R.string.ht_blood_suger
            1 -> R.string.ht_blood_pressure
            else -> R.string.ht_heart_rate
        }
        val typeName = context.getString(typeNameRes)
        val displayDesc = pushMessage.desc.replace(ASSISTANT_TYPE_PLACEHOLDER, typeName)

        val convertedActionType = when (nextIndex) {
            0 -> 2
            1 -> 3
            else -> 6
        }

        val displayMessage = pushMessage.copy(desc = displayDesc)
        val clickMessage = displayMessage.copy(actionType = convertedActionType)
        return displayMessage to clickMessage
    }

    /**
     * 创建点击事件的 PendingIntent
     * 通过 NotificationActionReceiver 处理，以支持 Loop 推送停止
     */
    private fun createClickPendingIntent(pushMessage: PushMessage, notificationId: Int, scenario: PushScenario,isSilent: Boolean = false): PendingIntent {
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
                else -> "local_push"
            })
        }

        if(!isSilent){
            if(BuildState.debug) "非静默通知，上报事件".logd(PushOrchestrator.TAG)
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
        }else{
            if(BuildState.debug) "静默通知，不上报事件".logd(PushOrchestrator.TAG)
        }


        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE


        // 使用 getActivity 而不是 getBroadcast，符合 Android 10+ 要求
        return PendingIntent.getActivity(
            context,
            notificationId,  // 使用 notificationId 作为 requestCode 确保唯一性
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

    private  fun canClose() = SpUtils.getString(PUSH_CLOSE_ACTION) != "0"

}
