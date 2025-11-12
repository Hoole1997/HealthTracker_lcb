package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.getStatusStringRes
import com.healthtracker.blood.suger.databinding.ActivityHealthStatisticsBinding
import com.healthtracker.blood.suger.tips.HealthTips
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
import com.healthtracker.blood.suger.ui.history.BloodSugarHistoryItem
import com.healthtracker.blood.suger.ui.history.HistoryAdapter
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.blood.suger.viewmodel.HealthStatisticsViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
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

    @Inject
    lateinit var chartManagerFactory: HealthLineChartManager.Factory

    private lateinit var chartManager: HealthLineChartManager
    private val historyAdapter = HistoryAdapter().apply { showDeleteButton = false }
    private var latestDateRange: HealthStatisticsViewModel.DateRange? = null
    private var isHistorySectionVisible: Boolean = false
    private var isHistoryListVisible: Boolean = false

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
                        showCustomDatePicker()
                        // RadioGroup 已经将 custom 置为选中，但 ViewModel 仍是旧 preset。
                        // 这里立即同步一次，避免 UI 状态与实际不符。
                        HealthStatisticsViewModel.DateRangePreset.CUSTOM
                    }
                }
                mViewModel.selectPreset(mode)

            }
        }
    }

    private fun setupStatusFilter() {
        with(mViewBind.tvFilterStatu) {
            isVisible = true
            clickWithDuration {
                StatusSelectDialog.show(
                    fragmentManager = supportFragmentManager,
                    currentStatus = mViewModel.selectedStatus.value,
                    showAllOption = true
                ) { status ->
                    mViewModel.updateStatusFilter(status)
                }
            }
        }
    }

    private fun setupHistoryList() {
        historyAdapter.setOnItemClickListener(object : HistoryAdapter.OnItemClickListener {
            override fun onItemClick(item: HistoryRecordItem, position: Int) {
                BsDetailActivity.start(this@HealthStatisticsActivity, item.getId())
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
            HistoryRecordActivity.start(this, recordType = HistoryRecordItem.RecordType.BLOOD_SUGAR)
        }
        mViewBind.btnAddRecord.clickWithDuration {
            BsRecordActivity.start(this)
        }
    }

    override fun createObserver() {
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
            updateStatusDisplay(status)
        }
        collectLatest(mViewModel.statsUiState) { stats ->
            renderStats(stats)
        }
        collectLatest(mViewModel.chartUiState) { state ->
            val hasData = chartManager.render(state)
            mViewBind.chartView.isVisible = hasData
        }
        collectLatest(mViewModel.historyPreview) { records ->
            val items = records.map { BloodSugarHistoryItem(it) }
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
            mViewBind.tvAllHistory.isVisible = isHistorySectionVisible && show
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
    }

    private fun renderHealthTips(tips: HealthTips) {
        mViewBind.tvTipsTitle.text = tips.title
        mViewBind.tvTipsDes.text = tips.description
    }

    private fun updateHistoryHeaderVisibility(visible: Boolean) {
        val header: View? = mViewBind.tvHistory.parent as? View
        header?.isVisible = visible
        if (!visible) {
            mViewBind.rvHistory.isVisible = false
            mViewBind.tvAllHistory.isVisible = false
        }
    }

    private fun updateHistoryListVisibility() {
        mViewBind.rvHistory.isVisible = isHistorySectionVisible && isHistoryListVisible
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
