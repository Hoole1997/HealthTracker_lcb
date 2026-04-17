package com.daily.health.manager.face.act

// 移除广播接收器相关导入，改用页面可见状态检查月份变化
import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager.widget.ViewPager
import com.android.common.weather.WeatherActivity
import com.android.common.weather.util.TemperaturePreferences
import com.android.common.weather.util.WeatherIconMapper
import com.app.raise.AppraiseManager
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.alarm.PermissionManager
import com.daily.health.manager.config.models.PushMessage
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.FcActivityMainBinding
import com.daily.health.manager.databinding.FcLayoutHomeTabItemBinding
import com.daily.health.manager.face.adapter.FragmentsAdapter
import com.daily.health.manager.face.compose.HomeFeatureGuideOverlay
import com.daily.health.manager.face.compose.HomeGuideOverlayUi
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.face.dialog.ActivityPerRequestDialog
import com.daily.health.manager.face.dialog.ExitDialog
import com.daily.health.manager.face.fragment.HomeFrg
import com.daily.health.manager.face.fragment.InsightsFrg
import com.daily.health.manager.face.fragment.MedsFrg
import com.daily.health.manager.face.fragment.RecordFrg
import com.daily.health.manager.face.fragment.SettingsFrg
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.trackEnterPageClick
import com.daily.health.manager.face.viewmodel.MainViewModel
import com.daily.health.manager.helper.CustomNotificationHelper
import com.daily.health.manager.helper.HealthTrackerEvaluateListener
import com.daily.health.manager.permission.PermissionProvider
import com.daily.health.manager.permission.PermissionRequest
import com.daily.health.manager.service.HealthServiceConstants
import com.daily.health.manager.strategy.PushScenario
import com.daily.health.manager.utils.loadBanner
import com.google.android.material.tabs.TabLayout
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.Restore
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.AdPosition
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume

class MainAct : BaseMVVMActivity<MainViewModel, FcActivityMainBinding>(), PermissionProvider {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var homeFrg: HomeFrg? = null
    private var medFrg: MedsFrg? = null
    private var recordFrg: RecordFrg? = null
    private var insightsFrg: InsightsFrg? = null
    private var settingsFrg: SettingsFrg? = null

    private val customNotificationHelper: CustomNotificationHelper by inject()
    private val permissionManager: PermissionManager by inject()
    @Restore
    private var currentTabIndex = 0

    private val bannerShowComplete = CompletableDeferred<Boolean>()

    private val homeFrgReady = CompletableDeferred<HomeFrg>()
    private val hostHomeGuideOverlayUi = MutableStateFlow<HomeGuideOverlayUi?>(null)
    private val hostCurrentTab = MutableStateFlow(0)

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
        // 检查是否需要显示评分弹窗（新手引导完成后触发）
        checkPendingRateDialog()
    }

    /**
     * 检查是否有待处理的评分弹窗
     * 新手引导完成后会设置标记，在 onResume 时检测并显示评分弹窗
     */
    private fun checkPendingRateDialog() {
        // 仅在从外部返回时检查（如从记录页返回）
        checkAndShowRateDialogAfterOnboarding()
    }

    /**
     * 检查并显示新手引导后的评分弹窗
     */
    private fun checkAndShowRateDialogAfterOnboarding() {
        val pendingRate = SpUtils.getBoolean(
            HealthTrackerEvaluateListener.KEY_PENDING_RATE_AFTER_ONBOARDING, false
        )
        val hasRated = SpUtils.getBoolean(
            HealthTrackerEvaluateListener.KEY_HAS_RATED, false
        )

        if (pendingRate && !hasRated) {
            // 清除待评分标记
            SpUtils.putBoolean(
                HealthTrackerEvaluateListener.KEY_PENDING_RATE_AFTER_ONBOARDING, false
            )
            // 延迟显示评分弹窗，确保界面完全加载
            mViewBind.root.postDelayed({
                showRateDialog("home")
            }, 500)
        }
    }

    /**
     * 显示评分弹窗
     * @param source 来源标识，用于埋点统计
     */
    private fun showRateDialog(source: String) {
        ReportDataManager.reportData("rate_us_show", mapOf("source" to source))
        val manager = AppraiseManager(this, star5GoMarket = false)
        manager.showAppraiseDialog(HealthTrackerEvaluateListener(this, source))
    }

    override fun onStop() {
        super.onStop()
        // 协程会被 repeatOnLifecycle 自动取消，无需手动管理
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    internal val homeFrgAdapter =
        FragmentsAdapter(supportFragmentManager, 5, object : FragmentsAdapter.Callback {
            override fun createInstance(position: Int) = when (position) {
                0 -> HomeFrg()
                1 -> MedsFrg()
                2 -> InsightsFrg()
                3 -> RecordFrg()
                4 -> SettingsFrg()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }

            override fun onInstance(position: Int, fragment: Fragment) {
                when (fragment) {
                    is HomeFrg -> {
                        homeFrg = fragment
                        if (!homeFrgReady.isCompleted) {
                            homeFrgReady.complete(fragment)
                        }
                    }
                    is RecordFrg -> recordFrg = fragment
                    is MedsFrg -> medFrg = fragment
                    is InsightsFrg -> insightsFrg = fragment
                    is SettingsFrg -> settingsFrg = fragment
                }
            }
        })


    /**
     * 根据Tab位置更新UI状态
     * @param position Tab位置索引
     */
    private fun updateUIForTabPosition(position: Int) {
        hostCurrentTab.value = position
        window.statusBarColor = if (position == 0 && hostHomeGuideOverlayUi.value != null) {
            android.graphics.Color.parseColor("#99000000")
        } else {
            android.graphics.Color.TRANSPARENT
        }
        with(mViewBind) {
            applyHostBackgroundForTab(position)
            llWeather.gone()
            tvTitle.visible()
            if (NotificationFeatureSwitch.reminderEntryEnabled) {
                ivRemind.visible()
            } else {
                ivRemind.gone()
            }
            tvMonth.gone()

            // 根据不同位置设置特定的UI状态和标题
            val titleRes = when (position) {
                0 -> {
                    // Home页面：显示默认状态
                    R.string.fc_home
                }

                1 -> {
                    ReportDataManager.reportData("Meds_tab_enter",mapOf())
                    // Meds页面：隐藏设置和提醒按钮，显示月份
                    ivRemind.gone()
                    tvMonth.visible()
                    updateMonthDisplay()
                    medFrg?.needLoadAd()
                    R.string.fc_meds_manager
                }

                2 -> {
                    ReportDataManager.reportData("Insights_tab_enter",mapOf())
                    ivRemind.gone()
                    R.string.fc_insights
                }

                3 -> {
                    ReportDataManager.reportData("Tracker_tab_enter",mapOf())
                    // Record页面：隐藏提醒按钮
                    ivRemind.gone()
                    R.string.fc_record
                }

                4 -> {
                    ReportDataManager.reportData("Settings_tab_enter",mapOf())
                    ivRemind.gone()
                    R.string.fc_settings
                }

                else -> R.string.fc_home
            }
            tvTitle.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (position == 1) 18f else 20f
            )
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

    private fun applyHostBackgroundForTab(position: Int) {
        val isHomeTab = position == 0
        val homeColor = ContextCompat.getColor(this, R.color.fc_home_host_bg)
        val defaultColor = ContextCompat.getColor(this, R.color.c1)
        val subpageColor = ContextCompat.getColor(this, R.color.fc_subpage_bg)

        if (isHomeTab) {
            mViewBind.root.setBackgroundResource(R.drawable.fc_bg_home_host_gradient)
            mViewBind.areaBar.setBackgroundColor(Color.TRANSPARENT)
            mViewBind.viewPagerHome.setBackgroundColor(Color.TRANSPARENT)
            mViewBind.adViewContainer.setBackgroundColor(homeColor)
        } else {
            mViewBind.root.setBackgroundColor(defaultColor)
            mViewBind.areaBar.setBackgroundColor(defaultColor)
            mViewBind.viewPagerHome.setBackgroundColor(
                if (position in 2..4) subpageColor else defaultColor
            )
            mViewBind.adViewContainer.setBackgroundResource(R.drawable.fc_bg_bottom_banner)
        }
    }

    override fun createViewBinding() = FcActivityMainBinding.inflate(layoutInflater)

    override fun getVMModelClass() = MainViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        ReportDataManager.reportData("Home_Show",mapOf())
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionRequest = permission()
            permissionRequest?.with(this)
        }
        mViewModel.startHealthService()
        mViewBind.composeHomeGuideOverlay.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind.composeHomeGuideOverlay.setContent {
            val overlayUi by hostHomeGuideOverlayUi.collectAsStateWithLifecycle()
            val currentTab by hostCurrentTab.collectAsStateWithLifecycle()

            HealthTrackerTheme {
                if (currentTab == 0) {
                    overlayUi?.let { ui ->
                        HomeFeatureGuideOverlay(
                            step = ui.step,
                            anchorRects = ui.anchorRects,
                            onNextClick = { homeFrg?.onHomeGuideNext() },
                            onDismiss = { homeFrg?.dismissHomeGuide() },
                            onTargetClick = { homeFrg?.handleHomeGuideTargetClick() },
                        )
                    }
                }
            }
        }
        with(mViewBind) {
            viewPagerHome.offscreenPageLimit = 0

            if (NotificationFeatureSwitch.reminderEntryEnabled) {
                // Debug 专用：长按发送测试通知
                if (BuildState.debug) {
                    ivRemind.setOnLongClickListener {
                        sendTestNotifications()
                        true
                    }
                }

                ivRemind.clickWithDuration {
                    startActivity<AlarmManageScreen>()
                }
            } else {
                ivRemind.gone()
            }

            setupBottomNavBar()
            setupViewPager()
            viewPagerHome.currentItem = currentTabIndex
            hostCurrentTab.value = currentTabIndex
            updateUIForTabPosition(currentTabIndex)

            llWeather.gone()

            // 延迟处理通知点击参数，确保UI完全初始化
            root.postDelayed({
                handleNotificationAction(intent)
            }, 500)
        }
        
        // Banner 和权限流程
        lifecycleScope.launch {
            awaitResumedIfNeeded()
            val homeFragment = homeFrgReady.await()
            homeFragment.onNotificationPermissionFlowFinished()
            if (currentTabIndex == 0) {
                homeFragment.highLightComplete.await()
                // 高亮引导完成后立即检查并显示评分弹窗
                checkAndShowRateDialogAfterOnboarding()
            }
            awaitResumedIfNeeded()
            loadBanner(mViewBind.adViewContainer, AdPosition.BA_HOME_BOTTOM, onClose = {
                if (!bannerShowComplete.isCompleted) {
                    bannerShowComplete.complete(true)
                }
            }) {
                if (!bannerShowComplete.isCompleted) {
                    bannerShowComplete.complete(it)
                }
            }
            bannerShowComplete.await()
        }
        
        lifecycleScope.launch {
            val homeFragment = homeFrgReady.await()
            homeFragment.homeGuideOverlayUi.collect { overlayUi ->
                hostHomeGuideOverlayUi.value = overlayUi
                window.statusBarColor = if (overlayUi != null && currentTabIndex == 0) {
                    android.graphics.Color.parseColor("#99000000")
                } else {
                    android.graphics.Color.TRANSPARENT
                }
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
                            
                            llWeather.gone()
                            tvTitle.visible()
                        }
                    }
                }
            }
        }
    }

    private suspend fun awaitResumedIfNeeded() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            return
        }
        awaitNextResume()
    }

    private suspend fun checkNotificationPermissionFlow(): Boolean {
        var goSetting = false
        val result = suspendCancellableCoroutine<Boolean> { cont ->
            permissionManager.checkNotificationPermission(
                activity = this@MainAct,
                onGoSetting = {
                    goSetting = true
                }
            ) {
                if (cont.isActive) {
                    cont.resume(it)
                }
            }
        }
        if (goSetting) {
            awaitNextResume()
            return permissionManager.isNotificationPermissionGranted()
        }
        return result
    }

    private suspend fun awaitNextResume() {
        suspendCancellableCoroutine<Unit> { cont ->
            lateinit var observer: LifecycleEventObserver
            val needPauseFirst = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            var hasPaused = false
            observer = LifecycleEventObserver { _, event ->
                if (needPauseFirst && !hasPaused) {
                    if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                        hasPaused = true
                    }
                    return@LifecycleEventObserver
                }
                if (event == Lifecycle.Event.ON_RESUME) {
                    lifecycle.removeObserver(observer)
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }
            }
            lifecycle.addObserver(observer)
            cont.invokeOnCancellation {
                lifecycle.removeObserver(observer)
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
                Pair(R.drawable.fc_selector_nav_home, R.string.fc_home),
                Pair(R.drawable.fc_selector_nav_meds, R.string.fc_meds),
                Pair(R.drawable.fc_selector_nav_insights, R.string.fc_insights),
                Pair(R.drawable.fc_selector_nav_record, R.string.fc_record),
                Pair(R.drawable.fc_selector_nav_settings, R.string.fc_settings),

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
            customView = FcLayoutHomeTabItemBinding.inflate(layoutInflater, tabLayout, false).let {
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
            offscreenPageLimit = homeFrgAdapter.count
            adapter = homeFrgAdapter
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
        val action =
            intent?.getStringExtra(HealthServiceConstants.EXTRA_NOTIFICATION_ACTION) ?: return

        when (action) {
            HealthServiceConstants.ACTION_VALUE_BLOOD_SUGAR -> {
                HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BLOOD_SUGAR
                )
            }

            HealthServiceConstants.ACTION_VALUE_BLOOD_PRESSURE -> {
                HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BLOOD_PRESSURE
                )
            }

            HealthServiceConstants.ACTION_VALUE_HEART_RATE -> {
                HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.HEART_RATE
                )
            }

            HealthServiceConstants.ACTION_VALUE_HYDRATION -> {
                startActivity<HydrateAct>()
            }

            HealthServiceConstants.ACTION_VALUE_STEPS -> {
                checkStepPermissionAndNavigate()
            }
            // 自定义通知的新增动作处理分支
            HealthServiceConstants.ACTION_VALUE_HOMEPAGE -> {
                // 已在主页，无需跳转
            }

            HealthServiceConstants.ACTION_VALUE_CHOLESTEROL -> {
                HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.CHOLESTEROL
                )
            }

            HealthServiceConstants.ACTION_VALUE_BMI -> {
                HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BMI
                )
            }

            HealthServiceConstants.ACTION_VALUE_HISTORY -> {
                startActivity<HistoryRecordAct>()
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
    private fun sendTestNotifications() {
        if (!NotificationFeatureSwitch.notificationsEnabled) {
            return
        }
        lifecycleScope.launch {
            val messages = PushMessage.createDefaultList()
            Toast.makeText(
                this@MainAct,
                "Sending ${messages.size} test notifications...",
                Toast.LENGTH_SHORT
            ).show()


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
                this@MainAct,
                "✅ All ${messages.size} test notifications sent!",
                Toast.LENGTH_SHORT
            ).show()

            "All test notifications sent successfully".logd(TAG)
        }
    }

    override fun handleBackPress(): Boolean {
        ExitDialog.show(supportFragmentManager) {
            App.INSTANCE.litelitetoolhub()
        }
        return true
    }

    override fun permission() = PermissionRequest(Manifest.permission.ACTIVITY_RECOGNITION)

    fun checkStepPermissionAndNavigate() {
        permissionRequest?.let {
            it.launch { isSuccess, showSettingsRedirect, hasPermission ->
                if (isSuccess || hasPermission) {
                    trackEnterPageClick(HealthType.WALKING_STEPS)
                    startActivity<StepCountAct>()
                } else if (showSettingsRedirect) {
                    ActivityPerRequestDialog.show(supportFragmentManager) {
                        it.goSetting(this)
                        App.INSTANCE.isGoSetting = true
                    }
                }
            }
        } ?: {
            trackEnterPageClick(HealthType.WALKING_STEPS)
            startActivity<StepCountAct>()
        }
    }

    override fun getStatusBarColor() = com.healthtracker.framework.R.color.transparent


}
