package net.corekit.monetize.ads.bidding

import net.corekit.monetize.ads.log.AdLogger

/**
 * 竞价结果缓存管理器
 * 
 * 用于缓存预竞价结果，避免展示时重复执行竞价逻辑。
 * 支持激励广告、原生广告等多种广告类型的竞价结果缓存。
 */
object BiddingResultCache {

    private const val TAG = "BiddingResultCache"
    
    // 竞价结果有效期：30 分钟
    private const val RESULT_EXPIRE_TIME = 30 * 60 * 1000L

    // ============ 激励广告竞价结果缓存 ============
    
    private var rewardBidResult: FinalBidResult? = null
    private var rewardBidTime: Long = 0

    /**
     * 缓存激励广告竞价结果
     */
    fun cacheRewardBidResult(result: FinalBidResult) {
        rewardBidResult = result
        rewardBidTime = System.currentTimeMillis()
        AdLogger.d("[$TAG] 缓存激励广告竞价结果: ${result.winner?.platform?.name} - ${result.winner?.winnerType?.name}")
    }

    /**
     * 获取有效的激励广告竞价结果
     * @return 有效的竞价结果，如果过期或不存在则返回 null
     */
    fun getValidRewardBidResult(): FinalBidResult? {
        val result = rewardBidResult ?: return null
        
        // 检查是否过期
        if (System.currentTimeMillis() - rewardBidTime > RESULT_EXPIRE_TIME) {
            AdLogger.d("[$TAG] 激励广告竞价结果已过期，清空缓存")
            rewardBidResult = null
            return null
        }
        
        // 检查胜出广告是否仍有有效缓存
        result.winner?.let { winner ->
            if (!isAdStillValid(winner)) {
                AdLogger.d("[$TAG] 胜出广告已被消耗或过期，清空缓存")
                rewardBidResult = null
                return null
            }
        }
        
        return result
    }

    /**
     * 清空激励广告竞价结果（展示后调用）
     */
    fun clearRewardBidResult() {
        rewardBidResult = null
        rewardBidTime = 0
        AdLogger.d("[$TAG] 已清空激励广告竞价结果缓存")
    }

    /**
     * 检查是否有有效的激励广告竞价结果
     */
    fun hasValidRewardBidResult(): Boolean {
        return getValidRewardBidResult() != null
    }

    // ============ 原生广告竞价结果缓存 ============
    
    private var nativeBidResult: BiddingWinner? = null
    private var nativeBidTime: Long = 0

    /**
     * 缓存原生广告竞价结果
     */
    fun cacheNativeBidResult(winner: BiddingWinner) {
        nativeBidResult = winner
        nativeBidTime = System.currentTimeMillis()
        AdLogger.d("[$TAG] 缓存原生广告竞价结果: ${winner.name}")
    }

    /**
     * 获取有效的原生广告竞价结果
     */
    fun getValidNativeBidResult(): BiddingWinner? {
        val result = nativeBidResult ?: return null
        
        if (System.currentTimeMillis() - nativeBidTime > RESULT_EXPIRE_TIME) {
            AdLogger.d("[$TAG] 原生广告竞价结果已过期")
            nativeBidResult = null
            return null
        }
        
        return result
    }

    /**
     * 清空原生广告竞价结果
     */
    fun clearNativeBidResult() {
        nativeBidResult = null
        nativeBidTime = 0
    }

    // ============ 辅助方法 ============

    /**
     * 检查胜出的广告是否仍然有效（未被消耗、未过期）
     */
    private fun isAdStillValid(winner: PlatformBidResult): Boolean {
        return when (winner.winnerType) {
            BiddingAdType.REWARDED -> {
                when (winner.platform) {
                    BiddingPlatform.ADMOB -> {
                        net.corekit.monetize.ads.RewardedAds.getInstance().hasCachedAd()
                    }
                    BiddingPlatform.PANGLE -> {
                        net.corekit.monetize.ads.pangle.PangleRewardedAdController.getInstance().hasValidCache()
                    }
                    BiddingPlatform.TOPON -> {
                        net.corekit.monetize.ads.topon.TopOnRewardedAdController.getInstance().hasValidCache()
                    }
                }
            }
            BiddingAdType.REWARDED_INTERSTITIAL -> {
                net.corekit.monetize.ads.RewardedInterstitialAds.getInstance().hasCachedAd()
            }
            BiddingAdType.INTERSTITIAL -> {
                when (winner.platform) {
                    BiddingPlatform.ADMOB -> {
                        net.corekit.monetize.ads.AdsManager.Controllers.interstitial.hasCachedAd()
                    }
                    BiddingPlatform.PANGLE -> {
                        net.corekit.monetize.ads.pangle.PangleInterstitialAdController.getInstance().hasValidCache()
                    }
                    BiddingPlatform.TOPON -> {
                        net.corekit.monetize.ads.topon.TopOnInterstitialAdController.getInstance().hasValidCache()
                    }
                }
            }
            else -> true
        }
    }

    /**
     * 清空所有缓存
     */
    fun clearAll() {
        clearRewardBidResult()
        clearNativeBidResult()
        AdLogger.d("[$TAG] 已清空所有竞价结果缓存")
    }
}
