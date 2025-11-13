package com.healthtracker.blood.suger.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
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
import kotlinx.coroutines.launch

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
    private val _bsRecords = MutableStateFlow<List<BloodSugarRecord>>(emptyList())
    val bsRecords: StateFlow<List<BloodSugarRecord>> = _bsRecords.asStateFlow()

    // 血压记录数据
    private val _bpRecords = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    val bpRecords: StateFlow<List<BloodPressureRecord>> = _bpRecords.asStateFlow()

    // 胆固醇记录数据
    private val _cholRecords = MutableStateFlow<List<CholesterolRecord>>(emptyList())
    val cholRecords: StateFlow<List<CholesterolRecord>> = _cholRecords.asStateFlow()

    // 心率记录数据
    private val _hrRecords = MutableStateFlow<List<HeartRateRecord>>(emptyList())
    val hrRecords: StateFlow<List<HeartRateRecord>> = _hrRecords.asStateFlow()

    // BMI记录数据
    private val _bmiRecords = MutableStateFlow<List<BmiRecord>>(emptyList())
    val bmiRecords: StateFlow<List<BmiRecord>> = _bmiRecords.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _selectedMetricType,
                _dateRange,
                _selectedStatus
            ) { metricType, range, status ->
                Triple(metricType, range, status)
            }.collect { (metricType, range, status) ->
                when (metricType) {
                    HealthMetric.BLOOD_SUGAR -> loadBloodSugarRecords(range, status)
                    HealthMetric.BLOOD_PRESSURE -> loadBloodPressureRecords(range)
                    HealthMetric.CHOLESTEROL -> loadCholesterolRecords(range)
                    HealthMetric.HEART_RATE -> loadHeartRateRecords(range)
                    HealthMetric.BMI -> loadBmiRecords(range)
                    else -> {} // STEPS, HYDRATION 暂未实现
                }
            }
        }
    }

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
        _selectedMetricType,
        _bsRecords,
        _bpRecords,
        _cholRecords,
        _hrRecords,
        _bmiRecords
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
        _selectedMetricType,
        _bsRecords,
        _bpRecords,
        _cholRecords,
        _hrRecords,
        _bmiRecords
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
        _selectedMetricType,
        _statisticDimension,
        _bsRecords,
        _bpRecords,
        _cholRecords,
        _hrRecords,
        _bmiRecords,
        _preferredUnit
    ) { flows: Array<Any> ->
        val metricType = flows[0] as HealthMetric
        val dimension = flows[1] as StatisticDimension
        val bs = flows[2] as List<BloodSugarRecord>
        val bp = flows[3] as List<BloodPressureRecord>
        val chol = flows[4] as List<CholesterolRecord>
        val hr = flows[5] as List<HeartRateRecord>
        val bmi = flows[6] as List<BmiRecord>
        val unit = flows[7] as BsUnit
        when (metricType) {
            HealthMetric.BLOOD_SUGAR -> buildBsStats(bs, unit)
            HealthMetric.BLOOD_PRESSURE -> buildBpStats(bp, dimension)
            HealthMetric.CHOLESTEROL -> buildCholStats(chol, dimension)
            HealthMetric.HEART_RATE -> buildHrStats(hr)
            HealthMetric.BMI -> buildBmiStats(bmi)
            else -> StatsUiState()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    val chartUiState: StateFlow<ChartUiState> = combine(
        _selectedMetricType,
        _bsRecords,
        _bpRecords,
        _cholRecords,
        _hrRecords,
        _bmiRecords,
        _preferredUnit
    ) { flows: Array<Any> ->
        val metricType = flows[0] as HealthMetric
        val bs = flows[1] as List<BloodSugarRecord>
        val bp = flows[2] as List<BloodPressureRecord>
        val chol = flows[3] as List<CholesterolRecord>
        val hr = flows[4] as List<HeartRateRecord>
        val bmi = flows[5] as List<BmiRecord>
        val unit = flows[6] as BsUnit
        when (metricType) {
            HealthMetric.BLOOD_SUGAR -> buildBsChart(bs, unit)
            HealthMetric.BLOOD_PRESSURE -> buildBpChart(bp)
            HealthMetric.CHOLESTEROL -> buildCholChart(chol)
            HealthMetric.HEART_RATE -> buildHrChart(hr)
            HealthMetric.BMI -> buildBmiChart(bmi)
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
            _selectedStatus.value = null
        }

        // 切换到非血压/胆固醇时重置维度选择
        if (type != HealthMetric.BLOOD_PRESSURE && type != HealthMetric.CHOLESTEROL) {
            _statisticDimension.value = StatisticDimension.AVG
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
            savedStateHandle[KEY_SELECTED_STATUS] = status?.statusType
        }
    }

    fun refreshPreferredUnit() {
        val preferred = BsUnit.getPreferredUnit()
        if (_preferredUnit.value != preferred) {
            _preferredUnit.value = preferred
        }
    }

    private fun loadBloodSugarRecords(range: DateRange, status: BloodSugarStatus?) {
        viewModelScope.launch {
            bloodSugarRepository.getRecordsByRangeAndStatus(
                range.start,
                range.end,
                status?.statusType
            ).collect { records ->
                _bsRecords.value = records.sortedByDescending { it.recordTime }
            }
        }
    }

    private fun loadBloodPressureRecords(range: DateRange) {
        viewModelScope.launch {
            bloodPressureRepository.getBloodPressureRecordsByTimeRange(range.start, range.end)
                .collect { records ->
                    _bpRecords.value = records.sortedByDescending { it.recordTime }
                }
        }
    }

    private fun loadCholesterolRecords(range: DateRange) {
        viewModelScope.launch {
            cholesterolRepository.getCholesterolRecordsByTimeRange(range.start, range.end)
                .collect { records ->
                    _cholRecords.value = records.sortedByDescending { it.recordTime }
                }
        }
    }

    private fun loadHeartRateRecords(range: DateRange) {
        viewModelScope.launch {
            heartRateRepository.getHeartRateRecordsByTimeRange(range.start, range.end)
                .collect { records ->
                    _hrRecords.value = records.sortedByDescending { it.recordTime }
                }
        }
    }

    private fun loadBmiRecords(range: DateRange) {
        viewModelScope.launch {
            bmiRepository.getBmiRecordsByTimeRange(range.start, range.end)
                .collect { records ->
                    _bmiRecords.value = records.sortedByDescending { it.recordTime }
                }
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
        val yValues = sorted.map {
            val heightM = it.heightCm / 100.0
            formatValue( (it.weightKg / (heightM * heightM))).toFloat()

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
