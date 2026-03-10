package com.daily.health.manager.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.constants.PUSH_CLOSE_ACTION
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.helper.BloodSugarNotificationContent
import com.daily.health.manager.helper.NotificationResourceMapper.NotificationResources
import com.daily.health.manager.receiver.NotificationActionReceiver
import com.daily.health.manager.service.HealthServiceConstants
import com.daily.health.manager.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION
import com.daily.health.manager.face.act.SplashScreen
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.SpUtils

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
class AlarmNotificationManager(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "AlarmNotificationManager"
        
        // 统一的健康提醒通知渠道ID（合并了血糖和血压提醒）
        private const val CHANNEL_ID_ALARM = "health_tracker_reminder"
        
        // 通知ID基础值
        private const val NOTIFICATION_ID_BASE = 10000
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
                    context.getString(R.string.tr_notification_channel_alarm_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.tr_notification_channel_alarm_description)
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
            val icon = when (alarmRecord.type) {
                AlarmRecord.TYPE_BLOOD_PRESSURE -> R.drawable.tr_ic_notifcation_pb
                else -> R.drawable.ic_notification_bs
            }
            // 创建点击意图
            val clickIntent = createClickPendingIntent(alarmRecord.type, notificationId)

            val collapsedView = createCollapsedView(alarmRecord,notificationId)
            val expandedView = createExpandedView(alarmRecord, notificationId)

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
            val notification = notificationBuilder.build()
            
            val hasPostNotificationsPermission = if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.checkSelfPermission(
                    App.INSTANCE,
                    "android.permission.POST_NOTIFICATIONS"
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (!hasPostNotificationsPermission) return
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
        alarmRecord: AlarmRecord, notificationId: Int
    ): RemoteViews {
        val (time, title, desc, btnText) = getNotificationContent(alarmRecord)
        val notifResources = getAlarmNotificationRes(alarmRecord.type)
        
        // 血糖类型使用专属布局
        return if (alarmRecord.type == AlarmRecord.TYPE_BLOOD_SUGAR) {
            RemoteViews(context.packageName, R.layout.tr_layout_bs_notify).apply {
                setTextViewText(R.id.tv_time, time)
                setTextViewText(R.id.tv_time_type, title)
                setTextViewText(R.id.tv_action, btnText)
            }
        } else {
            RemoteViews(context.packageName, R.layout.tr_layout_meds_notify).apply {
                // 设置标题和按钮文字
                setTextViewText(R.id.tv_title, desc)
                setTextViewText(R.id.tv_time, time)
                notifResources?.decorIcon?.let { icon ->
                    setImageViewResource(R.id.iv_type, icon)
                }
                if(canClose()){
                    // Bind close button click to delete intent
                    setOnClickPendingIntent(R.id.iv_close, createDeletePendingIntent(notificationId))
                }
            }
        }
    }

    private fun canClose() = SpUtils.getString(PUSH_CLOSE_ACTION) != "0"

    /**
     * 创建展开状态的 RemoteViews
     */
    private fun createExpandedView(alarmRecord: AlarmRecord, notificationId: Int
    ): RemoteViews {
        val (time, title, desc, btnText) = getNotificationContent(alarmRecord)
        val notifResources = getAlarmNotificationRes(alarmRecord.type)
        
        // 血糖类型使用专属布局
        return if (alarmRecord.type == AlarmRecord.TYPE_BLOOD_SUGAR) {
            RemoteViews(context.packageName, R.layout.tr_layout_bs_notify_big).apply {
                setTextViewText(R.id.tv_time, time)
                setTextViewText(R.id.tv_time_des, desc)
                setTextViewText(R.id.tv_action, btnText)
            }
        } else {
            RemoteViews(context.packageName, R.layout.tr_layout_meds_notify_big).apply {
                // 设置背景（如果有）
                notifResources?.decorIcon?.let { bg ->
                    setImageViewResource(R.id.ic_bg_icon, bg)
                }
                // 设置标题和按钮文字
                setTextViewText(R.id.tv_title, time)
                setTextViewText(R.id.tv_content, desc)
                setTextViewText(R.id.tv_btn, btnText)
                notifResources?.decorIcon?.let { icon ->
                    setImageViewResource(R.id.iv_type, icon)
                }
                if(canClose()){
                    // Bind close button click to delete intent
                    setOnClickPendingIntent(R.id.iv_close, createDeletePendingIntent(notificationId))
                }
            }
        }
    }

    /**
     * 创建删除事件的 PendingIntent
     * 用于处理用户点击关闭按钮的情况
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
     * 获取通知内容
     * 
     * @param alarmRecord 闹钟记录
     * @return Triple(渠道ID, 标题, 内容)
     */
    /**
     * 通知内容数据类
     * @property time 格式化时间，如 "07:30"
     * @property title 标题，血糖场景为测量状态，如 "空腹"
     * @property desc 描述内容
     * @property btnText 按钮文字
     */
    private data class NotificationContent(
        val time: String,
        val title: String,
        val desc: String,
        val btnText: String
    )

    private fun getNotificationContent(alarmRecord: AlarmRecord): NotificationContent {
        return when (alarmRecord.type) {
            AlarmRecord.TYPE_BLOOD_SUGAR -> {
                // 血糖类型使用 BloodSugarNotificationContent 获取场景差异化文案
                val sceneId = alarmRecord.textExt1
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = context.getString(BloodSugarNotificationContent.getTitleResId(sceneId)),
                    desc = context.getString(BloodSugarNotificationContent.getDescResId(sceneId)),
                    btnText = context.getString(BloodSugarNotificationContent.getButtonResId())
                )
            }
            AlarmRecord.TYPE_BLOOD_PRESSURE -> {
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = "",
                    desc = context.getString(R.string.tr_alarm_blood_pressure_content),
                    btnText = context.getString(R.string.tr_record_now)
                )
            }
            AlarmRecord.TYPE_MEDICATION -> {
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = "",
                    desc = context.getString(R.string.tr_medication_reminder_content),
                    btnText = context.getString(R.string.tr_take_now)
                )
            }
            AlarmRecord.TYPE_HYDRATION -> {
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = "",
                    desc = context.getString(R.string.tr_alarm_hydration_content),
                    btnText = context.getString(R.string.tr_drink_now)
                )
            }
            AlarmRecord.TYPE_HEART_RATE -> {
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = "",
                    desc = context.getString(R.string.tr_alarm_heart_rate_content),
                    btnText = context.getString(R.string.tr_record_now)
                )
            }
            AlarmRecord.TYPE_BMI -> {
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = "",
                    desc = context.getString(R.string.tr_alarm_bmi_content),
                    btnText = context.getString(R.string.tr_record_now)
                )
            }
            AlarmRecord.TYPE_CHOLESTEROL -> {
                NotificationContent(
                    time = alarmRecord.getFormattedTime(),
                    title = "",
                    desc = context.getString(R.string.tr_alarm_cholesterol_content),
                    btnText = context.getString(R.string.tr_record_now)
                )
            }
            else -> {
                // 默认使用健康提醒
                NotificationContent(
                    time = context.getString(R.string.tr_alarm_default_title),
                    title = "",
                    desc = context.getString(R.string.tr_alarm_default_content),
                    btnText = context.getString(R.string.tr_view_now)
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
    private fun createClickPendingIntent(alarmType: Int, notificationId: Int): PendingIntent {
        // 将内部 action 映射到对应的参数值
        val actionValue = getNotificationAction(alarmType)

        // 直接创建启动 SplashActivity 的 Intent
        val intent = Intent(context, SplashScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NOTIFICATION_ACTION, actionValue)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // 使用 notificationId 作为 requestCode，避免同类型不同闹钟的 PendingIntent 互相覆盖
        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            flags
        )
    }


    private fun getNotificationAction(alarmType:Int) =  when(alarmType) {
        AlarmRecord.TYPE_BLOOD_SUGAR -> HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR
        AlarmRecord.TYPE_BLOOD_PRESSURE -> HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE
        AlarmRecord.TYPE_MEDICATION -> HealthServiceConstants.ACTION_VALUE_MEDICATION
        AlarmRecord.TYPE_HYDRATION -> HealthServiceConstants.ACTION_VALUE_HYDRATION
        AlarmRecord.TYPE_HEART_RATE -> HealthServiceConstants.ACTION_VALUE_HEART_RATE
        AlarmRecord.TYPE_BMI -> HealthServiceConstants.ACTION_VALUE_BMI
        AlarmRecord.TYPE_CHOLESTEROL -> HealthServiceConstants.ACTION_VALUE_CHOLESTEROL
        else -> null
    }
    
    private fun getAlarmNotificationRes(recordType:Int) = when(recordType){
        AlarmRecord.TYPE_BLOOD_SUGAR -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_home_card_bs,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_BLOOD_PRESSURE-> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_home_card_bp,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_MEDICATION -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_ic_meds_notify,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_HYDRATION -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_home_card_water,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_HEART_RATE -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_home_hero_heart,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_BMI -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_home_card_weight,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        AlarmRecord.TYPE_CHOLESTEROL -> NotificationResources(
            smallIcon = R.drawable.ic_notification_bs,
            background = R.drawable.tr_bg_rect_white_12,
            largeIcon = R.drawable.tr_ic_remind_notify,
            decorIcon = R.mipmap.tr_home_card_cholesterol,
            btnTextColor = com.healthtracker.framework.R.color.white
        )
        else -> null
    }
}