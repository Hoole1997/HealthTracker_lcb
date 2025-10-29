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

    // ========== FSI锁屏推送配置 ==========



    /**
     * FSI配置聚合JSON
     * 示例: {"fsi_enable":"true","quiet_period":"12","time_window":"07:00-23:00","max_prompts":"3"}
     */
    const val FSI_CONFIG_JSON = "fsi_config"
}
