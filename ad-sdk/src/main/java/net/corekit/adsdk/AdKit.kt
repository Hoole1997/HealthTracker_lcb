package net.corekit.adsdk

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.corekit.adsdk.config.AdConfigManager
import net.corekit.adsdk.config.AdUnitConfig
import net.corekit.adsdk.controller.AppOpenAdController
import net.corekit.adsdk.core.AdController
import net.corekit.adsdk.core.AdException
import net.corekit.adsdk.core.AdResult
import net.corekit.adsdk.core.AdShowData
import net.corekit.adsdk.core.AdType
import net.corekit.adsdk.event.AdEvent
import net.corekit.adsdk.event.AdEventBus
import net.corekit.adsdk.event.AdEventListener
import net.corekit.adsdk.loader.AppOpenAdLoader
import net.corekit.adsdk.metric.AdMetrics
import net.corekit.adsdk.metric.DefaultAdMetrics
import net.corekit.adsdk.service.OptimizedAdCache
import net.corekit.adsdk.util.AdLogger
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * AdKit - 广告 SDK 统一入口
 * 提供简洁的 API 用于广告管理
 *
 * 功能：
 * - SDK 初始化
 * - 广告加载和展示
 * - 事件监听
 * - 配置管理
 * - 指标统计
 */
object AdKit {

    // ========== 核心组件 ==========

    private lateinit var eventBus: AdEventBus
    private lateinit var configManager: AdConfigManager
    private lateinit var metrics: AdMetrics

    // 控制器存储
    private val controllers = ConcurrentHashMap<AdType, AdController<*>>()
    private val initDeferred = CompletableDeferred<Boolean>()
    // 🔧 FIX: 初始化互斥锁，防止并发初始化
    private val initializationMutex = Mutex()

    // 初始化状态
    @Volatile
    private var isInitialized = false

    // AdMob Application ID
    private var admobAppId: String = ""

    // ========== 初始化 ==========

    /**
     * 初始化广告 SDK
     *
     * @param context 上下文
     * @param admobAppId AdMob Application ID
     * @param configs 广告位配置列表
     * @return 初始化结果
     */
    suspend fun initialize(
        context: Context,
        admobAppId: String,
        configs: List<AdUnitConfig> = emptyList()
    ): AdResult<Unit> = initializationMutex.withLock {
        // 🔧 FIX: 使用 Mutex 确保线程安全的初始化
        if (isInitialized) {
            AdLogger.w("AdSdk already initialized")
            return AdResult.Success(Unit)
        }

        return try {
            // 保存 Application ID
            this.admobAppId = admobAppId

            // 初始化组件
            eventBus = AdEventBus()
            configManager = AdConfigManager()

            // ✨ 初始化 MMKV 并创建持久化 metrics
            MMKV.initialize(context.applicationContext)
            val metricsMMKV = MMKV.mmkvWithID("ad_sdk_metrics", MMKV.MULTI_PROCESS_MODE)
            metrics = DefaultAdMetrics(metricsMMKV)

            // 设置配置
            if (configs.isNotEmpty()) {
                configManager.setConfigs(configs)
            }

            // 初始化 AdMob SDK
            val initResult = initializeAdMob(context, admobAppId)

            if (initResult.isSuccess) {
                // 初始化控制器
                initializeControllers()
                isInitialized = true
                initDeferred.complete(isInitialized)

                AdLogger.i("AdSdk initialized successfully")
                AdResult.Success(Unit)
            } else {
                initResult
            }
        } catch (e: Exception) {
            AdLogger.e("AdSdk initialization failed", e)
            AdResult.Failure(AdException.from(e))
        }
    }

    private suspend fun awaitInitialized() {
        initDeferred.await()  // 阻塞调用方直到初始化完成
    }

    /**
     * 初始化 AdMob SDK
     */
    private suspend fun initializeAdMob(
        context: Context,
        admobAppId: String
    ): AdResult<Unit> = suspendCancellableCoroutine { continuation ->
        val initConfig = InitializationConfig.Builder(admobAppId).build()

        MobileAds.initialize(context, initConfig) { initStatus ->
            val adapterStatuses = initStatus.adapterStatusMap
            AdLogger.d("AdMob SDK initialized with ${adapterStatuses.size} adapters")

            for ((adapterName, status) in adapterStatuses) {
                AdLogger.d("Adapter: $adapterName, State: ${status.initializationState}, Description: ${status.description}")
            }

            if (continuation.isActive) {
                continuation.resume(AdResult.Success(Unit))
            }
        }
    }

    /**
     * 初始化广告控制器
     */
    private fun initializeControllers() {
        // 开屏广告控制器
        val appOpenConfig = configManager.getConfig(AdType.APP_OPEN)
        if (appOpenConfig.adUnitId.isNotEmpty()) {
            controllers[AdType.APP_OPEN] = AppOpenAdController(
                loader = AppOpenAdLoader(),
                cache = OptimizedAdCache(
                    maxSizePerUnit = appOpenConfig.maxCacheSize,
                    expiryDurationMs = appOpenConfig.expiryDurationMs
                ),
                eventBus = eventBus,
                metrics = metrics,
                maxCacheSize = appOpenConfig.maxCacheSize
            )
            AdLogger.d("AppOpenAdController initialized")
        }

        // TODO: 初始化其他广告类型的控制器
        // - InterstitialAdController
        // - BannerAdController
        // - NativeAdController
        // - RewardedAdController
    }

    // ========== 公共 API ==========

    /**
     * 展示广告
     * 简化的 API，使用配置中的广告位 ID
     *
     * @param activity Activity 上下文
     * @param adType 广告类型
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果，包含奖励信息（如果是激励广告）
     */
    suspend fun show(
        activity: Activity,
        adType: AdType,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> {
        AdLogger.d("到达开屏场景")
        awaitInitialized()
        val config = configManager.getConfig(adType)

        if (!config.enabled) {
            AdLogger.w("Ad type $adType is disabled")
            return AdResult.Failure(AdException.configError("Ad type is disabled"))
        }

        if (config.adUnitId.isEmpty()) {
            AdLogger.e("Ad unit ID not configured for $adType")
            return AdResult.Failure(AdException.configError("Ad unit ID not configured"))
        }

        return show(activity, adType, config.adUnitId, onEvent)
    }

    /**
     * 展示广告（指定广告位 ID）
     *
     * @param activity Activity 上下文
     * @param adType 广告类型
     * @param adUnitId 广告位 ID
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果，包含奖励信息（如果是激励广告）
     */
    suspend fun show(
        activity: Activity,
        adType: AdType,
        adUnitId: String,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> {
        awaitInitialized()

        val controller = getController<Any>(adType)
            ?: return AdResult.Failure(AdException.unsupportedAdType(adType))

        AdLogger.d("准备展示开屏广告")
        return controller.show(activity, adUnitId, onEvent)
    }

    // ========== 类型特定的展示方法 ==========

    /**
     * 展示开屏广告
     *
     * @param activity Activity 上下文
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果
     */
    suspend fun showOpen(
        activity: Activity,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> = show(activity, AdType.APP_OPEN, onEvent)

    /**
     * 展示插屏广告
     *
     * @param activity Activity 上下文
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果
     */
    suspend fun showInterstitial(
        activity: Activity,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> = show(activity, AdType.INTERSTITIAL, onEvent)

    /**
     * 展示横幅广告
     *
     * @param activity Activity 上下文
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果
     */
    suspend fun showBanner(
        activity: Activity,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> = show(activity, AdType.BANNER, onEvent)

    /**
     * 展示原生广告
     *
     * @param activity Activity 上下文
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果
     */
    suspend fun showNative(
        activity: Activity,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> = show(activity, AdType.NATIVE, onEvent)

    /**
     * 展示激励视频广告
     *
     * @param activity Activity 上下文
     * @param onEvent 可选的事件回调，仅接收当前展示实例的事件
     * @return 展示结果（包含奖励信息）
     */
    suspend fun showRewarded(
        activity: Activity,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData> = show(activity, AdType.REWARDED, onEvent)

    /**
     * 预加载广告
     *
     * @param adType 广告类型
     * @return 加载结果
     */
    suspend fun preload(adType: AdType): AdResult<Unit> {
        checkInitialized()

        val config = configManager.getConfig(adType)

        if (!config.enabled || config.adUnitId.isEmpty()) {
            return AdResult.Failure(AdException.configError("Ad type not configured"))
        }

        return preload(adType, config.adUnitId)
    }

    /**
     * 预加载广告（指定广告位 ID）
     *
     * @param adType 广告类型
     * @param adUnitId 广告位 ID
     * @return 加载结果
     */
    suspend fun preload(adType: AdType, adUnitId: String): AdResult<Unit> {
        checkInitialized()

        val controller = getController<Any>(adType)
            ?: return AdResult.Failure(AdException.unsupportedAdType(adType))

        return controller.preload(adUnitId)
    }

    // ========== 类型特定的预加载方法 ==========



    /**
     * 检查是否有可用的广告
     *
     * @param adType 广告类型
     * @return true 如果有可用广告
     */
    fun isAvailable(adType: AdType): Boolean {
        if (!isInitialized) return false

        val config = configManager.getConfig(adType)
        if (!config.enabled || config.adUnitId.isEmpty()) {
            return false
        }

        val controller = getController<Any>(adType) ?: return false
        return controller.isAvailable(config.adUnitId)
    }

    // ========== 类型特定的可用性检查方法 ==========

    /**
     * 检查开屏广告是否可用
     */
    fun isOpenAvailable(): Boolean = isAvailable(AdType.APP_OPEN)

    /**
     * 检查插屏广告是否可用
     */
    fun isInterstitialAvailable(): Boolean = isAvailable(AdType.INTERSTITIAL)

    /**
     * 检查横幅广告是否可用
     */
    fun isBannerAvailable(): Boolean = isAvailable(AdType.BANNER)

    /**
     * 检查原生广告是否可用
     */
    fun isNativeAvailable(): Boolean = isAvailable(AdType.NATIVE)

    /**
     * 检查激励视频广告是否可用
     */
    fun isRewardedAvailable(): Boolean = isAvailable(AdType.REWARDED)

    // ========== 事件监听 ==========

    /**
     * 注册事件监听器
     */
    fun addEventListener(listener: AdEventListener) {
        checkInitialized()
        eventBus.register(listener)
    }

    /**
     * 添加事件监听器（生命周期感知 - 推荐）
     *
     * 监听器会在 LifecycleOwner 销毁时自动移除，无需手动调用 removeEventListener()
     *
     * 支持匿名监听器：
     * ```kotlin
     * AdKit.addEventListener(this, object : AdEventListener {
     *     override fun onEvent(event: AdEvent) { ... }
     * })
     * ```
     *
     * @param owner LifecycleOwner (Activity 或 Fragment)
     * @param listener 事件监听器（可以是匿名对象）
     */
    fun addEventListener(owner: LifecycleOwner, listener: AdEventListener) {
        checkInitialized()
        eventBus.register(owner, listener)
    }

    /**
     * 注销事件监听器
     */
    fun removeEventListener(listener: AdEventListener) {
        if (isInitialized) {
            eventBus.unregister(listener)
        }
    }

    // ========== 配置管理 ==========

    /**
     * 设置广告位配置
     */
    fun setConfig(config: AdUnitConfig) {
        checkInitialized()
        configManager.setConfig(config)
    }

    /**
     * 批量设置配置
     */
    fun setConfigs(configs: List<AdUnitConfig>) {
        checkInitialized()
        configManager.setConfigs(configs)
    }

    /**
     * 获取配置
     */
    fun getConfig(adType: AdType): AdUnitConfig {
        checkInitialized()
        return configManager.getConfig(adType)
    }

    // ========== 指标统计 ==========

    /**
     * 获取指标统计器
     *
     * 内部使用，用于监控和调试
     */
    internal fun getMetrics(): AdMetrics {
        checkInitialized()
        return metrics
    }

    // ========== 资源清理 ==========

    /**
     * 🔧 FIX: 完善的清理逻辑
     * 清理所有资源，包括：
     * - 取消所有 Controller 的协程
     * - 关闭所有 Cache 的后台任务
     * - 清空所有缓存数据
     * - 释放所有监听器
     */
    fun cleanup() {
        if (!isInitialized) return

        // 1. 清理所有 Controller（取消协程）
        controllers.values.forEach { controller ->
            try {
                controller.clearAllCache()
                // 调用 BaseAdController.cleanup() 取消协程
                if (controller is AppOpenAdController) {
                    (controller as? Any)?.let { ctrl ->
                        ctrl::class.java.getMethod("cleanup").invoke(ctrl)
                    }
                }
            } catch (e: Exception) {
                AdLogger.e("Failed to cleanup controller", e)
            }
        }
        controllers.clear()

        // 2. 清空事件监听器
        eventBus.clearAll()

        // 3. 清空配置
        configManager.clear()

        // 4. 重置指标
        metrics.resetAll()

        isInitialized = false
        AdLogger.i("AdSdk cleaned up completely")
    }

    // ========== 内部方法 ==========

    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("AdSdk not initialized. Call initialize() first.")
        }
    }

    /**
     * 获取控制器（类型安全）
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> getController(adType: AdType): AdController<T>? {
        return controllers[adType] as? AdController<T>
    }
}
