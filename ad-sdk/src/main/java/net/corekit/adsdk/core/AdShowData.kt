package net.corekit.adsdk.core

/**
 * 广告展示数据
 * 封装广告展示成功后的相关信息
 *
 * @param reward 奖励信息（仅激励广告有值，其他广告类型为 null）
 */
data class AdShowData(
    val reward: RewardInfo? = null
) {
    companion object {
        /**
         * 无奖励的展示数据（用于普通广告）
         */
        val NO_REWARD = AdShowData(reward = null)
    }

    /**
     * 是否有奖励
     */
    val hasReward: Boolean get() = reward != null
}

/**
 * 奖励信息（用于激励广告）
 *
 * @param type 奖励类型（如 "coin", "life", "extra_time"）
 * @param amount 奖励数量
 */
data class RewardInfo(
    val type: String,
    val amount: Int
) {
    override fun toString(): String = "$type x $amount"
}
