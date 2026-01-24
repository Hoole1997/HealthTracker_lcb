package net.corekit.monetize.ads.bidding

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.RewardedInterstitialAds
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.log.BiddingLogger
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController
import net.corekit.monetize.ads.pangle.PangleRewardedAdController
import net.corekit.monetize.ads.topon.TopOnInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnRewardedAdController

/**
 * 激励广告两层预加载管理器
 * (包含展示时竞价逻辑)
 */
object RewardTwoLayerPreloadManager {

    private const val TAG = "RewardTwoLayerPreload"
    private const val PRELOAD_TIMEOUT_MS = 15000L
    private const val SHOW_TIMEOUT_MS = 60000L

    private val rewardedInterstitialController get() = RewardedInterstitialAds.getInstance()
    private val rewardedController get() = RewardedAds.getInstance()

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        
        // 获取激励场景的竞价模式
        val biddingMode = BiddingConfigManager.getSceneBiddingMode("reward")
        val isTwoLayer = biddingMode == "two_layer"
        val modeCn = if (isTwoLayer) "两层" else "单层"
        
        AdLogger.d("[$TAG] 开始${modeCn}预加载")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        jobs += async { RewardedPreloadManager.preloadAll(context) }
        
        // 预加载时使用 shouldParticipateInPreload（不检查频控）
        if (controller.shouldParticipateInPreload(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    rewardedInterstitialController.loadInAdvance(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (isTwoLayer) {
            jobs += async { InterstitialPreloadManager.preloadAll(context) }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] ${modeCn}预加载完成 (Mode: $biddingMode)")
    }

    /**
     * 定向预加载：根据消耗的广告类型和平台补充缓存
     * 
     * @param consumedAdType 刚被消耗的广告类型
     * @param consumedPlatform 刚被消耗的广告平台（可选，传入时只补充该平台）
     */
    suspend fun preloadByConsumedType(
        context: Context, 
        consumedAdType: BiddingAdType,
        consumedPlatform: BiddingPlatform? = null
    ) = coroutineScope {
        val platformInfo = consumedPlatform?.name ?: "ALL"
        AdLogger.d("[$TAG] 定向预加载 | 消耗类型: %s | 消耗平台: %s", consumedAdType.name, platformInfo)
        
        when (consumedAdType) {
            BiddingAdType.REWARDED -> {
                if (consumedPlatform != null) {
                    // 只补充消耗平台的激励广告缓存
                    RewardedPreloadManager.preloadByPlatform(context, consumedPlatform)
                } else {
                    // 未指定平台，补充所有平台的激励广告缓存
                    RewardedPreloadManager.preloadAll(context)
                }
            }
            BiddingAdType.REWARDED_INTERSTITIAL -> {
                // 消耗了插页激励广告，只补充插页激励广告缓存（仅 AdMob 支持）
                val controller = BiddingPlatformController
                if (controller.shouldParticipateInPreload(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())) {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        rewardedInterstitialController.loadInAdvance(context)
                        Unit
                    }
                }
            }
            BiddingAdType.INTERSTITIAL -> {
                if (consumedPlatform != null) {
                    // 只补充消耗平台的插屏广告缓存
                    InterstitialPreloadManager.preloadByPlatform(context, consumedPlatform)
                } else {
                    // 未指定平台，补充所有平台的插屏广告缓存
                    InterstitialPreloadManager.preloadAll(context)
                }
            }
            else -> {
                // 其他类型，预加载所有
                preloadAll(context)
            }
        }
        
        AdLogger.d("[$TAG] 定向预加载完成 | 类型: %s | 平台: %s", consumedAdType.name, platformInfo)
    }

    suspend fun performTwoLayerBidding(context: Context): FinalBidResult = coroutineScope {
        val controller = BiddingPlatformController
        val startTime = System.currentTimeMillis()
        val platformResults = mutableListOf<PlatformBidResult>()
        
        // A simple way to check the bidding mode
        val biddingMode = BiddingConfigManager.getSceneBiddingMode("reward")
        val isTwoLayer = biddingMode == "two_layer"
        AdLogger.d("[$TAG] 竞价模式: $biddingMode")
        
        // 收集日志条目
        val layer1Entries = mutableListOf<BiddingLogger.BiddingEntry>()
        val layer2Entries = mutableListOf<BiddingLogger.BiddingEntry>()
        
        // 并行执行各层竞价
        val rewardedDeferred = async { collectRewardedBidding(context, controller, layer1Entries) }
        
        // 仅在 Two-Layer 模式下请求插屏
        val interstitialDeferred = if (isTwoLayer) {
            async { collectInterstitialBidding(context, controller, layer2Entries) }
        } else {
            null
        }
        
        // AdMob 插页激励广告（归入第一层激励类）
        val rewardedInterstitialResult = collectRewardedInterstitial(context, controller, layer1Entries)
        
        // 等待异步竞价结果
        val rewardedWinner = rewardedDeferred.await()
        val interstitialWinner = interstitialDeferred?.await()
        
        rewardedWinner?.let { platformResults.add(it) }
        rewardedInterstitialResult?.let { platformResults.add(it) }
        interstitialWinner?.let { platformResults.add(it) }
        
        val biddingTime = System.currentTimeMillis() - startTime
        val finalWinner = platformResults.maxByOrNull { it.ecpm }
        
        // 输出统一格式日志
        val layer1Winner = layer1Entries.filter { it.status == BiddingLogger.EntryStatus.READY }
            .maxByOrNull { it.ecpm }
        val layer2Winner = layer2Entries.filter { it.status == BiddingLogger.EntryStatus.READY }
            .maxByOrNull { it.ecpm }
        val finalEntry = finalWinner?.let {
            BiddingLogger.BiddingEntry(it.platform.name, it.winnerType.name, BiddingLogger.EntryStatus.READY, it.ecpm)
        }
        
        BiddingLogger.logTwoLayerBidding(
            scene = "激励",
            layer1Name = "激励广告",
            layer1Entries = layer1Entries,
            layer1Winner = layer1Winner,
            layer2Name = "插屏广告",
            layer2Entries = layer2Entries,
            layer2Winner = layer2Winner,
            finalWinner = finalEntry,
            durationMs = biddingTime
        )
        
        if (platformResults.isEmpty()) {
            FinalBidResult.failed(biddingTime)
        } else {
            FinalBidResult(
                winner = finalWinner,
                allResults = platformResults,
                biddingTimeMs = biddingTime
            )
        }
    }

    private suspend fun collectRewardedBidding(
        context: Context,
        controller: BiddingPlatformController,
        entries: MutableList<BiddingLogger.BiddingEntry>
    ): PlatformBidResult? {
        var winner: PlatformBidResult? = null
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        // AdMob Rewarded
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = rewardedController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            val hasCache = rewardedController.hasCachedAd()
            val freqInfo = getFrequencyInfo(BiddingPlatform.ADMOB, BiddingAdType.REWARDED)
            
            entries.add(BiddingLogger.BiddingEntry(
                platform = "AdMob",
                adType = "Rewarded",
                status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.ADMOB to ecpm)
        }
        
        // Pangle Rewarded
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = PangleRewardedAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            val hasCache = PangleRewardedAdController.getInstance().hasValidCache()
            val freqInfo = getFrequencyInfo(BiddingPlatform.PANGLE, BiddingAdType.REWARDED)
            
            entries.add(BiddingLogger.BiddingEntry(
                platform = "Pangle",
                adType = "Rewarded",
                status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.PANGLE to ecpm)
        }
        
        // TopOn Rewarded
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = TopOnRewardedAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            val hasCache = TopOnRewardedAdController.getInstance().hasValidCache()
            val freqInfo = getFrequencyInfo(BiddingPlatform.TOPON, BiddingAdType.REWARDED)
            
            entries.add(BiddingLogger.BiddingEntry(
                platform = "TopOn",
                adType = "Rewarded",
                status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.TOPON to ecpm)
        }
        
        if (results.isNotEmpty()) {
            val winnerPair = results.maxByOrNull { it.second }!!
            winner = PlatformBidResult(winnerPair.first, BiddingAdType.REWARDED, winnerPair.second)
        }
        return winner
    }

    private suspend fun collectRewardedInterstitial(
        context: Context,
        controller: BiddingPlatformController,
        entries: MutableList<BiddingLogger.BiddingEntry>
    ): PlatformBidResult? {
        if (!controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())) {
            return null
        }
        
        val rawEcpm = rewardedInterstitialController.getCachedAdPrice(context) ?: 0.0
        val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
        val hasCache = rewardedInterstitialController.hasCachedAd()
        val freqInfo = getFrequencyInfo(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL)
        
        entries.add(BiddingLogger.BiddingEntry(
            platform = "AdMob",
            adType = "RewardedInter",
            status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
            ecpm = ecpm,
            frequencyInfo = freqInfo
        ))
        
        return if (hasCache) {
            PlatformBidResult(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL, ecpm)
        } else null
    }

    private suspend fun collectInterstitialBidding(
        context: Context,
        controller: BiddingPlatformController,
        entries: MutableList<BiddingLogger.BiddingEntry>
    ): PlatformBidResult? {
        var winner: PlatformBidResult? = null
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        // AdMob
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val admobCtrl = AdsManager.Controllers.interstitial
            val rawEcpm = admobCtrl.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            val hasCache = admobCtrl.hasCachedAd()
            val freqInfo = getFrequencyInfo(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL)
            
            entries.add(BiddingLogger.BiddingEntry(
                platform = "AdMob",
                adType = "Interstitial",
                status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.ADMOB to ecpm)
        }
        
        // Pangle
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = PangleInterstitialAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            val hasCache = PangleInterstitialAdController.getInstance().hasValidCache()
            val freqInfo = getFrequencyInfo(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL)
            
            entries.add(BiddingLogger.BiddingEntry(
                platform = "Pangle",
                adType = "Interstitial",
                status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.PANGLE to ecpm)
        }
        
        // TopOn
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = TopOnInterstitialAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            val hasCache = TopOnInterstitialAdController.getInstance().hasValidCache()
            val freqInfo = getFrequencyInfo(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL)
            
            entries.add(BiddingLogger.BiddingEntry(
                platform = "TopOn",
                adType = "Interstitial",
                status = if (hasCache) BiddingLogger.EntryStatus.READY else BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.TOPON to ecpm)
        }
        
        if (results.isNotEmpty()) {
            val winnerPair = results.maxByOrNull { it.second }!!
            winner = PlatformBidResult(winnerPair.first, BiddingAdType.INTERSTITIAL, winnerPair.second)
        }
        return winner
    }

    private fun getFrequencyInfo(platform: BiddingPlatform, adType: BiddingAdType): BiddingLogger.FrequencyInfo? {
        // 如果频控未启用，返回 null
        if (!BiddingConfigManager.isPlatformFrequencyEnabled()) return null
        
        val config = BiddingConfigManager.getPlatformFrequencyConfig(platform, adType.toConfigKey())
            ?: return null
        
        val dailyShow = PlatformFrequencyManager.getDailyShowCount(platform, adType)
        return BiddingLogger.FrequencyInfo(
            dailyShow = dailyShow,
            maxDailyShow = config.maxDailyShow
        )
    }

    suspend fun showWinnerAd(
        activity: Activity,
        result: FinalBidResult,
        onRewardEarned: ((Boolean) -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ): AdResult<Unit> {
        val winner = result.winner ?: return AdResult.Failure(
            AdException(AdException.ERROR_NOT_LOADED, "No winning ad")
        )
        
        AdLogger.d("[$TAG] Show winning ad: %s - %s", winner.platform.name, winner.winnerType.name)
        
        return withTimeoutOrNull(SHOW_TIMEOUT_MS) {
            when (winner.winnerType) {
                BiddingAdType.REWARDED -> showRewardedAd(activity, winner.platform, onRewardEarned, onDismiss)
                BiddingAdType.REWARDED_INTERSTITIAL -> {
                    val result = rewardedInterstitialController.displayAd(activity, "bidding")
                    if (result is AdResult.Success) {
                        onRewardEarned?.invoke(true)
                    }
                    onDismiss?.invoke()
                    
                    when (result) {
                        is AdResult.Success -> AdResult.Success(Unit)
                        is AdResult.Failure -> AdResult.Failure(result.error)
                        else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Unknown show result"))
                    }
                }
                BiddingAdType.INTERSTITIAL -> {
                    onRewardEarned?.invoke(false)
                    showInterstitialAd(activity, winner.platform, onDismiss)
                }
                else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Unsupported ad type"))
            }
        } ?: AdResult.Failure(AdException(AdException.ERROR_TIMEOUT, "Ad show timeout"))
    }

    private suspend fun showRewardedAd(
        activity: Activity,
        platform: BiddingPlatform,
        onRewardEarned: ((Boolean) -> Unit)?,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                val showResult = rewardedController.show(activity, "bidding")
                if (showResult is AdResult.Success) {
                    onRewardEarned?.invoke(showResult.data.rewarded)
                }
                onDismiss?.invoke()
                
                when (showResult) {
                    is AdResult.Success -> AdResult.Success(Unit)
                    is AdResult.Failure -> AdResult.Failure(showResult.error)
                    else -> AdResult.Failure(AdException(AdException.ERROR_INTERNAL, "Unknown show result"))
                }
            }
            BiddingPlatform.PANGLE -> {
                PangleRewardedAdController.getInstance().showAd(activity, onRewardEarned, onDismiss)
            }
            BiddingPlatform.TOPON -> {
                TopOnRewardedAdController.getInstance().showAd(activity, onRewardEarned, onDismiss)
            }
        }
    }

    private suspend fun showInterstitialAd(
        activity: Activity,
        platform: BiddingPlatform,
        onDismiss: (() -> Unit)?
    ): AdResult<Unit> {
        return when (platform) {
            BiddingPlatform.ADMOB -> {
                AdsManager.Controllers.interstitial.displayAd(activity, "bidding")
                onDismiss?.invoke()
                AdResult.Success(Unit)
            }
            BiddingPlatform.PANGLE -> {
                PangleInterstitialAdController.getInstance().showAd(activity, onDismiss)
            }
            BiddingPlatform.TOPON -> {
                TopOnInterstitialAdController.getInstance().showAd(activity, onDismiss)
            }
        }
    }

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (RewardedPreloadManager.hasReadyAd()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED_INTERSTITIAL.toConfigKey())
            && rewardedInterstitialController.hasCachedAd()) return true
        
        if (InterstitialPreloadManager.hasReadyAd()) return true
        
        return false
    }
}
