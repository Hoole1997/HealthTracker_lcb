package net.corekit.monetize.ads

import android.app.Activity
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.bidding.BiddingAdType
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.interceptor.ClickLimitInterceptor
import net.corekit.monetize.ads.interceptor.GlobalAdSwitchInterceptor
import net.corekit.monetize.ads.interceptor.InterceptorChain
import net.corekit.monetize.ads.interceptor.ShowCountLimitInterceptor
import net.corekit.monetize.ads.interceptor.ShowIntervalLimitInterceptor
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.model.PendingShowRequest
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil
import kotlin.coroutines.resume
import kotlin.math.ceil


/**
 * 开屏广告控制器
 * 专门处理开屏广告的加载和显示，包含广告过期逻辑
 */
class LaunchAds private constructor() {
    
    // 累积点击统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("pdf_e5t3x1n8", 0)

    // 累积关闭统计（持久化）
    private var totalCloseCount by DataStoreIntDelegate("pdf_f9r6w4j2", 0)
    
    // 累积加载次数统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pdf_g3v8h5m7", 0)

    // 累积加载成功次数统计（持久化）
    private var totalLoadSucCount by DataStoreIntDelegate("pdf_h7p2k9s1", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("splash_load_fail_count", 0)

    // 累积展示失败次数统计（持久化）
    private var totalShowFailCount by DataStoreIntDelegate("pdf_i4q6y8z3", 0)
    
    // 累积触发统计（持久化）
    private var totalShowTriggerCount by DataStoreIntDelegate("pdf_j9w1t7r5", 0)
    
    // 累积展示统计（持久化）
    private var totalShowCount by DataStoreIntDelegate("pdf_k2m5x3n6", 0)
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 使用 var 以便每次启动时可以重置
    private var needInterceptor = CompletableDeferred<Boolean>()
    /**
     * 当前待恢复的展示请求
     * 只保存一个请求，新请求会覆盖旧请求
     */
    private var pendingRequest: PendingShowRequest<AppOpenAd>? = null
    
    companion object {
        private const val TAG = "LaunchAds"
        private const val AD_TIMEOUT = 4 * 60 * 60 * 1000L // 4小时过期
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 1
        
        @Volatile
        private var INSTANCE: LaunchAds? = null
        
        fun getInstance(): LaunchAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LaunchAds().also { INSTANCE = it }
            }
        }
    }
    
    private val adCachePool = mutableListOf<CachedAppOpenAd>()
    private val maxCacheSizePerAdUnit = DEFAULT_CACHE_SIZE_PER_AD_UNIT

    // 正在加载中的任务计数
    private val inflightLoads = mutableMapOf<String, Int>()
    
    // 拦截器链
    private val interceptorChain = InterceptorChain(
        interceptors = listOf(
            GlobalAdSwitchInterceptor(),
            ShowCountLimitInterceptor(),
            ShowIntervalLimitInterceptor(),
            ClickLimitInterceptor()
        )
    )

    /**
     * 缓存的开屏广告数据类
     */
    private data class CachedAppOpenAd(
        val ad: AppOpenAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > AD_TIMEOUT
        }
    }

    /**
     * 重置权限拦截器
     * 每次 SplashActivity 启动时调用，确保权限等待逻辑正确工作
     */
    fun resetInterceptor() {
        val old = needInterceptor
        if (!old.isCompleted) {
            AdLogger.d("[权限] 重置权限拦截器（上次未完成，先兜底解除等待）")
            old.complete(true)
        } else {
            AdLogger.d("[权限] 重置权限拦截器（上次已完成）")
        }
        needInterceptor = CompletableDeferred()
    }

    fun cancelInterceptor(){
        AdLogger.d("[权限] 权限授权完成，取消拦截")
        needInterceptor.complete(true)
    }

    /**
     * 等待权限授权完成
     * 用于竞价模式下，确保在展示任何广告前权限已授权完成
     */
    suspend fun awaitPermissionReady() {
        if (needInterceptor.isCompleted) {
            AdLogger.d("[竞价] 权限已完成，无需等待")
            return
        }
        AdLogger.d("[竞价] 等待权限授权完成...")
        needInterceptor.await()
        AdLogger.d("[竞价] 权限授权已完成，可以展示广告")
    }
    
    /**
     * 预加载开屏广告
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun loadInAdvance(context: Context, adUnitId: String? = null): AdResult<Unit> {
        if(!GlobalAdSwitchInterceptor.isGlobalAdEnabled()){
            return AdResult.Failure(
                AdErrorCode.GLOBAL_AD_DISABLED.toAdException())
        }
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_SPLASH_ID

        // 1. 检查是否可加载（防止并发导致的溢出）
        val canLoad = synchronized(adCachePool) {
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
            loadAdToCache(context, finalAdUnitId)
        } finally {
            // 2. 释放加载中的名额
            synchronized(adCachePool) {
                val currentInflight = inflightLoads[finalAdUnitId] ?: 0
                if (currentInflight > 0) {
                    inflightLoads[finalAdUnitId] = currentInflight - 1
                }
            }
        }
    }

    // 正在加载的 Deferred
    private var loadingDeferred: CompletableDeferred<AdResult<Unit>>? = null

    /**
     * 等待广告加载完成
     * @param timeoutMillis 超时时间（毫秒）
     * @return 广告加载结果
     */
    suspend fun waitForAd(timeoutMillis: Long): AdResult<Unit> {
        val deferred = synchronized(this) {
            // 如果已有缓存，直接返回成功
            if (hasCachedAd()) {
                return@synchronized CompletableDeferred(AdResult.Success(Unit))
            }
            // 如果正在加载，返回当前的 deferred
            loadingDeferred
        }

        if (deferred == null) {
            return AdResult.Failure(AdErrorCode.AD_NOT_READY.toAdException())
        }

        return try {
            withTimeoutOrNull(timeoutMillis) {
                deferred.await()
            } ?: AdResult.Failure(AdErrorCode.AD_LOAD_TIMEOUT.toAdException())
        } catch (e: Exception) {
            @Suppress("UNCHECKED_CAST")
            (deferred as? CompletableDeferred<AdResult<Unit>>)?.complete(AdResult.Failure(AdErrorCode.AD_LOAD_INTERRUPTED.toAdException(e)))
            synchronized(this) {
                if (loadingDeferred == deferred) {
                    loadingDeferred = null
                }
            }
            throw e
        }
    }

    suspend fun checkInterceptor(context: Context) = interceptorChain.intercept(context,
        AdConfigManager.getAppOpenConfig())
    
    /**
     * 基础广告加载方法（可复用）
     */
    private suspend fun loadAd(context: Context, adUnitId: String): AppOpenAd? {
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = net.corekit.monetize.ads.frequency.PlatformFrequencyManager.canLoadAd(
            net.corekit.monetize.ads.bidding.BiddingPlatform.ADMOB, 
            net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH
        )
        if (!canLoad) {
            val statusLog = net.corekit.monetize.ads.frequency.PlatformFrequencyManager.getFrequencyStatusLog(
                net.corekit.monetize.ads.bidding.BiddingPlatform.ADMOB, 
                net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH
            )
            AdLogger.w("[$TAG] 加载跳过 | 平台: AdMob | 类型: Splash | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Admob"
            ))
            return null
        }
        
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("开屏广告累积加载次数: $totalLoadCount")
        
        // 创建新的 deferred
        val deferred = CompletableDeferred<AdResult<Unit>>()
        synchronized(this) {
            loadingDeferred = deferred
        }

        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )
        
        return try {
            suspendCancellableCoroutine { continuation ->
                val startTime = System.currentTimeMillis()
    
                val adRequest = AdRequest.Builder().build()

                val loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.d("开屏广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                        totalLoadSucCount++
                        
                        // 完成 deferred
                        deferred.complete(AdResult.Success(Unit))
                        synchronized(this@LaunchAds) {
                            if (loadingDeferred == deferred) {
                                loadingDeferred = null
                            }
                        }

                        reportAdData(
                            eventName = "ad_loaded",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadSucCount,
                                "ad_source" to (ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            )
                        )
                        FpuController.onAdFill("SP")
                        if (continuation.isActive) continuation.resume(ad)
                    }
    
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        totalLoadFailCount++
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("开屏广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", adUnitId, loadTime, loadAdError.message)
                        
                        // 失败 deferred
                        deferred.complete(AdResult.Failure(AdException(loadAdError.code, loadAdError.message)))
                        synchronized(this@LaunchAds) {
                            if (loadingDeferred == deferred) {
                                loadingDeferred = null
                            }
                        }

                        reportAdData(
                            eventName = "ad_load_fail",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadFailCount,
                                "ad_source" to (loadAdError.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                "reason" to loadAdError.message
                            )
                        )
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
    
                // 启动广告加载
                AppOpenAd.load(context, adUnitId, adRequest, loadCallback)
            }
        } catch (e: Exception) {
            deferred.complete(AdResult.Failure(AdErrorCode.AD_LOAD_EXCEPTION.toAdException(e)))
            synchronized(this) {
                if (loadingDeferred == deferred) {
                    loadingDeferred = null
                }
            }
            null
        }
    }

    /**
     * 加载广告到缓存
     */
    private suspend fun loadAdToCache(context: Context, adUnitId: String): AdResult<Unit> {
        return try {
            // 加载广告
            val appOpenAd = loadAd(context, adUnitId)
            if (appOpenAd != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedAppOpenAd(appOpenAd, adUnitId))
                    val currentCount = getCachedAdCount(adUnitId)
                    AdLogger.d("开屏广告加载成功并缓存，广告位ID: %s，该广告位缓存数量: %d/%d", adUnitId, currentCount, maxCacheSizePerAdUnit)
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(AdErrorCode.AD_LOAD_FAILED.toAdException())
            }
        } catch (e: Exception) {
            AdLogger.e("开屏loadAdToCache异常", e)
            AdResult.Failure(AdErrorCode.AD_LOAD_EXCEPTION.toAdException(e))
        }
    }
    
    /**
     * 显示开屏广告（自动处理加载和过期检查）
     * @param activity Activity上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun displayAd(activity: Activity, position: String, adUnitId: String = BuildConfig.ADMOB_SPLASH_ID, onLoaded:((isSuc: Boolean)->Unit) ?= null, onShow: (() -> Unit)? = null): AdResult<Unit> {

        AdsManager.awaitInitialized()
        // 累积触发广告展示次数统计
        totalShowTriggerCount++
        AdLogger.d("开屏广告累积触发展示次数: $totalShowTriggerCount")

        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to (adUnitId ?: ""),
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.ADMOB, BiddingAdType.SPLASH)) {
            totalShowFailCount++
            AdLogger.w("开屏广告展示失败 | 位置: %s | 原因: 平台频控拦截 | 累计失败: %d", position, totalShowFailCount)
            onLoaded?.invoke(false)
            reportAdData(
                eventName = "ad_show_error",
                params = mapOf(
                    "ad_unit_name" to (adUnitId ?: ""),
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit",
                )
            )
            return AdResult.Failure(AdErrorCode.AD_SHOW_FAILED.toAdException("platform_frequency_limit"))
        }
        
        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(activity, AdConfigManager.getAppOpenConfig())) {
            is AdResult.Failure -> {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("开屏广告累积展示失败次数: $totalShowFailCount")
                onLoaded?.invoke(false)
                reportAdData(
                    eventName = "ad_show_error",
                    params = mapOf(
                        "ad_unit_name" to (adUnitId ?: ""),
                        "position" to position,
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message,
                    )
                )
                return if(AdConfigManager.shouldShowInterstitialAfterAppOpenFailure()){
                      AdsManager.Controllers.interstitial.displayAd(
                        activity,
                        position,
                        BuildConfig.ADMOB_INTERSTITIAL_ID
                    )
                } else interceptResult
            }
            else -> { /* continue */ }
        }
        
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_SPLASH_ID
        val adResult = try {
            // 1. 尝试从缓存获取广告
            var cachedAd = getCachedAd(finalAdUnitId)

            // 2. 如果缓存为空，立即加载并缓存一个广告
            if (cachedAd == null) {
                AdLogger.d("缓存为空，立即加载开屏广告，广告位ID: %s", finalAdUnitId)
                loadAdToCache(activity, finalAdUnitId)
                cachedAd = getCachedAd(finalAdUnitId)
            }

            if (cachedAd != null) {
                AdLogger.d("使用缓存中的开屏广告，广告位ID: %s", finalAdUnitId)
                onLoaded?.invoke(true)
                if(BuildState.debug) "准备执行开屏拦截等待".logd("PermissionManager")
                needInterceptor.await()
                if(BuildState.debug) "开屏拦截等待结束".logd("PermissionManager")
                if(activity.isFinishing || activity.isDestroyed ){
                    AdLogger.d("页面${activity.javaClass.simpleName}，已关闭")
                   return AdResult.Failure(AdErrorCode.ACTIVITY_FINISHING.toAdException())
                }

                if(activity is LifecycleOwner){
                   val isResume = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if(!isResume){
                        AdLogger.d(" Activity ${activity.javaClass.simpleName} not in RESUMED state (current: ${activity.lifecycle.currentState}), waiting for resume...")
                        return suspendCancellableCoroutine { continuation ->
                            val observer = ResumeLifecycleObserver(activity,activity)
                            pendingRequest = PendingShowRequest(
                                cachedAd.ad,finalAdUnitId,position,continuation, onShow
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
                val result = showAdInternal(activity, cachedAd.ad, finalAdUnitId, position, onShow)

                result

            } else {
                onLoaded?.invoke(false)
                AdResult.Failure(AdErrorCode.AD_LOAD_FAILED.toAdException())
            }
        } catch (e: Exception) {
            AdLogger.e("显示开屏广告异常", e)
            AdResult.Failure(AdErrorCode.AD_SHOW_EXCEPTION.toAdException(e))
        }

        return if(adResult is AdResult.Failure && AdConfigManager.shouldShowInterstitialAfterAppOpenFailure()){
            AdsManager.Controllers.interstitial.displayAd(
                activity,
                position,
                BuildConfig.ADMOB_INTERSTITIAL_ID
            )
        } else adResult
    }
    
    /**
     * 从缓存获取广告
     */
    private fun getCachedAd(adUnitId: String): CachedAppOpenAd? {
        synchronized(adCachePool) {
            val index = adCachePool.indexOfFirst { it.adUnitId == adUnitId && !it.isExpired() }
            return if (index != -1) {
                adCachePool.removeAt(index)
            } else {
                null
            }
        }
    }
    
    /**
     * 获取指定广告位的缓存数量
     */
    private fun getCachedAdCount(adUnitId: String): Int {
        synchronized(adCachePool) {
            return adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
        }
    }

    /**
     * 查看缓存中的广告（不移除）
     * 用于获取价格进行竞价
     * @param adUnitId 广告位ID
     * @return 缓存的广告对象，如果不存在或已过期返回null
     */
    fun peekCachedAd(adUnitId: String? = null): AppOpenAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_SPLASH_ID
        synchronized(adCachePool) {
            return adCachePool.firstOrNull { it.adUnitId == finalAdUnitId && !it.isExpired() }?.ad
        }
    }

    /**
     * 获取当前缓存广告的价格（用于竞价）
     * 如果缓存不存在则调用加载，使用反射获取价格后返回
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     * @return 广告价格（已除以1000000转换为美元），如果获取失败返回null
     */
    suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_SPLASH_ID

        // 尝试从缓存获取广告（不移除）
        val cachedAd = peekCachedAd(finalAdUnitId)

        if (cachedAd == null) {
            AdLogger.w("[竞价] 获取开屏广告价格失败：缓存为空")
            return null
        }

        // 使用反射获取价格（避免主线程执行反射）
        val adValue = withContext(Dispatchers.Default) {
            AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd)
        }

        return if (adValue != null) {
            val price = adValue.valueMicros / 1_000_000.0
            AdLogger.d("[竞价] 获取开屏广告价格成功: %.6f %s (精度: %s)", price, adValue.currencyCode, adValue.precisionType)
            price
        } else {
            AdLogger.w("[竞价] 获取开屏广告价格失败：反射获取AdValue为空")
            null
        }
    }

    /**
     * 检查是否有可用的缓存广告
     * @param adUnitId 广告位ID
     * @return true 如果有可用的缓存广告
     */
    fun hasCachedAd(adUnitId: String? = null): Boolean {
        return peekCachedAd(adUnitId) != null
    }
    
    /**
     * 检查指定广告位缓存是否已满
     */
    private fun isCacheFull(adUnitId: String): Boolean {
        return getCachedAdCount(adUnitId) >= maxCacheSizePerAdUnit
    }
    
    /**
     * 显示广告的内部实现
     */
    private suspend fun showAdInternal(activity: Activity, appOpenAd: AppOpenAd, adUnitId: String, position: String, onShow: (() -> Unit)? = null): AdResult<Unit> {
        return suspendCancellableCoroutine { continuation ->
            // 临时变量保存收益数据
            var currentAdValue: AdValue? = null

            appOpenAd.onPaidEventListener = OnPaidEventListener { value ->
                AdLogger.d("开屏广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                // 保存到临时变量
                currentAdValue = value

                reportAdData(
                    eventName = "ad_impression",
                    params = mapOf(
                        "ad_unit_name" to adUnitId,
                        "position" to position,
                        "number" to totalShowCount,
                        "ad_source" to (appOpenAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                        "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                        "currency" to (currentAdValue?.currencyCode ?: "")
                    )
                )

                // 上报真实的广告收益数据
                reportAdRevenueWithValue(appOpenAd, adUnitId, value)

                IpuController.onAdImpression("SP", value.valueMicros)
                RpuController.onAdRevenue("SP", value.valueMicros)
            }

            appOpenAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    totalCloseCount ++
                    AdLogger.d("开屏广告关闭")
                    reportAdData(
                        eventName = "ad_dismiss",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalCloseCount,
                            "ad_source" to (appOpenAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                            "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )
                    val result = AdResult.Success(Unit)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    AdLogger.w("开屏广告显示失败: %s", adError.message)
                    totalShowFailCount++
                    reportAdData(
                        eventName = "ad_show_error",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalShowFailCount,
                            "reason" to adError.message
                        )
                    )
                    val result = AdResult.Failure(AdErrorCode.AD_SHOW_FAILED.toAdException("Show failed: ${adError.message}"))
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    AdLogger.d("开屏广告开始显示")

                    // 累积展示统计
                    totalShowCount++
                    AdLogger.d("开屏广告累积展示次数: $totalShowCount")
                    onShow?.invoke()

                    AdConfigManager.getAppOpenConfig().recordShow()
                }

                override fun onAdClicked() {
                    AdLogger.d("开屏广告被点击")

                    // 累积点击统计
                    totalClickCount++
                    AdLogger.d("开屏广告累积点击次数: $totalClickCount")
                    AdLogger.d("开屏广告点击时收益数据: ${if (currentAdValue != null) "value=${currentAdValue.valueMicros}, currency=${currentAdValue.currencyCode}" else "暂无收益数据"}")

                    AdConfigManager.getAppOpenConfig().recordClick()
                    PlatformFrequencyManager.recordClick(BiddingPlatform.ADMOB, BiddingAdType.SPLASH)
                    reportAdData(
                        eventName = "ad_click",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to position,
                            "number" to totalClickCount,
                            "ad_source" to (appOpenAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                            "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                }

                override fun onAdImpression() {
                    AdLogger.d("开屏广告展示完成")

                    // 异步预加载下一个广告到缓存（如果缓存未满）
                    if (!isCacheFull(adUnitId)) {
                        AdLogger.d("开屏开始异步预加载下一个广告，广告位ID: %s", adUnitId)
                        PreloadController.preloadPlatformAdType(activity, net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB, net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH)
                    }
                }

            }
            appOpenAd.show(activity)
        }
    }

    /**
     * 上报广告收益数据（使用真实收益值）
     * @param appOpenAd 开屏广告对象
     * @param adValue 广告收益值
     */
    private fun reportAdRevenueWithValue(appOpenAd: AppOpenAd,adUnitId: String, adValue: AdValue) {
        // 创建广告收益数据
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = appOpenAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = appOpenAd.responseInfo?.loadedAdapterResponseInfo?.adSourceInstanceName.orEmpty(),
            adFormat = "Splash"
        )

        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("开屏广告真实收益数据已上报，广告位ID: ${adUnitId}, 收益: ${adValue.valueMicros}微元 ${adValue.currencyCode}")
    }

    
    /**
     * 获取缓存状态
     */
    fun getCacheStatus(adUnitId: String? = null): net.corekit.monetize.ads.log.BiddingLogger.CacheEntry {
        val finalAdUnitId = adUnitId ?: net.corekit.monetize.BuildConfig.ADMOB_SPLASH_ID
        return net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
            adType = "Splash",
            platform = "AdMob",
            adUnitId = finalAdUnitId,
            currentCount = getCachedAdCount(finalAdUnitId),
            maxCount = maxCacheSizePerAdUnit
        )
    }

    /**
     * 销毁广告
     */
    fun releaseAd() {
        synchronized(adCachePool) {
            adCachePool.clear()
        }
        AdLogger.d("开屏广告已销毁")
    }
    
    /**
     * 销毁控制器
     */
    fun cleanup() {
        releaseAd()
        AdLogger.d("开屏广告控制器已清理")
    }
    
    /**
     * 创建广告异常
     */
    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(
            code = 0,
            message = message,
            cause = cause
        )
    }
    
    /**
     * 通用数据上报函数
     * @param eventName 事件名称
     * @param params 参数Map，会与基础参数合并
     */
    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to "Splash"
        )
        
        // 直接合并传入的参数
        data.putAll(params)

        if(eventName == "ad_impression"){
            ReportDataManager.reportDataByName("ThinkingData",eventName, data)
        } else{
            ReportDataManager.reportData(eventName, data)
        }
    }


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
                                showAdInternal(activity,pending.ad,pending.adUnitId,pending.position, pending.onShow)
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
            if (pendingRequest != null) {
                AdLogger.d("[OpenAd] Activity destroyed while waiting for resume, cancelling pending request")
                if (pendingRequest?.continuation?.isActive == true) {
                    pendingRequest?.continuation?.resume(AdResult.Failure(createAdException("Activity destroyed")))
                }
                pendingRequest = null
            }
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    }
} 
