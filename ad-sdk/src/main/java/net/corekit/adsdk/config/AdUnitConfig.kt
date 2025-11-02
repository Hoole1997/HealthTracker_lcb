package net.corekit.adsdk.config

import net.corekit.adsdk.core.AdType

/**
 * 广告位配置
 * 存储单个广告位的配置信息
 *
 * @param adType 广告类型
 * @param adUnitId 广告位 ID
 * @param enabled 是否启用
 * @param maxCacheSize 最大缓存数量
 * @param expiryDurationMs 广告过期时间（毫秒）
 */
data class AdUnitConfig(
    val adType: AdType,
    val adUnitId: String,
    val enabled: Boolean = true,
    val maxCacheSize: Int = 2,
    val expiryDurationMs: Long = 3600_000L // 1 小时
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun default(adType: AdType, adUnitId: String): AdUnitConfig {
            return AdUnitConfig(
                adType = adType,
                adUnitId = adUnitId,
                enabled = true,
                maxCacheSize = 2,
                expiryDurationMs = 3600_000L
            )
        }
    }
}
