package com.healthtracker.earthquake.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.healthtracker.earthquake.R
import com.healthtracker.earthquake.model.EarthquakeProperties
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 地震通知辅助：创建渠道、构建并发送通知。
 */
class EarthquakeNotificationHelper(private val context: Context) {

    private val manager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EarthquakePushIds.CHANNEL_ID,
                EarthquakePushIds.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(props: EarthquakeProperties, isHigh: Boolean): NotificationCompat.Builder {
        // Title: "M" + magnitude + " EARTHQUAKE CONFIRMED"
        val magStr = props.mag?.let { String.format(Locale.US, "%.1f", it) } ?: "?"
        val titleText = "M $magStr EARTHQUAKE CONFIRMED"

        // Content: "JST" + current time + " - See Impact Radius……"
        val sdf = SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        }
        val jstTime = sdf.format(Date())
        val contentText = "JST $jstTime - See Impact Radius……"

        // Build custom small content view
        val contentView = RemoteViews(context.packageName, R.layout.notification_small_earthquake).apply {
            setImageViewResource(R.id.ivIcon, R.mipmap.ic_earthquack_noti_icon)
            setTextViewText(R.id.tvTitle, titleText)
            setTextViewText(R.id.tvContent, contentText)
        }

        return NotificationCompat.Builder(context, EarthquakePushIds.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_earthquack_noti_icon)
            .setCustomContentView(contentView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setColor(ContextCompat.getColor(context, R.color.eq_noti_red))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
    }

    fun notifyHighSeverity(props: EarthquakeProperties) {
        ensureChannel()
        if (!hasPermission()) return
        val notification = buildNotification(props, isHigh = true).build()
        manager.notify(EarthquakePushIds.NOTIFICATION_ID_HIGH, notification)
    }

    fun notifyLowSeverity(props: EarthquakeProperties) {
        ensureChannel()
        if (!hasPermission()) return
        val notification = buildNotification(props, isHigh = false).build()
        manager.notify(EarthquakePushIds.NOTIFICATION_ID_LOW, notification)
    }

    private fun hasPermission(): Boolean {
        // 库内进行最小权限检查；权限请求由宿主App处理
        val perm = android.Manifest.permission.POST_NOTIFICATIONS
        return ContextCompat.checkSelfPermission(context, perm) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}