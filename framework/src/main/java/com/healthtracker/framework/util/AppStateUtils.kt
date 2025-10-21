package com.healthtracker.framework.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.healthtracker.framework.lifecycle.AppLifecycleState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 应用状态便捷工具类
 *
 * 提供简化的API访问应用前后台状态,内部委托给AppLifecycleManager
 *
 * 使用示例:
 * ```kotlin
 * // 简单判断
 * if (AppStateUtils.isForeground()) {
 *     // 前台逻辑
 * }
 *
 * // 响应式订阅
 * lifecycleScope.launch {
 *     AppStateUtils.lifecycleState.collect { state ->
 *         // 处理状态变化
 *     }
 * }
 * ```
 */
object AppStateUtils {

    /**
     * 应用是否在前台
     * @return true表示至少有一个Activity可见
     */
    fun isForeground(): Boolean = AppLifecycleManager.isForeground()

    /**
     * 应用是否在后台
     * @return true表示所有Activity都不可见
     */
    fun isBackground(): Boolean = AppLifecycleManager.isBackground()

    /**
     * 屏幕是否锁定
     * @return true表示屏幕已锁定
     */
    fun isScreenLocked(): Boolean = AppLifecycleManager.currentState.isScreenLocked

    /**
     * 应用是否真正活跃(前台且未锁屏)
     * @return true表示应用在前台且屏幕未锁定
     */
    fun isActiveForeground(): Boolean = AppLifecycleManager.currentState.isActiveForeground

    /**
     * 应用是否完全后台(包括锁屏场景)
     * @return true表示应用在后台或屏幕已锁定
     */
    fun isCompleteBackground(): Boolean = AppLifecycleManager.currentState.isCompleteBackground

    /**
     * 当前活跃的Activity数量
     * @return 活跃Activity数量
     */
    fun getActiveActivityCount(): Int = AppLifecycleManager.currentState.activeActivityCount

    /**
     * 获取当前完整状态
     * @return 当前状态快照
     */
    fun getCurrentState(): AppLifecycleState = AppLifecycleManager.currentState

    /**
     * 响应式状态流
     * 可在协程中collect订阅状态变化
     */
    val lifecycleState: StateFlow<AppLifecycleState>
        get() = AppLifecycleManager.lifecycleState
}

/**
 * ViewModel扩展函数 - 观察应用生命周期
 *
 * 在ViewModel中便捷地监听前后台状态变化
 *
 * 使用示例:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     init {
 *         observeAppLifecycle(
 *             onForeground = { refreshData() },
 *             onBackground = { pauseOperations() }
 *         )
 *     }
 * }
 * ```
 *
 * @param onForeground 进入前台时的回调
 * @param onBackground 进入后台时的回调
 * @param onScreenLocked 屏幕锁定时的回调(可选)
 * @param onScreenUnlocked 屏幕解锁时的回调(可选)
 */
fun ViewModel.observeAppLifecycle(
    onForeground: () -> Unit = {},
    onBackground: () -> Unit = {},
    onScreenLocked: () -> Unit = {},
    onScreenUnlocked: () -> Unit = {}
) {
    viewModelScope.launch {
        var previousState = AppStateUtils.getCurrentState()

        AppStateUtils.lifecycleState.collect { state ->
            // 检测前台变化
            if (state.isForeground && !previousState.isForeground) {
                onForeground()
            }

            // 检测后台变化
            if (state.isBackground && !previousState.isBackground) {
                onBackground()
            }

            // 检测锁屏变化
            if (state.isScreenLocked && !previousState.isScreenLocked) {
                onScreenLocked()
            }

            // 检测解锁变化
            if (!state.isScreenLocked && previousState.isScreenLocked) {
                onScreenUnlocked()
            }

            previousState = state
        }
    }
}
