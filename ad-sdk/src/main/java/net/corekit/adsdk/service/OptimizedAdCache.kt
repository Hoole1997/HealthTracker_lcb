package net.corekit.adsdk.service

import kotlinx.coroutines.*
import net.corekit.adsdk.util.AdLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue

/**
 * 优化的广告缓存实现
 * 特性：
 * - 使用 ConcurrentHashMap + LinkedBlockingQueue 实现 O(1) 查找
 * - 自动清理过期广告（协程后台任务）
 * - 线程安全
 * - 支持配置最大缓存数量和过期时间
 *
 * @param T 平台广告对象类型
 * @param maxSizePerUnit 每个广告位的最大缓存数量
 * @param expiryDurationMs 广告过期时间（毫秒）
 */
internal class OptimizedAdCache<T>(
    private val maxSizePerUnit: Int = 2,
    private val expiryDurationMs: Long = 3600_000L // 默认 1 小时
) : AdCache<T> {

    /**
     * 缓存的广告数据类
     *
     * @param ad 广告对象
     * @param loadTime 加载时间戳
     */
    private data class CachedAd<T>(
        val ad: T,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        /**
         * 检查广告是否已过期
         *
         * @param expiryDuration 过期时长（毫秒）
         */
        fun isExpired(expiryDuration: Long): Boolean {
            return System.currentTimeMillis() - loadTime > expiryDuration
        }
    }

    // 主缓存存储：AdUnitId -> Queue<CachedAd>
    // 使用 ConcurrentHashMap 保证线程安全
    // 使用 LinkedBlockingQueue 保证 FIFO 和并发安全
    private val cacheMap = ConcurrentHashMap<String, LinkedBlockingQueue<CachedAd<T>>>()

    // 后台清理任务
    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var cleanupJob: Job? = null

    init {
        // 启动定期清理任务（每 5 分钟）
        startCleanupTask()
    }

    /**
     * 启动后台清理任务
     */
    private fun startCleanupTask() {
        cleanupJob = cleanupScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000L) // 5 分钟
                try {
                    cleanup()
                } catch (e: Exception) {
                    AdLogger.e("Cache cleanup error", e)
                }
            }
        }
    }

    /**
     * 缓存广告
     */
    override fun cache(adUnitId: String, ad: T) {
        // 获取或创建队列
        val queue = cacheMap.getOrPut(adUnitId) {
            LinkedBlockingQueue(maxSizePerUnit)
        }

        // 检查队列是否已满
        if (queue.size >= maxSizePerUnit) {
            // 移除最旧的广告（FIFO）
            queue.poll()
            AdLogger.d("Cache full for $adUnitId, removed oldest ad")
        }

        // 添加新广告
        val cached = CachedAd(ad)
        queue.offer(cached)

        AdLogger.d("Ad cached for $adUnitId, cache size: ${queue.size}/$maxSizePerUnit")
    }

    /**
     * 获取缓存的广告
     * 自动跳过已过期的广告
     */
    override suspend fun get(adUnitId: String): T? {
        val queue = cacheMap[adUnitId] ?: return null

        // 遍历队列查找第一个未过期的广告
        while (queue.isNotEmpty()) {
            val cached = queue.poll() ?: break

            if (!cached.isExpired(expiryDurationMs)) {
                // 找到未过期的广告
                AdLogger.d("Ad retrieved from cache for $adUnitId, remaining: ${queue.size}")
                return cached.ad
            } else {
                // 已过期，继续查找下一个
                AdLogger.d("Expired ad removed from cache for $adUnitId")
            }
        }

        // 没有可用广告
        AdLogger.d("No available ad in cache for $adUnitId")
        return null
    }

    /**
     * 🔧 FIX: 获取指定广告位的有效缓存数量（排除过期的）
     */
    override fun size(adUnitId: String): Int {
        val queue = cacheMap[adUnitId] ?: return 0
        // 统计未过期的广告数量
        return queue.count { !it.isExpired(expiryDurationMs) }
    }

    /**
     * 清空指定广告位的缓存
     */
    override fun clear(adUnitId: String) {
        cacheMap[adUnitId]?.clear()
        AdLogger.d("Cache cleared for $adUnitId")
    }

    /**
     * 清空所有缓存
     */
    override fun clearAll() {
        cacheMap.clear()
        AdLogger.d("All cache cleared")
    }

    /**
     * 清理所有过期的广告
     */
    override suspend fun cleanup() {
        var totalRemoved = 0

        // 🔧 FIX: 创建副本避免在迭代中修改 map
        val entries = cacheMap.entries.toList()

        entries.forEach { (adUnitId, queue) ->
            val originalSize = queue.size

            // 移除过期广告
            queue.removeIf { it.isExpired(expiryDurationMs) }

            val removedCount = originalSize - queue.size
            if (removedCount > 0) {
                totalRemoved += removedCount
                AdLogger.d("Cleaned up $removedCount expired ads for $adUnitId")
            }

            // 如果队列为空，移除该键
            if (queue.isEmpty()) {
                cacheMap.remove(adUnitId)
            }
        }

        if (totalRemoved > 0) {
            AdLogger.d("Total cleaned up: $totalRemoved expired ads")
        }
    }

    /**
     * 🔧 FIX: 实现 AutoCloseable.close()
     * 停止清理任务并释放所有资源
     */
    override fun close() {
        cleanupJob?.cancel()
        cleanupScope.cancel()
        clearAll()
        AdLogger.d("AdCache closed and all resources released")
    }

    /**
     * 获取缓存统计信息（调试用）
     */
    fun getStats(): Map<String, Int> {
        return cacheMap.mapValues { it.value.size }
    }
}
