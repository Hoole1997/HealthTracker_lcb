package com.healthtracker.blood.suger.ui.act

import androidx.lifecycle.viewModelScope
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.ui.chart.ChartDataSet
import com.healthtracker.blood.suger.ui.chart.ChartUiState
import com.healthtracker.framework.base.BaseViewModel
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class StepCountViewModel : BaseViewModel() {
    private val repo = StepRepository.get(App.INSTANCE)
    private val dateFormatter = SimpleDateFormat("M/d", Locale.getDefault())
    private val kiloFormatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

    companion object {
        private const val DEFAULT_GOAL_STEPS = 6000
        private const val DAYS_TO_SHOW = 7
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val STEP_AXIS_STEPS = 8
        private const val STEP_INTERVAL = 50.0
    }

    val todayStatFlow: Flow<DailyStepStat?> = repo.observeTodayDynamic()

    private val chartDataFlow: Flow<ChartPayload> = buildChartDataFlow()

    val chartUiStateFlow: Flow<ChartUiState> = chartDataFlow.map { payload ->
        val labels = payload.labels
        val xValues = payload.xValues
        val yValues = payload.yValues

        val maxSteps = yValues.maxOrNull()?.toDouble() ?: 0.0
        val divisor = (STEP_AXIS_STEPS - 1).coerceAtLeast(1)
        val intervalBase = if (divisor == 0) maxSteps else maxSteps / divisor
        val intervalMultiplier = ceil(intervalBase / STEP_INTERVAL).coerceAtLeast(1.0)
        val interval = intervalMultiplier * STEP_INTERVAL
        val maxY = interval * divisor
        val minY = 0.0
        val goalValue = DEFAULT_GOAL_STEPS.toDouble()
        val shouldShowGoal = maxY > goalValue
        val useKiloFormat = maxY >= 1000.0
        val stepsAxisFormatter = object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?
            ): CharSequence = formatStepsLabel(value, useKiloFormat)
        }

        ChartUiState(
            labels = labels,
            dataSets = listOf(
                ChartDataSet(
                    id = "steps",
                    xValues = xValues,
                    yValues = yValues
                )
            ),
            axisPaddingRatio = 0.0,
            emptyMessage = if (yValues.all { it == 0f }) "暂无步数记录" else null,
            forceIntegerYAxis = true,
            goalValue = if (shouldShowGoal) goalValue else null,
            baselineLabel = null,
            precomputedRange = minY to maxY,
            axisSteps = STEP_AXIS_STEPS,
            startAxisFormatter = stepsAxisFormatter
        )
    }

    val statsFlow: Flow<StepStats> = chartDataFlow.map { it.stats }

    fun recent7DaysFlow(): Flow<List<DailyStepStat>> {
        val end = DateTimeUtils.getTodayRange().first.time / MILLIS_PER_DAY
        val start = end - 6
        return repo.range(start, end)
    }

    private fun buildChartDataFlow(): Flow<ChartPayload> {
        val endEpochDay = DateTimeUtils.getTodayRange().first.time / MILLIS_PER_DAY
        val startEpochDay = endEpochDay - (DAYS_TO_SHOW - 1)

        return repo.range(startEpochDay, endEpochDay)
            .map { records -> buildChartPayload(records, startEpochDay, endEpochDay) }
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                replay = 1
            )
    }

    private fun buildChartPayload(
        records: List<DailyStepStat>,
        startEpochDay: Long,
        endEpochDay: Long
    ): ChartPayload {
        val dateRange = (startEpochDay..endEpochDay).toList()
        val recordMap = records.associateBy { it.dateEpochDay }

        val labels = mutableListOf<String>()
        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()

        dateRange.forEachIndexed { index, epochDay ->
            val date = Date(epochDay * MILLIS_PER_DAY)
            val stat = recordMap[epochDay]
            labels.add(dateFormatter.format(date))
            xValues.add(index.toFloat())
            yValues.add(stat?.steps?.toFloat() ?: 0f)
        }

        val stepValues = yValues.map(Float::toInt)
        val stats = StepStats(
            max = stepValues.maxOrNull() ?: 0,
            min = stepValues.minOrNull() ?: 0,
            average = if (stepValues.isEmpty()) 0 else stepValues.average().roundToInt()
        )

        return ChartPayload(
            labels = labels,
            xValues = xValues,
            yValues = yValues,
            stats = stats
        )
    }

    private fun formatStepsLabel(value: Double, useKiloFormat: Boolean): String {
        if (value <= 0.0) return "0"
        if (!useKiloFormat) {
            val isWhole = abs(value - value.toInt()) < 1e-4
            return if (isWhole) value.toInt().toString() else kiloFormatter.format(value)
        }
        val thousands = value / 1000.0
        val isWhole = abs(thousands - thousands.toInt()) < 1e-4
        val formatted = if (isWhole) {
            thousands.toInt().toString()
        } else {
            kiloFormatter.format(thousands)
        }
        return "${formatted}k"
    }
}

data class StepStats(
    val max: Int,
    val min: Int,
    val average: Int
)

private data class ChartPayload(
    val labels: List<String>,
    val xValues: List<Float>,
    val yValues: List<Float>,
    val stats: StepStats
)
