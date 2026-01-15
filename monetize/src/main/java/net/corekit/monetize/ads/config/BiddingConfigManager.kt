package net.corekit.monetize.ads.config

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.core.controller.ChannelUserController
import net.corekit.core.ext.DataStoreStringDelegate
import net.corekit.core.utils.ConfigRemoteManager
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.BiddingWinner
import net.corekit.monetize.ads.log.AdLogger

/**
 * 竞价配置管理器
 * 
 * 管理多平台竞价相关配置，从 Firebase Remote Config 读取
 */
@SuppressLint("StaticFieldLeak")
object BiddingConfigManager {

    private const val TAG = "BiddingConfig"
    private const val REMOTE_CONFIG_KEY = "biddingConfigJson"
    
    // 缓存到本地的配置 JSON
    private var biddingConfigJsonFromRemote by DataStoreStringDelegate("bidding_config_json", "")
    
    private var configData: BiddingConfigData? = null
    private var context: Context? = null
    private var isInitialized = false

    /**
     * 确保配置已初始化（用于解决异步初始化时序问题）
     * 如果尚未初始化，则立即同步初始化
     */
    fun ensureInitialized(context: Context) {
        if (!isInitialized) {
            AdLogger.d("[$TAG] ensureInitialized: 配置未初始化，立即同步初始化")
            initialize(context)
        }
    }

    /**
     * 初始化竞价配置
     */
    fun initialize(context: Context) {
        this.context = context
        
        try {
            // 1. 尝试使用缓存的远程配置
            val cachedJson = biddingConfigJsonFromRemote
            if (!cachedJson.isNullOrEmpty()) {
                try {
                    configData = Gson().fromJson(cachedJson, BiddingConfigData::class.java)
                    BiddingPlatformController.setConfigData(configData)
                    AdLogger.d("[$TAG] 使用缓存的竞价配置")
                } catch (e: Exception) {
                    AdLogger.e("[$TAG] 解析缓存配置失败", e)
                }
            }

            // 2. 尝试从 assets 读取默认配置
            if (configData == null) {
                configData = loadFromAssets(context)
                if (configData != null) {
                    BiddingPlatformController.setConfigData(configData)
                    AdLogger.d("[$TAG] 使用 assets 默认竞价配置")
                }
            }

            // 3. 使用硬编码默认配置作为最终兜底
            if (configData == null) {
                configData = createDefaultConfig()
                BiddingPlatformController.setConfigData(configData)
                AdLogger.d("[$TAG] 使用硬编码默认竞价配置")
            }

            // 3. 异步获取远程配置
            fetchRemoteConfig()
            
            isInitialized = true
            AdLogger.d("[$TAG] 竞价配置初始化完成")
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 竞价配置初始化失败", e)
        }
    }

    /**
     * 异步获取远程配置
     */
    private fun fetchRemoteConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AdLogger.d("[$TAG] 开始获取远程竞价配置")
                val remoteJson = ConfigRemoteManager.getString(REMOTE_CONFIG_KEY, "")
                
                if (!remoteJson.isNullOrEmpty()) {
                    val remoteConfig = Gson().fromJson(remoteJson, BiddingConfigData::class.java)
                    if (remoteConfig != null && (remoteConfig.natural != null || remoteConfig.paid != null)) {
                        configData = remoteConfig
                        biddingConfigJsonFromRemote = remoteJson
                        BiddingPlatformController.setConfigData(configData)
                        AdLogger.d("[$TAG] 远程竞价配置更新成功")
                        logCurrentConfig()
                    } else {
                        AdLogger.w("[$TAG] 远程竞价配置格式无效")
                    }
                } else {
                    AdLogger.w("[$TAG] 远程竞价配置为空，使用本地配置")
                }
            } catch (e: Exception) {
                AdLogger.e("[$TAG] 获取远程竞价配置失败", e)
            }
        }
    }

    // ==================== 全局开关 ====================

    /**
     * 是否启用多平台竞价
     */
    fun isBiddingEnabled(): Boolean {
        return getCurrentChannelConfig()?.biddingEnabled == 1
    }

    /**
     * 是否启用两层竞价
     * true: 平台内竞价 + 跨平台竞价
     * false: 仅跨平台竞价（各平台固定广告类型）
     */
    fun isTwoLayerBiddingEnabled(): Boolean {
        return getCurrentChannelConfig()?.twoLayerBiddingEnabled == 1
    }

    /**
     * 获取竞价超时时间（毫秒）
     */
    fun getBiddingTimeoutMs(): Long {
        return (getCurrentChannelConfig()?.biddingTimeoutSeconds ?: 10) * 1000L
    }

    // ==================== 平台配置 ====================

    /**
     * 判断平台是否启用
     */
    fun isPlatformEnabled(platform: BiddingWinner): Boolean {
        return BiddingPlatformController.isPlatformEnabled(platform)
    }

    /**
     * 判断平台的某广告类型是否启用
     */
    fun isAdTypeEnabled(platform: BiddingWinner, adType: String): Boolean {
        return BiddingPlatformController.isAdTypeEnabled(platform, adType)
    }

    /**
     * 判断平台的某广告类型是否参与竞价
     */
    fun shouldParticipateInBidding(platform: BiddingWinner, adType: String): Boolean {
        return BiddingPlatformController.shouldParticipateInBidding(platform, adType)
    }

    /**
     * 获取平台优先级（eCPM 相同时使用）
     */
    fun getPlatformPriority(platform: BiddingWinner): Int {
        return BiddingPlatformController.getPlatformPriority(platform)
    }

    // ==================== 场景配置 ====================

    /**
     * 获取场景的竞价模式
     */
    fun getSceneBiddingMode(scene: String): String {
        return getCurrentChannelConfig()?.sceneConfig?.get(scene)?.biddingMode ?: "two_layer"
    }

    /**
     * 获取场景的平台内竞价广告类型列表
     */
    fun getInternalBiddingTypes(scene: String): List<String> {
        return getCurrentChannelConfig()?.sceneConfig?.get(scene)?.internalBiddingTypes 
            ?: listOf("splash", "interstitial")
    }

    /**
     * 获取场景的回退配置
     * @return Pair<回退平台, 回退广告类型>
     */
    fun getFallbackConfig(scene: String): Pair<String, String> {
        val sceneConfig = getCurrentChannelConfig()?.sceneConfig?.get(scene)
        return Pair(
            sceneConfig?.fallbackPlatform ?: "admob",
            sceneConfig?.fallbackAdType ?: "splash"
        )
    }

    // ==================== 平台级频控 ====================

    /**
     * 平台级频控是否启用
     * 默认禁用（追求收入最大化）
     */
    fun isPlatformFrequencyEnabled(): Boolean {
        return getCurrentChannelConfig()?.platformFrequencyEnabled ?: false
    }

    /**
     * 获取指定平台和广告类型的频控配置
     * 
     * @param platform 平台类型
     * @param adType 广告类型（如 "splash", "interstitial", "rewarded"）
     * @return 频控配置，如果未配置则返回 null
     */
    fun getPlatformFrequencyConfig(
        platform: net.corekit.monetize.ads.bidding.BiddingPlatform, 
        adType: String
    ): BiddingConfigData.PlatformFrequencyConfig? {
        val frequencyConfigs = getCurrentChannelConfig()?.platformFrequency ?: return null
        val platformConfigs = when (platform) {
            net.corekit.monetize.ads.bidding.BiddingPlatform.ADMOB -> frequencyConfigs.admob
            net.corekit.monetize.ads.bidding.BiddingPlatform.PANGLE -> frequencyConfigs.pangle
            net.corekit.monetize.ads.bidding.BiddingPlatform.TOPON -> frequencyConfigs.topon
        }
        return platformConfigs?.get(adType)
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 assets 加载默认配置
     */
    private fun loadFromAssets(context: Context): BiddingConfigData? {
        return try {
            val jsonString = context.assets.open("bidding_config_default.json")
                .bufferedReader()
                .use { it.readText() }
            Gson().fromJson(jsonString, BiddingConfigData::class.java)
        } catch (e: Exception) {
            AdLogger.w("[$TAG] 从 assets 加载配置失败: ${e.message}")
            null
        }
    }

    private fun getCurrentChannelConfig(): BiddingConfigData.ChannelBiddingConfig? {
        return configData?.let { config ->
            try {
                when (ChannelUserController.getCurrentChannel()) {
                    ChannelUserController.UserChannelType.NATURAL -> config.natural
                    ChannelUserController.UserChannelType.PAID -> config.paid
                }
            } catch (e: Exception) {
                AdLogger.e("[$TAG] 获取用户渠道失败，使用 natural 配置", e)
                config.natural
            }
        }
    }

    /**
     * 创建默认配置（全部启用）
     */
    private fun createDefaultConfig(): BiddingConfigData {
        val defaultAdTypes = mapOf(
            "splash" to BiddingConfigData.AdTypeConfig(1, 1),
            "interstitial" to BiddingConfigData.AdTypeConfig(1, 1),
            "native" to BiddingConfigData.AdTypeConfig(1, 1),
            "full_native" to BiddingConfigData.AdTypeConfig(1, 1),
            "banner" to BiddingConfigData.AdTypeConfig(1, 1),
            "rewarded" to BiddingConfigData.AdTypeConfig(1, 1),
            "rewarded_interstitial" to BiddingConfigData.AdTypeConfig(1, 1)
        )
        
        val admobConfig = BiddingConfigData.PlatformConfig(1, 1, defaultAdTypes)
        val pangleConfig = BiddingConfigData.PlatformConfig(1, 2, defaultAdTypes)
        val toponConfig = BiddingConfigData.PlatformConfig(1, 3, defaultAdTypes)
        
        val platformsConfig = BiddingConfigData.PlatformsConfig(admobConfig, pangleConfig, toponConfig)
        
        val defaultSceneConfig = mapOf(
            "splash_scene" to BiddingConfigData.SceneConfig(
                biddingMode = "two_layer",
                internalBiddingTypes = listOf("splash", "interstitial"),
                fallbackPlatform = "admob",
                fallbackAdType = "splash"
            ),
            "reward_scene" to BiddingConfigData.SceneConfig(
                biddingMode = "two_layer",
                internalBiddingTypes = listOf("rewarded", "rewarded_interstitial", "interstitial"),
                fallbackPlatform = "admob",
                fallbackAdType = "rewarded"
            )
        )
        
        val channelConfig = BiddingConfigData.ChannelBiddingConfig(
            biddingEnabled = 1,
            twoLayerBiddingEnabled = 1,
            biddingTimeoutSeconds = 10,
            platforms = platformsConfig,
            sceneConfig = defaultSceneConfig
        )
        
        return BiddingConfigData(channelConfig, channelConfig)
    }

    /**
     * 打印当前配置（表格化输出）
     */
    private fun logCurrentConfig() {
        val config = getCurrentChannelConfig() ?: return
        
        // 收集所有配置条目
        val entries = mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.ConfigEntry>()
        val adTypes = listOf("splash", "interstitial", "native", "banner", "rewarded", "rewarded_interstitial", "full_native")

        // 收集 AdMob, Pangle, TopOn
        val platformEnums = listOf(
            net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB,
            net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE,
            net.corekit.monetize.ads.bidding.BiddingWinner.TOPON
        )

        platformEnums.forEach { platform ->
            adTypes.forEach { adType ->
                val enabled = BiddingPlatformController.isAdTypeEnabled(platform, adType)
                val isBidding = BiddingPlatformController.shouldParticipateInBidding(platform, adType)
                
                // 获取频控信息
                val freqConfig = getPlatformFrequencyConfig(
                    when(platform) {
                        net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB -> net.corekit.monetize.ads.bidding.BiddingPlatform.ADMOB
                        net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE -> net.corekit.monetize.ads.bidding.BiddingPlatform.PANGLE
                        net.corekit.monetize.ads.bidding.BiddingWinner.TOPON -> net.corekit.monetize.ads.bidding.BiddingPlatform.TOPON
                    }, 
                    adType
                )
                val freqStr = freqConfig?.let { "Daily:${it.maxDailyShow}" }
                
                entries.add(net.corekit.monetize.ads.log.BiddingLogger.ConfigEntry(
                    platform = platform.name,
                    adType = adType,
                    enabled = enabled,
                    isBidding = isBidding,
                    frequencyLimit = freqStr,
                    configSource = if (configData?.natural != null) "Remote/Local" else "Default"
                ))
            }
        }
        
        // 调用 Logger 输出
        net.corekit.monetize.ads.log.BiddingLogger.logAllConfigs(
            entries, 
            configSource = if (!biddingConfigJsonFromRemote.isNullOrEmpty()) "Remote" else "Local/Default"
        )
    }
}
