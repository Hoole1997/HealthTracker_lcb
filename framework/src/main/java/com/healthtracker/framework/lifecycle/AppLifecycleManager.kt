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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.healthtracker.framework.BuildState
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
    private var isScreenLocked = false
    private var isInitialized = false

    // 去抖动处理
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingStateUpdate: Runnable? = null

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

    fun isScreenLock() = isScreenLocked

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
            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                activeActivityCount++
                logDebug("Activity resumed: ${activity.javaClass.simpleName}, active count=$activeActivityCount")
            }

            override fun onActivityPaused(activity: Activity) {
                activeActivityCount--
                logDebug("Activity paused: ${activity.javaClass.simpleName}, active count=$activeActivityCount")
            }

            override fun onActivityStopped(activity: Activity) {}
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
                    Intent.ACTION_SCREEN_ON -> {
                        logDebug("Screen unlocked")
                        handleScreenUnlocked()
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
            addAction(Intent.ACTION_SCREEN_ON)
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
     */
    private fun handleForeground() {
        scheduleStateUpdate {
            val oldState = currentState
            if (oldState.isBackground) {
                val newState = oldState.copy(
                    isForeground = true,
                    isBackground = false,
                    activeActivityCount = activeActivityCount,
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
                updateState(newState, oldState)

                // 通知观察器
                notifyObservers { it.onAppForeground() }
            }
        }
    }

    /**
     * 处理后台事件
     */
    private fun handleBackground() {
        scheduleStateUpdate {
            val oldState = currentState
            if (oldState.isForeground) {
                val newState = oldState.copy(
                    isForeground = false,
                    isBackground = true,
                    activeActivityCount = activeActivityCount,
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
                updateState(newState, oldState)

                // 通知观察器
                notifyObservers { it.onAppBackground() }
            }
        }
    }

    /**
     * 处理屏幕锁定
     */
    private fun handleScreenLocked() {
        isScreenLocked = true
        scheduleStateUpdate {
            val oldState = currentState
            val newState = oldState.copy(
                isScreenLocked = true,
                lastStateChangeTimestamp = System.currentTimeMillis()
            )
            updateState(newState, oldState)

            // 通知观察器
            notifyObservers { it.onScreenLocked() }
        }
    }

    /**
     * 处理屏幕解锁
     */
    private fun handleScreenUnlocked() {
        isScreenLocked = false
        scheduleStateUpdate {
            val oldState = currentState
            val newState = oldState.copy(
                isScreenLocked = false,
                lastStateChangeTimestamp = System.currentTimeMillis()
            )
            updateState(newState, oldState)

            // 通知观察器
            notifyObservers { it.onScreenUnlocked() }
        }
    }

    /**
     * 调度状态更新(去抖动)
     */
    private fun scheduleStateUpdate(action: () -> Unit) {
        // 取消之前的待处理更新
        pendingStateUpdate?.let { mainHandler.removeCallbacks(it) }

        // 调度新的更新
        val runnable = Runnable { action() }
        pendingStateUpdate = runnable
        mainHandler.postDelayed(runnable, debounceMillis)
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
     * 配置类
     */
    data class Configuration(
        var enableDebugLog: Boolean = true,
        var debounceMillis: Long = 300L,
        var trackScreenLock: Boolean = true
    )
}
