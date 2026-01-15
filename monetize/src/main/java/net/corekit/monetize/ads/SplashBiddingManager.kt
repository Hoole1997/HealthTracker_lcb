package net.corekit.monetize.ads

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.monetize.BuildConfig
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.SplashTwoLayerPreloadManager
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import java.util.Locale

/**
 * 开屏广告竞价管理器
 * 
 * 实现开屏竞价逻辑：
 * 1. 冷热启动时，同时请求一个开屏和一个插屏广告
 * 2. 超时时间使用配置的 splash_time_out（默认10秒）
 * 3. 情况1：两个都成功 → 比较eCPM，展示更高的
 * 4. 情况2：只有一个成功 → 展示成功的
 * 5. 情况3：都失败 → 直接进入APP
 */
object SplashBiddingManager {

    private const val TAG = "SplashBidding"

    /**
     * 竞价结果
     */
    sealed class BidResult {
        /** 展示开屏广告 */
        data class ShowSplash(val ecpm: Double?) : BidResult()
        /** 展示插屏广告 */
        data class ShowInterstitial(val ecpm: Double?) : BidResult()
        /** 直接进入APP（两个广告都加载失败） */
        object EnterApp : BidResult()
    }

    /**
     * 竞价加载结果
     */
    data class BidLoadResult(
        val splashLoaded: Boolean,
        val splashEcpm: Double?,
        val interstitialLoaded: Boolean,
        val interstitialEcpm: Double?,
        val winner: BidResult,
        val loadTimeMs: Long
    )

    /**
     * 执行竞价加载
     * 同时请求开屏和插屏广告，在超时时间内等待结果
     * 
     * @param context 上下文
     * @return 竞价加载结果
     */
    suspend fun loadWithBidding(context: Context): BidLoadResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val timeoutMs = AdConfigManager.getSplashTimeout() * 1000L

        AdLogger.d("[%s] ========== 开始竞价加载 ==========", TAG)
        AdLogger.d("[%s] 超时时间: %d 秒", TAG, timeoutMs / 1000)

        // 并行请求开屏和插屏广告
        val splashDeferred = async {
            try {
                AdLogger.d("[%s] 开始加载开屏广告...", TAG)
                val result = LaunchAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_SPLASH_ID)
                currentCoroutineContext().ensureActive()
                val success = result is AdResult.Success
                AdLogger.d("[%s] 开屏广告加载%s", TAG, if (success) "成功" else "失败")
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AdLogger.e("[%s] 开屏广告加载异常: %s", TAG, e.message)
                false
            }
        }

        val interstitialDeferred = async {
            try {
                AdLogger.d("[%s] 开始加载插屏广告...", TAG)
                val result = InterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_INTERSTITIAL_ID)
                currentCoroutineContext().ensureActive()
                val success = result is AdResult.Success
                AdLogger.d("[%s] 插屏广告加载%s", TAG, if (success) "成功" else "失败")
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AdLogger.e("[%s] 插屏广告加载异常: %s", TAG, e.message)
                false
            }
        }

        // 等待结果（带超时）
        val results = withTimeoutOrNull(timeoutMs) {
            val splashSuccess = splashDeferred.await()
            val interstitialSuccess = interstitialDeferred.await()
            Pair(splashSuccess, interstitialSuccess)
        }

        val loadTimeMs = System.currentTimeMillis() - startTime
        
        // 处理超时情况
        val (splashLoaded, interstitialLoaded) = if (results != null) {
            results
        } else {
            AdLogger.w("[%s] 竞价加载超时（%d ms），检查当前缓存状态", TAG, loadTimeMs)
            splashDeferred.cancel()
            interstitialDeferred.cancel()
            // 超时后检查是否有任何广告已经加载完成
            val hasSplash = LaunchAds.getInstance().hasCachedAd()
            val hasInterstitial = InterstitialAds.getInstance().hasCachedAd()
            Pair(hasSplash, hasInterstitial)
        }

        // 获取 eCPM
        val splashEcpm = if (splashLoaded) {
            LaunchAds.getInstance().getCachedAdPrice(context)
        } else null

        val interstitialEcpm = if (interstitialLoaded) {
            InterstitialAds.getInstance().getCachedAdPrice(context)
        } else null

        // 决定竞价胜者
        val winner = decideBidWinner(splashLoaded, splashEcpm, interstitialLoaded, interstitialEcpm)

       try {
           // 打印竞价结果日志
           logBidResult(splashLoaded, splashEcpm, interstitialLoaded, interstitialEcpm, winner, loadTimeMs)
       }catch (_: Throwable){
       }

        BidLoadResult(
            splashLoaded = splashLoaded,
            splashEcpm = splashEcpm,
            interstitialLoaded = interstitialLoaded,
            interstitialEcpm = interstitialEcpm,
            winner = winner,
            loadTimeMs = loadTimeMs
        )
    }

    /**
     * 决定竞价胜者
     */
    private fun decideBidWinner(
        splashLoaded: Boolean,
        splashEcpm: Double?,
        interstitialLoaded: Boolean,
        interstitialEcpm: Double?
    ): BidResult {
        return when {
            // 情况1：两个都加载成功 → 比较eCPM
            splashLoaded && interstitialLoaded -> {
                val splashPrice = splashEcpm ?: 0.0
                val interstitialPrice = interstitialEcpm ?: 0.0
                
                if (splashPrice >= interstitialPrice) {
                    AdLogger.d("[%s] 竞价结果：开屏胜出 (开屏: %.6f >= 插屏: %.6f)", TAG, splashPrice, interstitialPrice)
                    BidResult.ShowSplash(splashEcpm)
                } else {
                    AdLogger.d("[%s] 竞价结果：插屏胜出 (插屏: %.6f > 开屏: %.6f)", TAG, interstitialPrice, splashPrice)
                    BidResult.ShowInterstitial(interstitialEcpm)
                }
            }
            // 情况2a：仅开屏加载成功
            splashLoaded -> {
                AdLogger.d("[%s] 竞价结果：仅开屏加载成功，展示开屏", TAG)
                BidResult.ShowSplash(splashEcpm)
            }
            // 情况2b：仅插屏加载成功
            interstitialLoaded -> {
                AdLogger.d("[%s] 竞价结果：仅插屏加载成功，展示插屏", TAG)
                BidResult.ShowInterstitial(interstitialEcpm)
            }
            // 情况3：都加载失败
            else -> {
                AdLogger.d("[%s] 竞价结果：两个广告都加载失败，直接进入APP", TAG)
                BidResult.EnterApp
            }
        }
    }

    /**
     * 打印竞价结果日志
     */
    private fun logBidResult(
        splashLoaded: Boolean,
        splashEcpm: Double?,
        interstitialLoaded: Boolean,
        interstitialEcpm: Double?,
        winner: BidResult,
        loadTimeMs: Long
    ) {
        AdLogger.d("[%s] ========== 竞价结果汇总 ==========", TAG)
        AdLogger.d("[%s] 加载耗时: %d ms", TAG, loadTimeMs)
        AdLogger.d("[%s] 开屏广告: %s, eCPM: %s", TAG, 
            if (splashLoaded) "已加载" else "加载失败",
            splashEcpm?.let { String.format(Locale.US,"%.6f", it) } ?: "N/A"
        )
        AdLogger.d("[%s] 插屏广告: %s, eCPM: %s", TAG,
            if (interstitialLoaded) "已加载" else "加载失败",
            interstitialEcpm?.let { String.format(Locale.US,"%.6f", it) } ?: "N/A"
        )
        AdLogger.d("[%s] 胜出者: %s", TAG, when (winner) {
            is BidResult.ShowSplash -> "开屏广告"
            is BidResult.ShowInterstitial -> "插屏广告"
            is BidResult.EnterApp -> "无（直接进入APP）"
        })
        AdLogger.d("[%s] =====================================", TAG)

        // 上报竞价结果埋点
        val biddingLog = String.format(
            Locale.US,
            format = "开屏竞价结果 -> 开屏: %.8f 美元, 插页: %.8f 美元",
            splashEcpm ?: 0.0,
            interstitialEcpm ?: 0.0
        )
        AdLogger.d(biddingLog)
        ReportDataManager.reportDataByName(reporterName = "ThinkingData", eventName = "bidding", data = mapOf("log" to biddingLog))
    }

    /**
     * 根据竞价结果展示广告
     * 
     * @param activity Activity上下文
     * @param bidResult 竞价结果
     * @param onAdLoaded 广告加载回调
     * @return 广告展示结果
     */
    suspend fun showByBidResult(
        activity: Activity,
        bidResult: BidResult,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        return when (bidResult) {
            is BidResult.ShowSplash -> {
                AdLogger.d("[%s] 根据竞价结果展示开屏广告", TAG)
                LaunchAds.getInstance().displayAd(activity, AdPosition.SP_APP_START, onLoaded = onAdLoaded)
            }
            is BidResult.ShowInterstitial -> {
                AdLogger.d("[%s] 根据竞价结果展示插屏广告", TAG)
                onAdLoaded?.invoke(true)
                // 等待权限授权完成后再展示插屏广告
                LaunchAds.getInstance().awaitPermissionReady()
                InterstitialAds.getInstance().displayAd(activity, AdPosition.SP_APP_START, ignoreFullNative = true)
            }
            is BidResult.EnterApp -> {
                AdLogger.d("[%s] 竞价失败，直接进入APP", TAG)
                onAdLoaded?.invoke(false)
                AdResult.Failure(AdException(0, "竞价失败：两个广告都加载失败"))
            }
        }
    }

    /**
     * 执行完整的竞价流程（加载 + 展示）
     * 
     * @param activity Activity上下文
     * @param onAdLoaded 广告加载回调
     * @return 广告展示结果
     */
    suspend fun bidAndShow(
        activity: Activity,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        // 1. 执行竞价加载
        val bidLoadResult = loadWithBidding(activity)
        
        // 2. 根据结果展示广告
        return showByBidResult(activity, bidLoadResult.winner, onAdLoaded)
    }

    /**
     * 检查是否应该使用竞价模式
     */
    fun shouldUseBidding(): Boolean {
        return AdConfigManager.isSplashBiddingEnabled()
    }

    // ============ 多平台竞价支持 ============

    /**
     * 检查是否应该使用多平台竞价
     * 当多平台竞价启用时，使用新的竞价管理器
     */
    fun shouldUseMultiPlatformBidding(): Boolean {
        return BiddingPlatformController.isMultiPlatformBiddingEnabled()
    }

    /**
     * 执行多平台竞价流程（预加载 + 竞价 + 展示）
     * 
     * @param activity Activity 上下文
     * @param container TopOn 开屏广告需要的容器（可选）
     * @param onAdLoaded 广告加载回调
     * @return 广告展示结果
     */
    suspend fun multiPlatformBidAndShow(
        activity: Activity,
        container: ViewGroup? = null,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        AdLogger.d("[$TAG] ========== 开始多平台竞价 ==========")
        
        // 1. 执行预加载
        SplashTwoLayerPreloadManager.preloadAll(activity)
        
        // 2. 执行竞价
        val bidResult = SplashTwoLayerPreloadManager.performTwoLayerBidding(activity)
        
        // 3. 检查结果
        if (bidResult.winner == null) {
            AdLogger.w("[$TAG] 多平台竞价失败，没有可用广告")
            onAdLoaded?.invoke(false)
            return AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "多平台竞价失败"))
        }
        
        AdLogger.d("[$TAG] 多平台竞价胜出: %s - %s, eCPM: %.6f USD",
            bidResult.winner.platform.name,
            bidResult.winner.winnerType.name,
            bidResult.winner.ecpm)
        
        onAdLoaded?.invoke(true)
        
        // 4. 展示广告
        return SplashTwoLayerPreloadManager.showWinnerAd(activity, container, bidResult)
    }

    /**
     * 智能竞价：根据配置自动选择单平台或多平台竞价
     */
    suspend fun smartBidAndShow(
        activity: Activity,
        container: ViewGroup? = null,
        onAdLoaded: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        // 确保竞价配置已初始化（解决异步初始化时序问题）
        BiddingConfigManager.ensureInitialized(activity)
        
        return if (shouldUseMultiPlatformBidding()) {
            multiPlatformBidAndShow(activity, container, onAdLoaded)
        } else {
            bidAndShow(activity, onAdLoaded)
        }
    }
}

