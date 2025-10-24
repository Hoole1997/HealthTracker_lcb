package com.healthtracker.blood.suger.strategy

import com.healthtracker.blood.suger.config.models.PushMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 推送消息数据仓库
 *
 * 职责：
 * - 提供可用的推送消息列表（使用 PushMessage.createDefaultList() 中的 11 条消息）
 * - Phase 2: 硬编码消息池
 * - Phase 3: 从配置文件或服务器获取
 */
@Singleton
class PushMessageRepository @Inject constructor() {

    /**
     * 消息列表（使用 PushMessage.createDefaultList() 获取 11 条默认消息）
     */
    private val messages: List<PushMessage> by lazy {
        PushMessage.createDefaultList()
    }

    /**
     * 获取所有可用消息
     * @return 消息列表（11 条消息）
     */
    fun getAllMessages(): List<PushMessage> {
        return messages
    }

    /**
     * 获取消息总数
     * @return 消息数量
     */
    fun getMessageCount(): Int {
        return messages.size
    }

    /**
     * 根据场景获取消息（Phase 2 未使用，预留）
     * @param scenario 推送场景
     * @return 消息列表
     */
    fun getMessagesByScenario(scenario: PushScenario): List<PushMessage> {
        // Phase 2: 返回所有消息
        // Phase 3: 可根据场景过滤
        return messages
    }
}
