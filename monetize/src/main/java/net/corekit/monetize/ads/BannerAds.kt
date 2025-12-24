package net.corekit.monetize.ads

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.healthtracker.framework.util.ScreenUtil
import com.remax.bill.ads.report.IpuController
import com.remax.bill.ads.report.RpuController
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
import net.corekit.monetize.ui.BannerAdView
import net.corekit.monetize.util.PositionGet
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * Banner广告控制器
 * 提供标准Banner广告显示功能
 */
class BannerAds private constructor() {
    
    // 累积点击统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("pdf_x9s4p6m2", 0)

    // 累积关闭统计（持久化）
    private var totalCloseCount by DataStoreIntDelegate("pdf_y3w7t1k8", 0)
    
    // 累积加载次数统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pdf_z6q2v9h5", 0)

    // 累积加载成功次数统计（持久化）
    private var totalLoadSucCount by DataStoreIntDelegate("pdf_a1x8j4r7", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("banner_load_fail_count", 0)

    // 累积展示失败次数统计（持久化）
    private var totalShowFailCount by DataStoreIntDelegate("pdf_b4n6y3z9", 0)
    
    // 累积触发统计（持久化）
    private var totalShowTriggerCount by DataStoreIntDelegate("pdf_c8m1w5p2", 0)
    
    // 累积展示统计（持久化）
    private var totalShowCount by DataStoreIntDelegate("pdf_d2k7s9q4", 0)
    
    // 当前广告的收益信息（临时存储）
    private var currentAdValue: AdValue? = null
    
    companion object {
        private const val TAG = "BannerAds"
        private const val AD_TIMEOUT = 1 * 60 * 60 * 1000L
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 1
        
        @Volatile
        private var INSTANCE: BannerAds? = null
        
        fun getInstance(): BannerAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BannerAds().also { INSTANCE = it }
            }
        }
    }
    
    // 内存缓存池 - 存储预加载的广告
    private val adCachePool = mutableListOf<CachedBannerAd>()
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
     * 缓存的Banner广告数据类
     */
    private data class CachedBannerAd(
        val ad: BannerAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > AD_TIMEOUT
        }
    }

    private var loadTime: Long = 0L
    private val adUnitId = BuildConfig.ADMOB_BANNER_ID
    private val bannerView = BannerAdView()
    
    // 状态流
    private val _loadingState = MutableStateFlow<AdResult<BannerAd>>(AdResult.Loading)
    val loadingState: StateFlow<AdResult<BannerAd>> = _loadingState.asStateFlow()
    
    private val _adExpiredState = MutableStateFlow(false)
    val adExpiredState: StateFlow<Boolean> = _adExpiredState.asStateFlow()


    private fun getAdSize(context: Context): AdSize{
        val widthPixels = ScreenUtil.screenWidth()
        val density = Resources.getSystem().displayMetrics.density
        val adWidth = (widthPixels / density).toInt()
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context,adWidth)

        return adSize
    }
    
    /**
     * 从缓存获取广告
     */
    private fun getCachedAd(adUnitId: String): CachedBannerAd? {
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
     * 检查指定广告位的缓存是否已满
     */
    private fun isCacheFull(adUnitId: String): Boolean {
        return getCachedAdCount(adUnitId) >= maxCacheSizePerAdUnit
    }
    
    /**
     * 创建广告异常
     */
    private fun createAdException(message: String, cause: Throwable? = null): AdException {
        return AdException(
            code = -1,
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
            "ad_format" to "Banner"
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
     * 加载Banner广告
     * @param context 上下文
     * @param adUnitId 广告单元id
     */
    private suspend fun loadAdInternal(context: Context, adUnitId: String): BannerAd? {
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("Banner广告累积加载次数: $totalLoadCount")
        
        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )
        
        return suspendCancellableCoroutine { continuation ->
            val adRequest = BannerAdRequest.Builder(adUnitId, getAdSize(context)).setGoogleExtrasBundle(
                Bundle().apply {
//                    putString("collapsible", "bottom")
                }).build()
            BannerAd.load(adRequest,object : AdLoadCallback<BannerAd>{
                private var loadStartTime = System.currentTimeMillis()
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    val loadTime = System.currentTimeMillis() - loadStartTime
                        totalLoadFailCount++
                        AdLogger.e("Banner广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", adUnitId, loadTime, adError.message)
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

                    // 重置开始时间，为下次刷新做准备
                    loadStartTime = System.currentTimeMillis()
                    continuation.resume(null)
                }
                override fun onAdLoaded(ad: BannerAd) {
                    val loadTime = System.currentTimeMillis() - loadStartTime
                    AdLogger.d("Banner广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
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
                    // 重置开始时间，为下次刷新做准备
                    loadStartTime = System.currentTimeMillis()
                    FpuController.onAdFill("BA")
                    continuation.resume(ad)
                }
            })
        }
    }
    
    /**
     * 加载广告到缓存
     */
    private suspend fun loadAdToCache(context: Context, adUnitId: String): AdResult<Unit> {
        return try {
            val currentAdUnitCount = adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
            if (currentAdUnitCount >= maxCacheSizePerAdUnit) {
                AdLogger.w("广告位 %s 缓存已满，当前缓存: %d/%d", adUnitId, currentAdUnitCount, maxCacheSizePerAdUnit)
                return AdResult.Success(Unit)
            }
            val loadedAdView = loadAdInternal(context, adUnitId)
            if (loadedAdView != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedBannerAd(loadedAdView, adUnitId))
                    val currentCount = getCachedAdCount(adUnitId)
                    AdLogger.d("Banner广告加载成功并缓存，广告位ID: %s，该广告位缓存数量: %d/%d", adUnitId, currentCount, maxCacheSizePerAdUnit)
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("Banner loadAdToCache异常", e)
            AdResult.Failure(AdException(0, "加载异常: ${e.message}", e))
        }
    }
    
    /**
     * 预加载Banner广告（可选，用于提前准备）
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
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_BANNER_ID
        return loadAdToCache(context, finalAdUnitId)
    }
    
    /**
     * 显示Banner广告（自动处理加载）
     * @param context 上下文
     * @param container 目标容器
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun displayAd(context: Context, container: ViewGroup, adUnitId: String? = null,onClick:(() -> Unit)? = null,onClose:(() -> Unit)? = null): AdResult<Boolean> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_BANNER_ID
        val position = PositionGet.get()
        
        // 累积触发统计
        totalShowTriggerCount++
        AdLogger.d("Banner广告累积触发展示次数: $totalShowTriggerCount")
        
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to finalAdUnitId,
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )
        
        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(context, AdConfigManager.getBannerConfig())) {
            is AdResult.Failure -> {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("Banner广告累积展示失败次数: $totalShowFailCount")
                
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to position,
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message
                    )
                )
                
                AdLogger.w("Banner广告拦截器检查失败: %s", interceptResult.error.message)
                return AdResult.Failure(interceptResult.error)
            }
            else -> { /* continue */ }
        }
        
        return try {
            // 1. 尝试从缓存获取广告
            var cachedAd = getCachedAd(finalAdUnitId)
            if (cachedAd == null) {
                AdLogger.d("缓存为空，立即加载Banner广告，广告位ID: %s", finalAdUnitId)
                loadAdToCache(context, finalAdUnitId)
                cachedAd = getCachedAd(finalAdUnitId)
            }
            
            if (cachedAd != null) {
                AdLogger.d("使用缓存中的Banner广告，广告位ID: %s", finalAdUnitId)
                val ad = cachedAd.ad

                ad.adEventCallback = object : BannerAdEventCallback{
                    override fun onAdPaid(value: AdValue) {
                        AdLogger.d("Banner广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                        // 存储当前广告的收益信息
                        currentAdValue = value

                        reportAdData(
                            eventName = "ad_impression",
                            params = mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "position" to position,
                                "number" to totalShowCount,
                                "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                "currency" to (currentAdValue?.currencyCode ?: "")
                            )
                        )

                        // 上报真实的广告收益数据
                        reportAdRevenueWithValue(ad, finalAdUnitId,value)

                        IpuController.onAdImpression("BA", value.valueMicros)
                        RpuController.onAdRevenue("BA", value.valueMicros)
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        AdLogger.d("Banner广告被点击")
                        onClick?.invoke()
                        // 累积点击统计
                        totalClickCount++
                        AdLogger.d("Banner广告累积点击次数: $totalClickCount")

                        AdConfigManager.getBannerConfig().recordClick()

                        reportAdData(
                            eventName = "ad_click",
                            params = mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "position" to position,
                                "number" to totalClickCount,
                                "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 } ?: 0.0),
                                "currency" to (currentAdValue?.currencyCode ?: "")
                            )
                        )
                    }

                    override fun onAdImpression() {
                        AdLogger.d("Banner广告展示完成")

                        // 累积展示统计
                        totalShowCount++
                        AdLogger.d("Banner广告累积展示次数: $totalShowCount")
                    }

                    override fun onAdDismissedFullScreenContent() {
                        onClose?.invoke()
                        AdLogger.d("Banner广告关闭")
                        totalCloseCount++
                        reportAdData(
                            eventName = "ad_close",
                            params = mapOf(
                                "ad_unit_name" to finalAdUnitId,
                                "position" to position,
                                "number" to totalCloseCount,
                                "ad_source" to (ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                                "currency" to (currentAdValue?.currencyCode ?: "")
                            )
                        )
                    }
                }
                
                // 显示加载视图
                container.removeAllViews()
//                container.addView(bannerView.createBannerLoadingView(context))
                
                val success = bannerView.bindBannerAdToContainer(
                    context, container, cachedAd.ad, null
                )
                
                if (success) {
                    AdConfigManager.getBannerConfig().recordShow()
                    if (!isCacheFull(finalAdUnitId)) {
                        PreloadController.preload(context)
                    }
                    AdResult.Success(ad.isCollapsible())
                } else {
                    AdResult.Failure(createAdException("广告绑定失败"))
                }
            } else {
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("Banner广告累积展示失败次数: $totalShowFailCount")

                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalAdUnitId,
                        "position" to position,
                        "number" to totalShowFailCount,
                        "reason" to "No fill"
                    )
                )

                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to finalAdUnitId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to e.message.orEmpty()
                )
            )
            AdLogger.e("显示Banner广告失败", e)
            container.removeAllViews()
            AdResult.Failure(
                AdException(
                    code = -1,
                    message = "显示Banner广告异常: ${e.message}",
                    cause = e
                )
            )
        }
    }
    



    
    /**
     * 检查广告是否已过期
     */
    fun checkAdExpired(): Boolean {
        val expired = loadTime != 0L && System.currentTimeMillis() - loadTime > AD_TIMEOUT
        if (expired && !_adExpiredState.value) {
            _adExpiredState.value = true
            AdLogger.d("Banner广告已过期")
        }
        return expired
    }
    

    
    /**
     * 获取剩余有效时间（毫秒）
     */
    fun getRemainingTime(): Long {
        if (loadTime == 0L) return 0L
        val remaining = AD_TIMEOUT - (System.currentTimeMillis() - loadTime)
        return if (remaining > 0) remaining else 0L
    }
    
    /**
     * 暂停广告
     */
    fun pause() {

        AdLogger.d("Banner广告已暂停")
    }
    
    /**
     * 上报广告收益数据（使用真实收益值）
     * @param ad BannerAd
     * @param adUnitId
     * @param adValue 广告收益值
     */
    private fun reportAdRevenueWithValue(ad: BannerAd,adUnitId: String, adValue: AdValue) {
        // 创建广告收益数据
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = ad.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "Banner"
        )
        
        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("Banner广告真实收益数据已上报，广告位ID: ${adUnitId}, 收益: ${adValue.valueMicros}微元 ${adValue.currencyCode}")
    }
    

    
    /**
     * 销毁广告
     */
    fun releaseAd() {
        synchronized(adCachePool) {
            adCachePool.forEach { cachedAd -> cachedAd.ad.destroy()}
            adCachePool.clear()
        }
        bannerView.reset()
        loadTime = 0L
        AdLogger.d("Banner广告已销毁")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        releaseAd()
        _loadingState.value = AdResult.Loading
        _adExpiredState.value = false
        AdLogger.d("Banner广告控制器已清理")
    }
} 