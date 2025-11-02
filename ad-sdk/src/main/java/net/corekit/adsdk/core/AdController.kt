package net.corekit.adsdk.core

import android.app.Activity
import net.corekit.adsdk.event.AdEvent

/**
 * 广告控制器接口
 * 定义广告加载、展示、缓存等核心操作
 *
 * @param T 平台广告对象类型（如 AppOpenAd, InterstitialAd）
 */
internal interface AdController<T> {
    /**
     * 广告类型
     */
    val adType: AdType

    /**
     * 预加载广告
     * 将广告加载到缓存池中，不立即展示
     *
     * @param adUnitId 广告位 ID
     * @return 加载结果
     */
    suspend fun preload(adUnitId: String): AdResult<Unit>

    /**
     * 展示广告
     * 优先使用缓存中的广告，如果缓存为空则立即加载并展示
     *
     * 此方法会阻塞直到广告关闭，然后返回展示结果：
     * - 普通广告（开屏、插屏、横幅、原生）：返回 Success(AdShowData(reward = null))
     * - 激励广告：返回 Success(AdShowData(reward = RewardInfo(...)))
     * - 失败：返回 Failure(AdException)
     *
     * @param activity Activity 上下文
     * @param adUnitId 广告位 ID
     * @param onEvent 可选的事件回调，用于接收当前展示实例的中间事件（展示、点击等）
     *                仅接收当前调用的事件，不会收到其他页面/实例的事件
     * @return 展示结果，包含奖励信息（如果是激励广告）
     *
     * 使用示例：
     * ```kotlin
     * when (val result = controller.show(activity, adUnitId)) {
     *     is AdResult.Success -> {
     *         result.data.reward?.let { reward ->
     *             // 发放奖励（仅激励广告）
     *             grantReward(reward.type, reward.amount)
     *         }
     *     }
     *     is AdResult.Failure -> {
     *         handleError(result.error)
     *     }
     * }
     * ```
     */
    suspend fun show(
        activity: Activity,
        adUnitId: String,
        onEvent: ((AdEvent) -> Unit)? = null
    ): AdResult<AdShowData>

    /**
     * 🔧 FIX: 移除不必要的 suspend 关键字
     * 检查是否有可用的广告（同步方法）
     *
     * @param adUnitId 广告位 ID
     * @return true 如果有可用广告，false 否则
     */
    fun isAvailable(adUnitId: String): Boolean

    /**
     * 获取缓存的广告数量
     *
     * @param adUnitId 广告位 ID
     * @return 缓存数量
     */
    fun getCacheSize(adUnitId: String): Int

    /**
     * 清空指定广告位的缓存
     *
     * @param adUnitId 广告位 ID
     */
    fun clearCache(adUnitId: String)

    /**
     * 清空所有缓存
     */
    fun clearAllCache()
}
