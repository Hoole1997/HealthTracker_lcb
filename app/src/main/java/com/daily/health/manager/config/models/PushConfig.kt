package com.daily.health.manager.config.models

/**
 * 推送配置总模型
 *
 * 包含所有推送相关的配置信息
 *
 * @property paidChannel 付费渠道配置
 * @property organicChannel 自然量渠道配置
 * @property pushMessages 推送消息内容列表
 */
data class PushConfig(
    val paidChannel: ChannelConfig,
    val organicChannel: ChannelConfig,
    val pushMessages: List<PushMessage>
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(): PushConfig {
            return PushConfig(
                paidChannel = ChannelConfig.createDefaultPaid(),
                organicChannel = ChannelConfig.createDefaultOrganic(),
                pushMessages = PushMessage.createDefaultList()
            )
        }
    }

    /**
     * 验证配置有效性
     */
    fun isValid(): Boolean {
        return paidChannel.isValid() &&
                organicChannel.isValid() &&
                pushMessages.isNotEmpty() &&
                pushMessages.all { it.isValid() }
    }

    /**
     * 根据渠道类型获取配置
     *
     * @param isPaidChannel 是否为付费渠道
     * @return 对应的渠道配置
     */
    fun getChannelConfig(isPaidChannel: Boolean): ChannelConfig {
        return if (isPaidChannel) paidChannel else organicChannel
    }

    /**
     * 根据 ID 获取推送消息
     *
     * @param messageId 消息 ID
     * @return 对应的推送消息，未找到返回 null
     */
    fun getPushMessageById(messageId: String): PushMessage? {
        return pushMessages.find { it.id == messageId }
    }

    /**
     * 根据图标类型获取推送消息列表
     *
     * @param iconType 图标类型
     * @return 符合条件的推送消息列表
     */
    fun getPushMessagesByIconType(iconType: Int): List<PushMessage> {
        return pushMessages.filter { it.iconType == iconType }
    }

    /**
     * 根据行为类型获取推送消息列表
     *
     * @param actionType 行为类型
     * @return 符合条件的推送消息列表
     */
    fun getPushMessagesByActionType(actionType: Int): List<PushMessage> {
        return pushMessages.filter { it.actionType == actionType }
    }

    /**
     * 获取随机推送消息
     *
     * @return 随机选择的推送消息，列表为空返回 null
     */
    fun getRandomPushMessage(): PushMessage? {
        return pushMessages.randomOrNull()
    }

    /**
     * 获取推送消息总数
     */
    fun getPushMessageCount(): Int = pushMessages.size

    /**
     * 检查推送消息是否存在
     *
     * @param messageId 消息 ID
     * @return true 如果存在
     */
    fun hasPushMessage(messageId: String): Boolean {
        return pushMessages.any { it.id == messageId }
    }
}
