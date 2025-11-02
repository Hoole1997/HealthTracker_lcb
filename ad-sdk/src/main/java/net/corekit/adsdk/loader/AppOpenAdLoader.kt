package net.corekit.adsdk.loader

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.adsdk.core.AdException
import net.corekit.adsdk.core.AdResult
import net.corekit.adsdk.service.AdLoader
import kotlin.coroutines.resume

/**
 * 开屏广告加载器
 * 使用新版 AdMob SDK 加载开屏广告
 */
internal class AppOpenAdLoader : AdLoader<AppOpenAd> {

    override suspend fun load(adUnitId: String): AdResult<AppOpenAd> {
        return suspendCancellableCoroutine { continuation ->
            val adRequest = AdRequest.Builder(adUnitId).build()

            val loadCallback = object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    if (continuation.isActive) {
                        continuation.resume(AdResult.Success(ad))
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    if (continuation.isActive) {
                        val exception = AdException.loadFailed(
                            message = adError.message,
                            cause = null
                        )
                        continuation.resume(AdResult.Failure(exception))
                    }
                }
            }

            AppOpenAd.load(adRequest, loadCallback)
        }
    }
}
