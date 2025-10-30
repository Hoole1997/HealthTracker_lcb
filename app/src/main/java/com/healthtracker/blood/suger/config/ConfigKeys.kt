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
     *
     * 配置说明：
     * - fsi_enable: 功能开关（true/false）
     * - quiet_period: 沉默时间（小时），用户多久未使用APP才触发FSI（默认12）
     * - time_window: 触发时间窗口，格式 "HH:mm-HH:mm"（如 "08:00-22:00"）
     * - max_prompts: 最大触发次数（默认3）
     *
     * 注意：安装后冷却期固定为 24 小时，由代码控制，不在此配置
     *
     * 示例: {"fsi_enable":"true","quiet_period":"12","time_window":"08:00-22:00","max_prompts":"3"}
     */
    const val FSI_CONFIG_JSON = "fsi_config"
}
