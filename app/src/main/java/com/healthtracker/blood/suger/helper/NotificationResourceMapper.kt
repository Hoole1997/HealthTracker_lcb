package com.healthtracker.blood.suger.helper

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.AlarmRecord

/**
 * 通知资源映射器
 * 根据 PushMessage 的 iconType 映射到对应的通知 UI 资源
 */
class NotificationResourceMapper {

    /**
     * 通知资源集合
     * @param smallIcon 状态栏小图标（必需）
     * @param background 通知背景 drawable（可选）
     * @param largeIcon 通知内大图标（可选）
     * @param decorIcon 装饰性背景图标（可选）
     */
    data class NotificationResources(
        val smallIcon: Int,
        val background: Int?,
        val largeIcon: Int?,
        val decorIcon: Int?,
        val btnTextColor:Int? = null
    )

    /**
     * 布局资源集合
     * @param collapsedLayout 折叠状态布局
     * @param expandedLayout 展开状态布局
     */
    data class LayoutResources(
        val collapsedLayout: Int,
        val expandedLayout: Int
    )

    /**
     * 根据 iconType 获取通知资源
     */
    fun getNotificationResources(iconType: Int): NotificationResources {
        return when (iconType) {
            1 -> NotificationResources(
                smallIcon = R.drawable.ic_notification_bs,
                background = R.drawable.bg_hr_notify,
                largeIcon = R.drawable.ic_homepage_notify,
                decorIcon = R.drawable.bg_homepage_notify_icon,
                btnTextColor = R.color.color_FF4420
            )
            2 -> NotificationResources(
                smallIcon = R.drawable.ic_notification_bs,
                background = R.drawable.bg_bs_notify,
                largeIcon = R.drawable.ic_bs_notify,
                decorIcon = R.mipmap.bg_bs_notify_icon,
                btnTextColor = R.color.color_02BC77
            )
            3 -> NotificationResources(
                smallIcon = R.drawable.ic_notifcation_pb,
                background = R.drawable.bg_bp_notify,
                largeIcon = R.drawable.ic_bp_notify,
                decorIcon = R.mipmap.bg_bp_notify_icon,
                btnTextColor = R.color.color_2AA1FC
            )
            4 -> NotificationResources(
                smallIcon = R.drawable.ic_cholesterol_notify,
                background = R.drawable.bg_cholesterol_notify,
                largeIcon = R.drawable.ic_cholesterol_notify,
                decorIcon = R.mipmap.bg_cholesterol_notify_icon,
                btnTextColor = R.color.color_F0832D
            )
            5 -> NotificationResources(
                smallIcon = R.drawable.ic_bmi_notify,
                background = R.drawable.bg_bmi_notify,
                largeIcon = R.drawable.ic_bmi_notify,
                decorIcon = R.mipmap.bg_bmi_notify_icon,
                btnTextColor = R.color.color_30A6ED
            )
            6 -> NotificationResources(
                smallIcon = R.drawable.ic_notification_bs,
                background = R.drawable.bg_hr_notify,
                largeIcon = R.drawable.ic_hr_notify,
                decorIcon = R.mipmap.bg_hr_notify_icon,
                btnTextColor = R.color.color_FF4420
            )
            7 -> NotificationResources(
                smallIcon = R.drawable.ic_statistical_notify,
                background = R.drawable.bg_bs_notify,
                largeIcon = R.drawable.ic_statistical_notify,
                decorIcon = R.drawable.bg_statistical_notify_icon,
                btnTextColor = R.color.color_02BC77
            )
            9 -> NotificationResources(
                smallIcon = R.drawable.ic_hydrate_notify,
                background = R.drawable.bg_bmi_notify,
                largeIcon = R.drawable.ic_hydrate_notify,
                decorIcon = R.mipmap.bg_hydrate_notify_icon,
                btnTextColor = R.color.color_30A6ED
            )
            10 -> NotificationResources(
                smallIcon = R.drawable.ic_step_notify,
                background = R.drawable.bg_cholesterol_notify,
                largeIcon = R.drawable.ic_step_notify,
                decorIcon = R.mipmap.bg_step_notify_icon,
                btnTextColor = R.color.color_F0832D
            )
            11 -> NotificationResources(
                smallIcon = com.android.common.weather.R.drawable.ic_cloudy,
                background = null,
                largeIcon = null,
                decorIcon = null,
                btnTextColor = null
            )
            12 -> NotificationResources(
                smallIcon = R.drawable.ic_notification_bs,
                background = null,
                largeIcon = null,
                decorIcon = null,
                btnTextColor = null
            )
            else -> NotificationResources(
                smallIcon = R.drawable.ic_notification_bs,
                background = null,
                largeIcon = null,
                decorIcon = R.drawable.bg_homepage_notify_icon
            )
        }
    }

    /**
     * 根据 iconType 获取布局资源
     */
    fun getLayoutResources(iconType: Int): LayoutResources {
        return  if (iconType == 11) {
            // 天气通知使用天气模块布局
            LayoutResources(
                collapsedLayout = com.android.common.weather.R.layout.layout_weather_notification_normal,
                expandedLayout = com.android.common.weather.R.layout.layout_weather_notification_big
            )
        } else if (iconType == 12) {
            LayoutResources(
                collapsedLayout = R.layout.layout_assistant_notify,
                expandedLayout = R.layout.layout_assistant_notify_big
            )
        } else {
            // 其他类型使用通用布局
            LayoutResources(
                collapsedLayout = R.layout.layout_common_notify,
                expandedLayout = R.layout.layout_common_notify_big
            )
        }
    }
}
