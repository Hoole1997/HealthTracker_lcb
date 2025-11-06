package net.corekit.monetize.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import net.corekit.monetize.R
import net.corekit.monetize.ads.log.AdLogger

/**
 * 原生广告UI视图组件
 * 封装原生广告的布局创建和数据绑定逻辑
 */
class NativeAdView {

    companion object {
        private const val TAG = "NativeAdView"
    }

    /**
     * 创建并绑定原生广告视图到容器中
     * @param context 上下文
     * @param container 目标容器
     * @param nativeAd 原生广告数据
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

            // 创建原生广告布局
            val adView = createNativeAdLayout(context, style)

            // 绑定广告数据
            bindNativeAdData(style, adView, nativeAd)

            // 添加到容器
            container.addView(adView)

            if(context is LifecycleOwner){
                context.lifecycle.addObserver(object : DefaultLifecycleObserver{
                    override fun onDestroy(owner: LifecycleOwner) {
                        super.onDestroy(owner)
                        nativeAd.adEventCallback = null
                        nativeAd.destroy()
                        context.lifecycle.removeObserver(this)
                    }
                })
            }

            AdLogger.d("原生广告视图绑定成功")
            true
        } catch (e: Exception) {
            AdLogger.e("原生广告视图绑定失败", e)
            false
        }
    }

    /**
     * 创建原生广告布局
     */
    private fun createNativeAdLayout(
        context: Context,
        style: NativeAdStyle
    ): com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView {
        return LayoutInflater.from(context)
            .inflate(style.layoutResId, null) as com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
    }

    /**
     * 绑定原生广告数据到视图
     */
    private fun bindNativeAdData(
        style: NativeAdStyle,
        adView: com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView,
        nativeAd: NativeAd
    ) {
        try {
            val titleView = adView.findViewById<TextView>(R.id.ads_tv_title)
            val ctaButton = adView.findViewById<TextView>(R.id.ads_btn_cta)
            val iconView = adView.findViewById<ImageView>(R.id.ads_iv_icon)
            val ratingLayout = adView.findViewById<LinearLayout>(R.id.ads_start_ll)
            val descView = adView.findViewById<TextView>(R.id.ads_tv_description)
            val mediaView = adView.findViewById<MediaView>(R.id.ads_mv_media)

            // 设置广告标题
            titleView?.text = nativeAd.headline ?: "Test Google Ads"

            // 设置CTA按钮
            ctaButton?.text = nativeAd.callToAction ?: "INSTALL"

            // 设置广告描述
            descView?.text = nativeAd.body


            // 设置媒体内容（如果有）
            nativeAd.mediaContent?.let { mediaContent ->
                mediaView?.mediaContent = nativeAd.mediaContent
                mediaView?.visibility = View.VISIBLE
            } ?: run {
                mediaView?.visibility = View.GONE
            }

            // 设置评分（如果有）
            nativeAd.starRating?.let { rating ->
                // 显示评分布局
                ratingLayout?.visibility = View.VISIBLE
                // 根据评分动态设置星级图标
                updateStarRating(style, ratingLayout, rating.toFloat())
            } ?: run {
                // 如果没有评分，显示默认评分（4.5分）
                ratingLayout?.visibility = View.VISIBLE
                updateStarRating(style, ratingLayout, 4.5f)
            }

            // 设置图标
            nativeAd.icon?.let { icon ->
                iconView?.setImageDrawable(icon.drawable)
                iconView?.visibility = View.VISIBLE
            } ?: run {
                iconView?.setImageResource(android.R.drawable.ic_menu_info_details)
                iconView?.visibility = View.VISIBLE
            }

            // 绑定AdMob NativeAdView
            adView.headlineView = titleView
            adView.callToActionView = ctaButton
            adView.iconView = iconView
            adView.bodyView = descView
            adView.starRatingView = ratingLayout
            adView.advertiserView = null
            adView.priceView = null
            adView.storeView = null
            // 绑定广告数据
            adView.registerNativeAd(nativeAd,mediaView)

            AdLogger.d("原生广告数据绑定完成")

        } catch (e: Exception) {
            AdLogger.e("绑定原生广告数据失败", e)
        }
    }

    /**
     * 更新星级评分显示
     * @param ratingLayout 评分布局容器
     * @param rating 评分值 (0.0-5.0)
     */
    private fun updateStarRating(style: NativeAdStyle, ratingLayout: LinearLayout?, rating: Float) {
        ratingLayout?.let { layout ->
            // 确保评分在有效范围内
            val validRating = rating.coerceIn(0f, 5f)

            // 获取所有星级图标
            val starViews = mutableListOf<ImageView>()
            for (i in 0 until layout.childCount) {
                val child = layout.getChildAt(i)
                if (child is ImageView) {
                    starViews.add(child)
                }
            }

            // 如果找到了星级图标，则更新它们
            if (starViews.isNotEmpty()) {
                updateStarIcons(style, starViews, validRating)
            }
        }
    }

    /**
     * 更新星级图标显示
     * @param starViews 星级图标列表
     * @param rating 评分值
     */
    private fun updateStarIcons(style: NativeAdStyle, starViews: List<ImageView>, rating: Float) {
        val fullStars = rating.toInt() // 满星数量
        val hasHalfStar = rating % 1 >= 0.5f // 是否有半星

        starViews.forEachIndexed { index, imageView ->
            when {
                index < fullStars -> {
                    // 满星
                    imageView.setImageResource(if (style.description == "card2") R.drawable.ic_star_filled_green else R.drawable.ic_star_filled)
                    imageView.visibility = android.view.View.VISIBLE
                }

                index == fullStars && hasHalfStar -> {
                    // 半星
                    imageView.setImageResource((if (style.description == "card2") R.drawable.ic_star_half_green else R.drawable.ic_star_half))
                    imageView.visibility = android.view.View.VISIBLE
                }

                else -> {
                    // 空星 - 使用半星图标但设置为透明
                    imageView.setImageResource(R.drawable.ic_star_empty)
                    imageView.visibility = android.view.View.VISIBLE
                }
            }
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
            gravity = android.view.Gravity.CENTER
            setPadding(16, 16, 16, 16)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

} 