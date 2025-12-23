package com.daily.health.manager.config.models

/**
 * 推送消息内容模型
 *
 * 表示单条推送消息的内容和行为
 *
 * @property id 消息唯一标识
 * @property title 推送标题
 * @property desc 推送描述内容
 * @property buttonText 按钮文本
 * @property iconType 图标类型 (1-10 对应不同的健康指标图标)
 * @property actionType 点击行为类型 (1-10 对应不同的跳转页面)
 */
data class PushMessage(
    val id: String,
    val title: String,
    val desc: String,
    val buttonText: String,
    val iconType: Int,
    val actionType: Int
) : java.io.Serializable {
    companion object {
        /**
         * 创建默认推送消息
         */
        fun createDefault(): PushMessage {
            return PushMessage(
                id = "default_001",
                title = "Health Check Reminder",
                desc = "It's time to log your health data",
                buttonText = "Log Now",
                iconType = 1,
                actionType = 1
            )
        }

        /**
         * 创建默认推送消息列表（完整版）
         *
         * 包含与 RemoteConfig 一致的全部 13 条推送消息配置
         * 用于 RemoteConfig 获取失败时的降级方案
         *
         * 数据来源: Blood Pressure & Sugar Track v1.0.1 商业化需求文档
         *
         * @return 包含 13 条完整推送消息的列表
         */
        fun createDefaultList(): List<PushMessage> {
            return listOf(
                // 1. 每日健康检查提醒
                PushMessage(
                    id = "push_001",
                    title = "💖 Your Daily Health Check-in!",
                    desc = "Tracking your blood sugar, blood pressure, and weight is the first step to wellness. Have you measured today?",
                    buttonText = "RECOVER NOW",
                    iconType = 1,
                    actionType = 1
                ),
                // 2. 健康数据记录提醒
                PushMessage(
                    id = "push_002",
                    title = "📝 Time to Log Your Health Data!",
                    desc = "Take a minute to record your blood sugar/pressure. Tracking trends is the first step to better health.",
                    buttonText = "Log Now",
                    iconType = 1,
                    actionType = 1
                ),
                // 3. 血糖检查提醒
                PushMessage(
                    id = "push_003",
                    title = "🩸 Time to Check Your Blood Sugar!",
                    desc = "Log your levels for the day and guard your health at every step.",
                    buttonText = "Log Now",
                    iconType = 2,
                    actionType = 2
                ),
                // 4. 血压检查提醒
                PushMessage(
                    id = "push_004",
                    title = "💓 Time for Your Blood Pressure Check!",
                    desc = "Take a minute to log your BP and keep your heart health in check.",
                    buttonText = " Measure Now",
                    iconType = 3,
                    actionType = 3
                ),
                // 5. 血糖状态检查
                PushMessage(
                    id = "push_005",
                    title = "🩸 Is Your Sugar in Check? ",
                    desc = " Your body is talking. One quick tap to log your glucose and stay in the green zone.",
                    buttonText = "Log Now",
                    iconType = 2,
                    actionType = 2
                ),
                // 6. 胆固醇记录提醒
                PushMessage(
                    id = "push_006",
                    title = "🧪 Don't Forget Your Cholesterol!",
                    desc = "Regularly knowing your levels is key to cardiovascular health.",
                    buttonText = "Log Now",
                    iconType = 4,
                    actionType = 4
                ),
                // 7. 心脏健康提醒
                PushMessage(
                    id = "push_007",
                    title = "❤️ One Minute for a Healthier Heart? ",
                    desc = "Shed light on your cholesterol and build a healthier future, now.",
                    buttonText = "Log Now",
                    iconType = 4,
                    actionType = 4
                ),
                // 8. 每周体重检查
                PushMessage(
                    id = "push_008",
                    title = "⚖️ It's Weekly Weigh-In Time!",
                    desc = "Step on the scale, log your BMI, and manage your long-term health.",
                    buttonText = "Log Now",
                    iconType = 5,
                    actionType = 5
                ),
                // 9. 心率记录
                PushMessage(
                    id = "push_009",
                    title = "❤️ Heart Rate Record",
                    desc = "Log your resting heart rate to understand your heart's healthy rhythm.",
                    buttonText = "Log Now",
                    iconType = 6,
                    actionType = 6
                ),
                // 10. 健康报告更新
                PushMessage(
                    id = "push_010",
                    title = "📊 Your Health Report is Updated!",
                    desc = "Blood sugar, blood pressure, heart rate... All your key metrics at a glance. Check out your health trends.",
                    buttonText = "View Full Report",
                    iconType = 7,
                    actionType = 7
                ),
                // 11. 用药提醒
                PushMessage(
                    id = "push_011",
                    title = " It's time to take medicine.",
                    desc = " It's time to take medicine.",
                    buttonText = "View Full Report",
                    iconType = 8,
                    actionType = 8
                ),
                // 12. 饮水提醒
                PushMessage(
                    id = "push_012",
                    title = "💧 Stay Hydrated!",
                    desc = "Don't forget to drink water! Staying hydrated is essential for your health and well-being.",
                    buttonText = "Drink Now",
                    iconType = 9,
                    actionType = 9
                ),
                // 13. 计步提醒
                PushMessage(
                    id = "push_013",
                    title = "👟 Keep Moving!",
                    desc = "Track your daily steps and stay active. Every step counts towards a healthier you!",
                    buttonText = "View Steps",
                    iconType = 10,
                    actionType = 10
                ),
                // 14. 助手来电
                PushMessage(
                    id = "push_022",
                    title = "Your Health Assistant",
                    desc = "I'm here to remind you to add your [type] record!",//[type]用来占位的，需要替换具体的血糖，血压，心率
                    buttonText = "",
                    iconType = 12,
                    actionType = 12
                )
            )
        }
    }

    /**
     * 验证消息数据有效性
     */
    fun isValid(): Boolean {
        if (id.isBlank()) return false

        // 助手来电：允许 buttonText 为空
        if (iconType == 12 || actionType == 12) {
            return title.isNotBlank() &&
                    desc.isNotBlank() &&
                    iconType == 12 &&
                    actionType == 12
        }

        return title.isNotBlank() &&
                desc.isNotBlank() &&
                buttonText.isNotBlank() &&
                iconType in 1..10 &&
                actionType in 1..10
    }
}
