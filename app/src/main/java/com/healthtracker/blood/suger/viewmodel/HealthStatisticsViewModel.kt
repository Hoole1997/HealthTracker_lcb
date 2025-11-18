package com.healthtracker.blood.suger.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.config.HydrateSettingManager.CupUnit
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.entity.HydrateRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.repository.HydrateRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.blood.suger.data.utils.toLocalEpochDay
import com.healthtracker.blood.suger.tips.HealthTips
import com.healthtracker.blood.suger.tips.HealthTipsProvider
import com.healthtracker.blood.suger.tips.HealthMetric
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.ChartPalette
import com.healthtracker.blood.suger.util.LineStyle
import com.healthtracker.blood.suger.viewmodel.HealthStatisticsViewModel.StatsUiState.Companion.KEY_SELECTED_STATUS
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * 统计维度（用于血压和胆固醇的统计值选择）
 */
enum class StatisticDimension {
    AVG,    // 平均值
    MIN,    // 最小值
    MAX     // 最大值
}

@HiltViewModel
class HealthStatisticsViewModel @Inject constructor(
    private val bloodSugarRepository: BloodSugarRepository,
    private val bloodPressureRepository: BloodPressureRepository,
    private val cholesterolRepository: CholesterolRepository,
    private val heartRateRepository: HeartRateRepository,
    private val bmiRepository: BmiRepository,
    private val hydrateRepository: HydrateRepository,
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
            const val KEY_METRIC_TYPE = "metric_type"
            const val KEY_SELECTED_STATUS = "selected_status"
            const val KEY_STATISTIC_DIMENSION = "statistic_dimension"
            const val HISTORY_PREVIEW_LIMIT = 2
        }
    }

    private val stepRepository = StepRepository.get(App.INSTANCE)
    private val stepKiloFormatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
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

    // 当前选择的健康指标类型
    private val _selectedMetricType = MutableStateFlow(
        savedStateHandle.get<Int>(StatsUiState.KEY_METRIC_TYPE)?.let {
            HealthMetric.entries[it]
        } ?: HealthMetric.BLOOD_SUGAR
    )
    val selectedMetricType: StateFlow<HealthMetric> = _selectedMetricType.asStateFlow()

    // 统计维度选择（仅血压和胆固醇使用）
    private val _statisticDimension = MutableStateFlow(
        savedStateHandle.get<Int>(StatsUiState.KEY_STATISTIC_DIMENSION)?.let {
            StatisticDimension.entries[it]
        } ?: StatisticDimension.AVG
    )
    val statisticDimension: StateFlow<StatisticDimension> = _statisticDimension.asStateFlow()

    // 血糖记录数据
    val bsRecords: StateFlow<List<BloodSugarRecord>> = combine(
        selectedMetricType,
        _dateRange,
        _selectedStatus
    ) { metricType, range, status ->
        Triple(metricType, range, status)
    }.flatMapLatest { (metricType, range, status) ->
        if (metricType == HealthMetric.BLOOD_SUGAR) {
            bloodSugarRepository.getRecordsByRangeAndStatus(
                range.start,
                range.end,
                status?.statusType
            )
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedByDescending { it.recordTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 血压记录数据
    val bpRecords: StateFlow<List<BloodPressureRecord>> = combine(
        selectedMetricType,
        _dateRange
    ) { metricType, range ->
        metricType to range
    }.flatMapLatest { (metricType, range) ->
        if (metricType == HealthMetric.BLOOD_PRESSURE) {
            bloodPressureRepository.getBloodPressureRecordsByTimeRange(range.start, range.end)
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedByDescending { it.recordTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 胆固醇记录数据
    val cholRecords: StateFlow<List<CholesterolRecord>> = combine(
        selectedMetricType,
        _dateRange
    ) { metricType, range ->
        metricType to range
    }.flatMapLatest { (metricType, range) ->
        if (metricType == HealthMetric.CHOLESTEROL) {
            cholesterolRepository.getCholesterolRecordsByTimeRange(range.start, range.end)
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedByDescending { it.recordTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 心率记录数据
    val hrRecords: StateFlow<List<HeartRateRecord>> = combine(
        selectedMetricType,
        _dateRange
    ) { metricType, range ->
        metricType to range
    }.flatMapLatest { (metricType, range) ->
        if (metricType == HealthMetric.HEART_RATE) {
            heartRateRepository.getHeartRateRecordsByTimeRange(range.start, range.end)
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedByDescending { it.recordTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // BMI记录数据
    val bmiRecords: StateFlow<List<BmiRecord>> = combine(
        selectedMetricType,
        _dateRange
    ) { metricType, range ->
        metricType to range
    }.flatMapLatest { (metricType, range) ->
        if (metricType == HealthMetric.BMI) {
            bmiRepository.getBmiRecordsByTimeRange(range.start, range.end)
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedByDescending { it.recordTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val stepRecords: StateFlow<List<DailyStepStat>> = combine(
        selectedMetricType,
        _dateRange
    ) { metricType, range ->
        metricType to range
    }.flatMapLatest { (metricType, range) ->
        if (metricType == HealthMetric.STEPS) {
            val startEpochDay = range.start.toLocalEpochDay()
            val endEpochDay = range.end.toLocalEpochDay()
            stepRepository.range(startEpochDay, endEpochDay)
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedBy { it.dateEpochDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // 饮水记录数据
    private val hydrateRecords: StateFlow<List<HydrateRecord>> = combine(
        selectedMetricType,
        _dateRange
    ) { metricType, range ->
        metricType to range
    }.flatMapLatest { (metricType, range) ->
        if (metricType == HealthMetric.HYDRATION) {
            hydrateRepository.getRecordsByTimeRange(range.start, range.end)
        } else {
            flowOf(emptyList())
        }
    }.map { records ->
        records.sortedBy { it.recordTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedPreset: StateFlow<DateRangePreset> = _selectedPreset.asStateFlow()
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()
    val selectedStatus: StateFlow<BloodSugarStatus?> = _selectedStatus.asStateFlow()

    val dateRangeText: StateFlow<String> = _dateRange
        .map(::formatRange)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatRange(_dateRange.value))

    // 状态过滤器可见性（只有血糖显示）
    val statusFilterVisible: StateFlow<Boolean> = _selectedMetricType
        .map { it == HealthMetric.BLOOD_SUGAR }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // 统计维度选择器可见性（血压和胆固醇显示）
    val dimensionSelectorVisible: StateFlow<Boolean> = _selectedMetricType
        .map { it == HealthMetric.BLOOD_PRESSURE || it == HealthMetric.CHOLESTEROL }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val historyPreview: StateFlow<List<Any>> = combine(
        selectedMetricType,
        bsRecords,
        bpRecords,
        cholRecords,
        hrRecords,
        bmiRecords
    ) { flows: Array<Any> ->
        val metricType = flows[0] as HealthMetric
        val bs = flows[1] as List<BloodSugarRecord>
        val bp = flows[2] as List<BloodPressureRecord>
        val chol = flows[3] as List<CholesterolRecord>
        val hr = flows[4] as List<HeartRateRecord>
        val bmi = flows[5] as List<BmiRecord>
        when (metricType) {
            HealthMetric.BLOOD_SUGAR -> bs
            HealthMetric.BLOOD_PRESSURE -> bp
            HealthMetric.CHOLESTEROL -> chol
            HealthMetric.HEART_RATE -> hr
            HealthMetric.BMI -> bmi
            else -> emptyList()
        }.take(StatsUiState.HISTORY_PREVIEW_LIMIT)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyListVisible: StateFlow<Boolean> = historyPreview
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val totalRecordCount: StateFlow<Int> = combine(
        selectedMetricType,
        bsRecords,
        bpRecords,
        cholRecords,
        hrRecords,
        bmiRecords
    ) { flows: Array<Any> ->
        val metricType = flows[0] as HealthMetric
        val bs = flows[1] as List<BloodSugarRecord>
        val bp = flows[2] as List<BloodPressureRecord>
        val chol = flows[3] as List<CholesterolRecord>
        val hr = flows[4] as List<HeartRateRecord>
        val bmi = flows[5] as List<BmiRecord>
        when (metricType) {
            HealthMetric.BLOOD_SUGAR -> bs.size
            HealthMetric.BLOOD_PRESSURE -> bp.size
            HealthMetric.CHOLESTEROL -> chol.size
            HealthMetric.HEART_RATE -> hr.size
            HealthMetric.BMI -> bmi.size
            else -> 0
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val historySectionVisible: StateFlow<Boolean> = historyPreview
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val allHistoryVisible: StateFlow<Boolean> = totalRecordCount
        .map { total -> total > StatsUiState.HISTORY_PREVIEW_LIMIT }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val statsUiState: StateFlow<StatsUiState> = combine(
        selectedMetricType,
        _statisticDimension,
        bsRecords,
        bpRecords,
        cholRecords,
        hrRecords,
        bmiRecords,
        _preferredUnit,
        stepRecords,
        hydrateRecords
    ) { flows: Array<Any> ->
        val metricType = flows[0] as HealthMetric
        val dimension = flows[1] as StatisticDimension
        val bs = flows[2] as List<BloodSugarRecord>
        val bp = flows[3] as List<BloodPressureRecord>
        val chol = flows[4] as List<CholesterolRecord>
        val hr = flows[5] as List<HeartRateRecord>
        val bmi = flows[6] as List<BmiRecord>
        val unit = flows[7] as BsUnit
        val steps = flows[8] as List<DailyStepStat>
        val hydrates = flows[9] as List<HydrateRecord>
        when (metricType) {
            HealthMetric.BLOOD_SUGAR -> buildBsStats(bs, unit)
            HealthMetric.BLOOD_PRESSURE -> buildBpStats(bp, dimension)
            HealthMetric.CHOLESTEROL -> buildCholStats(chol, dimension)
            HealthMetric.HEART_RATE -> buildHrStats(hr)
            HealthMetric.BMI -> buildBmiStats(bmi)
            HealthMetric.STEPS -> buildStepStats(steps)
            HealthMetric.HYDRATION -> buildHydrateStats(hydrates)
            else -> StatsUiState()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    val chartUiState: StateFlow<ChartUiState> = combine(
        selectedMetricType,
        bsRecords,
        bpRecords,
        cholRecords,
        hrRecords,
        bmiRecords,
        _preferredUnit,
        stepRecords,
        hydrateRecords,
        _dateRange
    ) { flows: Array<Any> ->
        val metricType = flows[0] as HealthMetric
        val bs = flows[1] as List<BloodSugarRecord>
        val bp = flows[2] as List<BloodPressureRecord>
        val chol = flows[3] as List<CholesterolRecord>
        val hr = flows[4] as List<HeartRateRecord>
        val bmi = flows[5] as List<BmiRecord>
        val unit = flows[6] as BsUnit
        val steps = flows[7] as List<DailyStepStat>
        val hydrates = flows[8] as List<HydrateRecord>
        val range = flows[9] as DateRange
        when (metricType) {
            HealthMetric.BLOOD_SUGAR -> buildBsChart(bs, unit)
            HealthMetric.BLOOD_PRESSURE -> buildBpChart(bp)
            HealthMetric.CHOLESTEROL -> buildCholChart(chol)
            HealthMetric.HEART_RATE -> buildHrChart(hr)
            HealthMetric.BMI -> buildBmiChart(bmi)
            HealthMetric.STEPS -> buildStepChart(steps, range)
            HealthMetric.HYDRATION -> buildHydrateChart(hydrates, range)
            else -> ChartUiState()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartUiState())

    val healthTips: StateFlow<HealthTips> = combine(
        _selectedMetricType,
        _dateRange
    ) { metricType, _ ->
        HealthTipsProvider.pickRandom(metricType)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HealthTipsProvider.pickRandom(HealthMetric.BLOOD_SUGAR)
    )

    fun setMetricType(type: HealthMetric) {
        _selectedMetricType.value = type
        savedStateHandle[StatsUiState.KEY_METRIC_TYPE] = type.ordinal

        // 切换到非血糖指标时清空状态过滤
        if (type != HealthMetric.BLOOD_SUGAR) {
            if (_selectedStatus.value != null) {
                _selectedStatus.value = null
            }
            savedStateHandle.clearSavedStateProvider(KEY_SELECTED_STATUS)
        }

        // 切换到非血压/胆固醇时重置维度选择
        if (type != HealthMetric.BLOOD_PRESSURE && type != HealthMetric.CHOLESTEROL) {
            if (_statisticDimension.value != StatisticDimension.AVG) {
                _statisticDimension.value = StatisticDimension.AVG
            }
            savedStateHandle[StatsUiState.KEY_STATISTIC_DIMENSION] = StatisticDimension.AVG.ordinal
        }
    }

    fun setStatisticDimension(dimension: StatisticDimension) {
        _statisticDimension.value = dimension
        savedStateHandle[StatsUiState.KEY_STATISTIC_DIMENSION] = dimension.ordinal
    }

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
        // 只在血糖指标时有效
        if (_selectedMetricType.value == HealthMetric.BLOOD_SUGAR) {
            _selectedStatus.value = status
            if (status == null) {
                savedStateHandle.remove(KEY_SELECTED_STATUS)
            } else {
                savedStateHandle[KEY_SELECTED_STATUS] = status.statusType
            }
        }
    }

    fun refreshPreferredUnit() {
        val preferred = BsUnit.getPreferredUnit()
        if (_preferredUnit.value != preferred) {
            _preferredUnit.value = preferred
        }
    }

    private fun buildBsStats(records: List<BloodSugarRecord>, unit: BsUnit): StatsUiState {
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

    private fun buildBpStats(records: List<BloodPressureRecord>, dimension: StatisticDimension): StatsUiState {
        val unit = App.INSTANCE.getString(R.string.mmHg)
        if (records.isEmpty()) {
            return StatsUiState(unitLabel = unit)
        }

        val systolicValues = records.map { it.systolicPressure }
        val diastolicValues = records.map { it.diastolicPressure }
        val pulseValues = records.map { it.pulseRate }

        val (sys, dia, pulse) = when (dimension) {
            StatisticDimension.AVG -> Triple(
                systolicValues.average().toInt(),
                diastolicValues.average().toInt(),
                pulseValues.average().toInt()
            )
            StatisticDimension.MIN -> Triple(
                systolicValues.minOrNull() ?: 0,
                diastolicValues.minOrNull() ?: 0,
                pulseValues.minOrNull() ?: 0
            )
            StatisticDimension.MAX -> Triple(
                systolicValues.maxOrNull() ?: 0,
                diastolicValues.maxOrNull() ?: 0,
                pulseValues.maxOrNull() ?: 0
            )
        }

        return StatsUiState(
            avgValue = sys.toString(),
            minValue = dia.toString(),
            maxValue = pulse.toString(),
            unitLabel = unit,
            hasData = true
        )
    }

    private fun buildCholStats(records: List<CholesterolRecord>, dimension: StatisticDimension): StatsUiState {
        val unit = App.INSTANCE.getString(R.string.mg_dl)
        if (records.isEmpty()) {
            return StatsUiState(unitLabel = unit)
        }

        val tgValues = records.map { it.triglyceride }
        val ldlValues = records.map { it.ldl }
        val hdlValues = records.map { it.hdl }

        val (tg, ldl, hdl) = when (dimension) {
            StatisticDimension.AVG -> Triple(
                tgValues.average().toInt(),
                ldlValues.average().toInt(),
                hdlValues.average().toInt()
            )
            StatisticDimension.MIN -> Triple(
                tgValues.minOrNull() ?: 0,
                ldlValues.minOrNull() ?: 0,
                hdlValues.minOrNull() ?: 0
            )
            StatisticDimension.MAX -> Triple(
                tgValues.maxOrNull() ?: 0,
                ldlValues.maxOrNull() ?: 0,
                hdlValues.maxOrNull() ?: 0
            )
        }

        return StatsUiState(
            avgValue = hdl.toString(),
            minValue = ldl.toString(),
            maxValue = tg.toString(),
            unitLabel = unit,
            hasData = true
        )
    }

    private fun buildHrStats(records: List<HeartRateRecord>): StatsUiState {
        val unit = App.INSTANCE.getString(R.string.bpm)
        if (records.isEmpty()) {
            return StatsUiState(unitLabel = unit)
        }

        val hrValues = records.map { it.heartRateBpm }
        val avg = hrValues.average().toInt()
        val min = hrValues.minOrNull() ?: 0
        val max = hrValues.maxOrNull() ?: 0

        return StatsUiState(
            avgValue = avg.toString(),
            minValue = min.toString(),
            maxValue = max.toString(),
            unitLabel = unit,
            hasData = true
        )
    }

    private fun buildBmiStats(records: List<BmiRecord>): StatsUiState {
        if (records.isEmpty()) {
            return StatsUiState()
        }

        val bmiValues = records.map {
            val heightM = it.heightCm / 100.0
            it.weightKg / (heightM * heightM)
        }
        val avg = bmiValues.average()
        val min = bmiValues.minOrNull() ?: 0.0
        val max = bmiValues.maxOrNull() ?: 0.0

        return StatsUiState(
            avgValue = formatValue(avg),
            minValue = formatValue(min),
            maxValue = formatValue(max),
            unitLabel = "",
            hasData = true
        )
    }

    private fun formatValue(source: Double) = String.format(Locale.getDefault(),"%.1f",source)

    private fun roundToSingleDecimal(value: Double): Float {
        return ((value * 10).roundToInt() / 10f)
    }

    private fun buildBsChart(records: List<BloodSugarRecord>, unit: BsUnit): ChartUiState {
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

    private fun buildBpChart(records: List<BloodPressureRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }
        val sorted = records.sortedWith(
            compareBy<BloodPressureRecord> { it.recordTime.time }.thenBy { it.updatedAt }
        )
        val labels = sorted.map { labelFormatter.format(it.recordTime) }
        val xValues = sorted.indices.map { it.toFloat() }
        val systolicValues = sorted.map { it.systolicPressure.toFloat() }
        val diastolicValues = sorted.map { it.diastolicPressure.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BP_SYS,
                    xValues = xValues,
                    yValues = systolicValues,
                    label = "Systolic",
                    style = LineStyle(color = ChartPalette.lineBpSystolic)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.BP_DIA,
                    xValues = xValues,
                    yValues = diastolicValues,
                    label = "Diastolic",
                    style = LineStyle(color = ChartPalette.lineBpDiastolic)
                )
            )
        )
    }

    private fun buildCholChart(records: List<CholesterolRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }
        val sorted = records.sortedWith(
            compareBy<CholesterolRecord> { it.recordTime.time }.thenBy { it.updatedAt }
        )
        val labels = sorted.map { labelFormatter.format(it.recordTime) }
        val xValues = sorted.indices.map { it.toFloat() }
        val tgValues = sorted.map { it.triglyceride.toFloat() }
        val ldlValues = sorted.map { it.ldl.toFloat() }
        val hdlValues = sorted.map { it.hdl.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.CHO_TG,
                    xValues = xValues,
                    yValues = tgValues,
                    label = "TG",
                    style = LineStyle(color = ChartPalette.lineCholesterolTg)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.CHO_LDL,
                    xValues = xValues,
                    yValues = ldlValues,
                    label = "LDL",
                    style = LineStyle(color = ChartPalette.lineCholesterolLdl)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.CHO_HDL,
                    xValues = xValues,
                    yValues = hdlValues,
                    label = "HDL",
                    style = LineStyle(color = ChartPalette.lineCholesterolHdl)
                )
            )
        )
    }

    private fun buildHrChart(records: List<HeartRateRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }
        val sorted = records.sortedWith(
            compareBy<HeartRateRecord> { it.recordTime.time }.thenBy { it.updatedAt }
        )
        val labels = sorted.map { labelFormatter.format(it.recordTime) }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { it.heartRateBpm.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.HR_MAIN,
                    xValues = xValues,
                    yValues = yValues,
                    label = "BPM",
                    style = LineStyle(color = ChartPalette.lineHeartRate)
                )
            )
        )
    }

    private fun buildBmiChart(records: List<BmiRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }
        val sorted = records.sortedWith(
            compareBy<BmiRecord> { it.recordTime.time }.thenBy { it.updatedAt }
        )
        val labels = sorted.map { labelFormatter.format(it.recordTime) }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { record ->
            val heightM = record.heightCm / 100.0
            if (heightM <= 0) {
                0f
            } else {
                roundToSingleDecimal(record.weightKg / (heightM * heightM))
            }
        }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BMI,
                    xValues = xValues,
                    yValues = yValues,
                    label = "BMI",
                    style = LineStyle(color = ChartPalette.lineBmi)
                )
            )
        )
    }

    private fun buildStepStats(records: List<DailyStepStat>): StatsUiState {
        val unit = App.INSTANCE.getString(R.string.text_steps)
        if (records.isEmpty()) {
            return StatsUiState(unitLabel = unit)
        }
        val values = records.map { it.steps }
        val hasData = values.any { it > 0 }
        if (!hasData) {
            return StatsUiState(unitLabel = unit)
        }
        val avg = values.average().roundToInt()
        val min = values.minOrNull() ?: 0
        val max = values.maxOrNull() ?: 0
        return StatsUiState(
            avgValue = avg.toString(),
            minValue = min.toString(),
            maxValue = max.toString(),
            unitLabel = unit,
            hasData = true
        )
    }

    /**
     * 构建饮水统计数据
     */
    private fun buildHydrateStats(records: List<HydrateRecord>): StatsUiState {
        val hydrateUnit = HydrateSettingManager.getCupUnit()
        val unitString = when (hydrateUnit) {
            HydrateSettingManager.CupUnit.FL_OZ -> App.INSTANCE.getString(R.string.fl_oz)
            HydrateSettingManager.CupUnit.ML -> App.INSTANCE.getString(R.string.unit_ml)
        }
        val mlPerUnit = when (hydrateUnit) {
            HydrateSettingManager.CupUnit.FL_OZ -> HydrateSettingManager.toMl(1, HydrateSettingManager.CupUnit.FL_OZ).toDouble()
            HydrateSettingManager.CupUnit.ML -> 1.0
        }
        if (records.isEmpty()) {
            return StatsUiState(unitLabel = unitString)
        }

        val dailyIntakes = records
            .groupBy { record ->
                val cal = Calendar.getInstance()
                cal.time = record.recordTime
                cal.get(Calendar.YEAR) to cal.get(Calendar.DAY_OF_YEAR)
            }
            .mapValues { (_, grouped) -> grouped.sumOf { it.intakeMl }.toDouble() / mlPerUnit }
            .values

        if (dailyIntakes.isEmpty()) {
            return StatsUiState(unitLabel = unitString)
        }

        val avg = dailyIntakes.average().roundToInt()
        val min = dailyIntakes.minOrNull()?.roundToInt() ?: 0
        val max = dailyIntakes.maxOrNull()?.roundToInt() ?: 0

        return StatsUiState(
            avgValue = avg.toString(),
            minValue = min.toString(),
            maxValue = max.toString(),
            unitLabel = unitString,
            hasData = true
        )
    }

    private fun buildStepChart(records: List<DailyStepStat>, range: DateRange): ChartUiState {
        val startEpoch = range.start.toLocalEpochDay()
        val endEpoch = range.end.toLocalEpochDay()
        val recordMap = records.associateBy { it.dateEpochDay }

        val labels = mutableListOf<String>()
        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()

        val calendar = Calendar.getInstance().apply {
            time = millisToStartOfDay(range.start.time)
        }

        var currentEpochDay = startEpoch
        val todayEpoch = millisToStartOfDay(System.currentTimeMillis()).toLocalEpochDay()
        var index = 0
        while (currentEpochDay <= endEpoch && currentEpochDay <= todayEpoch) {
            labels.add(labelFormatter.format(calendar.time))
            xValues.add(index.toFloat())
            yValues.add(recordMap[currentEpochDay]?.steps?.toFloat() ?: 0f)

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            currentEpochDay++
            index++
        }

        if (yValues.all { it <= 0f }) {
            return ChartUiState()
        }

        val maxSteps = yValues.maxOrNull()?.toDouble() ?: 0.0
        val divisor = (STEP_AXIS_STEPS - 1).coerceAtLeast(1)
        val interval = resolveStepInterval(maxSteps, divisor)
        val maxY = interval * divisor
        val useKiloFormat = maxY >= 1000.0
        val stepsAxisFormatter = object : com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter {
            override fun format(
                context: com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: com.patrykandpatrick.vico.core.cartesian.axis.Axis.Position.Vertical?
            ): CharSequence = formatStepsLabel(value, useKiloFormat)
        }
        val highlightIndex = if (todayEpoch in startEpoch..currentEpochDay - 1) {
            (todayEpoch - startEpoch).toInt()
        } else {
            null
        }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.STEP,
                    xValues = xValues,
                    yValues = yValues
                )
            ),
            axisPaddingRatio = 0.0,
            forceIntegerYAxis = true,
            precomputedRange = 0.0 to maxY,
            axisSteps = STEP_AXIS_STEPS,
            startAxisFormatter = stepsAxisFormatter,
            highlightX = highlightIndex?.toDouble()
        )
    }

    /**
     * 构建饮水柱状图
     */
    private fun buildHydrateChart(records: List<HydrateRecord>, range: DateRange): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val hydrateUnit = HydrateSettingManager.getCupUnit()
        val mlPerUnit = when (hydrateUnit) {
            HydrateSettingManager.CupUnit.FL_OZ -> HydrateSettingManager.toMl(1, HydrateSettingManager.CupUnit.FL_OZ).toDouble()
            HydrateSettingManager.CupUnit.ML -> 1.0
        }
        val dailyIntakeMap = records
            .groupBy { millisToStartOfDay(it.recordTime.time) }
            .mapValues { (_, grouped) ->
                val displayValue = grouped.sumOf { it.intakeMl }.toDouble() / mlPerUnit
                if (hydrateUnit == HydrateSettingManager.CupUnit.FL_OZ) {
                    displayValue.roundToInt().toFloat()
                } else {
                    displayValue.toFloat()
                }
            }

        val labels = mutableListOf<String>()
        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()

        val calendar = Calendar.getInstance().apply {
            time = millisToStartOfDay(range.start.time)
        }
        val endCalendar = Calendar.getInstance().apply {
            time = millisToEndOfDay(range.end.time)
        }

        var index = 0
        val todayStart = millisToStartOfDay(System.currentTimeMillis())
        while (calendar.timeInMillis <= endCalendar.timeInMillis && calendar.timeInMillis <= todayStart.time) {
            val dayStart = millisToStartOfDay(calendar.timeInMillis)
            labels.add(labelFormatter.format(dayStart))
            xValues.add(index.toFloat())
            yValues.add(dailyIntakeMap[dayStart] ?: 0f)

            calendar.add(Calendar.DAY_OF_YEAR, 1)
            index++
        }

        if (yValues.all { it <= 0f }) {
            return ChartUiState()
        }

        val maxIntake = yValues.maxOrNull()?.toDouble() ?: 0.0
        val divisor = (HYDRATE_AXIS_STEPS - 1).coerceAtLeast(1)
        val interval = resolveHydrateInterval(maxIntake, divisor)
        val maxY = interval * divisor

        val hydrateAxisFormatter = object : com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter {
            override fun format(
                context: com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: com.patrykandpatrick.vico.core.cartesian.axis.Axis.Position.Vertical?
            ): CharSequence {
                return if (value <= 0.0) "0" else value.toInt().toString()
            }
        }

        val startDay = millisToStartOfDay(range.start.time)
        val endDay = millisToStartOfDay(calendar.timeInMillis - DAY_MILLIS)
        val highlightIndex = if (todayStart.time in startDay.time..endDay.time) {
            ((todayStart.time - startDay.time) / DAY_MILLIS).toInt()
        } else {
            null
        }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.WATER,
                    xValues = xValues,
                    yValues = yValues
                )
            ),
            axisPaddingRatio = 0.0,
            forceIntegerYAxis = true,
            precomputedRange = 0.0 to maxY,
            axisSteps = HYDRATE_AXIS_STEPS,
            startAxisFormatter = hydrateAxisFormatter,
            highlightX = highlightIndex?.toDouble()
        )
    }

    private fun resolveHydrateInterval(maxIntake: Double, divisor: Int): Double {
        if (maxIntake > 500.0) {
            val rank = if (maxIntake <= 2000.0) {
                1.0
            } else {
                1.0 + ceil((maxIntake - 2000.0) / 2000.0)
            }
            return rank * 250.0
        }
        val intervalBase = if (divisor == 0) maxIntake else maxIntake / divisor
        val interval = if(HydrateSettingManager.getCupUnit() == CupUnit.ML) HYDRATE_INTERVAL else HYDRATE_FL_INTERVAL
        val intervalMultiplier = ceil(intervalBase / interval).coerceAtLeast(1.0)
        return intervalMultiplier * interval
    }

    private fun resolveStepInterval(maxSteps: Double, divisor: Int): Double {
        if (maxSteps > 700.0) {
            val rank = if (maxSteps <= 3500.0) {
                1.0
            } else {
                1.0 + ceil((maxSteps - 3500.0) / 3500.0)
            }
            return rank * 500.0
        }
        val intervalBase = if (divisor == 0) maxSteps else maxSteps / divisor
        val intervalMultiplier = ceil(intervalBase / STEP_INTERVAL).coerceAtLeast(1.0)
        return intervalMultiplier * STEP_INTERVAL
    }

    private fun formatStepsLabel(value: Double, useKiloFormat: Boolean): String {
        if (value <= 0.0) return "0"
        if (!useKiloFormat) {
            val isWhole = abs(value - value.toInt()) < 1e-4
            return if (isWhole) value.toInt().toString() else stepKiloFormatter.format(value)
        }
        val thousands = value / 1000.0
        val isWhole = abs(thousands - thousands.toInt()) < 1e-4
        val formatted = if (isWhole) thousands.toInt().toString() else stepKiloFormatter.format(thousands)
        return "${formatted}k"
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
        private const val STEP_AXIS_STEPS = 8
        private const val STEP_INTERVAL = 50.0
        private const val HYDRATE_AXIS_STEPS = 8
        private const val HYDRATE_INTERVAL = 100.0
        private const val HYDRATE_FL_INTERVAL = 5.0
        private const val DAY_MILLIS = 86_400_000L
    }

    private fun convertToDisplayUnit(valueMgdl: Double, target: BsUnit): Float {
        return BsUnit.convertValue(valueMgdl.toFloat(), BsUnit.MG_DL, target)
    }
}
