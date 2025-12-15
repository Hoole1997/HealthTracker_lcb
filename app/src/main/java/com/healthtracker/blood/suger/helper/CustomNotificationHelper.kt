package com.healthtracker.blood.suger.helper

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.common.weather.cache.WeatherCacheManager
import com.android.common.weather.util.TemperaturePreferences
import com.android.common.weather.util.WeatherIconMapper
import com.android.common.weather.util.fahrenheitToCelsius
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_CONTENT
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_FROM
import com.healthtracker.blood.suger.constants.LANDING_NOTIFICATION_TITLE
import com.healthtracker.blood.suger.constants.PUSH_CLOSE_ACTION
import com.healthtracker.blood.suger.receiver.NotificationActionReceiver
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.service.HealthServiceConstants.EXTRA_NOTIFICATION_ACTION
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.blood.suger.ui.act.SplashActivity
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import net.corekit.core.report.ReportDataManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import com.android.common.weather.R as WeatherR

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
                11 -> HealthServiceConstants.ACTION_VALUE_WEATHER
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
            // 首次调用时创建渠道并缓存
            ensureChannelCreated()

            // 获取资源
            val notifResources = resourceMapper.getNotificationResources(pushMessage.iconType)
            val layoutResources = resourceMapper.getLayoutResources(pushMessage.iconType)

            val finalNotificationId = notificationId ?: (NOTIFICATION_ID_BASE + pushMessage.id.hashCode())
            // 创建 RemoteViews
            val collapsedView = createCollapsedView(pushMessage, notifResources, layoutResources)
            val expandedView = createExpandedView(pushMessage, notifResources, layoutResources, finalNotificationId)

            // 如果是天气通知布局，则绑定天气数据
            if (layoutResources.collapsedLayout == WeatherR.layout.layout_weather_notification_normal) {
                bindWeatherData(collapsedView, expandedView)
            }

            // 创建点击和删除 PendingIntent
            val clickIntent = createClickPendingIntent(pushMessage,finalNotificationId,scenario,isSilent)
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

            val notification = notificationBuilder.build()

            // 发送通知
            if (ActivityCompat.checkSelfPermission(
                    App.INSTANCE,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return 0
            }
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
        val isWeatherLayout =
            layoutResources.collapsedLayout == WeatherR.layout.layout_weather_notification_normal

        return RemoteViews(context.packageName, layoutResources.collapsedLayout).apply {
            if (isWeatherLayout) {
                // 天气通知使用独立布局，后续由 bindWeatherData 绑定数据，这里不触碰通用 id
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
        notificationId: Int
    ): RemoteViews {
        val isWeatherLayout =
            layoutResources.expandedLayout == WeatherR.layout.layout_weather_notification_big

        return RemoteViews(context.packageName, layoutResources.expandedLayout).apply {
            if (isWeatherLayout) {
                // 天气通知使用独立布局，后续由 bindWeatherData 绑定数据，这里不触碰通用 id
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
                setOnClickPendingIntent(R.id.iv_close, createDeletePendingIntent(notificationId))
            }

        }
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

    /**
     * 绑定天气数据到 RemoteViews
     */
    private fun bindWeatherData(collapsedView: RemoteViews, expandedView: RemoteViews?) {
        try {
            val weatherData = WeatherCacheManager.getCachedResponse() ?: return

            val context = context
            val isCelsius = TemperaturePreferences.isCelsius()

        // 1. 绑定当前天气 (Collapsed & Expanded)
        val currentCondition = weatherData.currentConditions?.firstOrNull()
        val currentTemp = currentCondition?.temperature?.imperial?.value?.let { 
            if (isCelsius) it.fahrenheitToCelsius() else it.roundToInt() 
        } ?: 0
        val weatherText = currentCondition?.weatherText ?: ""
        val weatherIconId = currentCondition?.weatherIcon ?: 1
        val weatherIconRes = WeatherIconMapper.getIconResource(weatherIconId)
        
        val locationName = weatherData.city?.localizedName ?: weatherData.city?.englishName ?: "Unknown"

        // 更新 Collapsed View
        with(collapsedView) {
            setTextViewText(WeatherR.id.tv_temperature, "$currentTemp°")
            setTextViewText(WeatherR.id.tv_weather, weatherText)
            setTextViewText(WeatherR.id.tv_location, locationName)
            setImageViewResource(WeatherR.id.iv_weather, weatherIconRes)
        }

        // 更新 Expanded View
        expandedView?.let { view ->
            // 基本信息
            view.setTextViewText(WeatherR.id.tv_temperature, "$currentTemp°")
            view.setTextViewText(WeatherR.id.tv_weather, weatherText)
            view.setTextViewText(WeatherR.id.tv_location, locationName)
            view.setImageViewResource(WeatherR.id.iv_weather, weatherIconRes)
            
            // 日期
            val dateFormat = SimpleDateFormat("EEE, MMM dd", LanguageUtils.getAppLocale(context))
            view.setTextViewText(WeatherR.id.tv_date, dateFormat.format(Date()))

            // 过滤过期数据 (同 WeatherActivity 逻辑)
            val forecasts = weatherData.dailyForecasts?.dailyForecasts ?: emptyList()
            val todayCalendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

//            val validForecasts = forecasts.filter { forecast ->
//                val forecastDateStr = forecast.date
//                if (forecastDateStr != null) {
//                    try {
//                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
//                        val date = isoFormat.parse(forecastDateStr)
//                        if (date != null) {
//                            val forecastCalendar = java.util.Calendar.getInstance()
//                            forecastCalendar.time = date
//                            forecastCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
//                            forecastCalendar.set(java.util.Calendar.MINUTE, 0)
//                            forecastCalendar.set(java.util.Calendar.SECOND, 0)
//                            forecastCalendar.set(java.util.Calendar.MILLISECOND, 0)
//                            !forecastCalendar.before(todayCalendar)
//                        } else true
//                    } catch (e: Exception) { true }
//                } else true
//            }

//            val finalForecasts = forecasts.ifEmpty { forecasts }

            // 今日的高低温 (High:21° Low:15°)
            val todayForecast = forecasts.firstOrNull()
            todayForecast?.let {
                val high = if (isCelsius) it.temperature?.maximum?.value?.fahrenheitToCelsius() else it.temperature?.maximum?.value?.roundToInt()
                val low = if (isCelsius) it.temperature?.minimum?.value?.fahrenheitToCelsius() else it.temperature?.minimum?.value?.roundToInt()
                view.setTextViewText(WeatherR.id.tv_temperature_range, "High:${high ?: "--"}° Low:${low ?: "--"}°")
            }

            // 5天预报列表
            view.removeAllViews(WeatherR.id.ll_forecast_container)
            forecasts.take(5).forEach { forecast ->
                val itemRemoteView = RemoteViews(context.packageName, WeatherR.layout.layout_notification_daily_item)
                
                // 日期 (Thu)
                forecast.date?.let { dateStr ->
                    try {
                        // 使用兼容 API < 24 的时区格式
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                        // 处理 +08:00 格式转换为 +0800
                        val normalizedDateStr = dateStr.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
                        val dayFormat = SimpleDateFormat("EEE", LanguageUtils.getAppLocale(context))
                        val date = isoFormat.parse(normalizedDateStr)
                        val dayStr = date?.let { dayFormat.format(it) } ?: "N/A"
                        itemRemoteView.setTextViewText(WeatherR.id.tv_date, dayStr)
                    } catch (e: Exception) {
                        itemRemoteView.setTextViewText(WeatherR.id.tv_date, "N/A")
                    }
                }
                
                // 图标
                forecast.day?.icon?.let { iconId ->
                    itemRemoteView.setImageViewResource(WeatherR.id.iv_weather, WeatherIconMapper.getIconResource(iconId))
                }
                
                // 温度范围 (Low/High)
                // 注意：DailyForecastAdapter 使用 $low/$high
                val high = if (isCelsius) forecast.temperature?.maximum?.value?.fahrenheitToCelsius() else forecast.temperature?.maximum?.value?.roundToInt()
                val low = if (isCelsius) forecast.temperature?.minimum?.value?.fahrenheitToCelsius() else forecast.temperature?.minimum?.value?.roundToInt()
                itemRemoteView.setTextViewText(WeatherR.id.tv_temperature_range, "${low ?: "--"}°/${high ?: "--"}°")
                
                // 降水概率
                val rainPercent = forecast.day?.precipitationProbability ?: 0
                itemRemoteView.setTextViewText(WeatherR.id.tv_rain_percent, "$rainPercent%")
                
                view.addView(WeatherR.id.ll_forecast_container, itemRemoteView)
            }
        }
        } catch (e: Exception) {
            "Failed to bind weather data: ${e.message}".loge(TAG)
        }
    }
}
