package com.healthtracker.framework.lifecycle

/**
 * 应用前后台状态观察器接口
 *
 * 用于监听应用生命周期状态变化,所有方法都有默认空实现,
 * 实现类只需重写关心的方法即可
 *
 * 使用示例:
 * ```kotlin
 * AppLifecycleManager.addObserver(object : AppForegroundObserver {
 *     override fun onAppForeground() {
 *         // 应用进入前台时的逻辑
 *     }
 * })
 * ```
 *
 * ⚠️ 注意:
 * - 观察器在主线程回调,避免执行耗时操作
 * - 使用完毕后应调用 removeObserver() 防止内存泄漏
 */
interface AppForegroundObserver {

    /**
     * 应用进入前台
     * 触发时机: 从后台切回前台,至少有一个Activity可见
     */
    fun onAppForeground() {}

    /**
     * 应用进入后台
     * 触发时机: 所有Activity不可见,应用完全进入后台
     */
    fun onAppBackground() {}

    /**
     * 屏幕锁定
     * 触发时机: 用户锁定屏幕(按电源键或自动锁屏)
     * 可选实现,默认为空
     */
    fun onScreenLocked() {}

    /**
     * 屏幕解锁
     * 触发时机: 用户解锁屏幕
     * 可选实现,默认为空
     */
    fun onScreenUnlocked() {}

    /**
     * 状态变化通知(通用回调)
     * 触发时机: 任何状态发生变化时都会触发
     * @param newState 新的状态
     * @param oldState 旧的状态
     */
    fun onStateChanged(newState: AppLifecycleState, oldState: AppLifecycleState) {}
}
