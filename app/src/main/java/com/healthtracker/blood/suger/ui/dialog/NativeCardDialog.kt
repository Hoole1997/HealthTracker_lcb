package com.healthtracker.blood.suger.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.databinding.DialogCardNativeAdBinding
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.ads.NativeAds
import net.corekit.monetize.ui.NativeAdStyle

class NativeCardDialog : BaseVbDialogFragment<DialogCardNativeAdBinding>() {

    private var createTime = 0L

    companion object{
        private const val MIN_DISPLAY_TIME = 300L // 最少显示300毫秒
        private var lastShowTime = 0L
        private const val SHOW_INTERVAL = 60 * 1000L // 一分钟间隔
        fun showOncePerMinute(activity: FragmentActivity) {
            val currentTime = System.currentTimeMillis()

            // 检查是否在一分钟内已经触发过
            if (currentTime - lastShowTime < SHOW_INTERVAL) {
                return
            }

            // 无缓存跳过
            if(!NativeAds.getInstance().checkAdReady()){
                GlobalScope.launch {
                    NativeAds.getInstance().loadInAdvance(activity, BuildConfig.ADMOB_NATIVE_ID)
                }
                return
            }

            // 更新最后显示时间
            lastShowTime = currentTime
            NativeCardDialog().show(activity.supportFragmentManager)


        }
    }



    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogCardNativeAdBinding.inflate(layoutInflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        isCancelable = false
        mViewBind?.apply {
            close.clickWithDuration {
                dismissAllowingStateLoss()
            }

            if (context is FragmentActivity) {
                showLoading(this)
                (context as FragmentActivity).loadNative(
                    adsContainer,
                    style = NativeAdStyle.CARD_4
                ) {
                    if (!it) {
                        showLoading(this)
                        dismissAllowingStateLoss()
                    } else {
                        close.visible()
                    }
                }
            }
        }

    }

    private fun showLoading(binding: DialogCardNativeAdBinding) {
        val loadingView = LayoutInflater.from(context)
            .inflate(
                net.corekit.monetize.R.layout.layout_fullscreen_loading,
                binding.adsContainer,
                false
            )
        loadingView.setBackgroundColor(Color.TRANSPARENT)
        binding.adsContainer.removeAllViews()
        binding.adsContainer.addView(loadingView)
        binding.adsContainer.visible()
    }

    override fun dismiss() {
        lifecycleScope.launch {
            val elapsedTime = System.currentTimeMillis() - createTime

            // 如果显示时间不足300ms，延迟到300ms
            if (elapsedTime < MIN_DISPLAY_TIME) {
                val remainingTime = MIN_DISPLAY_TIME - elapsedTime
                delay(remainingTime)
            }

            withContext(Dispatchers.Main) {
                super.dismiss()
            }
        }
    }
}