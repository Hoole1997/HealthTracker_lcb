package net.corekit.monetize.ads

import android.app.Activity
import android.content.Context
import com.blankj.utilcode.util.ActivityUtils
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.remax.bill.ads.report.IpuController
import com.remax.bill.ads.report.RpuController
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.interceptor.ClickLimitInterceptor
import net.corekit.monetize.ads.interceptor.GlobalAdSwitchInterceptor
import net.corekit.monetize.ads.interceptor.InterceptorChain
import net.corekit.monetize.ads.interceptor.ShowCountLimitInterceptor
import net.corekit.monetize.ads.interceptor.ShowIntervalLimitInterceptor
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
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
    
    // 累积展示失败次数统计（持久化）
    private var totalShowFailCount by DataStoreIntDelegate("pdf_i4q6y8z3", 0)
    
    // 累积触发统计（持久化）
    private var totalShowTriggerCount by DataStoreIntDelegate("pdf_j9w1t7r5", 0)
    
    // 累积展示统计（持久化）
    private var totalShowCount by DataStoreIntDelegate("pdf_k2m5x3n6", 0)
    
    companion object {
        private const val TAG = "LaunchAds"
        private const val AD_TIMEOUT = 4 * 60 * 60 * 1000L // 4小时过期
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2
        
        @Volatile
        private var INSTANCE: LaunchAds? = null
        
        fun getInstance(): LaunchAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LaunchAds().also { INSTANCE = it }
            }
        }
    }
    
    // 内存缓存池 - 存储预加载的广告
    private val adCachePool = mutableListOf<CachedAppOpenAd>()
    private val maxCacheSizePerAdUnit = DEFAULT_CACHE_SIZE_PER_AD_UNIT
    
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
     * 预加载开屏广告
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun loadInAdvance(context: Context, adUnitId: String? = null): AdResult<Unit> {
        if(!GlobalAdSwitchInterceptor.isGlobalAdEnabled()){
            return AdResult.Failure(
                AdException(
                    code = -100,
                    message = "开屏全局广告已关闭，中断加载"
                ))
        }
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_SPLASH_ID
        return loadAdToCache(context, finalAdUnitId)
    }

    /**
     * 基础广告加载方法（可复用）
     */
    private suspend fun loadAd(context: Context, adUnitId: String): AppOpenAd? {
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("开屏广告累积加载次数: $totalLoadCount")
        
        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )
        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()

            val adRequest = AdRequest.Builder(adUnitId)
                .build()

            val loadCallback = object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.d("开屏广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                    totalLoadSucCount++
                    reportAdData(
                        eventName = "ad_loaded",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "pass_time" to ceil(loadTime / 1000.0).toInt()
                        )
                    )
                    FpuController.onAdFill("SP")
                    continuation.resume(ad)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("开屏广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", adUnitId, loadTime, loadAdError.message)
                    reportAdData(
                        eventName = "ad_load_fail",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to (loadAdError.responseInfo?.loadedAdSourceResponseInfo?.name.orEmpty()),
                            "pass_time" to ceil(loadTime / 1000.0).toInt(),
                            "reason" to loadAdError.message
                        )
                    )
                    continuation.resume(null)
                }
            }

            // 启动广告加载
            AppOpenAd.load(adRequest, loadCallback)
        }
    }

    suspend fun checkInterceptor(context: Context) = interceptorChain.intercept(context,
        AdConfigManager.getAppOpenConfig())
    
    /**
     * 加载广告到缓存
     */
    private suspend fun loadAdToCache(context: Context, adUnitId: String): AdResult<Unit> {
        return try {
            
            // 检查缓存是否已满
            val currentAdUnitCount = adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
            if (currentAdUnitCount >= maxCacheSizePerAdUnit) {
                AdLogger.w("广告位 %s 缓存已满，当前缓存: %d/%d", adUnitId, currentAdUnitCount, maxCacheSizePerAdUnit)
                return AdResult.Success(Unit)
            }
            
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
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("开屏loadAdToCache异常", e)
            AdResult.Failure(AdException(0, "加载异常: ${e.message}", e))
        }
    }
    
    /**
     * 显示开屏广告（自动处理加载和过期检查）
     * @param activity Activity上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun displayAd(activity: Activity, adUnitId: String = BuildConfig.ADMOB_SPLASH_ID, onLoaded:((isSuc: Boolean)->Unit) ?= null): AdResult<Unit> {
        // 累积触发广告展示次数统计
        totalShowTriggerCount++
        AdLogger.d("开屏广告累积触发展示次数: $totalShowTriggerCount")

        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to (adUnitId ?: ""),
                "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                "number" to totalShowTriggerCount
            )
        )
        
        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(activity, AdConfigManager.getAppOpenConfig())) {
            is AdResult.Failure -> {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("开屏广告累积展示失败次数: $totalShowFailCount")
                onLoaded?.invoke(false)
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to (adUnitId ?: ""),
                        "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message,
                    )
                )
                return if(AdConfigManager.shouldShowInterstitialAfterAppOpenFailure()){
                      AdsManager.Controllers.interstitial.displayAd(
                        activity,
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

                // 3. 显示广告
                val result = showAdInternal(activity, cachedAd.ad, finalAdUnitId)

                result
            } else {
                onLoaded?.invoke(false)
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("显示开屏广告异常", e)
            AdResult.Failure(createAdException("显示广告异常: ${e.message}", e))
        }

        return if(adResult is AdResult.Failure && AdConfigManager.shouldShowInterstitialAfterAppOpenFailure()){
            AdsManager.Controllers.interstitial.displayAd(
                activity,
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
     * 检查指定广告位缓存是否已满
     */
    private fun isCacheFull(adUnitId: String): Boolean {
        return getCachedAdCount(adUnitId) >= maxCacheSizePerAdUnit
    }
    
    /**
     * 显示广告的内部实现
     */
    private suspend fun showAdInternal(activity: Activity, appOpenAd: AppOpenAd, adUnitId: String): AdResult<Unit> {
        return suspendCancellableCoroutine { continuation ->
            // 临时变量保存收益数据
            var currentAdValue: AdValue? = null
            
            // 设置收益监听器
            appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdPaid(value: AdValue) {
                    AdLogger.d("开屏广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                    // 保存到临时变量
                    currentAdValue = value

                    reportAdData(
                        eventName = "ad_impression",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                            "number" to totalShowCount,
                            "ad_source" to (appOpenAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                    // 上报真实的广告收益数据
                    reportAdRevenueWithValue(appOpenAd, adUnitId,value)

                    IpuController.onAdImpression("SP", value.valueMicros)
                    RpuController.onAdRevenue("SP", value.valueMicros)
                }

                override fun onAdDismissedFullScreenContent() {
                    totalCloseCount ++
                    AdLogger.d("开屏广告关闭")
                    reportAdData(
                        eventName = "ad_close",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                            "number" to totalCloseCount,
                            "ad_source" to (appOpenAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )
                    val result = AdResult.Success(Unit)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    AdLogger.w("开屏广告显示失败: %s", fullScreenContentError.message)
                    totalShowFailCount++
                    reportAdData(
                        eventName = "ad_show_fail",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                            "number" to totalShowFailCount,
                            "reason" to fullScreenContentError.message
                        )
                    )
                    val result = AdResult.Failure(createAdException("显示失败: ${fullScreenContentError.message}"))
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onAdShowedFullScreenContent() {
                    AdLogger.d("开屏广告开始显示")

                    // 累积展示统计
                    totalShowCount++
                    AdLogger.d("开屏广告累积展示次数: $totalShowCount")

                    AdConfigManager.getAppOpenConfig().recordShow()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdLogger.d("开屏广告被点击")

                    // 累积点击统计
                    totalClickCount++
                    AdLogger.d("开屏广告累积点击次数: $totalClickCount")
                    AdLogger.d("开屏广告点击时收益数据: ${if (currentAdValue != null) "value=${currentAdValue.valueMicros}, currency=${currentAdValue.currencyCode}" else "暂无收益数据"}")

                    AdConfigManager.getAppOpenConfig().recordClick()
                    reportAdData(
                        eventName = "ad_click",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to ActivityUtils.getTopActivity()::class.java.simpleName,
                            "number" to totalClickCount,
                            "ad_source" to (appOpenAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    AdLogger.d("开屏广告展示完成")

                    // 异步预加载下一个广告到缓存（如果缓存未满）
                    if (!isCacheFull(adUnitId)) {
                        AdLogger.d("开屏开始异步预加载下一个广告，广告位ID: %s", adUnitId)
                        PreloadController.preload(activity)
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
            adRevenueNetwork = appOpenAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = appOpenAd.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "Splash"
        )

        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("开屏广告真实收益数据已上报，广告位ID: ${adUnitId}, 收益: ${adValue.valueMicros}微元 ${adValue.currencyCode}")
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
} 