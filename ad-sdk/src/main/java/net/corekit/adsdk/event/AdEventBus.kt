package net.corekit.adsdk.event

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.corekit.adsdk.util.AdLogger
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 广告事件总线
 * 负责事件的发布和订阅管理
 *
 * ## 📌 重要：三层事件体系
 *
 * SDK 提供三种事件监听机制，适用不同场景：
 *
 * ### 1️⃣ show() 返回值 - 最重要的结果
 * **用途**: 获取广告展示的最终结果（关闭、奖励）
 * **作用域**: 单次 show() 调用
 * **推荐场景**: 所有需要处理展示结果的场景
 *
 * ```kotlin
 * val result = AdKit.show(activity, AdType.REWARDED)
 * when (result) {
 *     is AdResult.Success -> {
 *         // 检查是否获得奖励
 *         if (result.data.hasReward) {
 *             grantReward(result.data.reward!!)
 *         }
 *     }
 *     is AdResult.Failure -> showError()
 * }
 * ```
 *
 * ### 2️⃣ onEvent 回调 - 单次展示的中间事件
 * **用途**: 监听当前广告实例的中间事件（展示、点击等）
 * **作用域**: 单次 show() 调用（不会收到其他页面/实例的事件）
 * **推荐场景**: 页面级 UI 更新、单次展示的数据追踪
 *
 * ```kotlin
 * AdKit.show(
 *     activity = this,
 *     adType = AdType.NATIVE,
 *     onEvent = { event ->  // ✅ 仅接收当前广告的事件
 *         when (event) {
 *             is AdEvent.Impression -> {
 *                 // 更新页面UI
 *                 showLoadingIndicator()
 *             }
 *             is AdEvent.Click -> {
 *                 // 追踪点击（仅当前广告）
 *                 trackAdClick()
 *             }
 *         }
 *     }
 * )
 * ```
 *
 * ### 3️⃣ EventBus（本类）- 全局广告监控
 * **用途**: 跨页面监控所有广告事件
 * **作用域**: 全局（接收所有页面、所有实例的事件）
 * **推荐场景**: 统计上报、全局调试、中台数据对接
 *
 * ```kotlin
 * // Application.onCreate()
 * AdKit.addEventListener { event ->
 *     // ✅ 接收所有页面的所有广告事件
 *     when (event) {
 *         is AdEvent.Impression -> reportToAnalytics(event)
 *         is AdEvent.Click -> reportClick(event)
 *         is AdEvent.LoadFailed -> reportError(event)
 *     }
 * }
 * ```
 *
 * ## 💡 选择指南
 *
 * | 场景 | 推荐方式 | 理由 |
 * |------|---------|------|
 * | 页面级UI更新 | onEvent 回调 | 不会收到其他页面的事件 |
 * | 获取奖励信息 | show() 返回值 | 直接拿到结果，无需监听 |
 * | 全局数据上报 | EventBus | 统一收集所有广告数据 |
 * | 调试广告流程 | EventBus | 监控所有环节的事件 |
 *
 * ## ⚠️ 常见误区
 *
 * ❌ **错误用法**：多个页面展示同类型广告，用 EventBus 监听
 * ```kotlin
 * // 页面A
 * AdKit.show(this, AdType.NATIVE)
 * AdKit.addEventListener(this) { event ->
 *     // ❌ 会收到页面B的原生广告事件！
 *     handleEvent(event)
 * }
 *
 * // 页面B
 * AdKit.show(this, AdType.NATIVE)
 * ```
 *
 * ✅ **正确用法**：使用 onEvent 回调
 * ```kotlin
 * // 页面A
 * AdKit.show(this, AdType.NATIVE, onEvent = { event ->
 *     // ✅ 仅接收页面A的广告事件
 *     handleEvent(event)
 * })
 *
 * // 页面B
 * AdKit.show(this, AdType.NATIVE, onEvent = { event ->
 *     // ✅ 仅接收页面B的广告事件
 *     handleEvent(event)
 * })
 * ```
 *
 * ---
 *
 * ## EventBus 使用说明（全局监控场景）
 *
 * 特性：
 * - 线程安全（使用 CopyOnWriteArrayList）
 * - 异常隔离（单个监听器异常不影响其他监听器）
 * - 支持多监听器
 * - 生命周期感知（推荐使用，零内存泄漏）
 *
 * ## 推荐使用方式（生命周期感知 - 零内存泄漏）
 *
 * 对于 Activity 和 Fragment，推荐使用生命周期感知的注册方式：
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // ✅ 推荐：自动管理生命周期，支持匿名监听器
 *         AdKit.addEventListener(this, object : AdEventListener {
 *             override fun onEvent(event: AdEvent) {
 *                 when (event) {
 *                     is AdEvent.Click -> handleClick(event)
 *                     is AdEvent.Impression -> handleImpression(event)
 *                     else -> {}
 *                 }
 *             }
 *         })
 *     }
 *     // 无需 onDestroy，自动清理
 * }
 * ```
 *
 * 也可以使用成员变量（如果需要在多处访问）：
 * ```kotlin
 * class MyActivity : AppCompatActivity() {
 *     private val myListener = object : AdEventListener {
 *         override fun onEvent(event: AdEvent) { ... }
 *     }
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         AdKit.addEventListener(this, myListener)  // 自动管理
 *     }
 * }
 * ```
 *
 * ## 手动管理方式（特殊场景）
 *
 * 对于非 LifecycleOwner 的场景（如 Service、Application 级监听器），
 * 使用传统的手动注册/注销方式：
 * ```kotlin
 * class MyApplication : Application() {
 *     private val listener = object : AdEventListener { ... }
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         AdKit.addEventListener(listener)  // 手动注册
 *     }
 *
 *     override fun onTerminate() {
 *         AdKit.removeEventListener(listener)  // 手动注销
 *         super.onTerminate()
 *     }
 * }
 * ```
 *
 * ⚠️ **内存泄漏警告（仅手动管理方式）**：
 * 如果使用 `register(listener)` 手动注册方式，必须在适当的时机调用 `unregister(listener)`。
 */
internal class AdEventBus {
    // 使用 CopyOnWriteArrayList 保证线程安全
    // 适合读多写少的场景（监听器注册/注销少，事件发布多）
    private val listeners = CopyOnWriteArrayList<AdEventListener>()

    /**
     * 注册事件监听器
     *
     * @param listener 事件监听器
     */
    fun register(listener: AdEventListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            AdLogger.d("Event listener registered, total: ${listeners.size}")
        }
    }

    /**
     * 注册事件监听器（生命周期感知 - 推荐）
     *
     * 监听器会在 LifecycleOwner 销毁时自动注销，无需手动调用 unregister()
     *
     * **支持匿名监听器**：
     * ```kotlin
     * // ✅ 安全：匿名监听器会被自动管理
     * AdKit.addEventListener(this, object : AdEventListener {
     *     override fun onEvent(event: AdEvent) { ... }
     * })
     * ```
     *
     * 推荐使用此方法，适用于 Activity 和 Fragment
     *
     * @param owner LifecycleOwner (Activity 或 Fragment)
     * @param listener 事件监听器（可以是匿名对象）
     */
    fun register(owner: LifecycleOwner, listener: AdEventListener) {
        // 如果已注册，不重复添加
        if (listeners.contains(listener)) {
            AdLogger.d("Event listener already registered")
            return
        }

        // 添加监听器到列表（强引用）
        listeners.add(listener)
        AdLogger.d("Event listener registered with lifecycle, total: ${listeners.size}")

        // 创建生命周期观察器
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // 移除监听器
                unregister(listener)

                // 🔧 关键：移除自己，避免内存泄漏
                owner.lifecycle.removeObserver(this)

                AdLogger.d("Event listener and observer auto-removed on lifecycle destroy")
            }
        }

        // 注册生命周期观察器
        owner.lifecycle.addObserver(observer)
    }

    /**
     * 注销事件监听器
     *
     * @param listener 事件监听器
     */
    fun unregister(listener: AdEventListener) {
        if (listeners.remove(listener)) {
            AdLogger.d("Event listener unregistered, remaining: ${listeners.size}")
        }
    }

    /**
     * 清空所有监听器
     */
    fun clearAll() {
        val count = listeners.size
        listeners.clear()
        AdLogger.d("All event listeners cleared, count: $count")
    }

    /**
     * 发布事件
     * 依次通知所有已注册的监听器
     *
     * @param event 广告事件
     */
    fun post(event: AdEvent) {
        if (listeners.isEmpty()) {
            AdLogger.d("No listeners registered for event: ${event::class.simpleName}")
            return
        }

        // 遍历所有监听器并通知
        listeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (e: Exception) {
                // 隔离异常，防止单个监听器异常影响其他监听器
                AdLogger.e("Event listener error for ${event::class.simpleName}", e)
            }
        }
    }

    /**
     * 获取监听器数量
     */
    fun getListenerCount(): Int = listeners.size
}
