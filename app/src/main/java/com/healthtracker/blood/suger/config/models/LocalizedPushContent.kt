package com.healthtracker.blood.suger.config.models

/**
 * 本地化推送内容
 *
 * 表示单个语言的推送消息内容
 *
 * @property title 推送标题
 * @property content 推送内容描述
 * @property buttonText 按钮文字
 */
data class LocalizedPushContent(
    val title: String,
    val content: String,
    val buttonText: String
) : java.io.Serializable {
    /**
     * 验证内容有效性
     */
    fun isValid(): Boolean {
        return title.isNotBlank() &&
                content.isNotBlank() &&
                buttonText.isNotBlank()
    }
}
