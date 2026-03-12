package net.corekit.monetize.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CompletableDeferred
import net.corekit.monetize.ads.log.AdLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.bidding.AdIdHelper

/**
 * AdMob SDK 管理器
 * 负责SDK初始化和全局配置
 */
object AdsManager {
    
    private const val TAG = "AdsManager"
    
    private val _initializationState = MutableStateFlow<AdResult<Unit>>(AdResult.Loading)
    val initializationState: StateFlow<AdResult<Unit>> = _initializationState.asStateFlow()
    private val initDeferred = CompletableDeferred<Boolean>()
    private var isInitialized = false
    
    /**
     * 初始化 AdMob SDK
     */
    suspend fun init(context: Context): AdResult<Unit> {
        if (isInitialized) {
            return AdResult.Success(Unit)
        }
        
        return suspendCancellableCoroutine { continuation ->
            _initializationState.value = AdResult.Loading
            MobileAds.initialize(context) { initializationStatus ->
                try {
                    val statusMap = initializationStatus.adapterStatusMap
                    AdLogger.d("AdMob SDK初始化完成")
                    
                    // 输出各个适配器的状态
                    for ((className, status) in statusMap) {
                        AdLogger.d("AdMob 适配器: $className, 状态: ${status.initializationState}, 描述: ${status.description}")
                    }
                    
                    isInitialized = true
                    initDeferred.complete(true)
                    val result = AdResult.Success(Unit)
                    _initializationState.value = result
                    continuation.resume(result)
                    
                } catch (e: Exception) {
                    AdLogger.e("AdMob SDK初始化过程中发生异常", e)
                    val result = AdResult.Failure(
                        AdException(
                            code = AdException.ERROR_INTERNAL,
                            message = "SDK初始化异常: ${e.message}",
                            cause = e
                        )
                    )
                    _initializationState.value = result
                    continuation.resume(result)
                }
            }
        }
    }

    internal suspend fun awaitInitialized() {
        initDeferred.await()  // 阻塞调用方直到初始化完成
    }
    /**
     * 检查SDK是否已初始化
     */
    fun checkInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 获取当前初始化状态
     */
    fun getInitState(): AdResult<Unit> {
        return _initializationState.value
    }
    
    /**
     * 获取所有广告控制器的快捷访问器
     */
    object Controllers {
        val interstitial: InterstitialAds
            get() = InterstitialAds.getInstance()
            
        val appOpen: LaunchAds
            get() = LaunchAds.getInstance()
            
        val native: NativeAds
            get() = NativeAds.getInstance()
            
        val fullScreenNative: FullNativeAds
            get() = FullNativeAds.getInstance()
            
        val banner: BannerAds
            get() = BannerAds.getInstance()
    }
    
    /**
     * 清理所有控制器资源
     */
    fun cleanupAll() {
//        Controllers.interstitial.cleanup()
        Controllers.appOpen.cleanup()
        Controllers.native.cleanup()
        Controllers.fullScreenNative.cleanup()
        Controllers.banner.cleanup()
        AdLogger.d("所有广告控制器已清理")
    }

    /**
     * 启动广告预加载（包含多平台初始化）
     * 建议在应用启动后延迟调用，避免影响启动速度
     */
    fun startAdPreloading(context: Context) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            AdLogger.d("[$TAG] 🚀 触发启动广告预加载...")
            
            // 1. 初始化多平台竞价系统 (Pangle/TopOn)
            net.corekit.monetize.ads.bidding.BiddingInitializer.initialize(context)
            
            // 2. 并行触发所有类型的预加载
            launch { 
                net.corekit.monetize.ads.bidding.AppOpenPreloadManager.preloadAll(context) 
            }
            // 注意: InterstitialPreloadManager 已在 RewardTwoLayerPreloadManager 内部调用，此处不重复调用
            launch { 
                net.corekit.monetize.ads.bidding.NativePreloadManager.preloadAll(context) 
            }
            launch { 
                net.corekit.monetize.ads.bidding.BannerPreloadManager.preloadAll(context) 
            }
            // 激励广告（包含插页广告预加载）
            launch {
                net.corekit.monetize.ads.bidding.RewardTwoLayerPreloadManager.preloadAll(context)
            }

            // 延迟打印缓存状态摘要，给预加载留出处理时间
            launch {
                kotlinx.coroutines.delay(10000L)
                logAllCacheStatus()
            }
        }
    }

    /**
     * 汇总输出全平台广告缓存状态表格
     */
    fun logAllCacheStatus() {
        val entries = mutableListOf<net.corekit.monetize.ads.log.BiddingLogger.CacheEntry>()
        
        // 1. AdMob 状态
        entries.add(Controllers.appOpen.getCacheStatus())
        entries.add(Controllers.interstitial.getCacheStatus())
        entries.add(Controllers.native.getCacheStatus())
        entries.add(Controllers.fullScreenNative.getCacheStatus())
        entries.add(Controllers.banner.getCacheStatus())
        entries.add(RewardedAds.getInstance().getCacheStatus())
        entries.add(RewardedInterstitialAds.getInstance().getCacheStatus())
        
        // 2. Pangle 状态
        if (AdIdHelper.hasPangleSplashId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Splash", "Pangle", net.corekit.monetize.BuildConfig.PANGLE_SPLASH_ID,
                if (net.corekit.monetize.ads.pangle.PangleAppOpenAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasPangleInterstitialId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Interstitial", "Pangle", net.corekit.monetize.BuildConfig.PANGLE_INTERSTITIAL_ID,
                if (net.corekit.monetize.ads.pangle.PangleInterstitialAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasPangleRewardedId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Rewarded", "Pangle", net.corekit.monetize.BuildConfig.PANGLE_REWARDED_ID,
                if (net.corekit.monetize.ads.pangle.PangleRewardedAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasPangleNativeId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Native", "Pangle", net.corekit.monetize.BuildConfig.PANGLE_NATIVE_ID,
                if (net.corekit.monetize.ads.pangle.PangleNativeAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasPangleBannerId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Banner", "Pangle", net.corekit.monetize.BuildConfig.PANGLE_BANNER_ID,
                if (net.corekit.monetize.ads.pangle.PangleBannerAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasPangleFullNativeId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "FullNative", "Pangle", net.corekit.monetize.BuildConfig.PANGLE_FULL_NATIVE_ID,
                if (net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        
        // 3. TopOn 状态
        if (AdIdHelper.hasTopOnSplashId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Splash", "TopOn", net.corekit.monetize.BuildConfig.TOPON_SPLASH_ID,
                if (net.corekit.monetize.ads.topon.TopOnSplashAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasTopOnInterstitialId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Interstitial", "TopOn", net.corekit.monetize.BuildConfig.TOPON_INTERSTITIAL_ID,
                if (net.corekit.monetize.ads.topon.TopOnInterstitialAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasTopOnRewardedId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Rewarded", "TopOn", net.corekit.monetize.BuildConfig.TOPON_REWARDED_ID,
                if (net.corekit.monetize.ads.topon.TopOnRewardedAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasTopOnNativeId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Native", "TopOn", net.corekit.monetize.BuildConfig.TOPON_NATIVE_ID,
                if (net.corekit.monetize.ads.topon.TopOnNativeAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasTopOnBannerId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "Banner", "TopOn", net.corekit.monetize.BuildConfig.TOPON_BANNER_ID,
                if (net.corekit.monetize.ads.topon.TopOnBannerAdController.getInstance().hasValidCache()) 1 else 0, 1
            ))
        }
        if (AdIdHelper.hasTopOnFullNativeId()) {
            entries.add(net.corekit.monetize.ads.log.BiddingLogger.CacheEntry(
                "FullNative", "TopOn", net.corekit.monetize.BuildConfig.TOPON_FULL_NATIVE_ID,
                if (net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdController.getInstance().hasCachedAd()) 1 else 0, 1
            ))
        }
        
        // 打印表格
        net.corekit.monetize.ads.log.BiddingLogger.logCacheStatus(entries)
    }
} 
