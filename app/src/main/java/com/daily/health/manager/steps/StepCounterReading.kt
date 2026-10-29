package com.daily.health.manager.steps

/** 系统计数与所属启动周期一起传递，避免重启后的计数与旧计数直接相减。 */
data class StepCounterReading(
    val rawSteps: Int,
    val bootCount: Int,
    val recordedAtMillis: Long,
) {
    companion object {
        const val UNKNOWN_BOOT_COUNT = -1
    }
}

data class StepCounterState(
    val steps: Int,
    val baselineRaw: Int,
    val lastRaw: Int,
    val bootCount: Int,
)

/** 无 Android 依赖的累计规则，数据库和传感器均不参与计算。 */
internal object StepCounterAccumulator {
    fun update(previous: StepCounterState?, rawSteps: Int, bootCount: Int): StepCounterState {
        require(rawSteps >= 0)
        if (previous == null) return StepCounterState(0, rawSteps, rawSteps, bootCount)

        val rebooted = previous.bootCount != StepCounterReading.UNKNOWN_BOOT_COUNT &&
            bootCount != StepCounterReading.UNKNOWN_BOOT_COUNT && previous.bootCount != bootCount
        val counterReset = rebooted || rawSteps < previous.lastRaw
        // 重启后传感器从零开始；旧的当天累计不变，再加上本次启动以来的新步数。
        val delta = if (counterReset) rawSteps else rawSteps - previous.lastRaw
        val total = (previous.steps.toLong() + delta).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return StepCounterState(
            steps = total,
            baselineRaw = if (counterReset) 0 else previous.baselineRaw,
            lastRaw = rawSteps,
            bootCount = bootCount,
        )
    }
}
