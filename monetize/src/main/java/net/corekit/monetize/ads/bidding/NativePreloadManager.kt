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
 * 原生广告预加载管理器
 * 
 * 支持 AdMob、Pangle、TopOn 三个平台的原生广告预加载
 */
object NativePreloadManager {

    private const val TAG = "NativePreload"
    private const val PRELOAD_TIMEOUT_MS = 15000L

    private val admobController get() = AdsManager.Controllers.native
    private val pangleController get() = PangleNativeAdController.getInstance()
    private val toponController get() = TopOnNativeAdController.getInstance()

    /**
     * 并行预加载各平台原生广告
     */
    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        val entries = java.util.Collections.synchronizedList(mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry>())
        
        // AdLogger.d("[$TAG] ========== 开始原生广告预加载 ==========")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.NATIVE.toConfigKey())) {
            jobs += async { 
                val startTime = System.currentTimeMillis()
                var status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.FAILURE
                var msg: String? = null
                
                try {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        admobController.loadInAdvance(context)
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.SUCCESS
                        Unit
                    } ?: run {
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.TIMEOUT
                    }
                } catch (e: Exception) {
                    msg = e.message
                } finally {
                    entries.add(net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry(
                        adType = "Native",
                        platform = "AdMob",
                        adUnitId = net.corekit.monetize.BuildConfig.ADMOB_NATIVE_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime,
                        message = msg
                    ))
                }
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.NATIVE.toConfigKey())) {
            jobs += async {
                val startTime = System.currentTimeMillis()
                var status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.FAILURE
                var ecpm: Double? = null
                var msg: String? = null
                
                try {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        val result = pangleController.preloadAd(context)
                        if (result is net.corekit.monetize.ads.AdResult.Success) {
                            status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.SUCCESS
                            ecpm = pangleController.getEcpm()
                        } else if (result is net.corekit.monetize.ads.AdResult.Failure) {
                            msg = result.error.message
                        }
                        Unit
                    } ?: run {
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.TIMEOUT
                    }
                } catch (e: Exception) {
                    msg = e.message
                } finally {
                    entries.add(net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry(
                        adType = "Native",
                        platform = "Pangle",
                        adUnitId = net.corekit.monetize.BuildConfig.PANGLE_NATIVE_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime,
                        ecpm = ecpm,
                        message = msg
                    ))
                }
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.NATIVE.toConfigKey())) {
            jobs += async {
                val startTime = System.currentTimeMillis()
                var status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.FAILURE
                var ecpm: Double? = null
                var msg: String? = null
                
                try {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        val result = toponController.preloadAd(context)
                        if (result is net.corekit.monetize.ads.AdResult.Success) {
                            status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.SUCCESS
                            ecpm = toponController.getEcpm()
                        } else if (result is net.corekit.monetize.ads.AdResult.Failure) {
                            msg = result.error.message
                        }
                        Unit
                    } ?: run {
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.TIMEOUT
                    }
                } catch (e: Exception) {
                    msg = e.message
                } finally {
                    entries.add(net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry(
                        adType = "Native",
                        platform = "TopOn",
                        adUnitId = net.corekit.monetize.BuildConfig.TOPON_NATIVE_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime,
                        ecpm = ecpm,
                        message = msg
                    ))
                }
            }
        }
        
        jobs.awaitAll()
        
        if (entries.isNotEmpty()) {
            net.corekit.monetize.ads.log.BiddingLogger.logPreload(entries)
        }
    }

    /**
     * 执行原生广告竞价
     */
    fun performBidding(
        admobLoadResult: AdResult<*>? = null,
        pangleLoadResult: AdResult<*>? = null,
        toponLoadResult: AdResult<*>? = null,
        pangleAdUnitId: String? = null,
        toponPlacementId: String? = null
    ): BiddingWinner {
        val controller = BiddingPlatformController
        val startTime = System.currentTimeMillis()
        val entries = mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry>()
        
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
        
        // 收集日志条目
        if (admobEnabled) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "AdMob",
                adType = "Native",
                status = if (admobLoadResult is AdResult.Success<*>) 
                        net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = admobEffectiveEcpm,
                frequencyInfo = getFrequencyInfo(BiddingPlatform.ADMOB, BiddingAdType.NATIVE)
            ))
        }
        
        if (pangleEnabled) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "Pangle",
                adType = "Native",
                status = if (pangleLoadResult is AdResult.Success<*>) 
                        net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = pangleEffectiveEcpm,
                frequencyInfo = getFrequencyInfo(BiddingPlatform.PANGLE, BiddingAdType.NATIVE)
            ))
        }
        
        if (toponEnabled) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "TopOn",
                adType = "Native",
                status = if (toponLoadResult is AdResult.Success<*>) 
                        net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = toponEffectiveEcpm,
                frequencyInfo = getFrequencyInfo(BiddingPlatform.TOPON, BiddingAdType.NATIVE)
            ))
        }
        
        // 只在启用的平台中选择胜出者
        val winner = when {
            admobEnabled && admobEffectiveEcpm >= pangleEffectiveEcpm && admobEffectiveEcpm >= toponEffectiveEcpm -> BiddingWinner.ADMOB
            pangleEnabled && pangleEffectiveEcpm >= toponEffectiveEcpm && pangleEffectiveEcpm >= admobEffectiveEcpm -> BiddingWinner.PANGLE
            toponEnabled -> BiddingWinner.TOPON
            admobEnabled -> BiddingWinner.ADMOB
            pangleEnabled -> BiddingWinner.PANGLE
            else -> BiddingWinner.ADMOB
        }
        
        val winnerEcpm = when(winner) {
            BiddingWinner.ADMOB -> admobEffectiveEcpm
            BiddingWinner.PANGLE -> pangleEffectiveEcpm
            BiddingWinner.TOPON -> toponEffectiveEcpm
        }
        
        val biddingTime = System.currentTimeMillis() - startTime
        
        // 确定胜出条目
        val winnerEntry = entries.find { it.platform.equals(winner.name, ignoreCase = true) }
        
        // 使用统一格式输出日志
        net.corekit.monetize.ads.log.BiddingLogger.logSingleLayerBidding(
            scene = "原生",
            entries = entries,
            winner = winnerEntry,
            durationMs = biddingTime
        )
        
        // 上报竞价数据
        val biddingLog = String.format(
            Locale.US,
            "原生竞价结果 -> AdMob: %.8f 美元%s, Pangle: %.8f 美元%s, TopOn: %.8f 美元%s, 胜出: %s",
            admobEffectiveEcpm, if (admobEnabled) "" else "(禁用)",
            pangleEffectiveEcpm, if (pangleEnabled) "" else "(禁用)",
            toponEffectiveEcpm, if (toponEnabled) "" else "(禁用)",
            winner.name
        )
        
        ReportDataManager.reportDataByName(
            reporterName = "ThinkingData",
            eventName = "bidding",
            data = mapOf("log" to biddingLog)
        )
        
        return winner
    }

    private fun getFrequencyInfo(platform: BiddingPlatform, adType: BiddingAdType): net.corekit.monetize.ads.log.BiddingLogger.FrequencyInfo? {
        if (!net.corekit.monetize.ads.config.BiddingConfigManager.isPlatformFrequencyEnabled()) return null
        
        val config = net.corekit.monetize.ads.config.BiddingConfigManager.getPlatformFrequencyConfig(platform, adType.toConfigKey())
            ?: return null
        
        val dailyShow = net.corekit.monetize.ads.frequency.PlatformFrequencyManager.getDailyShowCount(platform, adType)
        return net.corekit.monetize.ads.log.BiddingLogger.FrequencyInfo(
            dailyShow = dailyShow,
            maxDailyShow = config.maxDailyShow
        )
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
