package com.daily.health.manager.face.act

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
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.entity.BloodPressureRecord
import com.daily.health.manager.data.entity.BloodSugarRecord
import com.daily.health.manager.data.entity.BmiRecord
import com.daily.health.manager.data.entity.CholesterolRecord
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.BloodSugarStatus
import com.daily.health.manager.data.enums.getStatusStringRes
import com.daily.health.manager.databinding.TrActivityHealthStatisticsBinding
import com.daily.health.manager.tips.HealthMetric
import com.daily.health.manager.tips.HealthTips
import com.daily.health.manager.face.chart.HealthLineChartManager
import com.daily.health.manager.face.dialog.StatusSelectDialog
import com.daily.health.manager.face.history.BloodPressureHistoryItem
import com.daily.health.manager.face.history.BloodSugarHistoryItem
import com.daily.health.manager.face.history.BmiHistoryItem
import com.daily.health.manager.face.history.CholesterolHistoryItem
import com.daily.health.manager.face.history.HeartRateHistoryItem
import com.daily.health.manager.face.history.HistoryAdapter
import com.daily.health.manager.face.history.HistoryRecordItem
import com.daily.health.manager.face.widget.StatisticDimensionMenu
import com.daily.health.manager.viewmodel.HealthStatisticsViewModel
import com.daily.health.manager.viewmodel.StatisticDimension
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import com.daily.health.manager.face.tracker.HealthType
import com.healthtracker.framework.util.getRobotoMedium
import org.koin.android.ext.android.inject
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Health Statistics Activity
 */
class HealthStatisticsAct :
    BaseInterActivity<HealthStatisticsViewModel, TrActivityHealthStatisticsBinding>() {

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

            context.startActivity<HealthStatisticsAct>(*extras.toTypedArray())
        }
    }

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()

    private var chartManager: HealthLineChartManager? = null
    private val historyAdapter = HistoryAdapter().apply { showDeleteButton = false }
    private var latestDateRange: HealthStatisticsViewModel.DateRange? = null
    private var isHistorySectionVisible: Boolean = false
    private var isHistoryListVisible: Boolean = false
    private var dimensionMenu: StatisticDimensionMenu? = null
    private var currentMetricType: HealthMetric = HealthMetric.BLOOD_SUGAR

    override fun createViewBinding(): TrActivityHealthStatisticsBinding {
        return TrActivityHealthStatisticsBinding.inflate(layoutInflater)
    }

    override fun getVMModelClass(): Class<HealthStatisticsViewModel> {
        return HealthStatisticsViewModel::class.java
    }

    override fun onResume() {
        super.onResume()
        mViewModel.refreshPreferredUnit()
    }
    
    /**
     * 获取当前显示的健康类型用于返回事件追踪
     */
    override fun getCurrentHealthType(): HealthType {
        return when (currentMetricType) {
            HealthMetric.BLOOD_SUGAR -> HealthType.BLOOD_SUGAR
            HealthMetric.BLOOD_PRESSURE -> HealthType.BLOOD_PRESSURE
            HealthMetric.CHOLESTEROL -> HealthType.CHOLESTEROL
            HealthMetric.HEART_RATE -> HealthType.HEART_RATE
            HealthMetric.BMI -> HealthType.BMI
            HealthMetric.STEPS -> HealthType.WALKING_STEPS
            HealthMetric.HYDRATION -> HealthType.HYDRATE
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        // ViewModel automatically initialized via SavedStateHandle thanks to BaseMVVMActivity fixes

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
                            dimensionMenu = StatisticDimensionMenu(this@HealthStatisticsAct) { dimension ->
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
                    HistoryRecordItem.RecordType.BLOOD_SUGAR -> HealthDetailAct.start(this@HealthStatisticsAct, HealthDetailAct.DetailType.BLOOD_SUGAR, item.getId())
                    HistoryRecordItem.RecordType.BLOOD_PRESSURE -> HealthDetailAct.start(this@HealthStatisticsAct, HealthDetailAct.DetailType.BLOOD_PRESSURE, item.getId())
                    HistoryRecordItem.RecordType.CHOLESTEROL -> HealthDetailAct.start(this@HealthStatisticsAct, HealthDetailAct.DetailType.CHOLESTEROL, item.getId())
                    HistoryRecordItem.RecordType.HEART_RATE -> HealthDetailAct.start(this@HealthStatisticsAct, HealthDetailAct.DetailType.HEART_RATE, item.getId())
                    HistoryRecordItem.RecordType.BMI_RECORD -> HealthDetailAct.start(this@HealthStatisticsAct, HealthDetailAct.DetailType.BMI, item.getId())
                }
            }

            override fun onDeleteClick(item: HistoryRecordItem, position: Int) {
                // 删除入口在统计页关闭
            }
        })
        mViewBind.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HealthStatisticsAct)
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
            HistoryRecordAct.start(this, recordType = recordType)
        }
        mViewBind.btnAddRecord.clickWithDuration {
            when (mViewModel.selectedMetricType.value) {
                HealthMetric.BLOOD_SUGAR -> HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BLOOD_SUGAR
                )
                HealthMetric.BLOOD_PRESSURE -> HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BLOOD_PRESSURE
                )
                HealthMetric.CHOLESTEROL -> HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.CHOLESTEROL
                )
                HealthMetric.HEART_RATE -> HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.HEART_RATE
                )
                HealthMetric.BMI -> HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BMI
                )
                HealthMetric.HYDRATION -> HydrateAct.start(this)
                else -> HealthRecordAct.start(
                    this,
                    HealthRecordAct.RecordType.BLOOD_SUGAR
                )
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
                chartManager?.renderColumn(state, isShowLabel = true, enableScroll = true)
            } else {
                chartManager?.render(state)
            }
        }
        collectLatest(mViewModel.historyPreview) { records ->
            val items = records.mapNotNull { record ->
                when (mViewModel.selectedMetricType.value) {
                    HealthMetric.BLOOD_SUGAR -> (record as? BloodSugarRecord)?.let { BloodSugarHistoryItem(it) }
                    HealthMetric.BLOOD_PRESSURE -> (record as? BloodPressureRecord)?.let { BloodPressureHistoryItem(it) }
                    HealthMetric.CHOLESTEROL -> (record as? CholesterolRecord)?.let { CholesterolHistoryItem(it) }
                    HealthMetric.HEART_RATE -> (record as? HeartRateRecord)?.let { HeartRateHistoryItem(it) }
                    HealthMetric.BMI -> (record as? BmiRecord)?.let { BmiHistoryItem(it) }
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
            getString(R.string.tr_all_types)
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
        "Unit:${if(currentMetricType == HealthMetric.HYDRATION) unitLabel.lowercase() else unitLabel}".also { mViewBind.tvUnit.text = it }
        mViewBind.tvUnit.isVisible = unitLabel.isNotEmpty() && currentMetricType != HealthMetric.BLOOD_PRESSURE && currentMetricType != HealthMetric.CHOLESTEROL
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
                        R.string.tr_heart_rate
                    }
                    HealthMetric.BMI -> {
                        R.string.tr_weight_and_bmi
                    }
                    else -> {
                        R.string.tr_blood_suger
                    }
                }
                mViewBind.tvTitle.text = getString(titleRes)
                // 显示 Avg/Min/Max，绿色
                mViewBind.tvAvg.text = getString(R.string.tr_avg)
                mViewBind.tvMin.text = getString(R.string.tr_min)
                mViewBind.tvMax.text = getString(R.string.tr_max)

                val greenColor = ContextCompat.getColor(this,R.color.c5)
                mViewBind.tvAvg.setTextColor(greenColor)
                mViewBind.tvMin.setTextColor(greenColor)
                mViewBind.tvMax.setTextColor(greenColor)
            }
            HealthMetric.STEPS -> {
                mViewBind.tvTitle.text = getString(R.string.tr_step_count)
                mViewBind.tvAvg.text = getString(R.string.tr_avg)
                mViewBind.tvMin.text = getString(R.string.tr_min)
                mViewBind.tvMax.text = getString(R.string.tr_max)

                val greenColor = ContextCompat.getColor(this, R.color.c5)
                mViewBind.tvAvg.setTextColor(greenColor)
                mViewBind.tvMin.setTextColor(greenColor)
                mViewBind.tvMax.setTextColor(greenColor)
            }
            HealthMetric.BLOOD_PRESSURE -> {
                mViewBind.tvTitle.text = getString(R.string.tr_blood_pressure)
                mViewBind.tvFilterStatu.setTypeface(getRobotoMedium(this))
                // 显示 Systolic/Diastolic/Pulse，灰色
                mViewBind.tvAvg.text = getString(R.string.tr_systolic)
                mViewBind.tvMin.text = getString(R.string.tr_diastolic)
                mViewBind.tvMax.text = getString(R.string.tr_pulse)


                val grayColor = ContextCompat.getColor(this,R.color.color_999)
                mViewBind.tvAvg.setTextColor(grayColor)
                mViewBind.tvMin.setTextColor(grayColor)
                mViewBind.tvMax.setTextColor(grayColor)
                mViewBind.llBpChartDes.root.visible()
            }
            HealthMetric.CHOLESTEROL -> {
                mViewBind.tvTitle.text = getString(R.string.tr_cholesterol)
                mViewBind.tvFilterStatu.setTypeface(getRobotoMedium(this))
                // 显示 TG/LDL/HDL，灰色
                mViewBind.tvAvg.text = getString(R.string.tr_hdl)
                mViewBind.tvMin.text = getString(R.string.tr_ldl)
                mViewBind.tvMax.text = getString(R.string.tr_tg)

                val grayColor = ContextCompat.getColor(this,R.color.color_999)
                mViewBind.tvAvg.setTextColor(grayColor)
                mViewBind.tvMin.setTextColor(grayColor)
                mViewBind.tvMax.setTextColor(grayColor)
                mViewBind.llChoChartDes.root.visible()

            }
            HealthMetric.HYDRATION -> {
                mViewBind.tvTitle.text = getString(R.string.tr_hydrate)
                mViewBind.tvAvg.text = getString(R.string.tr_avg)
                mViewBind.tvMin.text = getString(R.string.tr_min)
                mViewBind.tvMax.text = getString(R.string.tr_max)

                val greenColor = ContextCompat.getColor(this, R.color.c5)
                mViewBind.tvAvg.setTextColor(greenColor)
                mViewBind.tvMin.setTextColor(greenColor)
                mViewBind.tvMax.setTextColor(greenColor)
            }
            else -> {
                // 其他指标默认显示 Avg/Min/Max
                mViewBind.tvAvg.text = getString(R.string.tr_avg)
                mViewBind.tvMin.text = getString(R.string.tr_min)
                mViewBind.tvMax.text = getString(R.string.tr_max)
            }
        }
    }

    private fun updateDimensionDisplay(dimension: StatisticDimension) {
        val text = when (dimension) {
            StatisticDimension.AVG -> getString(R.string.tr_avg)
            StatisticDimension.MIN -> getString(R.string.tr_min)
            StatisticDimension.MAX -> getString(R.string.tr_max)
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
