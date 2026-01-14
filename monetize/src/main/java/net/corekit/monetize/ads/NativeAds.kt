package net.corekit.monetize.ads

import android.content.Context
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import net.corekit.monetize.ui.NativeAdStyle
import net.corekit.monetize.ui.NativeAdView
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * 原生广告控制器
 * 提供原生广告的加载和管理功能
 */
class NativeAds private constructor() {
    
    // 累积点击统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("pdf_c5h8t4p1", 0)

    // 累积关闭统计（持久化）
    private var totalCloseCount by DataStoreIntDelegate("pdf_d6r3v9q2", 0)
    
    // 累积加载次数统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pdf_e9s7w2k4", 0)

    // 累积加载成功次数统计（持久化）
    private var totalLoadSucCount by DataStoreIntDelegate("pdf_f1m4x8n5", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("native_load_fail_count", 0)
    
    // 累积展示失败次数统计（持久化）
    private var totalShowFailCount by DataStoreIntDelegate("pdf_g3p6y1j7", 0)
    
    // 累积触发统计（持久化）
    private var totalShowTriggerCount by DataStoreIntDelegate("pdf_h8q5z3r9", 0)
    
    // 累积展示统计（持久化）
    private var totalShowCount by DataStoreIntDelegate("pdf_i2t9w7s4", 0)
    
    // 当前广告的收益信息（临时存储）
    private var currentAdValue: AdValue? = null
    
    companion object {
        private const val TAG = "NativeAds"
        private const val AD_TIMEOUT = 1 * 60 * 60 * 1000L
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2
        
        @Volatile
        private var INSTANCE: NativeAds? = null
        
        fun getInstance(): NativeAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NativeAds().also { INSTANCE = it }
            }
        }
    }
    
    // 内存缓存池 - 存储预加载的广告
    private val adCachePool = mutableListOf<CachedNativeAd>()
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
    
    private val nativeAdView = NativeAdView()
    
    // 状态流
    private val _loadingState = MutableStateFlow<AdResult<NativeAd>>(AdResult.Loading)
    val loadingState: StateFlow<AdResult<NativeAd>> = _loadingState.asStateFlow()
    
    /**
     * 缓存的原生广告数据类
     */
    private data class CachedNativeAd(
        val ad: NativeAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > AD_TIMEOUT
        }
    }
    
    /**
     * 预加载原生广告（可选，用于提前准备）
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
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_NATIVE_ID
        return loadAdToCache(context, finalAdUnitId)
    }
    
    /**
     * 获取原生广告（自动处理加载）
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun retrieveAd(context: Context, adUnitId: String? = null,style: NativeAdStyle = NativeAdStyle.STANDARD): AdResult<NativeAd> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_NATIVE_ID
        
        // 1. 尝试从缓存获取广告
        var cachedAd = getCachedAd(finalAdUnitId)
        
        // 2. 如果缓存为空，立即加载并缓存一个广告
        if (cachedAd == null) {
            AdLogger.d("缓存为空，立即加载原生广告，广告位ID: %s", finalAdUnitId)
            loadAdToCache(context, finalAdUnitId,style)
            cachedAd = getCachedAd(finalAdUnitId)
        }
        
        return if (cachedAd != null) {
            AdLogger.d("使用缓存中的原生广告，广告位ID: %s", finalAdUnitId)
            AdResult.Success(cachedAd.ad)
        } else {
            AdResult.Failure(createAdException("广告加载失败"))
        }
    }
    
    /**
     * 显示原生广告到指定容器（简化版接口）
     * @param context 上下文
     * @param container 目标容器
     * @param style 广告样式，默认为标准样式
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     * @return 是否显示成功
     */
    suspend fun displayAdInView(
        context: Context, 
        container: ViewGroup,
        position: String,
        style: NativeAdStyle = NativeAdStyle.STANDARD,
        adUnitId: String? = null,
        onClick:(() -> Unit)? = null
    ): Boolean {
        // 检查是否启用多平台竞价（透明切换）
        if (net.corekit.monetize.ads.bidding.BiddingPlatformController.isMultiPlatformBiddingEnabled()) {
            AdLogger.d("原生广告启用多平台竞价，自动切换到 smartBidAndShow")
            return net.corekit.monetize.ads.bidding.NativeSmartBiddingManager.smartBidAndShow(
                context, container, position, style, onClick
            )
        }
        
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_NATIVE_ID
        
        // 累积触发统计
        totalShowTriggerCount++
        AdLogger.d("原生广告累积触发展示次数: $totalShowTriggerCount")
        
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            ),
            style
        )
        
        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(context, AdConfigManager.getNativeConfig())) {
            is AdResult.Failure -> {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("原生广告累积展示失败次数: $totalShowFailCount")
                
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to position,
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message
                    ),
                    style
                )
                
                AdLogger.w("原生广告拦截器检查失败: %s", interceptResult.error.message)
                return false
            }
            else -> { /* continue */ }
        }
        
        return try {
            // 显示加载视图
//            container.removeAllViews()
//            container.addView(nativeAdView.createLoadingView(context))
            
            when (val result = retrieveAd(context, adUnitId,style)) {
                is AdResult.Success -> {
                    val nativeAd = result.data

                    nativeAd.adEventCallback = object : NativeAdEventCallback{
                        override fun onAdPaid(value: AdValue) {
                            AdLogger.d("原生广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                            // 存储当前广告的收益信息
                            currentAdValue = value

                            reportAdData(
                                eventName = "ad_impression",
                                params = mapOf(
                                    "ad_unit_name" to finalAdUnitId,
                                    "position" to position,
                                    "number" to totalShowCount,
                                    "ad_source" to (nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 }
                                        ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                ),
                                style
                            )

                            // 上报真实的广告收益数据
                            reportAdRevenueWithValue(finalAdUnitId, nativeAd, value)

                            IpuController.onAdImpression("NA", value.valueMicros)
                            RpuController.onAdRevenue("NA", value.valueMicros)
                        }

                        override fun onAdClicked() {
                            AdLogger.d("原生广告被点击")

                            // 累积点击统计
                            totalClickCount++
                            onClick?.invoke()
                            AdLogger.d("原生广告累积点击次数: $totalClickCount")

                            AdConfigManager.getNativeConfig().recordClick()

                            reportAdData(
                                eventName = "ad_click",
                                params = mapOf(
                                    "ad_unit_name" to finalAdUnitId,
                                    "position" to position,
                                    "number" to totalClickCount,
                                    "ad_source" to (nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                ),
                                style
                            )
                        }

                        override fun onAdImpression() {
                            AdLogger.d("原生广告展示完成")

                            // 累积展示统计
                            totalShowCount++
                            AdLogger.d("原生广告累积展示次数: $totalShowCount")

                            // 记录展示
                            AdConfigManager.getNativeConfig().recordShow()

                            // 异步预加载下一个广告到缓存（如果缓存未满）
                            if (!isCacheFull(finalAdUnitId)) {
                                PreloadController.preload(context)
                            }
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            totalCloseCount++
                            reportAdData(
                                eventName = "ad_close",
                                params = mapOf(
                                    "ad_unit_name" to finalAdUnitId,
                                    "position" to position,
                                    "number" to totalCloseCount,
                                    "ad_source" to (nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                ),
                                style
                            )
                        }



                    }


                    // 绑定广告到容器
                    nativeAdView.bindNativeAdToContainer(context, container, result.data, style)
                    true
                }
                is AdResult.Failure -> {
                    // 累积展示失败次数统计
                    totalShowFailCount++
                    AdLogger.d("原生广告累积展示失败次数: $totalShowFailCount")
                    
                    reportAdData(
                        eventName = "ad_show_fail",
                        params = mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to position,
                            "number" to totalShowFailCount,
                            "reason" to result.error.message
                        ),
                        style
                    )
                    
                    // 显示错误视图
//                    container.removeAllViews()
//                    container.addView(nativeAdView.createErrorView(context, result.error.message))
                    false
                }
                AdResult.Loading -> {
                    // 保持加载状态
                    false
                }
            }
        } catch (e: Exception) {
            // 累积展示失败次数统计
            totalShowFailCount++
            AdLogger.d("原生广告累积展示失败次数: $totalShowFailCount")
            
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "${e.message}"
                ),
                style
            )
            
            AdLogger.e("显示原生广告失败", e)
//            container.removeAllViews()
//            container.addView(nativeAdView.createErrorView(context, "广告显示异常"))
            false
        }
    }
    
    /**
     * 基础广告加载方法（可复用）
     */
    private suspend fun loadAd(context: Context, adUnitId: String,style: NativeAdStyle = NativeAdStyle.STANDARD): NativeAd? {
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("原生广告累积加载次数: $totalLoadCount")
        
        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            ),
            style
        )
        
        return suspendCancellableCoroutine { continuation ->
            _loadingState.value = AdResult.Loading
            val startTime = System.currentTimeMillis()
            var nativeAds :NativeAd ?=null
            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
                .setAdChoicesPlacement(
                    AdChoicesPlacement.TOP_RIGHT
                ).setMediaAspectRatio(
                    NativeAd.NativeMediaAspectRatio.ANY
                ).setVideoOptions(videoOptions).build()
            NativeAdLoader.load(adRequest,object : NativeAdLoaderCallback{
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    nativeAds = nativeAd
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.d("原生广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                    totalLoadSucCount++
                    reportAdData(
                        eventName = "ad_loaded",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to (nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "pass_time" to ceil(loadTime / 1000.0).toInt()
                        ),
                        style
                    )
                    FpuController.onAdFill("NA")

                    val result = AdResult.Success(nativeAd)
                    _loadingState.value = result
                    continuation.resume(nativeAd)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e("原生广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", adUnitId, loadTime, adError.message)

                    totalLoadFailCount++
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

                    val result = AdResult.Failure(
                        AdException(
                            code = adError.code.value,
                            message = adError.message
                        )
                    )
                    _loadingState.value = result
                    continuation.resume(null)
                }
            })
        }
    }
    
    /**
     * 加载广告到缓存
     */
    private suspend fun loadAdToCache(context: Context, adUnitId: String,style: NativeAdStyle = NativeAdStyle.STANDARD): AdResult<Unit> {
        return try {
            
            // 检查缓存是否已满
            val currentAdUnitCount = adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
            if (currentAdUnitCount >= maxCacheSizePerAdUnit) {
                AdLogger.w("广告位 %s 缓存已满，当前缓存: %d/%d", adUnitId, currentAdUnitCount, maxCacheSizePerAdUnit)
                return AdResult.Success(Unit)
            }
            
            // 加载广告
            val nativeAd = loadAd(context, adUnitId,style)
            if (nativeAd != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedNativeAd(nativeAd, adUnitId))
                    val currentCount = getCachedAdCount(adUnitId)
                    AdLogger.d("原生广告加载成功并缓存，广告位ID: %s，该广告位缓存数量: %d/%d", adUnitId, currentCount, maxCacheSizePerAdUnit)
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("原生loadAdToCache异常", e)
            AdResult.Failure(AdException(0, "加载异常: ${e.message}", e))
        }
    }
    
    /**
     * 从缓存获取广告
     */
    private fun getCachedAd(adUnitId: String): CachedNativeAd? {
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
     * 获取当前加载的广告数据
     */
    fun retrieveCurrentAd(): NativeAd? {
        return getCachedAd(BuildConfig.ADMOB_NATIVE_ID)?.ad
    }
    
    /**
     * 检查是否有可用的广告
     */
    fun checkAdReady(): Boolean {
        return getCachedAdCount(BuildConfig.ADMOB_NATIVE_ID) > 0
    }
    
    /**
     * 获取当前加载状态
     */
    fun retrieveLoadingState(): AdResult<NativeAd> {
        return _loadingState.value
    }
    
    /**
     * 销毁广告
     */
    fun releaseAd() {
        synchronized(adCachePool) {
            adCachePool.forEach { cachedAd ->
                cachedAd.ad.destroy()
            }
            adCachePool.clear()
        }
        AdLogger.d("原生广告已销毁")
    }
    
    /**
     * 上报广告收益数据（使用真实收益值）
     * @param nativeAd 原生广告对象
     * @param adValue 广告收益值
     */
    private fun reportAdRevenueWithValue(adUnitId: String,nativeAd: NativeAd, adValue: AdValue) {
        // 创建广告收益数据
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "Native"
        )
        
        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("原生广告真实收益数据已上报，广告位ID: ${adUnitId}, 收益: ${adValue.valueMicros}微元 ${adValue.currencyCode}")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        releaseAd()
        _loadingState.value = AdResult.Loading
        AdLogger.d("原生广告控制器已清理")
    }
    
    /**
     * 通用数据上报函数
     * @param eventName 事件名称
     * @param params 参数Map，会与基础参数合并
     */
    private fun reportAdData(eventName: String, params: Map<String, Any>,style: NativeAdStyle = NativeAdStyle.STANDARD) {
        val format = when(style.description){
            NativeAdStyle.CARD_4.description -> "dialognative"
            "full_native" -> "Fullnative"
            else -> "Native"
        }
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to format
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
} 