package net.corekit.monetize.ads

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ui.NativeAdStyle

/**
 * 原生广告自动刷新管理器
 * 
 * 功能说明：
 * 1. 广告成功展示后开始30秒倒计时
 * 2. 广告不可见时停止计时
 * 3. 广告再次可见时，重新请求并展示广告，计时从0开始
 * 4. 全屏原生广告类型不参与自动刷新
 * 
 * 使用方式：
 * ```
 * val refreshManager = NativeAdAutoRefreshManager(
 *     container = adContainer,
 *     style = NativeAdStyle.STANDARD,
 *     lifecycleOwner = this,
 *     onRefresh = { /* 刷新回调 */ }
 * )
 * refreshManager.startRefreshTimer()
 * ```
 */
class NativeAdAutoRefreshManager(
    private val container: ViewGroup,
    private val style: NativeAdStyle,
    private val lifecycleOwner: LifecycleOwner,
    private val onRefresh: suspend () -> Boolean,
    private val onClick: (() -> Unit)? = null
) {
    
    companion object {
        private const val TAG = "NativeAdAutoRefresh"
        
        /** 默认自动刷新间隔：30秒 */
        const val DEFAULT_REFRESH_INTERVAL_MS = 30 * 1000L
        
        /** 可见后冷却时间：防止频繁切换时重复刷新（毫秒） */
        private const val VISIBILITY_COOLDOWN_MS = 2000L
        
        /** Dwell Time：可见后需要停留的时间才触发刷新（毫秒） */
        private const val DWELL_TIME_MS = 1500L
    }
    
    /** 
     * 获取当前配置的刷新间隔（毫秒）
     * 优先使用在线配置，若未配置则使用默认值
     */
    private fun getRefreshIntervalMs(): Long {
        return try {
            AdConfigManager.getNativeAdRefreshIntervalMs()
        } catch (e: Exception) {
            AdLogger.w("[$TAG] 获取刷新间隔配置失败，使用默认值: ${DEFAULT_REFRESH_INTERVAL_MS}ms")
            DEFAULT_REFRESH_INTERVAL_MS
        }
    }
    
    private val handler = Handler(Looper.getMainLooper())
    
    /** 刷新任务 */
    private val refreshRunnable = Runnable {
        AdLogger.d("[$TAG] 计时完成，触发广告刷新")
        performRefresh()
    }
    
    /** 是否已启动 */
    private var isStarted = false
    
    /** 是否可见 */
    private var isVisible = true
    
    /** 上次可见状态 */
    private var lastVisibleState = true
    
    /** 是否已销毁 */
    private var isDestroyed = false
    
    /** 协程作用域 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /** 当前刷新任务（用于取消竞态） */
    private var currentRefreshJob: Job? = null
    
    /** 上次刷新时间（用于冷却检测） */
    private var lastRefreshTime = 0L
    
    /** Dwell Time 检测任务 */
    private val dwellTimeRunnable = Runnable {
        AdLogger.d("[$TAG] Dwell Time 检测通过，执行刷新")
        performRefreshInternal()
    }
    
    /** View 附加状态监听器 */
    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            AdLogger.d("[$TAG] View 已附加到窗口")
            onVisibilityChanged(true)
        }
        
        override fun onViewDetachedFromWindow(v: View) {
            AdLogger.d("[$TAG] View 已从窗口分离")
            onVisibilityChanged(false)
        }
    }
    
    /** 生命周期观察者 */
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            AdLogger.d("[$TAG] 生命周期 onResume")
            onVisibilityChanged(true)
        }
        
        override fun onPause(owner: LifecycleOwner) {
            AdLogger.d("[$TAG] 生命周期 onPause")
            onVisibilityChanged(false)
        }
        
        override fun onDestroy(owner: LifecycleOwner) {
            AdLogger.d("[$TAG] 生命周期 onDestroy，释放资源")
            release()
        }
    }
    
    init {
        // 注册生命周期观察者
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        // 注册 View 附加状态监听器
        container.addOnAttachStateChangeListener(attachStateListener)
    }
    
    /**
     * 广告展示成功后调用，启动刷新计时器
     */
    fun startRefreshTimer() {
        if (isDestroyed) {
            AdLogger.w("[$TAG] 管理器已销毁，无法启动计时器")
            return
        }
        
        isStarted = true
        isVisible = true
        lastVisibleState = true
        
        AdLogger.d("[$TAG] 启动30秒刷新计时器")
        scheduleRefresh()
    }
    
    /**
     * 可见性变化处理
     * @param visible 是否可见
     */
    private fun onVisibilityChanged(visible: Boolean) {
        if (isDestroyed || !isStarted) return
        
        isVisible = visible
        
        // 状态没有变化，直接返回
        if (lastVisibleState == visible) return
        lastVisibleState = visible
        
        if (visible) {
            // 从不可见变为可见：检查冷却时间，避免频繁刷新
            val timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTime
            if (timeSinceLastRefresh < VISIBILITY_COOLDOWN_MS) {
                AdLogger.d("[$TAG] 可见性变化冷却中，距上次刷新 ${timeSinceLastRefresh}ms，跳过刷新，继续计时")
                // 不刷新，但重新开始计时
                scheduleRefresh()
                return
            }
            
            // 使用 Dwell Time 检测：用户需要停留一定时间才触发刷新
            AdLogger.d("[$TAG] 广告从不可见变为可见，启动 Dwell Time 检测 (${DWELL_TIME_MS}ms)")
            handler.removeCallbacks(dwellTimeRunnable)
            handler.postDelayed(dwellTimeRunnable, DWELL_TIME_MS)
        } else {
            // 从可见变为不可见：停止计时和 Dwell Time 检测
            AdLogger.d("[$TAG] 广告从可见变为不可见，停止计时")
            handler.removeCallbacks(dwellTimeRunnable)
            cancelRefresh()
        }
    }
    
    /**
     * 调度刷新任务
     */
    private fun scheduleRefresh() {
        cancelRefresh()
        val intervalMs = getRefreshIntervalMs()
        handler.postDelayed(refreshRunnable, intervalMs)
        AdLogger.d("[$TAG] 已调度刷新任务，将在 ${intervalMs / 1000} 秒后执行")
    }
    
    /**
     * 取消刷新任务
     */
    private fun cancelRefresh() {
        handler.removeCallbacks(refreshRunnable)
        AdLogger.d("[$TAG] 已取消刷新任务")
    }
    
    /**
     * 执行刷新（由定时器触发）
     */
    private fun performRefresh() {
        if (isDestroyed || !isStarted) return
        performRefreshInternal()
    }
    
    /**
     * 内部刷新逻辑
     * 修复问题：
     * 1. 刷新失败后继续计时，而非永久停止
     * 2. 使用 Job 管理协程，避免竞态条件
     * 3. 记录刷新时间，用于冷却检测
     */
    private fun performRefreshInternal() {
        if (isDestroyed || !isStarted) return
        
        cancelRefresh()
        
        // 取消上一个未完成的刷新任务，避免竞态
        currentRefreshJob?.cancel()
        
        currentRefreshJob = scope.launch {
            try {
                AdLogger.d("[$TAG] 开始刷新原生广告")
                lastRefreshTime = System.currentTimeMillis()
                
                val success = onRefresh.invoke()
                
                if (isStarted && !isDestroyed && isVisible) {
                    if (success) {
                        // 刷新成功，重新开始计时
                        AdLogger.d("[$TAG] 广告刷新成功，重新开始计时")
                    } else {
                        // 刷新失败（可能被拦截），仍然继续计时，避免永久停止
                        AdLogger.w("[$TAG] 广告刷新失败，继续计时等待下次刷新")
                    }
                    // 无论成功失败，都继续计时（修复永久停止问题）
                    scheduleRefresh()
                }
            } catch (e: Exception) {
                AdLogger.e("[$TAG] 广告刷新异常", e)
                // 异常情况也继续计时
                if (isStarted && !isDestroyed && isVisible) {
                    scheduleRefresh()
                }
            }
        }
    }
    
    /**
     * 手动停止自动刷新
     */
    fun stop() {
        AdLogger.d("[$TAG] 手动停止自动刷新")
        isStarted = false
        cancelRefresh()
    }
    
    /**
     * 释放资源
     */
    fun release() {
        if (isDestroyed) return
        
        isDestroyed = true
        isStarted = false
        cancelRefresh()
        
        // 取消当前刷新任务和 Dwell Time 检测
        currentRefreshJob?.cancel()
        handler.removeCallbacks(dwellTimeRunnable)
        
        // 取消协程作用域
        try {
            scope.cancel()
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 取消协程作用域失败", e)
        }
        
        try {
            container.removeOnAttachStateChangeListener(attachStateListener)
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 移除 View 监听器失败", e)
        }
        
        try {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 移除生命周期观察者失败", e)
        }
        
        AdLogger.d("[$TAG] 资源已释放")
    }
}
