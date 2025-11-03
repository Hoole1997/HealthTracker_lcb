package net.corekit.adsdk.core

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.corekit.adsdk.event.AdEvent
import net.corekit.adsdk.event.AdEventBus
import net.corekit.adsdk.metric.AdMetrics
import net.corekit.adsdk.service.AdCache
import net.corekit.adsdk.service.AdLoader
import net.corekit.adsdk.util.AdLogger
import net.corekit.adsdk.util.awaitWithBusinessTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * 广告控制器抽象基类
 * 封装了所有广告类型的通用逻辑，包括：
 * - 预加载和缓存管理
 * - 展示流程控制
 * - 事件发布
 * - 指标统计
 *
 * 子类只需实现 showInternal() 方法处理平台特定的展示逻辑
 *
 * @param T 平台广告对象类型
 * @param adType 广告类型
 * @param loader 广告加载器
 * @param cache 广告缓存
 * @param eventBus 事件总线
 * @param metrics 指标统计
 * @param maxCacheSize 最大缓存数量（默认 2）
 * @param loadTimeoutMillis 即时加载时的超时时间，单位毫秒，null 表示不限制
 */
internal abstract class BaseAdController<T>(
    override val adType: AdType,
    protected val loader: AdLoader<T>,
    protected val cache: AdCache<T>,
    protected val eventBus: AdEventBus,
    protected val metrics: AdMetrics,
    private val maxCacheSize: Int = 2,
    private val loadTimeoutMillis: Long? = null
) : AdController<T> {

    // 🔧 FIX: 管理的协程作用域，防止内存泄漏
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 🔧 FIX: 防止并发 show() 调用的竞态条件
    private val isShowingAd = AtomicBoolean(false)

    /**
     * 待恢复的展示请求
     * 当 Activity 暂时不可交互时保存，等待恢复后继续展示
     *
     * @param ad 广告对象
     * @param adUnitId 广告位 ID
     * @param onEvent 事件回调
     * @param continuation 挂起的协程 continuation，用于恢复执行
     * @param wasFromCache 广告是否来自缓存
     */
    private data class PendingShowRequest<T>(
        val ad: T,
        val adUnitId: String,
        val onEvent: ((AdEvent) -> Unit)?,
        val continuation: CancellableContinuation<AdResult<AdShowData>>,
        val wasFromCache: Boolean
    )

    /**
     * 当前待恢复的展示请求
     * 只保存一个请求，新请求会覆盖旧请求
     */
    private var pendingRequest: PendingShowRequest<T>? = null

    /**
     * 预加载锁映射：adUnitId -> 是否正在预加载
     *
     * 用于防止同一广告位的并发预加载：
     * - 当预加载开始时，设置为 true
     * - 当预加载完成（成功或失败）时，重置为 false
     * - 使用 AtomicBoolean 保证线程安全的 CAS 操作
     */
    private val preloadLocks = java.util.concurrent.ConcurrentHashMap<String, AtomicBoolean>()

    /**
     * 预加载广告到缓存
     *
     * 流程：
     * 1. 检查缓存是否已满
     * 2. 发布加载开始事件
     * 3. 调用 loader 加载广告
     * 4. 成功后加入缓存并发布成功事件
     * 5. 失败则发布失败事件
     */
    override suspend fun preload(adUnitId: String): AdResult<Unit> {
        return try {
            // 1. 检查缓存
            if (cache.size(adUnitId) >= maxCacheSize) {
                AdLogger.d("[$adType] Cache full for $adUnitId, skip preload")
                return AdResult.Success(Unit)
            }

            // 2. 发布加载开始事件
            eventBus.post(AdEvent.LoadStart(adType, adUnitId))
            metrics.incrementLoad(adType)

            val startTime = System.currentTimeMillis()

            // 3. 加载广告
            when (val result = loader.load(adUnitId)) {
                is AdResult.Success -> {
                    val duration = System.currentTimeMillis() - startTime

                    // 4. 加入缓存
                    cache.cache(adUnitId, result.data)

                    // 5. 发布成功事件
                    eventBus.post(AdEvent.LoadSuccess(adType, adUnitId, duration))
                    AdLogger.d("[$adType] Preload success for $adUnitId, duration: ${duration}ms")

                    AdResult.Success(Unit)
                }

                is AdResult.Failure -> {
                    val duration = System.currentTimeMillis() - startTime

                    // 发布失败事件
                    eventBus.post(AdEvent.LoadFailure(
                        adType,
                        adUnitId,
                        result.error.message,
                        duration
                    ))
                    AdLogger.e("[$adType] Preload failed for $adUnitId: ${result.error.message}")

                    result
                }

                else -> AdResult.Failure(AdException.from(Exception("Unexpected load result")))
            }
        } catch (e: Exception) {
            AdLogger.e("[$adType] Preload exception for $adUnitId", e)
            AdResult.Failure(AdException.from(e))
        }
    }

    /**
     * 展示广告
     *
     * 流程：
     * 1. 发布触发展示事件
     * 2. 尝试从缓存获取广告
     * 3. 如果缓存为空，立即加载
     * 4. 检查 Activity 是否正在销毁或已销毁
     *    - 如果是，直接返回失败（不会恢复）
     * 5. 检查 Activity 是否处于可交互状态（RESUMED）
     *    - 如果不可交互但未销毁，挂起协程等待 Activity 恢复
     *    - 等待 onResume（继续展示）或 onDestroy（返回失败）
     *    - 无超时限制，完全依赖 Activity 生命周期
     * 6. 调用 showInternal() 展示
     * 7. 展示成功后触发预加载下一个广告
     *
     * @return 展示结果，可能需要等待 Activity 恢复后才返回
     */
    override suspend fun show(
        activity: Activity,
        adUnitId: String,
        onEvent: ((AdEvent) -> Unit)?
    ): AdResult<AdShowData> {
        return try {
            // 🔧 FIX: 防止并发展示
            if (!isShowingAd.compareAndSet(false, true)) {
                AdLogger.w("[$adType] Another ad is showing, skip")
                return AdResult.Failure(AdException.showFailed("Another ad is showing"))
            }

            // 1. 发布触发展示事件
            eventBus.post(AdEvent.ShowTrigger(adType, adUnitId))

            // 2. 尝试从缓存获取
            var ad = cache.get(adUnitId)
            val wasFromCache = (ad != null)  // 记录广告是否来自缓存

            // 3. 缓存为空则立即加载
            if (ad == null) {
                AdLogger.d("[$adType] No cached ad for $adUnitId, loading...")

                val timeout = loadTimeoutMillis?.takeIf { it > 0 }
                val start = System.currentTimeMillis()
                val loadDeferred = controllerScope.async {
                    loader.load(adUnitId)
                }

                /**
                 * 处理业务层的超时逻辑
                 */
                val loadResult = loadDeferred.awaitWithBusinessTimeout(timeout) { deferred ->
                    controllerScope.launch {
                        runCatching { deferred.await() }
                            .onSuccess { result ->
                                when (result) {
                                    is AdResult.Success -> {
                                        if (cache.size(adUnitId) < maxCacheSize) {
                                            cache.cache(adUnitId, result.data)
                                            AdLogger.d(
                                                "[$adType] Cached ad for $adUnitId after timeout completion, load duration: ${System.currentTimeMillis() - start}ms"
                                            )
                                        }
                                    }

                                    is AdResult.Failure -> {
                                        AdLogger.e(
                                            "[$adType] Deferred load failed after timeout for $adUnitId: ${result.error.message}"
                                        )
                                    }

                                    else -> {
                                        AdLogger.w("[$adType] Unexpected load result after timeout for $adUnitId")
                                    }
                                }
                            }
                            .onFailure { error ->
                                AdLogger.e(
                                    "[$adType] Failed to obtain deferred load result after timeout for $adUnitId",
                                    error
                                )
                            }
                    }
                }

                if (loadResult == null) {
                    val effectiveTimeout = timeout ?: loadTimeoutMillis ?: -1L
                    val message = "Load timeout after ${effectiveTimeout}ms"
                    AdLogger.e("[$adType] $message for $adUnitId")
                    eventBus.post(AdEvent.ShowFailure(
                        adType,
                        adUnitId,
                        message
                    ))
                    isShowingAd.set(false)
                    return AdResult.Failure(AdException.timeout("Load ad for $adUnitId"))
                }

                /**
                 * 业务层的超时逻辑结束，没有超时
                 */
                when (loadResult) {
                    is AdResult.Success -> {
                        ad = loadResult.data
                        AdLogger.d("[$adType] Ad loaded successfully for immediate show, duration: ${System.currentTimeMillis() - start}ms")
                    }

                    is AdResult.Failure -> {
                        AdLogger.e("[$adType] Failed to load ad for show: ${loadResult.error.message}")
                        eventBus.post(AdEvent.ShowFailure(
                            adType,
                            adUnitId,
                            "Load failed: ${loadResult.error.message}"
                        ))
                        isShowingAd.set(false)
                        return loadResult
                    }

                    else -> {
                        // ✅ 实际错误：意外的加载结果类型
                        publishEvent(
                            AdEvent.ShowFailure(adType, adUnitId, "Unexpected load result"),
                            onEvent
                        )
                        isShowingAd.set(false)
                        return AdResult.Failure(AdException.noAd(adUnitId))
                    }
                }
            }

            // 4. 检查 Activity 是否正在销毁或已销毁（明确不会恢复的情况）
            if (activity.isFinishing || activity.isDestroyed) {
                AdLogger.w("[$adType] Activity is finishing or destroyed, cannot show ad")

                // 如果广告是新加载的，放回缓存
                if (!wasFromCache && ad != null) {
                    cache.cache(adUnitId, ad)
                    AdLogger.d("[$adType] Ad returned to cache")
                }

                // ✅ 生命周期拦截：不发布事件，不通知业务层
                // 这是正常流程，业务方通过返回的 AdResult.Failure 自行判断即可

                // 重置展示标志
                isShowingAd.set(false)

                return AdResult.Failure(AdException.showFailed("Activity is finishing or destroyed"))
            }

            // 5. 检查 Activity 是否可交互（生命周期检查）
            if (activity is LifecycleOwner) {
                val isResumed = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

                if (!isResumed) {
                    // Activity 暂时不可交互但未销毁，挂起协程等待恢复
                    AdLogger.d("[$adType] Activity not in RESUMED state (current: ${activity.lifecycle.currentState}), waiting for resume...")

                    // 挂起协程，等待 Activity 恢复或销毁
                    return suspendCancellableCoroutine { continuation ->
                        // 创建 LifecycleObserver
                        val observer = ResumeLifecycleObserver(activity, activity)

                        // 保存待恢复请求
                        pendingRequest = PendingShowRequest(
                            ad = ad!!,
                            adUnitId = adUnitId,
                            onEvent = onEvent,
                            continuation = continuation,
                            wasFromCache = wasFromCache
                        )

                        // 注册生命周期观察者
                        activity.lifecycle.addObserver(observer)

                        // 协程取消时清理资源
                        continuation.invokeOnCancellation {
                            pendingRequest = null
                            activity.lifecycle.removeObserver(observer)
                            isShowingAd.set(false)

                            AdLogger.d("[$adType] Pending show cancelled for $adUnitId")
                        }
                    }
                }
            }

            // 6. 展示广告（子类实现）
            // ad 在此处不可能为 null，因为上面的逻辑已经保证了
            val showResult = withContext(Dispatchers.Main.immediate) {
                showInternal(activity, checkNotNull(ad) { "Ad should not be null at this point" }, adUnitId, onEvent)
            }

            // 🔧 FIX: 重置展示标志
            isShowingAd.set(false)

            showResult

        } catch (e: Exception) {
            // 🔧 FIX: 异常时重置展示标志
            isShowingAd.set(false)

            AdLogger.e("[$adType] Show exception for $adUnitId", e)
            val error = AdException.from(e)
            eventBus.post(AdEvent.ShowFailure(adType, adUnitId, error.message))
            AdResult.Failure(error)
        }
    }

    /**
     * 平台特定的展示逻辑
     * 子类必须实现此方法，处理：
     * - 设置广告事件回调
     * - 调用平台 SDK 的 show 方法
     * - 处理展示过程中的事件（impression, click, close 等）
     * - 发布对应的事件到 eventBus
     * - 更新 metrics 统计
     *
     * @param activity Activity 上下文
     * @param ad 平台广告对象
     * @param adUnitId 广告位 ID
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果，包含奖励信息（如果是激励广告）
     */
    protected abstract suspend fun showInternal(
        activity: Activity,
        ad: T,
        adUnitId: String,
        onEvent: ((AdEvent) -> Unit)?
    ): AdResult<AdShowData>

    /**
     * 触发预加载下一个广告
     *
     * 应该在广告真正曝光时调用（onAdImpression 回调中），而不是等待广告关闭。
     * 这样可以利用用户观看广告的时间进行预加载，提升下次展示的响应速度。
     *
     * 内部会检查缓存是否已满，避免重复预加载。
     *
     * @param adUnitId 广告位 ID
     */
    protected fun triggerPreloadIfNeeded(adUnitId: String) {
        if (cache.size(adUnitId) < maxCacheSize) {
            // ✅ 获取或创建该广告位的预加载锁
            val lock = preloadLocks.getOrPut(adUnitId) { AtomicBoolean(false) }

            // ✅ 原子检查并设置预加载标志（CAS 操作）
            if (lock.compareAndSet(false, true)) {
                AdLogger.d("[$adType] Triggering preload for next ad after impression")
                controllerScope.launch {
                    try {
                        preload(adUnitId)
                    } catch (e: Exception) {
                        AdLogger.e("[$adType] Preload after impression failed", e)
                    } finally {
                        // ✅ 无论成功或失败，都释放锁
                        lock.set(false)
                    }
                }
            } else {
                AdLogger.d("[$adType] Preload already in progress for $adUnitId, skipping")
            }
        }
    }

    /**
     * 发布广告事件
     *
     * 事件发布顺序：
     * 1. 优先调用局部回调（onEvent）- 页面级监听
     * 2. 再发布到全局 EventBus - 全局监控
     *
     * @param event 要发布的事件
     * @param localCallback 局部事件回调（来自 show() 方法的 onEvent 参数）
     */
    protected fun publishEvent(
        event: AdEvent,
        localCallback: ((AdEvent) -> Unit)?
    ) {
        // 1. 优先调用局部回调（如果提供）
        localCallback?.invoke(event)

        // 2. 发布到全局 EventBus
        eventBus.post(event)
    }

    /**
     * 检查是否有可用的广告
     */
    override fun isAvailable(adUnitId: String): Boolean {
        return cache.size(adUnitId) > 0
    }

    /**
     * 获取缓存大小
     */
    override fun getCacheSize(adUnitId: String): Int {
        return cache.size(adUnitId)
    }

    /**
     * 清空指定广告位的缓存
     */
    override fun clearCache(adUnitId: String) {
        cache.clear(adUnitId)
        AdLogger.d("[$adType] Cache cleared for $adUnitId")
    }

    /**
     * 清空所有缓存
     */
    override fun clearAllCache() {
        cache.clearAll()
        AdLogger.d("[$adType] All cache cleared")
    }

    /**
     * 🔧 FIX: 清理资源，取消所有协程
     * 应该在 Controller 不再使用时调用
     */
    fun cleanup() {
        controllerScope.cancel()
        AdLogger.d("[$adType] Controller cleaned up")
    }

    /**
     * Activity 生命周期观察者
     * 监听 Activity 的 onResume 和 onDestroy 事件
     * - onResume: 继续展示待展示的广告
     * - onDestroy: 清理待展示状态，返回失败结果
     */
    private inner class ResumeLifecycleObserver(
        private val activity: Activity,
        private val lifecycleOwner: LifecycleOwner
    ) : DefaultLifecycleObserver {

        /**
         * Activity 恢复到可交互状态，继续展示广告
         */
        override fun onResume(owner: LifecycleOwner) {
            val pending = pendingRequest ?: return

            // 清理状态
            pendingRequest = null
            lifecycleOwner.lifecycle.removeObserver(this)

            AdLogger.d("[$adType] Activity resumed, continue showing ad for ${pending.adUnitId}")

            // 在协程中继续展示
            controllerScope.launch {
                val result = try {
                    // 再次检查 Activity 状态（防止竞态条件）
                    when {
                        activity.isFinishing || activity.isDestroyed -> {
                            AdLogger.w("[$adType] Activity is finishing/destroyed after resume event")
                            AdResult.Failure(AdException.showFailed("Activity destroyed"))
                        }

                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                            // 继续展示广告
                            withContext(Dispatchers.Main.immediate) {
                                showInternal(activity, pending.ad, pending.adUnitId, pending.onEvent)
                            }
                        }

                        else -> {
                            AdLogger.w("[$adType] Activity not in RESUMED state after resume event")
                            AdResult.Failure(AdException.showFailed("Activity not resumed"))
                        }
                    }
                } catch (e: Exception) {
                    AdLogger.e("[$adType] Failed to show ad after resume", e)
                    // ✅ 实际错误：发布事件通知业务层
                    publishEvent(
                        AdEvent.ShowFailure(adType, pending.adUnitId, "Exception after resume: ${e.message}"),
                        pending.onEvent
                    )
                    AdResult.Failure(AdException.from(e))
                } finally {
                    // 重置展示标志
                    isShowingAd.set(false)
                }

                // 恢复挂起的协程，返回最终结果
                if (pending.continuation.isActive) {
                    pending.continuation.resume(result)
                }
            }
        }

        /**
         * Activity 销毁，取消待展示的广告
         */
        override fun onDestroy(owner: LifecycleOwner) {
            val pending = pendingRequest ?: return

            // 清理状态
            pendingRequest = null
            lifecycleOwner.lifecycle.removeObserver(this)

            AdLogger.d("[$adType] Activity destroyed, cancel pending show for ${pending.adUnitId}")

            // 如果广告是新加载的，放回缓存
            if (!pending.wasFromCache) {
                cache.cache(pending.adUnitId, pending.ad)
                AdLogger.d("[$adType] Newly loaded ad returned to cache")
            }

            // ✅ 生命周期拦截：不发布事件，不通知业务层
            // Activity 销毁是正常生命周期，业务方通过返回的 AdResult.Failure 自行判断

            // 恢复协程并返回失败结果
            if (pending.continuation.isActive) {
                pending.continuation.resume(
                    AdResult.Failure(AdException.showFailed("Activity destroyed"))
                )
            }

            // 重置展示标志
            isShowingAd.set(false)
        }
    }
}
