package com.daily.health.manager.service

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.daily.health.manager.config.models.PushConfig
import com.daily.health.manager.data.repo.StepRepository
import com.daily.health.manager.helper.ResidentNotificationHelper
import com.daily.health.manager.strategy.PushOrchestrator
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.report.ReportDataManager
import org.koin.core.context.GlobalContext

/**
 * 健康监测前台服务
 * 用于显示常驻通知，提供快捷健康数据记录入口
 *
 * 功能：
 * - 启动前台服务，显示常驻通知
 * - 每 5 分钟自动刷新通知，确保通知始终显示
 */
class HealthService : Service() {

    companion object {
        private const val TAG = "HealthService"

        // 通知刷新间隔：5 分钟
        private const val REFRESH_INTERVAL_MINUTES = 5L

        const val IS_SILENT = "is_silent"
    }

    private var notificationHelper: ResidentNotificationHelper? = null
    private var configManager: RemoteConfigManager? = null
    // 服务级别的协程作用域
    private val serviceScope = CoroutineScope(Main + SupervisorJob())

    // 刷新通知的协程任务
    private var refreshJob: Job? = null

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private val stepRepo by lazy { StepRepository.get(applicationContext) }
    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val raw = event.values.firstOrNull()?.toInt() ?: return
                serviceScope.launch { stepRepo.handleRawStep(raw) }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate() {
        super.onCreate()

        val koin = runCatching { GlobalContext.get() }.getOrNull()
        notificationHelper = koin?.get<ResidentNotificationHelper>()
        configManager = koin?.get<RemoteConfigManager>()

        if (notificationHelper == null || configManager == null) {
            "Koin not ready, stopping service".loge(TAG)
            stopSelf()
            return
        }
        
        // 安全检查：确保 Application 是 Hilt 初始化的 App 类
        // 防止系统通过 START_STICKY 重启 Service 时 Hilt 未就绪导致崩溃
        if (application !is com.daily.health.manager.App) {
            "Application not properly initialized, stopping service".loge(TAG)
            stopSelf()
            return
        }
        
        "Health service created".logd(TAG)
        notificationHelper?.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val silent = intent?.getBooleanExtra(IS_SILENT,true) ?: true
            // 构建通知
            val notification = notificationHelper?.buildNotification(silent) ?: run {
                stopSelf()
                return START_NOT_STICKY
            }
            "启动前台服务，发送常驻通知 silent：$silent".logd(PushOrchestrator.TAG)

            // 启动前台服务
            startForeground(
                HealthServiceConstants.NOTIFICATION_ID_HEALTH_SERVICE,
                notification
            )

            "Health service started in foreground".logd(TAG)

            // 启动通知循环刷新任务
            startNotificationRefreshLoop()

            registerStepSensor()

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
            unregisterStepSensor()

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

        val configManager = configManager ?: return
        val interval = configManager.getConfig<PushConfig>().getChannelConfig(ChannelUserController.isPaidChannel()).keepalivePollingIntervalMinutes
        refreshJob = serviceScope.launch {


            while (isActive) {
                try {
                    // 首次延迟 5 分钟后执行
                    delay(interval * 60 * 1000L)
                    ReportDataManager.reportData("Notific_Pull", mapOf("topic" to "timer"))
                    refreshNotification()

                    if (BuildState.debug) {
                        "Notification refreshed at ${System.currentTimeMillis()}".logd(TAG)
                    }
                } catch (e: Exception) {
                    "Failed to refresh notification: ${e.message}".loge(TAG)
                    // 发生错误后继续循环，不中断
                }
            }
        }

        if (BuildState.debug) {
            "Notification refresh loop started (interval: $interval minutes)".logd(TAG)
        }
    }

    private fun registerStepSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            sensorManager?.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun unregisterStepSensor() {
        sensorManager?.unregisterListener(stepListener)
        sensorManager = null
        stepSensor = null
    }

    /**
     * 刷新前台通知
     * 重新构建并显示通知，使用相同的 notification ID
     */
    private fun refreshNotification() {
        val notification = notificationHelper?.buildNotification() ?: return

        // 使用相同的 ID 更新前台通知
        // startForeground() 是幂等的，重复调用只会更新通知
        startForeground(
            HealthServiceConstants.NOTIFICATION_ID_HEALTH_SERVICE,
            notification
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
