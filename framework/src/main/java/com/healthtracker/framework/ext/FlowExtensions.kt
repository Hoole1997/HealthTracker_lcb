package com.healthtracker.framework.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Flow 生命周期扩展函数集合
 *
 * 提供便捷的Flow收集方法，自动处理生命周期管理
 * 可在Activity、Fragment、Compose等任何有LifecycleOwner的地方使用
 */

/**
 * 收集StateFlow的便捷方法 - 只处理最新值
 *
 * 特点：使用 collectLatest，只处理最新的值，会取消之前正在执行的收集块
 *
 * 使用场景：
 * - UI状态更新（如加载状态、错误状态、数据状态）
 * - 只需要最新状态的场景
 * - 避免过时的UI更新
 *
 * 示例：
 * ```kotlin
 * // 在Activity中
 * viewModel.isLoading.collectLatestLifecycle(this) { isLoading ->
 *     binding.progressBar.isVisible = isLoading
 * }
 *
 * // 在Fragment中
 * viewModel.userData.collectLatestLifecycle(viewLifecycleOwner) { userData ->
 *     updateUI(userData)
 * }
 * ```
 *
 * @param lifecycleOwner 生命周期所有者
 * @param minActiveState 最小活跃状态，默认为STARTED
 * @param action 收集到数据时的处理逻辑
 */
inline fun <T> StateFlow<T>.collectLatestLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            collectLatest { action(it) }
        }
    }
}

/**
 * 收集Flow的便捷方法 - 处理所有值
 *
 * 特点：使用普通 collect，会处理所有值，包括中间值，按顺序执行每个收集块
 *
 * 与 collectLatestLifecycle 的区别：
 * - collectLatestLifecycle：只处理最新值，会取消之前的操作
 * - collectLifecycle：处理所有值，不丢失任何事件
 *
 * 使用场景：
 * - 收集一次性事件流（如导航事件、错误事件）
 * - 处理用户操作响应流
 * - 监听网络状态变化
 * - 处理来自Repository的数据流
 * - 需要确保不丢失任何事件的场景
 *
 * 示例：
 * ```kotlin
 * // 事件流处理
 * viewModel.events.collectLifecycle(this) { event ->
 *     when (event) {
 *         is NavigateEvent -> navigateTo(event.target)
 *         is ShowErrorEvent -> showError(event.message)
 *     }
 * }
 *
 * // 数据流处理
 * repository.dataUpdates.collectLifecycle(this) { data ->
 *     processData(data) // 每次更新都要处理
 * }
 * ```
 *
 * @param lifecycleOwner 生命周期所有者
 * @param minActiveState 最小活跃状态，默认为STARTED
 * @param action 收集到数据时的处理逻辑
 */
inline fun <T> Flow<T>.collectLifecycle(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            collect { action(it) }
        }
    }
}

/**
 * 收集两个StateFlow的便捷方法 - 组合最新值
 *
 * 使用场景：
 * - 同时监听多个UI状态变化（如加载状态 + 数据状态）
 * - 组合多个数据源进行UI更新
 * - 处理复杂的表单状态（如输入验证 + 提交状态）
 * - 监听用户权限状态 + 数据加载状态
 *
 * 示例：
 * ```kotlin
 * collectCombined(
 *     this,
 *     viewModel.isLoading,
 *     viewModel.userData
 * ) { isLoading, userData ->
 *     binding.progressBar.isVisible = isLoading
 *     if (!isLoading && userData != null) {
 *         updateUI(userData)
 *     }
 * }
 * ```
 *
 * @param lifecycleOwner 生命周期所有者
 * @param stateFlow1 第一个StateFlow
 * @param stateFlow2 第二个StateFlow
 * @param minActiveState 最小活跃状态，默认为STARTED
 * @param action 收集到组合数据时的处理逻辑
 */
inline fun <T1, T2> collectCombined(
    lifecycleOwner: LifecycleOwner,
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T1, T2) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            combine(stateFlow1, stateFlow2) { value1, value2 ->
                value1 to value2
            }.collect { (value1, value2) ->
                action(value1, value2)
            }
        }
    }
}

/**
 * 收集三个StateFlow的便捷方法 - 组合最新值
 *
 * 示例：
 * ```kotlin
 * collectCombined(
 *     this,
 *     viewModel.isLoading,
 *     viewModel.userData,
 *     viewModel.permissions
 * ) { isLoading, userData, permissions ->
 *     updateComplexUI(isLoading, userData, permissions)
 * }
 * ```
 */
inline fun <T1, T2, T3> collectCombined(
    lifecycleOwner: LifecycleOwner,
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    stateFlow3: StateFlow<T3>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T1, T2, T3) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            combine(stateFlow1, stateFlow2, stateFlow3) { value1, value2, value3 ->
                Triple(value1, value2, value3)
            }.collect { (value1, value2, value3) ->
                action(value1, value2, value3)
            }
        }
    }
}

/**
 * 收集四个StateFlow的便捷方法 - 组合最新值
 */
inline fun <T1, T2, T3, T4> collectCombined(
    lifecycleOwner: LifecycleOwner,
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    stateFlow3: StateFlow<T3>,
    stateFlow4: StateFlow<T4>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T1, T2, T3, T4) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            combine(stateFlow1, stateFlow2, stateFlow3, stateFlow4) { value1, value2, value3, value4 ->
                listOf(value1, value2, value3, value4)
            }.collect { values ->
                @Suppress("UNCHECKED_CAST")
                action(values[0] as T1, values[1] as T2, values[2] as T3, values[3] as T4)
            }
        }
    }
}

/**
 * 为LifecycleOwner添加扩展方法，使语法更简洁
 */

/**
 * 简化的收集StateFlow方法 - 只处理最新值
 *
 * 示例：
 * ```kotlin
 * // 在Activity或Fragment中
 * collectLatest(viewModel.isLoading) { isLoading ->
 *     binding.progressBar.isVisible = isLoading
 * }
 * ```
 */
inline fun <T> LifecycleOwner.collectLatest(
    stateFlow: StateFlow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit
) {
    stateFlow.collectLatestLifecycle(this, minActiveState, action)
}

/**
 * 简化的收集Flow方法 - 处理所有值
 *
 * 示例：
 * ```kotlin
 * // 在Activity或Fragment中
 * collect(viewModel.events) { event ->
 *     handleEvent(event)
 * }
 * ```
 */
inline fun <T> LifecycleOwner.collect(
    flow: Flow<T>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit
) {
    flow.collectLifecycle(this, minActiveState, action)
}

/**
 * 简化的组合收集方法
 *
 * 示例：
 * ```kotlin
 * // 在Activity或Fragment中
 * collectCombined(viewModel.isLoading, viewModel.userData) { isLoading, userData ->
 *     updateUI(isLoading, userData)
 * }
 * ```
 */
inline fun <T1, T2> LifecycleOwner.collectCombined(
    stateFlow1: StateFlow<T1>,
    stateFlow2: StateFlow<T2>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (T1, T2) -> Unit
) {
    collectCombined(this, stateFlow1, stateFlow2, minActiveState, action)
}