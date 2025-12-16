package net.corekit.monetize.ads

import android.app.Activity
import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
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
import net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil
import net.corekit.monetize.ui.FullScreenNativeAdActivity
import net.corekit.monetize.ui.dialog.ADLoadingDialog
import net.corekit.monetize.util.PositionGet
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * 插页广告控制器
 */
class InterstitialAds private constructor() {
    
    // 累积点击统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("pdf_j5k2m9x6", 0)

    // 累积关闭统计（持久化）
    private var totalCloseCount by DataStoreIntDelegate("pdf_k7n1p4v8", 0)
    
    // 累积加载次数统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pdf_l4q8r6w3", 0)

    // 累积加载成功次数统计（持久化）
    private var totalLoadSucCount by DataStoreIntDelegate("pdf_m9s3t7y5", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("inter_load_fail_count", 0)

    // 累积展示失败次数统计（持久化）
    private var totalShowFailCount by DataStoreIntDelegate("pdf_n2w6z1j8", 0)
    
    // 累积触发统计（持久化）
    private var totalShowTriggerCount by DataStoreIntDelegate("pdf_o6x4h9k2", 0)
    
    // 累积展示统计（持久化）
    private var totalShowCount by DataStoreIntDelegate("pdf_p1y7m5q3", 0)
    
    // 当前广告的收益信息（临时存储）
    private var currentAdValue: AdValue? = null
    
    // 插页广告是否正在显示的标识
    private var isShowing: Boolean = false
    
    companion object {
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2

        @Volatile
        private var INSTANCE: InterstitialAds? = null

        fun getInstance(): InterstitialAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InterstitialAds().also { INSTANCE = it }
            }
        }
    }

    // 内存缓存池 - 存储预加载的广告
    private val adCachePool = mutableListOf<CachedInterstitialAd>()
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
     * 缓存的插页广告数据类
     */
    private data class CachedInterstitialAd(
        val ad: InterstitialAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > 1 * 60 * 60 * 1000L
        }
    }

    /**
     * 预加载广告
     */
    suspend fun loadInAdvance(context: Context, adUnitId: String? = null): AdResult<Unit> {
        if(!GlobalAdSwitchInterceptor.isGlobalAdEnabled()){
            return AdResult.Failure(
                AdException(
                    code = -100,
                    message = "开屏全局广告已关闭，中断加载"
                ))
        }
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_INTERSTITIAL_ID
        return loadAdToCache(context, finalAdUnitId)
    }

    /**
     * 显示广告
     */
    suspend fun displayAd(activity: Activity, adUnitId: String? = null,ignoreFullNative: Boolean  = false): AdResult<Unit> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_INTERSTITIAL_ID
        
        // 累积触发统计
        totalShowTriggerCount++
        AdLogger.d("插页广告累积触发展示次数: $totalShowTriggerCount")
        
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to PositionGet.get(),
                "number" to totalShowTriggerCount
            )
        )
        
        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(activity, AdConfigManager.getInterstitialConfig())) {
            is AdResult.Failure -> {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("插页广告累积展示失败次数: $totalShowFailCount")
                
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to PositionGet.get(),
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message
                    )
                )
                
                return interceptResult
            }
            else -> { /* continue */ }
        }

        // 是否加载全屏原生
        val interval = AdConfigManager.getFullscreenNativeAfterInterstitialCount()
        val todayShowInter = AdConfigManager.getInterstitialConfig().getDailyShowCount()
        val needShowNativeFull = interval > 0 && todayShowInter > 0 && todayShowInter % interval == 0
        AdLogger.d("当日已展示${todayShowInter}个插页，每显示${interval}个插页将显示原生，下一个是否显示全屏原生${needShowNativeFull}")

        if(!ignoreFullNative && needShowNativeFull && FullNativeAds.getInstance().checkCachedAdAvailable()){
            return FullScreenNativeAdActivity.start(activity,showInterstitial = true)
        }

        return try {
            // 1. 尝试从缓存获取广告
            var cachedAd = getCachedAd(finalAdUnitId)

            // 2. 如果缓存为空，立即加载并缓存一个广告
            if (cachedAd == null) {
                // 插页阻塞loading
                ADLoadingDialog.show(activity)
                AdLogger.d("缓存为空，立即加载插页广告，广告位ID: %s", finalAdUnitId)
                loadAdToCache(activity, finalAdUnitId)
                cachedAd = getCachedAd(finalAdUnitId)
            }

            if (cachedAd != null) {
                ADLoadingDialog.hide()
                AdLogger.d("使用缓存中的插页广告，广告位ID: %s", finalAdUnitId)

                // 3. 显示广告
                val result = showAdInternal(activity, cachedAd.ad, finalAdUnitId)

                result
            } else {
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("显示插页广告异常", e)
            AdResult.Failure(createAdException("显示广告异常: ${e.message}", e))
        } finally {
            ADLoadingDialog.hide()
        }
    }

    /**
     * 基础广告加载方法（可复用）
     */
    private suspend fun loadAd(context: Context, adUnitId: String): InterstitialAd? {
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("插页广告累积加载次数: $totalLoadCount")
        
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
                 // 7秒超时
                .build()

            InterstitialAd.load(adRequest, object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.d("插页广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
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
                    FpuController.onAdFill("IV")
                    
                    continuation.resume(ad)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    totalLoadFailCount++
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("插页广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", adUnitId, loadTime, adError.message)
                    
                    reportAdData(
                        eventName = "ad_load_fail",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadFailCount,
                            "ad_source" to (adError.responseInfo?.loadedAdSourceResponseInfo?.name.orEmpty()),
                            "pass_time" to ceil(loadTime / 1000.0).toInt(),
                            "reason" to adError.message
                        )
                    )
                    
                    continuation.resume(null)
                }
            })
        }
    }

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
            val interstitialAd = loadAd(context.applicationContext, adUnitId)
            if (interstitialAd != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedInterstitialAd(interstitialAd, adUnitId))
                    val currentCount = getCachedAdCount(adUnitId)
                    AdLogger.d("插页广告加载成功并缓存，广告位ID: %s，该广告位缓存数量: %d/%d", adUnitId, currentCount, maxCacheSizePerAdUnit)
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("插页loadAdToCache异常", e)
            AdResult.Failure(AdException(0, "加载异常: ${e.message}", e))
        }
    }

    /**
     * 从缓存获取广告
     */
    private fun getCachedAd(adUnitId: String): CachedInterstitialAd? {
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
     * 查看缓存中的广告（不移除）
     * 用于获取价格进行竞价
     * @param adUnitId 广告位ID
     * @return 缓存的广告对象，如果不存在或已过期返回null
     */
    fun peekCachedAd(adUnitId: String? = null): InterstitialAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_INTERSTITIAL_ID
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
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_INTERSTITIAL_ID

        // 尝试从缓存获取广告（不移除）
        var cachedAd = peekCachedAd(finalAdUnitId)

        // 如果缓存为空，立即加载
        if (cachedAd == null) {
            AdLogger.d("[竞价] 获取插屏价格时缓存为空，立即加载，广告位ID: %s", finalAdUnitId)
            loadAdToCache(context, finalAdUnitId)
            cachedAd = peekCachedAd(finalAdUnitId)
        }

        if (cachedAd == null) {
            AdLogger.w("[竞价] 获取插屏广告价格失败：缓存为空")
            return null
        }

        // 使用反射获取价格
        val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd)

        return if (adValue != null) {
            val price = adValue.valueMicros / 1_000_000.0
            AdLogger.d("[竞价] 获取插屏广告价格成功: %.6f %s (精度: %s)", price, adValue.currencyCode, adValue.precisionType)
            price
        } else {
            AdLogger.w("[竞价] 获取插屏广告价格失败：反射获取AdValue为空")
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
     * 显示广告的内部实现
     */
    private suspend fun showAdInternal(activity: Activity, interstitialAd: InterstitialAd, adUnitId: String): AdResult<Unit> {
        return suspendCancellableCoroutine { continuation ->
            interstitialAd.adEventCallback = object : InterstitialAdEventCallback{
                override fun onAdDismissedFullScreenContent() {
                    AdLogger.d("插页广告关闭")
                    
                    // 设置广告不再显示标识
                    isShowing = false
                    
                    totalCloseCount++
                    
                    reportAdData(
                        eventName = "ad_close",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to PositionGet.get(),
                            "number" to totalCloseCount,
                            "ad_source" to (interstitialAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                    interstitialAd.destroy()
                    val result = AdResult.Success(Unit)
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                    AdLogger.w("插页广告显示失败: %s", fullScreenContentError.message)

                    // 累积展示失败次数统计
                    totalShowFailCount++
                    AdLogger.d("插页广告累积展示失败次数: $totalShowFailCount")

                    reportAdData(
                        eventName = "ad_show_fail",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to PositionGet.get(),
                            "number" to totalShowFailCount,
                            "ad_source" to (interstitialAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "reason" to fullScreenContentError.message
                        )
                    )
                    interstitialAd.destroy()

                    val result = AdResult.Failure(createAdException("显示失败: ${fullScreenContentError.message}"))
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }



                override fun onAdShowedFullScreenContent() {
                    AdLogger.d("插页广告开始显示")
                    
                    AdConfigManager.getInterstitialConfig().recordShow()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AdLogger.d("插页广告被点击")
                    
                    // 累积点击统计
                    totalClickCount++
                    AdLogger.d("插页广告累积点击次数: $totalClickCount")
                    
                    AdConfigManager.getInterstitialConfig().recordClick()
                    
                    reportAdData(
                        eventName = "ad_click",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to PositionGet.get(),
                            "number" to totalClickCount,
                            "ad_source" to (interstitialAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    AdLogger.d("插页广告展示完成")
                    
                    // 设置广告正在显示标识
                    isShowing = true

                    // 累积展示统计
                    totalShowCount++
                    AdLogger.d("插页广告累积展示次数: $totalShowCount")

                    // 异步预加载下一个广告到缓存（如果缓存未满）
                    if (!isCacheFull(adUnitId)) {
                        PreloadController.preload(activity)
                    }
                }

                override fun onAdPaid(value: AdValue) {
                    super.onAdPaid(value)
                    AdLogger.d("插页广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                    // 存储当前广告的收益信息
                    currentAdValue = value

                    reportAdData(
                        eventName = "ad_impression",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "position" to PositionGet.get(),
                            "number" to totalShowCount,
                            "ad_source" to (interstitialAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 }
                                ?: 0.0),
                            "currency" to (currentAdValue?.currencyCode ?: "")
                        )
                    )

                    // 上报真实的广告收益数据
                    reportAdRevenueWithValue(interstitialAd, adUnitId,value)

                    IpuController.onAdImpression("IV", value.valueMicros)
                    RpuController.onAdRevenue("IV", value.valueMicros)
                }
            }

            interstitialAd.show(activity)
        }
    }

    /**
     * 销毁广告
     */
    fun releaseAd() {
        synchronized(adCachePool) {
            adCachePool.clear()
        }
        AdLogger.d("插页广告已销毁")
    }
    
    /**
     * 上报广告收益数据（使用真实收益值）
     * @param interstitialAd 插页广告对象
     * @param adValue 广告收益值
     */
    private fun reportAdRevenueWithValue(interstitialAd: InterstitialAd,adUnitId: String, adValue: AdValue) {
        // 创建广告收益数据
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = interstitialAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = interstitialAd.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "Interstitial"
        )
        
        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("插页广告真实收益数据已上报，广告位ID: ${adUnitId}, 收益: ${adValue.valueMicros}微元 ${adValue.currencyCode}")
    }

    /**
     * 销毁控制器
     */
    fun cleanup() {
        releaseAd()
        AdLogger.d("插页广告控制器已清理")
    }

    /**
     * 通用数据上报函数
     * @param eventName 事件名称
     * @param params 参数Map，会与基础参数合并
     */
    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to "Interstitial"
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
     * 获取插页广告是否正在显示的状态
     * @return true 如果插页广告正在显示，false 否则
     */
    fun checkAdShowing(): Boolean {
        return isShowing
    }
} 