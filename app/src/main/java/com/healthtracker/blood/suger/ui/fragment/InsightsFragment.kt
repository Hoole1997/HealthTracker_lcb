package com.healthtracker.blood.suger.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.FragmentInsightsBinding
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.getRobotoBold
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

class InsightsFragment: BaseMVVMFragment<BaseViewModel, FragmentInsightsBinding>() {
    companion object{
        private const val TAG = "InsightsFragment"
    }
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentInsightsBinding.inflate(inflater,parent,attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupMagicIndicator()

    }

    private val insightsTabsTitle = arrayOf(R.string.blood_suger,R.string.blood_pressure,R.string.heart_rate,R.string.hydrate,R.string.walking)
    private fun setupMagicIndicator(){
        mViewBind?.let { binding ->
            binding.magicIndicator.navigator = CommonNavigator(requireContext()).apply {
                adapter = object :CommonNavigatorAdapter(){
                    override fun getCount() = insightsTabsTitle.size

                    @SuppressLint("RestrictedApi")
                    override fun getTitleView(
                        context: Context?,
                        index: Int
                    ) = ColorTransitionPagerTitleView(requireContext()).apply {
                        normalColor = ContextCompat.getColor(requireContext(),R.color.t1)
                        selectedColor = ContextCompat.getColor(requireContext(),R.color.c5)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP,16f)
                        text = getString(insightsTabsTitle[index])
                        typeface = getRobotoBold(requireContext())
                        clickWithDuration {
                            "tab click:${getString(insightsTabsTitle[index])}".logd(TAG)
                            binding.viewpager.setCurrentItem(index,true)
                        }
                    }

                    override fun getIndicator(context: Context?) = LinePagerIndicator(requireContext()).apply {
                        setColors(ContextCompat.getColor(requireContext(),R.color.c5))
                        val displayMetrics = resources.displayMetrics
                        val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, displayMetrics)
                        val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, displayMetrics)
                        val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22f, displayMetrics)
                        roundRadius = radius
                        mode = LinePagerIndicator.MODE_EXACTLY
                        lineWidth = width
                        lineHeight = height
                    }
                }
            }

            ViewPagerHelper.bind(binding.magicIndicator,binding.viewpager)
        }
    }
}