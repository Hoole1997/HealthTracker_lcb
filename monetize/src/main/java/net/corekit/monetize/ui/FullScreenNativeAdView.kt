package net.corekit.monetize.ui

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import net.corekit.monetize.R
import net.corekit.monetize.ads.log.AdLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 全屏原生广告UI视图组件
 * 封装全屏原生广告的布局创建、数据绑定和交互逻辑
 */
class FullScreenNativeAdView {
    
    companion object {
        private const val TAG = "FullScreenNativeAdView"
        private const val AUTO_CLOSE_DELAY = 10000L // 10秒自动关闭
    }
    
    /**
     * 创建并绑定全屏原生广告视图到容器中
     * @param context 上下文
     * @param container 目标容器
     * @param nativeAd 原生广告数据
     * @param lifecycleOwner 生命周期所有者（用于倒计时）
     * @param onCloseCallback 关闭回调
     * @return 是否绑定成功
     */
    fun bindFullScreenNativeAdToContainer(
        context: Context,
        container: ViewGroup,
        nativeAd: NativeAd,
        lifecycleOwner: LifecycleOwner
    ): Boolean {
        return try {
            // 清空容器
            container.removeAllViews()
            
            // 创建全屏原生广告布局
            val adView = createFullScreenNativeAdLayout(context)
            
            // 绑定广告数据
            bindFullScreenNativeAdData(adView, nativeAd, lifecycleOwner)
            
            // 添加到容器
            container.addView(adView)

            AdLogger.d("全屏原生广告视图绑定成功")
            true
        } catch (e: Exception) {
            AdLogger.e("全屏原生广告视图绑定失败", e)
            false
        }
    }
    
    /**
     * 创建全屏加载视图
     */
    fun createFullScreenLoadingView(
        context: Context,
        container: ViewGroup,
    ) {
        try {
            container.removeAllViews()
            
            val loadingView = LayoutInflater.from(context)
                .inflate(R.layout.layout_fullscreen_loading, container, false)
            

            container.addView(loadingView)
            
        } catch (e: Exception) {
            AdLogger.e("创建全屏加载视图失败", e)
        }
    }
    
    /**
     * 创建全屏原生广告布局
     */
    private fun createFullScreenNativeAdLayout(context: Context): com.google.android.gms.ads.nativead.NativeAdView {
        return LayoutInflater.from(context).inflate(R.layout.layout_fullscreen_native_ad, null) as com.google.android.gms.ads.nativead.NativeAdView
    }
    
    /**
     * 绑定全屏原生广告数据到视图
     */
    private fun bindFullScreenNativeAdData(
        adView: com.google.android.gms.ads.nativead.NativeAdView, 
        nativeAd: NativeAd,
        lifecycleOwner: LifecycleOwner,
    ) {
        try {
            val titleView = adView.findViewById<TextView>(R.id.ads_tv_title)
            val descView = adView.findViewById<TextView>(R.id.ads_tv_description)
            val ctaButton = adView.findViewById<TextView>(R.id.ads_btn_cta)
            val iconView = adView.findViewById<ImageView>(R.id.ads_iv_icon)
            val mediaView = adView.findViewById<MediaView>(R.id.ads_mv_media)

            // 设置广告标题
            titleView?.text = nativeAd.headline ?: "Test Google Ads"
            
            // 设置广告描述
            descView?.text = nativeAd.body ?: "Test Google Ads"
            
            // 设置CTA按钮
            ctaButton?.text = nativeAd.callToAction ?: "Open"

            
            // 设置图标
            nativeAd.icon?.let { icon ->
                iconView?.setImageDrawable(icon.drawable)
                iconView?.visibility = View.VISIBLE
            } ?: run {
                iconView?.setImageResource(android.R.drawable.ic_menu_info_details)
                iconView?.visibility = View.VISIBLE
            }
            
            // 设置媒体内容（如果有）
            nativeAd.mediaContent?.let { mediaContent ->
                mediaView?.setMediaContent(mediaContent)
                mediaView?.visibility = View.VISIBLE
            } ?: run {
                mediaView?.visibility = View.GONE
            }
            
            // 绑定 AdMob NativeAdView
            adView.headlineView = titleView
            adView.bodyView = descView
            adView.callToActionView = ctaButton
            adView.iconView = iconView
            adView.starRatingView = null
            adView.mediaView = mediaView
            adView.advertiserView = null
            adView.priceView = null
            adView.storeView = null
            
            // 绑定广告数据
            adView.setNativeAd(nativeAd)

            AdLogger.d("全屏原生广告数据绑定完成")
            
        } catch (e: Exception) {
            AdLogger.e( "绑定全屏原生广告数据失败", e)
        }
    }
    
}