package net.corekit.monetize.ads

import android.content.Context
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.bidding.BiddingPlatformController
import net.corekit.monetize.ads.bidding.BiddingWinner
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.*
import net.corekit.monetize.ads.topon.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.app.Activity
import com.healthtracker.framework.lifecycle.AppLifecycleManager
import net.corekit.monetize.ads.lifecycle.AdLifecycleGuard

/**
 * 广告预加载控制器
 * 
 * 支持 AdMob、Pangle、TopOn 多平台预加载
 */
object PreloadController {

    private const val TAG = "PreloadController"
    

    // 使用单例作用域，便于统一管理和取消
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 预加载防抖 Job
    private var preloadJob: Job? = null
    // 防抖延迟时间 (ms)
    private const val PRELOAD_DEBOUNCE_DELAY = 1000L

    /**
     * 预加载所有平台广告
     * 
     * 根据 BiddingPlatformController 配置决定加载哪些平台
     */
    fun preload(context: Context) {
        // 防抖处理：取消上一次未执行的任务
        preloadJob?.cancel()
        
        preloadJob = scope.launch(Dispatchers.Main) {
            delay(PRELOAD_DEBOUNCE_DELAY)
            
            // 使用 ApplicationContext 进行检查，确保预加载只受应用级生命周期（前后台）影响，
            // 而不受触发该请求的 Activity 销毁（如 B 页面退出）影响。
            val appContext = context.applicationContext
            if (!checkLifecycle(appContext)) return@launch
            
            AdLogger.d("[$TAG] 开始多平台广告预加载 (Debounced)")
            
            // AdMob 预加载
            preloadAdMob(appContext)
            
            // Pangle 预加载（如果启用）
            if (BiddingPlatformController.isPlatformEnabled(BiddingWinner.PANGLE)) {
                preloadPangle(appContext)
            }
            
            // TopOn 预加载（如果启用）
            if (BiddingPlatformController.isPlatformEnabled(BiddingWinner.TOPON)) {
                preloadTopOn(appContext)
            }
        }
    }

    /**
     * 仅预加载 AdMob 广告（原有逻辑）
     */
    private fun preloadAdMob(context: Context) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.Main) {
            try {
                AdLogger.d("[$TAG] [AdMob] 开屏开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_SPLASH_ID)
                LaunchAds.getInstance().loadInAdvance(appContext, BuildConfig.ADMOB_SPLASH_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 开屏异步预加载广告失败", e)
            }
        }

        scope.launch(Dispatchers.Main) {
            try {
                AdLogger.d("[$TAG] [AdMob] 插页开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_INTERSTITIAL_ID)
                InterstitialAds.getInstance().loadInAdvance(appContext, BuildConfig.ADMOB_INTERSTITIAL_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 插页异步预加载广告失败", e)
            }
        }

        scope.launch(Dispatchers.Main) {
            try {
                AdLogger.d("[$TAG] [AdMob] Banner开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_BANNER_ID)
                BannerAds.getInstance().loadInAdvance(appContext, BuildConfig.ADMOB_BANNER_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] Banner异步预加载广告失败", e)
            }
        }

        scope.launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 原生开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_NATIVE_ID)
                NativeAds.getInstance().loadInAdvance(appContext, BuildConfig.ADMOB_NATIVE_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 原生异步预加载广告失败", e)
            }
        }

        scope.launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 全屏原生开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_FULL_NATIVE_ID)
                FullNativeAds.getInstance().loadInAdvance(appContext, BuildConfig.ADMOB_FULL_NATIVE_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 全屏原生异步预加载广告失败", e)
            }
        }

        scope.launch(Dispatchers.Main) {
            try {
                AdLogger.d("[$TAG] [AdMob] 激励开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_REWARDED_ID)
                RewardedAds.getInstance().load(appContext, BuildConfig.ADMOB_REWARDED_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 激励异步预加载广告失败", e)
            }
        }

        scope.launch(Dispatchers.Main) {
            try {
                AdLogger.d("[$TAG] [AdMob] 插页激励开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)
                RewardedInterstitialAds.getInstance().loadInAdvance(appContext, BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 插页激励异步预加载广告失败", e)
            }
        }
    }

    /**
     * 预加载 Pangle 广告
     */
    private fun preloadPangle(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                AdLogger.d("[$TAG] [Pangle] 开始初始化并预加载")
                
                // 初始化 Pangle SDK
                PangleManager.initialize(appContext)
                
                // 并行预加载各类型广告
                launch {
                    try {
                        PangleAppOpenAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 开屏广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleInterstitialAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 插页广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleRewardedAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 激励广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleNativeAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleFullScreenNativeAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 全屏原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleBannerAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] Banner广告预加载失败", e)
                    }
                }
                
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [Pangle] 初始化或预加载失败", e)
            }
        }
    }

    /**
     * 预加载 TopOn 广告
     */
    private fun preloadTopOn(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                AdLogger.d("[$TAG] [TopOn] 开始初始化并预加载")
                
                // 初始化 TopOn SDK
                TopOnManager.initialize(appContext)
                
                // 并行预加载各类型广告
                launch {
                    try {
                        TopOnSplashAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 开屏广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnInterstitialAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 插页广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnRewardedAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 激励广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnNativeAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnFullScreenNativeAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 全屏原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnBannerAdController.getInstance().preloadAd(appContext)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] Banner广告预加载失败", e)
                    }
                }
                
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [TopOn] 初始化或预加载失败", e)
            }
        }
    }

    /**
     * 预加载特定平台广告
     */
    fun preloadPlatform(context: Context, platform: BiddingWinner) {
        val appContext = context.applicationContext
        scope.launch {
            if (!checkLifecycle(appContext)) return@launch
            
            when (platform) {
                BiddingWinner.ADMOB -> preloadAdMob(appContext)
                BiddingWinner.PANGLE -> preloadPangle(appContext)
                BiddingWinner.TOPON -> preloadTopOn(appContext)
            }
        }
    }

    /**
     * 预加载特定平台的特定广告类型（精细化预加载）
     * 
     * @param context 上下文
     * @param platform 平台
     * @param adType 广告类型
     */
    fun preloadPlatformAdType(context: Context, platform: BiddingWinner, adType: net.corekit.monetize.ads.bidding.BiddingAdType) {
        val appContext = context.applicationContext
        scope.launch {
            if (!checkLifecycle(appContext)) return@launch

            when (platform) {
                BiddingWinner.ADMOB -> preloadAdMobAdType(appContext, adType)
                BiddingWinner.PANGLE -> preloadPangleAdType(appContext, adType)
                BiddingWinner.TOPON -> preloadTopOnAdType(appContext, adType)
            }
        }
    }

    private suspend fun preloadAdMobAdType(context: Context, adType: net.corekit.monetize.ads.bidding.BiddingAdType) {
        try {
            when (adType) {
                net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载开屏广告")
                    LaunchAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_SPLASH_ID)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载插页广告")
                    InterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_INTERSTITIAL_ID)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载原生广告")
                    NativeAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_NATIVE_ID)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.FULL_NATIVE -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载全屏原生广告")
                    FullNativeAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_FULL_NATIVE_ID)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.BANNER -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载Banner广告")
                    BannerAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_BANNER_ID)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载激励广告")
                    RewardedAds.getInstance().load(context, BuildConfig.ADMOB_REWARDED_ID)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED_INTERSTITIAL -> {
                    AdLogger.d("[$TAG] [AdMob] 定向预加载插页激励广告")
                    RewardedInterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)
                }
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] [AdMob] 定向预加载 ${adType.name} 失败", e)
        }
    }

    private suspend fun preloadPangleAdType(context: Context, adType: net.corekit.monetize.ads.bidding.BiddingAdType) {
        try {
            PangleManager.initialize(context)
            when (adType) {
                net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH -> {
                    AdLogger.d("[$TAG] [Pangle] 定向预加载开屏广告")
                    PangleAppOpenAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL -> {
                    AdLogger.d("[$TAG] [Pangle] 定向预加载插页广告")
                    PangleInterstitialAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE -> {
                    AdLogger.d("[$TAG] [Pangle] 定向预加载原生广告")
                    PangleNativeAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.FULL_NATIVE -> {
                    AdLogger.d("[$TAG] [Pangle] 定向预加载全屏原生广告")
                    PangleFullScreenNativeAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.BANNER -> {
                    AdLogger.d("[$TAG] [Pangle] 定向预加载Banner广告")
                    PangleBannerAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED -> {
                    AdLogger.d("[$TAG] [Pangle] 定向预加载激励广告")
                    PangleRewardedAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED_INTERSTITIAL -> {
                    AdLogger.d("[$TAG] [Pangle] 暂不支持插页激励广告")
                }
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] [Pangle] 定向预加载 ${adType.name} 失败", e)
        }
    }

    private suspend fun preloadTopOnAdType(context: Context, adType: net.corekit.monetize.ads.bidding.BiddingAdType) {
        try {
            TopOnManager.initialize(context)
            when (adType) {
                net.corekit.monetize.ads.bidding.BiddingAdType.SPLASH -> {
                    AdLogger.d("[$TAG] [TopOn] 定向预加载开屏广告")
                    TopOnSplashAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.INTERSTITIAL -> {
                    AdLogger.d("[$TAG] [TopOn] 定向预加载插页广告")
                    TopOnInterstitialAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.NATIVE -> {
                    AdLogger.d("[$TAG] [TopOn] 定向预加载原生广告")
                    TopOnNativeAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.FULL_NATIVE -> {
                    AdLogger.d("[$TAG] [TopOn] 定向预加载全屏原生广告")
                    TopOnFullScreenNativeAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.BANNER -> {
                    AdLogger.d("[$TAG] [TopOn] 定向预加载Banner广告")
                    TopOnBannerAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED -> {
                    AdLogger.d("[$TAG] [TopOn] 定向预加载激励广告")
                    TopOnRewardedAdController.getInstance().preloadAd(context)
                }
                net.corekit.monetize.ads.bidding.BiddingAdType.REWARDED_INTERSTITIAL -> {
                    AdLogger.d("[$TAG] [TopOn] 暂不支持插页激励广告")
                }
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] [TopOn] 定向预加载 ${adType.name} 失败", e)
        }
    }

    /**
     * 检查生命周期状态
     */
    private fun checkLifecycle(context: Context): Boolean {
        // 1. 如果是 Activity Context，检查 Activity 状态
        if (context is Activity) {
            val lifecycleResult = AdLifecycleGuard.checkImmediate(context)
            if (lifecycleResult !is AdLifecycleGuard.CheckResult.Ready) {
                AdLogger.w("[$TAG] 预加载取消: 页面状态不满足 ($lifecycleResult)")
                return false
            }
        } else {
            // 2. 如果是 Application Context，检查应用前后台状态及锁屏状态
            if (AppLifecycleManager.isBackground()) {
                AdLogger.w("[$TAG] 预加载取消: 应用在后台")
                return false
            }
            if (AppLifecycleManager.isScreenLock()) {
                AdLogger.w("[$TAG] 预加载取消: 屏幕已锁定")
                return false
            }
        }
        return true
    }

    /**
     * 取消所有预加载任务
     * 在用户登出、隐私协议撤回等场景调用
     */
    fun cancelAll() {
        scope.coroutineContext.cancelChildren()
        AdLogger.d("[$TAG] 所有预加载任务已取消")
    }
}
