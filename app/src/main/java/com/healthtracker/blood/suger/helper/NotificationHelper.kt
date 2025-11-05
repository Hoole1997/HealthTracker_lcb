package com.healthtracker.blood.suger.helper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.strategy.PushResult
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.blood.suger.work.PeriodicScanWorker
import com.healthtracker.blood.suger.work.WorkerDependencies
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    // ✅ 通过 EntryPoint 获取依赖（延迟初始化）
    private val dependencies: WorkerDependencies by lazy {
        EntryPointAccessors.fromApplication(
            App.INSTANCE,
            WorkerDependencies::class.java
        )
    }

    private val pushOrchestrator: PushOrchestrator by lazy {
        dependencies.pushOrchestrator()
    }

    private val healthServiceManager: HealthServiceManager by lazy {
        dependencies.healthServiceManager()
    }

    private val notificationHelper: ResidentNotificationHelper by lazy {
        dependencies.notificationHelper()
    }

    fun show(scenario: PushScenario){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 触发解锁场景推送
                // Phase 1: 所有用户视为自然用户（isPaidUser = false）
                // Phase 2: 将从配置或用户数据读取付费状态
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
                    return@launch
                }else{
                     val result = pushOrchestrator.triggerPush(
                        scenario = scenario,
                        extras = null
                    )
                    // 处理推送结果
                    handlePushResult(result,scenario)
                }




            } catch (e: Exception) {
                "Error handling unlock event: ${e.message}".loge(TAG)
                e.printStackTrace()
            } finally {
                // 完成异步操作，释放 BroadcastReceiver
            }
        }
    }


    /**
     * 处理推送结果
     * 根据不同的结果类型记录相应的日志
     */
    private fun handlePushResult(result: PushResult,pushScenario: PushScenario) {
        when (result) {
            is PushResult.Success -> {
                if (BuildState.debug) {
                    "$pushScenario push successful: pushId=${result.pushId}".logd(TAG)
                }
            }

            is PushResult.Blocked -> {
                if (BuildState.debug) {
                    "Unlock push blocked: ${result.reason}".logw(TAG)
                }
            }

            is PushResult.NoSuitableMessage -> {
                if (BuildState.debug) {
                    "Unlock push skipped: no suitable message".logw(TAG)
                }
            }

            is PushResult.Error -> {
                "Unlock push failed: ${result.exception.message}".loge(TAG)
                result.exception.printStackTrace()
            }
        }
    }

    /**
     * 检查是否应该发送通知
     * 避免重复发送
     */
    fun shouldHandleNotification(): Boolean {
        val isRunning = healthServiceManager.isServiceRunning()

        if (BuildState.debug) {
            "Service running status: $isRunning, should handle: ${!isRunning}".logd(TAG
            )
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
    fun handleNotificationStrategy(): Boolean {
        val isAndroid12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val isForeground = AppLifecycleManager.isForeground()

        if (BuildState.debug) {
            "Notification strategy: Android12+=$isAndroid12Plus, Foreground=$isForeground".logd(TAG)
        }

        return when {
            // 场景 1: Android 11- 或应用在前台 → 启动前台服务
            !isAndroid12Plus || isForeground -> {
                if (BuildState.debug) {
                    "Starting foreground service (Android 11- or foreground)".logd(TAG
                    )
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