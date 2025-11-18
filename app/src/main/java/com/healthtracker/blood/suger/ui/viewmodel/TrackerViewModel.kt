package com.healthtracker.blood.suger.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.entity.HydrateRecord
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.data.repository.HydrateRepository
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.blood.suger.data.utils.toLocalEpochDay
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartSeriesIds
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.blood.suger.util.ChartConfigHelper
import com.healthtracker.blood.suger.util.ChartPalette
import com.healthtracker.blood.suger.util.LineStyle
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val bsRepository: BloodSugarRepository,
    private val bpRepository: BloodPressureRepository,
    private val hrRepository: HeartRateRepository,
    private val choRepository: CholesterolRepository,
    private val bmiRepository: BmiRepository,
    private val hydrateRepository: HydrateRepository
) : BaseViewModel() {

    companion object {
        /** 图表显示的记录数量 */
        private const val CHART_RECORDS_LIMIT = 7
        private const val STEP_AXIS_STEPS = 8
        private const val STEP_INTERVAL = 50.0
    }

    // ========== 血糖 ==========
    private val _bsRecords = MutableStateFlow<List<BloodSugarRecord>>(emptyList())
    val bloodSugarChartState: StateFlow<ChartUiState> = _bsRecords
        .map { records -> buildBloodSugarChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== 血压 ==========
    private val _bpRecords = MutableStateFlow<List<BloodPressureRecord>>(emptyList())
    val bloodPressureChartState: StateFlow<ChartUiState> = _bpRecords
        .map { records -> buildBloodPressureChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== 心率 ==========
    private val _hrRecords = MutableStateFlow<List<HeartRateRecord>>(emptyList())
    val heartRateChartState: StateFlow<ChartUiState> = _hrRecords
        .map { records -> buildHeartRateChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== 胆固醇 ==========
    private val _choRecords = MutableStateFlow<List<CholesterolRecord>>(emptyList())
    val cholesterolChartState: StateFlow<ChartUiState> = _choRecords
        .map { records -> buildCholesterolChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    // ========== BMI ==========
    private val _bmiRecords = MutableStateFlow<List<BmiRecord>>(emptyList())
    val bmiChartState: StateFlow<ChartUiState> = _bmiRecords
        .map { records -> buildBmiChart(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ChartUiState())

    private val stepRepository = StepRepository.get(App.INSTANCE)
    private val weekLabels: List<String> = App.INSTANCE.resources
        .getStringArray(R.array.week_simple)
        .toList()
    private val kiloFormatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
    private val _stepChartState = MutableStateFlow(ChartUiState())
    val stepChartState: StateFlow<ChartUiState> = _stepChartState
    private val _hydrateChartState = MutableStateFlow(ChartUiState())
    val hydrateChartState: StateFlow<ChartUiState> = _hydrateChartState

    // 观察标记：防止重复订阅
    private var isObserving = false

    /**
     * 开始观察数据（由 Fragment 可见时调用）
     */
    fun startObservingData() {
        if (isObserving) return
        isObserving = true

        viewModelScope.launch {
            // 并行观察所有5个数据源
            launch {
                bsRepository.getLatestBloodSugarRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _bsRecords.value = records }
            }

            launch {
                bpRepository.getLatestBloodPressureRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _bpRecords.value = records }
            }

            launch {
                hrRepository.getLatestHeartRateRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _hrRecords.value = records }
            }

            launch {
                choRepository.getLatestCholesterolRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _choRecords.value = records }
            }

            launch {
                bmiRepository.getLatestBmiRecords(CHART_RECORDS_LIMIT)
                    .collect { records -> _bmiRecords.value = records }
            }

            launch {
                observeWeeklySteps()
            }

            launch {
                observeWeeklyHydrate()
            }
        }
    }

    /**
     * 构建血糖图表数据（已存在，保持不变）
     */
    private fun buildBloodSugarChart(records: List<BloodSugarRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        // 反转顺序：数据库已按 DESC 排序，图表需要 ASC 显示
        val sorted = records.reversed()

        // 简单数字标签
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { it.glucoseValue.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BS_GLUCOSE,
                    xValues = xValues,
                    yValues = yValues,
                    label = "Blood Sugar",
                    style = LineStyle(color = ChartPalette.lineBloodSugar)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建血压图表数据（双线：收缩压 + 舒张压）
     */
    private fun buildBloodPressureChart(records: List<BloodPressureRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BP_SYS,
                    xValues = xValues,
                    yValues = sorted.map { it.systolicPressure.toFloat() },
                    label = "Systolic",
                    style = LineStyle(color = ChartPalette.lineBpSystolic)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.BP_DIA,
                    xValues = xValues,
                    yValues = sorted.map { it.diastolicPressure.toFloat() },
                    label = "Diastolic",
                    style = LineStyle(color = ChartPalette.lineBpDiastolic)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建心率图表数据（单线）
     */
    private fun buildHeartRateChart(records: List<HeartRateRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }
        val yValues = sorted.map { it.heartRateBpm.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.HR_MAIN,
                    xValues = xValues,
                    yValues = yValues,
                    label = "Heart Rate",
                    style = LineStyle(color = ChartPalette.lineHeartRate)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建胆固醇图表数据（三线：HDL + LDL + TG）
     */
    private fun buildCholesterolChart(records: List<CholesterolRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.CHO_HDL,
                    xValues = xValues,
                    yValues = sorted.map { it.hdl.toFloat() },
                    label = "HDL",
                    style = LineStyle(color = ChartPalette.lineCholesterolHdl)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.CHO_LDL,
                    xValues = xValues,
                    yValues = sorted.map { it.ldl.toFloat() },
                    label = "LDL",
                    style = LineStyle(color = ChartPalette.lineCholesterolLdl)
                ),
                ChartDataSet(
                    id = ChartSeriesIds.CHO_TG,
                    xValues = xValues,
                    yValues = sorted.map { it.triglyceride.toFloat() },
                    label = "TG",
                    style = LineStyle(color = ChartPalette.lineCholesterolTg)
                )
            )
        )
    }

    /**
     * ✨ 新增：构建BMI图表数据（单线，计算BMI值）
     */
    private fun buildBmiChart(records: List<BmiRecord>): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val sorted = records.reversed()
        val labels = sorted.indices.map { (it + 1).toString() }
        val xValues = sorted.indices.map { it.toFloat() }

        // 计算 BMI = weightKg / (heightCm/100)²
        val bmiValues = sorted.map { record ->
            val heightM = record.heightCm / 100.0
            (record.weightKg / heightM.pow(2)).toFloat()
        }

        return ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.BMI,
                    xValues = xValues,
                    yValues = bmiValues,
                    label = "BMI",
                    style = LineStyle(color = ChartPalette.lineBmi)
                )
            )
        )
    }

    private suspend fun observeWeeklySteps() {
        val weekRange = resolveCurrentWeekRange()
        stepRepository.range(weekRange.startEpochDay, weekRange.endEpochDay)
            .collect { records ->
                _stepChartState.value = buildStepChart(records, weekRange)
            }
    }

    private suspend fun observeWeeklyHydrate() {
        val weekRange = resolveCurrentWeekRange()
        hydrateRepository.getRecordsByTimeRange(weekRange.startDate, weekRange.endDate)
            .collect { records ->
                _hydrateChartState.value = buildHydrateChart(records, weekRange)
            }
    }

    private fun buildStepChart(records: List<DailyStepStat>, weekRange: WeekRange): ChartUiState {
        val dateRange = (weekRange.startEpochDay..weekRange.endEpochDay).toList()
        val recordMap = records.associateBy { it.dateEpochDay }

        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()
        dateRange.forEachIndexed { index, epochDay ->
            xValues.add(index.toFloat())
            yValues.add(recordMap[epochDay]?.steps?.toFloat() ?: 0f)
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

        return ChartUiState(
            labels = weekLabels,
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
            startAxisFormatter = stepsAxisFormatter
        )
    }

    private fun resolveCurrentWeekRange(): WeekRange {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromSunday = dayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromSunday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        val startEpochDay = startDate.toLocalEpochDay()

        val endCalendar = calendar.clone() as Calendar
        endCalendar.add(Calendar.DAY_OF_YEAR, CHART_RECORDS_LIMIT - 1)
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)
        val endDate = endCalendar.time
        val endEpochDay = endDate.toLocalEpochDay()

        return WeekRange(startEpochDay, endEpochDay, startDate, endDate)
    }

    private fun resolveStepInterval(maxSteps: Double, divisor: Int): Double {
        if (maxSteps > 700.0) {
            val rank = if (maxSteps <= 3500.0) {
                1.0
            } else {
                1.0 + kotlin.math.ceil((maxSteps - 3500.0) / 3500.0)
            }
            return rank * 500.0
        }
        val intervalBase = if (divisor == 0) maxSteps else maxSteps / divisor
        val intervalMultiplier = kotlin.math.ceil(intervalBase / STEP_INTERVAL).coerceAtLeast(1.0)
        return intervalMultiplier * STEP_INTERVAL
    }

    private fun formatStepsLabel(value: Double, useKiloFormat: Boolean): String {
        if (value <= 0.0) return "0"
        if (!useKiloFormat) {
            val isWhole = kotlin.math.abs(value - value.toInt()) < 1e-4
            return if (isWhole) value.toInt().toString() else kiloFormatter.format(value)
        }
        val thousands = value / 1000.0
        val isWhole = kotlin.math.abs(thousands - thousands.toInt()) < 1e-4
        val formatted = if (isWhole) thousands.toInt().toString() else kiloFormatter.format(thousands)
        return "${formatted}k"
    }

    private fun buildHydrateChart(records: List<HydrateRecord>, weekRange: WeekRange): ChartUiState {
        if (records.isEmpty()) {
            return ChartUiState()
        }

        val dateRange = (weekRange.startEpochDay..weekRange.endEpochDay).toList()
        val totalsByDay = mutableMapOf<Long, Int>()
        records.forEach { record ->
            val epochDay = record.recordTime.toLocalEpochDay()
            if (epochDay in dateRange) {
                val previous = totalsByDay[epochDay] ?: 0
                totalsByDay[epochDay] = previous + record.intakeMl
            }
        }

        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()
        dateRange.forEachIndexed { index, epochDay ->
            xValues.add(index.toFloat())
            yValues.add((totalsByDay[epochDay] ?: 0).toFloat())
        }

        if (yValues.all { it <= 0f }) {
            return ChartUiState()
        }

        val dailyGoalMl = HydrateSettingManager.getDailyCups() * HydrateSettingManager.getCupVolume()
        val (minY, maxY) = ChartConfigHelper.computeNiceRange(
            series = listOf(
                yValues.map(Float::toDouble),
                listOf(dailyGoalMl.toDouble())
            ),
            axisSteps = STEP_AXIS_STEPS,
            paddingRatio = 0.0,
            minLimit = 0.0
        )

        val useKiloFormat = maxY >= 1000.0
        val formatter = object : com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter {
            override fun format(
                context: com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: com.patrykandpatrick.vico.core.cartesian.axis.Axis.Position.Vertical?
            ): CharSequence = formatStepsLabel(value, useKiloFormat = useKiloFormat)
        }

        return ChartUiState(
            labels = weekLabels,
            dataSets = listOf(
                ChartDataSet(
                    id = ChartSeriesIds.WATER,
                    xValues = xValues,
                    yValues = yValues
                )
            ),
            axisPaddingRatio = 0.0,
            goalValue = dailyGoalMl.toDouble(),
            precomputedRange = minY to maxY,
            axisSteps = STEP_AXIS_STEPS,
            startAxisFormatter = formatter,
            baselineLabel = App.INSTANCE.getString(R.string.daily_water_intake)
        )
    }

    private data class WeekRange(
        val startEpochDay: Long,
        val endEpochDay: Long,
        val startDate: Date,
        val endDate: Date
    )
}
