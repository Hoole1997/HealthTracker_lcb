package com.daily.health.manager.ad.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.android.common.bill.ads.renderer.AdLoadingDialogRenderer
import com.android.common.bill.ads.renderer.AdmobFullScreenNativeAdRenderer
import com.android.common.bill.ads.renderer.AdmobNativeAdRenderer
import com.android.common.bill.ads.renderer.GamFullScreenNativeAdRenderer
import com.android.common.bill.ads.renderer.GamNativeAdRenderer
import com.android.common.bill.ads.renderer.PangleFullScreenNativeAdRenderer
import com.android.common.bill.ads.renderer.PangleNativeAdRenderer
import com.android.common.bill.ads.renderer.ToponFullScreenNativeAdRenderer
import com.android.common.bill.ads.renderer.ToponNativeAdRenderer
import com.android.common.bill.ui.NativeAdStyle
import com.android.common.bill.ui.pangle.PangleNativeAdStyle
import com.android.common.bill.ui.topon.ToponNativeAdStyle
import com.bumptech.glide.Glide
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGViewBinder
import com.daily.health.manager.R
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.thinkup.nativead.api.TUNativeMaterial
import com.thinkup.nativead.api.TUNativePrepareInfo

class DefaultAdLoadingDialogRenderer : AdLoadingDialogRenderer {
    override fun getLayoutResId(): Int = R.layout.layout_ad_loading_content

    override fun onViewCreated(view: View, onReady: () -> Unit) {
        val lottieView = view.findViewById<LottieAnimationView>(R.id.lottie_loading)
        if (lottieView?.composition != null) {
            onReady()
        } else {
            lottieView?.addLottieOnCompositionLoadedListener {
                onReady()
            }
        }
    }

    override fun updateText(view: View, text: String) {
        view.findViewById<TextView>(R.id.tv_loading_text)?.text = text
    }

    override fun findCloseView(view: View): View? = null

    override fun onDestroy(view: View) {
        view.findViewById<LottieAnimationView>(R.id.lottie_loading)?.cancelAnimation()
    }
}

class DefaultAdmobNativeAdRenderer(
    private val fallbackLayoutResId: Int = R.layout.layout_native_ads
) : AdmobNativeAdRenderer {
    override fun createLayout(context: Context, style: NativeAdStyle): NativeAdView {
        val layoutResId = style.layoutResId.takeIf { it != 0 } ?: fallbackLayoutResId
        return LayoutInflater.from(context).inflate(layoutResId, null) as NativeAdView
    }

    override fun bindData(adView: NativeAdView, nativeAd: NativeAd) {
        val titleView = adView.findViewById<TextView>(R.id.tv_ad_title)
        val ctaButton = adView.findViewById<TextView>(R.id.btn_ad_cta)
        val iconView = adView.findViewById<ImageView>(R.id.iv_ad_icon)
        val descView = adView.findViewById<TextView>(R.id.tv_ad_description)

        titleView?.text = nativeAd.headline.orEmpty()
        ctaButton?.text = nativeAd.callToAction ?: "INSTALL"
        descView?.text = nativeAd.body.orEmpty()
        nativeAd.icon?.drawable?.let { iconView?.setImageDrawable(it) }

        adView.headlineView = titleView
        adView.callToActionView = ctaButton
        adView.iconView = iconView
        adView.bodyView = descView
        adView.registerNativeAd(nativeAd, null)
    }
}

class DefaultGamNativeAdRenderer(
    private val fallbackLayoutResId: Int = R.layout.layout_native_ads
) : GamNativeAdRenderer {
    override fun createLayout(context: Context, style: NativeAdStyle): NativeAdView {
        val layoutResId = style.layoutResId.takeIf { it != 0 } ?: fallbackLayoutResId
        return LayoutInflater.from(context).inflate(layoutResId, null) as NativeAdView
    }

    override fun bindData(adView: NativeAdView, nativeAd: NativeAd) {
        val titleView = adView.findViewById<TextView>(R.id.tv_ad_title)
        val ctaButton = adView.findViewById<TextView>(R.id.btn_ad_cta)
        val iconView = adView.findViewById<ImageView>(R.id.iv_ad_icon)
        val descView = adView.findViewById<TextView>(R.id.tv_ad_description)

        titleView?.text = nativeAd.headline.orEmpty()
        ctaButton?.text = nativeAd.callToAction ?: "INSTALL"
        descView?.text = nativeAd.body.orEmpty()
        nativeAd.icon?.drawable?.let {
            iconView?.setImageDrawable(it)
            iconView?.visibility = View.VISIBLE
        }

        adView.headlineView = titleView
        adView.callToActionView = ctaButton
        adView.iconView = iconView
        adView.bodyView = descView
        adView.advertiserView = null
        adView.priceView = null
        adView.storeView = null
        adView.registerNativeAd(nativeAd, null)
    }
}

class DefaultPangleNativeAdRenderer(
    private val fallbackLayoutResId: Int = R.layout.layout_pangle_native_ads
) : PangleNativeAdRenderer {
    override fun createLayout(context: Context, style: PangleNativeAdStyle): ViewGroup {
        val layoutResId = style.layoutResId.takeIf { it != 0 } ?: fallbackLayoutResId
        return LayoutInflater.from(context).inflate(layoutResId, null) as ViewGroup
    }

    override fun bindData(context: Context, adView: ViewGroup, nativeAdData: PAGNativeAdData) {
        bindCommonTextAndIcon(
            context = context,
            adView = adView,
            title = nativeAdData.title.orEmpty(),
            description = nativeAdData.description.orEmpty(),
            cta = nativeAdData.buttonText ?: "INSTALL",
            iconUrl = nativeAdData.icon?.imageUrl
        )
        adView.findViewById<FrameLayout>(R.id.fl_ad_logo)?.let { logoContainer ->
            logoContainer.removeAllViews()
            nativeAdData.adLogoView?.let {
                logoContainer.addView(it)
                logoContainer.visibility = View.VISIBLE
            } ?: run {
                logoContainer.visibility = View.GONE
            }
        }
    }

    override fun createViewBinder(container: ViewGroup, adView: ViewGroup): PAGViewBinder {
        return PAGViewBinder.Builder(container)
            .titleTextView(adView.findViewById(R.id.tv_ad_title))
            .descriptionTextView(adView.findViewById(R.id.tv_ad_description))
            .logoViewGroup(adView.findViewById(R.id.fl_ad_logo))
            .iconImageView(adView.findViewById(R.id.iv_ad_icon))
            .build()
    }

    override fun getClickViews(adView: ViewGroup): List<View> {
        return commonClickViews(adView)
    }
}

class DefaultToponNativeAdRenderer(
    private val fallbackLayoutResId: Int = R.layout.layout_topon_native_ads
) : ToponNativeAdRenderer {
    override fun createLayout(context: Context, style: ToponNativeAdStyle): ViewGroup {
        val layoutResId = style.layoutResId.takeIf { it != 0 } ?: fallbackLayoutResId
        return LayoutInflater.from(context).inflate(layoutResId, null) as ViewGroup
    }

    override fun bindData(adView: ViewGroup, material: TUNativeMaterial) {
        bindCommonTextAndIcon(
            context = adView.context,
            adView = adView,
            title = material.title.orEmpty(),
            description = material.descriptionText.orEmpty(),
            cta = material.callToActionText ?: "INSTALL",
            iconUrl = material.iconImageUrl
        )
    }

    override fun createPrepareInfo(adView: ViewGroup): TUNativePrepareInfo {
        return TUNativePrepareInfo().apply {
            adView.findViewById<TextView>(R.id.tv_ad_title)?.let {
                clickViewList.add(it)
                setTitleView(it)
            }
            adView.findViewById<TextView>(R.id.tv_ad_description)?.let {
                clickViewList.add(it)
                descView = it
            }
            adView.findViewById<TextView>(R.id.btn_ad_cta)?.let {
                clickViewList.add(it)
                ctaView = it
            }
            adView.findViewById<ImageView>(R.id.iv_ad_icon)?.let {
                clickViewList.add(it)
                setIconView(it)
            }
        }
    }
}

class DefaultAdmobFullScreenNativeAdRenderer : AdmobFullScreenNativeAdRenderer {
    override fun createLayout(context: Context): NativeAdView {
        return LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_native_ad, null) as NativeAdView
    }

    override fun bindData(adView: NativeAdView, nativeAd: NativeAd, lifecycleOwner: LifecycleOwner) {
        val titleView = adView.findViewById<TextView>(R.id.tv_ad_title)
        val descView = adView.findViewById<TextView>(R.id.tv_ad_description)
        val ctaButton = adView.findViewById<TextView>(R.id.btn_ad_cta)
        val iconView = adView.findViewById<ImageView>(R.id.iv_ad_icon)
        val mediaView = adView.findViewById<MediaView>(R.id.mv_ad_media)

        titleView?.text = nativeAd.headline.orEmpty()
        descView?.text = nativeAd.body.orEmpty()
        ctaButton?.text = nativeAd.callToAction ?: "OPEN"
        nativeAd.icon?.drawable?.let { iconView?.setImageDrawable(it) }
        nativeAd.mediaContent?.let { mediaView?.mediaContent = it }

        adView.headlineView = titleView
        adView.bodyView = descView
        adView.callToActionView = ctaButton
        adView.iconView = iconView
        adView.registerNativeAd(nativeAd, mediaView)
    }

    override fun createLoadingView(context: Context, container: ViewGroup) {
        container.removeAllViews()
        container.addView(LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_loading, container, false))
    }
}

class DefaultGamFullScreenNativeAdRenderer : GamFullScreenNativeAdRenderer {
    override fun createLayout(context: Context): NativeAdView {
        return LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_native_ad, null) as NativeAdView
    }

    override fun bindData(adView: NativeAdView, nativeAd: NativeAd, lifecycleOwner: LifecycleOwner) {
        val titleView = adView.findViewById<TextView>(R.id.tv_ad_title)
        val descView = adView.findViewById<TextView>(R.id.tv_ad_description)
        val ctaButton = adView.findViewById<TextView>(R.id.btn_ad_cta)
        val iconView = adView.findViewById<ImageView>(R.id.iv_ad_icon)
        val mediaView = adView.findViewById<MediaView>(R.id.mv_ad_media)

        titleView?.text = nativeAd.headline.orEmpty()
        descView?.text = nativeAd.body.orEmpty()
        ctaButton?.text = nativeAd.callToAction ?: "OPEN"
        nativeAd.icon?.drawable?.let {
            iconView?.setImageDrawable(it)
            iconView?.visibility = View.VISIBLE
        }
        nativeAd.mediaContent?.let { mediaContent ->
            mediaView?.mediaContent = mediaContent
            mediaView?.visibility = View.VISIBLE
        } ?: run {
            mediaView?.visibility = View.GONE
        }

        adView.headlineView = titleView
        adView.bodyView = descView
        adView.callToActionView = ctaButton
        adView.iconView = iconView
        adView.starRatingView = null
        adView.advertiserView = null
        adView.priceView = null
        adView.storeView = null
        adView.registerNativeAd(nativeAd, mediaView)
    }

    override fun createLoadingView(context: Context, container: ViewGroup) {
        container.removeAllViews()
        container.addView(LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_loading, container, false))
    }
}

class DefaultPangleFullScreenNativeAdRenderer : PangleFullScreenNativeAdRenderer {
    override fun createLayout(context: Context): ViewGroup {
        return LayoutInflater.from(context).inflate(R.layout.layout_pangle_fullscreen_native_ad, null) as ViewGroup
    }

    override fun bindData(context: Context, adView: ViewGroup, nativeAdData: PAGNativeAdData) {
        bindCommonTextAndIcon(context, adView, nativeAdData.title.orEmpty(), nativeAdData.description.orEmpty(), nativeAdData.buttonText ?: "INSTALL", nativeAdData.icon?.imageUrl)
        adView.findViewById<FrameLayout>(R.id.fl_ad_media)?.let { mediaContainer ->
            mediaContainer.removeAllViews()
            nativeAdData.mediaView?.let { mediaContainer.addView(it) }
        }
        adView.findViewById<FrameLayout>(R.id.fl_ad_logo)?.let { logoContainer ->
            logoContainer.removeAllViews()
            nativeAdData.adLogoView?.let { logoContainer.addView(it) }
        }
    }

    override fun createViewBinder(container: ViewGroup, adView: ViewGroup): PAGViewBinder {
        return PAGViewBinder.Builder(container)
            .titleTextView(adView.findViewById(R.id.tv_ad_title))
            .descriptionTextView(adView.findViewById(R.id.tv_ad_description))
            .logoViewGroup(adView.findViewById(R.id.fl_ad_logo))
            .iconImageView(adView.findViewById(R.id.iv_ad_icon))
            .mediaContentViewGroup(adView.findViewById(R.id.fl_ad_media))
            .build()
    }

    override fun getClickViews(adView: ViewGroup): List<View> = commonClickViews(adView)

    override fun createLoadingView(context: Context, container: ViewGroup) {
        container.removeAllViews()
        container.addView(LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_loading, container, false))
    }
}

class DefaultToponFullScreenNativeAdRenderer : ToponFullScreenNativeAdRenderer {
    override fun createLayout(context: Context): ViewGroup {
        return LayoutInflater.from(context).inflate(R.layout.layout_topon_fullscreen_native_ad, null) as ViewGroup
    }

    override fun bindData(adView: ViewGroup, material: TUNativeMaterial) {
        bindCommonTextAndIcon(adView.context, adView, material.title.orEmpty(), material.descriptionText.orEmpty(), material.callToActionText ?: "INSTALL", material.iconImageUrl)
        material.mainImageUrl?.let { mainImageUrl ->
            adView.findViewById<FrameLayout>(R.id.fl_ad_media)?.let { mediaContainer ->
                mediaContainer.removeAllViews()
                ImageView(mediaContainer.context).also {
                    it.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(mediaContainer.context).load(mainImageUrl).into(it)
                    mediaContainer.addView(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            }
        }
    }

    override fun createPrepareInfo(adView: ViewGroup): TUNativePrepareInfo {
        return TUNativePrepareInfo().apply {
            adView.findViewById<TextView>(R.id.tv_ad_title)?.let { setTitleView(it) }
            adView.findViewById<TextView>(R.id.tv_ad_description)?.let { descView = it }
            adView.findViewById<TextView>(R.id.btn_ad_cta)?.let { ctaView = it }
            adView.findViewById<ImageView>(R.id.iv_ad_icon)?.let { setIconView(it) }
            adView.findViewById<ViewGroup>(R.id.fl_ad_media)?.let { setMainImageView(it) }
        }
    }

    override fun createLoadingView(context: Context, container: ViewGroup) {
        container.removeAllViews()
        container.addView(LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_loading, container, false))
    }
}

private fun bindCommonTextAndIcon(
    context: Context,
    adView: ViewGroup,
    title: String,
    description: String,
    cta: String,
    iconUrl: String?
) {
    adView.findViewById<TextView>(R.id.tv_ad_title)?.text = title
    adView.findViewById<TextView>(R.id.tv_ad_description)?.text = description
    adView.findViewById<TextView>(R.id.btn_ad_cta)?.text = cta
    val iconView = adView.findViewById<ImageView>(R.id.iv_ad_icon)
    if (!iconUrl.isNullOrBlank() && iconView != null) {
        Glide.with(context).load(iconUrl).into(iconView)
    }
}

private fun commonClickViews(adView: ViewGroup): List<View> {
    return listOfNotNull(
        adView.findViewById<TextView>(R.id.tv_ad_title),
        adView.findViewById<TextView>(R.id.tv_ad_description),
        adView.findViewById<TextView>(R.id.btn_ad_cta),
        adView.findViewById<ImageView>(R.id.iv_ad_icon)
    )
}
