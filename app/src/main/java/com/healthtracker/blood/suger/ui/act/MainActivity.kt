package com.healthtracker.blood.suger.ui.act

// 移除广播接收器相关导入，改用页面可见状态检查月份变化
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager.widget.ViewPager
import com.android.common.weather.WeatherActivity
import com.android.common.weather.util.TemperaturePreferences
import com.android.common.weather.util.WeatherIconMapper
import com.google.android.material.tabs.TabLayout
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.HtActivityMainBinding
import com.healthtracker.blood.suger.databinding.HtLayoutHomeTabItemBinding
import com.healthtracker.blood.suger.helper.CustomNotificationHelper
import com.healthtracker.blood.suger.permission.PermissionProvider
import com.healthtracker.blood.suger.permission.PermissionRequest
import com.healthtracker.blood.suger.service.HealthServiceConstants
import com.healthtracker.blood.suger.strategy.PushScenario
import com.healthtracker.blood.suger.ui.adapter.FragmentsAdapter
import com.healthtracker.blood.suger.ui.dialog.ActivityPerRequestDialog
import com.healthtracker.blood.suger.ui.dialog.ExitDialog
import com.healthtracker.blood.suger.ui.fragment.HomeFragment
import com.healthtracker.blood.suger.ui.fragment.InsightsFragment
import com.healthtracker.blood.suger.ui.fragment.MedsFragment
import com.healthtracker.blood.suger.ui.fragment.RecordFragment
import com.healthtracker.blood.suger.ui.tracker.HealthType
import com.healthtracker.blood.suger.ui.tracker.trackEnterPageClick
import com.healthtracker.blood.suger.ui.viewmodel.MainViewModel
import com.healthtracker.blood.suger.utils.loadBanner
import com.healthtracker.blood.suger.utils.loadRewardBidding
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
        // 刷新天气显示（温度单位可能已切换）
        mViewModel.refreshWeatherDisplay()
    }

    override fun onStop() {
        super.onStop()
        // 协程会被 repeatOnLifecycle 自动取消，无需手动管理
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    internal val homeFragmentAdapter =
        FragmentsAdapter(supportFragmentManager, 4, object : FragmentsAdapter.Callback {
            override fun createInstance(position: Int) = when (position) {
                0 -> HomeFragment()
                1 -> MedsFragment()
                2 -> InsightsFragment()
                3 -> RecordFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }

            override fun onInstance(position: Int, fragment: Fragment) {
                when (fragment) {
                    is HomeFragment -> homeFrg = fragment
                    is RecordFragment -> recordFrg = fragment
                    is MedsFragment -> medFrg = fragment
                    is InsightsFragment -> insightsFrg = fragment
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
                    R.string.ht_home
                }

                1 -> {
                    ReportDataManager.reportData("Meds_tab_enter",mapOf())
                    // Meds页面：隐藏设置和提醒按钮，显示月份
                    ivSetting.gone()
                    ivRemind.gone()
                    tvMonth.visible()
                    updateMonthDisplay()
                    medFrg?.needLoadAd()
                    R.string.ht_meds_manager
                }

                2 -> {
                    ReportDataManager.reportData("Insights_tab_enter",mapOf())
                    ivSetting.gone()
                    ivRemind.gone()
                    R.string.ht_insights
                }

                3 -> {
                    ReportDataManager.reportData("Tracker_tab_enter",mapOf())
                    // Record页面：隐藏提醒按钮
                    ivRemind.gone()
                    R.string.ht_tracker
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
            ivSetting.clickWithDuration {
                settingLauncher.launch(Intent(this@MainActivity, SettingActivity::class.java))
            }
            viewPagerHome.offscreenPageLimit = 0

            // Debug 专用：长按发送测试通知
            if (BuildState.debug) {
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

            // 天气数据点击事件（从 tvTitle 迁移到 llWeather）
            llWeather.clickWithDuration {
                ReportDataManager.reportData("weather_page_click")
                startActivity<WeatherActivity>()
            }

            // 延迟处理通知点击参数，确保UI完全初始化
            root.postDelayed({
                handleNotificationAction(intent)
            }, 500)
        }
        
        // Banner 和权限流程
        lifecycleScope.launch {
            delay(500)
            homeFrg?.highLightComplete?.await()
            if(BuildState.debug) "高亮完成，继续流程".logd(PermissionManager.TAG)
            loadBanner(mViewBind.adViewContainer, onClose = {
                if (!bannerShowComplete.isCompleted) {
                    bannerShowComplete.complete(true)
                }
                if(BuildState.debug) "首页折叠banner收起".logd(PermissionManager.TAG)
            }){
                if (!bannerShowComplete.isCompleted) {
                    bannerShowComplete.complete(it)
                }
                if(BuildState.debug) "非折叠banner或未展示成功".logd(PermissionManager.TAG)
            }
            if(BuildState.debug) "等待首页banner完成".logd(PermissionManager.TAG)
            bannerShowComplete.await()
            if(BuildState.debug) "首页banner完成，继续流程".logd(PermissionManager.TAG)
            permissionManager.checkNotificationPermission(this@MainActivity){
                if(BuildState.debug) "通知权限检查完成".logd(PermissionManager.TAG)
            }
        }
        
        // 观察天气数据
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.weatherData.collect { weatherData ->
                    weatherData?.let { data ->
                        with(mViewBind) {
                            // 更新天气图标
                            val iconRes = WeatherIconMapper.getIconResource(data.weatherIconId)
                            ivWeather.setImageResource(iconRes)
                            
                            // 更新温度
                            tvTemperature.text = data.getDisplayTemperature().toString()
                            
                            // 更新温度单位
                            tvTemperatureUnit.text = if (TemperaturePreferences.isCelsius()) "°C" else "°F"
                            
                            // 显示天气控件，隐藏标题
                            llWeather.visible()
                            tvTitle.gone()
                        }
                    }
                }
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

            HealthServiceConstants.ACTION_VALUE_WEATHER -> {
                startActivity<WeatherActivity>()
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