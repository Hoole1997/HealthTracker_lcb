package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.RewardedAds
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleRewardedAdController
import net.corekit.monetize.ads.topon.TopOnRewardedAdController

/**
 * 激励广告预加载管理器
 */
object RewardedPreloadManager {

    private const val TAG = "RewardedPreload"
    private const val PRELOAD_TIMEOUT_MS = 15000L

    private val admobController get() = RewardedAds.getInstance()

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        val entries = java.util.Collections.synchronizedList(mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry>())
        
        AdLogger.d("[$TAG] 开始并行预加载激励广告")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInPreload(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey())) {
            jobs += async { 
                val startTime = System.currentTimeMillis()
                var status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.FAILURE
                try {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        admobController.load(context)
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.SUCCESS
                        Unit
                    } ?: run {
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.TIMEOUT
                    }
                } catch (e: Exception) {
                    AdLogger.e("[$TAG] AdMob预加载异常: ${e.message}")
                } finally {
                    entries.add(net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry(
                        adType = "Rewarded",
                        platform = "AdMob",
                        adUnitId = net.corekit.monetize.BuildConfig.ADMOB_REWARDED_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime
                    ))
                }
            }
        }
        
        if (controller.shouldParticipateInPreload(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())) {
            jobs += async {
                val startTime = System.currentTimeMillis()
                var status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.FAILURE
                try {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        PangleRewardedAdController.getInstance().preloadAd(context)
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.SUCCESS
                        Unit
                    } ?: run {
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.TIMEOUT
                    }
                } catch (e: Exception) {
                    AdLogger.e("[$TAG] Pangle预加载异常: ${e.message}")
                } finally {
                    entries.add(net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry(
                        adType = "Rewarded",
                        platform = "Pangle",
                        adUnitId = net.corekit.monetize.BuildConfig.PANGLE_REWARDED_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime
                    ))
                }
            }
        }
        
        if (controller.shouldParticipateInPreload(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())) {
            jobs += async {
                val startTime = System.currentTimeMillis()
                var status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.FAILURE
                try {
                    withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                        TopOnRewardedAdController.getInstance().preloadAd(context)
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.SUCCESS
                        Unit
                    } ?: run {
                        status = net.corekit.monetize.ads.log.BiddingLogger.LoadStatus.TIMEOUT
                    }
                } catch (e: Exception) {
                    AdLogger.e("[$TAG] TopOn预加载异常: ${e.message}")
                } finally {
                    entries.add(net.corekit.monetize.ads.log.BiddingLogger.PreloadEntry(
                        adType = "Rewarded",
                        platform = "TopOn",
                        adUnitId = net.corekit.monetize.BuildConfig.TOPON_REWARDED_ID,
                        status = status,
                        durationMs = System.currentTimeMillis() - startTime
                    ))
                }
            }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 激励广告预加载完成")
        
        if (entries.isNotEmpty()) {
            net.corekit.monetize.ads.log.BiddingLogger.logPreload(entries)
        }
    }

    suspend fun performBidding(context: Context): PlatformBidResult? {
        val controller = BiddingPlatformController
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        AdLogger.d("[$TAG] 开始执行激励广告竞价")
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = admobController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            if (admobController.hasCachedAd()) {
                results.add(BiddingPlatform.ADMOB to ecpm)
                AdLogger.d("[$TAG] AdMob eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = PangleRewardedAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            if (PangleRewardedAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.PANGLE to ecpm)
                AdLogger.d("[$TAG] Pangle eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())) {
            val rawEcpm = TopOnRewardedAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            if (TopOnRewardedAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.TOPON to ecpm)
                AdLogger.d("[$TAG] TopOn eCPM: %.6f USD", ecpm)
            }
        }
        
        if (results.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的激励广告参与竞价")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        AdLogger.d("[$TAG] ✅ 竞价胜出: %s, eCPM: %.6f USD", winner.first.name, winner.second)
        
        return PlatformBidResult(
            platform = winner.first,
            winnerType = BiddingAdType.REWARDED,
            ecpm = winner.second
        )
    }

    /**
     * 按平台定向预加载：只加载指定平台的激励广告
     * 
     * @param platform 需要补货的平台
     */
    suspend fun preloadByPlatform(context: Context, platform: BiddingPlatform) = coroutineScope {
        val controller = BiddingPlatformController
        val startTime = System.currentTimeMillis()
        
        AdLogger.d("[$TAG] 定向预加载激励广告 | 平台: %s", platform.name)
        
        val result = when (platform) {
            BiddingPlatform.ADMOB -> {
                if (controller.shouldParticipateInPreload(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey())) {
                    try {
                        withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                            admobController.load(context)
                            "SUCCESS"
                        } ?: "TIMEOUT"
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] AdMob定向预加载异常: ${e.message}")
                        "FAILURE"
                    }
                } else "SKIPPED"
            }
            BiddingPlatform.PANGLE -> {
                if (controller.shouldParticipateInPreload(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())) {
                    try {
                        withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                            PangleRewardedAdController.getInstance().preloadAd(context)
                            "SUCCESS"
                        } ?: "TIMEOUT"
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] Pangle定向预加载异常: ${e.message}")
                        "FAILURE"
                    }
                } else "SKIPPED"
            }
            BiddingPlatform.TOPON -> {
                if (controller.shouldParticipateInPreload(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())) {
                    try {
                        withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                            TopOnRewardedAdController.getInstance().preloadAd(context)
                            "SUCCESS"
                        } ?: "TIMEOUT"
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] TopOn定向预加载异常: ${e.message}")
                        "FAILURE"
                    }
                } else "SKIPPED"
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        AdLogger.d("[$TAG] 定向预加载完成 | 平台: %s | 结果: %s | 耗时: %d ms", platform.name, result, duration)
    }

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.REWARDED.toConfigKey()) 
            && admobController.hasCachedAd()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.REWARDED.toConfigKey())
            && PangleRewardedAdController.getInstance().hasValidCache()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.REWARDED.toConfigKey())
            && TopOnRewardedAdController.getInstance().hasValidCache()) {
            return true
        }
        
        return false
    }
}
