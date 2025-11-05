package com.healthtracker.framework.lifecycle

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 应用生命周期管理器
 *
 * 提供准确的前后台状态判断,结合多种检测机制:
 * 1. ProcessLifecycleOwner - 应用级生命周期(主要)
 * 2. ActivityLifecycleCallbacks - Activity级生命周期(辅助验证)
 * 3. BroadcastReceiver - 屏幕锁定/解锁监听(补充)
 *
 * 使用示例:
 * ```kotlin
 * // 1. 在Application.onCreate()中初始化
 * AppLifecycleManager.initialize(this)
 *
 * // 2. 同步获取状态
 * val isForeground = AppLifecycleManager.isForeground()
 *
 * // 3. 响应式订阅
 * lifecycleScope.launch {
 *     AppLifecycleManager.lifecycleState.collect { state ->
 *         // 处理状态变化
 *     }
 * }
 *
 * // 4. 添加观察器
 * AppLifecycleManager.addObserver(object : AppForegroundObserver {
 *     override fun onAppForeground() { }
 * })
 * ```
 */
object AppLifecycleManager {

    private const val TAG = "AppLifecycleManager"

    // 配置项
    private var enableDebugLog = false
    private var debounceMillis = 300L
    private var trackScreenLock = true

    var clickAdTime = 0L

    // 状态管理
    private val _lifecycleState = MutableStateFlow(AppLifecycleState.initial())
    val lifecycleState: StateFlow<AppLifecycleState> = _lifecycleState.asStateFlow()

    // 当前状态快照
    val currentState: AppLifecycleState
        get() = _lifecycleState.value

    // 观察器集合(线程安全)
    private val observers = CopyOnWriteArraySet<AppForegroundObserver>()

    // 内部状态追踪
    private var activeActivityCount = 0
    // ✅ 修复: 移除 isScreenLocked boolean，统一使用 StateFlow 作为唯一真相源
    // private var isScreenLocked = false  // 已移除
    private var isInitialized = false

    // ✅ 新增: 保存 Application 引用，用于 cleanup 方法
    private var application: Application? = null

    // 去抖动处理
    private val mainHandler = Handler(Looper.getMainLooper())
    // ✅ 线程安全: 添加 @Volatile 标记
    @Volatile
    private var pendingStateUpdate: Runnable? = null
    // ✅ 新增: 防抖最大延迟控制
    @Volatile
    private var firstPendingTime: Long? = null
    private val MAX_DEBOUNCE_DELAY = 3000L  // 最多延迟 3 秒

    // 屏幕锁定广播接收器
    private var screenStateReceiver: BroadcastReceiver? = null

    /**
     * 初始化管理器
     * ⚠️ 必须在Application.onCreate()中调用,且仅在主进程调用
     *
     * @param application Application实例
     */
    fun initialize(application: Application) {
        if (isInitialized) {
            logDebug("Already initialized, skipping")
            return
        }

        logDebug("Initializing AppLifecycleManager")

        // ✅ 保存 application 引用，用于 cleanup
        this.application = application

        // 1. 注册ProcessLifecycle监听
        setupProcessLifecycleObserver()

        // 2. 注册ActivityLifecycle监听
        setupActivityLifecycleCallbacks(application)

        // 3. 注册屏幕锁定监听
        if (trackScreenLock) {
            setupScreenLockReceiver(application)
        }

        isInitialized = true
        logDebug("AppLifecycleManager initialized successfully")
    }

    /**
     * 配置管理器参数
     * 应在initialize()之后,业务逻辑使用之前调用
     */
    fun configure(block: Configuration.() -> Unit) {
        val config = Configuration().apply(block)
        enableDebugLog = config.enableDebugLog
        debounceMillis = config.debounceMillis
        trackScreenLock = config.trackScreenLock
    }

    /**
     * 是否在前台
     */
    fun isForeground(): Boolean = currentState.isForeground

    /**
     * 是否在后台
     */
    fun isBackground(): Boolean = currentState.isBackground

    /**
     * 屏幕是否锁定
     * ✅ 修复: 从 StateFlow 读取，确保与状态流一致
     */
    fun isScreenLock(): Boolean = currentState.isScreenLocked

    /**
     * 添加观察器
     * @return true表示添加成功
     */
    fun addObserver(observer: AppForegroundObserver): Boolean {
        return observers.add(observer).also {
            logDebug("Observer added: ${observer.javaClass.simpleName}, total=${observers.size}")
        }
    }

    /**
     * 移除观察器
     * @return true表示移除成功
     */
    fun removeObserver(observer: AppForegroundObserver): Boolean {
        return observers.remove(observer).also {
            logDebug("Observer removed: ${observer.javaClass.simpleName}, remaining=${observers.size}")
        }
    }

    /**
     * 清除所有观察器
     */
    fun clearObservers() {
        observers.clear()
        logDebug("All observers cleared")
    }

    // ========== 内部实现 ==========

    /**
     * 设置ProcessLifecycle观察器(主要检测机制)
     */
    private fun setupProcessLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        logDebug("ProcessLifecycle: ON_START")
                        handleForeground()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        logDebug("ProcessLifecycle: ON_STOP")
                        handleBackground()
                    }
                    else -> {
                        // 忽略其他事件
                    }
                }
            }
        })
    }

    /**
     * 设置ActivityLifecycle回调(辅助验证)
     */
    private fun setupActivityLifecycleCallbacks(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                // 系统栏隐藏处理
                SysBarUtils.hideNavigationBar(activity)

                // ✅ 修复: 使用 onStart 而非 onResume
                // onStart 表示 Activity 可见，onResume 表示可交互
                // Dialog 显示时会触发 onPause 但 Activity 仍可见
                activeActivityCount++
                logDebug("Activity started: ${activity.javaClass.simpleName}, active count=$activeActivityCount")
            }

            override fun onActivityResumed(activity: Activity) {
                // onResume 不再用于计数，仅用于其他需要的逻辑
            }

            override fun onActivityPaused(activity: Activity) {
                // onPause 不再用于计数，避免 Dialog 等场景误判
            }

            override fun onActivityStopped(activity: Activity) {
                // ✅ 修复: 使用 onStop 而非 onPause
                // onStop 表示 Activity 不可见，是正确的后台判断时机
                activeActivityCount--
                logDebug("Activity stopped: ${activity.javaClass.simpleName}, active count=$activeActivityCount")
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * 设置屏幕锁定广播接收器(补充检测)
     */
    private fun setupScreenLockReceiver(application: Application) {
        screenStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        logDebug("Screen locked")
                        handleScreenLocked()
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        logDebug("User present (unlocked)")
                        handleScreenUnlocked()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        try {
            ContextCompat.registerReceiver(
                application,
                screenStateReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            logDebug("Screen state receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen state receiver", e)
        }
    }

    /**
     * 处理前台事件
     * ✅ 修复: 立即执行，不使用防抖，避免被其他事件（如锁屏）取消
     * ✅ 修复: 在执行时读取最新的 activeActivityCount，避免竞态条件
     */
    private fun handleForeground() {
        // ✅ 使用 mainHandler.post 立即加入消息队列，不会被 scheduleStateUpdate 取消
        // ProcessLifecycle 的 ON_START 是关键生命周期事件，应该立即响应
        mainHandler.post {
            val oldState = currentState
            val currentCount = this@AppLifecycleManager.activeActivityCount

            // ✅ 先构造不带时间戳的状态
            val stateWithoutTimestamp = oldState.copy(
                isForeground = true,
                isBackground = false,
                activeActivityCount = currentCount
            )

            // ✅ 检查是否真正改变（排除时间戳），只在状态改变时才更新时间戳
            if (stateWithoutTimestamp != oldState) {
                val newState = stateWithoutTimestamp.copy(
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
                logDebug("handleForeground: updating state from $oldState to $newState")
                updateState(newState, oldState)

                // 通知观察器
                notifyObservers { it.onAppForeground() }
            } else {
                logDebug("handleForeground: state unchanged, skipping update")
            }
        }
    }

    /**
     * 处理后台事件
     * ✅ 修复: 立即执行，不使用防抖，避免被其他事件（如锁屏）取消
     * ✅ 修复: 在执行时读取最新的 activeActivityCount，避免竞态条件
     */
    private fun handleBackground() {
        // ✅ 使用 mainHandler.post 立即加入消息队列，不会被 scheduleStateUpdate 取消
        // ProcessLifecycle 的 ON_STOP 是关键生命周期事件，应该立即响应
        mainHandler.post {
            val oldState = currentState
            val currentCount = this@AppLifecycleManager.activeActivityCount

            // ✅ 先构造不带时间戳的状态
            val stateWithoutTimestamp = oldState.copy(
                isForeground = false,
                isBackground = true,
                activeActivityCount = currentCount
            )

            // ✅ 检查是否真正改变（排除时间戳），只在状态改变时才更新时间戳
            if (stateWithoutTimestamp != oldState) {
                val newState = stateWithoutTimestamp.copy(
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
                logDebug("handleBackground: updating state from $oldState to $newState")
                updateState(newState, oldState)

                // 通知观察器
                notifyObservers { it.onAppBackground() }
            } else {
                logDebug("handleBackground: state unchanged, skipping update")
            }
        }
    }

    /**
     * 处理屏幕锁定
     * ✅ 修复: 移除立即赋值，统一在 StateFlow 更新时生效
     * ✅ 修复: 先检查状态是否改变，只在改变时才更新时间戳和通知观察器
     */
    private fun handleScreenLocked() {
        // 移除: isScreenLocked = true
        scheduleStateUpdate {
            val oldState = currentState

            // ✅ 先构造不带时间戳的状态
            val stateWithoutTimestamp = oldState.copy(
                isScreenLocked = true
            )

            // ✅ 检查是否真正改变（排除时间戳），只在状态改变时才更新时间戳
            if (stateWithoutTimestamp != oldState) {
                val newState = stateWithoutTimestamp.copy(
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
                logDebug("handleScreenLocked: updating state from $oldState to $newState")
                updateState(newState, oldState)

                // 通知观察器
                notifyObservers { it.onScreenLocked() }
            } else {
                logDebug("handleScreenLocked: state unchanged, skipping update")
            }
        }
    }

    /**
     * 处理屏幕解锁
     * ✅ 修复: 移除立即赋值，统一在 StateFlow 更新时生效
     * ✅ 修复: 先检查状态是否改变，只在改变时才更新时间戳和通知观察器
     */
    private fun handleScreenUnlocked() {
        // 移除: isScreenLocked = false
        scheduleStateUpdate {
            val oldState = currentState

            // ✅ 先构造不带时间戳的状态
            val stateWithoutTimestamp = oldState.copy(
                isScreenLocked = false
            )

            // ✅ 检查是否真正改变（排除时间戳），只在状态改变时才更新时间戳
            if (stateWithoutTimestamp != oldState) {
                val newState = stateWithoutTimestamp.copy(
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
                logDebug("handleScreenUnlocked: updating state from $oldState to $newState")
                updateState(newState, oldState)

                // 通知观察器
                notifyObservers { it.onScreenUnlocked() }
            } else {
                logDebug("handleScreenUnlocked: state unchanged, skipping update")
            }
        }
    }

    /**
     * 调度状态更新(去抖动 + 最大延迟限制)
     * ✅ 修复: 添加最大延迟限制，防止状态更新被无限期推迟
     * ✅ 线程安全: 使用 synchronized 保护共享状态
     */
    private fun scheduleStateUpdate(action: () -> Unit) {
        synchronized(this) {
            val now = System.currentTimeMillis()

            // 记录首次挂起时间
            if (firstPendingTime == null) {
                firstPendingTime = now
            }

            // 计算已等待时间
            val elapsed = now - (firstPendingTime ?: now)

            // 如果超过最大延迟，则立即执行
            val actualDelay = if (elapsed >= MAX_DEBOUNCE_DELAY) {
                logDebug("Max debounce delay reached ($elapsed ms), executing immediately")
                0L
            } else {
                debounceMillis
            }

            // 取消之前的待处理更新
            pendingStateUpdate?.let { mainHandler.removeCallbacks(it) }

            // 调度新的更新
            val runnable = Runnable {
                action()
                firstPendingTime = null  // 重置计时器
            }
            pendingStateUpdate = runnable
            mainHandler.postDelayed(runnable, actualDelay)
        }
    }

    /**
     * 更新状态
     */
    private fun updateState(newState: AppLifecycleState, oldState: AppLifecycleState) {
        _lifecycleState.value = newState
        logDebug("State updated: $oldState -> $newState")

        // 通知通用状态变化
        notifyObservers { it.onStateChanged(newState, oldState) }
    }

    /**
     * 通知所有观察器
     */
    private fun notifyObservers(action: (AppForegroundObserver) -> Unit) {
        observers.forEach { observer ->
            try {
                action(observer)
            } catch (e: Exception) {
                // ✅ 修复: 添加异常日志，避免静默失败
                android.util.Log.e(TAG, "Observer error: ${observer.javaClass.simpleName}", e)
                // 可选: 如果项目集成了 Firebase Crashlytics，可以在这里上报
                // FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    /**
     * 调试日志
     */
    private fun logDebug(message: String) {
        message.logd(TAG)
    }

    /**
     * 清理资源（主要用于测试）
     * ⚠️ 注意：生产环境不应调用此方法
     *
     * 此方法会：
     * 1. 注销 BroadcastReceiver
     * 2. 清除所有 Handler 回调
     * 3. 清空观察者集合
     * 4. 重置所有内部状态
     *
     * 用途：
     * - 单元测试中重置状态
     * - 集成测试中隔离测试环境
     * - 调试时手动清理（不推荐）
     */
    @VisibleForTesting
    fun cleanup() {
        logDebug("Cleaning up AppLifecycleManager...")

        // 1. 注销 BroadcastReceiver
        screenStateReceiver?.let { receiver ->
            try {
                application?.unregisterReceiver(receiver)
                logDebug("BroadcastReceiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "BroadcastReceiver already unregistered or not registered", e)
            }
        }
        screenStateReceiver = null

        // 2. 清除所有 Handler 回调
        mainHandler.removeCallbacksAndMessages(null)
        pendingStateUpdate = null
        firstPendingTime = null
        logDebug("Handler callbacks cleared")

        // 3. 清空观察者集合
        val observerCount = observers.size
        observers.clear()
        logDebug("$observerCount observers cleared")

        // 4. 重置内部状态
        activeActivityCount = 0
        isInitialized = false
        application = null

        // 5. 重置 StateFlow 到初始状态
        _lifecycleState.value = AppLifecycleState.initial()

        logDebug("AppLifecycleManager cleanup completed")
    }

    /**
     * 配置类
     */
    data class Configuration(
        var enableDebugLog: Boolean = true,
        var debounceMillis: Long = 300L,
        var trackScreenLock: Boolean = true
    )
}
