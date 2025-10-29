package com.healthtracker.framework.config.core

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

/**
 * 远程配置管理器（框架核心）
 *
 * 提供统一的配置获取、刷新、监听接口
 * 支持泛型 API，类型安全，易于使用
 *
 * 使用示例:
 * ```kotlin
 * // 获取配置
 * val pushConfig = configManager.getConfig<PushConfig>()
 *
 * // 观察配置变化
 * configManager.observeConfig<PushConfig>().collect { config ->
 *     // 处理配置更新
 * }
 *
 * // 刷新配置
 * configManager.refreshConfig()
 * ```
 */
@Singleton
class RemoteConfigManager @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig,
    private val registry: ConfigRegistry,
    private val cache: ConfigCache
) {
    companion object {
        private const val TAG = "RemoteConfigManager"
        private const val DEFAULT_FETCH_INTERVAL_SECONDS = 3600L  // 1小时
    }

    // 配置变化通知
    private val _configUpdates = MutableStateFlow<ConfigUpdateEvent>(ConfigUpdateEvent.Idle)
    val configUpdates: StateFlow<ConfigUpdateEvent> = _configUpdates.asStateFlow()

    // 配置观察者（每种配置类型一个）
    private val configObservers = mutableMapOf<KClass<*>, MutableStateFlow<Any>>()

    /**
     * 初始化配置管理器
     *
     * 应在 Application.onCreate() 中调用
     *
     * @return 初始化结果
     */
    suspend fun initialize(): Result<Unit> {
        return try {
            "Initializing RemoteConfigManager...".logd(TAG)

            val interval = if(BuildState.debug) 5 * 60 else DEFAULT_FETCH_INTERVAL_SECONDS
            // 设置 Remote Config 参数
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(interval)
                .build()

            remoteConfig.setConfigSettingsAsync(configSettings).await()

            // 拉取并激活配置
            val activated = remoteConfig.fetchAndActivate().await()

            if (activated) {
                "New config fetched and activated successfully".logd(TAG)
                try {
                    if(BuildState.debug){
                        for ((k, v) in remoteConfig.all) {
                            "$k:${v?.asString()}".logd(TAG)
                        }
                    }
                } catch (_: Throwable) {
                }
            } else {
                "Config is already up to date".logd(TAG)
            }

            "Registered ${registry.getRegisteredCount()} config parsers".logd(TAG)

            Result.success(Unit)
        } catch (e: Exception) {
            "Failed to fetch config during initialization: ${e.message}".loge(TAG)
            "Will continue with default/cached config".logd(TAG)
            // 不返回 failure，避免阻塞应用启动
            Result.success(Unit)
        }
    }

    /**
     * 获取配置（泛型方法）
     *
     * @param T 配置类型
     * @param forceRefresh 是否强制重新解析（忽略缓存）
     * @return 配置对象，解析失败返回默认配置
     *
     * @throws IllegalStateException 如果配置类型未注册
     */
    inline fun <reified T : Any> getConfig(forceRefresh: Boolean = false): T {
        return getConfigInternal(T::class, forceRefresh)
    }

    /**
     * 获取配置（KClass 版本）
     *
     * @param configClass 配置类型
     * @param forceRefresh 是否强制重新解析
     * @return 配置对象
     */
    fun <T : Any> getConfigInternal(configClass: KClass<T>, forceRefresh: Boolean = false): T {
        // 1. 如果不强制刷新，先从缓存获取
        if (!forceRefresh) {
            cache.get(configClass)?.let {
                "Config loaded from cache: ${configClass.simpleName}".logd(TAG)
                return it
            }
        }

        // 2. 获取解析器
        val parser = registry.getParser(configClass)
            ?: throw IllegalStateException(
                "No parser registered for ${configClass.simpleName}. " +
                        "Please register it in AppConfigRegistry."
            )

        // 3. 从 Remote Config 获取原始值
        val rawValue = remoteConfig.getString(parser.configKey)

        // 4. 解析配置
        val config = try {
            if (rawValue.isNotEmpty()) {
                parser.parse(rawValue)?.also { parsed ->
                    // 验证配置
                    if (!parser.validate(parsed)) {
                        "Config validation failed for ${configClass.simpleName}, using default".logd(TAG)
                        null
                    } else {
                        "Config parsed successfully: ${configClass.simpleName}".logd(TAG)
                        parsed
                    }
                }
            } else {
                "Empty config value for ${configClass.simpleName}, using default".logd(TAG)
                null
            }
        } catch (e: Exception) {
            "Failed to parse config for ${configClass.simpleName}, using default: ${e.message}".loge(TAG)
            null
        } ?: parser.getDefault()

        // 5. 缓存配置
        cache.put(configClass, config)

        // 6. 更新观察者
        updateObserver(configClass, config)

        return config
    }

    /**
     * 观察配置变化（Flow）
     *
     * @param T 配置类型
     * @return 配置状态流
     */
    inline fun <reified T : Any> observeConfig(): StateFlow<T> {
        return observeConfigInternal(T::class)
    }

    /**
     * 观察配置变化（内部方法）
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> observeConfigInternal(configClass: KClass<T>): StateFlow<T> {
        // 获取或创建观察者
        val observer = configObservers.getOrPut(configClass) {
            MutableStateFlow(getConfigInternal(configClass))
        } as MutableStateFlow<T>

        return observer.asStateFlow()
    }

    /**
     * 刷新配置（从服务器获取最新配置）
     *
     * @return 刷新结果
     */
    suspend fun refreshConfig(): Result<Unit> {
        return try {
            "Refreshing config from server...".logd(TAG)
            _configUpdates.value = ConfigUpdateEvent.Refreshing

            // 从服务器获取并激活配置
            val activated = remoteConfig.fetchAndActivate().await()

            if (activated) {
                "Config refreshed successfully, clearing cache".logd(TAG)

                // 清除缓存
                cache.clearAll()

                // 更新所有观察者
                refreshAllObservers()

                _configUpdates.value = ConfigUpdateEvent.Success
                Result.success(Unit)
            } else {
                "Config is already up to date".logd(TAG)
                _configUpdates.value = ConfigUpdateEvent.NoChange
                Result.success(Unit)
            }
        } catch (e: Exception) {
            "Failed to refresh config: ${e.message}".loge(TAG)
            _configUpdates.value = ConfigUpdateEvent.Error(e)
            Result.failure(e)
        }
    }

    /**
     * 清除指定配置缓存
     *
     * @param T 配置类型
     */
    inline fun <reified T : Any> clearCache() {
        clearCacheInternal(T::class)
    }

    /**
     * 清除指定配置缓存（内部方法）
     */
    fun <T : Any> clearCacheInternal(configClass: KClass<T>) {
        cache.remove(configClass)
        "Cache cleared for ${configClass.simpleName}".logd(TAG)
    }

    /**
     * 清除所有配置缓存
     */
    fun clearAllCache() {
        cache.clearAll()
        "All config cache cleared".logd(TAG)
    }

    /**
     * 更新观察者
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> updateObserver(configClass: KClass<T>, config: T) {
        (configObservers[configClass] as? MutableStateFlow<T>)?.value = config
    }

    /**
     * 刷新所有观察者
     */
    private fun refreshAllObservers() {
        configObservers.keys.forEach { configClass ->
            try {
                @Suppress("UNCHECKED_CAST")
                val config = getConfigInternal(configClass as KClass<Any>, forceRefresh = true)
                updateObserver(configClass, config)
            } catch (e: Exception) {
                "Failed to refresh observer for ${configClass.simpleName}: ${e.message}".loge(TAG)
            }
        }
    }

    /**
     * 配置更新事件
     */
    sealed class ConfigUpdateEvent {
        /** 空闲状态 */
        object Idle : ConfigUpdateEvent()

        /** 正在刷新 */
        object Refreshing : ConfigUpdateEvent()

        /** 刷新成功 */
        object Success : ConfigUpdateEvent()

        /** 配置未变化 */
        object NoChange : ConfigUpdateEvent()

        /** 刷新失败 */
        data class Error(val exception: Exception) : ConfigUpdateEvent()
    }
}
