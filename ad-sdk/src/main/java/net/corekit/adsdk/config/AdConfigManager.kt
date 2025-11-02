package net.corekit.adsdk.config

import net.corekit.adsdk.core.AdType
import java.util.concurrent.ConcurrentHashMap

/**
 * 广告配置管理器
 * 负责管理所有广告位的配置
 */
internal class AdConfigManager {
    // 配置存储：AdType -> AdUnitConfig
    private val configs = ConcurrentHashMap<AdType, AdUnitConfig>()

    /**
     * 设置广告位配置
     */
    fun setConfig(config: AdUnitConfig) {
        configs[config.adType] = config
    }

    /**
     * 批量设置配置
     */
    fun setConfigs(configs: List<AdUnitConfig>) {
        configs.forEach { setConfig(it) }
    }

    /**
     * 获取广告位配置
     * 如果不存在，返回默认配置
     */
    fun getConfig(adType: AdType): AdUnitConfig {
        return configs[adType] ?: AdUnitConfig.default(
            adType = adType,
            adUnitId = "" // 空 ID，需要业务层配置
        )
    }

    /**
     * 检查广告位是否启用
     */
    fun isEnabled(adType: AdType): Boolean {
        return configs[adType]?.enabled ?: false
    }

    /**
     * 🔧 FIX: 修复 setEnabled() 在配置不存在时无效的问题
     * 启用/禁用广告位
     */
    fun setEnabled(adType: AdType, enabled: Boolean) {
        val existingConfig = configs[adType]
        if (existingConfig != null) {
            // 配置存在，更新 enabled 状态
            configs[adType] = existingConfig.copy(enabled = enabled)
        } else {
            // 配置不存在，创建默认配置并设置 enabled
            configs[adType] = AdUnitConfig.default(
                adType = adType,
                adUnitId = ""
            ).copy(enabled = enabled)
        }
    }

    /**
     * 获取所有配置
     */
    fun getAllConfigs(): Map<AdType, AdUnitConfig> {
        return configs.toMap()
    }

    /**
     * 清空所有配置
     */
    fun clear() {
        configs.clear()
    }
}
