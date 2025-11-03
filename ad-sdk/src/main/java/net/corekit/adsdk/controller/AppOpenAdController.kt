package net.corekit.adsdk.controller

import ads_mobile_sdk.fs0
import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.adsdk.core.AdException
import net.corekit.adsdk.core.AdResult
import net.corekit.adsdk.core.AdShowData
import net.corekit.adsdk.core.AdType
import net.corekit.adsdk.core.BaseAdController
import net.corekit.adsdk.event.AdEvent
import net.corekit.adsdk.event.AdEventBus
import net.corekit.adsdk.metric.AdMetrics
import net.corekit.adsdk.service.AdCache
import net.corekit.adsdk.service.AdLoader
import kotlin.coroutines.resume

/**
 * 开屏广告控制器
 * 继承自 BaseAdController，只需实现平台特定的展示逻辑
 */
internal class AppOpenAdController(
    loader: AdLoader<AppOpenAd>,
    cache: AdCache<AppOpenAd>,
    eventBus: AdEventBus,
    metrics: AdMetrics,
    maxCacheSize: Int = 2,
    loadTimeoutMillis: Long = -1L
) : BaseAdController<AppOpenAd>(
    adType = AdType.APP_OPEN,
    loader = loader,
    cache = cache,
    eventBus = eventBus,
    metrics = metrics,
    maxCacheSize = maxCacheSize,
    loadTimeoutMillis = if(loadTimeoutMillis > 0L) loadTimeoutMillis else DEFAULT_LOAD_TIMEOUT_MS
) {

    companion object {
        // 默认开屏广告即时加载的超时时间（毫秒）
        private const val DEFAULT_LOAD_TIMEOUT_MS = 8_000L
    }

    /**
     * 开屏广告展示逻辑
     * 设置事件回调并调用 show 方法
     */
    override suspend fun showInternal(
        activity: Activity,
        ad: AppOpenAd,
        adUnitId: String,
        onEvent: ((AdEvent) -> Unit)?
    ): AdResult<AdShowData> = suspendCancellableCoroutine { continuation ->

        // 🔧 FIX: 包装在 try-catch 中防止未处理异常
        try {
            // 设置广告事件回调
            ad.adEventCallback = object : AppOpenAdEventCallback {

                /**
                 * 广告产生收益（曝光）
                 */
                override fun onAdPaid(value: AdValue) {
                    publishEvent(
                        AdEvent.Impression(
                            adType = adType,
                            adUnitId = adUnitId,
                            valueMicros = value.valueMicros,
                            currencyCode = value.currencyCode,
                            adSource = ad.getResponseInfo().loadedAdSourceResponseInfo?.name.orEmpty()
                        ),
                        onEvent
                    )
                    metrics.incrementImpression(adType)
                }

                /**
                 * 广告开始展示
                 */
                override fun onAdShowedFullScreenContent() {
                    publishEvent(AdEvent.ShowSuccess(adType, adUnitId), onEvent)
                    metrics.incrementShow(adType)
                }

                /**
                 * 广告展示失败
                 */
                override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                    val exception = AdException.showFailed(error.message)
                    publishEvent(AdEvent.ShowFailure(adType, adUnitId, error.message), onEvent)

                    if (continuation.isActive) {
                        continuation.resume(AdResult.Failure(exception))
                    }
                }

                /**
                 * 广告被点击
                 */
                override fun onAdClicked() {
                    publishEvent(AdEvent.Click(adType, adUnitId), onEvent)
                    metrics.incrementClick(adType)
                }

                /**
                 * 广告关闭
                 */
                override fun onAdDismissedFullScreenContent() {
                    publishEvent(AdEvent.Close(adType, adUnitId), onEvent)

                    if (continuation.isActive) {
                        // 开屏广告无奖励，返回 NO_REWARD
                        continuation.resume(AdResult.Success(AdShowData.NO_REWARD))
                    }
                }

                /**
                 * 广告曝光（展示完成）
                 */
                override fun onAdImpression() {
                    // 曝光事件已在 onAdPaid 中处理
                    // 这里可以添加其他统计逻辑

                    // ✨ 广告真正曝光后立即预加载下一个广告
                    // 利用用户观看广告的时间进行预加载，提升用户体验
                    triggerPreloadIfNeeded(adUnitId)
                }
            }

            if(continuation.isActive){
                // 展示广告
                ad.setImmersiveMode(true)
                ad.show(activity)
            }

        } catch (e: Exception) {
            // 捕获 show() 过程中的任何异常
            if (continuation.isActive) {
                continuation.resume(AdResult.Failure(AdException.from(e)))
            }
        }
    }
}
