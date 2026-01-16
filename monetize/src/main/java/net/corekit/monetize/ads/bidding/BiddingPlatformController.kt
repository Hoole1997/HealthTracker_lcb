package net.corekit.monetize.ads.bidding

import net.corekit.core.controller.ChannelUserController
import net.corekit.monetize.ads.config.BiddingConfigData
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.log.AdLogger

/**
 * 竞价平台控制器
 * 
 * 根据配置控制各平台、各广告类型的启用状态
 * 配置优先从 BiddingConfigManager 读取，未配置时使用默认值
 */
object BiddingPlatformController {

    private const val TAG = "PlatformConfig"

    // ==================== 默认配置（硬编码兜底） ====================
    
    // 默认启用的平台
    private const val DEFAULT_ADMOB_ENABLED = true
    private const val DEFAULT_PANGLE_ENABLED = true
    private const val DEFAULT_TOPON_ENABLED = true
    
    // 默认平台优先级（数字越小优先级越高）
    private const val DEFAULT_ADMOB_PRIORITY = 1
    private const val DEFAULT_PANGLE_PRIORITY = 2
    private const val DEFAULT_TOPON_PRIORITY = 3

    // ==================== 配置引用 ====================
    
    private var configData: BiddingConfigData? = null
    
    /**
     * 设置配置数据
     */
    fun setConfigData(config: BiddingConfigData?) {
        configData = config
        AdLogger.d("[$TAG] 配置数据已更新")
    }

    // ==================== 平台启用判断 ====================

    /**
     * 判断平台是否启用
     */
    fun isPlatformEnabled(platform: BiddingWinner): Boolean {
        val platformConfig = getPlatformConfig(platform)
        val enabled = platformConfig?.enabled == 1
        
        return if (platformConfig != null) {
            enabled
        } else {
            // 使用默认值
            when (platform) {
                BiddingWinner.ADMOB -> DEFAULT_ADMOB_ENABLED
                BiddingWinner.PANGLE -> DEFAULT_PANGLE_ENABLED
                BiddingWinner.TOPON -> DEFAULT_TOPON_ENABLED
            }
        }
    }

    /**
     * 判断平台的某广告类型是否启用
     */
    fun isAdTypeEnabled(platform: BiddingWinner, adType: String): Boolean {
        val platformConfig = getPlatformConfig(platform)
        val adTypeConfig = platformConfig?.adTypes?.get(adType)
        return adTypeConfig?.enabled == 1
    }

    /**
     * 判断平台的某广告类型是否参与预加载（不检查频控）
     * 
     * 用于预加载阶段，允许广告预先缓存好，不受频控限制
     * 频控检查在展示/竞价阶段进行
     */
    fun shouldParticipateInPreload(platform: BiddingWinner, adType: String): Boolean {
        // 1. 检查平台是否启用
        if (!isPlatformEnabled(platform)) {
            return false
        }
        
        // 2. 检查是否有有效的广告 ID（空 ID 不参与预加载）
        if (!AdIdHelper.hasValidAdId(platform, adType)) {
            return false
        }
        
        // 3. 检查配置中是否允许参与竞价
        val platformConfig = getPlatformConfig(platform)
        val adTypeConfig = platformConfig?.adTypes?.get(adType)
        
        // 如果没有配置，默认参与
        if (adTypeConfig == null) {
            return true
        }
        
        return adTypeConfig.enabled == 1 && adTypeConfig.participateBidding == 1
    }

    /**
     * 判断平台的某广告类型是否参与竞价（包含频控检查）
     * 
     * 需要满足以下条件：
     * 1. 平台已启用
     * 2. 该广告类型已配置有效的广告 ID
     * 3. 配置中该广告类型允许参与竞价
     * 4. 平台级频控允许
     */
    fun shouldParticipateInBidding(platform: BiddingWinner, adType: String): Boolean {
        // 1-3. 基础检查（复用预加载检查）
        if (!shouldParticipateInPreload(platform, adType)) {
            return false
        }
        
        // 4. 检查平台级频控（仅在竞价时检查，预加载时不检查）
        val biddingPlatform = platform.toBiddingPlatform()
        val biddingAdType = BiddingAdType.fromConfigKey(adType)
        if (biddingPlatform != null && biddingAdType != null) {
            if (!PlatformFrequencyManager.canParticipate(biddingPlatform, biddingAdType)) {
                AdLogger.d("[$TAG] %s %s frequency limit reached, skip bidding", platform.name, adType)
                return false
            }
        }
        
        return true
    }

    /**
     * 获取平台优先级（eCPM 相同时使用，数字越小优先级越高）
     */
    fun getPlatformPriority(platform: BiddingWinner): Int {
        val platformConfig = getPlatformConfig(platform)
        return platformConfig?.priority ?: when (platform) {
            BiddingWinner.ADMOB -> DEFAULT_ADMOB_PRIORITY
            BiddingWinner.PANGLE -> DEFAULT_PANGLE_PRIORITY
            BiddingWinner.TOPON -> DEFAULT_TOPON_PRIORITY
        }
    }

    /**
     * 判断多平台竞价是否启用
     * 
     * 当配置存在且至少有两个平台启用时返回 true
     */
    fun isMultiPlatformBiddingEnabled(): Boolean {
        // 检查是否有配置数据
        val config = configData
        if (config == null) {
            AdLogger.w("[$TAG] isMultiPlatformBiddingEnabled: false (configData 为空)")
            return false
        }
        
        // 统计启用的平台数量
        val admobEnabled = isPlatformEnabled(BiddingWinner.ADMOB)
        val pangleEnabled = isPlatformEnabled(BiddingWinner.PANGLE)
        val toponEnabled = isPlatformEnabled(BiddingWinner.TOPON)
        
        val enabledCount = listOf(admobEnabled, pangleEnabled, toponEnabled).count { it }
        
        val result = enabledCount >= 2
        AdLogger.d("[$TAG] isMultiPlatformBiddingEnabled: %s (AdMob=%s, Pangle=%s, TopOn=%s, 启用数=%d)",
            result, admobEnabled, pangleEnabled, toponEnabled, enabledCount)
        
        // 至少需要两个平台启用才算多平台竞价
        return result
    }

    // ==================== 辅助方法 ====================

    fun getCurrentChannelConfig(): BiddingConfigData.ChannelBiddingConfig? {
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

    private fun getPlatformConfig(platform: BiddingWinner): BiddingConfigData.PlatformConfig? {
        val platforms = getCurrentChannelConfig()?.platforms ?: return null
        return when (platform) {
            BiddingWinner.ADMOB -> platforms.admob
            BiddingWinner.PANGLE -> platforms.pangle
            BiddingWinner.TOPON -> platforms.topon
        }
    }

    /**
     * 打印当前平台配置（仅 Debug 模式）
     */
    fun logCurrentConfig() {
        AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 当前平台配置")
        AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
        
        listOf(BiddingWinner.ADMOB, BiddingWinner.PANGLE, BiddingWinner.TOPON).forEach { platform ->
            val enabled = isPlatformEnabled(platform)
            val priority = getPlatformPriority(platform)
            AdLogger.d("[$TAG] ║ %s: %s (优先级: %d)", 
                platform.name,
                if (enabled) "✅ 启用" else "❌ 禁用",
                priority
            )
        }
        
        AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
    }

    // ==================== 测试模式支持 ====================

    /**
     * 测试模式 Mock eCPM 值 (USD)
     * 用于在测试广告 ID 返回 0 eCPM 时提供模拟值以验证竞价逻辑
     */
    private val TEST_MODE_MOCK_ECPM = mapOf(
        BiddingWinner.ADMOB to 0.010,
        BiddingWinner.PANGLE to 0.018,
        BiddingWinner.TOPON to 0.019
    )

    /**
     * 获取用于竞价的有效 eCPM 值
     * 
     * 测试模式下，如果真实值为 0，则返回 Mock 值以便验证竞价逻辑
     * 
     * @param platform 平台类型
     * @param realEcpm 从 SDK 获取的真实 eCPM 值
     * @return 有效的 eCPM 值（真实值或 Mock 值）
     */
    fun getEffectiveEcpm(platform: BiddingWinner, realEcpm: Double): Double {
        // 定义占位符阈值：低于此值的 eCPM 被视为占位符，需要在测试模式下注入 Mock
        val placeholderThreshold = 0.005
        
        // 真实值有效时（高于占位符阈值）直接使用
        if (realEcpm >= placeholderThreshold) {
            return realEcpm
        }
        
        // 非测试模式直接返回真实值
        if (!isTestMode()) {
            return realEcpm
        }
        
        // 测试模式：注入 Mock 值（优先使用自定义值）
        val mockEcpm = customMockEcpm[platform] ?: TEST_MODE_MOCK_ECPM[platform] ?: 0.0
        AdLogger.d("[$TAG] [TestMode] 注入 Mock eCPM: %s = %.6f USD (真实值: %.6f)", 
            platform.name, mockEcpm, realEcpm)
        return mockEcpm
    }

    /**
     * 判断是否处于测试模式
     * 
     * 测试模式下会在真实 eCPM 为 0 时注入 Mock 值
     * 支持调试面板强制开关覆盖
     */
    fun isTestMode(): Boolean {
        return forceTestModeEnabled ?: net.corekit.monetize.BuildConfig.DEBUG
    }

    /**
     * 更新 Mock eCPM 值（用于测试不同竞价场景）
     * 
     * 注意：此方法仅在测试模式下有效
     */
    private val customMockEcpm = mutableMapOf<BiddingWinner, Double>()
    
    fun setMockEcpm(platform: BiddingWinner, ecpm: Double) {
        if (!isTestMode()) {
            AdLogger.w("[$TAG] 非测试模式，无法设置 Mock eCPM")
            return
        }
        customMockEcpm[platform] = ecpm
        AdLogger.d("[$TAG] [TestMode] 设置 %s Mock eCPM = %.6f USD", platform.name, ecpm)
    }
    
    fun getMockEcpm(platform: BiddingWinner): Double {
        return customMockEcpm[platform] ?: TEST_MODE_MOCK_ECPM[platform] ?: 0.0
    }

    // ==================== 调试面板支持 ====================

    /**
     * 强制测试模式开关（用于调试面板）
     * 优先级高于 BuildConfig.DEBUG
     */
    private var forceTestModeEnabled: Boolean? = null

    /**
     * 设置测试模式（用于调试面板）
     * 
     * @param enabled true 强制启用，false 强制禁用，null 恢复默认行为
     */
    fun setTestMode(enabled: Boolean?) {
        forceTestModeEnabled = enabled
        AdLogger.d("[$TAG] [TestMode] 测试模式已${if (enabled == true) "启用" else if (enabled == false) "禁用" else "恢复默认"}")
    }

    /**
     * 清除所有自定义 Mock eCPM 值
     */
    fun clearMockEcpm() {
        customMockEcpm.clear()
        AdLogger.d("[$TAG] [TestMode] Mock eCPM values cleared")
    }
}

/**
 * BiddingWinner to BiddingPlatform conversion
 */
internal fun BiddingWinner.toBiddingPlatform(): BiddingPlatform = when (this) {
    BiddingWinner.ADMOB -> BiddingPlatform.ADMOB
    BiddingWinner.PANGLE -> BiddingPlatform.PANGLE
    BiddingWinner.TOPON -> BiddingPlatform.TOPON
}
