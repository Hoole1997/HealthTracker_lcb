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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 广告预加载控制器
 * 
 * 支持 AdMob、Pangle、TopOn 多平台预加载
 */
object PreloadController {

    private const val TAG = "PreloadController"

    /**
     * 预加载所有平台广告
     * 
     * 根据 BiddingPlatformController 配置决定加载哪些平台
     */
    fun preload(context: Context) {
        AdLogger.d("[$TAG] 开始多平台广告预加载")
        
        // AdMob 预加载
        preloadAdMob(context)
        
        // Pangle 预加载（如果启用）
        if (BiddingPlatformController.isPlatformEnabled(BiddingWinner.PANGLE)) {
            preloadPangle(context)
        }
        
        // TopOn 预加载（如果启用）
        if (BiddingPlatformController.isPlatformEnabled(BiddingWinner.TOPON)) {
            preloadTopOn(context)
        }
    }

    /**
     * 仅预加载 AdMob 广告（原有逻辑）
     */
    private fun preloadAdMob(context: Context) {
        MainScope().launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 开屏开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_SPLASH_ID)
                LaunchAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_SPLASH_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 开屏异步预加载广告失败", e)
            }
        }

        MainScope().launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 插页开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_INTERSTITIAL_ID)
                InterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_INTERSTITIAL_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 插页异步预加载广告失败", e)
            }
        }

        MainScope().launch {
            try {
                AdLogger.d("[$TAG] [AdMob] Banner开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_BANNER_ID)
                BannerAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_BANNER_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] Banner异步预加载广告失败", e)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 原生开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_NATIVE_ID)
                NativeAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_NATIVE_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 原生异步预加载广告失败", e)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 全屏原生开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_FULL_NATIVE_ID)
                FullNativeAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_FULL_NATIVE_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 全屏原生异步预加载广告失败", e)
            }
        }

        MainScope().launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 激励开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_REWARDED_ID)
                RewardedAds.getInstance().load(context, BuildConfig.ADMOB_REWARDED_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 激励异步预加载广告失败", e)
            }
        }

        MainScope().launch {
            try {
                AdLogger.d("[$TAG] [AdMob] 插页激励开始异步预加载，广告位ID: %s", BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)
                RewardedInterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID)
            } catch (e: Exception) {
                AdLogger.e("[$TAG] [AdMob] 插页激励异步预加载广告失败", e)
            }
        }
    }

    /**
     * 预加载 Pangle 广告
     */
    private fun preloadPangle(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AdLogger.d("[$TAG] [Pangle] 开始初始化并预加载")
                
                // 初始化 Pangle SDK
                PangleManager.initialize(context)
                
                // 并行预加载各类型广告
                launch {
                    try {
                        PangleAppOpenAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 开屏广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleInterstitialAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 插页广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleRewardedAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 激励广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleNativeAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleFullScreenNativeAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [Pangle] 全屏原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        PangleBannerAdController.getInstance().preloadAd(context)
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
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AdLogger.d("[$TAG] [TopOn] 开始初始化并预加载")
                
                // 初始化 TopOn SDK
                TopOnManager.initialize(context)
                
                // 并行预加载各类型广告
                launch {
                    try {
                        TopOnSplashAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 开屏广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnInterstitialAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 插页广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnRewardedAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 激励广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnNativeAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnFullScreenNativeAdController.getInstance().preloadAd(context)
                    } catch (e: Exception) {
                        AdLogger.e("[$TAG] [TopOn] 全屏原生广告预加载失败", e)
                    }
                }
                
                launch {
                    try {
                        TopOnBannerAdController.getInstance().preloadAd(context)
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
        when (platform) {
            BiddingWinner.ADMOB -> preloadAdMob(context)
            BiddingWinner.PANGLE -> preloadPangle(context)
            BiddingWinner.TOPON -> preloadTopOn(context)
        }
    }
}
