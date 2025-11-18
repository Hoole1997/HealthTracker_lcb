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
import com.healthtracker.blood.suger.ui.act.HistoryRecordActivity
import com.healthtracker.blood.suger.ui.act.MainActivity
import com.healthtracker.blood.suger.ui.act.StepCountActivity
import com.healthtracker.blood.suger.ui.act.StepSettingActivity
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.blood.suger.ui.viewmodel.TrackerViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.invisible
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecordFragment: BaseMVVMFragment<TrackerViewModel, FragmentRecordBinding>() {

    @Inject
    lateinit var chartManagerFactory: HealthLineChartManager.Factory

    // ========== 图表管理器 ==========
    private lateinit var bsChartManager: HealthLineChartManager
    private lateinit var bpChartManager: HealthLineChartManager
    private lateinit var hrChartManager: HealthLineChartManager
    private lateinit var choChartManager: HealthLineChartManager
    private lateinit var bmiChartManager: HealthLineChartManager
    private lateinit var stepChartManager: HealthLineChartManager

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
        }
    }

    /**
     * ✨ 修改：观察所有5个图表的数据
     */
    private fun observeAllChartData() {
        // Blood Sugar
        collectLatest(mViewModel.bloodSugarChartState) { state ->
            lifecycleScope.launch {
                val hasData = bsChartManager.render(state, isShowLabel = false)
                updateChartVisibility(hasData, mViewBind?.includeBs)
            }
        }

        // Blood Pressure
        collectLatest(mViewModel.bloodPressureChartState) { state ->
            lifecycleScope.launch {
                val hasData = bpChartManager.render(state, isShowLabel = false)
                updateChartVisibility(hasData, mViewBind?.includeBp)
            }
        }

        // Heart Rate
        collectLatest(mViewModel.heartRateChartState) { state ->
            lifecycleScope.launch {
                val hasData = hrChartManager.render(state, isShowLabel = false)
                updateChartVisibility(hasData, mViewBind?.includeHr)
            }
        }

        // Cholesterol
        collectLatest(mViewModel.cholesterolChartState) { state ->
            lifecycleScope.launch {
                val hasData = choChartManager.render(state, isShowLabel = false)
                updateChartVisibility(hasData, mViewBind?.includeCho)
            }
        }

        // BMI
        collectLatest(mViewModel.bmiChartState) { state ->
            lifecycleScope.launch {
                val hasData = bmiChartManager.render(state, isShowLabel = false)
                updateChartVisibility(hasData, mViewBind?.includeBmi)
            }
        }

        // Steps (column chart)
        collectLatest(mViewModel.stepChartState) { state ->
            lifecycleScope.launch {
                val hasPermission = hasActivityRecognitionPermission()
                if (hasPermission) {
                    val hasData = stepChartManager.renderColumn(state, isShowLabel = false)
                    updateChartVisibility(hasData, mViewBind?.includeStep)
                } else {
                    updateChartVisibility(false, mViewBind?.includeStep)
                }
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
