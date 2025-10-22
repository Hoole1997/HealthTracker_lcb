package com.healthtracker.blood.suger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.healthtracker.blood.suger.helper.HealthNotificationHelper
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 健康监测前台服务
 * 用于显示常驻通知，提供快捷健康数据记录入口
 */
@AndroidEntryPoint
class HealthService : Service() {

    companion object {
        private const val TAG = "HealthService"
    }

    @Inject
    lateinit var notificationHelper: HealthNotificationHelper

    override fun onCreate() {
        super.onCreate()
        "Health service created".logd(TAG)

        // 创建通知渠道
        notificationHelper.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // 构建通知
            val notification = notificationHelper.buildNotification()

            // 启动前台服务
            startForeground(
                HealthServiceConstants.NOTIFICATION_ID_HEALTH_SERVICE,
                notification
            )

            "Health service started in foreground".logd(TAG)

        } catch (e: Exception) {
            "Failed to start foreground service: ${e.message}".loge(TAG)
            stopSelf()
        }

        // START_STICKY: 服务被系统杀死后会自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            "Health service destroyed".logd(TAG)
        } catch (e: Exception) {
            "Failed to stop foreground service: ${e.message}".loge(TAG)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}