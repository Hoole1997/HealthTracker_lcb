package com.healthtracker.framework.config.core

/**
 * 配置键定义
 * 所有 Remote Config 的键统一在此管理
 *
 * 使用规范:
 * - 使用下划线分隔的小写命名
 * - 添加注释说明配置用途
 * - 按业务模块分组
 */
object ConfigKey {

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

    // ========== 广告配置 ==========

    /**
     * 广告总开关配置
     * 格式: JSON 对象，控制广告展示策略
     */
    const val AD_CONFIG = "ad_config"

    /**
     * 广告位配置
     * 格式: JSON 数组，定义各个广告位的参数
     */
    const val AD_PLACEMENT_CONFIG = "ad_placement_config"

    // ========== 功能开关 ==========

    /**
     * 功能开关配置
     * 格式: JSON 对象，控制各功能模块的开启/关闭
     */
    const val FEATURE_FLAGS = "feature_flags"

    // ========== AB 测试 ==========

    /**
     * AB 测试配置
     * 格式: JSON 对象，定义实验变体分配
     */
    const val AB_TEST_CONFIG = "ab_test_config"

    // ========== 业务配置 ==========

    /**
     * 健康建议内容配置
     * 格式: JSON 数组，包含健康提示内容
     */
    const val HEALTH_TIPS_CONFIG = "health_tips_config"

    /**
     * 提醒策略配置
     * 格式: JSON 对象，定义各类提醒的触发条件
     */
    const val REMINDER_CONFIG = "reminder_config"

    /**
     * 应用更新配置
     * 格式: JSON 对象，控制强制更新和推荐更新策略
     */
    const val APP_UPDATE_CONFIG = "app_update_config"
}
