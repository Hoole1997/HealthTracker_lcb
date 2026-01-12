package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleNativeAdController
import net.corekit.monetize.ads.topon.TopOnNativeAdController
import net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil
import java.util.Locale

/**
 * 原生广告竞价管理器
 * 
 * 支持 AdMob、Pangle、TopOn 三个平台的原生广告竞价
 */
object NativeBiddingManager {

    private const val TAG = "NativeBidding"
    private const val PRELOAD_TIMEOUT_MS = 15000L

    private val admobController get() = AdsManager.Controllers.native
    private val pangleController get() = PangleNativeAdController.getInstance()
    private val toponController get() = TopOnNativeAdController.getInstance()

    /**
     * 并行预加载各平台原生广告
     */
    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        
        AdLogger.d("[$TAG] ========== 开始原生广告预加载 ==========")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey())) {
            jobs += async { 
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    admobController.loadInAdvance(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    pangleController.preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    toponController.preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 原生广告预加载完成")
    }

    /**
     * 执行原生广告竞价
     * 
     * @param admobLoadResult AdMob 加载结果
     * @param pangleLoadResult Pangle 加载结果
     * @param toponLoadResult TopOn 加载结果
     * @param pangleAdUnitId Pangle 广告位 ID
     * @param toponPlacementId TopOn 广告位 ID
     * @return 竞价胜出的平台
     */
    fun performBidding(
        admobLoadResult: AdResult<*>? = null,
        pangleLoadResult: AdResult<*>? = null,
        toponLoadResult: AdResult<*>? = null,
        pangleAdUnitId: String? = null,
        toponPlacementId: String? = null
    ): BiddingWinner {
        val controller = BiddingPlatformController
        
        // 获取各平台的启用状态
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey())
        
        // 获取 AdMob 收益
        val admobValueUsd = if (admobEnabled && admobLoadResult is AdResult.Success<*>) {
            admobController.retrieveCurrentAd()?.let { ad ->
                AdmobNextGenReflectionUtil.getRevenueByPath(ad)?.valueMicros?.toDouble()?.div(1_000_000.0)
            } ?: 0.0
        } else 0.0
        
        // 获取 Pangle 收益
        val pangleValueUsd = if (pangleEnabled && pangleLoadResult is AdResult.Success<*>) {
            pangleController.getEcpm()
        } else 0.0
        
        // 获取 TopOn 收益
        val toponValueUsd = if (toponEnabled && toponLoadResult is AdResult.Success<*>) {
            toponController.getEcpm()
        } else 0.0
        
        // 应用测试模式的 mock eCPM
        val admobEffectiveEcpm = if (admobEnabled) controller.getEffectiveEcpm(BiddingPlatform.ADMOB, admobValueUsd) else 0.0
        val pangleEffectiveEcpm = if (pangleEnabled) controller.getEffectiveEcpm(BiddingPlatform.PANGLE, pangleValueUsd) else 0.0
        val toponEffectiveEcpm = if (toponEnabled) controller.getEffectiveEcpm(BiddingPlatform.TOPON, toponValueUsd) else 0.0
        
        // 只在启用的平台中选择胜出者
        val winner = when {
            admobEnabled && admobEffectiveEcpm >= pangleEffectiveEcpm && admobEffectiveEcpm >= toponEffectiveEcpm -> BiddingWinner.ADMOB
            pangleEnabled && pangleEffectiveEcpm >= toponEffectiveEcpm && pangleEffectiveEcpm >= admobEffectiveEcpm -> BiddingWinner.PANGLE
            toponEnabled -> BiddingWinner.TOPON
            admobEnabled -> BiddingWinner.ADMOB
            pangleEnabled -> BiddingWinner.PANGLE
            else -> BiddingWinner.ADMOB // 默认
        }
        
        val winnerEcpm = when(winner) {
            BiddingWinner.ADMOB -> admobEffectiveEcpm
            BiddingWinner.PANGLE -> pangleEffectiveEcpm
            BiddingWinner.TOPON -> toponEffectiveEcpm
        }
        
        // 生成格式化的竞价日志
        AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 原生广告竞价")
        AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 平台状态:")
        AdLogger.d("[$TAG] ║   • AdMob:   %s", if (admobEnabled) "✅ 启用" else "❌ 禁用")
        AdLogger.d("[$TAG] ║   • Pangle:  %s", if (pangleEnabled) "✅ 启用" else "❌ 禁用")
        AdLogger.d("[$TAG] ║   • TopOn:   %s", if (toponEnabled) "✅ 启用" else "❌ 禁用")
        AdLogger.d("[$TAG] ╟──────────────────────────────────────────────────────────────")
        AdLogger.d("[$TAG] ║ eCPM 报价:")
        AdLogger.d("[$TAG] ║   • AdMob:   %.8f 美元", admobEffectiveEcpm)
        AdLogger.d("[$TAG] ║   • Pangle:  %.8f 美元", pangleEffectiveEcpm)
        AdLogger.d("[$TAG] ║   • TopOn:   %.8f 美元", toponEffectiveEcpm)
        AdLogger.d("[$TAG] ╟──────────────────────────────────────────────────────────────")
        AdLogger.d("[$TAG] ║ ✅ 竞价胜出: %s (eCPM: %.8f 美元)", winner.name, winnerEcpm)
        AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
        
        // 生成单行日志用于上报
        val biddingLog = String.format(
            Locale.US,
            "原生竞价结果 -> AdMob: %.8f 美元%s, Pangle: %.8f 美元%s, TopOn: %.8f 美元%s, 胜出: %s",
            admobEffectiveEcpm, if (admobEnabled) "" else "(禁用)",
            pangleEffectiveEcpm, if (pangleEnabled) "" else "(禁用)",
            toponEffectiveEcpm, if (toponEnabled) "" else "(禁用)",
            winner.name
        )
        
        // 上报竞价数据
        ReportDataManager.reportDataByName(
            reporterName = "ThinkingData",
            eventName = "bidding",
            data = mapOf("log" to biddingLog)
        )
        
        return winner
    }

    /**
     * 简化版竞价（用于已缓存广告的快速竞价）
     */
    suspend fun performBiddingFromCache(context: Context): PlatformBidResult? {
        val controller = BiddingPlatformController
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey())
        
        // AdMob
        if (admobEnabled && admobController.checkAdReady()) {
            val rawEcpm = admobController.retrieveCurrentAd()?.let { ad ->
                AdmobNextGenReflectionUtil.getRevenueByPath(ad)?.valueMicros?.toDouble()?.div(1_000_000.0)
            } ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            results.add(BiddingPlatform.ADMOB to ecpm)
        }
        
        // Pangle
        if (pangleEnabled && pangleController.hasValidCache()) {
            val rawEcpm = pangleController.getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            results.add(BiddingPlatform.PANGLE to ecpm)
        }
        
        // TopOn
        if (toponEnabled && toponController.hasValidCache()) {
            val rawEcpm = toponController.getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            results.add(BiddingPlatform.TOPON to ecpm)
        }
        
        // 生成格式化的竞价日志
        val admobEcpm = results.find { it.first == BiddingPlatform.ADMOB }?.second ?: 0.0
        val pangleEcpm = results.find { it.first == BiddingPlatform.PANGLE }?.second ?: 0.0
        val toponEcpm = results.find { it.first == BiddingPlatform.TOPON }?.second ?: 0.0
        
        if (results.isEmpty()) {
            AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
            AdLogger.d("[$TAG] ║ 原生广告竞价")
            AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
            AdLogger.d("[$TAG] ║ ❌ 没有可用的原生广告参与竞价")
            AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        
        AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 原生广告竞价")
        AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 平台状态:")
        AdLogger.d("[$TAG] ║   • AdMob:   %s", if (admobEnabled) "✅ 启用" else "❌ 禁用")
        AdLogger.d("[$TAG] ║   • Pangle:  %s", if (pangleEnabled) "✅ 启用" else "❌ 禁用")
        AdLogger.d("[$TAG] ║   • TopOn:   %s", if (toponEnabled) "✅ 启用" else "❌ 禁用")
        AdLogger.d("[$TAG] ╟──────────────────────────────────────────────────────────────")
        AdLogger.d("[$TAG] ║ eCPM 报价:")
        AdLogger.d("[$TAG] ║   • AdMob:   %.8f 美元", admobEcpm)
        AdLogger.d("[$TAG] ║   • Pangle:  %.8f 美元", pangleEcpm)
        AdLogger.d("[$TAG] ║   • TopOn:   %.8f 美元", toponEcpm)
        AdLogger.d("[$TAG] ╟──────────────────────────────────────────────────────────────")
        AdLogger.d("[$TAG] ║ ✅ 竞价胜出: %s (eCPM: %.8f 美元)", winner.first.name, winner.second)
        AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
        
        // 生成单行日志用于上报
        val biddingLog = String.format(
            Locale.US,
            "原生竞价结果 -> AdMob: %.8f 美元%s, Pangle: %.8f 美元%s, TopOn: %.8f 美元%s, 胜出: %s",
            admobEcpm, if (admobEnabled) "" else "(禁用)",
            pangleEcpm, if (pangleEnabled) "" else "(禁用)",
            toponEcpm, if (toponEnabled) "" else "(禁用)",
            winner.first.name
        )
        
        // 上报竞价数据
        ReportDataManager.reportDataByName(
            reporterName = "ThinkingData",
            eventName = "bidding",
            data = mapOf("log" to biddingLog)
        )
        
        return PlatformBidResult(
            platform = winner.first,
            winnerType = BiddingAdType.NATIVE,
            ecpm = winner.second
        )
    }

    /**
     * 检查是否有任何平台有可用的原生广告
     */
    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey()) 
            && admobController.checkAdReady()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())
            && pangleController.hasValidCache()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey())
            && toponController.hasValidCache()) return true
        
        return false
    }
}
