package com.healthtracker.blood.suger.observer

import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.helper.NotificationHelper
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.lifecycle.AppForegroundObserver
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康服务前台观察器
 *
 * 监听应用前后台切换，当应用回到前台时：
 * 1. 自动启动 HealthService（前台服务）
 * 2. 前台通知会自动替换之前的普通通知（相同 notification ID）
 *
 * 设计原理：
 * - Android 12+ 后台时只能发送普通通知（可被删除）
 * - 应用回到前台时，启动前台服务（常驻通知）
 * - 使用相同 notification ID，前台通知无缝替换普通通知
 *
 * 工作流程：
 * ```
 * WorkManager 后台执行
 *     ↓
 * 发送普通通知（Android 12+ 后台）
 *     ↓
 * 用户打开应用（回到前台）
 *     ↓
 * onAppForeground() 触发
 *     ↓
 * 启动前台服务
 *     ↓
 * 前台通知替换普通通知 ✅
 * ```
 */
@Singleton
class HealthServiceForegroundObserver @Inject constructor(
    private val healthServiceManager: HealthServiceManager
) : AppForegroundObserver {

    companion object {
        private const val TAG = "HealthServiceForegroundObserver"
    }
    @Inject
    lateinit var pushOrchestrator: PushOrchestrator

    override fun onAppForeground() {
        // 应用回到前台时，启动健康服务
        if (BuildState.debug) {
            "App entered foreground, attempting to start HealthService".logd(TAG)
        }


        // 检查是否已运行，避免重复启动
        if (!healthServiceManager.isServiceRunning()) {
            healthServiceManager.startHealthService()
            if (BuildState.debug) {
                "HealthService started successfully".logd(TAG)
            }
        } else {
            if (BuildState.debug) {
                "HealthService already running, skipping start".logd(TAG)
            }
        }
    }

    override fun onAppBackground() {
        // 应用进入后台时的逻辑（可选）
        // 当前不需要特殊处理，因为：
        // 1. HealthService 会保持运行（如果已启动）
        // 2. Worker 后台执行时会根据策略发送普通通知

        if (BuildState.debug) {
            "App entered background, HealthService will continue if running".logd(TAG)
        }



        if(System.currentTimeMillis() - AppLifecycleManager.clickAdTime < 2000L){
            "click ad leave app,block all push".logd(PushOrchestrator.TAG)
            return
        }

        if(App.INSTANCE.isFeatureLeave || App.INSTANCE.isClickAdLeave || App.INSTANCE.isGoSetting){
            "feature or click ad leave app,block all push".logd(PushOrchestrator.TAG)
            return
        }

        NotificationHelper.show(PushScenario.BACKGROUND)
    }
}
