package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.getStatusStringRes
import com.healthtracker.blood.suger.databinding.ActivityHealthStatisticsBinding
import com.healthtracker.blood.suger.tips.HealthMetric
import com.healthtracker.blood.suger.tips.HealthTips
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
import com.healthtracker.blood.suger.ui.history.BloodPressureHistoryItem
import com.healthtracker.blood.suger.ui.history.BloodSugarHistoryItem
import com.healthtracker.blood.suger.ui.history.BmiHistoryItem
import com.healthtracker.blood.suger.ui.history.CholesterolHistoryItem
import com.healthtracker.blood.suger.ui.history.HeartRateHistoryItem
import com.healthtracker.blood.suger.ui.history.HistoryAdapter
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.blood.suger.ui.widget.StatisticDimensionMenu
import com.healthtracker.blood.suger.viewmodel.HealthStatisticsViewModel
import com.healthtracker.blood.suger.viewmodel.StatisticDimension
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.getRobotoMedium
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

/**
 * Health Statistics Activity
 */
@AndroidEntryPoint
class HealthStatisticsActivity :
    BaseInterActivity<HealthStatisticsViewModel, ActivityHealthStatisticsBinding>() {

    companion object {
        // Intent extra keys - must match ViewModel's SavedStateHandle keys
        private const val EXTRA_METRIC_TYPE = "metric_type"
        private const val EXTRA_DATE_RANGE_PRESET = "date_range_preset"

        /**
         * Launch Health Statistics Activity
         *
         * @param context Android context
         * @param metricType Initial health metric to display (null = default to BLOOD_SUGAR)
         * @param dateRangePreset Initial date range preset (null = default to DAYS_7)
         *
         * Usage examples:
         * ```
         * // Default launch (Blood Sugar, 7 days)
         * HealthStatisticsActivity.start(context)
         *
         * // Show specific metric
         * HealthStatisticsActivity.start(context, HealthMetric.BLOOD_PRESSURE)
         *
         * // Show specific metric with custom date range
         * HealthStatisticsActivity.start(
         *     context,
         *     HealthMetric.BMI,
         *     DateRangePreset.MONTH_3
         * )
         * ```
         */
        fun start(
            context: Context,
            metricType: HealthMetric? = null,
            dateRangePreset: HealthStatisticsViewModel.DateRangePreset? = null
        ) {
            val extras = mutableListOf<Pair<String, Any?>>()

            metricType?.let {
                extras.add(EXTRA_METRIC_TYPE to it.ordinal)
            }

            dateRangePreset?.let {
                extras.add(EXTRA_DATE_RANGE_PRESET to it.ordinal)
            }

            context.startActivity<HealthStatisticsActivity>(*extras.toTypedArray())
        }
    }

    @Inject
    lateinit var chartManagerFactory: HealthLineChartManager.Factory

    private lateinit var chartManager: HealthLineChartManager
    private val historyAdapter = HistoryAdapter().apply { showDeleteButton = false }
    private var latestDateRange: HealthStatisticsViewModel.DateRange? = null
    private var isHistorySectionVisible: Boolean = false
    private var isHistoryListVisible: Boolean = false
    private var dimensionMenu: StatisticDimensionMenu? = null
    private var currentMetricType: HealthMetric = HealthMetric.BLOOD_SUGAR

    override fun createViewBinding(): ActivityHealthStatisticsBinding {
        return ActivityHealthStatisticsBinding.inflate(layoutInflater)
    }

    override fun getVMModelClass(): Class<HealthStatisticsViewModel> {
        return HealthStatisticsViewModel::class.java
    }

    override fun onResume() {
        super.onResume()
        mViewModel.refreshPreferredUnit()
    }

    override fun initView(savedInstanceState: Bundle?) {
        chartManager = chartManagerFactory.create(mViewBind.chartView, this)
        setupToolbar()
        setupRangeButtons()
        setupStatusFilter()
        setupHistoryList()
        setupActions()
    }

    private fun setupToolbar() {
        mViewBind.btnBack.click { handleBackPress() }
    }

    private fun setupRangeButtons() {
        with(mViewBind) {
            rgDateRange.setOnCheckedChangeListener { _, id ->
                val mode = when (id) {
                    R.id.rb_7_days -> HealthStatisticsViewModel.DateRangePreset.DAYS_7
                    R.id.rb_1_month -> HealthStatisticsViewModel.DateRangePreset.MONTH_1
                    R.id.rb_3_month -> HealthStatisticsViewModel.DateRangePreset.MONTH_3
                    else -> {
                        HealthStatisticsViewModel.DateRangePreset.CUSTOM
                    }
                }
                mViewModel.selectPreset(mode)

            }
            rbCustom.clickWithDuration {
                showCustomDatePicker()
                // RadioGroup 已经将 custom 置为选中，但 ViewModel 仍是旧 preset。
                // 这里立即同步一次，避免 UI 状态与实际不符。

            }
        }
    }

    private fun setupStatusFilter() {
        with(mViewBind.tvFilterStatu) {
            // 动态设置点击行为
            clickWithDuration {
                when (mViewModel.selectedMetricType.value) {
                    HealthMetric.BLOOD_SUGAR -> {
                        StatusSelectDialog.show(
                            fragmentManager = supportFragmentManager,
                            currentStatus = mViewModel.selectedStatus.value,
                            showAllOption = true
                        ) { status ->
                            mViewModel.updateStatusFilter(status)
                        }
                    }
                    HealthMetric.BLOOD_PRESSURE, HealthMetric.CHOLESTEROL -> {
                        if (dimensionMenu == null) {
                            dimensionMenu = StatisticDimensionMenu(this@HealthStatisticsActivity) { dimension ->
                                mViewModel.setStatisticDimension(dimension)
                            }.apply {
                                isFocusable = true
                                isOutsideTouchable = true

                                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                            }
                        }
                        dimensionMenu?.show(this)
                    }
                    else -> {
                        // 其他指标不处理
                    }
                }
            }
        }
    }

    private fun setupHistoryList() {
        historyAdapter.setOnItemClickListener(object : HistoryAdapter.OnItemClickListener {
            override fun onItemClick(item: HistoryRecordItem, position: Int) {
                when (item.getRecordType()) {
                    HistoryRecordItem.RecordType.BLOOD_SUGAR -> BsDetailActivity.start(this@HealthStatisticsActivity, item.getId())
                    HistoryRecordItem.RecordType.BLOOD_PRESSURE -> BpDetailActivity.start(this@HealthStatisticsActivity, item.getId())
                    HistoryRecordItem.RecordType.CHOLESTEROL -> CholesterolDetailActivity.start(this@HealthStatisticsActivity, item.getId())
                    HistoryRecordItem.RecordType.HEART_RATE -> HeartRateDetailActivity.start(this@HealthStatisticsActivity, item.getId())
                    HistoryRecordItem.RecordType.BMI_RECORD -> BmiDetailActivity.start(this@HealthStatisticsActivity, item.getId())
                }
            }

            override fun onDeleteClick(item: HistoryRecordItem, position: Int) {
                // 删除入口在统计页关闭
            }
        })
        mViewBind.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HealthStatisticsActivity)
            adapter = historyAdapter
        }
    }

    private fun setupActions() {
        mViewBind.tvAllHistory.clickWithDuration {
            val recordType = when (mViewModel.selectedMetricType.value) {
                HealthMetric.BLOOD_SUGAR -> HistoryRecordItem.RecordType.BLOOD_SUGAR
                HealthMetric.BLOOD_PRESSURE -> HistoryRecordItem.RecordType.BLOOD_PRESSURE
                HealthMetric.CHOLESTEROL -> HistoryRecordItem.RecordType.CHOLESTEROL
                HealthMetric.HEART_RATE -> HistoryRecordItem.RecordType.HEART_RATE
                HealthMetric.BMI -> HistoryRecordItem.RecordType.BMI_RECORD
                else -> HistoryRecordItem.RecordType.BLOOD_SUGAR
            }
            HistoryRecordActivity.start(this, recordType = recordType)
        }
        mViewBind.btnAddRecord.clickWithDuration {
            when (mViewModel.selectedMetricType.value) {
                HealthMetric.BLOOD_SUGAR -> BsRecordActivity.start(this)
                HealthMetric.BLOOD_PRESSURE -> BpRecordActivity.start(this)
                HealthMetric.CHOLESTEROL -> CholesterolRecordActivity.start(this)
                HealthMetric.HEART_RATE -> HeartRateRecordActivity.start(this)
                HealthMetric.BMI -> BmiRecordActivity.start(this)
                HealthMetric.HYDRATION -> HydrateActivity.start(this)
                else -> BsRecordActivity.start(this)
            }
        }
    }

    override fun createObserver() {
        collectLatest(mViewModel.selectedMetricType) { metricType ->
            currentMetricType = metricType
            updateUIForMetricType(metricType)
            updateActionButtonVisibility()
//            applyChartPaddingForMetric(metricType)
        }
        collectLatest(mViewModel.statusFilterVisible) { visible ->
            mViewBind.tvFilterStatu.isVisible = visible
        }
        collectLatest(mViewModel.dimensionSelectorVisible) { visible ->
            mViewBind.tvFilterStatu.isVisible = visible
        }
        collectLatest(mViewModel.statisticDimension) { dimension ->
            updateDimensionDisplay(dimension)
        }
        collectLatest(mViewModel.selectedPreset) { preset ->
            updatePresetSelection(preset)
        }
        collectLatest(mViewModel.dateRange) { range ->
            latestDateRange = range
        }
        collectLatest(mViewModel.dateRangeText) { formatted ->
            mViewBind.tvDateRange.text = formatted
        }
        collectLatest(mViewModel.selectedStatus) { status ->
            if(mViewModel.selectedMetricType.value == HealthMetric.BLOOD_SUGAR)
            updateStatusDisplay(status)
        }
        collectLatest(mViewModel.statsUiState) { stats ->
            renderStats(stats)
        }
        collectLatest(mViewModel.chartUiState) { state ->
            if (currentMetricType == HealthMetric.STEPS || currentMetricType == HealthMetric.HYDRATION) {
                mViewBind.chartView.setAnimationDuration(0)
                mViewBind.chartView.animateIn = false
                chartManager.renderColumn(state, isShowLabel = true, enableScroll = true)
            } else {
                chartManager.render(state)
            }
        }
        collectLatest(mViewModel.historyPreview) { records ->
            val items = records.mapNotNull { record ->
                when (mViewModel.selectedMetricType.value) {
                    HealthMetric.BLOOD_SUGAR -> (record as? com.healthtracker.blood.suger.data.entity.BloodSugarRecord)?.let { BloodSugarHistoryItem(it) }
                    HealthMetric.BLOOD_PRESSURE -> (record as? com.healthtracker.blood.suger.data.entity.BloodPressureRecord)?.let { BloodPressureHistoryItem(it) }
                    HealthMetric.CHOLESTEROL -> (record as? com.healthtracker.blood.suger.data.entity.CholesterolRecord)?.let { CholesterolHistoryItem(it) }
                    HealthMetric.HEART_RATE -> (record as? com.healthtracker.blood.suger.data.entity.HeartRateRecord)?.let { HeartRateHistoryItem(it) }
                    HealthMetric.BMI -> (record as? com.healthtracker.blood.suger.data.entity.BmiRecord)?.let { BmiHistoryItem(it) }
                    else -> null
                }
            }
            historyAdapter.submitList(items)
        }
        collectLatest(mViewModel.historySectionVisible) { visible ->
            isHistorySectionVisible = visible
            updateHistoryHeaderVisibility(visible)
            updateHistoryListVisibility()
        }
        collectLatest(mViewModel.historyListVisible) { listVisible ->
            isHistoryListVisible = listVisible
            updateHistoryListVisibility()
        }
        collectLatest(mViewModel.allHistoryVisible) { show ->
            mViewBind.tvAllHistory.isVisible = isHistorySectionVisible && show && isHistoryEnabled()
        }
        collectLatest(mViewModel.healthTips) { tips ->
            renderHealthTips(tips)
        }
    }

    private fun updatePresetSelection(preset: HealthStatisticsViewModel.DateRangePreset) {
        with(mViewBind) {
            rb7Days.isChecked = preset == HealthStatisticsViewModel.DateRangePreset.DAYS_7
            rb1Month.isChecked = preset == HealthStatisticsViewModel.DateRangePreset.MONTH_1
            rb3Month.isChecked = preset == HealthStatisticsViewModel.DateRangePreset.MONTH_3
            rbCustom.isChecked = preset == HealthStatisticsViewModel.DateRangePreset.CUSTOM
        }
    }

    private fun updateStatusDisplay(status: BloodSugarStatus?) {
        val text = if (status == null) {
            getString(R.string.all_types)
        } else {
            getString(getStatusStringRes(status.statusType))
        }
        mViewBind.tvFilterStatu.text = text
    }

    private fun renderStats(stats: HealthStatisticsViewModel.StatsUiState) {
        mViewBind.tvAvgValue.text = formatStatValue(stats.avgValue, stats)
        mViewBind.tvMinValue.text = formatStatValue(stats.minValue, stats)
        mViewBind.tvMaxValue.text = formatStatValue(stats.maxValue, stats)
        val unitLabel = if (stats.unitLabelRes != 0) getString(stats.unitLabelRes) else ""
        "Unit:${unitLabel.lowercase()}".also { mViewBind.tvUnit.text = it }
        mViewBind.tvUnit.isVisible = unitLabel.isNotEmpty()
    }

    private fun renderHealthTips(tips: HealthTips) {
        mViewBind.tvTipsTitle.text = tips.title
        mViewBind.tvTipsDes.text = tips.description
    }

    private fun updateHistoryHeaderVisibility(visible: Boolean) {
        val header: View? = mViewBind.tvHistory.parent as? View
        val shouldShow = visible && isHistoryEnabled()
        header?.isVisible = shouldShow
        if (!shouldShow) {
            mViewBind.rvHistory.isVisible = false
            mViewBind.tvAllHistory.isVisible = false
        }
    }

    private fun updateHistoryListVisibility() {
        mViewBind.rvHistory.isVisible = isHistorySectionVisible && isHistoryListVisible && isHistoryEnabled()
    }

    private fun updateActionButtonVisibility() {
        mViewBind.btnAddRecord.isVisible =
            currentMetricType != HealthMetric.STEPS
    }

    private fun isHistoryEnabled(): Boolean =
        currentMetricType != HealthMetric.STEPS && currentMetricType != HealthMetric.HYDRATION

    private fun applyChartPaddingForMetric(metricType: HealthMetric) {
        val chart = mViewBind.chartView
        val baseStart = chart.paddingStart
        val baseTop = chart.paddingTop
        val baseBottom = chart.paddingBottom
        val paddingEnd = if (metricType == HealthMetric.STEPS) {
            resources.getDimensionPixelSize(com.healthtracker.framework.R.dimen.dp_12)
        } else {
            0
        }
        chart.setPadding(baseStart, baseTop, paddingEnd, baseBottom)
        chart.clipToPadding = metricType != HealthMetric.STEPS
    }

    private fun updateUIForMetricType(metricType: HealthMetric) {
        mViewBind.llBpChartDes.root.gone()
        mViewBind.llChoChartDes.root.gone()
        when (metricType) {
            HealthMetric.BLOOD_SUGAR, HealthMetric.HEART_RATE, HealthMetric.BMI -> {
                val titleRes = when (metricType) {
                    HealthMetric.HEART_RATE -> {
                        R.string.heart_rate
                    }
                    HealthMetric.BMI -> {
                        R.string.weight_and_bmi
                    }
                    else -> {
                        R.string.blood_suger
                    }
                }
                mViewBind.tvTitle.text = getString(titleRes)
                // 显示 Avg/Min/Max，绿色
                mViewBind.tvAvg.text = getString(R.string.avg)
                mViewBind.tvMin.text = getString(R.string.min)
                mViewBind.tvMax.text = getString(R.string.max)

                val greenColor = ContextCompat.getColor(this,R.color.c5)
                mViewBind.tvAvg.setTextColor(greenColor)
                mViewBind.tvMin.setTextColor(greenColor)
                mViewBind.tvMax.setTextColor(greenColor)
            }
            HealthMetric.STEPS -> {
                mViewBind.tvTitle.text = getString(R.string.step_count)
                mViewBind.tvAvg.text = getString(R.string.avg)
                mViewBind.tvMin.text = getString(R.string.min)
                mViewBind.tvMax.text = getString(R.string.max)

                val greenColor = ContextCompat.getColor(this, R.color.c5)
                mViewBind.tvAvg.setTextColor(greenColor)
                mViewBind.tvMin.setTextColor(greenColor)
                mViewBind.tvMax.setTextColor(greenColor)
            }
            HealthMetric.BLOOD_PRESSURE -> {
                mViewBind.tvTitle.text = getString(R.string.blood_pressure)
                mViewBind.tvFilterStatu.setTypeface(getRobotoMedium(this))
                // 显示 Systolic/Diastolic/Pulse，灰色
                mViewBind.tvAvg.text = getString(R.string.systolic)
                mViewBind.tvMin.text = getString(R.string.diastolic)
                mViewBind.tvMax.text = getString(R.string.pulse)


                val grayColor = ContextCompat.getColor(this,R.color.color_999)
                mViewBind.tvAvg.setTextColor(grayColor)
                mViewBind.tvMin.setTextColor(grayColor)
                mViewBind.tvMax.setTextColor(grayColor)
                mViewBind.llBpChartDes.root.visible()
            }
            HealthMetric.CHOLESTEROL -> {
                mViewBind.tvTitle.text = getString(R.string.cholesterol)
                mViewBind.tvFilterStatu.setTypeface(getRobotoMedium(this))
                // 显示 TG/LDL/HDL，灰色
                mViewBind.tvAvg.text = getString(R.string.hdl)
                mViewBind.tvMin.text = getString(R.string.ldl)
                mViewBind.tvMax.text = getString(R.string.tg)

                val grayColor = ContextCompat.getColor(this,R.color.color_999)
                mViewBind.tvAvg.setTextColor(grayColor)
                mViewBind.tvMin.setTextColor(grayColor)
                mViewBind.tvMax.setTextColor(grayColor)
                mViewBind.llChoChartDes.root.visible()

            }
            HealthMetric.HYDRATION -> {
                mViewBind.tvTitle.text = getString(R.string.hydrate)
                mViewBind.tvAvg.text = getString(R.string.avg)
                mViewBind.tvMin.text = getString(R.string.min)
                mViewBind.tvMax.text = getString(R.string.max)

                val greenColor = ContextCompat.getColor(this, R.color.c5)
                mViewBind.tvAvg.setTextColor(greenColor)
                mViewBind.tvMin.setTextColor(greenColor)
                mViewBind.tvMax.setTextColor(greenColor)
            }
            else -> {
                // 其他指标默认显示 Avg/Min/Max
                mViewBind.tvAvg.text = getString(R.string.avg)
                mViewBind.tvMin.text = getString(R.string.min)
                mViewBind.tvMax.text = getString(R.string.max)
            }
        }
    }

    private fun updateDimensionDisplay(dimension: StatisticDimension) {
        val text = when (dimension) {
            StatisticDimension.AVG -> getString(R.string.avg)
            StatisticDimension.MIN -> getString(R.string.min)
            StatisticDimension.MAX -> getString(R.string.max)
        }
        mViewBind.tvFilterStatu.text = text
    }

    private fun showCustomDatePicker() {
        val range = latestDateRange ?: mViewModel.dateRange.value
        val constraints = CalendarConstraints.Builder()
            .setOpenAt(range.end.time)
            .build()
        val picker = MaterialDatePicker.Builder.dateRangePicker().apply {
            setTheme(R.style.CustomDatePickerTheme)
            setCalendarConstraints(constraints)
            setSelection(
                androidx.core.util.Pair(
                    range.start.toUtcPickerMillis(),
                    range.end.toUtcPickerMillis()
                )
            )
        }.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val start = selection.first ?: return@addOnPositiveButtonClickListener
            val end = selection.second ?: start
            mViewModel.updateCustomRange(
                utcMillisToLocal(start),
                utcMillisToLocal(end)
            )
            mViewModel.selectPreset(HealthStatisticsViewModel.DateRangePreset.CUSTOM)
        }

        picker.show(supportFragmentManager, "BS_STATS_DATE_RANGE")
    }

    private fun formatStatValue(value: String, stats: HealthStatisticsViewModel.StatsUiState) =
        value
}

fun Date.toUtcPickerMillis(): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@toUtcPickerMillis.time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val offset = calendar.timeZone.getOffset(calendar.timeInMillis)
    return calendar.timeInMillis + offset
}

private fun utcMillisToLocal(utcMillis: Long): Long {
    val offset = TimeZone.getDefault().getOffset(utcMillis)
    return utcMillis - offset
}
