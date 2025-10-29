package com.healthtracker.blood.suger.helper

import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.strategy.PushResult
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.blood.suger.work.WorkerDependencies
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
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

    fun show(scenario: PushScenario){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 触发解锁场景推送
                // Phase 1: 所有用户视为自然用户（isPaidUser = false）
                // Phase 2: 将从配置或用户数据读取付费状态
                val result = pushOrchestrator.triggerPush(
                    scenario = scenario,
                    extras = null
                )

                // 处理推送结果
                handlePushResult(result)

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
    private fun handlePushResult(result: PushResult) {
        when (result) {
            is PushResult.Success -> {
                if (BuildState.debug) {
                    "Unlock push successful: pushId=${result.pushId}".logd(TAG)
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
}