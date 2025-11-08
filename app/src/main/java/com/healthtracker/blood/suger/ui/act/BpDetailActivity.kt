package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityBpDetailBinding
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.util.AxisStyle
import com.healthtracker.blood.suger.util.ChartConfigHelper
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.startActivity
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import java.text.DecimalFormat

@AndroidEntryPoint
class BpDetailActivity: BaseInterActivity<BpDetailViewModel, ActivityBpDetailBinding>() {

    private val chartModelProducer = CartesianChartModelProducer()
    private var chartLabels: List<String> = emptyList()

    companion object{
        private const val RECORD_ID = "record_id"
        // 启动详情页面
        fun start(context: Context, recordId: Long) {
            context.startActivity<BpDetailActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = ActivityBpDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BpDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupChart()
        with(mViewBind){
            btnBack.clickWithDuration {
                onBackPress()
            }
            btnDelete.clickWithDuration {
                showDeleteConfirm()
            }

            btnEdit.clickWithDuration {
                mViewModel.bloodPressureRecord.value?.let {
                    BpRecordActivity.start(this@BpDetailActivity,it.id)
                }
            }

            // 设置专家建议控件监听器
            expertAdviceView.setOnExpertAdviceListener(object : ExpertAdviceView.OnExpertAdviceListener {
                override fun onCountdownFinished() {
                    // TODO: 倒计时结束，显示广告或解锁内容
                    showReword()
                }

                override fun onGetTipClicked() {
                    // TODO: 点击获取提示，显示广告
                    showReword()

                }

                override fun onCancelClicked() {
                    // 用户取消倒计时，不需要额外处理
                }
            })
            loadNative(adContainer, style = NativeAdStyle.CARD_5)
        }

        // 观察数据变化
        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            // 观察血压记录数据变化
            mViewModel.bloodPressureRecord.collect { record ->
                record?.let {
                    updateUI(it)
                }
            }
        }

        lifecycleScope.launch {
            // 观察错误状态
            mViewModel.error.collect { error ->
                error?.let {
                    // TODO: 显示错误信息
                }
            }
        }

        lifecycleScope.launch {
            // 观察加载状态
            mViewModel.isLoading.collect { isLoading ->
                // TODO: 显示/隐藏加载状态
            }
        }

        lifecycleScope.launch {
            mViewModel.chartUiState.collectLatest { state ->
                updateChart(state)
            }
        }
    }

    private fun updateUI(record: BloodPressureRecord) {
        with(mViewBind) {
            // 使用通用等级视图：设置等级列表与当前索引
            val levels = LeveDataFactory.BloodPressure.buildItems(this@BpDetailActivity)
            bpStatusView.setLevels(levels)
            val idx = LeveDataFactory.BloodPressure.indexFor(record.systolicPressure, record.diastolicPressure)
            bpStatusView.setCurrentLevel(idx)
            tvSystolicValue.text = record.systolicPressure.toString()
            tvDiastolicValue.text = record.diastolicPressure.toString()
            tvPulseValue.text = record.pulseRate.toString()
            tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)
            val rangeDes = resources.getStringArray(R.array.bp_level_expert_advice)
            // 设置专家建议文案
            val adviceText = String.format(rangeDes[idx], record.systolicPressure, record.diastolicPressure)
            expertAdviceView.setAdviceText(adviceText)
        }
    }

    override fun getStatusBarColor() = R.color.c5

    private fun showDeleteConfirm() {
        showDeleteConfirm {
            mViewModel.deleteRecord()
        }
    }




    override fun hideMask() {
        mViewBind.expertAdviceView.setMaskVisible(false)
    }

    private fun setupChart() {
        val axisStyle = AxisStyle(
            bottomAxisValueFormatter = createBottomAxisFormatter(),
            deduplicateBottomLabels = true,
            minY = 0.0,
            maxY = 200.0,
            startAxisValueFormatter = CartesianValueFormatter.decimal(DecimalFormat("#"))
        )
        mViewBind.chartView.apply {
            chart = ChartConfigHelper.createDualLineChart(axisStyle = axisStyle)
            modelProducer = chartModelProducer
        }
    }

    private suspend fun updateChart(state: BpDetailViewModel.BpChartUiState) {
        chartLabels = state.labels
        val axisStyle = AxisStyle(
            bottomAxisValueFormatter = createBottomAxisFormatter(),
            deduplicateBottomLabels = true,
            minY = state.minY,
            maxY = state.maxY,
            startAxisValueFormatter = CartesianValueFormatter.decimal(DecimalFormat("#"))
        )
        mViewBind.chartView.chart = ChartConfigHelper.createDualLineChart(axisStyle = axisStyle)
        if (!state.hasData) return
        chartModelProducer.runTransaction {
            lineSeries {
                series(x = state.xValues, y = state.systolicValues)
                series(x = state.xValues, y = state.diastolicValues)
            }
        }
    }

    private fun createBottomAxisFormatter(): CartesianValueFormatter {
        return object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?
            ): CharSequence {
                return chartLabels.getOrNull(value.toInt()) ?: ""
            }
        }
    }

}
