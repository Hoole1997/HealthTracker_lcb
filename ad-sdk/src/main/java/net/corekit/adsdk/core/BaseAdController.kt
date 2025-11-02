package net.corekit.adsdk.core

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.corekit.adsdk.event.AdEvent
import net.corekit.adsdk.event.AdEventBus
import net.corekit.adsdk.metric.AdMetrics
import net.corekit.adsdk.service.AdCache
import net.corekit.adsdk.service.AdLoader
import net.corekit.adsdk.util.AdLogger
import java.util.concurrent.atomic.AtomicBoolean

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
 */
internal abstract class BaseAdController<T>(
    override val adType: AdType,
    protected val loader: AdLoader<T>,
    protected val cache: AdCache<T>,
    protected val eventBus: AdEventBus,
    protected val metrics: AdMetrics,
    private val maxCacheSize: Int = 2
) : AdController<T> {

    // 🔧 FIX: 管理的协程作用域，防止内存泄漏
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 🔧 FIX: 防止并发 show() 调用的竞态条件
    private val isShowingAd = AtomicBoolean(false)

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
     * 4. 调用 showInternal() 展示
     * 5. 展示成功后触发预加载下一个广告
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

            // 3. 缓存为空则立即加载
            if (ad == null) {
                AdLogger.d("[$adType] No cached ad for $adUnitId, loading...")

                when (val loadResult = loader.load(adUnitId)) {
                    is AdResult.Success -> {
                        ad = loadResult.data
                        AdLogger.d("[$adType] Ad loaded successfully for immediate show")
                    }

                    is AdResult.Failure -> {
                        AdLogger.e("[$adType] Failed to load ad for show: ${loadResult.error.message}")
                        eventBus.post(AdEvent.ShowFailure(
                            adType,
                            adUnitId,
                            "Load failed: ${loadResult.error.message}"
                        ))
                        return loadResult
                    }

                    else -> {
                        return AdResult.Failure(AdException.noAd(adUnitId))
                    }
                }
            }

            // 4. 展示广告（子类实现）
            // ad 在此处不可能为 null，因为上面的逻辑已经保证了
            val showResult = showInternal(activity, ad!!, adUnitId, onEvent)

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
            AdLogger.d("[$adType] Triggering preload for next ad after impression")
            controllerScope.launch {
                try {
                    preload(adUnitId)
                } catch (e: Exception) {
                    AdLogger.e("[$adType] Preload after impression failed", e)
                }
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
}
