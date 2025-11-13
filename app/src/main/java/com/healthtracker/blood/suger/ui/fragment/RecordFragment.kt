package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.databinding.FragmentRecordBinding
import com.healthtracker.blood.suger.ui.act.HistoryRecordActivity
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.blood.suger.ui.viewmodel.TrackerViewModel
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.invisible
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecordFragment: BaseMVVMFragment<TrackerViewModel, FragmentRecordBinding>() {
    
    @Inject
    lateinit var chartManagerFactory: HealthLineChartManager.Factory
    private lateinit var chartManager: HealthLineChartManager

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentRecordBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = TrackerViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 初始化图表管理器
        chartManager = chartManagerFactory.create(mViewBind!!.bsChartView, viewLifecycleOwner)

        // 设置按钮点击事件
        mViewBind?.run {
            llBpHistory.clickWithDuration {
                HistoryRecordActivity.start(requireActivity(),HistoryRecordItem.RecordType.BLOOD_PRESSURE)
            }
        }

        // 启动数据观察
        mViewModel.startObservingData()

        // 观察图表数据
        observeChartData()
    }
    
    private fun observeChartData() {
        // 观察图表状态变化
        collectLatest(mViewModel.chartUiState) { state ->
            lifecycleScope.launch {
                // 渲染图表，隐藏坐标轴标签
                val hasData = chartManager.render(state, isShowLabel = false)
                updateChartVisibility(hasData)
            }
        }
    }
    
    /**
     * 更新图表和空视图的可见性
     */
    private fun updateChartVisibility(hasData: Boolean) {
        mViewBind?.run {
            if (hasData) {
                // 有数据时显示图表，隐藏空视图
                bsChartView.visible()
                gpBsEmpty.invisible()
            } else {
                // 无数据时隐藏图表，显示空视图
                bsChartView.gone()
                gpBsEmpty.visible()
            }
        }
    }
}