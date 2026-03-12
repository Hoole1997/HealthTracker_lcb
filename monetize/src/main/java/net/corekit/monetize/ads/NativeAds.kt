package net.corekit.monetize.ads

import android.content.Context
import android.view.ViewGroup
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
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
import net.corekit.monetize.ads.bidding.BiddingAdType
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.frequency.PlatformFrequencyManager
import net.corekit.monetize.ads.interceptor.ClickLimitInterceptor
import net.corekit.monetize.ads.interceptor.GlobalAdSwitchInterceptor
import net.corekit.monetize.ads.interceptor.InterceptorChain
import net.corekit.monetize.ads.interceptor.ShowCountLimitInterceptor
import net.corekit.monetize.ads.interceptor.ShowIntervalLimitInterceptor
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.util.runClassicGmaOnMain
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
        // 原生广告缓存数量：1个（最常见场景只展示一个）
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 1
        
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
    
    private val nativeAdView = NativeAdView()
    private var currentDisplayPosition: String = ""
    private var currentDisplayAdUnitId: String = ""
    private var currentDisplayStyle: NativeAdStyle = NativeAdStyle.STANDARD
    private var currentClickCallback: (() -> Unit)? = null
    private var currentContainer: ViewGroup? = null
    
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
    suspend fun loadInAdvance(context: Context, adUnitId: String? = null, style: NativeAdStyle = NativeAdStyle.STANDARD): AdResult<Unit> {
        if(!GlobalAdSwitchInterceptor.isGlobalAdEnabled()){
            return AdResult.Failure(
                AdErrorCode.GLOBAL_AD_DISABLED.toAdException())
        }
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_NATIVE_ID

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
            AdLogger.logD(TAG, "跳过加载 | 原因: 缓存已满或加载中 | 缓存: %d/%d | 加载中: %d", 
                currentCount, maxCacheSizePerAdUnit, inflightLoads[finalAdUnitId] ?: 0)
            return AdResult.Success(Unit)
        }

        return try {
            loadAdToCache(context, finalAdUnitId, style)
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
            AdLogger.logD(TAG, "缓存未命中 | 触发即时加载")
            loadAdToCache(context, finalAdUnitId,style)
            cachedAd = getCachedAd(finalAdUnitId)
        }
        
        return if (cachedAd != null) {
            AdLogger.logD(TAG, "缓存命中 | 返回缓存广告")
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
        onClick:(() -> Unit)? = null,
        bypassBidding: Boolean = false
    ): Boolean {
        // 检查是否启用多平台竞价（透明切换）
        if (!bypassBidding && net.corekit.monetize.ads.bidding.BiddingPlatformController.isMultiPlatformBiddingEnabled()) {
            AdLogger.logD(TAG, "启用多平台竞价 | 切换到 SmartBidding")
            return net.corekit.monetize.ads.bidding.NativeSmartBiddingManager.smartBidAndShow(
                context, container, position, style, onClick
            )
        }
        
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_NATIVE_ID
        
        // 累积触发统计
        totalShowTriggerCount++
        AdLogger.logD(TAG, "展示触发 | 位置: %s | 累计触发: %d", position, totalShowTriggerCount)
        
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            ),
            style
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.ADMOB, BiddingAdType.NATIVE)) {
            totalShowFailCount++
            AdLogger.logW(TAG, "展示失败 | 位置: %s | 原因: 平台频控拦截 | 累计失败: %d", position, totalShowFailCount)

            reportAdData(
                eventName = "ad_show_error",
                params = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                ),
                style
            )

            return false
        }
        
        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(context, AdConfigManager.getNativeConfig())) {
            is AdResult.Failure -> {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.logW(TAG, "展示失败 | 位置: %s | 原因: 拦截器拦截 | 累计失败: %d", position, totalShowFailCount)
                
                reportAdData(
                    eventName = "ad_show_error",
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

            currentDisplayPosition = position
            currentDisplayAdUnitId = finalAdUnitId
            currentDisplayStyle = style
            currentClickCallback = onClick
            currentContainer = container
            
            when (val result = retrieveAd(context, adUnitId,style)) {
                is AdResult.Success -> {
                    val nativeAd = result.data

                    nativeAd.setOnPaidEventListener(OnPaidEventListener { value ->
                        AdLogger.d("原生广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                        currentAdValue = value

                        reportAdData(
                            eventName = "ad_impression",
                            params = mapOf(
                                "ad_unit_name" to currentDisplayAdUnitId,
                                "position" to currentDisplayPosition,
                                "number" to totalShowCount,
                                "ad_source" to (nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                "currency" to (currentAdValue?.currencyCode ?: "")
                            ),
                            currentDisplayStyle
                        )

                        reportAdRevenueWithValue(currentDisplayAdUnitId, nativeAd, value)

                        IpuController.onAdImpression("NA", value.valueMicros)
                        RpuController.onAdRevenue("NA", value.valueMicros)
                    })


                    // 绑定广告到容器
                    nativeAdView.bindNativeAdToContainer(context, container, result.data, style)
                    true
                }
                is AdResult.Failure -> {
                    // 累积展示失败次数统计
                    totalShowFailCount++
                    AdLogger.logW(TAG, "展示失败 | 位置: %s | 原因: %s | 累计失败: %d", position, result.error.message, totalShowFailCount)
                    
                    reportAdData(
                        eventName = "ad_show_error",
                        params = mapOf(
                            "ad_unit_name" to finalAdUnitId,
                            "position" to position,
                            "number" to totalShowFailCount,
                            "reason" to result.error.message
                        ),
                        style
                    )
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
            AdLogger.logE(TAG, "展示异常 | 位置: %s | 原因: %s | 累计失败: %d", position, e.message, totalShowFailCount)
            
            reportAdData(
                eventName = "ad_show_error",
                params = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "${e.message}"
                ),
                style
            )
//            container.removeAllViews()
//            container.addView(nativeAdView.createErrorView(context, "广告显示异常"))
            false
        }
    }
    
    /**
     * 基础广告加载方法（可复用）
     */
    private suspend fun loadAd(context: Context, adUnitId: String,style: NativeAdStyle = NativeAdStyle.STANDARD): NativeAd? {
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.ADMOB, BiddingAdType.NATIVE)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.ADMOB, BiddingAdType.NATIVE)
            AdLogger.logW(TAG, "加载跳过 | 平台: AdMob | 类型: Native | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Admob"
            ), style)
            return null
        }
        
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.logD(TAG, "开始加载 | 平台: AdMob | 累计加载: %d", totalLoadCount)
        
        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            ),
            style
        )
        
        return runClassicGmaOnMain {
            suspendCancellableCoroutine { continuation ->
                _loadingState.value = AdResult.Loading
                val startTime = System.currentTimeMillis()
                val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
                val nativeAdOptions = NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
                    .setVideoOptions(videoOptions)
                    .build()
                var loadedNativeAd: NativeAd? = null
                val adLoader = AdLoader.Builder(context, adUnitId)
                    .forNativeAd { nativeAd ->
                        loadedNativeAd = nativeAd
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.d("原生广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                        totalLoadSucCount++
                        reportAdData(
                            eventName = "ad_loaded",
                            params = mapOf(
                                "ad_unit_name" to adUnitId,
                                "number" to totalLoadSucCount,
                                "ad_source" to (nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            ),
                            style
                        )
                        FpuController.onAdFill("NA")

                        val result = AdResult.Success(nativeAd)
                        _loadingState.value = result
                        if (continuation.isActive) {
                            continuation.resume(nativeAd)
                        }
                    }
                    .withNativeAdOptions(nativeAdOptions)
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            val loadTime = System.currentTimeMillis() - startTime
                            AdLogger.e("原生广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", adUnitId, loadTime, adError.message)

                            totalLoadFailCount++
                            reportAdData(
                                eventName = "ad_load_fail",
                                params = mapOf(
                                    "ad_unit_name" to adUnitId,
                                    "number" to totalLoadFailCount,
                                    "ad_source" to (adError.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                    "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                    "reason" to adError.message
                                )
                            )

                            val result = AdResult.Failure(
                                AdException(
                                    code = adError.code,
                                    message = adError.message
                                )
                            )
                            _loadingState.value = result
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }

                        override fun onAdClicked() {
                            val nativeAd = loadedNativeAd ?: return
                            AdLogger.d("原生广告被点击")

                            totalClickCount++
                            currentClickCallback?.invoke()
                            AdLogger.logD(TAG, "用户点击 | 位置: %s | 累计点击: %d", currentDisplayPosition, totalClickCount)

                            AdConfigManager.getNativeConfig().recordClick()
                            PlatformFrequencyManager.recordClick(BiddingPlatform.ADMOB, BiddingAdType.NATIVE)

                            reportAdData(
                                eventName = "ad_click",
                                params = mapOf(
                                    "ad_unit_name" to currentDisplayAdUnitId,
                                    "position" to currentDisplayPosition,
                                    "number" to totalClickCount,
                                    "ad_source" to (nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                ),
                                currentDisplayStyle
                            )

                            if (PlatformFrequencyManager.isClickLimitReached(BiddingPlatform.ADMOB, BiddingAdType.NATIVE)) {
                                AdLogger.logW(TAG, "点击达到配额上限，移除正在展示的广告 | 位置: %s", currentDisplayPosition)
                                currentContainer?.removeAllViews()
                            }
                        }

                        override fun onAdImpression() {
                            totalShowCount++
                            AdLogger.logD(TAG, "展示成功 | 平台: AdMob | 位置: %s | 累计展示: %d", currentDisplayPosition, totalShowCount)

                            AdConfigManager.getNativeConfig().recordShow()

                            if (!isCacheFull(adUnitId)) {
                                PreloadController.preloadPlatformAdType(
                                    context,
                                    net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB,
                                    net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE
                                )
                            }
                        }

                        override fun onAdClosed() {
                            val nativeAd = loadedNativeAd ?: return
                            totalCloseCount++
                            reportAdData(
                                eventName = "ad_dismiss",
                                params = mapOf(
                                    "ad_unit_name" to currentDisplayAdUnitId,
                                    "position" to currentDisplayPosition,
                                    "number" to totalCloseCount,
                                    "ad_source" to (nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                ),
                                currentDisplayStyle
                            )
                        }
                    })
                    .build()

                adLoader.loadAd(AdRequest.Builder().build())
            }
        }
    }
    
    /**
     * 加载广告到缓存
     */
    private suspend fun loadAdToCache(context: Context, adUnitId: String,style: NativeAdStyle = NativeAdStyle.STANDARD): AdResult<Unit> {
        return try {
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
                AdResult.Failure(AdErrorCode.AD_LOAD_FAILED.toAdException())
            }
        } catch (e: Exception) {
            AdLogger.e("原生loadAdToCache异常", e)
            AdResult.Failure(AdErrorCode.AD_LOAD_EXCEPTION.toAdException(e))
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
     * 读取缓存广告 (Peek 语义，不删除)
     * 仅供竞价使用，确保比价和展示使用同一个广告实例
     */
    fun peekCachedAd(adUnitId: String? = null): NativeAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_NATIVE_ID
        synchronized(adCachePool) {
            return adCachePool.firstOrNull { it.adUnitId == finalAdUnitId && !it.isExpired() }?.ad
        }
    }

    /**
     * 获取当前加载的广告数据 (Pop 语义，会从缓存移除)
     * 仅供展示时使用
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
     * 获取缓存状态
     */
    fun getCacheStatus(adUnitId: String? = null): net.corekit.monetize.ads.log.BiddingLogger.CacheEntry {
        val finalAdUnitId = adUnitId ?: net.corekit.monetize.BuildConfig.ADMOB_NATIVE_ID
        return net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
            adType = "Native",
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
            adRevenueNetwork = nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceName.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = nativeAd.responseInfo?.loadedAdapterResponseInfo?.adSourceInstanceName.orEmpty(),
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
