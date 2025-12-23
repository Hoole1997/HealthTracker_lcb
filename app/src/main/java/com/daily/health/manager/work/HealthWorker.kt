package com.daily.health.manager.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daily.health.manager.helper.NotificationHelper
import com.daily.health.manager.strategy.PushScenario
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import kotlinx.coroutines.delay

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
class HealthWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HealthWorker"
    }


    override suspend fun doWork(): Result {
        if (BuildState.debug) "HealthWorker Run".logd(TAG)
        try {
            delay(1000L)
            if(AppLifecycleManager.isBackground()){
                NotificationHelper.show(PushScenario.KEEPALIVE)
            }
        } catch (e: Throwable) {
            "HealthWorker error: ${e.message}".logd(TAG)
            e.printStackTrace()
        }
        return Result.success()
    }


}
