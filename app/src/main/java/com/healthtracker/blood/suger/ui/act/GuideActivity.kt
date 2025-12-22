package com.healthtracker.blood.suger.ui.act

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.ethanhua.skeleton.ViewSkeletonScreen
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.HtActivityGuideBinding
import com.healthtracker.blood.suger.databinding.HtFragmentGuide1Binding
import com.healthtracker.blood.suger.databinding.HtFragmentGuide2Binding
import com.healthtracker.blood.suger.databinding.HtFragmentGuide3Binding
import com.healthtracker.blood.suger.saveHasNewGuide
import com.healthtracker.blood.suger.utils.loadFullNative
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.visible
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ui.NativeAdStyle

class GuideActivity : BaseMVVMActivity<BaseViewModel, HtActivityGuideBinding>() {
    override fun createViewBinding() = HtActivityGuideBinding.inflate(layoutInflater)

    private val isShowNative = AdConfigManager.shouldShowGuideFullNative()
    override fun getVMModelClass() = BaseViewModel::class.java
    private val hasPassStep = mutableListOf<Int>()
    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind) {

            val frags = mutableListOf(GuideFrag1(), GuideFrag2(), GuideFrag3())
            if(isShowNative){
                frags.add(1,GuideFragAd())
            }
            viewpager.adapter = GuidePageAdapter(frags, this@GuideActivity)

            tvSkip.clickWithDuration {
                goNext()
            }

            btnNext.clickWithDuration{
                val currentItem = viewpager.currentItem
                if (currentItem == frags.size - 1) {
                    goNext()

                } else {
                    viewpager.setCurrentItem(currentItem + 1, true)
                }
                if (!hasPassStep.contains(currentItem)) {
                    hasPassStep.add(currentItem)

                }

            }
            viewpager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    indicatorView.onPageSelected(position)
                    group.visible()
                    if(isShowNative){
                        if(position < 1){
                            reportGuide(position + 2)
                        } else if(position > 1){
                            reportGuide(position + 1)
                        }
                    }else{
                        reportGuide(position + 2)
                    }
                    if (position == frags.size - 1) {
                        btnNext.text = getString(R.string.ht_start_health_journey)
                    } else {
                        btnNext.text = getString(R.string.ht_next)
                        if(isShowNative && position == 1){
                            group.gone()
                        }
                    }
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    indicatorView.onPageScrolled(position, positionOffset, positionOffsetPixels)
                }

            })

            indicatorView.apply {
                setSliderColor(ContextCompat.getColor(this@GuideActivity,R.color.color_F1F1F0), ContextCompat.getColor(this@GuideActivity,R.color.c5))
                setSliderWidth(resources.getDimension(com.healthtracker.framework.R.dimen.dp_6), selectedSliderWidth = resources.getDimension(com.healthtracker.framework.R.dimen.dp_14))
                setSliderHeight(resources.getDimension(com.healthtracker.framework.R.dimen.dp_6))
                setSlideMode(IndicatorSlideMode.WORM)
                setIndicatorStyle(IndicatorStyle.ROUND_RECT)
                setPageSize(frags.size)
                notifyDataChanged()
            }
        }
    }


    private fun goNext() {
        saveHasNewGuide()
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtras(intent)
        })
        finish()
    }

    private class GuidePageAdapter(private val frags: List<Fragment>, activity: FragmentActivity) :
        FragmentStateAdapter(activity) {
        override fun getItemCount() = frags.size

        override fun createFragment(position: Int) = frags[position]


    }

    fun scrollNextPage() {
        mViewBind?.viewpager?.let { pager ->
            val itemCount = pager.adapter?.itemCount ?: return
            val nextIndex = (pager.currentItem + 1).coerceAtMost(itemCount - 1)
            if (nextIndex != pager.currentItem) {
                pager.post { pager.setCurrentItem(nextIndex, true) }
            }
        }
    }

    companion object {
        const val IS_SHOW_GUIDE = "is_show_guide"
    }


}


fun reportGuide(flag:Int){
    ReportDataManager.reportData("Guide",mapOf("guide" to flag).also {
        if(BuildState.debug) "上报：Guide,data:$it".logd("GuideActivity")
    })
}


class GuideFrag1 : BaseMVVMFragment<BaseViewModel, HtFragmentGuide1Binding>() {

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtFragmentGuide1Binding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {

    }
}


class GuideFragAd : BaseMVVMFragment<BaseViewModel, HtFragmentGuide2Binding>() {

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtFragmentGuide2Binding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java
    private lateinit var skeleton: ViewSkeletonScreen
    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.apply {
            skeleton = ViewSkeletonScreen.Builder(adContainer)
                .load(R.layout.ht_fragment_guide_ad)
                .shimmer(true)
                .angle(30)
                .duration(1200)
                .color(com.healthtracker.framework.R.color.white)
                .show()

            if (context is GuideActivity) {
                val activity = context as GuideActivity
                activity.loadFullNative(
                    adContainer,
                    style = NativeAdStyle.createCustom(
                        net.corekit.monetize.R.layout.layout_fullscreen_native_ad,
                        "full_native"
                    ),
                    call = { success ->
                        if (success) {
                            skeleton.hide()
                        } else {
                           lifecycleScope.launch {
                               delay(2000)
                               if (!isAdded || activity.isFinishing || isDetached || activity.isDestroyed) {
                                   return@launch
                               }
                               activity.scrollNextPage()
                           }
                        }
                    }
                )
            }
        }

    }
}


class GuideFrag2 : BaseMVVMFragment<BaseViewModel, HtFragmentGuide1Binding>() {

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtFragmentGuide1Binding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.apply {
            ivGuide.setImageResource(R.mipmap.ht_ic_guide_2)
            tvGuideTitle.text = getString(R.string.ht_guide_title_2)
            tvGuideDes.text = getString(R.string.ht_guide_2_des)
        }

    }
}

class GuideFrag3 : BaseMVVMFragment<BaseViewModel, HtFragmentGuide3Binding>() {

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtFragmentGuide3Binding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {

    }
}
