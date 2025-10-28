package net.corekit.monetize.ads

import android.content.Context
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.log.AdLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PreloadController {
    fun preload(context: Context){
        MainScope().launch {
            try {
                AdLogger.d("插页开始异步预加载下一个广告，广告位ID: %s", BuildConfig.ADMOB_INTERSTITIAL_ID)
                InterstitialAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_INTERSTITIAL_ID)
            }catch (e: Exception){
                AdLogger.e("插页异步预加载广告失败", e)
            }
        }
        MainScope().launch {
            try {
                AdLogger.d("banner开始异步预加载下一个广告，广告位ID: %s", BuildConfig.ADMOB_BANNER_ID)
                BannerAds.getInstance().loadInAdvance(context,BuildConfig.ADMOB_BANNER_ID)
            }catch (e: Exception){
                AdLogger.e("banner异步预加载广告失败", e)
            }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AdLogger.d("原生开始异步预加载下一个广告，广告位ID: %s", BuildConfig.ADMOB_NATIVE_ID)
                NativeAds.getInstance().loadInAdvance(context, BuildConfig.ADMOB_NATIVE_ID)
            } catch (e: Exception) {
                AdLogger.e("原生异步预加载广告失败", e)
            }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AdLogger.d("全屏原生开始异步预加载下一个广告，广告位ID: %s", BuildConfig.ADMOB_FULL_NATIVE_ID)
                FullNativeAds.getInstance().loadInAdvance(context,BuildConfig.ADMOB_FULL_NATIVE_ID)
            } catch (e: Exception) {
                AdLogger.e("全屏原生异步预加载广告失败", e)
            }
        }
    }
}