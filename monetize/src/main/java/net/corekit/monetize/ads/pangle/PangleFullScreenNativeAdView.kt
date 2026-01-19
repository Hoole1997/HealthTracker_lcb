package net.corekit.monetize.ads.pangle

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData
import com.bumptech.glide.Glide
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionCallback
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGViewBinder
import net.corekit.monetize.R
import net.corekit.monetize.ads.log.AdLogger

/**
 * Pangle全屏原生广告视图组件
 * 提供全屏原生广告的布局创建、数据绑定和交互注册
 */
class PangleFullScreenNativeAdView {

    companion object {
        private const val TAG = "PangleFullScreenNativeView"
    }

    /**
     * 创建并绑定全屏原生广告视图到容器中
     */
    fun bindFullScreenNativeAdToContainer(
        context: Context,
        container: ViewGroup,
        nativeAd: PAGNativeAd,
        @Suppress("UNUSED_PARAMETER") lifecycleOwner: LifecycleOwner? = null,
        interactionListener: PAGNativeAdInteractionCallback? = null
    ): Boolean {
        return try {
            container.removeAllViews()

            val adView = LayoutInflater.from(context)
                .inflate(R.layout.layout_pangle_fullscreen_native_ad, container, false)

            val nativeAdData = nativeAd.nativeAdData
            val creativeViews = bindNativeAdData(context, adView, nativeAdData)

            container.addView(adView)

            val clickViews = arrayListOf<View>().apply {
                adView.findViewById<TextView>(R.id.ads_tv_title)?.let { add(it) }
                adView.findViewById<TextView>(R.id.ads_tv_description)?.let { add(it) }
                adView.findViewById<TextView>(R.id.ads_btn_cta)?.let { add(it) }
                adView.findViewById<ImageView>(R.id.ads_iv_icon)?.let { add(it) }
            }
            val binder = PAGViewBinder.Builder(container)
                .titleTextView(adView.findViewById<TextView>(R.id.ads_tv_title))
                .descriptionTextView( adView.findViewById<TextView>(R.id.ads_tv_description))
                .logoViewGroup(adView.findViewById<FrameLayout>(R.id.fl_ad_logo))
                .iconImageView(adView.findViewById<ImageView>(R.id.ads_iv_icon))
                .mediaContentViewGroup(adView.findViewById<FrameLayout>(R.id.fl_ad_media))
                .build()

            @Suppress("UNCHECKED_CAST")
            nativeAd.registerViewForInteraction(
                binder,
                clickViews as MutableList<View>,
                interactionListener
            )

            true
        } catch (e: Exception) {
            AdLogger.e("Pangle全屏原生广告视图绑定失败", e)
            false
        }
    }

    /**
     * 创建加载视图
     */
    fun createFullScreenLoadingView(context: Context, container: ViewGroup) {
        try {
            container.removeAllViews()
            val loadingView = LayoutInflater.from(context)
                .inflate(R.layout.layout_fullscreen_loading, container, false)
            container.addView(loadingView)
        } catch (e: Exception) {
            AdLogger.e("Pangle全屏原生加载视图创建失败", e)
        }
    }

    private fun bindNativeAdData(
        context: Context,
        adView: View,
        nativeAdData: PAGNativeAdData
    ): MutableList<View>? {
        try {
            val titleView = adView.findViewById<TextView>(R.id.ads_tv_title)
            val descView = adView.findViewById<TextView>(R.id.ads_tv_description)
            val ctaView = adView.findViewById<TextView>(R.id.ads_btn_cta)
            val iconView = adView.findViewById<ImageView>(R.id.ads_iv_icon)
            val mediaContainer = adView.findViewById<FrameLayout>(R.id.fl_ad_media)
            val logoContainer = adView.findViewById<FrameLayout>(R.id.fl_ad_logo)

            val creativeViews = mutableListOf<View>()

            titleView?.text = nativeAdData.title ?: ""
            descView?.text = nativeAdData.description ?: ""
            ctaView?.text = nativeAdData.buttonText ?: "INSTALL"

            nativeAdData.icon?.let { icon ->
                try {
                    Glide.with(context)
                        .load(icon.imageUrl)
                        .into(iconView ?: return@let)
                    iconView?.visibility = View.VISIBLE
                } catch (e: Exception) {
                    iconView?.visibility = View.GONE
                }
            } ?: run {
                iconView?.visibility = View.GONE
            }

            mediaContainer?.let { container ->
                container.removeAllViews()
                nativeAdData.mediaView?.let { mediaView ->
                    container.addView(mediaView)
                    container.visibility = View.VISIBLE
                    creativeViews.add(mediaView)
                } ?: run {
                    container.visibility = View.GONE
                }
            }

            logoContainer?.let { container ->
                container.removeAllViews()
                nativeAdData.adLogoView?.let { logoView ->
                    container.addView(logoView)
                    container.visibility = View.VISIBLE
                } ?: run {
                    container.visibility = View.GONE
                }
            }

            return creativeViews

        } catch (e: Exception) {
            AdLogger.e("Pangle全屏原生广告数据绑定失败", e)
        }
        return null
    }
}
