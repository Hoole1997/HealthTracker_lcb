package net.corekit.monetize.ads.lifecycle

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import net.corekit.monetize.ads.log.AdLogger
import kotlin.coroutines.resume

/**
 * 广告生命周期守卫
 * 
 * 统一处理所有广告展示前的生命周期检查：
 * 1. Activity 是否已销毁
 * 2. 应用是否在后台
 * 3. 屏幕是否锁定
 * 4. Activity 是否在 RESUMED 状态
 * 
 * 使用示例：
 * ```kotlin
 * suspend fun showAd(activity: Activity): AdResult<Unit> {
 *     val result = AdLifecycleGuard.awaitReady(activity)
 *     if (result !is CheckResult.Ready) {
 *         return AdResult.Failure(AdException(-400, "生命周期检查失败: $result"))
 *     }
 *     // 展示广告...
 * }
 * ```
 */
object AdLifecycleGuard {

    private const val TAG = "AdLifecycleGuard"
    
    // 等待 Activity 恢复的默认最大时间
    private const val DEFAULT_MAX_WAIT_RESUME_MS = 5000L

    /**
     * 检查结果
     */
    sealed class CheckResult {
        /** 可以展示广告 */
        object Ready : CheckResult() {
            override fun toString() = "Ready"
        }
        
        /** Activity 已销毁或正在结束 */
        object ActivityFinishing : CheckResult() {
            override fun toString() = "ActivityFinishing"
        }
        
        /** 应用在后台 */
        object AppInBackground : CheckResult() {
            override fun toString() = "AppInBackground"
        }
        
        /** 屏幕已锁定 */
        object ScreenLocked : CheckResult() {
            override fun toString() = "ScreenLocked"
        }
        
        /** 等待恢复超时 */
        data class Timeout(val reason: String) : CheckResult() {
            override fun toString() = "Timeout($reason)"
        }
        
        /** Activity 不在 RESUMED 状态，需要等待 */
        data class WaitingResume(val currentState: String) : CheckResult() {
            override fun toString() = "WaitingResume($currentState)"
        }
    }

    /**
     * 立即检查 Activity 是否可以展示广告（不等待）
     * 
     * @param activity 目标 Activity
     * @return 检查结果
     */
    fun checkImmediate(activity: Activity): CheckResult {
        // 1. 检查 Activity 状态
        if (activity.isFinishing || activity.isDestroyed) {
            AdLogger.d("[$TAG] Activity ${activity.simpleName} is finishing/destroyed")
            return CheckResult.ActivityFinishing
        }
        
        // 2. 检查应用前后台状态
        if (AppLifecycleManager.isBackground()) {
            AdLogger.d("[$TAG] App is in background")
            return CheckResult.AppInBackground
        }
        
        // 3. 检查屏幕锁定状态
        if (AppLifecycleManager.isScreenLock()) {
            AdLogger.d("[$TAG] Screen is locked")
            return CheckResult.ScreenLocked
        }
        
        // 4. 检查 Activity 是否在 RESUMED 状态
        if (activity is LifecycleOwner) {
            val lifecycle = activity.lifecycle
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val stateStr = lifecycle.currentState.name
                AdLogger.d("[$TAG] Activity ${activity.simpleName} not RESUMED (current: $stateStr)")
                return CheckResult.WaitingResume(stateStr)
            }
        }
        
        AdLogger.d("[$TAG] Activity ${activity.simpleName} ready for ad display")
        return CheckResult.Ready
    }

    /**
     * 等待 Activity 可以展示广告（阻塞等待，带超时）
     * 
     * 如果 Activity 当前不在 RESUMED 状态但仍然有效，会等待 onResume 回调。
     * 
     * @param activity 目标 Activity
     * @param timeoutMs 最大等待时间（毫秒）
     * @return 检查结果
     */
    suspend fun awaitReady(
        activity: Activity,
        timeoutMs: Long = DEFAULT_MAX_WAIT_RESUME_MS
    ): CheckResult {
        // 先做立即检查
        val immediateResult = checkImmediate(activity)
        
        // 如果不需要等待，直接返回
        when (immediateResult) {
            is CheckResult.Ready,
            is CheckResult.ActivityFinishing,
            is CheckResult.AppInBackground,
            is CheckResult.ScreenLocked,
            is CheckResult.Timeout -> return immediateResult
            is CheckResult.WaitingResume -> {
                // 需要继续等待
            }
        }
        
        // 需要等待 Activity 恢复
        if (activity !is LifecycleOwner) {
            AdLogger.w("[$TAG] Activity is not LifecycleOwner, cannot wait for resume")
            return CheckResult.Timeout("Activity is not LifecycleOwner")
        }
        
        AdLogger.d("[$TAG] Waiting for Activity ${activity.simpleName} to resume (timeout: ${timeoutMs}ms)...")
        
        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { continuation ->
                    val observer = ResumeLifecycleObserver(activity, continuation)
                    
                    // 直接添加观察者（假设当前在主线程，或由生命周期内部处理）
                    activity.lifecycle.addObserver(observer)
                    
                    continuation.invokeOnCancellation {
                        activity.lifecycle.removeObserver(observer)
                        AdLogger.d("[$TAG] Wait for resume cancelled")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdLogger.w("[$TAG] Wait for resume timeout after ${timeoutMs}ms")
            CheckResult.Timeout("Wait for resume timeout after ${timeoutMs}ms")
        }
    }

    /**
     * Activity 恢复生命周期观察者
     */
    private class ResumeLifecycleObserver(
        private val activity: Activity,
        private val continuation: CancellableContinuation<CheckResult>
    ) : DefaultLifecycleObserver {
        
        override fun onResume(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            
            if (!continuation.isActive) return
            
            // 恢复后再次检查各种状态
            val result = when {
                activity.isFinishing || activity.isDestroyed -> {
                    AdLogger.d("[$TAG] Activity ${activity.simpleName} is finishing after resume event")
                    CheckResult.ActivityFinishing
                }
                AppLifecycleManager.isBackground() -> {
                    AdLogger.d("[$TAG] App went to background after resume event")
                    CheckResult.AppInBackground
                }
                AppLifecycleManager.isScreenLock() -> {
                    AdLogger.d("[$TAG] Screen locked after resume event")
                    CheckResult.ScreenLocked
                }
                else -> {
                    AdLogger.d("[$TAG] Activity ${activity.simpleName} resumed, ready for ad display")
                    CheckResult.Ready
                }
            }
            
            continuation.resume(result)
        }
        
        override fun onDestroy(owner: LifecycleOwner) {
            owner.lifecycle.removeObserver(this)
            
            if (!continuation.isActive) return
            
            AdLogger.d("[$TAG] Activity ${activity.simpleName} destroyed while waiting for resume")
            continuation.resume(CheckResult.ActivityFinishing)
        }
    }

    /**
     * 扩展：获取 Activity 简名
     */
    private val Activity.simpleName: String
        get() = this.javaClass.simpleName
}
