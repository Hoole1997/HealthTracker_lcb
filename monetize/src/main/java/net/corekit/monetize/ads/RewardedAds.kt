package net.corekit.monetize.ads


import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.bidding.BiddingAdType
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil
import net.corekit.monetize.ui.dialog.ADLoadingDialog
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * 激励广告控制器（支持缓存池）
 */
class RewardedAds private constructor() {

    companion object {
        private const val TAG = "RewardedAds"
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 1

        @Volatile
        private var INSTANCE: RewardedAds? = null

        fun getInstance(): RewardedAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardedAds().also { INSTANCE = it }
            }
        }
    }
    private val cacheLock = Any()
    private val adCachePool = mutableListOf<CachedRewardedAd>()
    private val maxCacheSizePerAdUnit = DEFAULT_CACHE_SIZE_PER_AD_UNIT

    // 正在加载中的任务计数
    private val inflightLoads = mutableMapOf<String, Int>()
    
    private data class CachedRewardedAd(
        val ad: RewardedAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > 1 * 60 * 60 * 1000L
        }
    }

    private var totalTriggerCount by DataStoreIntDelegate("reward_trigger_count", 0)
    private var totalShowCount by DataStoreIntDelegate("reward_show_count", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("reward_load_fail_count", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("reward_load_suc_count", 0)
    private var totalShowFailCount by DataStoreIntDelegate("reward_show_fail_count", 0)
    private var totalRewardCount by DataStoreIntDelegate("reward_reward_count", 0)
    private var totalClickCount by DataStoreIntDelegate("reward_click_count", 0)
    // 累积加载次数统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("reward_load_count", 0)
    private var totalCloseCount by DataStoreIntDelegate("reward_close_count", 0)
    private var currentAdValue: AdValue? = null

    /**
     * 当前待恢复的展示请求
     * 只保存一个请求，新请求会覆盖旧请求
     */
    private var pendingRequest: PendingShowRequest<RewardedAd>? = null

    /**
     * 预加载激励广告
     */
    suspend fun load(context: Context, adUnitId: String? = null): AdResult<Unit> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID

        // 1. 检查是否可加载（防止并发导致的溢出）
        val canLoad = synchronized(cacheLock) {
            val currentCount = adCachePool.count { it.adUnitId == finalAdUnitId && !it.isExpired() }
            val currentInflight = inflightLoads[finalAdUnitId] ?: 0
            if (currentCount + currentInflight >= maxCacheSizePerAdUnit) {
                false
            } else {
                inflightLoads[finalAdUnitId] = currentInflight + 1
                true
            }
        }

        if (!canLoad) {
            val currentCount = getCachedAdCount(finalAdUnitId)
            AdLogger.d("[$TAG] 缓存已满或正在加载中，跳过加载: %s (当前缓存: %d/%d, 正在加载: %d)", 
                finalAdUnitId, currentCount, maxCacheSizePerAdUnit, inflightLoads[finalAdUnitId] ?: 0)
            return AdResult.Success(Unit)
        }

        return try {
            val rewardedAd = withContext(Dispatchers.Main) {
                reportAdData(
                    eventName = "ad_start_load",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "number" to totalLoadCount
                    )
                )
                loadInternal(context.applicationContext, finalAdUnitId)
            }
            if (rewardedAd != null) {
                synchronized(cacheLock) {
                    adCachePool.add(CachedRewardedAd(rewardedAd, finalAdUnitId))
                    val currentCount = getCachedAdCount(finalAdUnitId)
                    AdLogger.d(
                        "激励广告加载成功并缓存，广告位ID: %s，缓存数量: %d/%d",
                        finalAdUnitId,
                        currentCount,
                        maxCacheSizePerAdUnit
                    )
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(AdErrorCode.AD_LOAD_FAILED.toAdException())
            }
        } catch (e: Exception) {
            AdLogger.e("激励广告加载异常", e)
            AdResult.Failure(AdErrorCode.AD_LOAD_EXCEPTION.toAdException(e))
        } finally {
            // 2. 释放加载中的名额
            synchronized(cacheLock) {
                val currentInflight = inflightLoads[finalAdUnitId] ?: 0
                if (currentInflight > 0) {
                    inflightLoads[finalAdUnitId] = currentInflight - 1
                }
            }
        }
    }

    /**
     * 展示激励广告
     */
    suspend fun show(
        activity: Activity,
        position: String,
        adUnitId: String? = null
    ): AdResult<RewardOutcome> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID

        totalTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to position,
                "number" to totalTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.ADMOB, BiddingAdType.REWARDED)) {
            totalShowFailCount++
            reportAdData(
                "ad_show_fail",
                mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )
            return AdResult.Failure(AdErrorCode.AD_SHOW_FAILED.toAdException("platform_frequency_limit"))
        }

       val adResult = try {
            var cachedAd = getCachedAd(finalAdUnitId)
            var loadingShown = false

            if (cachedAd == null) {
                loadingShown = true
                ADLoadingDialog.show(activity)
                load(activity, finalAdUnitId)
                cachedAd = getCachedAd(finalAdUnitId)

            }

            val adHolder = cachedAd
            if (adHolder == null) {
                if (!loadingShown) {
                    ADLoadingDialog.hide()
                }
                val error = AdErrorCode.AD_CACHE_NOT_AVAILABLE.toAdException()
                AdResult.Failure(error)
            }

           ADLoadingDialog.hide()


           if(adHolder != null){
               if(activity.isFinishing || activity.isDestroyed){
                   AdLogger.d("页面${activity.javaClass.simpleName}，已关闭")
                   return AdResult.Failure(AdErrorCode.ACTIVITY_FINISHING.toAdException())
               }

               if(activity is LifecycleOwner){
                   val isResume = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                   if(!isResume){
                       if(AppLifecycleManager.isForeground()){
                           AdLogger.d("跳转到了其他页面，不在展示激励广告")
                           return AdResult.Failure(AdErrorCode.ACTIVITY_NOT_RESUMED.toAdException())
                       }
                       AdLogger.d(" Activity ${activity.javaClass.simpleName} not in RESUMED state (current: ${activity.lifecycle.currentState}), waiting for resume...")
                       return suspendCancellableCoroutine { continuation ->
                           val observer = ResumeLifecycleObserver(activity,activity)
                           pendingRequest = PendingShowRequest(
                               adHolder.ad,finalAdUnitId,position,continuation
                           )
                           activity.lifecycle.addObserver(observer)
                           continuation.invokeOnCancellation {
                               pendingRequest = null
                               activity.lifecycle.removeObserver(observer)
                               AdLogger.d("[OpenAd] Pending show cancelled for $adUnitId")
                           }
                       }
                   }
               }

               // 3. 显示广告
               val result = showAdInternal(activity, adHolder.ad, finalAdUnitId, position)

               result
           }else{
               AdResult.Failure(AdErrorCode.AD_LOAD_FAILED.toAdException())
           }





        } catch (e: Exception) {
            val error = AdErrorCode.AD_SHOW_EXCEPTION.toAdException(e)
            AdResult.Failure(error)
        }finally {
            ADLoadingDialog.hide()
        }

        return adResult
    }


    private suspend fun showAdInternal(activity: Activity,rewardAd: RewardedAd,finalAdUnitId: String, position: String): AdResult<RewardOutcome>{
        return suspendCancellableCoroutine { continuation ->
            var rewardItem: RewardItem? = null

            rewardAd.adEventCallback = object : RewardedAdEventCallback {
                override fun onAdPaid(value: AdValue) {
                    super.onAdPaid(value)
                    currentAdValue = value
                    AdLogger.d(
                        "激励广告收益回调: value=%d, currency=%s",
                        value.valueMicros,
                        value.currencyCode
                    )
                    // 上报收益
                    reportRevenue(rewardAd, finalAdUnitId, value)

                    // 补充 ad_impression 事件并路由到 ThinkingData
                    reportAdData(
                        "ad_impression",
                        mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to position,
                            "number" to totalShowCount,
                            "ad_source" to (rewardAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (value.valueMicros / 1_000_000.0),
                            "currency" to value.currencyCode
                        )
                    )

                    // 触发 Ipu / Rpu 钩子
                    IpuController.onAdImpression("RV", value.valueMicros)
                    RpuController.onAdRevenue("RV", value.valueMicros)
                }
                override fun onAdShowedFullScreenContent() {
                    totalShowCount++
                    AdLogger.d("激励广告展示成功，总展示次数: %d", totalShowCount)
                }

                override fun onAdDismissedFullScreenContent() {
                    totalCloseCount++
                    val outcome = RewardOutcome(
                        rewarded = rewardItem != null,
                        rewardType = rewardItem?.type,
                        rewardAmount = rewardItem?.amount
                    )
                    reportAdData(
                        "ad_close",
                        mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to position,
                            "number" to totalCloseCount,
                            "reward_granted" to outcome.rewarded,
                            "ad_source" to (rewardAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                    val result = AdResult.Success(outcome)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                    rewardAd.adEventCallback = null

                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    totalShowFailCount++
                    AdLogger.e("激励广告展示失败: %s", fullScreenContentError.message)
                    val error = AdErrorCode.AD_SHOW_FAILED.toAdException("show failed: ${fullScreenContentError.message}")
                    reportAdData(
                        "ad_show_fail",
                        mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to position,
                            "ad_source" to (rewardAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "reason" to fullScreenContentError.message,
                            "code" to fullScreenContentError.code,
                            "number" to totalShowFailCount
                        )
                    )
                    val result = AdResult.Failure(error)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                    rewardAd.adEventCallback = null
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    AdLogger.d("激励广告曝光完成")
                    // 异步预加载下一个广告到缓存（如果缓存未满）
                    if (!isCacheFull(finalAdUnitId)) {
                        AdLogger.d("开屏开始异步预加载下一个广告，广告位ID: %s", finalAdUnitId)
                        PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB, net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED)
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdLogger.d("激励广告被点击")

                    // 累积点击统计
                    totalClickCount++
                    AdLogger.d("激励广告累积点击次数: $totalClickCount")

                    AdConfigManager.getRewardedConfig().recordClick()
                    PlatformFrequencyManager.recordClick(BiddingPlatform.ADMOB, BiddingAdType.REWARDED)

                    reportAdData(
                        eventName = "ad_click",
                        params = mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to position,
                            "number" to totalClickCount,
                            "ad_source" to (rewardAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )
                }
            }

            rewardAd.show(activity) { item ->
                rewardItem = item
                totalRewardCount++
                reportAdData(
                    "ad_reward_earned",
                    mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to position,
                        "number" to totalRewardCount,
                        "type" to item.type,
                        "amount" to item.amount,
                        "ad_source" to (rewardAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                    )
                )
            }
        }
    }

    /**
     * 当前指定广告位是否有可用缓存
     */
    fun isReady(adUnitId: String? = null): Boolean {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID
        return getCachedAdCount(finalAdUnitId) > 0
    }

    /**
     * 清空缓存
     */
    fun release() {
        synchronized(cacheLock) { adCachePool.clear() }
        AdLogger.d("激励广告缓存已清理")
    }

    private suspend fun loadInternal(context: Context, adUnitId: String): RewardedAd? =
        suspendCancellableCoroutine { continuation ->
            // 频控前置检查（只检查配额，不检查间隔）
            // 注意：这里是在协程内，但为了安全起见使用同步调用，或者如果 canLoadAd 内部只是简单的内存检查也可以
            val (canLoad, reason) = net.corekit.monetize.ads.frequency.PlatformFrequencyManager.canLoadAd(
                net.corekit.monetize.ads.bidding.BiddingPlatform.ADMOB, 
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED
            )
            
            if (!canLoad) {
                val statusLog = net.corekit.monetize.ads.frequency.PlatformFrequencyManager.getFrequencyStatusLog(
                    net.corekit.monetize.ads.bidding.BiddingPlatform.ADMOB, 
                    net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED
                )
                AdLogger.w("[$TAG] 加载跳过 | 平台: AdMob | 类型: Rewarded | 原因: $reason | $statusLog")
                // 这里 reportAdData 是私有的，需要通过 companion object 或者 inner class 访问吗？ 
                // loadInternal 是成员方法，可以直接调用 reportAdData
                // 但是它在 suspendCancellableCoroutine 内部，this 可能是 continuation
                // 所以需要用 this@RewardedAds.reportAdData 或者直接调用成员方法（如果在 lambda 作用域内）
                
                // 由于 loadInternal 是 `= suspendCancellableCoroutine`, 这里的上下文是 lambda
                // 我们可以直接调用 reportAdData 因为它在 RewardedAds 类中
                
                // 但要小心 suspendCancellableCoroutine 需要 resume
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val startTime = System.currentTimeMillis()
            val adRequest = AdRequest.Builder(adUnitId)
                .build()

            RewardedAd.load(
                adRequest,
                object : AdLoadCallback<RewardedAd> {


                    override fun onAdLoaded(ad: RewardedAd) {
                        if (!continuation.isActive) {
                            return
                        }
                        totalLoadSucCount++
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.d("激励广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                        reportAdData(
                            "ad_loaded",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadSucCount,
                                "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            )
                        )
                        FpuController.onAdFill("RV")
                        continuation.resume(ad)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        if (!continuation.isActive) {
                            return
                        }
                        totalLoadFailCount++
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.w("激励广告加载失败: %s", adError.message)
                        reportAdData(
                            "ad_load_fail",
                            mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadFailCount,
                                "ad_source" to (adError.responseInfo?.loadedAdSourceResponseInfo?.name.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                "reason" to adError.message,
                                "code" to adError.code
                            )
                        )
                        continuation.resume(null)
                    }
                }
            )
        }

    fun peekCachedAd(adUnitId: String? = null): RewardedAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID
        synchronized(cacheLock) {
            adCachePool.removeAll { it.adUnitId == finalAdUnitId && it.isExpired() }
            return adCachePool.firstOrNull { it.adUnitId == finalAdUnitId && !it.isExpired() }?.ad
        }
    }

    fun hasCachedAd(adUnitId: String? = null): Boolean {
        return peekCachedAd(adUnitId) != null
    }

    suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID
        val cachedAd = peekCachedAd(finalAdUnitId)
        if (cachedAd == null) {
            AdLogger.w("[竞价] 获取激励广告价格失败：缓存为空")
            return null
        }

        val adValue = withContext(Dispatchers.Default) {
            AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd)
        }

        return if (adValue != null) {
            val price = adValue.valueMicros / 1_000_000.0
            AdLogger.d("[竞价] 获取激励广告价格成功: %.6f %s (精度: %s)", price, adValue.currencyCode, adValue.precisionType)
            price
        } else {
            AdLogger.w("[竞价] 获取激励广告价格失败：反射获取AdValue为空")
            null
        }
    }

    private fun getCachedAd(adUnitId: String): CachedRewardedAd? {
        synchronized(cacheLock) {
            val index = adCachePool.indexOfFirst { it.adUnitId == adUnitId && !it.isExpired() }
            return if (index != -1) {
                adCachePool.removeAt(index)
            } else {
                null
            }
        }
    }

    private fun getCachedAdCount(adUnitId: String): Int {
        synchronized(cacheLock) {
            // 同步移除过期广告
            adCachePool.removeAll { it.adUnitId == adUnitId && it.isExpired() }
            return adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
        }
    }

    private fun isCacheFull(adUnitId: String): Boolean {
        return getCachedAdCount(adUnitId) >= maxCacheSizePerAdUnit
    }

    private fun reportRevenue(rewardedAd: RewardedAd, adUnitId: String,adValue: AdValue) {
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = rewardedAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = rewardedAd.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "Rewarded"
        )

        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d(
            "激励广告收益已上报，广告位ID: %s, 收益(微元): %d %s",
            adUnitId,
            adValue.valueMicros,
            adValue.currencyCode
        )
    }

    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to "Rewarded"
        )
        data.putAll(params)
        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
    }

    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(
            code = 0,
            message = message,
            cause = cause
        )
    }

    data class RewardOutcome(
        val rewarded: Boolean,
        val rewardType: String? = null,
        val rewardAmount: Int? = null
    )
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Activity 生命周期观察者
     * 监听 Activity 的 onResume 和 onDestroy 事件
     * - onResume: 继续展示待展示的广告
     * - onDestroy: 清理待展示状态，返回失败结果
     */
    private inner class ResumeLifecycleObserver(
        private val activity: Activity,
        private val lifecycleOwner: LifecycleOwner
    ) : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            val pending = pendingRequest ?: return
            pendingRequest = null
            lifecycleOwner.lifecycle.removeObserver(this)
            controllerScope.launch {
                val result = try {
                    when{
                        activity.isFinishing || activity.isDestroyed ->{
                            AdLogger.w("Activity ${activity.javaClass.simpleName} is finishing/destroyed after resume event")
                            AdResult.Failure(createAdException("activity finish"))
                        }

                        lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                            withContext(Dispatchers.Main.immediate){
                                showAdInternal(activity,pending.ad,pending.adUnitId,pending.position)
                            }
                        }

                        else ->{
                            AdLogger.w("[OpenAd] Activity not in RESUMED state after resume event")
                            AdResult.Failure(createAdException("activity not resume"))
                        }
                    }
                }catch (e: Throwable){
                    AdLogger.e("[OpenAd] Failed to show ad after resume", e)
                    AdResult.Failure(createAdException("OpenAd show failed",e))
                }

                if(pending.continuation.isActive){
                    pending.continuation.resume(result)
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            pendingRequest?.ad?.adEventCallback = null
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    }

    data class PendingShowRequest<T>(
        val ad: T,
        val adUnitId: String,
        val position: String,
        val continuation: CancellableContinuation<AdResult<RewardedAds.RewardOutcome>>
    )

    /**
     * 获取缓存状态
     */
    fun getCacheStatus(adUnitId: String? = null): net.corekit.monetize.ads.log.BiddingLogger.CacheEntry {
        val finalAdUnitId = adUnitId ?: net.corekit.monetize.BuildConfig.ADMOB_REWARDED_ID
        return net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
            adType = "Rewarded",
            platform = "AdMob",
            adUnitId = finalAdUnitId,
            currentCount = getCachedAdCount(finalAdUnitId),
            maxCount = maxCacheSizePerAdUnit
        )
    }
}
