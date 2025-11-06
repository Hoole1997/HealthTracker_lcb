package com.healthtracker.blood.suger.config.models

/**
 * FSI（Full Screen Intent）锁屏推送配置
 *
 * 用于控制针对"推送沉默用户"的锁屏全屏提醒功能
 *
 * @property enabled 功能全局开关
 * @property quietPeriodHours 沉默时间（距离上次关闭首页的时间要求，单位：小时）
 *                            例如：12 表示用户 12 小时未使用 APP 才触发 FSI
 *                            注意：安装后冷却期固定为 24 小时，不受此参数影响
 * @property timeWindow 触发时间窗口（编码格式：startHour*100 + endHour，如 822 表示 8-22点）
 * @property maxTriggerCount 最大触发次数
 * @param delayInstallHour 安装后多久不发送，单位：h
 */
data class FsiConfig(
    val enabled: Boolean,
    val quietPeriodHours: Int,
    val timeWindow: Int,
    val maxTriggerCount: Int,
    val delayInstallHour:Int
) {
    companion object {
        /**
         * 创建默认FSI配置
         *
         * 默认配置说明：
         * - 启用FSI功能
         * - 沉默时间 12 小时（用户 12 小时未使用才触发）
         * - 安装后冷却期固定 24 小时（代码中硬编码）
         * - 全天候触发（0-23点，编码为 23）
         * - 最多触发3次
         */
        fun createDefault(): FsiConfig {
            return FsiConfig(
                enabled = true,
                quietPeriodHours = 12,
                timeWindow = 23,
                maxTriggerCount = 3,
                delayInstallHour = 12
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
