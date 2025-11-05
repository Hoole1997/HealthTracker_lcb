package com.healthtracker.blood.suger.work

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.blood.suger.helper.NotificationHelper
import com.healthtracker.blood.suger.helper.NotificationHelper.handleNotificationStrategy
import com.healthtracker.blood.suger.helper.NotificationHelper.shouldHandleNotification
import com.healthtracker.blood.suger.helper.ResidentNotificationHelper
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.blood.suger.strategy.PushScenario
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

    private var initTime = System.currentTimeMillis()




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

            val currentTime = System.currentTimeMillis()
            if (currentTime - initTime > 5 * 60 * 1000) {
                NotificationHelper.show(PushScenario.KEEPALIVE)
            } else {
                if(BuildState.debug) "PeriodicScanWorker init time less 5 min".logd(TAG)
            }

        } catch (e: Throwable) {
            "PeriodicScanWorker error: ${e.message}".logd(TAG)
            e.printStackTrace()
        }

        return Result.success()
    }


}
