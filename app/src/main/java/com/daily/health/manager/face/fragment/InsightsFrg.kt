package com.daily.health.manager.face.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.daily.health.manager.R
import com.daily.health.manager.databinding.FcFragmentInsightsBinding
import com.daily.health.manager.databinding.FcItemInsightsViewpageBinding
import com.daily.health.manager.face.act.InsightsDetailAct
import com.daily.health.manager.face.adapter.InsightsArticleAdapter
import com.daily.health.manager.utils.InsightAssetPreparer
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.util.getRobotoBold
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.trackInsightsCategoryClick
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

class InsightsFrg: BaseMVVMFragment<BaseViewModel, FcFragmentInsightsBinding>() {

    private data class InsightCategory(
        val titleRes: Int,
        val assetKey: String
    )

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FcFragmentInsightsBinding.inflate(inflater,parent,attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupViewPager()
        setupMagicIndicator()

    }

    private val insightCategories = listOf(
        InsightCategory(R.string.fc_blood_suger, "blood_sugar"),
        InsightCategory(R.string.fc_blood_pressure, "blood_pressure"),
        InsightCategory(R.string.fc_heart_rate, "heart_rate"),
        InsightCategory(R.string.fc_hydrate, "hydrate"),
        InsightCategory(R.string.fc_walking, "walking")
    )

    private fun setupViewPager() {
        mViewBind?.viewpager?.apply {
            adapter = InsightsPagerAdapter(insightCategories)
            offscreenPageLimit = insightCategories.size
            isEnableScroll = true
            isSmoothScroll = true
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {

                }

                override fun onPageSelected(position: Int) {
                   "onPageSelected: $position".logd("InsightsFragment")
                    val healthType = when(position) {
                        0 -> HealthType.BLOOD_SUGAR
                        1 -> HealthType.BLOOD_PRESSURE
                        2 -> HealthType.HEART_RATE
                        3 -> HealthType.HYDRATE
                        4 -> HealthType.WALKING_STEPS
                        else -> HealthType.BLOOD_SUGAR // Default fallback
                    }
                    requireContext().trackInsightsCategoryClick(healthType)
                }

                override fun onPageScrollStateChanged(state: Int) {

                }

            })
        }
    }

    private fun setupMagicIndicator(){
        mViewBind?.let { binding ->
            binding.magicIndicator.navigator = CommonNavigator(requireContext()).apply {
                adapter = object :CommonNavigatorAdapter(){
                    override fun getCount() = insightCategories.size

                    @SuppressLint("RestrictedApi")
                    override fun getTitleView(
                        context: Context?,
                        index: Int
                    ) = ColorTransitionPagerTitleView(requireContext()).apply {
                        normalColor = ContextCompat.getColor(requireContext(),R.color.t1)
                        selectedColor = ContextCompat.getColor(requireContext(),R.color.c5)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP,16f)
                        text = getString(insightCategories[index].titleRes)
                        typeface = getRobotoBold(requireContext())
                        clickWithDuration {
                            binding.viewpager.setCurrentItem(index,true)
                        }
                        val paddingStart = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics).toInt()
                        if(index == 0){
                            setPadding(paddingStart, 0, paddingStart / 2, 0)
                        }else if( index == insightCategories.size - 1){
                            setPadding(paddingStart / 2,0,paddingStart,0)
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

    private fun openArticleDetail(article: InsightAssetPreparer.InsightArticle) {
        InsightsDetailAct.start(requireContext(), article)
    }

    private inner class InsightsPagerAdapter(
        private val categories: List<InsightCategory>
    ) : PagerAdapter() {

        private val inflater = LayoutInflater.from(requireContext())

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val itemBinding = FcItemInsightsViewpageBinding.inflate(inflater, container, false)
            itemBinding.rvInsights.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = InsightsArticleAdapter(::openArticleDetail).also { adapter ->
                    adapter.submitList(
                        InsightAssetPreparer.getArticles(categories[position].assetKey)
                    )
                }
            }
            container.addView(itemBinding.root)
            return itemBinding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount() = categories.size

        override fun isViewFromObject(view: View, `object`: Any) = view === `object`
    }
}
