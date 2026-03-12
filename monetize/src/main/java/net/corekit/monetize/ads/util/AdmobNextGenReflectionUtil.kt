package net.corekit.monetize.ads.util

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import net.corekit.monetize.ads.log.AdLogger

/**
 * 展示前 eCPM 反射读取入口。
 *
 * 经典 GMA 24.9.0 没有稳定、可验证的内部字段路径可复用，因此这里统一安全降级为 null，
 * 让上层竞价流程继续走既有 fallback，而不是把反射逻辑散落到各控制器里。
 */
object AdmobNextGenReflectionUtil {

    private const val TAG = "AdmobReflection"

    fun getRevenue(ad: Any?): AdValue? = getRevenueByPath(ad)

    fun getRevenueByPath(ad: Any?): AdValue? {
        if (ad == null) {
            return null
        }

        if (ad is InterstitialAd ||
            ad is AppOpenAd ||
            ad is RewardedAd ||
            ad is RewardedInterstitialAd ||
            ad is NativeAd ||
            ad is AdView
        ) {
            AdLogger.d(
                "[%s] 展示前收益反射已对 classic GMA 安全降级，ad=%s",
                TAG,
                ad::class.java.simpleName
            )
        }

        return null
    }
}
