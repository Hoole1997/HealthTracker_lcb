package com.healthtracker.framework.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

