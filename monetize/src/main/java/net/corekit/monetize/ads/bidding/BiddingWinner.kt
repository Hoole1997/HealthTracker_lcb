package net.corekit.monetize.ads.bidding

/**
 * 竞价平台枚举
 * 
 * 表示参与广告竞价的平台
 */
enum class BiddingWinner {
    /** AdMob 平台 */
    ADMOB,
    /** Pangle (穿山甲) 平台 */
    PANGLE,
    /** TopOn 平台 */
    TOPON
}

/**
 * 类型别名：BiddingPlatform = BiddingWinner
 * 方便在竞价管理器中使用更语义化的名称
 */
typealias BiddingPlatform = BiddingWinner
