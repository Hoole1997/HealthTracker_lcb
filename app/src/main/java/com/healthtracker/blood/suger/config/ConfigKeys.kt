package com.healthtracker.blood.suger.config

/**
 * 应用Remote Config配置键
 */
object ConfigKeys {
    // ========== 推送配置 ==========

    /**
     * 推送消息内容数组
     * 格式: JSON 数组，包含所有推送消息模板
     */
    const val PUSH_CONTENT_ARRAY = "push_content_array"

    /**
     * 推送策略配置
     * 格式: JSON 对象，包含付费和自然渠道的推送策略
     */
    const val PUSH_CONFIG_JSON = "pushConfigJson"
}
