package net.corekit.adsdk.loader

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import net.corekit.adsdk.core.AdException
import net.corekit.adsdk.core.AdResult
import net.corekit.adsdk.service.AdLoader

/**
 * 开屏广告加载器
 * 使用新版 AdMob SDK 加载开屏广告
 */
internal class AppOpenAdLoader : AdLoader<AppOpenAd> {

    override suspend fun load(adUnitId: String) =
        when (val result = AppOpenAd.load(AdRequest.Builder(adUnitId).build())) {
            is AdLoadResult.Success -> AdResult.Success(result.ad)
            is AdLoadResult.Failure -> {
                val exception = AdException.loadFailed(
                    message = result.error.message,
                    cause = null
                )
                AdResult.Failure(exception)
            }
        }
}
