package com.healthtracker.framework.config.core

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * 配置注册中心
 *
 * 管理所有配置解析器的注册和查找
 * 采用线程安全的 ConcurrentHashMap 实现
 *
 * 使用示例:
 * ```kotlin
 * val registry = ConfigRegistry()
 *
 * // 注册配置解析器
 * registry.register(PushConfig::class, PushConfigParser(gson))
 * registry.register(AdConfig::class, AdConfigParser(gson))
 *
 * // 获取解析器
 * val parser = registry.getParser(PushConfig::class)
 * ```
 */
class ConfigRegistry {

    // 存储配置类型 -> 解析器的映射（线程安全）
    private val parsers = ConcurrentHashMap<KClass<*>, ConfigParser<*>>()

    /**
     * 注册配置解析器
     *
     * @param configClass 配置数据类的 KClass
     * @param parser 对应的解析器实例
     * @throws IllegalStateException 如果配置类型已注册
     */
    fun <T : Any> register(
        configClass: KClass<T>,
        parser: ConfigParser<T>
    ) {
        val existing = parsers.putIfAbsent(configClass, parser)
        if (existing != null) {
            throw IllegalStateException(
                "Parser for ${configClass.simpleName} is already registered"
            )
        }
    }

    /**
     * 获取配置解析器
     *
     * @param configClass 配置数据类的 KClass
     * @return 对应的解析器，未注册返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getParser(configClass: KClass<T>): ConfigParser<T>? {
        return parsers[configClass] as? ConfigParser<T>
    }

    /**
     * 检查是否已注册
     *
     * @param configClass 配置数据类的 KClass
     * @return true 已注册，false 未注册
     */
    fun <T : Any> isRegistered(configClass: KClass<T>): Boolean {
        return parsers.containsKey(configClass)
    }

    /**
     * 获取所有已注册的配置键
     *
     * @return 配置键集合
     */
    fun getAllConfigKeys(): Set<String> {
        return parsers.values.map { it.configKey }.toSet()
    }

    /**
     * 获取已注册的配置数量
     *
     * @return 配置数量
     */
    fun getRegisteredCount(): Int {
        return parsers.size
    }

    /**
     * 清除所有注册（仅用于测试）
     */
    internal fun clearAll() {
        parsers.clear()
    }
}
