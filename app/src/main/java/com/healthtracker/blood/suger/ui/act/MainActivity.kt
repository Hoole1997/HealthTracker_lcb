package com.healthtracker.blood.suger.ui.act

// 移除广播接收器相关导入，改用页面可见状态检查月份变化
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.healthtracker.blood.suger.BuildConfig
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.databinding.ActivityMainBinding
import com.healthtracker.blood.suger.databinding.LayoutHomeTabItemBinding
import com.healthtracker.blood.suger.helper.CustomNotificationHelper
import com.healthtracker.blood.suger.push.recordLastActiveTime
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.blood.suger.ui.adapter.FragmentsAdapter
import com.healthtracker.blood.suger.ui.dialog.ExitDialog
import com.healthtracker.blood.suger.ui.fragment.HomeFragment
import com.healthtracker.blood.suger.ui.fragment.MedsFragment
import com.healthtracker.blood.suger.ui.fragment.RecordFragment
import com.healthtracker.blood.suger.ui.viewmodel.MainViewModel
import com.healthtracker.blood.suger.utils.loadBanner
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.Restore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseMVVMActivity<MainViewModel, ActivityMainBinding>() {

    companion object{
        private const val TAG = "MainActivity"
    }

    private var homeFrg: HomeFragment? = null
    private var medFrg: MedsFragment? = null
    private var recordFrg: RecordFragment? = null

    @Inject
    lateinit var customNotificationHelper: CustomNotificationHelper

    @Restore
    private var currentTabIndex = 0
    

    
    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()

        // 记录用户最后活跃时间（首页关闭时）
        recordLastActiveTime()
        "Recorded last active time when MainActivity stopped".logd(TAG)
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
                    R.string.home
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
                else -> R.string.home
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
            lifecycleScope.launch {
                fragment.getFormattedMonthFlow().collect { monthText ->
                    mViewBind.tvMonth.text = monthText
                    "Month display updated: $monthText".logd(TAG)
                }
            }
        }
    }

    override fun createViewBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun getVMModelClass() = MainViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewModel.startHealthService()
        with(mViewBind){
            if(BuildState.debug){
                ivSetting.visible()
            }
            ivSetting.clickWithDuration {
                "setting click".logd(TAG)
                // TODO: 添加设置页面功能
                HealthStatisticsActivity.start(this@MainActivity,HealthMetric.BMI)
            }

            // Debug 专用：长按发送测试通知
            if (BuildConfig.DEBUG) {
                ivSetting.setOnLongClickListener {
                    sendTestNotifications()
                    true
                }
            }

            ivRemind.clickWithDuration {
                startActivity<AlarmManageActivity>()
            }

            setupBottomNavBar()
            setupViewPager()
            loadBanner(adViewContainer)

            // 延迟处理通知点击参数，确保UI完全初始化
            mViewBind.root.postDelayed({
                handleNotificationAction(intent)
            }, 500)
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

    /**
     * 处理通知点击参数
     * 根据传入的action参数跳转到对应的记录页面
     */
    private fun handleNotificationAction(intent: Intent?) {
        val action = intent?.getStringExtra(HealthServiceConstants.EXTRA_NOTIFICATION_ACTION)

        if (action == null) {
            "No notification action, normal app launch".logd(TAG)
            return
        }

        "Handling notification action: $action".logd(TAG)

        when (action) {
            HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR -> {
                startActivity<BsRecordActivity>()
            }
            HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE -> {
                startActivity<BpRecordActivity>()
            }
            HealthServiceConstants.ACTION_VALUE_HEART_RATE -> {
                startActivity<HeartRateRecordActivity>()
            }
            HealthServiceConstants.ACTION_VALUE_HYDRATION -> {
                startActivity<HydrateActivity>()
            }
            // 自定义通知的新增动作处理分支
            HealthServiceConstants.ACTION_VALUE_HOMEPAGE -> {
                // 已在主页，无需跳转
            }
            HealthServiceConstants.ACTION_VALUE_CHOLESTEROL -> {
                startActivity<CholesterolRecordActivity>()
            }
            HealthServiceConstants.ACTION_VALUE_BMI -> {
                startActivity<BmiRecordActivity>()
            }
            HealthServiceConstants.ACTION_VALUE_HISTORY -> {
                startActivity<HistoryRecordActivity>()
            }
            HealthServiceConstants.ACTION_VALUE_MEDICATION -> {
                mViewBind.viewPagerHome.currentItem = 1
            }
            else -> {
                "Unknown notification action: $action".logd(TAG)
            }
        }
    }

    /**
     * 发送测试通知（仅 Debug 构建）
     * 一次性发送 11 条预配置的测试通知，用于验证 UI
     */
    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    private fun sendTestNotifications() {
        lifecycleScope.launch {
            val messages = PushMessage.createDefaultList()
            Toast.makeText(
                this@MainActivity,
                "Sending ${messages.size} test notifications...",
                Toast.LENGTH_SHORT
            ).show()

            "Starting to send ${messages.size} test notifications".logd(TAG)

            messages.forEachIndexed { index, message ->
                customNotificationHelper.showCustomNotification(message, scenario = PushScenario.BACKGROUND)
                "Test notification ${index + 1}/${messages.size} sent: ${message.title}".logd(TAG)

                // 添加间隔避免通知瞬间全部显示
                if (index < messages.size - 1) {
                    delay(300) // 300ms 间隔
                }
            }

            Toast.makeText(
                this@MainActivity,
                "✅ All ${messages.size} test notifications sent!",
                Toast.LENGTH_SHORT
            ).show()

            "All test notifications sent successfully".logd(TAG)
        }
    }

    override fun handleBackPress(): Boolean {
        ExitDialog.show(supportFragmentManager){
            finish()
        }
        return true
    }

}