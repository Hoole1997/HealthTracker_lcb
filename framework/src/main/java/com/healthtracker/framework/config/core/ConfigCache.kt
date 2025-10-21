package com.healthtracker.framework.config.core

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 配置缓存管理
 *
 * 提供线程安全的内存缓存机制，避免重复解析配置
 * 采用 ConcurrentHashMap 实现，支持高并发访问
 *
 * 使用示例:
 * ```kotlin
 * val cache = ConfigCache()
 *
 * // 缓存配置
 * cache.put(PushConfig::class, pushConfig)
 *
 * // 获取缓存
 * val cached = cache.get(PushConfig::class)
 *
 * // 清除缓存
 * cache.remove(PushConfig::class)
 * ```
 */
class ConfigCache {

    // 内存缓存存储（线程安全）
    private val cache = ConcurrentHashMap<KClass<*>, Any>()

    /**
     * 缓存配置
     *
     * @param configClass 配置类型
     * @param config 配置对象
     */
    fun <T : Any> put(configClass: KClass<T>, config: T) {
        cache[configClass] = config
    }

    /**
     * 获取缓存配置
     *
     * @param configClass 配置类型
     * @return 缓存的配置对象，不存在返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(configClass: KClass<T>): T? {
        return cache[configClass] as? T
    }

    /**
     * 移除指定配置缓存
     *
     * @param configClass 配置类型
     * @return 被移除的配置对象，不存在返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> remove(configClass: KClass<T>): T? {
        return cache.remove(configClass) as? T
    }

    /**
     * 清除所有缓存
     */
    fun clearAll() {
        cache.clear()
    }

    /**
     * 检查是否有缓存
     *
     * @param configClass 配置类型
     * @return true 存在缓存，false 不存在
     */
    fun <T : Any> has(configClass: KClass<T>): Boolean {
        return cache.containsKey(configClass)
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存的配置数量
     */
    fun size(): Int {
        return cache.size
    }

    /**
     * 获取所有缓存的配置类型
     *
     * @return 配置类型集合
     */
    fun getCachedTypes(): Set<KClass<*>> {
        return cache.keys.toSet()
    }
}
