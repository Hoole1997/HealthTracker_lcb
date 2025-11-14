package com.healthtracker.earthquake.push

/**
 * 地震推送配置
 * 支持从外部设置低震级推送的最小间隔（默认 32 小时）。
 */
object EarthquakePushConfig {
    @Volatile
    var intervalHours: Long = 32L

    /**
     * 更新低震级推送的最小间隔（单位：小时）。
     * 避免与 Kotlin 生成的属性 setter 同名导致 JVM 签名冲突。
     */
    fun updateIntervalHours(hours: Long) {
        intervalHours = if (hours > 0) hours else 32L
    }

    val intervalMillis: Long
        get() = intervalHours * 60L * 60L * 1000L
}