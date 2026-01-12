package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleAppOpenAdController
import net.corekit.monetize.ads.topon.TopOnSplashAdController

/**
 * 开屏/App Open 广告竞价管理器
 */
object AppOpenBiddingManager {

    private const val TAG = "AppOpenBidding"
    private const val PRELOAD_TIMEOUT_MS = 15000L

    private val admobController get() = AdsManager.Controllers.appOpen

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        
        AdLogger.d("[$TAG] 开始并行预加载开屏广告")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.SPLASH.toConfigKey())) {
            jobs += async { 
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    admobController.loadInAdvance(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.SPLASH.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    PangleAppOpenAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.SPLASH.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    TopOnSplashAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 开屏广告预加载完成")
    }

    suspend fun performBidding(context: Context, timeoutMillis: Long): PlatformBidResult? {
        val controller = BiddingPlatformController
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        AdLogger.d("[$TAG] 开始执行开屏广告竞价, 超时: ${timeoutMillis}ms")

        // 并行等待各平台加载结果
        coroutineScope {
            val jobs = mutableListOf<Deferred<Unit>>()

            // AdMob
            if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.SPLASH.toConfigKey())) {
                jobs += async {
                    AdLogger.d("[$TAG] 等待 AdMob 加载...")
                    admobController.waitForAd(timeoutMillis) // AdMob 内部已实现 waitForAd
                    Unit
                }
            }

            // Pangle
            if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.SPLASH.toConfigKey())) {
                jobs += async {
                    AdLogger.d("[$TAG] 等待 Pangle 加载...")
                    PangleAppOpenAdController.getInstance().waitForAd(timeoutMillis)
                    Unit
                }
            }

            // TopOn
            if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.SPLASH.toConfigKey())) {
                jobs += async {
                    AdLogger.d("[$TAG] 等待 TopOn 加载...")
                    TopOnSplashAdController.getInstance().waitForAd(timeoutMillis)
                    Unit
                }
            }
            
            // 等待所有任务完成或整体超时（虽然各 wait 内部有超时，这里做兜底）
            try {
                withTimeoutOrNull(timeoutMillis + 500) { // 稍微多给一点缓冲
                    jobs.awaitAll()
                }
            } catch (e: Exception) {
                AdLogger.w("[$TAG] 竞价等待超时或中断")
            }
        }
        
        // 收集结果
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.SPLASH.toConfigKey())) {
            val rawEcpm = admobController.getCachedAdPrice(context) ?: 0.0
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.ADMOB, rawEcpm)
            if (admobController.hasCachedAd()) {
                results.add(BiddingPlatform.ADMOB to ecpm)
                AdLogger.d("[$TAG] AdMob eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.SPLASH.toConfigKey())) {
            val rawEcpm = PangleAppOpenAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            if (PangleAppOpenAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.PANGLE to ecpm)
                AdLogger.d("[$TAG] Pangle eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.SPLASH.toConfigKey())) {
            val rawEcpm = TopOnSplashAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            if (TopOnSplashAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.TOPON to ecpm)
            }
        }
        
        // 生成格式化日志
        val admobEnabled = controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.SPLASH.toConfigKey())
        val pangleEnabled = controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.SPLASH.toConfigKey())
        val toponEnabled = controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.SPLASH.toConfigKey())
        
        val admobEcpm = results.find { it.first == BiddingPlatform.ADMOB }?.second ?: 0.0
        val pangleEcpm = results.find { it.first == BiddingPlatform.PANGLE }?.second ?: 0.0
        val toponEcpm = results.find { it.first == BiddingPlatform.TOPON }?.second ?: 0.0
        
        if (results.isEmpty()) {
            AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
            AdLogger.d("[$TAG] ║ 开屏广告竞价")
            AdLogger.d("[$TAG] ╠══════════════════════════════════════════════════════════════")
            AdLogger.d("[$TAG] ║ ❌ 没有可用的开屏广告参与竞价")
            AdLogger.d("[$TAG] ╚══════════════════════════════════════════════════════════════")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        
        AdLogger.d("[$TAG] ╔══════════════════════════════════════════════════════════════")
        AdLogger.d("[$TAG] ║ 开屏广告竞价")
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
            winnerType = BiddingAdType.SPLASH,
            ecpm = winner.second
        )
    }

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.SPLASH.toConfigKey()) 
            && admobController.hasCachedAd()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.SPLASH.toConfigKey())
            && PangleAppOpenAdController.getInstance().hasValidCache()) {
            return true
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.SPLASH.toConfigKey())
            && TopOnSplashAdController.getInstance().hasValidCache()) {
            return true
        }
        
        return false
    }
}
