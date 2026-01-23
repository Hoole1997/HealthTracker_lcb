package net.corekit.monetize.ads

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement


import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
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
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.log.AdLogger
import kotlin.math.ceil
import net.corekit.monetize.ui.FullScreenNativeAdView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 全屏原生广告控制器
 * 专门处理全屏展示的原生广告，通常用于应用启动、页面切换等场景
 */
class FullNativeAds private constructor() {

    // 累积点击统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("pdf_q8z4n1r6", 0)

    // 累积关闭统计（持久化）
    private var totalCloseCount by DataStoreIntDelegate("pdf_r3t9s7w2", 0)

    // 累积加载次数统计（持久化）
    private var totalLoadCount by DataStoreIntDelegate("pdf_s6v2x8k5", 0)

    // 累积加载成功次数统计（持久化）
    private var totalLoadSucCount by DataStoreIntDelegate("pdf_t1w4y6p9", 0)
    private var totalLoadFailCount by DataStoreIntDelegate("full_native_load_fail_count", 0)

    // 累积展示失败次数统计（持久化）
    private var totalShowFailCount by DataStoreIntDelegate("pdf_u7j3m8h4", 0)

    // 累积触发统计（持久化）
    private var totalShowTriggerCount by DataStoreIntDelegate("pdf_v2k9q5z1", 0)

    // 累积展示统计（持久化）
    private var totalShowCount by DataStoreIntDelegate("pdf_w5r8n3t7", 0)

    // 当前广告的收益信息（临时存储）
    private var currentAdValue: AdValue? = null

    private var currentPosition: String = ""

    // 全屏原生广告是否正在显示的标识
    private var isShowing: Boolean = false

    companion object {
        private const val TAG = "FullNativeAds"
        private const val AD_TIMEOUT = 1 * 60 * 60 * 1000L
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 2

        @Volatile
        private var INSTANCE: FullNativeAds? = null

        fun getInstance(): FullNativeAds {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FullNativeAds().also { INSTANCE = it }
            }
        }
    }

    // 内存缓存池 - 存储预加载的广告
    private val adCachePool = mutableListOf<CachedFullScreenNativeAd>()
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

    private var fullScreenNativeAd: NativeAd? = null
    private var loadTime: Long = 0L
    private val fullScreenAdView = FullScreenNativeAdView()

    /**
     * 缓存的全屏原生广告数据类
     */
    private data class CachedFullScreenNativeAd(
        val ad: NativeAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > AD_TIMEOUT
        }
    }

    // 状态流
    private val _loadingState = MutableStateFlow<AdResult<NativeAd>>(AdResult.Loading)
    val loadingState: StateFlow<AdResult<NativeAd>> = _loadingState.asStateFlow()

    private val _showingState = MutableStateFlow<AdResult<Unit>?>(null)
    val showingState: StateFlow<AdResult<Unit>?> = _showingState.asStateFlow()

    private val _adExpiredState = MutableStateFlow(false)
    val adExpiredState: StateFlow<Boolean> = _adExpiredState.asStateFlow()

    var nativeAds: NativeAd? = null

    /**
     * 预加载全屏原生广告（可选，用于提前准备）
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun loadInAdvance(context: Context, adUnitId: String? = null): AdResult<Unit> {
        if (!GlobalAdSwitchInterceptor.isGlobalAdEnabled()) {
            return AdResult.Failure(
                AdException(
                    code = -100,
                    message = "开屏全局广告已关闭，中断加载"
                )
            )
        }
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_FULL_NATIVE_ID
        return loadAdToCache(context, finalAdUnitId)
    }

    fun triggerCloseEvent(adUnitId: String = "", position: String = "") {
        totalCloseCount++

        reportAdData(
            eventName = "ad_close",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "position" to position.ifBlank { currentPosition },
                "number" to totalCloseCount,
                "ad_source" to (nativeAds?.getResponseInfo()?.loadedAdSourceResponseInfo?.name.orEmpty()),
                "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                "currency" to (currentAdValue?.currencyCode ?: "")
            )
        )
        // 设置广告不再显示标识
        isShowing = false
    }

    /**
     * 获取全屏原生广告（自动处理加载）
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     */
    suspend fun retrieveAd(context: Context, adUnitId: String? = null): AdResult<NativeAd> {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_FULL_NATIVE_ID

        // 1. 尝试从缓存获取广告
        var cachedAd = getCachedAd(finalAdUnitId)
        if (cachedAd == null) {
            AdLogger.d("缓存为空，立即加载全屏原生广告，广告位ID: %s", finalAdUnitId)
            loadAdToCache(context, finalAdUnitId)
            cachedAd = getCachedAd(finalAdUnitId)
        }

        return if (cachedAd != null) {
            AdLogger.d("使用缓存中的全屏原生广告，广告位ID: %s", finalAdUnitId)
            AdResult.Success(cachedAd.ad)
        } else {
            AdResult.Failure(createAdException("广告加载失败"))
        }
    }

    /**
     * 显示全屏原生广告到指定容器（简化版接口）
     * @param context 上下文
     * @param container 目标容器
     * @param lifecycleOwner 生命周期所有者
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     * @return AdResult<Unit> 广告显示结果
     */
    suspend fun displayAdInView(
        context: Context,
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        position: String,
        adUnitId: String? = null
    ): AdResult<Unit> {
        totalShowTriggerCount++

        currentPosition = position
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to adUnitId.orEmpty(),
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        if (!PlatformFrequencyManager.canParticipate(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE)) {
            totalShowFailCount++
            AdLogger.w("全屏原生广告展示失败 | 位置: %s | 原因: 平台频控拦截 | 累计失败: %d", position, totalShowFailCount)

            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to adUnitId.orEmpty(),
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to "platform_frequency_limit"
                )
            )

            return AdResult.Failure(AdErrorCode.AD_SHOW_FAILED.toAdException("platform_frequency_limit"))
        }
        // 拦截器检查
        when (val interceptResult =
            interceptorChain.intercept(context, AdConfigManager.getFullscreenNativeConfig())) {
            is AdResult.Failure -> {
                AdLogger.w("全屏原生广告拦截器检查失败: %s", interceptResult.error.message)
                // 累积展示失败次数统计
                totalShowFailCount++
                AdLogger.d("全屏原生广告累积展示失败次数: $totalShowFailCount")

                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to adUnitId.orEmpty(),
                        "position" to position,
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message
                    )
                )
                return AdResult.Failure(interceptResult.error)
            }

            else -> { /* continue */
            }
        }

        return try {
            // 显示加载视图
            fullScreenAdView.createFullScreenLoadingView(context, container)

            when (val result = retrieveAd(context, adUnitId)) {
                is AdResult.Success -> {
                    _showingState.value = AdResult.Loading

                    // 绑定广告到容器
                    val success = fullScreenAdView.bindFullScreenNativeAdToContainer(
                        context, container, result.data, lifecycleOwner
                    )

                    if (success) {
                        AdResult.Success(Unit)
                    } else {
                        val error = AdException(code = -1, message = "广告绑定失败")
                        _showingState.value = AdResult.Failure(error)
                        AdResult.Failure(error)
                    }
                }

                is AdResult.Failure -> {
                    AdLogger.e("全屏原生广告加载失败: %s", result.error.message)
                    reportAdData(
                        eventName = "ad_show_fail",
                        params = mapOf(
                            "ad_unit_name" to adUnitId.orEmpty(),
                            "position" to position,
                            "number" to totalShowFailCount,
                            "reason" to result.error.message
                        )
                    )
                    AdResult.Failure(result.error)
                }

                AdResult.Loading -> {
                    AdLogger.w("全屏原生广告正在加载中")
                    AdResult.Loading
                }
            }
        } catch (e: Exception) {
            AdLogger.e("显示全屏原生广告失败", e)
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to adUnitId.orEmpty(),
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to e.message.orEmpty()
                )
            )
            AdResult.Failure(
                AdException(
                    code = -2,
                    message = "显示全屏原生广告异常: ${e.message}",
                    cause = e
                )
            )
        }
    }

    /**
     * 从缓存获取广告
     */
    private fun getCachedAd(adUnitId: String): CachedFullScreenNativeAd? {
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
     * 查看缓存中的广告（不移除）- 用于竞价和展示
     */
    fun peekCachedAd(adUnitId: String? = null): NativeAd? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_FULL_NATIVE_ID
        synchronized(adCachePool) {
            return adCachePool.firstOrNull { it.adUnitId == finalAdUnitId && !it.isExpired() }?.ad
        }
    }

    /**
     * 获取当前缓存广告的价格（用于竞价）
     * 如果缓存不存在则返回null
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     * @return 广告价格（已除以1000000转换为美元），如果获取失败返回null
     */
    suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_FULL_NATIVE_ID

        // 尝试从缓存获取广告（不移除）
        val cachedAd = peekCachedAd(finalAdUnitId)

        if (cachedAd == null) {
            AdLogger.w("[竞价] 获取FullNative广告价格失败：缓存为空")
            return null
        }

        // 使用反射获取价格（避免主线程执行反射）
        val adValue = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            net.corekit.monetize.ads.util.AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd)
        }

        return if (adValue != null) {
            val price = adValue.valueMicros / 1_000_000.0
            AdLogger.d("[竞价] 获取FullNative广告价格成功: %.6f %s (精度: %s)", price, adValue.currencyCode, adValue.precisionType)
            price
        } else {
            AdLogger.w("[竞价] 获取FullNative广告价格失败：反射获取AdValue为空")
            null
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
     * 检查缓存池是否存在元素
     * @param adUnitId 广告位ID，如果为空则检查所有广告位
     * @return 如果缓存池中存在有效广告则返回true，否则返回false
     */
    fun checkCachedAdAvailable(adUnitId: String? = null): Boolean {
        synchronized(adCachePool) {
            return if (adUnitId != null) {
                // 检查指定广告位是否有有效缓存
                adCachePool.any { it.adUnitId == adUnitId && !it.isExpired() }
            } else {
                // 检查缓存池中是否有任何有效广告
                adCachePool.any { !it.isExpired() }
            }
        }
    }

    /**
     * 加载广告到缓存
     */
    private suspend fun loadAdToCache(context: Context, adUnitId: String): AdResult<Unit> {
        return try {
            val currentAdUnitCount =
                adCachePool.count { it.adUnitId == adUnitId && !it.isExpired() }
            if (currentAdUnitCount >= maxCacheSizePerAdUnit) {
                AdLogger.w(
                    "广告位 %s 缓存已满，当前缓存: %d/%d",
                    adUnitId,
                    currentAdUnitCount,
                    maxCacheSizePerAdUnit
                )
                return AdResult.Success(Unit)
            }
            val nativeAd = loadAd(context.applicationContext, adUnitId)
            if (nativeAd != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedFullScreenNativeAd(nativeAd, adUnitId))
                    val currentCount = getCachedAdCount(adUnitId)
                    AdLogger.d(
                        "全屏原生广告加载成功并缓存，广告位ID: %s，该广告位缓存数量: %d/%d",
                        adUnitId,
                        currentCount,
                        maxCacheSizePerAdUnit
                    )
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("全屏原生loadAdToCache异常", e)
            AdResult.Failure(AdException(0, "加载异常: ${e.message}", e))
        }
    }

    /**
     * 通用数据上报函数
     * @param eventName 事件名称
     * @param params 参数Map，会与基础参数合并
     */
    private fun reportAdData(eventName: String, params: Map<String, Any>) {
        val data = mutableMapOf<String, Any>(
            "ad_platform" to "Admob",
            "ad_format" to "FullNative"
        )

        // 直接合并传入的参数
        data.putAll(params)

        if (eventName == "ad_impression") {
            ReportDataManager.reportDataByName("ThinkingData", eventName, data)
        } else {
            ReportDataManager.reportData(eventName, data)
        }
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
     * 加载广告
     * @param context 上下文
     * @param adUnitId 广告位ID
     */
    private suspend fun loadAd(context: Context, adUnitId: String): NativeAd? {
        // 频控前置检查（只检查配额，不检查间隔）
        val (canLoad, reason) = PlatformFrequencyManager.canLoadAd(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE)
        if (!canLoad) {
            val statusLog = PlatformFrequencyManager.getFrequencyStatusLog(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE)
            AdLogger.w("[$TAG] 加载跳过 | 平台: AdMob | 类型: FullNative | 原因: $reason | $statusLog")
            reportAdData("ad_load_skipped", mapOf(
                "ad_unit_name" to adUnitId,
                "reason" to (reason ?: "unknown"),
                "platform" to "Admob"
            ))
            return null
        }
        
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("全屏原生广告累积加载次数: $totalLoadCount")

        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to adUnitId,
                "number" to totalLoadCount
            )
        )

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adRequest = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
                .setAdChoicesPlacement(
                    AdChoicesPlacement.TOP_RIGHT
                ).setMediaAspectRatio(
                NativeAd.NativeMediaAspectRatio.PORTRAIT
            ).setVideoOptions(videoOptions).build()
            NativeAdLoader.load(adRequest, object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    nativeAds = nativeAd
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.d("全屏原生广告加载成功，广告位ID: %s, 耗时: %dms", adUnitId, loadTime)
                    totalLoadSucCount++
                    reportAdData(
                        eventName = "ad_loaded",
                        params = mapOf(
                            "ad_unit_name" to adUnitId,
                            "number" to totalLoadSucCount,
                            "ad_source" to (nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                            "pass_time" to ceil(loadTime / 1000.0).toInt()
                        )
                    )
                    FpuController.onAdFill("FullNa")

                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdPaid(value: AdValue) {
                            super.onAdPaid(value)
                            AdLogger.d("全屏原生广告收益回调: value=${value.valueMicros}, currency=${value.currencyCode}")

                            // 存储当前广告的收益信息
                            currentAdValue = value

                            reportAdData(
                                eventName = "ad_impression",
                                params = mapOf(
                                    "ad_unit_name" to adUnitId,
                                    "position" to currentPosition,
                                    "number" to totalShowCount,
                                    "ad_source" to (nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 }
                                        ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                )
                            )

                            // 上报真实的广告收益数据
                            reportAdRevenueWithValue(adUnitId, nativeAd, value)

                            IpuController.onAdImpression("FullNa", value.valueMicros)
                            RpuController.onAdRevenue("FullNa", value.valueMicros)
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            AdLogger.d("全屏原生广告被点击")

                            // 累积点击统计
                            totalClickCount++
                            AdLogger.d("全屏原生广告累积点击次数: $totalClickCount")

                            AdConfigManager.getFullscreenNativeConfig().recordClick()
                            PlatformFrequencyManager.recordClick(BiddingPlatform.ADMOB, BiddingAdType.FULL_NATIVE)

                            reportAdData(
                                eventName = "ad_click",
                                params = mapOf(
                                    "ad_unit_name" to adUnitId,
                                    "position" to currentPosition,
                                    "number" to totalClickCount,
                                    "ad_source" to (nativeAds?.getResponseInfo()?.loadedAdSourceResponseInfo?.name.orEmpty()),
                                    "value" to (currentAdValue?.let { it.valueMicros / 1_000_000.0 }
                                        ?: 0.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                )
                            )
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            AdLogger.d("全屏原生广告展示完成")

                            // 设置广告正在显示标识
                            isShowing = true

                            // 累积展示统计
                            totalShowCount++
                            AdLogger.d("全屏原生广告累积展示次数: $totalShowCount")

                            AdConfigManager.getFullscreenNativeConfig().recordShow()
                            if (!isCacheFull(adUnitId)) {
                                PreloadController.preloadPlatformAdType(context, net.corekit.monetize.ads.bidding.BiddingWinner.ADMOB, net.corekit.monetize.ads.bidding.BiddingAdType.FULL_NATIVE)
                            }
                            AdLogger.d("全屏原生广告显示成功")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            totalCloseCount++

                            reportAdData(
                                eventName = "ad_close",
                                params = mapOf(
                                    "ad_unit_name" to adUnitId,
                                    "position" to currentPosition,
                                    "number" to totalCloseCount,
                                    "ad_source" to (nativeAds?.getResponseInfo()?.loadedAdSourceResponseInfo?.name.orEmpty()),
                                    "value" to ((currentAdValue?.valueMicros ?: 0) / 1_000_000.0),
                                    "currency" to (currentAdValue?.currencyCode ?: "")
                                )
                            )
                        }

                    }

                    // 设置收益监听器


                    continuation.resume(nativeAd)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    val loadTime = System.currentTimeMillis() - startTime
                    AdLogger.e(
                        "全屏原生广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s",
                        adUnitId,
                        loadTime,
                        adError.message
                    )

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

                    continuation.resume(null)
                }
            })
        }
    }

    /**
     * 获取当前加载的广告数据
     */
    fun retrieveCurrentAd(): NativeAd? {
        return if (!checkAdExpired()) fullScreenNativeAd else null
    }

    /**
     * 检查是否有可用的广告
     */
    fun checkAdReady(): Boolean {
        return fullScreenNativeAd != null && !checkAdExpired()
    }

    /**
     * 检查广告是否已过期
     */
    fun checkAdExpired(): Boolean {
        val expired = loadTime != 0L && System.currentTimeMillis() - loadTime > AD_TIMEOUT
        if (expired && !_adExpiredState.value) {
            _adExpiredState.value = true
            AdLogger.d("全屏原生广告已过期")
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
            adCachePool.forEach { cachedAd -> cachedAd.ad.destroy() }
            adCachePool.clear()
        }
        fullScreenNativeAd = null
        loadTime = 0L
        AdLogger.d("全屏原生广告已销毁")
    }

    /**
     * 上报广告收益数据（使用真实收益值）
     * @param nativeAd 全屏原生广告对象
     * @param adValue 广告收益值
     */
    private fun reportAdRevenueWithValue(adUnitId: String, nativeAd: NativeAd, adValue: AdValue) {
        // 创建广告收益数据
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = adValue.valueMicros / 1_000_000.0,
                currencyCode = adValue.currencyCode
            ),
            adRevenueNetwork = nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty(),
            adRevenueUnit = adUnitId,
            adRevenuePlacement = nativeAd.getResponseInfo().loadedAdSourceResponseInfo?.instanceName.orEmpty(),
            adFormat = "FullNative"
        )

        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("全屏原生广告真实收益数据已上报，广告位ID: ${adUnitId}, 收益: ${adValue.valueMicros}微元 ${adValue.currencyCode}")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        releaseAd()
        _loadingState.value = AdResult.Loading
        _showingState.value = null
        _adExpiredState.value = false
        AdLogger.d("全屏原生广告控制器已清理")
    }

    /**
     * 获取全屏原生广告是否正在显示的状态
     * @return true 如果全屏原生广告正在显示，false 否则
     */
    fun checkAdShowing(): Boolean {
        return isShowing
    }

    /**
     * 获取缓存状态
     */
    fun getCacheStatus(adUnitId: String? = null): net.corekit.monetize.ads.log.BiddingLogger.CacheEntry {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_FULL_NATIVE_ID
        return net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
            adType = "FullNative",
            platform = "AdMob",
            adUnitId = finalAdUnitId,
            currentCount = if (checkCachedAdAvailable(finalAdUnitId)) 1 else 0,
            maxCount = 1
        )
    }
} 