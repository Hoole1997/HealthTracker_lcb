package com.daily.health.manager.config

/**
 * 应用Remote Config配置键
 */
object ConfigKeys {
    // ========== 推送配置 ==========

    /**
     * 推送消息内容数组（多语言支持）
     * 格式: JSON 数组，包含所有推送消息模板（支持 en/ja/ko 三种语言）
     * 
     * JSON 示例:
     * [
     *   {
     *     "id": "push_001",
     *     "iconType": 1,
     *     "actionType": 1,
     *     "en": { "title": "...", "content": "...", "buttonText": "..." },
     *     "ja": { "title": "...", "content": "...", "buttonText": "..." },
     *     "ko": { "title": "...", "content": "...", "buttonText": "..." }
     *   }
     * ]
     */
    const val PUSH_ARRAY = "push_array"

    /**
     * 推送策略配置
     * 格式: JSON 对象，包含付费和自然渠道的推送策略
     */
    const val PUSH_CONFIG_JSON = "pushConfigJson"

    /**
     * AI 助手开关
     * 0: 关闭, 1: 开启
     */
    const val AI_SWITCH = "AI_Switch"
}
