package net.corekit.monetize.ads.bidding

/**
 * 广告类型枚举
 * 
 * 用于两层竞价中的平台内广告类型竞价
 */
enum class BiddingAdType {
    /** 开屏广告 */
    SPLASH,
    /** 插屏广告 */
    INTERSTITIAL,
    /** 原生广告 */
    NATIVE,
    /** 全屏原生广告 */
    FULL_NATIVE,
    /** Banner 广告 */
    BANNER,
    /** 激励广告 */
    REWARDED,
    /** 插页激励广告 */
    REWARDED_INTERSTITIAL;

    /**
     * 转换为配置文件 key 字符串
     */
    fun toConfigKey(): String = name.lowercase();

    companion object {
        /**
         * 从配置文件 key 字符串解析
         */
        fun fromConfigKey(key: String): BiddingAdType? {
            return entries.find { it.name.equals(key, ignoreCase = true) }
        }
    }
}

