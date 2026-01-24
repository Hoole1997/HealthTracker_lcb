package net.corekit.monetize.ads.topon

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.thinkup.nativead.api.NativeAd
import com.thinkup.nativead.api.TUNativeAdView
import com.thinkup.nativead.api.TUNativePrepareInfo
import net.corekit.monetize.R
import net.corekit.monetize.ads.log.AdLogger

/**
 * TopOn 全屏原生广告视图
 */
class ToponFullScreenNativeAdView {

    companion object {
        private const val TAG = "ToponFullNaView"
    }

    /**
     * 绑定全屏原生广告到容器
     */
    fun bindFullScreenNativeAdToContainer(
        context: Context,
        container: ViewGroup,
        nativeAd: NativeAd,
        lifecycleOwner: LifecycleOwner
    ): Boolean {
        return try {
            container.removeAllViews()

            // 判断是模板渲染还是自渲染
            if (nativeAd.isNativeExpress) {
                bindTemplateAd(context, container, nativeAd)
            } else {
                bindSelfRenderAd(context, container, nativeAd)
            }

            lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    super.onDestroy(owner)
                    lifecycleOwner.lifecycle.removeObserver(this)
                }
            })

            AdLogger.d("[$TAG] 全屏原生广告视图绑定成功")
            true
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 全屏原生广告视图绑定失败", e)
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
        val nativeAdView = TUNativeAdView(context)
        nativeAd.renderAdContainer(nativeAdView, null)
        nativeAd.prepare(nativeAdView, null)
        container.addView(nativeAdView)
        AdLogger.d("[$TAG] 模板渲染广告绑定完成")
    }

    /**
     * 绑定自渲染广告
     */
    private fun bindSelfRenderAd(
        context: Context,
        container: ViewGroup,
        nativeAd: NativeAd
    ) {
        val adView = LayoutInflater.from(context)
            .inflate(R.layout.layout_topon_fullscreen_native_ad, null) as ViewGroup

        val nativeAdView = TUNativeAdView(context)
        val material = nativeAd.adMaterial

        // 绑定广告数据
        val titleView = adView.findViewById<TextView>(R.id.ads_tv_title)
        val descView = adView.findViewById<TextView>(R.id.ads_tv_description)
        val ctaButton = adView.findViewById<TextView>(R.id.ads_btn_cta)
        val iconView = adView.findViewById<ImageView>(R.id.ads_iv_icon)

        titleView?.text = material.title ?: "Advertisement"
        descView?.text = material.descriptionText ?: ""
        ctaButton?.text = material.callToActionText ?: "Learn More"

        // 加载图标
        material.iconImageUrl?.let { iconUrl ->
            iconView?.let { view ->
                try {
                    Glide.with(context).load(iconUrl).into(view)
                    view.visibility = View.VISIBLE
                } catch (e: Exception) {
                    view.visibility = View.GONE
                }
            }
        }

        // 创建 TUNativePrepareInfo
        val prepareInfo = TUNativePrepareInfo()
        prepareInfo.closeView = null
        titleView?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.setTitleView(it)
        }
        descView?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.descView = it
        }
        ctaButton?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.ctaView = it
        }
        iconView?.let {
            prepareInfo.clickViewList.add(it)
            prepareInfo.setIconView(it)
        }

        // 渲染广告容器
        nativeAd.renderAdContainer(nativeAdView, adView)
        nativeAd.prepare(nativeAdView, prepareInfo)

        container.addView(nativeAdView)
        AdLogger.d("[$TAG] 自渲染广告绑定完成")
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
            AdLogger.e("TopOn全屏原生加载视图创建失败", e)
        }
    }
}
