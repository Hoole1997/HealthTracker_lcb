package net.corekit.monetize.ads.bidding

/**
 * 最终竞价结果
 * 
 * 表示两层竞价的最终结果，包含胜出者和所有参与竞价的平台结果
 * 
 * @property winner 最终胜出者（可空，表示没有可用广告）
 * @property allResults 所有平台的竞价结果
 * @property biddingTimeMs 竞价总耗时（毫秒）
 */
data class FinalBidResult(
    val winner: PlatformBidResult?,
    val allResults: List<PlatformBidResult>,
    val biddingTimeMs: Long
) {
    /**
     * 是否有有效的广告
     */
    fun hasValidAd(): Boolean {
        return winner?.hasValidAd() == true
    }
    
    /**
     * 获取参与竞价的平台数量
     */
    fun getParticipantCount(): Int {
        return allResults.size
    }
    
    /**
     * 获取成功加载广告的平台数量
     */
    fun getSuccessCount(): Int {
        return allResults.count { it.hasValidAd() }
    }
    
    companion object {
        /**
         * 创建一个失败的竞价结果（所有平台都加载失败时使用）
         */
        fun failed(biddingTimeMs: Long): FinalBidResult {
            return FinalBidResult(
                winner = null,
                allResults = emptyList(),
                biddingTimeMs = biddingTimeMs
            )
        }
        
        /**
         * 创建一个回退竞价结果（使用默认平台和广告类型）
         */
        fun fallback(
            platform: BiddingPlatform,
            adType: BiddingAdType,
            biddingTimeMs: Long = 0
        ): FinalBidResult {
            val fallbackResult = PlatformBidResult(
                platform = platform,
                winnerType = adType,
                ecpm = 0.0,
                adTypeEcpmMap = emptyMap()
            )
            return FinalBidResult(
                winner = fallbackResult,
                allResults = listOf(fallbackResult),
                biddingTimeMs = biddingTimeMs
            )
        }
    }
}
