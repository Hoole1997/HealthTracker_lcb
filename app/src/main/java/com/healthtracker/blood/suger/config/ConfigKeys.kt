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
     * FSI功能全局开关
     * 格式: Boolean
     * 取值: true = 启用, false = 禁用
     * 默认值: true
     */
    const val FSI_ENABLED = "fsi_enabled"

    /**
     * FSI沉默期（新用户冷却时间）
     * 格式: String (兼容旧配置) 或 Integer
     * 单位: 小时
     * 默认值: "24"
     * 说明: 用户安装后需等待此时间才会触发FSI
     */
    const val FSI_QUIET_PERIOD = "fsi_quiet_period"

    /**
     * FSI触发时间窗口
     * 格式: Integer (SSEE编码，SS=开始小时，EE=结束小时)
     * 取值范围: 0-2323 (每位 0-23)
     * 默认值: 23 (0023 表示全天 0点到23点)
     * 示例:
     *   - 822 表示 8点到22点
     *   - 918 表示 9点到18点
     *   - 23 表示 0点到23点（全天）
     * 编码规则: (startHour * 100) + endHour
     */
    const val FSI_TIME_WINDOW = "fsi_time_window"

    /**
     * FSI最大触发次数
     * 格式: Integer
     * 默认值: 3
     * 说明: FSI提醒最多展示的次数，防止过度骚扰
     */
    const val FSI_MAX_TRIGGER_COUNT = "fsi_max_trigger_count"
}
