package com.healthtracker.blood.suger.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.tips.HealthTips
import com.healthtracker.blood.suger.tips.HealthTipsProvider
import com.healthtracker.blood.suger.tips.HealthTipsProvider.HealthMetric
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.ChartPalette
import com.healthtracker.blood.suger.util.LineStyle
import com.healthtracker.blood.suger.viewmodel.HealthStatisticsViewModel.StatsUiState.Companion.KEY_SELECTED_STATUS
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HealthStatisticsViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository,
    private var savedStateHandle: SavedStateHandle
) : BaseViewModel() {


    enum class DateRangePreset {
        DAYS_7,
        MONTH_1,
        MONTH_3,
        CUSTOM
    }

    data class DateRange(
        val start: Date,
        val end: Date
    )

    data class StatsUiState(
        val avgValue: String = PLACEHOLDER,
        val minValue: String = PLACEHOLDER,
        val maxValue: String = PLACEHOLDER,
        val unitLabel: String = "",
        val hasData: Boolean = false
    ) {
        companion object {
            const val PLACEHOLDER = "--"
            const val KEY_SELECTED_STATUS = "selected_status"
        }
    }

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val labelFormatter = SimpleDateFormat("M/d", Locale.getDefault())

    private val _selectedPreset = MutableStateFlow(DateRangePreset.DAYS_7)
    private val _dateRange = MutableStateFlow(createPresetRange(DateRangePreset.DAYS_7))
    private val _selectedStatus = MutableStateFlow<BloodSugarStatus?>(
        savedStateHandle.get<Int>(KEY_SELECTED_STATUS)?.let {
            BloodSugarStatus.fromStatusType(it)
        }
    )
    private val _preferredUnit = MutableStateFlow(BsUnit.getPreferredUnit())

    val selectedPreset: StateFlow<DateRangePreset> = _selectedPreset.asStateFlow()
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()
    val selectedStatus: StateFlow<BloodSugarStatus?> = _selectedStatus.asStateFlow()

    val dateRangeText: StateFlow<String> = _dateRange
        .map(::formatRange)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatRange(_dateRange.value))

    private val filteredRecords: StateFlow<List<BloodSugarRecord>> =
        combine(_dateRange, _selectedStatus) { range, status ->
            range to status?.statusType
        }.flatMapLatest { (range, statusCode) ->
            bloodSugarRepository.getRecordsByRangeAndStatus(range.start, range.end, statusCode)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyPreview: StateFlow<List<BloodSugarRecord>> = filteredRecords
        .map { records -> records.take(HISTORY_PREVIEW_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyListVisible: StateFlow<Boolean> = filteredRecords
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val totalRecordCount: StateFlow<Int> =
        bloodSugarRepository.getAllBloodSugarRecords()
            .map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val historySectionVisible: StateFlow<Boolean> = filteredRecords
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val allHistoryVisible: StateFlow<Boolean> = totalRecordCount
        .map { total -> total > HISTORY_PREVIEW_LIMIT }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val statsUiState: StateFlow<StatsUiState> = combine(filteredRecords, _preferredUnit) { records, unit ->
        buildStats(records, unit)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        buildStats(emptyList(), _preferredUnit.value)
    )

    val chartUiState: StateFlow<ChartUiState> = combine(filteredRecords, _preferredUnit) { records, unit ->
        buildChart(records, unit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartUiState())

    val healthTips: StateFlow<HealthTips> = combine(_dateRange, _selectedStatus) { _, _ ->
        HealthTipsProvider.pickRandom(HealthMetric.BLOOD_SUGAR)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HealthTipsProvider.pickRandom(HealthMetric.BLOOD_SUGAR)
    )

    fun selectPreset(preset: DateRangePreset) {
        _selectedPreset.value = preset
        if (preset != DateRangePreset.CUSTOM) {
            _dateRange.value = createPresetRange(preset)
        }
    }

    fun updateCustomRange(startMillis: Long, endMillis: Long) {
        val normalizedRange = DateRange(
            start = millisToStartOfDay(startMillis),
            end = millisToEndOfDay(endMillis)
        ).let { range ->
            if (range.start.after(range.end)) {
                DateRange(start = range.end, end = range.start)
            } else {
                range
            }
        }
        _selectedPreset.value = DateRangePreset.CUSTOM
        _dateRange.value = normalizedRange
    }

    fun updateStatusFilter(status: BloodSugarStatus?) {
        _selectedStatus.value = status
    }

    fun refreshPreferredUnit() {
        val preferred = BsUnit.getPreferredUnit()
        if (_preferredUnit.value != preferred) {
            _preferredUnit.value = preferred
        }
    }

    private fun buildStats(records: List<BloodSugarRecord>, unit: BsUnit): StatsUiState {
        if (records.isEmpty()) {
            return StatsUiState(unitLabel = unit.displayName)
        }
        val converted = records.map { convertToDisplayUnit(it.glucoseValue, unit) }
        val avg = converted.average().toFloat()
        val min = converted.minOrNull() ?: 0f
        val max = converted.maxOrNull() ?: 0f
        return StatsUiState(
            avgValue = BsUnit.formatValue(avg, unit),
            minValue = BsUnit.formatValue(min, unit),
            maxValue = BsUnit.formatValue(max, unit),
            unitLabel = unit.displayName,
            hasData = true
        )
    }

    private fun buildChart(records: List<BloodSugarRecord>, unit: BsUnit): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }
        val sorted = records.sortedWith(
            compareBy<BloodSugarRecord> { it.recordTime.time }.thenBy { it.updatedAt }
        )
        val labels = sorted.map { labelFormatter.format(it.recordTime) }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { convertToDisplayUnit(it.glucoseValue, unit) }
        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BS_GLUCOSE,
                    xValues = xValues,
                    yValues = yValues,
                    label = unit.displayName,
                    style = LineStyle(color = ChartPalette.lineBloodSugar)
                )
            )
        )
    }

    private fun createPresetRange(preset: DateRangePreset): DateRange {
        val calendar = Calendar.getInstance()
        val end = millisToEndOfDay(calendar.timeInMillis)
        when (preset) {
            DateRangePreset.DAYS_7 -> calendar.add(Calendar.DAY_OF_YEAR, -6)
            DateRangePreset.MONTH_1 -> calendar.add(Calendar.MONTH, -1)
            DateRangePreset.MONTH_3 -> calendar.add(Calendar.MONTH, -3)
            DateRangePreset.CUSTOM -> { /* handled elsewhere */ }
        }
        val start = millisToStartOfDay(calendar.timeInMillis)
        return DateRange(start = start, end = end)
    }

    private fun millisToStartOfDay(millis: Long): Date {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun millisToEndOfDay(millis: Long): Date {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    private fun formatRange(range: DateRange): String {
        return "${dateFormatter.format(range.start)} - ${dateFormatter.format(range.end)}"
    }

    companion object {
        private const val HISTORY_PREVIEW_LIMIT = 2
    }

    private fun convertToDisplayUnit(valueMgdl: Double, target: BsUnit): Float {
        val converted = BsUnit.convertValue(valueMgdl.toFloat(), BsUnit.MG_DL, target)
        val formatted = BsUnit.formatValue(converted, target)
        return formatted.toFloat()
    }
}
