package com.healthtracker.framework.ext

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fragment 的 Flow 扩展函数
 *
 * 为Fragment提供便捷的Flow收集方法，自动使用viewLifecycleOwner
 * 避免Fragment生命周期问题
 */

/**
 * Fragment中收集StateFlow的便捷方法 - 只处理最新值
 *
 * 自动使用viewLifecycleOwner，避免Fragment生命周期问题
 *
 * 示例：
 * ```kotlin
 * // 在Fragment中使用
 * collectLatest(viewModel.isLoading) { isLoading ->
 *     binding.progressBar.isVisible = isLoading
 * }
 * ```
 */
inline fun <T> Fragment.collectLatest(
    stateFlow: StateFlow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit
) {
    stateFlow.collectLatestLifecycle(viewLifecycleOwner, minActiveState, action)
}

/**
 * Fragment中收集Flow的便捷方法 - 处理所有值
 *
 * 示例：
 * ```kotlin
 * // 在Fragment中使用
 * collect(viewModel.events) { event ->
 *     handleEvent(event)
 * }
 * ```
 */
inline fun <T> Fragment.collect(
    flow: Flow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit
) {
    flow.collectLifecycle(viewLifecycleOwner, minActiveState, action)
}

/**
 * Fragment中组合收集两个StateFlow
 *
 * 示例：
 * ```kotlin
 * collectCombined(viewModel.isLoading, viewModel.userData) { isLoading, userData ->
 *     updateUI(isLoading, userData)
 * }
 * ```
 */
inline fun <T1, T2> Fragment.collectCombined(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T1, T2) -> Unit
) {
    collectCombined(viewLifecycleOwner, stateFlow1, stateFlow2, minActiveState, action)
}

/**
 * Fragment中组合收集三个StateFlow
 */
inline fun <T1, T2, T3> Fragment.collectCombined(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    stateFlow3: StateFlow<T3>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T1, T2, T3) -> Unit
) {
    collectCombined(viewLifecycleOwner, stateFlow1, stateFlow2, stateFlow3, minActiveState, action)
}