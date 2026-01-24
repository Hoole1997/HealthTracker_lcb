package net.corekit.monetize.ads

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.RewardTwoLayerPreloadManager
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.config.BiddingConfigManager
import net.corekit.monetize.ads.log.AdLogger
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object RewardBiddingManager {

    private const val TAG = "RewardBidding"
    private val isBidding = AtomicBoolean(false)

    sealed class BidResult {
        data class ShowRewarded(val ecpm: Double?) : BidResult()
        data class ShowRewardedInterstitial(val ecpm: Double?) : BidResult()
        data class ShowInterstitial(val ecpm: Double?) : BidResult()
        object EnterNext : BidResult()
    }

    data class BidLoadResult(
        val rewardedLoaded: Boolean,
        val rewardedEcpm: Double?,
        val rewardedInterstitialLoaded: Boolean,
        val rewardedInterstitialEcpm: Double?,
        val interstitialLoaded: Boolean,
        val interstitialEcpm: Double?,
        val winner: BidResult,
        val loadTimeMs: Long
    )

    suspend fun loadWithBidding(context: Context): BidLoadResult = coroutineScope {
        val startTime = System.currentTimeMillis()
        val timeoutMs = BiddingConfigManager.getBiddingTimeoutMs("reward")

        AdLogger.d("[%s] ========== 开始激励三方竞价加载 ==========", TAG)
        AdLogger.d("[%s] 超时时间: %d ms", TAG, timeoutMs)

        val rewardedDeferred = async {
            try {
                if (RewardedAds.getInstance().hasCachedAd()) {
                    AdLogger.d("[%s] 激励广告已存在缓存，跳过加载", TAG)
                    return@async true
                }
                AdLogger.d("[%s] 开始加载激励广告...", TAG)
                val result = RewardedAds.getInstance().load(context, BuildConfig.ADMOB_REWARDED_ID)
                currentCoroutineContext().ensureActive()
                val success = result is AdResult.Success
                AdLogger.d("[%s] 激励广告加载%s", TAG, if (success) "成功" else "失败")
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AdLogger.e("[%s] 激励广告加载异常: %s", TAG, e.message)
                false
            }
        }

        val rewardedInterstitialDeferred = async {
            try {
                if (RewardedInterstitialAds.getInstance().hasCachedAd()) {
                    AdLogger.d("[%s] 插页激励广告已存在缓存，跳过加载", TAG)
                    return@async true
                }
                AdLogger.d("[%s] 开始加载插页激励广告...", TAG)
                val result = RewardedInterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)
                currentCoroutineContext().ensureActive()
                val success = result is AdResult.Success
                AdLogger.d("[%s] 插页激励广告加载%s", TAG, if (success) "成功" else "失败")
                success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AdLogger.e("[%s] 插页激励广告加载异常: %s", TAG, e.message)
                false
            }
        }

        val interstitialDeferred = async {
            try {
                if (InterstitialAds.getInstance().hasCachedAd()) {
                    AdLogger.d("[%s] 插屏广告已存在缓存，跳过加载", TAG)
                    return@async true
                }
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

        val results = withTimeoutOrNull<Triple<Boolean, Boolean, Boolean>>(timeoutMs) {
            val r = rewardedDeferred.await()
            val ri = rewardedInterstitialDeferred.await()
            val i = interstitialDeferred.await()
            Triple(r, ri, i)
        }

        val loadTimeMs = System.currentTimeMillis() - startTime

        val (rewardedLoaded, rewardedInterstitialLoaded, interstitialLoaded) = results ?: run {
            AdLogger.w("[%s] 竞价加载超时（%d ms），检查当前缓存状态", TAG, loadTimeMs)
            rewardedDeferred.cancel()
            rewardedInterstitialDeferred.cancel()
            interstitialDeferred.cancel()

            Triple(
                RewardedAds.getInstance().hasCachedAd(),
                RewardedInterstitialAds.getInstance().hasCachedAd(),
                InterstitialAds.getInstance().hasCachedAd()
            )
        }

        val rewardedEcpm = if (rewardedLoaded) RewardedAds.getInstance().getCachedAdPrice(context) else null
        val rewardedInterstitialEcpm = if (rewardedInterstitialLoaded) RewardedInterstitialAds.getInstance().getCachedAdPrice(context) else null
        val interstitialEcpm = if (interstitialLoaded) InterstitialAds.getInstance().getCachedAdPrice(context) else null

        val winner = decideBidWinner(
            rewardedLoaded,
            rewardedEcpm,
            rewardedInterstitialLoaded,
            rewardedInterstitialEcpm,
            interstitialLoaded,
            interstitialEcpm
        )

        try {
            logBidResult(
                rewardedLoaded,
                rewardedEcpm,
                rewardedInterstitialLoaded,
                rewardedInterstitialEcpm,
                interstitialLoaded,
                interstitialEcpm,
                winner,
                loadTimeMs
            )
        } catch (_: Throwable) {
        }

        BidLoadResult(
            rewardedLoaded = rewardedLoaded,
            rewardedEcpm = rewardedEcpm,
            rewardedInterstitialLoaded = rewardedInterstitialLoaded,
            rewardedInterstitialEcpm = rewardedInterstitialEcpm,
            interstitialLoaded = interstitialLoaded,
            interstitialEcpm = interstitialEcpm,
            winner = winner,
            loadTimeMs = loadTimeMs
        )
    }

    private fun decideBidWinner(
        rewardedLoaded: Boolean,
        rewardedEcpm: Double?,
        rewardedInterstitialLoaded: Boolean,
        rewardedInterstitialEcpm: Double?,
        interstitialLoaded: Boolean,
        interstitialEcpm: Double?
    ): BidResult {
        if (!rewardedLoaded && !rewardedInterstitialLoaded && !interstitialLoaded) {
            AdLogger.d("[%s] 竞价结果：三个广告都加载失败，进入下一页面", TAG)
            return BidResult.EnterNext
        }

        val candidates = listOf(
            Triple("rewarded", rewardedLoaded, rewardedEcpm ?: 0.0),
            Triple("rewarded_interstitial", rewardedInterstitialLoaded, rewardedInterstitialEcpm ?: 0.0),
            Triple("interstitial", interstitialLoaded, interstitialEcpm ?: 0.0)
        ).filter { it.second }

        val max = candidates.maxWithOrNull(compareBy<Triple<String, Boolean, Double>> { it.third }
            .thenBy { when (it.first) {
                "rewarded_interstitial" -> 2
                "rewarded" -> 1
                else -> 0
            } })

        return when (max?.first) {
            "rewarded" -> {
                AdLogger.d("[%s] 竞价结果：激励胜出", TAG)
                BidResult.ShowRewarded(rewardedEcpm)
            }
            "rewarded_interstitial" -> {
                AdLogger.d("[%s] 竞价结果：插页激励胜出", TAG)
                BidResult.ShowRewardedInterstitial(rewardedInterstitialEcpm)
            }
            else -> {
                AdLogger.d("[%s] 竞价结果：插屏胜出", TAG)
                BidResult.ShowInterstitial(interstitialEcpm)
            }
        }
    }

    private fun logBidResult(
        rewardedLoaded: Boolean,
        rewardedEcpm: Double?,
        rewardedInterstitialLoaded: Boolean,
        rewardedInterstitialEcpm: Double?,
        interstitialLoaded: Boolean,
        interstitialEcpm: Double?,
        winner: BidResult,
        loadTimeMs: Long
    ) {
        AdLogger.d("[%s] ========== 激励三方竞价结果汇总 ==========", TAG)
        AdLogger.d("[%s] 加载耗时: %d ms", TAG, loadTimeMs)
        AdLogger.d(
            "[%s] 激励广告: %s, eCPM: %s",
            TAG,
            if (rewardedLoaded) "已加载" else "加载失败",
            rewardedEcpm?.let { String.format(Locale.US, "%.6f", it) } ?: "N/A"
        )
        AdLogger.d(
            "[%s] 插页激励: %s, eCPM: %s",
            TAG,
            if (rewardedInterstitialLoaded) "已加载" else "加载失败",
            rewardedInterstitialEcpm?.let { String.format(Locale.US, "%.6f", it) } ?: "N/A"
        )
        AdLogger.d(
            "[%s] 插屏广告: %s, eCPM: %s",
            TAG,
            if (interstitialLoaded) "已加载" else "加载失败",
            interstitialEcpm?.let { String.format(Locale.US, "%.6f", it) } ?: "N/A"
        )
        AdLogger.d(
            "[%s] 胜出者: %s",
            TAG,
            when (winner) {
                is BidResult.ShowRewarded -> "激励广告"
                is BidResult.ShowRewardedInterstitial -> "插页激励"
                is BidResult.ShowInterstitial -> "插屏广告"
                is BidResult.EnterNext -> "无（进入下一页面）"
            }
        )
        AdLogger.d("[%s] ========================================", TAG)

        val biddingLog = String.format(
            Locale.US,
            format = "激励竞价结果 -> 激励: %.8f, 插页激励: %.8f, 插屏: %.8f",
            rewardedEcpm ?: 0.0,
            rewardedInterstitialEcpm ?: 0.0,
            interstitialEcpm ?: 0.0
        )
        AdLogger.d(biddingLog)
        ReportDataManager.reportDataByName(
            reporterName = "ThinkingData",
            eventName = "reward_bidding",
            data = mapOf("log" to biddingLog)
        )
    }

    suspend fun showWithBidding(activity: Activity, position: String): AdResult<Unit> {
        if (!isBidding.compareAndSet(false, true)) {
            AdLogger.w("[%s] 竞价正在进行中，忽略本次请求", TAG)
            return AdResult.Failure(AdException(-1, "Bidding is in progress"))
        }

        try {
            val bid = loadWithBidding(activity)
            return when (val winner = bid.winner) {
                is BidResult.ShowRewarded -> {
                    AdLogger.d("[%s] 根据竞价结果展示激励广告", TAG)
                    when (val r = RewardedAds.getInstance().show(activity, position, BuildConfig.ADMOB_REWARDED_ID)) {
                        is AdResult.Success -> AdResult.Success(Unit)
                        is AdResult.Failure -> r
                        AdResult.Loading -> AdResult.Loading
                    }
                }

                is BidResult.ShowRewardedInterstitial -> {
                    AdLogger.d("[%s] 根据竞价结果展示插页激励广告", TAG)
                    when (val r = RewardedInterstitialAds.getInstance().displayAd(activity, position, BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)) {
                        is AdResult.Success -> AdResult.Success(Unit)
                        is AdResult.Failure -> AdResult.Failure(r.error)
                        AdResult.Loading -> AdResult.Loading
                    }
                }

                is BidResult.ShowInterstitial -> {
                    AdLogger.d("[%s] 根据竞价结果展示插屏广告", TAG)
                    AdLogger.d("[%s] 插屏胜出 -> 走竞价插屏入口（跳过cooldown，不更新lastShow/dailyShow）", TAG)
                    InterstitialAds.getInstance().displayAdForRewardBidding(activity, position, BuildConfig.ADMOB_INTERSTITIAL_ID)
                }

                is BidResult.EnterNext -> {
                    AdLogger.d("[%s] 竞价失败，进入下一页面", TAG)
                    AdResult.Failure(AdException(0, "竞价失败：三个广告都加载失败"))
                }
            }
        } finally {
            isBidding.set(false)
        }
    }

    // ============ 多平台竞价支持 ============

    /**
     * 检查是否应该使用多平台竞价
     */
    fun shouldUseMultiPlatformBidding(): Boolean {
        return BiddingPlatformController.isMultiPlatformBiddingEnabled()
    }

    /**
     * 执行多平台竞价流程（优先使用缓存结果）
     * 
     * @param activity Activity 上下文
     * @param position 广告位置标识
     * @param onRewardEarned 奖励回调
     * @return 广告展示结果
     */
    suspend fun multiPlatformShowWithBidding(
        activity: Activity,
        position: String,
        onRewardEarned: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        if (!isBidding.compareAndSet(false, true)) {
            AdLogger.w("[%s] 多平台竞价正在进行中，忽略本次请求", TAG)
            return AdResult.Failure(AdException(-1, "Multi-platform bidding is in progress"))
        }

        try {
            AdLogger.d("[$TAG] ========== 开始多平台激励竞价 ==========")
            
            // 执行实时竞价（广告已在后台预加载完成）
            // 竞价逻辑是非阻塞的，仅检查已缓存的广告
            val bidResult = RewardTwoLayerPreloadManager.performTwoLayerBidding(activity)
            
            // 检查结果
            if (bidResult.winner == null) {
                AdLogger.w("[$TAG] 多平台激励竞价失败，没有可用广告")
                return AdResult.Failure(AdException(AdException.ERROR_NOT_LOADED, "多平台激励竞价失败"))
            }
            
            AdLogger.d("[$TAG] 多平台激励竞价胜出: %s - %s, eCPM: %.6f USD",
                bidResult.winner.platform.name,
                bidResult.winner.winnerType.name,
                bidResult.winner.ecpm)
            
            // 展示广告
            val consumedAdType = bidResult.winner.winnerType
            val consumedPlatform = bidResult.winner.platform
            return RewardTwoLayerPreloadManager.showWinnerAd(activity, bidResult, onRewardEarned).also {
                // 展示后异步定向预加载消耗的广告类型和平台（在 also 块中确保能获取到 consumedAdType 和 consumedPlatform）
                CoroutineScope(Dispatchers.IO).launch {
                    preloadByConsumedType(activity, consumedAdType, consumedPlatform)
                }
            }
        } finally {
            isBidding.set(false)
        }
    }
    
    /**
     * 定向预加载：根据消耗的广告类型和平台补充缓存
     */
    private suspend fun preloadByConsumedType(
        context: Context, 
        consumedAdType: net.corekit.monetize.ads.bidding.BiddingAdType,
        consumedPlatform: net.corekit.monetize.ads.bidding.BiddingPlatform
    ) {
        try {
            AdLogger.d("[$TAG] 后台定向预加载 | 消耗类型: %s | 消耗平台: %s", consumedAdType.name, consumedPlatform.name)
            RewardTwoLayerPreloadManager.preloadByConsumedType(context, consumedAdType, consumedPlatform)
            AdLogger.d("[$TAG] 后台定向预加载完成")
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 后台预加载失败: %s", e.message)
        }
    }

    /**
     * 智能竞价：根据配置自动选择单平台或多平台竞价
     */
    suspend fun smartShowWithBidding(
        activity: Activity,
        position: String,
        onRewardEarned: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        return if (shouldUseMultiPlatformBidding()) {
            multiPlatformShowWithBidding(activity, position, onRewardEarned)
        } else {
            showWithBidding(activity, position)
        }
    }
}
