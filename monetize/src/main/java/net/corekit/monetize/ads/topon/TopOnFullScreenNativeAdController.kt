package net.corekit.monetize.ads.topon

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.thinkup.nativead.api.TUNative
import com.thinkup.nativead.api.TUNativeNetworkListener
import com.thinkup.nativead.api.NativeAd
import com.thinkup.core.api.TUAdInfo
import com.thinkup.core.api.AdError
import com.thinkup.core.api.TUAdConst
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativeDislikeListener
import com.thinkup.nativead.api.TUNativeEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.ads.RevenueAdData
import net.corekit.core.ads.RevenueAdManager
import net.corekit.core.ads.RevenueInfo
import net.corekit.core.ext.DataStoreIntDelegate
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.AdErrorCode
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.bidding.AdIdHelper
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.interceptor.ClickLimitInterceptor
import net.corekit.monetize.ads.interceptor.GlobalAdSwitchInterceptor
import net.corekit.monetize.ads.interceptor.InterceptorChain
import net.corekit.monetize.ads.interceptor.ShowCountLimitInterceptor
import net.corekit.monetize.ads.interceptor.ShowIntervalLimitInterceptor
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.report.FpuController
import net.corekit.monetize.ads.report.IpuController
import net.corekit.monetize.ads.report.RpuController
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.ceil

/**
 * TopOn 全屏原生广告控制器
 * 参考 AdMob 全屏原生广告控制器实现，保持埋点一致
 */
class TopOnFullScreenNativeAdController private constructor() {

    // 累积统计（持久化）
    private var totalClickCount by DataStoreIntDelegate("topon_full_native_total_clicks", 0)
    private var totalCloseCount by DataStoreIntDelegate("topon_full_native_total_close", 0)
    private var totalLoadCount by DataStoreIntDelegate("topon_full_native_total_loads", 0)
    private var totalLoadSucCount by DataStoreIntDelegate("topon_full_native_total_load_suc", 0)
    private var totalShowFailCount by DataStoreIntDelegate("topon_full_native_total_show_fails", 0)
    private var totalShowTriggerCount by DataStoreIntDelegate("topon_full_native_total_show_triggers", 0)
    private var totalShowCount by DataStoreIntDelegate("topon_full_native_total_shows", 0)

    companion object {
        private const val TAG = "TopOnFullScreenNativeAdController"
        private const val AD_TIMEOUT = 1 * 60 * 60 * 1000L // 1小时过期
        private const val DEFAULT_CACHE_SIZE_PER_AD_UNIT = 1

        @Volatile
        private var INSTANCE: TopOnFullScreenNativeAdController? = null

        fun getInstance(): TopOnFullScreenNativeAdController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TopOnFullScreenNativeAdController().also { INSTANCE = it }
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

    private val fullScreenAdView = ToponFullScreenNativeAdView()

    // 当前广告的收益信息（临时存储）
    private var currentAdInfo: TUAdInfo? = null

    // 全屏原生广告是否正在显示的标识
    private var isShowing: Boolean = false

    /**
     * 缓存的全屏原生广告数据类
     */
    private data class CachedFullScreenNativeAd(
        val ad: TUNative,
        val placementId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > AD_TIMEOUT
        }
    }

    /**
     * 预加载全屏原生广告
     * @param context 上下文
     * @param placementId 广告位ID，如果为空则使用默认ID
     */
    suspend fun preloadAd(context: Context, placementId: String? = null): AdResult<Unit> {
        if (!GlobalAdSwitchInterceptor.isGlobalAdEnabled()) {
            return AdResult.Failure(
                AdException(
                    code = -100,
                    message = "全屏原生广告全局开关已关闭"
                )
            )
        }
        val finalPlacementId = placementId ?: BuildConfig.TOPON_FULL_NATIVE_ID
        return loadAdToCache(context, finalPlacementId)
    }

    /**
     * 显示全屏原生广告到指定容器
     * @param context 上下文
     * @param container 目标容器
     * @param lifecycleOwner 生命周期所有者
     * @param placementId 广告位ID，如果为空则使用默认ID
     * @return AdResult<Unit> 广告显示结果
     */
    suspend fun showAdInContainer(
        context: Context,
        container: ViewGroup,
        lifecycleOwner: LifecycleOwner,
        position: String,
        placementId: String? = null
    ): AdResult<Unit> {
        val finalPlacementId = placementId ?: BuildConfig.TOPON_FULL_NATIVE_ID

        totalShowTriggerCount++
        reportAdData(
            eventName = "ad_position",
            params = mapOf(
                "ad_unit_name" to finalPlacementId,
                "position" to position,
                "number" to totalShowTriggerCount
            )
        )

        // 拦截器检查
        when (val interceptResult = interceptorChain.intercept(context, AdConfigManager.getFullscreenNativeConfig())) {
            is AdResult.Failure -> {
                totalShowFailCount++
                reportAdData(
                    eventName = "ad_show_fail",
                    params = mapOf(
                        "ad_unit_name" to finalPlacementId,
                        "position" to position,
                        "number" to totalShowFailCount,
                        "reason" to interceptResult.error.message
                    )
                )
                return AdResult.Failure(interceptResult.error)
            }
            else -> { /* continue */ }
        }

        return try {
            // 显示加载视图
            fullScreenAdView.createFullScreenLoadingView(context, container)

            when (val result = getAd(context, finalPlacementId)) {
                is AdResult.Success -> {
                    val tuNative = result.data
                    val nativeAd = tuNative.nativeAd
                    if (nativeAd == null) {
                        AdLogger.e("TopOn全屏原生广告获取NativeAd失败")
                        return AdResult.Failure(createAdException("广告数据获取失败"))
                    }

                    // 设置事件监听器
                    nativeAd.setNativeEventListener(createNativeEventListener(finalPlacementId,position))

                    // 设置关闭按钮监听器
                    nativeAd.setDislikeCallbackListener(object : TUNativeDislikeListener() {
                        override fun onAdCloseButtonClick(
                            p0: TUNativeAdView?,
                            adInfo: TUAdInfo
                        ) {
                            AdLogger.d("TopOn全屏原生广告关闭")
                            currentAdInfo = adInfo
                            totalCloseCount++

                            val revenueValue = adInfo.publisherRevenue ?: adInfo.ecpm ?: 0.0
                            val revenueCurrency = adInfo.currency ?: "USD"

                            reportAdData(
                                eventName = "ad_close",
                                params = mapOf(
                                    "ad_unit_name" to finalPlacementId,
                                    "position" to position,
                                    "number" to totalCloseCount,
                                    "ad_source" to (adInfo.networkName ?: ""),
                                    "value" to revenueValue,
                                    "currency" to revenueCurrency
                                )
                            )
                        }
                    })

                    // 绑定广告到容器
                    val success = fullScreenAdView.bindFullScreenNativeAdToContainer(
                        context, container, nativeAd, lifecycleOwner
                    )

                    if (success) {
                        AdResult.Success(Unit)
                    } else {
                        totalShowFailCount++
                        reportAdData(
                            eventName = "ad_show_fail",
                            params = mapOf(
                                "ad_unit_name" to finalPlacementId,
                                "position" to position,
                                "number" to totalShowFailCount,
                                "reason" to "广告绑定失败"
                            )
                        )
                        AdResult.Failure(createAdException("广告绑定失败"))
                    }
                }
                is AdResult.Failure -> {
                    totalShowFailCount++
                    reportAdData(
                        eventName = "ad_show_fail",
                        params = mapOf(
                            "ad_unit_name" to finalPlacementId,
                            "position" to position,
                            "number" to totalShowFailCount,
                            "reason" to result.error.message
                        )
                    )
                    AdResult.Failure(result.error)
                }
                AdResult.Loading -> {
                    AdLogger.w("TopOn全屏原生广告正在加载中")
                    AdResult.Loading
                }
            }
        } catch (e: Exception) {
            AdLogger.e("显示TopOn全屏原生广告失败", e)
            totalShowFailCount++
            reportAdData(
                eventName = "ad_show_fail",
                params = mapOf(
                    "ad_unit_name" to finalPlacementId,
                    "position" to position,
                    "number" to totalShowFailCount,
                    "reason" to e.message.orEmpty()
                )
            )
            AdResult.Failure(AdException(code = -2, message = "显示全屏原生广告异常: ${e.message}", cause = e))
        }
    }

    /**
     * 获取全屏原生广告（自动处理加载）
     * @param context 上下文
     * @param placementId 广告位ID，如果为空则使用默认ID
     */
    suspend fun getAd(context: Context, placementId: String? = null): AdResult<TUNative> {
        val finalPlacementId = placementId ?: BuildConfig.TOPON_FULL_NATIVE_ID

        // 1. 尝试从缓存获取广告
        var cachedAd = getCachedAd(finalPlacementId)

        // 2. 如果缓存为空，立即加载并缓存一个广告
        if (cachedAd == null) {
            AdLogger.d("缓存为空，立即加载TopOn全屏原生广告，广告位ID: %s", finalPlacementId)
            loadAdToCache(context, finalPlacementId)
            cachedAd = getCachedAd(finalPlacementId)
        }

        return if (cachedAd != null) {
            AdLogger.d("使用缓存中的TopOn全屏原生广告，广告位ID: %s", finalPlacementId)
            AdResult.Success(cachedAd.ad)
        } else {
            AdResult.Failure(createAdException("广告加载失败"))
        }
    }

    /**
     * 从缓存获取广告
     */
    private fun getCachedAd(placementId: String): CachedFullScreenNativeAd? {
        synchronized(adCachePool) {
            val index = adCachePool.indexOfFirst { it.placementId == placementId && !it.isExpired() }
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
    private fun getCachedAdCount(placementId: String): Int {
        synchronized(adCachePool) {
            return adCachePool.count { it.placementId == placementId && !it.isExpired() }
        }
    }

    /**
     * 检查指定广告位的缓存是否已满
     */
    private fun isCacheFull(placementId: String): Boolean {
        return getCachedAdCount(placementId) >= maxCacheSizePerAdUnit
    }

    /**
     * 加载广告到缓存
     */
    suspend fun loadAdToCache(context: Context, placementId: String): AdResult<Unit> {
        return try {
            // 检查缓存是否已满
            val currentPlacementCount = getCachedAdCount(placementId)
            if (currentPlacementCount >= maxCacheSizePerAdUnit) {
                AdLogger.w("广告位 %s 缓存已满，当前缓存: %d/%d", placementId, currentPlacementCount, maxCacheSizePerAdUnit)
                return AdResult.Success(Unit)
            }

            // 加载广告
            val tuNative = loadAd(context, placementId)
            if (tuNative != null) {
                synchronized(adCachePool) {
                    adCachePool.add(CachedFullScreenNativeAd(tuNative, placementId))
                    val currentCount = getCachedAdCount(placementId)
                    AdLogger.d("TopOn全屏原生广告加载成功并缓存，广告位ID: %s，该广告位缓存数量: %d/%d", placementId, currentCount, maxCacheSizePerAdUnit)
                }
                AdResult.Success(Unit)
            } else {
                AdResult.Failure(createAdException("广告加载失败"))
            }
        } catch (e: Exception) {
            AdLogger.e("TopOn全屏原生loadAdToCache异常", e)
            AdResult.Failure(AdException(0, "加载异常: ${e.message}", e))
        }
    }

    /**
     * 基础广告加载方法（可复用）
     */
    private suspend fun loadAd(context: Context, placementId: String): TUNative? {
        // 累积加载次数统计
        totalLoadCount++
        AdLogger.d("TopOn全屏原生广告开始加载，广告位ID: %s，当前累计加载次数: %d", placementId, totalLoadCount)

        reportAdData(
            eventName = "ad_start_load",
            params = mapOf(
                "ad_unit_name" to placementId,
                "number" to totalLoadCount
            )
        )

        return suspendCancellableCoroutine { continuation ->
            val startTime = System.currentTimeMillis()
            val applicationContext = context.applicationContext

            // 将 tuNative 定义在外部作用域，以便在回调中访问
            var tuNative: TUNative? = null

            try {
                tuNative = TUNative(applicationContext, placementId, object : TUNativeNetworkListener {
                    override fun onNativeAdLoaded() {
                        val loadTime = System.currentTimeMillis() - startTime
                        totalLoadSucCount++
                        AdLogger.d("TopOn全屏原生广告加载成功，广告位ID: %s, 耗时: %dms", placementId, loadTime)

                        reportAdData(
                            eventName = "ad_loaded",
                            params = mapOf(
                                "ad_unit_name" to placementId,
                                "number" to totalLoadSucCount,
                                "ad_source" to "",
                                "pass_time" to ceil(loadTime / 1000.0).toInt()
                            )
                        )
                        FpuController.onAdFill("FullNa")

                        // 直接返回 TUNative
                        continuation.resume(tuNative)
                    }

                    override fun onNativeAdLoadFail(adError: AdError) {
                        val loadTime = System.currentTimeMillis() - startTime
                        AdLogger.e("TopOn全屏原生广告加载失败，广告位ID: %s, 耗时: %dms, 错误: %s", placementId, loadTime, adError.getFullErrorInfo())

                        reportAdData(
                            eventName = "ad_load_fail",
                            params = mapOf(
                                "ad_unit_name" to placementId,
                                "number" to totalLoadSucCount,
                                "ad_source" to "",
                                "pass_time" to ceil(loadTime / 1000.0).toInt(),
                                "reason" to (adError.desc ?: adError.getFullErrorInfo())
                            )
                        )

                        continuation.resume(null)
                    }
                })

                // 配置广告宽高（全屏）
                val displayMetrics = applicationContext.resources.displayMetrics
                val adViewWidth = displayMetrics.widthPixels
                val adViewHeight = displayMetrics.heightPixels

                val localExtra = mutableMapOf<String, Any>()
                localExtra[TUAdConst.KEY.AD_WIDTH] = adViewWidth
                localExtra[TUAdConst.KEY.AD_HEIGHT] = adViewHeight
                tuNative.setLocalExtra(localExtra)

                // 发起广告请求
                tuNative.makeAdRequest()
            } catch (e: Exception) {
                AdLogger.e("TopOn全屏原生广告加载异常", e)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * 创建原生广告事件监听器
     */
    private fun createNativeEventListener(
        placementId: String,
        position: String,
    ): TUNativeEventListener {
        return object : TUNativeEventListener {
            override fun onAdImpressed(view: TUNativeAdView, adInfo: TUAdInfo) {
                AdLogger.d("TopOn全屏原生广告展示完成")
                currentAdInfo = adInfo

                // 设置广告正在显示标识
                isShowing = true

                // 累积展示统计
                totalShowCount++
                AdLogger.d("TopOn全屏原生广告累积展示次数: $totalShowCount")

                // 记录展示
                AdConfigManager.getFullscreenNativeConfig().recordShow()

                val revenueValue = adInfo.publisherRevenue ?: adInfo.ecpm ?: 0.0
                val revenueCurrency = adInfo.currency ?: "USD"

                reportAdData(
                    eventName = "ad_impression",
                    params = mapOf(
                        "ad_unit_name" to placementId,
                        "position" to position,
                        "number" to totalShowCount,
                        "ad_source" to (adInfo.networkName ?: ""),
                        "value" to revenueValue,
                        "currency" to revenueCurrency
                    )
                )

                // TopOn 的 revenueValue 已经是美元，不需要转换
                val revenueUsd = (revenueValue * 1_000_000).toLong()
                IpuController.onAdImpression("FullNa", revenueUsd)
                RpuController.onAdRevenue("FullNa", revenueUsd)

                reportAdRevenueWithValue(placementId, adInfo)

                // 异步预加载下一个广告到缓存（如果缓存未满）
                if (!isCacheFull(placementId)) {
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            preloadAd(view.context, placementId)
                        } catch (e: Exception) {
                            AdLogger.e("TopOn全屏原生广告预加载失败", e)
                        }
                    }
                }
            }

            override fun onAdClicked(view: TUNativeAdView, adInfo: TUAdInfo) {
                AdLogger.d("TopOn全屏原生广告被点击")
                currentAdInfo = adInfo

                // 累积点击统计
                totalClickCount++
                AdLogger.d("TopOn全屏原生广告累积点击次数: $totalClickCount")

                AdConfigManager.getFullscreenNativeConfig().recordClick()

                val revenueValue = adInfo.publisherRevenue ?: adInfo.ecpm ?: 0.0
                val revenueCurrency = adInfo.currency ?: "USD"

                reportAdData(
                    eventName = "ad_click",
                    params = mapOf(
                        "ad_unit_name" to placementId,
                        "position" to position,
                        "number" to totalClickCount,
                        "ad_source" to (adInfo.networkName ?: ""),
                        "value" to revenueValue,
                        "currency" to revenueCurrency
                    )
                )
            }

            override fun onAdVideoStart(p0: TUNativeAdView?) {
            }

            override fun onAdVideoEnd(p0: TUNativeAdView?) {
            }

            override fun onAdVideoProgress(
                p0: TUNativeAdView?,
                p1: Int
            ) {
            }

            fun onAdClosed(view: TUNativeAdView, adInfo: TUAdInfo) {
            }
        }
    }

    fun closeEvent(placementId: String = "",position: String) {
        // 设置广告不再显示标识
        isShowing = false
        totalCloseCount++

        reportAdData(
            eventName = "ad_close",
            params = mapOf(
                "ad_unit_name" to placementId,
                "position" to position,
                "number" to totalCloseCount,
                "ad_source" to (currentAdInfo?.networkName ?: ""),
                "value" to (currentAdInfo?.publisherRevenue ?: 0.0),
                "currency" to (currentAdInfo?.currency ?: "USD")
            )
        )
    }

    fun peekCachedAd(placementId: String = BuildConfig.TOPON_FULL_NATIVE_ID): TUNative? {
        return synchronized(adCachePool) {
            adCachePool.firstOrNull { it.placementId == placementId && !it.isExpired() }?.ad
        }
    }

    fun getCurrentAd(placementId: String? = null): TUNative? {
        val finalPlacementId = placementId ?: BuildConfig.TOPON_FULL_NATIVE_ID
        return peekCachedAd(finalPlacementId)
    }

    fun hasCachedAd(placementId: String? = null): Boolean {
        synchronized(adCachePool) {
            return if (placementId != null) {
                adCachePool.any { it.placementId == placementId && !it.isExpired() }
            } else {
                adCachePool.any { !it.isExpired() }
            }
        }
    }

    /**
     * 上报广告收益数据（使用真实收益值）
     * @param adInfo 广告信息
     */
    private fun reportAdRevenueWithValue(placementId: String, adInfo: TUAdInfo) {
        val revenueValue = adInfo.publisherRevenue ?: adInfo.ecpm ?: 0.0
        val revenueCurrency = adInfo.currency ?: "USD"

        // 创建广告收益数据
        val adRevenueData = RevenueAdData(
            revenue = RevenueInfo(
                value = revenueValue,
                currencyCode = revenueCurrency
            ),
            adRevenueNetwork = adInfo.networkName ?: "",
            adRevenueUnit = placementId,
            adRevenuePlacement = adInfo.placementId ?: "",
            adFormat = "FullNative"
        )

        // 上报收益数据（内部已处理初始化和异常）
        RevenueAdManager.reportAdRevenue(adRevenueData)
        AdLogger.d("TopOn全屏原生广告真实收益数据已上报，广告位ID: %s, 收益: %.8f %s", placementId, revenueValue, revenueCurrency)
    }

    /**
     * 销毁广告
     */
    fun destroyAd() {
        synchronized(adCachePool) {
            adCachePool.clear()
        }
        AdLogger.d("TopOn全屏原生广告已销毁")
    }

    /**
     * 销毁控制器
     */
    fun destroy() {
        destroyAd()
        AdLogger.d("TopOn全屏原生广告控制器已清理")
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
            "ad_platform" to "TopOn",
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
     * 获取全屏原生广告是否正在显示的状态
     * @return true 如果全屏原生广告正在显示，false 否则
     */
    fun isAdShowing(): Boolean {
        return isShowing
    }


    fun getEcpm(adUnitId: String? = null) = getCurrentAd(adUnitId)?.checkValidAdCaches()?.firstOrNull()?.publisherRevenue ?: 0.0
}


