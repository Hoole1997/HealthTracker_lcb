package com.healthtracker.blood.suger.strategy

import com.healthtracker.blood.suger.config.models.PushConfig
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.framework.config.core.RemoteConfigManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 推送消息数据仓库
 *
 * 职责：
 * - 提供可用的推送消息列表（优先从 Remote Config 获取多语言配置）
 * - 降级方案：使用 PushMessage.createDefaultList() 中的默认消息
 */
@Singleton
class PushMessageRepository @Inject constructor(
    private val configManager: RemoteConfigManager
) {

    /**
     * 获取所有可用消息
     * 
     * 优先级：
     * 1. Remote Config 中的多语言配置（已根据系统语言转换）
     * 2. 本地默认配置（纯英文）
     * 
     * @return 消息列表
     */
    fun getAllMessages(): List<PushMessage> {
        return try {
            // 从 Remote Config 获取 PushConfig
            val pushConfig = configManager.getConfig<PushConfig>()
            
            // 返回已经过多语言解析的推送消息列表
            pushConfig.pushMessages.takeIf { it.isNotEmpty() }
                ?: PushMessage.createDefaultList()
        } catch (e: Exception) {
            // 降级到本地默认配置
            PushMessage.createDefaultList()
        }
    }

    /**
     * 获取消息总数
     * @return 消息数量
     */
    fun getMessageCount(): Int {
        return getAllMessages().size
    }

    /**
     * 根据场景获取消息（预留扩展）
     * @param scenario 推送场景
     * @return 消息列表
     */
    fun getMessagesByScenario(scenario: PushScenario): List<PushMessage> {
        // 目前返回所有消息，未来可根据场景过滤
        return getAllMessages()
    }
}
