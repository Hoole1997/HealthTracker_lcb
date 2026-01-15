package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController
import net.corekit.monetize.ads.topon.TopOnInterstitialAdController

/**
 * 插页广告竞价管理器
 */
object InterstitialBiddingManager {

    private const val TAG = "InterstitialBidding"
    private const val PRELOAD_TIMEOUT_MS = 15000L

    private val admobController get() = AdsManager.Controllers.interstitial

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        
        AdLogger.d("[$TAG] 开始并行预加载插页广告")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            jobs += async { 
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    admobController.loadInAdvance(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    PangleInterstitialAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    TopOnInterstitialAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 插页广告预加载完成")
    }

    suspend fun performBidding(context: Context, timeoutMillis: Long): PlatformBidResult? {
        val controller = BiddingPlatformController
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        val entries = mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry>()
        
        // 收集各平台竞价结果
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = admobController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            val hasCache = admobController.hasCachedAd()
            val freqInfo = getFrequencyInfo(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL)
            
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "AdMob",
                adType = "Interstitial",
                status = if (hasCache) net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.ADMOB to ecpm)
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = PangleInterstitialAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            val hasCache = PangleInterstitialAdController.getInstance().hasValidCache()
            val freqInfo = getFrequencyInfo(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL)
            
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "Pangle",
                adType = "Interstitial",
                status = if (hasCache) net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.PANGLE to ecpm)
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = TopOnInterstitialAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            val hasCache = TopOnInterstitialAdController.getInstance().hasValidCache()
            val freqInfo = getFrequencyInfo(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL)
            
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.BiddingEntry(
                platform = "TopOn",
                adType = "Interstitial",
                status = if (hasCache) net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.READY 
                        else net.corekit.monetize.ads.log.BiddingLogger.EntryStatus.NO_CACHE,
                ecpm = ecpm,
                frequencyInfo = freqInfo
            ))
            if (hasCache) results.add(BiddingPlatform.TOPON to ecpm)
        }
        
        val biddingTime = System.currentTimeMillis() - startTime
        
        // 确定胜出者
        val winner = results.maxByOrNull { it.second }
        val winnerEntry = winner?.let { w ->
            entries.find { it.platform.equals(w.first.name, ignoreCase = true) }
        }
        
        // 使用统一格式输出日志
        net.corekit.monetize.ads.log.BiddingLogger.logSingleLayerBidding(
            scene = "插页",
            entries = entries,
            winner = winnerEntry,
            durationMs = biddingTime
        )
        
        return winner?.let {
            PlatformBidResult(
                platform = it.first,
                winnerType = BiddingAdType.INTERSTITIAL,
                ecpm = it.second
            )
        }
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

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL.toConfigKey()) 
            && admobController.hasCachedAd()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL.toConfigKey())
            && PangleInterstitialAdController.getInstance().hasValidCache()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL.toConfigKey())
            && TopOnInterstitialAdController.getInstance().hasValidCache()) {
            return true
        }
        
        return false
    }
}
