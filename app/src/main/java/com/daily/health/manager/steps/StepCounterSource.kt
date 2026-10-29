package com.daily.health.manager.steps

enum class StepTrackingStatus {
    STOPPED, ACTIVE, PERMISSION_REQUIRED, SENSOR_UNAVAILABLE, ERROR,
}

/** 采集端只负责权限和传感器；重复 start 必须保持幂等，不得重复注册监听器。 */
interface StepCounterSource {
    fun start(onReading: (StepCounterReading) -> Unit): StepTrackingStatus
    fun stop()
}
