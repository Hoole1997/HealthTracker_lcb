package com.daily.health.manager.strategy

/**
 * 推送场景类型
 */
enum class PushScenario {
    /** 解锁场景 */
    UNLOCK,

    /** 后台场景 */
    BACKGROUND,

    /** 保活场景 */
    KEEPALIVE,

    /** FCM 推送场景 */
    FCM
}

/**
 * 频率检查结果
 */
data class FrequencyCheckResult(
    /** 是否可以触发推送 */
    val canTrigger: Boolean,

    /** 不能触发的原因（仅当 canTrigger = false 时有值） */
    val reason: String? = null
)

/**
 * 推送结果
 */
sealed class PushResult {
    /** 成功推送 */
    data class Success(val pushId: String) : PushResult()

    /** 被阻止（频率限制、免打扰等） */
    data class Blocked(val reason: String) : PushResult()

    /** 没有合适的推送消息 */
    object NoSuitableMessage : PushResult()

    /** 执行错误 */
    data class Error(val exception: Exception) : PushResult()
}
