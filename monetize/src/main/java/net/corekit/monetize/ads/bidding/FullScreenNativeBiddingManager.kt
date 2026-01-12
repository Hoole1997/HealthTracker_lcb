package net.corekit.monetize.ads.bidding

import android.content.Context
import kotlinx.coroutines.*
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdController
import net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdController

/**
 * 全屏原生广告竞价管理器
 * 
 * 注意：AdMob 全屏原生广告当前不支持精确 eCPM 获取，使用默认值参与竞价
 */
object FullScreenNativeBiddingManager {

    private const val TAG = "FullNativeBidding"
    private const val PRELOAD_TIMEOUT_MS = 15000L
    private const val DEFAULT_ADMOB_ECPM = 1.0

    private val admobController get() = AdsManager.Controllers.fullScreenNative

    suspend fun preloadAll(context: Context) = coroutineScope {
        val controller = BiddingPlatformController
        
        AdLogger.d("[$TAG] 开始并行预加载全屏原生广告")
        
        val jobs = mutableListOf<Deferred<Unit>>()
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            jobs += async { 
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    admobController.loadInAdvance(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    PangleFullScreenNativeAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            jobs += async {
                withTimeoutOrNull(PRELOAD_TIMEOUT_MS) {
                    TopOnFullScreenNativeAdController.getInstance().preloadAd(context)
                    Unit
                } ?: Unit
            }
        }
        
        jobs.awaitAll()
        AdLogger.d("[$TAG] 全屏原生广告预加载完成")
    }

    suspend fun performBidding(context: Context): PlatformBidResult? {
        val controller = BiddingPlatformController
        val results = mutableListOf<Pair<BiddingPlatform, Double>>()
        
        AdLogger.d("[$TAG] 开始执行全屏原生广告竞价")
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            if (admobController.checkCachedAdAvailable()) {
                results.add(BiddingPlatform.ADMOB to DEFAULT_ADMOB_ECPM)
                AdLogger.d("[$TAG] AdMob 全屏原生广告可用 (使用默认 eCPM: %.2f USD)", DEFAULT_ADMOB_ECPM)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            val rawEcpm = PangleFullScreenNativeAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.PANGLE, rawEcpm)
            if (PangleFullScreenNativeAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.PANGLE to ecpm)
                AdLogger.d("[$TAG] Pangle eCPM: %.6f USD", ecpm)
            }
        }
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.FULL_NATIVE.toConfigKey())) {
            val rawEcpm = TopOnFullScreenNativeAdController.getInstance().getEcpm()
            val ecpm = controller.getEffectiveEcpm(BiddingPlatform.TOPON, rawEcpm)
            if (TopOnFullScreenNativeAdController.getInstance().hasValidCache()) {
                results.add(BiddingPlatform.TOPON to ecpm)
                AdLogger.d("[$TAG] TopOn eCPM: %.6f USD", ecpm)
            }
        }
        
        if (results.isEmpty()) {
            AdLogger.w("[$TAG] 没有可用的全屏原生广告参与竞价")
            return null
        }
        
        val winner = results.maxByOrNull { it.second }!!
        AdLogger.d("[$TAG] ✅ 竞价胜出: %s, eCPM: %.6f USD", winner.first.name, winner.second)
        
        return PlatformBidResult(
            platform = winner.first,
            winnerType = BiddingAdType.FULL_NATIVE,
            ecpm = winner.second
        )
    }

    fun hasReadyAd(): Boolean {
        val controller = BiddingPlatformController
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE.toConfigKey()) 
            && admobController.checkCachedAdAvailable()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.PANGLE, BiddingAdType.FULL_NATIVE.toConfigKey())
            && PangleFullScreenNativeAdController.getInstance().hasValidCache()) return true
        
        if (controller.shouldParticipateInBidding(BiddingPlatform.TOPON, BiddingAdType.FULL_NATIVE.toConfigKey())
            && TopOnFullScreenNativeAdController.getInstance().hasValidCache()) return true
        
        return false
    }
}
