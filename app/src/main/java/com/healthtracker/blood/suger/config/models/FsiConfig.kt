package com.healthtracker.blood.suger.config.models

/**
 * FSI（Full Screen Intent）锁屏推送配置
 *
 * 用于控制针对"推送沉默用户"的锁屏全屏提醒功能
 *
 * @property enabled 功能全局开关
 * @property quietPeriodHours 沉默期（新用户冷却时间，单位：小时）
 * @property timeWindow 触发时间窗口（SSEE编码，如 822 表示 8-22点）
 * @property maxTriggerCount 最大触发次数
 */
data class FsiConfig(
    val enabled: Boolean,
    val quietPeriodHours: Int,
    val timeWindow: Int,
    val maxTriggerCount: Int
) {
    companion object {
        /**
         * 创建默认FSI配置
         *
         * 默认配置说明：
         * - 启用FSI功能
         * - 新用户冷却24小时
         * - 全天候触发（0-23点，编码为 23）
         * - 最多触发3次
         */
        fun createDefault(): FsiConfig {
            return FsiConfig(
                enabled = true,
                quietPeriodHours = 24,
                timeWindow = 23,
                maxTriggerCount = 3
            )
        }
    }

    /**
     * 验证配置数据有效性
     */
    fun isValid(): Boolean {
        if (quietPeriodHours < 0 || maxTriggerCount < 0) {
            return false
        }

        val startHour = timeWindow / 100
        val endHour = timeWindow % 100

        return startHour in 0..23 &&
                endHour in 0..23 &&
                startHour <= endHour
    }

    /**
     * 检查当前时间是否在触发窗口内
     *
     * @param currentHour 当前小时（0-23）
     * @return true 如果当前时间在窗口内
     */
    fun isInTimeWindow(currentHour: Int): Boolean {
        if (!isValid()) {
            return true // 如果配置无效，默认允许
        }

        val startHour = timeWindow / 100
        val endHour = timeWindow % 100

        return currentHour in startHour..endHour
    }
}
