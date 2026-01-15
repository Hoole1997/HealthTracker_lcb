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
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        AdLogger.d("[$TAG] 开始执行插页广告竞价 (非阻塞模式)")

        // 优化：竞价只针对当前已 Ready 的广告，消除挂起等待时间
        // 加载逻辑应由 preloadAll 在前序异步完成
        
        // AdMob
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = admobController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            if (admobController.hasCachedAd()) {
                results.add(BiddingPlatform.ADMOB to ecpm)
                AdLogger.d("[$TAG] AdMob eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = PangleInterstitialAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            if (PangleInterstitialAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.PANGLE to ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL.toConfigKey())) {
            val rawEcpm = TopOnInterstitialAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            if (TopOnInterstitialAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.TOPON to ecpm)
            }
        }
        
        // 生成格式化日志
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.INTERSTITIAL.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.INTERSTITIAL.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.INTERSTITIAL.toConfigKey())
        
        val admobEcpm = results.find { it.first == BiddingPlatform.ADMOB }?.second ?: 0.0
        val pangleEcpm = results.find { it.first == BiddingPlatform.PANGLE }?.second ?: 0.0
        val toponEcpm = results.find { it.first == BiddingPlatform.TOPON }?.second ?: 0.0
        
        if (results.isEmpty()) {
            AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
            AdLogger.d("[$TAG] ║ 插页广告竞价")
            AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
            AdLogger.d("[$TAG] ║ ❌ 没有可用的插页广告参与竞价")
            AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        
        AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 插页广告竞价")
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
        
        return PlatformBidResult(
            platform = winner.first,
            winnerType = BiddingAdType.INTERSTITIAL,
            ecpm = winner.second
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
