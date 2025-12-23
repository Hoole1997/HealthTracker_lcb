package com.daily.health.manager.ui.act

// 移除广播接收器相关导入，改用页面可见状态检查月份变化
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.alarm.PermissionManager
import com.daily.health.manager.config.models.PushMessage
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtActivityMainBinding
import com.daily.health.manager.databinding.HtLayoutHomeTabItemBinding
import com.daily.health.manager.helper.CustomNotificationHelper
import com.daily.health.manager.permission.PermissionProvider
import com.daily.health.manager.permission.PermissionRequest
import com.daily.health.manager.service.HealthServiceConstants
import com.daily.health.manager.strategy.PushScenario
import com.daily.health.manager.ui.adapter.FragmentsAdapter
import com.daily.health.manager.ui.dialog.ActivityPerRequestDialog
import com.daily.health.manager.ui.dialog.ExitDialog
import com.daily.health.manager.ui.fragment.HomeFragment
import com.daily.health.manager.ui.fragment.InsightsFragment
import com.daily.health.manager.ui.fragment.MedsFragment
import com.daily.health.manager.ui.fragment.RecordFragment
import com.daily.health.manager.ui.fragment.SettingsFragment
import com.daily.health.manager.ui.tracker.HealthType
import com.daily.health.manager.ui.tracker.trackEnterPageClick
import com.daily.health.manager.ui.viewmodel.MainViewModel
import com.daily.health.manager.utils.loadBanner
import com.daily.health.manager.utils.loadRewardBidding
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.Restore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager
import org.koin.android.ext.android.inject

class MainActivity : BaseMVVMActivity<MainViewModel, HtActivityMainBinding>(), PermissionProvider {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var homeFrg: HomeFragment? = null
    private var medFrg: MedsFragment? = null
    private var recordFrg: RecordFragment? = null
    private var insightsFrg: InsightsFragment? = null
    private var settingsFrg: SettingsFragment? = null

    private val customNotificationHelper: CustomNotificationHelper by inject()
    private val permissionManager: PermissionManager by inject()
    @Restore
    private var currentTabIndex = 0

    private val bannerShowComplete = CompletableDeferred<Boolean>()

    private val settingLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                recreate()
            }
        }

    private var permissionRequest: PermissionRequest? = null
    
    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        // 协程会被 repeatOnLifecycle 自动取消，无需手动管理
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    internal val homeFragmentAdapter =
        FragmentsAdapter(supportFragmentManager, 5, object : FragmentsAdapter.Callback {
            override fun createInstance(position: Int) = when (position) {
                0 -> HomeFragment()
                1 -> MedsFragment()
                2 -> InsightsFragment()
                3 -> RecordFragment()
                4 -> SettingsFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }

            override fun onInstance(position: Int, fragment: Fragment) {
                when (fragment) {
                    is HomeFragment -> homeFrg = fragment
                    is RecordFragment -> recordFrg = fragment
                    is MedsFragment -> medFrg = fragment
                    is InsightsFragment -> insightsFrg = fragment
                    is SettingsFragment -> settingsFrg = fragment
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
            tvMonth.gone()

            // 根据不同位置设置特定的UI状态和标题
            val titleRes = when (position) {
                0 -> {
                    // Home页面：显示默认状态
                    R.string.ht_home
                }

                1 -> {
                    ReportDataManager.reportData("Meds_tab_enter",mapOf())
                    // Meds页面：隐藏设置和提醒按钮，显示月份
                    ivRemind.gone()
                    tvMonth.visible()
                    updateMonthDisplay()
                    medFrg?.needLoadAd()
                    R.string.ht_meds_manager
                }

                2 -> {
                    ReportDataManager.reportData("Insights_tab_enter",mapOf())
                    ivRemind.gone()
                    R.string.ht_insights
                }

                3 -> {
                    ReportDataManager.reportData("Tracker_tab_enter",mapOf())
                    // Record页面：隐藏提醒按钮
                    ivRemind.gone()
                    R.string.ht_tracker
                }

                4 -> {
                    ReportDataManager.reportData("Settings_tab_enter",mapOf())
                    ivRemind.gone()
                    R.string.ht_settings
                }

                else -> R.string.ht_home
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
                fragment.getDateFlow().collect { date ->
                    mViewBind.tvMonth.text = DateTimeUtils.formatMonthYear(date)
                }
            }
        }
    }

    override fun createViewBinding() = HtActivityMainBinding.inflate(layoutInflater)

    override fun getVMModelClass() = MainViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        ReportDataManager.reportData("Home_Show",mapOf())
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionRequest = permission()
            permissionRequest?.with(this)
        }
        mViewModel.startHealthService()
        with(mViewBind) {
            viewPagerHome.offscreenPageLimit = 0

            // Debug 专用：长按发送测试通知
            if (BuildState.debug) {
                ivRemind.setOnLongClickListener {
                    sendTestNotifications()
                    true
                }
            }

            ivRemind.clickWithDuration {
                startActivity<AlarmManageActivity>()
            }

            setupBottomNavBar()
            setupViewPager()

            // 延迟处理通知点击参数，确保UI完全初始化
            root.postDelayed({
                handleNotificationAction(intent)
            }, 500)
        }
        
        // Banner 和权限流程
        lifecycleScope.launch {
            delay(500)
            homeFrg?.highLightComplete?.await()
            loadBanner(mViewBind.adViewContainer, onClose = {
                if (!bannerShowComplete.isCompleted) {
                    bannerShowComplete.complete(true)
                }
            }){
                if (!bannerShowComplete.isCompleted) {
                    bannerShowComplete.complete(it)
                }
            }
            bannerShowComplete.await()
            if(BuildState.debug) "首页banner完成，继续流程".logd(PermissionManager.TAG)
            permissionManager.checkNotificationPermission(this@MainActivity){
                if(BuildState.debug) "通知权限检查完成".logd(PermissionManager.TAG)
            }
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
                Pair(R.drawable.ht_selector_nav_home, R.string.ht_home),
                Pair(R.drawable.ht_selector_nav_meds, R.string.ht_meds),
                Pair(R.drawable.ht_selector_nav_insights, R.string.ht_insights),
                Pair(R.drawable.ht_selector_nav_record, R.string.ht_tracker),
                Pair(R.drawable.ht_selector_nav_settings, R.string.ht_settings),

                )

            for (tab in tabs) {
                addBottomNavTab(this, tab.first, getString(tab.second))
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        currentTabIndex = it.position
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
            customView = HtLayoutHomeTabItemBinding.inflate(layoutInflater, tabLayout, false).let {
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
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    mViewBind.tbNav.getTabAt(position)?.select()
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

            HealthServiceConstants.ACTION_VALUE_STEPS -> {
                checkStepPermissionAndNavigate()
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

        intent.removeExtra(HealthServiceConstants.EXTRA_NOTIFICATION_ACTION)
    }

    /**
     * 发送测试通知（仅 Debug 构建）
     * 一次性发送 11 条预配置的测试通知，用于验证 UI
     */
    @androidx.annotation.RequiresPermission("android.permission.POST_NOTIFICATIONS")
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
                customNotificationHelper.showCustomNotification(
                    message,
                    scenario = PushScenario.BACKGROUND
                )
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
        ExitDialog.show(supportFragmentManager) {
            finish()
        }
        return true
    }

    override fun permission() = PermissionRequest(Manifest.permission.ACTIVITY_RECOGNITION)

    fun checkStepPermissionAndNavigate() {
        permissionRequest?.let {
            it.launch { isSuccess, showSettingsRedirect, hasPermission ->
                if (isSuccess || hasPermission) {
                    trackEnterPageClick(HealthType.WALKING_STEPS)
                    startActivity<StepCountActivity>()
                } else if (showSettingsRedirect) {
                    ActivityPerRequestDialog.show(supportFragmentManager) {
                        it.goSetting(this)
                        App.INSTANCE.isGoSetting = true
                    }
                }
            }
        } ?: {
            trackEnterPageClick(HealthType.WALKING_STEPS)
            startActivity<StepCountActivity>()
        }
    }


}