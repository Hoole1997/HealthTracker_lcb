package com.healthtracker.blood.suger.alarm

import android.Manifest
import android.R.attr.action
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.compose.foundation.layout.Row
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.FsiConfig
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.helper.CustomNotificationHelper
import com.healthtracker.blood.suger.helper.NotificationResourceMapper
import com.healthtracker.blood.suger.helper.NotificationResourceMapper.NotificationResources
import com.healthtracker.blood.suger.push.canUpgradeToFullScreen
import com.healthtracker.blood.suger.push.recordAlarmTriggerCount
import com.healthtracker.blood.suger.push.recordTrigger
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION
import com.healthtracker.blood.suger.ui.act.FsiNotificationActivity
import com.healthtracker.blood.suger.ui.act.FsiNotificationActivity.Companion.EXTRA_ALARM_RECORD
import com.healthtracker.blood.suger.ui.act.FsiNotificationActivity.Companion.EXTRA_PUSH_MESSAGE
import com.healthtracker.blood.suger.ui.act.MedicationReminderFullScreenActivity
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 闹钟通知管理器
 * 负责创建和管理闹钟触发时的系统通知
 * 
 * 主要功能：
 * 1. 创建统一的健康提醒通知渠道（合并了血糖和血压提醒）
 * 2. 显示闹钟提醒通知（支持血糖、血压等不同类型）
 * 3. 处理通知点击事件
 * 4. 管理通知样式和内容
 * 5. 提供渠道状态查询和权限检查
 * 
 * 版本更新说明：
 * - v2.0: 将血糖和血压通知渠道合并为统一的健康提醒渠道
 * - 保持向后兼容性，旧的API仍可使用但已标记为过时
 */
@Singleton
class AlarmNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: PermissionManager,
    private val configManager: RemoteConfigManager
) {
    
    companion object {
        private const val TAG = "AlarmNotificationManager"
        
        // 统一的健康提醒通知渠道ID（合并了血糖和血压提醒）
        private const val CHANNEL_ID_ALARM = "health_tracker_reminder"
        
        // 通知ID基础值
        private const val NOTIFICATION_ID_BASE = 10000
        private const val SNOOZE_REQUEST_OFFSET = 50000
        const val PUSH_ALARM = "push_alarm"
    }
    
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道
     * Android 8.0+ 需要创建通知渠道
     * 
     * 注意：已将血糖和血压提醒合并为统一的健康提醒渠道，
     * 简化了渠道管理并提供更一致的用户体验
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // 统一的健康提醒通知渠道（合并了血糖和血压提醒）
                val alarmChannel = NotificationChannel(
                    CHANNEL_ID_ALARM,
                    context.getString(R.string.notification_channel_alarm_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_alarm_description)
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                }
                
                // 注册通知渠道
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                systemNotificationManager.createNotificationChannel(alarmChannel)
                
                "Unified health reminder notification channel created successfully".logd(TAG)
            } catch (e: Exception) {
                "Failed to create notification channel: ${e.message}".loge(TAG)
            }
        }
    }
    
    /**
     * 显示闹钟提醒通知
     * 
     * @param alarmRecord 闹钟记录
     */
    fun showAlarmNotification(alarmRecord: AlarmRecord) {
        try {

            val notificationId = generateNotificationId(alarmRecord)
            val icon = if(alarmRecord.type == AlarmRecord.TYPE_BLOOD_PRESSURE) R.drawable.ic_notifcation_pb else R.drawable.ic_notification_bs
            // 创建点击意图
            val clickIntent = createClickPendingIntent(alarmRecord.type)

            val collapsedView = createCollapsedView(alarmRecord)
            val expandedView = createExpandedView(alarmRecord)

            // 构建通知（根据 isSilent 参数决定是否静音）
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
                .setSmallIcon(icon)
                .setCustomContentView(collapsedView)
                .setCustomHeadsUpContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(clickIntent)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
            if(canUpgradeToFullScreen(configManager.getConfig<FsiConfig>(),true)){
                val (time,des,btnText) = getNotificationContent(alarmRecord)
                val fullScreenIntent = Intent(context, FsiNotificationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(EXTRA_PUSH_MESSAGE, PushMessage(PUSH_ALARM,time,des,btnText,0,0))
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID,notificationId)
                    putExtra(EXTRA_NOTIFICATION_ACTION,getNotificationAction(alarmRecord.type))
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                recordAlarmTriggerCount()
                notificationBuilder.setFullScreenIntent(fullScreenPendingIntent,true)
            }

            val notification = notificationBuilder.build()
            
            // 显示通知
            if (ActivityCompat.checkSelfPermission(
                    App.INSTANCE,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notificationManager.notify(notificationId, notification)
            
            "Alarm notification shown: ID=${alarmRecord.id}, Type=${alarmRecord.type}".logd(TAG)
        } catch (e: Exception) {
            "Failed to show alarm notification: ${e.message}".loge(TAG)
        }
    }

    /**
     * 创建折叠状态的 RemoteViews
     */
    private fun createCollapsedView(
        alarmRecord: AlarmRecord,
    ): RemoteViews {
        val (time,des,btnText) = getNotificationContent(alarmRecord)
        val notifResources = getAlarmNotificationRes(alarmRecord.type)
        return RemoteViews(context.packageName, R.layout.layout_meds_notify).apply {

            // 设置标题和按钮文字
            setTextViewText(R.id.tv_title, des)
            setTextViewText(R.id.tv_btn,btnText)
            notifResources?.decorIcon?.let { icon ->
                setImageViewResource(R.id.iv_type, icon)
            }
        }
    }

    /**
     * 创建展开状态的 RemoteViews
     */
    private fun createExpandedView(alarmRecord: AlarmRecord
    ): RemoteViews {
        val (time,des,btnText) = getNotificationContent(alarmRecord)
        val notifResources = getAlarmNotificationRes(alarmRecord.type)
        return RemoteViews(context.packageName, R.layout.layout_meds_notify_big).apply {
           // 设置背景（如果有）
            notifResources?.decorIcon?.let { bg ->
                setImageViewResource(R.id.ic_bg_icon,bg)
            }


            // 设置标题和按钮文字
            setTextViewText(R.id.tv_title, time)
            setTextViewText(R.id.tv_content, des)
            setTextViewText(R.id.tv_btn,btnText)
            notifResources?.decorIcon?.let { icon ->
                setImageViewResource(R.id.iv_type, icon)
            }
        }
    }
    
    /**
     * 取消指定闹钟的通知
     * 
     * @param alarmRecord 闹钟记录
     */
    fun cancelAlarmNotification(alarmRecord: AlarmRecord) {
        try {
            val notificationId = generateNotificationId(alarmRecord)
            notificationManager.cancel(notificationId)
            "Alarm notification cancelled: ID=${alarmRecord.id}".logd(TAG)
        } catch (e: Exception) {
            "Failed to cancel alarm notification: ${e.message}".loge(TAG)
        }
    }
    
    /**
     * 取消所有闹钟通知
     */
    fun cancelAllAlarmNotifications() {
        try {
            notificationManager.cancelAll()
            "All alarm notifications cancelled".logd(TAG)
        } catch (e: Exception) {
            "Failed to cancel all alarm notifications: ${e.message}".loge(TAG)
        }
    }
    
    /**
     * 检查通知权限是否已授权
     * 
     * @return 是否已授权
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * 获取通知内容
     * 
     * @param alarmRecord 闹钟记录
     * @return Triple(渠道ID, 标题, 内容)
     */
    private fun getNotificationContent(alarmRecord: AlarmRecord): Triple<String, String, String> {
        return when (alarmRecord.type) {
            AlarmRecord.TYPE_BLOOD_SUGAR -> {
                Triple(

                    alarmRecord.getFormattedTime(),
                    context.getString(R.string.alarm_blood_sugar_content),
                    context.getString(R.string.record_now)

                )
            }
            AlarmRecord.TYPE_BLOOD_PRESSURE -> {
                Triple(

                    alarmRecord.getFormattedTime(),
                    context.getString(R.string.alarm_blood_pressure_content),
                    context.getString(R.string.record_now)
                )
            }
            AlarmRecord.TYPE_MEDICATION -> {
                Triple(

                    alarmRecord.getFormattedTime(),
                    context.getString(R.string.medication_reminder_content),
                    context.getString(R.string.take_now)
                )
            }
            AlarmRecord.TYPE_HYDRATION -> {
                Triple(
                    alarmRecord.getFormattedTime(),
                    context.getString(R.string.alarm_hydration_content),
                    context.getString(R.string.drink_now)
                )
            }
            else -> {
                // 默认使用健康提醒
                Triple(

                    context.getString(R.string.alarm_default_title), 
                    context.getString(R.string.alarm_default_content),
                    context.getString(R.string.view_now),

                    )
            }
        }
    }
    
    /**
     * 生成通知ID
     * 使用闹钟ID确保每个闹钟有唯一的通知ID
     * 
     * @param alarmRecord 闹钟记录
     * @return 通知ID
     */
    private fun generateNotificationId(alarmRecord: AlarmRecord): Int {
        return NOTIFICATION_ID_BASE + alarmRecord.id.toInt()
    }
    


    /**
     * 创建点击事件的PendingIntent
     * 直接启动 SplashActivity，不再通过 BroadcastReceiver 中转
     * 这样可以避免 Android 10+ 从后台启动 Activity 的限制
     */
    private fun createClickPendingIntent(alarmType: Int): PendingIntent {
        // 将内部 action 映射到对应的参数值
        val actionValue = getNotificationAction(alarmType)

        // 直接创建启动 SplashActivity 的 Intent
        val intent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NOTIFICATION_ACTION, actionValue)
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


    private fun getNotificationAction(alarmType:Int) =  when(alarmType) {
        AlarmRecord.TYPE_BLOOD_SUGAR -> HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR
        AlarmRecord.TYPE_BLOOD_PRESSURE -> HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE
        AlarmRecord.TYPE_MEDICATION -> HealthServiceConstants.ACTION_VALUE_MEDICATION
        AlarmRecord.TYPE_HYDRATION -> HealthServiceConstants.ACTION_VALUE_HYDRATION
        else -> null
    }
    
    /**
     * 获取通知渠道状态
     * 
     * @return 通知渠道信息
     */
    fun getNotificationChannelStatus(): NotificationChannelStatus {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val alarmChannel = systemNotificationManager.getNotificationChannel(CHANNEL_ID_ALARM)
            val isChannelEnabled = alarmChannel?.importance != NotificationManager.IMPORTANCE_NONE
            
            NotificationChannelStatus(
                alarmChannelEnabled = isChannelEnabled,
                notificationsEnabled = areNotificationsEnabled()
            )
        } else {
            // Android 8.0以下版本没有通知渠道概念，默认启用
            NotificationChannelStatus(
                alarmChannelEnabled = true,
                notificationsEnabled = areNotificationsEnabled()
            )
        }
    }

    // ==================== FSI通知管理 ====================

    /**
     * 创建服药提醒通知
     * 优先使用FSI，权限不可用时降级到增强标准通知
     */
    fun createMedicationNotification(
        medicationName: String,
        dosage: String = "",
        notes: String = "",
        reminderTime: String = "",
        reminderId: Long = -1L
    ) {
        if (permissionManager.isFSIPermissionAvailable()) {
            createFSINotification(medicationName, dosage, notes, reminderTime, reminderId)
        } else {
            createEnhancedStandardNotification(medicationName, dosage, notes, reminderTime, reminderId)
        }
    }

    /**
     * 创建FSI通知
     */
    private fun createFSINotification(
        medicationName: String,
        dosage: String,
        notes: String,
        reminderTime: String,
        reminderId: Long
    ) {
        try {
            // 创建FSI Activity的Intent
            val fullScreenIntent = Intent(context, MedicationReminderFullScreenActivity::class.java).apply {
                putExtra(MedicationReminderFullScreenActivity.EXTRA_MEDICATION_NAME, medicationName)
                putExtra(MedicationReminderFullScreenActivity.EXTRA_DOSAGE, dosage)
                putExtra(MedicationReminderFullScreenActivity.EXTRA_NOTES, notes)
                putExtra(MedicationReminderFullScreenActivity.EXTRA_REMINDER_TIME, reminderTime)
                putExtra(MedicationReminderFullScreenActivity.EXTRA_REMINDER_ID, reminderId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (NOTIFICATION_ID_BASE + reminderId).toInt(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 使用公共方法创建普通点击Intent（作为fallback）
            val clickPendingIntent = createMedicationClickIntent(reminderId)

            // 使用公共方法生成统一的标题和内容
            val (notificationTitle, contentText) = generateMedicationNotificationContent(medicationName, reminderTime)

            // 使用公共方法创建基础通知构建器，然后添加FSI特定配置
            val notification = createBaseMedicationNotificationBuilder(notificationTitle, contentText, clickPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)  // FSI关键配置
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(false)
                .build()

            // 使用公共方法显示通知
            showNotificationWithPermissionCheck(
                notification,
                reminderId,
                "FSI medication notification shown: $medicationName"
            )

        } catch (e: Exception) {
            "Failed to create FSI notification: ${e.message}".loge(TAG)
            // 降级到标准通知
            createEnhancedStandardNotification(medicationName, dosage, notes, reminderTime, reminderId)
        }
    }

    /**
     * 创建增强标准通知（FSI权限不可用时的降级方案）
     */
    private fun createEnhancedStandardNotification(
        medicationName: String,
        dosage: String,
        notes: String,
        reminderTime: String,
        reminderId: Long
    ) {
        try {
            // 使用公共方法创建点击Intent
            val clickPendingIntent = createMedicationClickIntent(reminderId)

            // 使用公共方法生成统一的标题和内容
            val (notificationTitle, contentText) = generateMedicationNotificationContent(medicationName, reminderTime)

            // 使用公共方法创建基础通知构建器，然后添加增强标准通知特定配置
            val notification = createBaseMedicationNotificationBuilder(notificationTitle, contentText, clickPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)  // 不可滑动删除
                .setOngoing(true)      // 持续显示
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                // 增强音效和震动
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            // 使用公共方法显示通知
            showNotificationWithPermissionCheck(
                notification,
                reminderId,
                "Enhanced standard medication notification shown: $medicationName"
            )

        } catch (e: Exception) {
            "Failed to create enhanced standard notification: ${e.message}".loge(TAG)
        }
    }

    /**
     * 生成药物提醒通知的标题和内容
     * @param medicationName 药物名称
     * @param reminderTime 提醒时间
     * @return Pair<标题, 内容>
     */
    private fun generateMedicationNotificationContent(
        medicationName: String,
        reminderTime: String
    ): Pair<String, String> {
        val title = if (reminderTime.isNotEmpty()) {
            context.getString(R.string.medication_notification_title_with_time, medicationName, reminderTime)
        } else {
            medicationName
        }
        val content = context.getString(R.string.medication_reminder_content)
        return Pair(title, content)
    }

    /**
     * 创建通用的点击Intent
     * @param reminderId 提醒ID
     * @return PendingIntent
     */
    private fun createMedicationClickIntent(reminderId: Long): PendingIntent {
        val clickIntent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("medication_reminder", true)
            putExtra("reminder_id", reminderId)
        }
        return PendingIntent.getActivity(
            context,
            (NOTIFICATION_ID_BASE + reminderId).toInt(),
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 构建药物提醒通知的基础Builder
     * @param title 通知标题
     * @param content 通知内容
     * @param clickIntent 点击Intent
     * @return NotificationCompat.Builder
     */
    private fun createBaseMedicationNotificationBuilder(
        title: String,
        content: String,
        clickIntent: PendingIntent
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
            .setSmallIcon(R.drawable.ic_meds)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(clickIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    /**
     * 显示通知（统一的权限检查和显示逻辑）
     * @param notification 通知对象
     * @param reminderId 提醒ID
     * @param logMessage 日志消息
     */
    private fun showNotificationWithPermissionCheck(
        notification: Notification,
        reminderId: Long,
        logMessage: String
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify((NOTIFICATION_ID_BASE + reminderId).toInt(), notification)
            logMessage.logd(TAG)
        }
    }

    /**
     * 取消服药提醒通知
     */
    fun cancelMedicationNotification(reminderId: Long) {
        try {
            notificationManager.cancel((NOTIFICATION_ID_BASE + reminderId).toInt())
            "Medication notification cancelled: ID=$reminderId".logd(TAG)
        } catch (e: Exception) {
            "Failed to cancel medication notification: ${e.message}".loge(TAG)
        }
    }

    private fun getAlarmNotificationRes(recordType:Int) = when(recordType){
        AlarmRecord.TYPE_BLOOD_SUGAR -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.bg_rect_white_12,
            largeIcon = R.drawable.ic_remind_notify,
            decorIcon = R.mipmap.ic_bs_notify_icon,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_BLOOD_PRESSURE-> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.bg_rect_white_12,
            largeIcon = R.drawable.ic_remind_notify,
            decorIcon = R.mipmap.ic_bp_notify_icon,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_MEDICATION -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.bg_rect_white_12,
            largeIcon = R.drawable.ic_remind_notify,
            decorIcon = R.mipmap.ic_meds_notify,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_HYDRATION -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.bg_rect_white_12,
            largeIcon = R.drawable.ic_remind_notify,
            decorIcon = R.mipmap.ic_hydrate_noti,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        else -> null
    }
}

/**
 * 通知渠道状态
 * 
 * @property alarmChannelEnabled 健康提醒通知渠道是否启用
 * @property notificationsEnabled 应用通知权限是否启用
 */
data class NotificationChannelStatus(
    val alarmChannelEnabled: Boolean,
    val notificationsEnabled: Boolean
) {
    /**
     * 所有通知是否都已启用
     */
    val allEnabled: Boolean
        get() = alarmChannelEnabled && notificationsEnabled

}