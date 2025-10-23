package com.healthtracker.blood.suger.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.healthtracker.blood.suger.helper.HealthNotificationHelper
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 健康监测前台服务
 * 用于显示常驻通知，提供快捷健康数据记录入口
 *
 * 功能：
 * - 启动前台服务，显示常驻通知
 * - 每 5 分钟自动刷新通知，确保通知始终显示
 */
@AndroidEntryPoint
class HealthService : Service() {

    companion object {
        private const val TAG = "HealthService"

        // 通知刷新间隔：5 分钟
        private const val REFRESH_INTERVAL_MINUTES = 1L
    }

    @Inject
    lateinit var notificationHelper: HealthNotificationHelper

    // 服务级别的协程作用域
    private val serviceScope = CoroutineScope(Main + SupervisorJob())

    // 刷新通知的协程任务
    private var refreshJob: Job? = null

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

            // 启动通知循环刷新任务
            startNotificationRefreshLoop()

        } catch (e: Exception) {
            "Failed to start foreground service: ${e.message}".loge(TAG)
            stopSelf()
        }

        // START_STICKY: 服务被系统杀死后会自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            // 取消刷新任务
            refreshJob?.cancel()
            refreshJob = null

            if (BuildState.debug) {
                "Notification refresh loop cancelled".logd(TAG)
            }

            // 取消协程作用域（释放所有协程资源）
            serviceScope.cancel()

            stopForeground(STOP_FOREGROUND_REMOVE)
            "Health service destroyed".logd(TAG)
        } catch (e: Exception) {
            "Failed to stop foreground service: ${e.message}".loge(TAG)
        }
        super.onDestroy()
    }

    /**
     * 启动通知循环刷新任务
     *
     * 每 5 分钟刷新一次前台通知，确保：
     * 1. 通知始终显示（不被系统清理）
     * 2. 服务保持活跃状态
     * 3. 为未来动态更新通知内容做准备
     */
    private fun startNotificationRefreshLoop() {
        // 如果已有刷新任务，先取消
        refreshJob?.cancel()

        refreshJob = serviceScope.launch {
            // 首次延迟 5 分钟后执行
            delay(REFRESH_INTERVAL_MINUTES * 60 * 1000L)

            while (isActive) {
                try {
                    refreshNotification()

                    if (BuildState.debug) {
                        "Notification refreshed at ${System.currentTimeMillis()}".logd(TAG)
                    }

                    // 等待 5 分钟后继续下一次刷新
                    delay(REFRESH_INTERVAL_MINUTES * 60 * 1000L)

                } catch (e: Exception) {
                    "Failed to refresh notification: ${e.message}".loge(TAG)
                    // 发生错误后继续循环，不中断
                }
            }
        }

        if (BuildState.debug) {
            "Notification refresh loop started (interval: $REFRESH_INTERVAL_MINUTES minutes)".logd(TAG)
        }
    }

    /**
     * 刷新前台通知
     * 重新构建并显示通知，使用相同的 notification ID
     */
    private fun refreshNotification() {
        val notification = notificationHelper.buildNotification()

        // 使用相同的 ID 更新前台通知
        // startForeground() 是幂等的，重复调用只会更新通知
        startForeground(
            HealthServiceConstants.NOTIFICATION_ID_HEALTH_SERVICE,
            notification
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
