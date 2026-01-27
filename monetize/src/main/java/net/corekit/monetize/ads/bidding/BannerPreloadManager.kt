package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleBannerAdController
import net.corekit.monetize.ads.topon.TopOnBannerAdController
import java.util.Locale

/**
 * Banner 广告预加载管理器
 * 
 * 支持 AdMob、Pangle、TopOn 三个平台的 Banner 广告预加载
 */
object BannerPreloadManager {

    private const val TAG = "BannerPreload"
    private const val PRELOAD_TIMEOUT_MS = 15000L
    private const val DEFAULT_ADMOB_ECPM = 0.0 // AdMob Banner 使用默认较低值

    private val admobController get() = AdsManager.Controllers.banner
    private val pangleController get() = PangleBannerAdController.getInstance()
    private val toponController get() = TopOnBannerAdController.getInstance()

    /**
     * 并行预加载各平台 Banner 广告
     */
    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        val entries = java.util.Collections.synchronizedList(mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry>())
        
        // AdLogger.d("[$TAG] ========== 开始 Banner 广告预加载 ==========")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.BANNER.toConfigKey())) {
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
                        adType = "Banner",
                        platform = "AdMob",
                        adUnitId = net.corekit.monetize.BuildConfig.ADMOB_BANNER_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime,
                        message = msg
                    ))
                }
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.BANNER.toConfigKey())) {
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
                        adType = "Banner",
                        platform = "Pangle",
                        adUnitId = net.corekit.monetize.BuildConfig.PANGLE_BANNER_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime,
                        ecpm = ecpm,
                        message = msg
                    ))
                }
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.BANNER.toConfigKey())) {
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
                        adType = "Banner",
                        platform = "TopOn",
                        adUnitId = net.corekit.monetize.BuildConfig.TOPON_BANNER_ID,
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
     * 执行 Banner 广告竞价
     */
    suspend fun performBidding(
        context: android.content.Context,
        admobLoadResult: AdResult<*>? = null,
        pangleLoadResult: AdResult<*>? = null,
        toponLoadResult: AdResult<*>? = null
    ): BiddingWinner {
        val controller = BiddingPlatformController
        val startTime = System.currentTimeMillis()
        val entries = mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry>()
        
        // 获取各平台的启用状态
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.BANNER.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.BANNER.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.BANNER.toConfigKey())
        
        // 获取各平台原始收益（通过反射获取 AdMob 真实 eCPM）
        val admobRawEcpm = if (admobEnabled && admobLoadResult is AdResult.Success<*>) {
            admobController.getCachedAdPrice(context) ?: DEFAULT_ADMOB_ECPM
        } else 0.0
        
        val pangleRawEcpm = if (pangleEnabled && pangleLoadResult is AdResult.Success<*>) {
            pangleController.getEcpm()
        } else 0.0
        
        val toponRawEcpm = if (toponEnabled && toponLoadResult is AdResult.Success<*>) {
            toponController.getEcpm()
        } else 0.0
        
        // 应用测试模式的 mock eCPM
        val admobEffectiveEcpm = if (admobEnabled) controller.getEffectiveEcpm(BiddingPlatform.ADMOB, admobRawEcpm) else 0.0
        val pangleEffectiveEcpm = if (pangleEnabled) controller.getEffectiveEcpm(BiddingPlatform.PANGLE, pangleRawEcpm) else 0.0
        val toponEffectiveEcpm = if (toponEnabled) controller.getEffectiveEcpm(BiddingPlatform.TOPON, toponRawEcpm) else 0.0
        
        // 收集日志条目
        if (admobEnabled) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "AdMob",
                adType = "Banner",
                status = if (admobLoadResult is AdResult.Success<*>) 
                        net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = admobEffectiveEcpm,
                frequencyInfo = getFrequencyInfo(BiddingPlatform.ADMOB, BiddingAdType.BANNER)
            ))
        }
        
        if (pangleEnabled) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "Pangle",
                adType = "Banner",
                status = if (pangleLoadResult is AdResult.Success<*>) 
                        net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = pangleEffectiveEcpm,
                frequencyInfo = getFrequencyInfo(BiddingPlatform.PANGLE, BiddingAdType.BANNER)
            ))
        }
        
        if (toponEnabled) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "TopOn",
                adType = "Banner",
                status = if (toponLoadResult is AdResult.Success<*>) 
                        net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = toponEffectiveEcpm,
                frequencyInfo = getFrequencyInfo(BiddingPlatform.TOPON, BiddingAdType.BANNER)
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
            scene = "Banner",
            entries = entries,
            winner = winnerEntry,
            durationMs = biddingTime
        )
        
        // 上报竞价数据
        val biddingLog = String.format(
            Locale.US,
            "Banner竞价结果 -> AdMob: %.8f 美元%s, Pangle: %.8f 美元%s, TopOn: %.8f 美元%s, 胜出: %s",
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
        
        AdLogger.d("[$TAG] ========== 开始 Banner 缓存广告竞价 ==========")
        
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.BANNER.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.BANNER.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.BANNER.toConfigKey())
        
        // AdMob
        if (admobEnabled) {
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, DEFAULT_ADMOB_ECPM)
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
        
        val biddingLog = String.format(
            Locale.US,
            "Banner竞价结果 -> AdMob: %.8f 美元%s, Pangle: %.8f 美元%s, TopOn: %.8f 美元%s",
            admobEcpm, if (admobEnabled) "" else "(禁用)",
            pangleEcpm, if (pangleEnabled) "" else "(禁用)",
            toponEcpm, if (toponEnabled) "" else "(禁用)"
        )
        AdLogger.d(biddingLog)
        
        // 上报竞价数据
        ReportDataManager.reportDataByName(
            reporterName = "ThinkingData",
            eventName = "bidding",
            data = mapOf("log" to biddingLog)
        )
        
        if (results.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的 Banner 广告参与竞价")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        AdLogger.d("[$TAG] ✅ 竞价胜出: %s (eCPM: %.8f 美元)", winner.first.name, winner.second)
        AdLogger.d("[$TAG] ==========================================")
        
        return PlatformBidResult(
            platform = winner.first,
            winnerType = BiddingAdType.BANNER,
            ecpm = winner.second
        )
    }

    /**
     * 检查是否有任何平台有可用的 Banner 广告
     */
    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.BANNER.toConfigKey())) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.BANNER.toConfigKey())
            && pangleController.hasValidCache()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.BANNER.toConfigKey())
            && toponController.hasValidCache()) return true
        
        return false
    }
}
