package com.healthtracker.blood.suger.ui.fragment

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.FragmentRecordBinding
import com.healthtracker.blood.suger.databinding.ItemHealthChartCardBinding
import com.healthtracker.blood.suger.tips.HealthMetric
import com.healthtracker.blood.suger.ui.act.BmiRecordActivity
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.act.BsRecordActivity
import com.healthtracker.blood.suger.ui.act.CholesterolRecordActivity
import com.healthtracker.blood.suger.ui.act.HealthStatisticsActivity
import com.healthtracker.blood.suger.ui.act.HeartRateRecordActivity
import com.healthtracker.blood.suger.ui.act.HydrateActivity
import com.healthtracker.blood.suger.ui.act.HydrateSettingActivity
import com.healthtracker.blood.suger.ui.act.MainActivity
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.viewmodel.TrackerViewModel
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.invisible
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import javax.inject.Inject

@AndroidEntryPoint
class RecordFragment: BaseMVVMFragment<TrackerViewModel, FragmentRecordBinding>() {

    companion object{
        private const val TAG = "RecordFragment"
    }

    @Inject
    lateinit var chartManagerFactory: HealthLineChartManager.Factory

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
    ) = FragmentRecordBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = TrackerViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupHealthCards()
        setupChartManagers()
        setupClickListeners()

        mViewModel.startObservingData()
        observeAllChartData()
        setupAdLazyLoading()
    }

    /**
     * 设置广告懒加载：当 adContainer 可见时才触发加载
     */
    private fun setupAdLazyLoading() {
        val scrollView = mViewBind?.root?.findViewById(
            mViewBind?.root?.getChildAt(0)?.id ?: return
        ) ?: (mViewBind?.root?.getChildAt(0) as? androidx.core.widget.NestedScrollView) ?: return
        val adContainer = mViewBind?.adContainer ?: return

        val visibilityChecker = { source: String ->
            val isVisible = isAdContainerVisible(adContainer, scrollView)
            if(BuildState.debug) "[$source] isAdLoaded=$isAdLoaded, isVisible=$isVisible".logd(TAG)
            if (!isAdLoaded && isVisible) {
                isAdLoaded = true
                if(BuildState.debug) "✅ Triggering ad load from: $source".logd(TAG)
                activity?.loadNative(adContainer, style = NativeAdStyle.CARD_8)
            }
        }

        // 初始布局完成后检测（处理不需要滚动就能看到的情况）
        adContainer.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                visibilityChecker("GlobalLayout")
                if (isAdLoaded) {
                    adContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        // 监听滚动事件
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            visibilityChecker("Scroll")
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
            includeBs.ivIcon.setImageResource(R.mipmap.ic_blood_suger)
            includeBs.tvTitle.text = getString(R.string.blood_suger)

            // Blood Pressure
            includeBp.ivIcon.setImageResource(R.mipmap.ic_blood_pressure)
            includeBp.tvTitle.text = getString(R.string.blood_pressure)

            // Heart Rate
            includeHr.ivIcon.setImageResource(R.mipmap.ic_heart)
            includeHr.tvTitle.text = getString(R.string.heart_rate)

            // Cholesterol
            includeCho.ivIcon.setImageResource(R.mipmap.ic_cholesterol)
            includeCho.tvTitle.text = getString(R.string.cholesterol)

            // BMI (组合 Weight + BMI)
            includeBmi.ivIcon.setImageResource(R.mipmap.ic_bmi)
            "${getString(R.string.weight)} & ${getString(R.string.bmi)}".also { includeBmi.tvTitle.text = it }

            // Steps
            includeStep.ivIcon.setImageResource(R.mipmap.ic_home_step)
            includeStep.tvTitle.text = getString(R.string.step_count)
            includeStep.btnAdd.apply {
                text = getString(R.string.hydrate_setting)
            }

            // Hydrate
            includeHydrate.ivIcon.setImageResource(R.mipmap.ic_home_cup)
            includeHydrate.tvTitle.text = getString(R.string.hydrate)
            includeHydrate.btnAdd.apply {
                text = getString(R.string.hydrate_setting)
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
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.BLOOD_SUGAR)
                }else{
                    BsRecordActivity.start(requireActivity())
                }
            }

            // Blood Pressure
            includeBp.root.clickWithDuration {
                if(includeBp.chartView.isVisible){
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.BLOOD_PRESSURE)
                }else{
                    BpRecordActivity.start(requireActivity())
                }
            }

            // Heart Rate
            includeHr.root.clickWithDuration {
                if(includeHr.chartView.isVisible){
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.HEART_RATE)
                }else{
                    HeartRateRecordActivity.start(requireActivity())
                }
            }

            // Cholesterol
            includeCho.root.clickWithDuration {
                if(includeCho.chartView.isVisible){
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.CHOLESTEROL)
                }else{
                    CholesterolRecordActivity.start(requireActivity())
                }
            }

            // BMI
            includeBmi.root.clickWithDuration {
                if(includeBmi.chartView.isVisible){
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.BMI)
                }else{
                    BmiRecordActivity.start(requireActivity())
                }
            }

            includeStep.root.clickWithDuration {
                if (includeStep.chartView.isVisible) {
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.STEPS)
                } else {
                    if(requireActivity() is MainActivity){
                        (requireActivity() as MainActivity).checkStepPermissionAndNavigate()
                    }
                }
            }

            includeHydrate.root.clickWithDuration {

                if (includeHydrate.chartView.isVisible) {
                    HealthStatisticsActivity.start(requireActivity(), HealthMetric.HYDRATION)
                } else {
                    HydrateActivity.start(requireContext())
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
        collectLatest(mViewModel.stepChartState) { state ->
            lifecycleScope.launch {
                val hasPermission = hasActivityRecognitionPermission()
                if (hasPermission) {
                    val hasData = stepChartManager?.renderColumn(
                        state,
                        isShowLabel = false,
                        showBaseline = false,
                        columnWidthScale = 0.5f
                    ) ?: false
                    updateChartVisibility(hasData, mViewBind?.includeStep)
                } else {
                    updateChartVisibility(false, mViewBind?.includeStep)
                }
            }
        }

        // Hydrate (column chart)
        collectLatest(mViewModel.hydrateChartState) { state ->
            lifecycleScope.launch {
                val hasData = hydrateChartManager?.renderColumn(
                    state,
                    isShowLabel = false,
                    showBaseline = false,
                    columnWidthScale = 0.5f
                ) ?: false
                updateChartVisibility(hasData, mViewBind?.includeHydrate)
            }
        }
    }

    /**
     * ✨ 简化：更新图表和空状态的可见性
     */
    private fun updateChartVisibility(hasData: Boolean, cardBinding: ItemHealthChartCardBinding?) {
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
