package net.corekit.monetize.ads.bidding

/**
 * 平台内竞价结果
 * 
 * 表示单个平台内部广告类型竞价的结果
 * 
 * @property platform 平台标识
 * @property winnerType 胜出的广告类型（开屏/插屏/激励等）
 * @property ecpm 胜出者的 eCPM（美元）
 * @property loadTimeMs 加载耗时（毫秒）
 * @property adTypeEcpmMap 各广告类型的 eCPM 映射（用于日志）
 */
data class PlatformBidResult(
    val platform: BiddingPlatform,
    val winnerType: BiddingAdType,
    val ecpm: Double,
    val loadTimeMs: Long = 0,
    val adTypeEcpmMap: Map<BiddingAdType, Double> = emptyMap()
) {
    /**
     * 获取指定广告类型的 eCPM
     */
    fun getEcpm(adType: BiddingAdType): Double {
        return adTypeEcpmMap[adType] ?: 0.0
    }
    
    /**
     * 是否有有效的广告
     */
    fun hasValidAd(): Boolean {
        return ecpm > 0.0
    }
    
    companion object {
        /**
         * 创建一个空的竞价结果（所有广告加载失败时使用）
         */
        fun empty(platform: BiddingPlatform): PlatformBidResult {
            return PlatformBidResult(
                platform = platform,
                winnerType = BiddingAdType.SPLASH,
                ecpm = 0.0,
                adTypeEcpmMap = emptyMap()
            )
        }
    }
}
