package com.healthtracker.framework.lifecycle

/**
 * 应用生命周期状态数据类
 *
 * 不可变设计,线程安全,用于描述应用当前的运行状态
 *
 * @property isForeground 是否在前台(至少有一个Activity可见)
 * @property isBackground 是否在后台(所有Activity不可见)
 * @property isScreenLocked 屏幕是否锁定
 * @property activeActivityCount 当前活跃(resumed)的Activity数量
 * @property lastStateChangeTimestamp 上次状态变化的时间戳(毫秒)
 */
data class AppLifecycleState(
    val isForeground: Boolean,
    val isBackground: Boolean,
    val isScreenLocked: Boolean,
    val activeActivityCount: Int,
    val lastStateChangeTimestamp: Long
) {
    /**
     * 应用是否真正在前台且屏幕未锁定
     * 某些场景下锁屏虽然触发onPause,但业务逻辑上不算完全后台
     */
    val isActiveForeground: Boolean
        get() = isForeground && !isScreenLocked

    /**
     * 应用是否完全处于后台(包括锁屏场景)
     */
    val isCompleteBackground: Boolean
        get() = isBackground || isScreenLocked

    companion object {
        /**
         * 初始状态(应用启动时默认为后台)
         */
        fun initial() = AppLifecycleState(
            isForeground = false,
            isBackground = true,
            isScreenLocked = false,
            activeActivityCount = 0,
            lastStateChangeTimestamp = System.currentTimeMillis()
        )
    }

    override fun toString(): String {
        return "AppLifecycleState(foreground=$isForeground, background=$isBackground, " +
                "locked=$isScreenLocked, activeActivities=$activeActivityCount, " +
                "timestamp=$lastStateChangeTimestamp)"
    }
}
