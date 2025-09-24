package com.healthtracker.blood.suger.ui.act

// 移除广播接收器相关导入，改用页面可见状态检查月份变化
import android.os.Build
import android.os.Bundle
import androidx.compose.ui.geometry.Rect
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityMainBinding
import com.healthtracker.blood.suger.databinding.LayoutHomeTabItemBinding
import com.healthtracker.blood.suger.ui.adapter.FragmentsAdapter
import com.healthtracker.blood.suger.ui.fragment.HomeFragment
import com.healthtracker.blood.suger.ui.fragment.MedsFragment
import com.healthtracker.blood.suger.ui.fragment.RecordFragment
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.Restore
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseMVVMActivity<BaseViewModel, ActivityMainBinding>() {

    companion object{
        private const val TAG = "MainActivity"
    }

    private var homeFrg: HomeFragment? = null
    private var medFrg: MedsFragment? = null
    private var recordFrg: RecordFragment? = null

    @Restore
    private var currentTabIndex = 0
    

    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }

    internal val homeFragmentAdapter =
        FragmentsAdapter(supportFragmentManager, 3, object : FragmentsAdapter.Callback {
            override fun createInstance(position: Int) = when(position){
                0 -> HomeFragment()
                1 -> MedsFragment()
                2 -> RecordFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }

            override fun onInstance(position: Int, fragment: Fragment) {
                when (fragment) {
                    is HomeFragment -> homeFrg = fragment
                    is RecordFragment -> recordFrg = fragment
                    is MedsFragment -> medFrg = fragment
                }
            }
        })
    

    
    /**
     * 根据Tab位置更新UI状态
     * @param position Tab位置索引
     */
    private fun updateUIForTabPosition(position: Int) {
        with(mViewBind) {
            // 重置所有UI元素的默认状态
            ivRemind.visible()
            ivSetting.visible()
            tvMonth.gone()
            
            // 根据不同位置设置特定的UI状态和标题
            val titleRes = when (position) {
                0 -> {
                    // Home页面：显示默认状态
                    R.string.app_name
                }
                1 -> {
                    // Meds页面：隐藏设置和提醒按钮，显示月份
                    ivSetting.gone()
                    ivRemind.gone()
                    tvMonth.visible()
                    updateMonthDisplay()
                    R.string.meds_manager
                }
                2 -> {
                    // Record页面：隐藏提醒按钮
                    ivRemind.gone()
                    R.string.record
                }
                else -> R.string.app_name
            }
            tvTitle.text = getString(titleRes)
        }
    }
    
    /**
     * 更新月份显示
     * 从MedsFragment获取日期数据并更新UI
     */
    private fun updateMonthDisplay() {
        medFrg?.let { fragment ->
            val monthText = fragment.getFormattedMonth()
            mViewBind.tvMonth.text = monthText
        }
    }

    override fun createViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind){
            ivSetting.clickWithDuration {
                "setting click".logd(TAG)
            }

            ivRemind.clickWithDuration {
                startActivity<AlarmManageActivity>()
            }

            setupBottomNavBar()
            setupViewPager()
        }
    }

    /**
     * 设置底部导航栏
     */
    private fun setupBottomNavBar() {


        mViewBind.tbNav.apply {
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL
            removeAllTabs()

            val tabs = arrayListOf(
                Pair(R.drawable.selector_nav_home, R.string.home),
                Pair(R.drawable.selector_nav_meds, R.string.meds),
                Pair(R.drawable.selector_nav_record, R.string.record),

                )

            for (tab in tabs) {
                addBottomNavTab(this, tab.first, getString(tab.second))
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        mViewBind.viewPagerHome.currentItem = it.position
                        updateUIForTabPosition(it.position)
                    }
                }
            })

            if (currentTabIndex != 0) {
                getTabAt(currentTabIndex)?.select()
            }
        }
    }

    /**
     * 添加底部导航Tab
     */
    private fun addBottomNavTab(tabLayout: TabLayout, icon: Int, title: String) {
        tabLayout.addTab(tabLayout.newTab().apply {
            text = title
            customView = LayoutHomeTabItemBinding.inflate(layoutInflater, tabLayout, false).let {
                it.tvTabText.text = title
                it.ivTabIcon.setImageResource(icon)

                it.root
            }
            view.apply {
                isLongClickable = false
                if (Build.VERSION.SDK_INT > 26) {
                    tooltipText = ""
                }
            }
        })
    }

    /**
     * 设置ViewPager
     */
    private fun setupViewPager() {
        mViewBind.viewPagerHome.apply {
            offscreenPageLimit = homeFragmentAdapter.count
            adapter = homeFragmentAdapter
            isEnableScroll = true
            isSmoothScroll = true
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener{
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {}

                override fun onPageSelected(position: Int) {
                    mViewBind.tbNav.getTabAt(position)?.select()
                    updateUIForTabPosition(position)
                }

                override fun onPageScrollStateChanged(state: Int) {}

            })
        }
    }


}