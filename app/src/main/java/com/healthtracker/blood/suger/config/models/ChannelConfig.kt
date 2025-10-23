package com.healthtracker.blood.suger.config.models

/**
 * 渠道推送配置模型
 *
 * 针对不同渠道（付费/自然量）的差异化推送策略
 *
 * @property totalPushCount 每日推送总次数限制
 * @property unlockPushInterval 解锁推送间隔（小时）
 * @property backgroundPushInterval 后台推送间隔（小时）
 * @property hoverDurationStrategySwitch 悬浮时长策略开关 (0=关闭, 1=开启)
 * @property hoverDurationLoopCount 悬浮时长循环次数
 * @property newUserCooldown 新用户冷却期（小时）
 * @property doNotDisturbStart 免打扰开始时间 (格式: "HH:mm")
 * @property doNotDisturbEnd 免打扰结束时间 (格式: "HH:mm")
 * @property notificationEnabled 通知开关 (0=关闭, 1=开启)
 * @property keepalivePollingIntervalMinutes 保活轮询间隔（分钟）
 */
data class ChannelConfig(
    val totalPushCount: Int,
    val unlockPushInterval: String,
    val backgroundPushInterval: String,
    val hoverDurationStrategySwitch: Int,
    val hoverDurationLoopCount: Int,
    val newUserCooldown: String,
    val doNotDisturbStart: String,
    val doNotDisturbEnd: String,
    val notificationEnabled: Int,
    val keepalivePollingIntervalMinutes: Int
) {
    companion object {
        /**
         * 创建默认付费渠道配置
         */
        fun createDefaultPaid(): ChannelConfig {
            return ChannelConfig(
                totalPushCount = 999,
                unlockPushInterval = "10",
                backgroundPushInterval = "10",
                hoverDurationStrategySwitch = 1,
                hoverDurationLoopCount = 9,
                newUserCooldown = "0",
                doNotDisturbStart = "02:00",
                doNotDisturbEnd = "07:00",
                notificationEnabled = 1,
                keepalivePollingIntervalMinutes = 15
            )
        }

        /**
         * 创建默认自然量渠道配置
         */
        fun createDefaultOrganic(): ChannelConfig {
            return ChannelConfig(
                totalPushCount = 3,
                unlockPushInterval = "10",
                backgroundPushInterval = "10",
                hoverDurationStrategySwitch = 0,
                hoverDurationLoopCount = 0,
                newUserCooldown = "24",
                doNotDisturbStart = "02:00",
                doNotDisturbEnd = "08:00",
                notificationEnabled = 1,
                keepalivePollingIntervalMinutes = 15
            )
        }
    }

    /**
     * 验证配置有效性
     */
    fun isValid(): Boolean {
        return totalPushCount > 0 &&
                unlockPushInterval.toIntOrNull() != null &&
                backgroundPushInterval.toIntOrNull() != null &&
                hoverDurationStrategySwitch in 0..1 &&
                hoverDurationLoopCount >= 0 &&
                newUserCooldown.toIntOrNull() != null &&
                doNotDisturbStart.matches(Regex("\\d{2}:\\d{2}")) &&
                doNotDisturbEnd.matches(Regex("\\d{2}:\\d{2}")) &&
                notificationEnabled in 0..1 &&
                keepalivePollingIntervalMinutes > 0
    }

    /**
     * 获取解锁推送间隔（小时）
     */
    fun getUnlockPushIntervalHours(): Int = unlockPushInterval.toIntOrNull() ?: 10

    /**
     * 获取后台推送间隔（小时）
     */
    fun getBackgroundPushIntervalHours(): Int = backgroundPushInterval.toIntOrNull() ?: 10

    /**
     * 获取新用户冷却期（小时）
     */
    fun getNewUserCooldownHours(): Int = newUserCooldown.toIntOrNull() ?: 24

    /**
     * 检查是否在免打扰时段
     *
     * @param currentTime 当前时间（格式: "HH:mm"）
     * @return true 如果在免打扰时段
     */
    fun isInDoNotDisturbPeriod(currentTime: String): Boolean {
        // 简单的时间比较，实际使用时建议转换为 LocalTime 进行比较
        val start = doNotDisturbStart.replace(":", "").toIntOrNull() ?: 0
        val end = doNotDisturbEnd.replace(":", "").toIntOrNull() ?: 0
        val current = currentTime.replace(":", "").toIntOrNull() ?: 0

        return if (start < end) {
            current in start..end
        } else {
            // 跨越午夜的情况 (例如: 22:00 ~ 07:00)
            current >= start || current <= end
        }
    }
}
