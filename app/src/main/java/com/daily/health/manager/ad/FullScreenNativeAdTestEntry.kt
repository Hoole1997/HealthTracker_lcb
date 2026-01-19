package com.daily.health.manager.ad

import android.app.Activity
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.bidding.FullScreenNativeBiddingManager
import net.corekit.monetize.ads.log.AdLogger

/**
 * 全屏原生广告测试入口
 * 
 * 使用示例：
 * ```kotlin
 * // 在 Activity 中调用
 * lifecycleScope.launch {
 *     FullScreenNativeAdTestEntry.showWithBidding(this@YourActivity)
 * }
 * 
 * // 或直接指定平台
 * FullScreenNativeAdTestEntry.showDirectly(this, BiddingPlatform.ADMOB)
 * ```
 */
object FullScreenNativeAdTestEntry {

    private const val TAG = "FullNaTest"

    /**
     * 执行竞价并展示全屏原生广告
     * 
     * @param activity 调用方 Activity
     * @param position 广告位置标识（用于日志和统计）
     */
    suspend fun showWithBidding(activity: Activity, position: String = "test") {
        AdLogger.d("[$TAG] 开始执行全屏原生广告竞价...")

        // 检查是否有可用广告
        if (!FullScreenNativeBiddingManager.hasAnyReadyAd()) {
            AdLogger.w("[$TAG] 没有可用的全屏原生广告，请先预加载")
            return
        }

        // 执行竞价获取胜出平台
        val winner = FullScreenNativeBiddingManager.bidding(activity)
        AdLogger.d("[$TAG] 竞价完成，胜出平台: %s", winner.name)

        // 启动全屏展示页面
        FullScreenNativeAdActivity.start(
            activity = activity,
            platform = winner.toBiddingPlatform(),
            position = position
        )
    }

    /**
     * 直接展示指定平台的全屏原生广告（跳过竞价）
     * 
     * @param activity 调用方 Activity
     * @param platform 指定平台
     * @param position 广告位置标识
     */
    fun showDirectly(activity: Activity, platform: BiddingPlatform, position: String = "test") {
        AdLogger.d("[$TAG] 直接展示 %s 全屏原生广告", platform.name)
        FullScreenNativeAdActivity.start(
            activity = activity,
            platform = platform,
            position = position
        )
    }

    /**
     * 自动竞价展示（简化调用）
     * 
     * @param activity 调用方 Activity
     * @param lifecycleScope 协程作用域
     * @param position 广告位置标识
     */
    fun showAuto(
        activity: Activity,
        lifecycleScope: LifecycleCoroutineScope,
        position: String = "test"
    ) {
        lifecycleScope.launch {
            showWithBidding(activity, position)
        }
    }

    /**
     * 检查是否有可用的全屏原生广告
     */
    fun hasReadyAd(): Boolean {
        return FullScreenNativeBiddingManager.hasAnyReadyAd()
    }
}

/**
 * BiddingWinner to BiddingPlatform 扩展函数
 */
private fun net.corekit.monetize.ads.bidding.BiddingWinner.toBiddingPlatform(): BiddingPlatform {
    return when (this) {
        net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB -> BiddingPlatform.ADMOB
        net.corekit.monetize.ads.bidding.BiddingWinner.PANGLE -> BiddingPlatform.PANGLE
        net.corekit.monetize.ads.bidding.BiddingWinner.TOPON -> BiddingPlatform.TOPON
    }
}
