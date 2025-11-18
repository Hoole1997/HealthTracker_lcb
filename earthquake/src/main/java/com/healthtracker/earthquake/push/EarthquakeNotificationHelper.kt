package com.healthtracker.earthquake.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.healthtracker.earthquake.R
import com.healthtracker.earthquake.EarthquakeActivity
import com.healthtracker.earthquake.model.EarthquakeFeature
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

    private fun buildNotification(feature: EarthquakeFeature, isHigh: Boolean): NotificationCompat.Builder {
        // Title: "M" + magnitude + " EARTHQUAKE CONFIRMED"
        val magStr = feature.properties?.mag?.let { String.format(Locale.US, "%.1f", it) } ?: "?"
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

        val intent = Intent(context, EarthquakeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("eq_extra_mag", feature.properties?.mag ?: -1.0)
            putExtra("eq_extra_place", feature.properties?.place ?: "")
            putExtra("eq_extra_time", feature.properties?.time ?: -1L)
            putExtra("eq_extra_mag_type", feature.properties?.magType ?: "")
            putExtra("eq_extra_status", feature.properties?.status ?: "")
            putExtra("eq_extra_tsunami", feature.properties?.tsunami ?: 0)
            putExtra("eq_extra_alert", feature.properties?.alert ?: "-")
            putExtra("eq_extra_id", feature.id ?: "")
            putExtra("eq_extra_url", feature.properties?.url ?: "")
            putExtra("eq_extra_lon", feature.geometry?.coordinates?.getOrNull(0) ?: Double.NaN)
            putExtra("eq_extra_lat", feature.geometry?.coordinates?.getOrNull(1) ?: Double.NaN)
            putExtra("eq_extra_depth", feature.geometry?.coordinates?.getOrNull(2) ?: Double.NaN)
        }
        val flags = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(context, 1001, intent, flags)

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
            .setContentIntent(pendingIntent)
    }

    fun notifyHighSeverity(feature: EarthquakeFeature) {
        ensureChannel()
        if (!hasPermission()) return
        val notification = buildNotification(feature, isHigh = true).build()
        manager.notify(EarthquakePushIds.NOTIFICATION_ID_HIGH, notification)
    }

    fun notifyLowSeverity(feature: EarthquakeFeature) {
        ensureChannel()
        if (!hasPermission()) return
        val notification = buildNotification(feature, isHigh = false).build()
        manager.notify(EarthquakePushIds.NOTIFICATION_ID_LOW, notification)
    }

    private fun hasPermission(): Boolean {
        // 库内进行最小权限检查；权限请求由宿主App处理
        val perm = android.Manifest.permission.POST_NOTIFICATIONS
        return ContextCompat.checkSelfPermission(context, perm) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}