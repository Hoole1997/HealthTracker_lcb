package com.healthtracker.blood.suger.work

import com.healthtracker.blood.suger.helper.HealthNotificationHelper
import com.healthtracker.blood.suger.manager.HealthServiceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Worker 依赖入口点
 *
 * 用途：
 * - 允许 Worker 从 Hilt 容器获取依赖，而无需使用 @HiltWorker
 * - 解决 Hilt WorkManager 集成的复杂性问题
 *
 * 设计原理：
 * - EntryPoint 是 Hilt 的"逃生门"机制
 * - 用于非 Hilt 管理的类（如标准 Worker）访问 Hilt 依赖图
 * - 安装在 SingletonComponent，确保获取单例依赖
 *
 * 使用方式：
 * ```kotlin
 * val dependencies = EntryPointAccessors.fromApplication(
 *     applicationContext,
 *     WorkerDependencies::class.java
 * )
 * val manager = dependencies.healthServiceManager()
 * ```
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerDependencies {

    /**
     * 提供 HealthServiceManager 单例
     * 负责健康服务的启动、停止和状态管理
     */
    fun healthServiceManager(): HealthServiceManager

    /**
     * 提供 HealthNotificationHelper 单例
     * 负责创建和管理常驻通知
     */
    fun notificationHelper(): HealthNotificationHelper
}
