package com.healthtracker.blood.suger.strategy

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 推送消息选择器（循环推送模式）
 *
 * 职责：
 * - 从消息池中按顺序循环选择消息
 * - 逻辑：从第一条开始推送，下次满足条件推送下一条
 * - 实现：使用 SpUtils 跟踪索引，循环递增（0→1→2...→10→0）
 *
 * 示例：
 * - 第 1 次推送：push_001（index 0）
 * - 第 2 次推送：push_002（index 1）
 * - ...
 * - 第 11 次推送：push_011（index 10）
 * - 第 12 次推送：push_001（index 0，循环回第一条）
 */
class PushMessageSelector(
    private val messageRepository: PushMessageRepository,
    private val context: Context
) {

    companion object {
        private const val TAG = "PushMessageSelector"
        private const val PREFS_NAME = "push_message_index"
        private const val KEY_LAST_INDEX = "last_index"
    }


    /**
     * 选择下一条推送消息（循环模式）
     *
     * 算法：
     * 1. 获取上次发送的索引
     * 2. 计算下一个索引：(lastIndex + 1) % messageCount
     * 3. 保存新索引
     * 4. 返回对应消息
     *
     * @param scenario 推送场景（预留参数，Phase 2 未使用）
     * @param isPaidUser 是否付费用户（预留参数，Phase 2 未使用）
     * @param extras 附加数据（预留参数，Phase 2 未使用）
     * @return 选中的消息
     */
    suspend fun selectMessage(
        scenario: PushScenario,
        isPaidUser: Boolean,
        extras: Bundle? = null
    ): PushMessage? = withContext(Dispatchers.IO) {
        try {
            // 步骤 1: 获取所有消息
            val allMessages = messageRepository.getAllMessages()

            if (allMessages.isEmpty()) {
                if(BuildState.debug)
                "No messages available in repository".logw(PushOrchestrator.TAG)
                return@withContext null
            }

            if (BuildState.debug) {
                "Total messages available: ${allMessages.size}".logd(PushOrchestrator.TAG)
            }

            // 步骤 2: 获取上次发送的索引
            val lastIndex = SpUtils.getInt(KEY_LAST_INDEX, -1)

            // 步骤 3: 计算下一个索引（循环递增）
            val nextIndex = (lastIndex + 1) % allMessages.size

            if (BuildState.debug) {
                "Round-robin selection: lastIndex=$lastIndex, nextIndex=$nextIndex, total=${allMessages.size}".logd(PushOrchestrator.TAG)
            }

            // 步骤 4: 保存新索引
            SpUtils.putInt(KEY_LAST_INDEX, nextIndex)

            // 步骤 5: 返回对应消息
            val selectedMessage = allMessages[nextIndex]

            if (BuildState.debug) {
                "Selected message: id=${selectedMessage.id}, title=${selectedMessage.title}, index=$nextIndex".logd(PushOrchestrator.TAG)
            }

            selectedMessage

        } catch (e: Exception) {
            "Error selecting message: ${e.message}".logw(TAG)
            e.printStackTrace()
            null
        }
    }

    /**
     * 重置索引（用于测试或手动重置）
     */
    fun resetIndex() {
        SpUtils.putInt(KEY_LAST_INDEX, -1)
        if (BuildState.debug) {
            "Message index reset to -1".logd(TAG)
        }
    }

    /**
     * 获取当前索引（用于调试）
     */
    fun getCurrentIndex(): Int {
        return SpUtils.getInt(KEY_LAST_INDEX, -1)
    }
}
