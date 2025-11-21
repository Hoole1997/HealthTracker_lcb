package com.healthtracker.blood.suger.strategy

import android.content.Context
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.helper.CustomNotificationHelper
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loop 推送管理器
 *
 * 职责：
 * 1. 管理 Loop 推送生命周期（启动、停止、状态跟踪）
 * 2. 按固定间隔（4秒）发送静音通知
 * 3. 支持多个 Loop 推送并发运行
 * 4. 线程安全的状态管理
 *
 * Loop 推送机制：
 * - 首条通知：正常通知（有声音、震动）
 * - 后续通知：静音通知（无声音、震动），每隔 4 秒发送
 * - 使用相同 notificationId 替换通知（不叠加）
 * - 停止条件：用户点击、划掉、切回前台、达到循环次数
 *
 * @param customNotificationHelper 通知辅助类
 * @param permissionManager 权限管理器
 * @param context 应用上下文
 */
@Singleton
class LoopPushManager @Inject constructor(
    private val customNotificationHelper: CustomNotificationHelper,
    private val permissionManager: PermissionManager,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "LoopPushManager"

        // Loop 推送配置
        private const val LOOP_INTERVAL_SECONDS = 4L  // 固定 4 秒间隔
    }

    /**
     * Loop 推送状态
     *
     * @property pushMessage 推送消息对象
     * @property notificationId 通知 ID（所有 Loop 复用相同 ID）
     * @property maxLoops 最大循环次数（来自 ChannelConfig.hoverDurationLoopCount）
     * @property currentLoop 当前循环次数
     * @property isPaidUser 是否付费用户
     * @property job 协程 Job（用于取消）
     */
    data class LoopState(
        val pushMessage: PushMessage,
        val notificationId: Int,
        val maxLoops: Int,
        var currentLoop: Int,
        val isPaidUser: Boolean,
        var job: Job?
    )

    // 活跃的 Loop 推送（线程安全）
    // Key: notificationId, Value: LoopState
    private val activeLoops = ConcurrentHashMap<Int, LoopState>()

    // 协程作用域（使用 SupervisorJob 避免子协程失败影响其他协程）
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 启动 Loop 推送
     *
     * @param pushMessage 推送消息
     * @param notificationId 通知 ID（首条通知的 ID）
     * @param isPaidUser 是否付费用户
     * @param loopCount 循环次数（来自 ChannelConfig.hoverDurationLoopCount）
     */
    fun startLoopPush(
        pushMessage: PushMessage,
        notificationId: Int,
        isPaidUser: Boolean,
        loopCount: Int,
        scenario: PushScenario
    ) {
        if (BuildState.debug) {
            "Starting Loop push: notificationId=$notificationId, loopCount=$loopCount, " +
                    "isPaid=$isPaidUser, pushId=${pushMessage.id}".logd(PushOrchestrator.TAG)
        }

        // 参数验证
        if (loopCount <= 1) {
            "Invalid loop count: $loopCount (must be > 1), skipping Loop push".logw(PushOrchestrator.TAG)
            return
        }

        // 检查是否已存在相同 notificationId 的 Loop（防止重复启动）
        if (activeLoops.containsKey(notificationId)) {
            "Loop push already active for notificationId=$notificationId, skipping".logw(PushOrchestrator.TAG)
            return
        }

        // 启动协程执行 Loop 推送
        val job = scope.launch {
            try {
                // 注意：首条通知已经发送，这里从第 2 条开始
                // loopCount - 1 是因为首条已发送
                repeat(loopCount - 1){ index ->

                    if(AppLifecycleManager.isScreenLock()){
                        if(BuildState.debug){
                            "Loop push stopped during screen lock: notificationId=$notificationId".logd(
                                PushOrchestrator.TAG)
                        }
                        return@launch
                    }

                    // 延迟 4 秒
                    delay(LOOP_INTERVAL_SECONDS * 1000)

                    // 检查是否已被停止
                    if (!activeLoops.containsKey(notificationId)) {
                        if (BuildState.debug) {
                            "Loop push stopped during delay: notificationId=$notificationId".logd(PushOrchestrator.TAG)
                        }
                        return@launch
                    }

                    // 检查通知权限
                    if (!permissionManager.isNotificationPermissionGranted()) {
                        if (BuildState.debug) {
                            "Notification permission revoked, stopping Loop push: " +
                                    "notificationId=$notificationId".logw(PushOrchestrator.TAG)
                        }
                        stopLoopPush(notificationId, "permission_revoked")
                        return@launch
                    }

                    // 更新当前循环次数
                    activeLoops[notificationId]?.currentLoop = index + 2  // +2 因为第 1 条已发送

                    // 发送静音通知（使用相同 notificationId 替换）
                    try {
                        customNotificationHelper.showCustomNotification(
                            pushMessage = pushMessage,
                            isSilent = true,
                            notificationId = notificationId,
                            scenario
                        )

                        if (BuildState.debug) {
                            "Loop push sent: notificationId=$notificationId, " +
                                    "loop=${index + 2}/$loopCount".logd(PushOrchestrator.TAG)
                        }
                    } catch (e: Exception) {
                        "Failed to send Loop push notification: ${e.message}".logw(PushOrchestrator.TAG)
                        // 不中断 Loop，继续下一次
                    }
                }

                // 所有循环完成，清理状态
                activeLoops.remove(notificationId)
                if (BuildState.debug) {
                    "Loop push completed: notificationId=$notificationId, " +
                            "total=$loopCount".logd(PushOrchestrator.TAG)
                }

            } catch (e: Exception) {
                "Loop push failed: notificationId=$notificationId, error=${e.message}".logw(PushOrchestrator.TAG)
                activeLoops.remove(notificationId)
            }
        }

        // 保存 Loop 状态
        val loopState = LoopState(
            pushMessage = pushMessage,
            notificationId = notificationId,
            maxLoops = loopCount,
            currentLoop = 1,  // 首条已发送
            isPaidUser = isPaidUser,
            job = job
        )

        activeLoops[notificationId] = loopState

        if (BuildState.debug) {
            "Loop push state saved: notificationId=$notificationId, " +
                    "activeLoops=${activeLoops.size}".logd(PushOrchestrator.TAG)
        }
    }

    /**
     * 停止指定的 Loop 推送
     *
     * @param notificationId 通知 ID
     * @param reason 停止原因（用于日志）
     */
    fun stopLoopPush(notificationId: Int, reason: String) {
        val loopState = activeLoops.remove(notificationId)

        if (loopState != null) {
            // 取消协程
            loopState.job?.cancel()

            if (BuildState.debug) {
                "Loop push stopped: notificationId=$notificationId, " +
                        "reason=$reason, " +
                        "progress=${loopState.currentLoop}/${loopState.maxLoops}".logd(PushOrchestrator.TAG)
            }
        } else {
            if (BuildState.debug) {
                "Loop push not found for stop: notificationId=$notificationId, " +
                        "reason=$reason".logd(PushOrchestrator.TAG)
            }
        }
    }

    /**
     * 停止所有 Loop 推送
     *
     * @param reason 停止原因（用于日志）
     */
    fun stopAllLoopPushes(reason: String) {
        val count = activeLoops.size

        if (count > 0) {
            if (BuildState.debug) {
                "Stopping all Loop pushes: count=$count, reason=$reason".logd(PushOrchestrator.TAG)
            }

            // 取消所有协程
            activeLoops.values.forEach { loopState ->
                loopState.job?.cancel()
            }

            // 清空状态
            activeLoops.clear()

            if (BuildState.debug) {
                "All Loop pushes stopped: count=$count".logd(PushOrchestrator.TAG)
            }
        } else {
            if (BuildState.debug) {
                "No active Loop pushes to stop: reason=$reason".logd(PushOrchestrator.TAG)
            }
        }
    }

    /**
     * 获取当前活跃的 Loop 推送数量（调试用）
     */
    fun getActiveLoopCount(): Int = activeLoops.size

    /**
     * 获取指定通知的 Loop 状态（调试用）
     */
    fun getLoopState(notificationId: Int): LoopState? = activeLoops[notificationId]

    /**
     * 检查指定通知是否有活跃的 Loop 推送
     */
    fun hasActiveLoop(notificationId: Int): Boolean = activeLoops.containsKey(notificationId)
}
