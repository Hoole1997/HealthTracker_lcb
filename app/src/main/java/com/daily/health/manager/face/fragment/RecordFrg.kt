package com.daily.health.manager.face.fragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.daily.health.manager.R
import com.daily.health.manager.databinding.TrFragmentRecordBinding
import com.daily.health.manager.databinding.TrItemHealthChartCardBinding
import com.daily.health.manager.tips.HealthMetric
import com.daily.health.manager.face.act.HealthRecordAct
import com.daily.health.manager.face.act.HealthStatisticsAct
import com.daily.health.manager.face.act.HydrateAct
import com.daily.health.manager.face.act.MainAct
import com.daily.health.manager.face.chart.HealthLineChartManager
import com.daily.health.manager.face.viewmodel.TrackerViewModel
import com.daily.health.manager.utils.loadNative
import com.daily.health.manager.utils.showInter
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.invisible
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.visible
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject

class RecordFrg: BaseMVVMFragment<TrackerViewModel, TrFragmentRecordBinding>() {

    companion object{
        private const val TAG = "RecordFragment"
    }

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()

    // ========== 广告加载标志 ==========
    private var isAdLoaded = false

    // ========== 图表管理器（使用可空类型，每次 View 创建时重新初始化） ==========
    private var bsChartManager: HealthLineChartManager? = null
    private var bpChartManager: HealthLineChartManager? = null
    private var hrChartManager: HealthLineChartManager? = null
    private var choChartManager: HealthLineChartManager? = null
    private var bmiChartManager: HealthLineChartManager? = null
    private var stepChartManager: HealthLineChartManager? = null
    private var hydrateChartManager: HealthLineChartManager? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = TrFragmentRecordBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = TrackerViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupHealthCards()
        setupChartManagers()
        setupClickListeners()

        mViewModel.startObservingData()
        observeAllChartData()
        setupAdLazyLoading()
    }

    override fun onResume() {
        super.onResume()
        checkAdVisibility("onResume")
    }

    /**
     * 设置广告懒加载
     */
    private fun setupAdLazyLoading() {
        val scrollView = mViewBind?.root?.findViewById(
            mViewBind?.root?.getChildAt(0)?.id ?: return
        ) ?: (mViewBind?.root?.getChildAt(0) as? androidx.core.widget.NestedScrollView) ?: return
        val adContainer = mViewBind?.adContainer ?: return

        // 初始布局完成后检测（处理不需要滚动就能看到的情况）
        adContainer.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                checkAdVisibility("GlobalLayout")
                if (isAdLoaded) {
                    adContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        // 监听滚动事件
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            checkAdVisibility("Scroll")
        }
    }

    /**
     * 检测广告可见性并触发加载
     */
    private fun checkAdVisibility(source: String) {
        val adContainer = mViewBind?.adContainer ?: return
        val scrollView = mViewBind?.root?.findViewById(
            mViewBind?.root?.getChildAt(0)?.id ?: return
        ) ?: (mViewBind?.root?.getChildAt(0) as? androidx.core.widget.NestedScrollView) ?: return

        val isVisible = isAdContainerVisible(adContainer, scrollView)
        if (BuildState.debug) "[$source] isAdLoaded=$isAdLoaded, isResumed=$isResumed, isVisible=$isVisible".logd(TAG)

        // 只有在 Fragment 已经 resume (在 ViewPager 中可见) 且视图进入视口时才触发
        if (!isAdLoaded && isResumed && isVisible) {
            isAdLoaded = true
            if (BuildState.debug) "✅ Triggering ad load from: $source".logd(TAG)
            activity?.loadNative(adContainer, AdPosition.NA_MAIN_TRACKER_MIDDLE, style = NativeAdStyle.CARD_8)
        }
    }

    /**
     * 检测 adContainer 是否部分进入可视区域
     */
    private fun isAdContainerVisible(
        adContainer: android.view.View,
        scrollView: androidx.core.widget.NestedScrollView
    ): Boolean {
        if (!adContainer.isShown) return false

        val scrollBounds = android.graphics.Rect()
        scrollView.getHitRect(scrollBounds)

        val adLocation = IntArray(2)
        adContainer.getLocationOnScreen(adLocation)

        val scrollLocation = IntArray(2)
        scrollView.getLocationOnScreen(scrollLocation)

        val adTop = adLocation[1] - scrollLocation[1]
        val adBottom = adTop + adContainer.height
        val scrollHeight = scrollView.height

        // 部分可见即触发：adContainer 的顶部在 scrollView 可视区域内，或底部在可视区域内
        return adTop < scrollHeight && adBottom > 0
    }

    /**
     * ✨ 新增：设置各健康指标卡片的图标和标题
     */
    private fun setupHealthCards() {
        mViewBind?.run {
            // Blood Sugar
            includeBs.ivIcon.setImageResource(R.mipmap.tr_home_card_bs)
            includeBs.tvTitle.text = getString(R.string.tr_blood_suger)

            // Blood Pressure
            includeBp.ivIcon.setImageResource(R.mipmap.tr_home_card_bp)
            includeBp.tvTitle.text = getString(R.string.tr_blood_pressure)

            // Heart Rate
            includeHr.ivIcon.setImageResource(R.mipmap.tr_home_hero_heart)
            includeHr.tvTitle.text = getString(R.string.tr_heart_rate)

            // Cholesterol
            includeCho.ivIcon.setImageResource(R.mipmap.tr_home_card_cholesterol)
            includeCho.tvTitle.text = getString(R.string.tr_cholesterol)

            // BMI (组合 Weight + BMI)
            includeBmi.ivIcon.setImageResource(R.mipmap.tr_home_card_weight)
            "${getString(R.string.tr_weight)} & ${getString(R.string.tr_bmi)}".also { includeBmi.tvTitle.text = it }

            // Steps
            includeStep.ivIcon.setImageResource(R.mipmap.tr_home_card_step)
            includeStep.tvTitle.text = getString(R.string.tr_step_count)
            includeStep.btnAdd.apply {
                text = getString(R.string.tr_settings)
            }

            // Hydrate
            includeHydrate.ivIcon.setImageResource(R.mipmap.tr_home_card_water)
            includeHydrate.tvTitle.text = getString(R.string.tr_hydrate)
            includeHydrate.btnAdd.apply {
                text = getString(R.string.tr_settings)
            }
        }
    }

    /**
     * ✨ 修改：初始化所有图表管理器
     */
    private fun setupChartManagers() {
        mViewBind?.run {
            bsChartManager = chartManagerFactory.create(includeBs.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
            bpChartManager = chartManagerFactory.create(includeBp.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
            hrChartManager = chartManagerFactory.create(includeHr.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
            choChartManager = chartManagerFactory.create(includeCho.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
            bmiChartManager = chartManagerFactory.create(includeBmi.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
            stepChartManager = chartManagerFactory.create(includeStep.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
            hydrateChartManager = chartManagerFactory.create(includeHydrate.chartView.apply {
                isEnabled = false
            }, viewLifecycleOwner)
        }
    }

    /**
     * ✨ 修改：设置所有卡片的点击事件
     */
    private fun setupClickListeners() {
        mViewBind?.run {
            // Blood Sugar - 点击卡片跳转历史记录
            includeBs.root.clickWithDuration {
                if(includeBs.chartView.isVisible){
                    requireActivity().showInter(AdPosition.IV_BLOOD_SUGAR_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.BLOOD_SUGAR)
                    }
                }else{
                    HealthRecordAct.start(
                        requireActivity(),
                        HealthRecordAct.RecordType.BLOOD_SUGAR
                    )
                }
            }

            // Blood Pressure
            includeBp.root.clickWithDuration {
                if(includeBp.chartView.isVisible){
                    requireActivity().showInter(AdPosition.IV_BLOOD_PRESSURE_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.BLOOD_PRESSURE)
                    }
                }else{
                    HealthRecordAct.start(
                        requireActivity(),
                        HealthRecordAct.RecordType.BLOOD_PRESSURE
                    )
                }
            }

            // Heart Rate
            includeHr.root.clickWithDuration {
                if(includeHr.chartView.isVisible){
                    requireActivity().showInter(AdPosition.IV_HEART_RATE_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.HEART_RATE)
                    }
                }else{
                    HealthRecordAct.start(
                        requireActivity(),
                        HealthRecordAct.RecordType.HEART_RATE
                    )
                }
            }

            // Cholesterol
            includeCho.root.clickWithDuration {
                if(includeCho.chartView.isVisible){
                    requireActivity().showInter(AdPosition.IV_CHOLESTEROL_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.CHOLESTEROL)
                    }
                }else{
                    HealthRecordAct.start(
                        requireActivity(),
                        HealthRecordAct.RecordType.CHOLESTEROL
                    )
                }
            }

            // BMI
            includeBmi.root.clickWithDuration {
                if(includeBmi.chartView.isVisible){
                    requireActivity().showInter(AdPosition.IV_BMI_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.BMI)
                    }
                }else{
                    HealthRecordAct.start(
                        requireActivity(),
                        HealthRecordAct.RecordType.BMI
                    )
                }
            }

            includeStep.root.clickWithDuration {
                if (includeStep.chartView.isVisible) {
                    requireActivity().showInter(AdPosition.IV_WALK_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.STEPS)
                    }
                } else {
                    if(requireActivity() is MainAct){
                        (requireActivity() as MainAct).checkStepPermissionAndNavigate()
                    }
                }
            }

            includeHydrate.root.clickWithDuration {
                if (includeHydrate.chartView.isVisible) {
                    requireActivity().showInter(AdPosition.IV_WATER_TRACK_ENTER) {
                        HealthStatisticsAct.start(requireActivity(), HealthMetric.HYDRATION)
                    }
                } else {
                    HydrateAct.start(requireContext())
                }
            }
        }
    }

    /**
     * ✨ 修改：观察所有5个图表的数据
     */
    private fun observeAllChartData() {
        // Blood Sugar
        collectLatest(mViewModel.bloodSugarChartState) { state ->
            lifecycleScope.launch {
                val hasData = bsChartManager?.render(state, isShowLabel = false) ?: false
                updateChartVisibility(hasData, mViewBind?.includeBs)
            }
        }

        // Blood Pressure
        collectLatest(mViewModel.bloodPressureChartState) { state ->
            lifecycleScope.launch {
                val hasData = bpChartManager?.render(state, isShowLabel = false) ?: false
                updateChartVisibility(hasData, mViewBind?.includeBp)
            }
        }

        // Heart Rate
        collectLatest(mViewModel.heartRateChartState) { state ->
            lifecycleScope.launch {
                val hasData = hrChartManager?.render(state, isShowLabel = false) ?: false
                updateChartVisibility(hasData, mViewBind?.includeHr)
            }
        }

        // Cholesterol
        collectLatest(mViewModel.cholesterolChartState) { state ->
            lifecycleScope.launch {
                val hasData = choChartManager?.render(state, isShowLabel = false) ?: false
                updateChartVisibility(hasData, mViewBind?.includeCho)
            }
        }

        // BMI
        collectLatest(mViewModel.bmiChartState) { state ->
            lifecycleScope.launch {
                val hasData = bmiChartManager?.render(state, isShowLabel = false) ?: false
                updateChartVisibility(hasData, mViewBind?.includeBmi)
            }
        }

        // Steps (column chart)
        // 修复：先设置可见，等待布局完成后再渲染，确保 chartView.width 正确
        collectLatest(mViewModel.stepChartState) { state ->
            lifecycleScope.launch {
                val hasPermission = hasActivityRecognitionPermission()
                val cardBinding = mViewBind?.includeStep
                if (hasPermission && state.hasData) {
                    // 先设置可见，再等待布局完成后渲染
                    cardBinding?.chartView?.visible()
                    cardBinding?.gpEmpty?.invisible()
                    cardBinding?.chartView?.doOnLayout {
                        // doOnLayout 回调非 suspend，需要启动新协程
                        lifecycleScope.launch {
                            stepChartManager?.renderColumn(
                                state,
                                isShowLabel = false,
                                showBaseline = false,
                                columnWidthScale = 0.5f,
                                layerPaddingDp = 0f
                            )
                        }
                    }
                } else {
                    updateChartVisibility(false, cardBinding)
                }
            }
        }

        // Hydrate (column chart)
        // 修复：先设置可见，等待布局完成后再渲染，确保 chartView.width 正确
        collectLatest(mViewModel.hydrateChartState) { state ->
            lifecycleScope.launch {
                val cardBinding = mViewBind?.includeHydrate
                if (state.hasData) {
                    // 先设置可见，再等待布局完成后渲染
                    cardBinding?.chartView?.visible()
                    cardBinding?.gpEmpty?.invisible()
                    cardBinding?.chartView?.doOnLayout {
                        // doOnLayout 回调非 suspend，需要启动新协程
                        lifecycleScope.launch {
                            hydrateChartManager?.renderColumn(
                                state,
                                isShowLabel = false,
                                showBaseline = false,
                                columnWidthScale = 0.5f,
                                layerPaddingDp = 0f
                            )
                        }
                    }
                } else {
                    updateChartVisibility(false, cardBinding)
                }
            }
        }
    }

    /**
     * ✨ 简化：更新图表和空状态的可见性
     */
    private fun updateChartVisibility(hasData: Boolean, cardBinding: TrItemHealthChartCardBinding?) {
        cardBinding?.run {
            if (hasData) {
                chartView.visible()
                gpEmpty.invisible()
            } else {
                chartView.gone()
                gpEmpty.visible()
            }
        }
    }

    private fun hasActivityRecognitionPermission(): Boolean {
        val context = requireContext()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
