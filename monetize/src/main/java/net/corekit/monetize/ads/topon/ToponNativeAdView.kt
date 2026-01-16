package net.corekit.monetize.ads.topon


import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.thinkup.nativead.api.NativeAd
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativeMaterial
import com.thinkup.nativead.api.TUNativePrepareInfo
import net.corekit.monetize.R
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ui.NativeAdStyle

/**
 * TopOn原生广告UI视图组件
 * 封装TopOn原生广告的布局创建和数据绑定逻辑
 */
class ToponNativeAdView {

    companion object {
        private const val TAG = "ToponNativeAdView"
    }

    /**
     * 创建并绑定TopOn原生广告视图到容器中
     * @param context 上下文
     * @param container 目标容器
     * @param nativeAd TopOn原生广告对象
     * @param style 广告样式，默认为标准样式
     * @return 是否绑定成功
     */
    fun bindNativeAdToContainer(
        context: Context,
        container: ViewGroup,
        nativeAd: NativeAd,
        style: NativeAdStyle = NativeAdStyle.STANDARD
    ): Boolean {
        return try {
            // 清空容器
            container.removeAllViews()

            // 判断是自渲染还是模板渲染
            val isNativeExpress = nativeAd.isNativeExpress
            
            if (isNativeExpress) {
                // 模板渲染：直接使用广告平台返回的渲染好的view
                bindTemplateAd(context, container, nativeAd)
            } else {
                // 自渲染：通过素材拼接
                bindSelfRenderAd(context, container, nativeAd, style)
            }

            AdLogger.d("TopOn原生广告视图绑定成功")
            true
        } catch (e: Exception) {
            AdLogger.e("TopOn原生广告视图绑定失败", e)
            false
        }
    }

    /**
     * 绑定模板渲染广告
     */
    private fun bindTemplateAd(
        context: Context,
        container: ViewGroup,
        nativeAd: NativeAd
    ) {
        try {
            // 创建 TUNativeAdView
            val nativeAdView = TUNativeAdView(context)
            
            // 渲染模板广告（View参数传null）
            nativeAd.renderAdContainer(nativeAdView, null)
            
            // 准备广告（TUNativePrepareInfo参数传null）
            nativeAd.prepare(nativeAdView, null)
            
            // 添加到容器
            container.addView(nativeAdView)
            
            AdLogger.d("TopOn模板渲染广告绑定完成")
        } catch (e: Exception) {
            AdLogger.e("TopOn模板渲染广告绑定失败", e)
            throw e
        }
    }

    /**
     * 绑定自渲染广告
     */
    private fun bindSelfRenderAd(
        context: Context,
        container: ViewGroup,
        nativeAd: NativeAd,
        style: NativeAdStyle
    ) {
        try {
            // 创建原生广告布局
            val adView = createNativeAdLayout(context, style)
            
            // 创建 TUNativeAdView
            val nativeAdView = TUNativeAdView(context)
            
            // 获取广告素材
            val material = nativeAd.adMaterial
            
            // 绑定广告数据
            bindNativeAdData(style, adView, material)
            
            // 创建 TUNativePrepareInfo 并绑定素材View
            val prepareInfo = createPrepareInfo(adView, material)
            
            // 渲染广告容器
            nativeAd.renderAdContainer(nativeAdView, adView)
            
            // 准备广告
            nativeAd.prepare(nativeAdView, prepareInfo)
            
            // 添加到容器
            container.addView(nativeAdView)
            
            AdLogger.d("TopOn自渲染广告绑定完成")
        } catch (e: Exception) {
            AdLogger.e("TopOn自渲染广告绑定失败", e)
            throw e
        }
    }

    /**
     * 创建原生广告布局
     */
    private fun createNativeAdLayout(
        context: Context,
        style: NativeAdStyle
    ): ViewGroup {
        return LayoutInflater.from(context)
            .inflate(style.getTopOnLayout(), null) as ViewGroup
    }

    /**
     * 创建 TUNativePrepareInfo
     */
    private fun createPrepareInfo(
        adView: ViewGroup,
        @Suppress("UNUSED_PARAMETER") material: TUNativeMaterial
    ): TUNativePrepareInfo {
        val prepareInfo = TUNativePrepareInfo()
        prepareInfo.closeView = null
        
        // 绑定标题View
        adView.findViewById<TextView>(R.id.ads_tv_title)?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.setTitleView(it)
        }
        
        // 绑定描述View
        adView.findViewById<TextView>(R.id.ads_tv_description)?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.descView = it
        }
        
        // 绑定CTA按钮View
        adView.findViewById<TextView>(R.id.ads_btn_cta)?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.ctaView = it
        }
        
        // 绑定图标View
        adView.findViewById<ImageView>(R.id.ads_iv_icon)?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.setIconView(it)
        }
        
        // 绑定主图View（如果有）
//        adView.findViewById<ImageView>(R.id.iv_ad_main_image)?.let {
//            prepareInfo.setMainImageView(it)
//        }

        return prepareInfo
    }

    /**
     * 绑定原生广告数据到视图
     */
    private fun bindNativeAdData(
        style: NativeAdStyle,
        adView: ViewGroup,
        material: TUNativeMaterial
    ) {
        try {
            val titleView = adView.findViewById<TextView>(R.id.ads_tv_title)
            val ctaButton = adView.findViewById<TextView>(R.id.ads_btn_cta)
            val iconView = adView.findViewById<ImageView>(R.id.ads_iv_icon)
            val descView = adView.findViewById<TextView>(R.id.ads_tv_description)

            // 设置广告标题
            titleView?.text = material.title ?: "Test TopOn Ads"

            // 设置CTA按钮
            ctaButton?.text = material.callToActionText ?: "INSTALL"

            // 设置广告描述
            descView?.text = material.descriptionText ?: ""

            // 设置图标
            material.iconImageUrl?.let { iconUrl ->
                iconView?.let { view ->
                    loadImage(view.context, iconUrl, view)
                    view.visibility = View.VISIBLE
                }
            } ?: run {
                iconView?.setImageResource(com.bytedance.R.drawable.applovin_ic_mediation_admob)
                iconView?.visibility = View.VISIBLE
            }

            AdLogger.d("TopOn原生广告数据绑定完成")

        } catch (e: Exception) {
            AdLogger.e("绑定TopOn原生广告数据失败", e)
        }
    }

    /**
     * 加载图片（使用Glide）
     */
    private fun loadImage(context: Context, imageUrl: String?, imageView: ImageView?) {
        if (imageUrl.isNullOrEmpty() || imageView == null) {
            return
        }
        try {
            Glide.with(context)
                .load(imageUrl)
                .into(imageView)
        } catch (e: Exception) {
            AdLogger.e("加载TopOn原生广告图片失败: $imageUrl", e)
        }
    }

    /**
     * 创建加载失败的占位视图
     */
    fun createErrorView(context: Context, errorMessage: String? = null): View {
        return TextView(context).apply {
            text = errorMessage ?: "广告加载失败"
            textSize = 12f
            setTextColor(0xFF999999.toInt())
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }
}

