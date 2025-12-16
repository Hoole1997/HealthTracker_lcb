package net.corekit.monetize.ads


import ads_mobile_sdk.nu
import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.ActivityUtils
import com.facebook.ads.RewardData
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import com.remax.bill.ads.report.IpuController
import com.remax.bill.ads.report.RpuController
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
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.model.PendingShowRequest
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ui.dialog.ADLoadingDialog
import net.corekit.monetize.util.PositionGet
import net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * 激励广告控制器（支持缓存池）
 */
class RewardedAds private constructor() {

    companion object {
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2

        @Volatile
        private var INSTANCE: RewardedAds? = null

        fun getInstance(): RewardedAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardedAds().also { INSTANCE = it }
            }
        }
    }

    private data class CachedRewardedAd(
        val ad: RewardedAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > 1 * 60 * 60 * 1000L
        }
    }

    private val cacheLock = Any()
    private val adCachePool = mutableListOf<CachedRewardedAd>()
    private val maxCacheSizePerAdUnit = DEFAULT_CACHE_SIZE_PER_AD_UNIT

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
        if (isCacheFull(finalAdUnitId)) {
            AdLogger.d(
                "激励广告缓存已满，广告位ID: %s，当前缓存: %d/%d",
                finalAdUnitId,
                getCachedAdCount(finalAdUnitId),
                maxCacheSizePerAdUnit
            )
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
                AdResult.Failure(createAdException("激励广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("激励广告加载异常", e)
            AdResult.Failure(createAdException("加载异常: ${e.message}", e))
        }
    }

    /**
     * 展示激励广告
     */
    suspend fun show(
        activity: Activity,
        adUnitId: String? = null
    ): AdResult<RewardOutcome> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID

        totalTriggerCount++
        reportAdData(
            "ad_position",
            mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to PositionGet.get(),
                "number" to totalTriggerCount
            )
        )

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
                val error = createAdException("激励广告缓存为空")
                AdResult.Failure(error)
            }

           ADLoadingDialog.hide()


           if(adHolder != null){
               if(activity.isFinishing || activity.isDestroyed){
                   AdLogger.d("页面${activity.javaClass.simpleName}，已关闭")
                   return AdResult.Failure(createAdException("activity is finish"))
               }

               if(activity is LifecycleOwner){
                   val isResume = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                   if(!isResume){
                       if(AppLifecycleManager.isForeground()){
                           AdLogger.d("跳转到了其他页面，不在展示激励广告")
                           return AdResult.Failure(createAdException("leave reward activity"))
                       }
                       AdLogger.d(" Activity ${activity.javaClass.simpleName} not in RESUMED state (current: ${activity.lifecycle.currentState}), waiting for resume...")
                       return suspendCancellableCoroutine { continuation ->
                           val observer = ResumeLifecycleObserver(activity,activity)
                           pendingRequest = PendingShowRequest(
                               adHolder.ad,finalAdUnitId,continuation
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
               val result = showAdInternal(activity, adHolder.ad, finalAdUnitId)

               result
           }else{
               AdResult.Failure(createAdException("广告加载失败"))
           }





        } catch (e: Exception) {
            val error = createAdException("展示异常: ${e.message}", e)
            AdResult.Failure(error)
        }finally {
            ADLoadingDialog.hide()
        }

        return adResult
    }


    private suspend fun showAdInternal(activity: Activity,rewardAd: RewardedAd,finalAdUnitId: String): AdResult<RewardOutcome>{
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
                            "position" to PositionGet.get(),
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
                            "position" to PositionGet.get(),
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
                    val error = createAdException("show failed: ${fullScreenContentError.message}")
                    reportAdData(
                        "ad_show_fail",
                        mapOf(
                            "ad_unit_name" to finalAdUnitId,
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
                    totalShowCount++
                    // 异步预加载下一个广告到缓存（如果缓存未满）
                    if (!isCacheFull(finalAdUnitId)) {
                        AdLogger.d("开屏开始异步预加载下一个广告，广告位ID: %s", finalAdUnitId)
                        PreloadController.preload(activity)
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdLogger.d("原生广告被点击")

                    // 累积点击统计
                    totalClickCount++
                    AdLogger.d("原生广告累积点击次数: $totalClickCount")

                    AdConfigManager.getNativeConfig().recordClick()

                    reportAdData(
                        eventName = "ad_click",
                        params = mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to PositionGet.get(),
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
                                showAdInternal(activity,pending.ad,pending.adUnitId)
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
        val continuation: CancellableContinuation<AdResult<RewardedAds.RewardOutcome>>
    )

}
