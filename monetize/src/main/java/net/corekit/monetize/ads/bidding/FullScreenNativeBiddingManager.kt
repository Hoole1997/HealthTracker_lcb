package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdController
import net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdController

/**
 * 全屏原生广告竞价管理器
 * 
 * 统一竞价入口，负责：
 * 1. 并行预加载各平台广告
 * 2. 获取各平台 eCPM 进行比价
 * 3. 选择胜出平台
 * 4. 上报竞价日志
 */
object FullScreenNativeBiddingManager {

    private const val TAG = "FullNaBidding"
    private const val BIDDING_TIMEOUT_MS = 8000L

    private val admobController get() = AdsManager.Controllers.fullScreenNative

    /**
     * 执行全屏原生广告竞价
     * 
     * @param context 上下文
     * @return 竞价胜出平台
     */
    suspend fun bidding(context: Context): BiddingWinner {
        val startTime = System.currentTimeMillis()
        AdLogger.d("[$TAG] 开始全屏原生广告竞价...")

        // 1. 并行预加载（带超时保护）
        withTimeoutOrNull(BIDDING_TIMEOUT_MS) {
            FullScreenNativePreloadManager.preloadAll(context)
        }

        // 2. 执行竞价获取结果
        val bidResult = FullScreenNativePreloadManager.performBidding(context)
        
        val duration = System.currentTimeMillis() - startTime

        // 3. 获取各平台 eCPM 用于日志（应用 Mock eCPM）
        val admobEcpm = if (admobController.checkCachedAdAvailable()) {
            val rawEcpm = admobController.getCachedAdPrice(context) ?: 0.0
            BiddingPlatformController.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
        } else 0.0
        val pangleEcpm = if (PangleFullScreenNativeAdController.getInstance().hasValidCache()) {
            val rawEcpm = PangleFullScreenNativeAdController.getInstance().getEcpm()
            BiddingPlatformController.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
        } else 0.0
        val toponEcpm = if (TopOnFullScreenNativeAdController.getInstance().hasValidCache()) {
            val rawEcpm = TopOnFullScreenNativeAdController.getInstance().getEcpm()
            BiddingPlatformController.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
        } else 0.0

        // 4. 确定胜出者
        val winner = bidResult?.platform?.toBiddingWinner() ?: getDefaultWinner()

        // 5. 上报竞价日志
        reportBiddingResult(
            winner = winner,
            admobEcpm = admobEcpm,
            pangleEcpm = pangleEcpm,
            toponEcpm = toponEcpm,
            winnerEcpm = bidResult?.ecpm ?: 0.0,
            duration = duration
        )

        AdLogger.d("[$TAG] ✅ 竞价完成: 胜出=%s, eCPM=%.6f, 耗时=%dms", 
            winner.name, bidResult?.ecpm ?: 0.0, duration)

        return winner
    }

    /**
     * 获取按 eCPM 排序的平台列表（用于失败回退）
     */
    suspend fun getSortedPlatforms(context: Context): List<BiddingPlatform> {
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        val controller = BiddingPlatformController

        // 获取各平台 eCPM
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            if (admobController.checkCachedAdAvailable()) {
                val ecpm = admobController.getCachedAdPrice(context) ?: 0.0
                results.add(BiddingPlatform.ADMOB to ecpm)
            }
        }

        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            if (PangleFullScreenNativeAdController.getInstance().hasValidCache()) {
                val ecpm = PangleFullScreenNativeAdController.getInstance().getEcpm()
                results.add(BiddingPlatform.PANGLE to ecpm)
            }
        }

        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            if (TopOnFullScreenNativeAdController.getInstance().hasValidCache()) {
                val ecpm = TopOnFullScreenNativeAdController.getInstance().getEcpm()
                results.add(BiddingPlatform.TOPON to ecpm)
            }
        }

        // 按 eCPM 降序排序
        return results.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * 检查是否有任何平台有可用广告
     */
    fun hasAnyReadyAd(): Boolean {
        return FullScreenNativePreloadManager.hasReadyAd()
    }

    /**
     * 获取默认胜出平台（无可用广告时的回退）
     */
    private fun getDefaultWinner(): BiddingWinner {
        val controller = BiddingPlatformController
        return when {
            controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE.toConfigKey()) 
                && admobController.checkCachedAdAvailable() -> BiddingWinner.ADMOB
            controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.FULL_NATIVE.toConfigKey()) 
                && PangleFullScreenNativeAdController.getInstance().hasValidCache() -> BiddingWinner.PANGLE
            controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.FULL_NATIVE.toConfigKey()) 
                && TopOnFullScreenNativeAdController.getInstance().hasValidCache() -> BiddingWinner.TOPON
            else -> BiddingWinner.ADMOB // 最终兜底
        }
    }

    /**
     * 上报竞价结果
     */
    private fun reportBiddingResult(
        winner: BiddingWinner,
        admobEcpm: Double,
        pangleEcpm: Double,
        toponEcpm: Double,
        winnerEcpm: Double,
        duration: Long
    ) {
        val data = mapOf(
            "ad_type" to "FullNative",
            "winner" to winner.name,
            "winner_ecpm" to winnerEcpm,
            "admob_ecpm" to admobEcpm,
            "pangle_ecpm" to pangleEcpm,
            "topon_ecpm" to toponEcpm,
            "duration_ms" to duration
        )
        ReportDataManager.reportData("ad_bidding", data)
        AdLogger.d("[$TAG] 竞价日志已上报: %s", data)
    }
}

/**
 * BiddingPlatform to BiddingWinner conversion
 */
fun BiddingPlatform.toBiddingWinner(): BiddingWinner = when (this) {
    BiddingPlatform.ADMOB -> BiddingWinner.ADMOB
    BiddingPlatform.PANGLE -> BiddingWinner.PANGLE
    BiddingPlatform.TOPON -> BiddingWinner.TOPON
}
