package com.healthtracker.framework.config.models

/**
 * 推送消息内容模型
 *
 * 表示单条推送消息的内容和行为
 *
 * @property id 消息唯一标识
 * @property title 推送标题
 * @property desc 推送描述内容
 * @property buttonText 按钮文本
 * @property iconType 图标类型 (1-8 对应不同的健康指标图标)
 * @property actionType 点击行为类型 (1-8 对应不同的跳转页面)
 */
data class PushMessage(
    val id: String,
    val title: String,
    val desc: String,
    val buttonText: String,
    val iconType: Int,
    val actionType: Int
) {
    companion object {
        /**
         * 创建默认推送消息
         */
        fun createDefault(): PushMessage {
            return PushMessage(
                id = "default_001",
                title = "Health Check Reminder",
                desc = "It's time to log your health data",
                buttonText = "Log Now",
                iconType = 1,
                actionType = 1
            )
        }
    }

    /**
     * 验证消息数据有效性
     */
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                title.isNotBlank() &&
                desc.isNotBlank() &&
                buttonText.isNotBlank() &&
                iconType in 1..8 &&
                actionType in 1..8
    }
}
