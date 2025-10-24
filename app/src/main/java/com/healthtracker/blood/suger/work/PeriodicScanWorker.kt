package com.healthtracker.blood.suger.work

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.blood.suger.helper.ResidentNotificationHelper
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import dagger.hilt.android.EntryPointAccessors

/**
 * 周期性扫描 Worker
 *
 * 职责：
 * 1. 每 15 分钟执行一次（默认）
 * 2. 检查设备状态（是否解锁）
 * 3. 发送通知或启动前台服务（根据策略）
 * 4. 触发一次性扫描任务（如果需要）
 *
 * 通知策略：
 * - Android 11- 或前台：启动前台服务（常驻通知）
 * - Android 12+ 且后台：发送普通通知（可删除）
 *
 * 依赖注入方案：
 * - 使用标准 Worker 构造函数（不使用 @HiltWorker）
 * - 通过 EntryPoint 手动获取 Hilt 依赖
 * - 延迟初始化（lazy）避免构造时阻塞
 */
class PeriodicScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "PeriodicScanWorker"
    }

    // ✅ 通过 EntryPoint 获取依赖（延迟初始化）
    private val dependencies: WorkerDependencies by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerDependencies::class.java
        )
    }

    private val healthServiceManager: HealthServiceManager by lazy {
        dependencies.healthServiceManager()
    }

    private val notificationHelper: ResidentNotificationHelper by lazy {
        dependencies.notificationHelper()
    }

    private val pushOrchestrator: com.healthtracker.blood.suger.strategy.PushOrchestrator by lazy {
        dependencies.pushOrchestrator()
    }

    override suspend fun doWork(): Result {
        if (BuildState.debug) "PeriodicScanWorker Run".logd(TAG)

        try {
            // ✅ 步骤 1: 处理通知/前台服务策略
            // 只在首次或服务未运行时处理，避免重复
            if (shouldHandleNotification()) {
                handleNotificationStrategy()
            } else {
                if (BuildState.debug) "Service already running, skipping notification".logd(TAG)
            }

        } catch (e: Throwable) {
            "PeriodicScanWorker error: ${e.message}".logd(TAG)
            e.printStackTrace()
        }

        return Result.success()
    }

    /**
     * 检查是否应该发送通知
     * 避免重复发送
     */
    private fun shouldHandleNotification(): Boolean {
        val isRunning = healthServiceManager.isServiceRunning()

        if (BuildState.debug) {
            "Service running status: $isRunning, should handle: ${!isRunning}".logd(TAG)
        }

        return !isRunning
    }

    /**
     * 执行通知策略
     *
     * 策略逻辑：
     * 1. Android 11- 或应用在前台 → 启动前台服务（常驻通知）
     * 2. Android 12+ 且应用在后台 → 发送普通通知（可被删除）
     */
    private suspend fun handleNotificationStrategy(): Boolean {
        val isAndroid12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val isForeground = AppLifecycleManager.isForeground()

        if (BuildState.debug) {
            "Notification strategy: Android12+=$isAndroid12Plus, Foreground=$isForeground".logd(TAG)
        }

        return when {
            // 场景 1: Android 11- 或应用在前台 → 启动前台服务
            !isAndroid12Plus || isForeground -> {
                if (BuildState.debug) {
                    "Starting foreground service (Android 11- or foreground)".logd(TAG)
                }
                healthServiceManager.startHealthService()
                true
            }

            // 场景 2: Android 12+ 且应用在后台 → 触发后台场景推送
            isAndroid12Plus && !isForeground -> {
                if (BuildState.debug) {
                    "Triggering background push (Android 12+ background)".logd(TAG)
                }
                notificationHelper.sendNormalNotification()
            }

            // 其他场景（理论上不会到达）
            else -> {
                if (BuildState.debug) "Unknown scenario, skipping".logd(TAG)
                false
            }
        }
    }
}
