package com.healthtracker.blood.suger.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtracker.blood.suger.helper.HealthNotificationHelper
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.EntryPointAccessors

/**
 * 一次性扫描 Worker
 *
 * 职责：
 * - 被 PeriodicScanWorker 触发
 * - 执行具体扫描任务（当前大部分功能已注释）
 *
 * 当前状态：
 * - ⚠️ 业务逻辑大部分已注释
 * - ⚠️ 实际执行时几乎什么都不做
 * - ✅ 已预留通知策略扩展接口
 *
 * 未来扩展：
 * 如果需要恢复注释的功能（早晚提醒、文件扫描等），可以：
 * 1. 取消注释业务代码（第 52-127 行）
 * 2. 启用下面的通知策略调用
 *
 * 依赖注入方案：
 * - 使用标准 Worker 构造函数（不使用 @HiltWorker）
 * - 通过 EntryPoint 手动获取 Hilt 依赖
 * - 当前未使用，预留扩展
 */
class OneTimeScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "OneTimeScanWorker"
    }

    // ✅ 预留：通过 EntryPoint 获取依赖（延迟初始化）
    // 当前未使用，如果启用业务逻辑，取消下面的 @Suppress
    @Suppress("unused")
    private val dependencies: WorkerDependencies by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerDependencies::class.java
        )
    }

    @Suppress("unused")
    private val healthServiceManager: HealthServiceManager by lazy {
        dependencies.healthServiceManager()
    }

    @Suppress("unused")
    private val notificationHelper: HealthNotificationHelper by lazy {
        dependencies.notificationHelper()
    }

    override suspend fun doWork(): Result {
        if (BuildState.debug) "OneTimeScanWorker Run".logd(TAG)

        try {
            // ⚠️ 未来扩展：如果启用业务逻辑，取消下面的注释
            // if (!healthServiceManager.isServiceRunning()) {
            //     handleNotificationStrategy()
            // }

        } catch (e: Throwable) {
            "OneTimeScanWorker error: ${e.message}".logd(TAG)
            e.printStackTrace()
        }

        if (BuildState.debug) "OneTimeScanWorker End".logd(TAG)

        return Result.success()
    }
}
