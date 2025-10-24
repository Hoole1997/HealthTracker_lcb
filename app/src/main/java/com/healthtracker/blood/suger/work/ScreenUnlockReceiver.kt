package com.healthtracker.blood.suger.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.strategy.PushResult
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenUnlockReceiver : BroadcastReceiver() {
    companion object {
       private const val  TAG = "ScreenUnlockReceiver"
    }
    @Inject
    lateinit var pushOrchestrator: PushOrchestrator
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action) {
            // 在这里调度WorkManager任务
            if(BuildState.debug) "ScreenUnlockReceiver onReceive".logd(TAG)

            if (BuildState.debug) {
                "Screen unlock detected, triggering push".logd(TAG)
            }

            // 使用 goAsync() 处理异步操作
            // 这会给 BroadcastReceiver 额外的时间完成协程操作
            val pendingResult = goAsync()

            // 在 IO 线程执行推送逻辑
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 触发解锁场景推送
                    // Phase 1: 所有用户视为自然用户（isPaidUser = false）
                    // Phase 2: 将从配置或用户数据读取付费状态
                    val result = pushOrchestrator.triggerPush(
                        scenario = PushScenario.UNLOCK,
                        isPaidUser = false,  // Phase 1 默认值
                        extras = null
                    )

                    // 处理推送结果
                    handlePushResult(result)

                } catch (e: Exception) {
                    "Error handling unlock event: ${e.message}".loge(TAG)
                    e.printStackTrace()
                } finally {
                    // 完成异步操作，释放 BroadcastReceiver
                    pendingResult.finish()
                }
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