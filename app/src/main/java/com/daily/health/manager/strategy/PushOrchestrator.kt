package com.daily.health.manager.strategy

import android.Manifest
import android.R.attr.type
import android.os.Bundle
import androidx.annotation.RequiresPermission
import com.daily.health.manager.config.models.PushConfig
import com.daily.health.manager.config.models.PushMessage
import com.daily.health.manager.helper.CustomNotificationHelper
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.logi
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.report.ReportDataManager
import java.util.*

/**
 * 推送总协调器
 *
 * 职责：
 * 1. 统一的推送触发入口
 * 2. 协调频率控制检查
 * 3. 执行推送（Phase 1: 简化版，直接发送普通通知）
 * 4. 记录推送触发历史
 *
 * Phase 1 架构：
 * PushOrchestrator (本类)
 *   ↓
 * PushFrequencyController (频率检查)
 *   ↓
 * HealthNotificationHelper (执行推送)
 *   ↓
 * 记录触发
 *
 * Phase 2 将增加：
 * - PushScenarioManager (场景管理)
 * - PushMessageSelector (消息选择)
 * - 条件评估逻辑
 */
class PushOrchestrator(
    private val frequencyController: PushFrequencyController,
    private val customNotificationHelper: CustomNotificationHelper,
    private val messageSelector: PushMessageSelector,
    private val loopPushManager: LoopPushManager,
    private val configManager: RemoteConfigManager
) {

    companion object {
         const val TAG = "PushOrchestrator"
    }

    /**
     * 触发推送
     *
     * Phase 2 完整流程：
     * 1. 频率检查（通过 PushFrequencyController）
     * 2. 消息选择（通过 PushMessageSelector）← 新增
     * 3. 执行推送（发送自定义通知）← 修改
     * 4. 记录触发
     *
     * @param scenario 推送场景
     * @param isPaidUser 是否付费用户
     * @param extras 附加数据（可选）
     * @return 推送结果
     */
    suspend fun triggerPush(
        scenario: PushScenario,
        extras: Bundle? = null
    ): PushResult = withContext(Dispatchers.IO)  {
        val pushId = generatePushId(scenario)
        val isPaidUser = ChannelUserController.isPaidChannel()


        if (BuildState.debug) {
            "Triggering push: pushId=$pushId, scenario=$scenario, isPaid=$isPaidUser".logd(TAG)
        }

        try {
            val pushConfig = configManager.getConfig<PushConfig>()
            val channelConfig = pushConfig.getChannelConfig(isPaidUser)
            // ===== 步骤 1: 频率检查 =====
            val frequencyResult = frequencyController.canTriggerPush(
                pushId = pushId,
                scenario = scenario,
                config = channelConfig
            )

            if (!frequencyResult.canTrigger) {
                val reason = frequencyResult.reason ?: ""
                ReportDataManager.reportData("Notific_Show_Fail", mapOf(
                    "reason" to reason
                ))
                reason.logi(TAG)
                return@withContext PushResult.Blocked(
                    frequencyResult.reason ?: "Frequency limit reached"
                )
            }

            // ===== 步骤 2: 选择消息（Phase 2 新增）=====
            val selectedMessage = messageSelector.selectMessage(
                scenario = scenario,
                isPaidUser = isPaidUser,
                extras = extras
            )

            if (selectedMessage == null) {
                if (BuildState.debug) {
                    "No suitable message found for scenario=$scenario".logw(TAG)
                }
                return@withContext PushResult.NoSuitableMessage
            }

            // ===== 步骤 3: 执行推送（Phase 2 修改）=====
            val pushSuccess = executePush(selectedMessage, isPaidUser,scenario)

            if (!pushSuccess) {
                val msg = "Failed to send notification"
                if (BuildState.debug) {
                    msg.loge(TAG)
                }
                ReportDataManager.reportData("Notific_Show_Fail", mapOf(
                    "reason" to msg
                ))
                return@withContext PushResult.Error(
                    Exception(msg)
                )
            }

            // ===== 步骤 4: 记录触发 =====
            frequencyController.recordPushTrigger(
                pushId = pushId,
                scenario = scenario
            )

            if (BuildState.debug) {
                "Push triggered successfully: pushId=$pushId, message=${selectedMessage.id}".logd(TAG)
            }

            PushResult.Success(pushId)

        } catch (e: Exception) {
            val msg = e.message ?: ""
            "Push execution error: ${msg}".loge(TAG)
            ReportDataManager.reportData("Notific_Show_Fail", mapOf(
                "reason" to msg
            ))
            e.printStackTrace()
            PushResult.Error(e)
        }
    }

    /**
     * 执行推送（Phase 2 版本 + Loop 推送支持）
     *
     * @param message 选中的推送消息
     * @param isPaidUser 是否付费用户
     * @return 是否成功
     */
    private fun executePush(message: PushMessage, isPaidUser: Boolean, scenario: PushScenario): Boolean {
        return try {
            if (BuildState.debug) {
                "Executing push for message: ${message.id}, isPaidUser=$isPaidUser".logd(TAG)
            }

            // 发送首条通知（正常通知，有声音）
            val notificationId = customNotificationHelper.showCustomNotification(
                pushMessage = message,
                isSilent = false,
                notificationId = null,
                scenario

            )

            if (notificationId <= 0) {
                "Failed to show notification: invalid notificationId=$notificationId".loge(TAG)
                return false
            }

            if (BuildState.debug) {
                "Custom notification sent successfully: ${message.title}, notificationId=$notificationId".logd(TAG)
            }

            // 获取 Loop 推送配置（从 RemoteConfig）
            try {
                val pushConfig = configManager.getConfig<PushConfig>()
                val channelConfig = pushConfig.getChannelConfig(isPaidUser)

                if (AppLifecycleManager.isScreenLock()) {
                    "Loop push disabled,screen is off".logd(
                        TAG
                    )
                } else {
                    val hoverSwitch = channelConfig.hoverDurationStrategySwitch
                    val hoverCount = channelConfig.hoverDurationLoopCount

                    if (BuildState.debug) {
                        "Loop config: switch=$hoverSwitch, count=$hoverCount, isPaid=$isPaidUser".logd(
                            TAG
                        )
                    }

                    // 如果配置开启且循环次数 > 1，启动 Loop 推送
                    if (hoverSwitch == 1 && hoverCount > 1) {
                        loopPushManager.startLoopPush(
                            pushMessage = message,
                            notificationId = notificationId,
                            isPaidUser = isPaidUser,
                            loopCount = hoverCount,
                            scenario
                        )

                        if (BuildState.debug) {
                            "Loop push started: notificationId=$notificationId, count=$hoverCount".logd(
                                TAG
                            )
                        }
                    } else {
                        if (BuildState.debug) {
                            "Loop push disabled or invalid config: switch=$hoverSwitch, count=$hoverCount".logd(
                                TAG
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                // Loop 推送配置失败不应阻塞主推送流程
                "Failed to get Loop config or start Loop push: ${e.message}".logw(TAG)
                // 继续执行，返回 true（主推送已成功）
            }

            true

        } catch (e: Exception) {
            "Failed to execute push: ${e.message}".loge(TAG)
            e.printStackTrace()
            false
        }
    }

    /**
     * 生成推送 ID
     * 格式: scenario_timestamp
     */
    private fun generatePushId(scenario: PushScenario): String {
        return "${scenario.name.lowercase()}_${System.currentTimeMillis()}"
    }

    /**
     * 获取当前每日推送计数（调试/UI 显示用）
     */
    fun getDailyPushCount(): Int {
        return frequencyController.getDailyPushCount()
    }

    /**
     * 重置频率控制数据（调试/测试用）
     */
    fun resetFrequencyData() {
        frequencyController.resetAll()
        if (BuildState.debug) {
            "Frequency data reset via orchestrator".logd(TAG)
        }
    }
}
