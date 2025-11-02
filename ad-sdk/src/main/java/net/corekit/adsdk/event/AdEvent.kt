package net.corekit.adsdk.event

import net.corekit.adsdk.core.AdType

/**
 * 广告事件密封类
 * 定义所有广告生命周期事件
 */
sealed class AdEvent {
    /**
     * 广告类型
     */
    abstract val adType: AdType

    /**
     * 广告位 ID
     */
    abstract val adUnitId: String

    /**
     * 事件时间戳
     */
    abstract val timestamp: Long

    /**
     * 加载开始事件
     */
    data class LoadStart(
        override val adType: AdType,
        override val adUnitId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 加载成功事件
     *
     * @param duration 加载耗时（毫秒）
     */
    data class LoadSuccess(
        override val adType: AdType,
        override val adUnitId: String,
        val duration: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 加载失败事件
     *
     * @param reason 失败原因
     * @param duration 加载耗时（毫秒）
     */
    data class LoadFailure(
        override val adType: AdType,
        override val adUnitId: String,
        val reason: String,
        val duration: Long,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 触发展示事件（调用 show 方法时）
     */
    data class ShowTrigger(
        override val adType: AdType,
        override val adUnitId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 展示成功事件（广告开始展示时）
     */
    data class ShowSuccess(
        override val adType: AdType,
        override val adUnitId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 展示失败事件
     *
     * @param reason 失败原因
     */
    data class ShowFailure(
        override val adType: AdType,
        override val adUnitId: String,
        val reason: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 广告点击事件
     */
    data class Click(
        override val adType: AdType,
        override val adUnitId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 广告曝光事件（产生收益时触发）
     *
     * @param valueMicros 收益值（微元，需除以 1,000,000）
     * @param currencyCode 货币代码（如 USD）
     * @param adSource 广告源（如 AdMob, Facebook）
     */
    data class Impression(
        override val adType: AdType,
        override val adUnitId: String,
        val valueMicros: Long,
        val currencyCode: String,
        val adSource: String = "",
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 广告关闭事件
     */
    data class Close(
        override val adType: AdType,
        override val adUnitId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()

    /**
     * 激励广告完成事件
     *
     * @param rewardType 奖励类型
     * @param rewardAmount 奖励数量
     */
    data class Rewarded(
        override val adType: AdType,
        override val adUnitId: String,
        val rewardType: String,
        val rewardAmount: Int,
        override val timestamp: Long = System.currentTimeMillis()
    ) : AdEvent()
}
