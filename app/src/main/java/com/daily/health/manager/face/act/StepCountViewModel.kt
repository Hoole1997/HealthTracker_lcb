package com.daily.health.manager.face.act

import androidx.lifecycle.viewModelScope
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.DailyStepStat
import com.daily.health.manager.data.repo.StepRepository
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.data.utils.toLocalEpochDay
import com.daily.health.manager.face.chart.ChartDataSet
import com.daily.health.manager.face.chart.ChartUiState
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.util.LanguageUtils
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class StepCountViewModel : BaseViewModel() {
    private val repo = StepRepository.get(App.INSTANCE)

    private val kiloFormatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

    companion object {
        private const val DEFAULT_GOAL_STEPS = 6000
        private const val DAYS_IN_WEEK = 7
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
        val interval = resolveInterval(maxSteps, divisor)
        val maxY = interval * divisor
        val minY = 0.0
        val goalValue = DEFAULT_GOAL_STEPS.toDouble()
        val shouldShowGoal = maxY > goalValue
        val useKiloFormat = maxY >= 1000.0
        val stepsAxisFormatter = CartesianValueFormatter { _, value, _ -> formatStepsLabel(value, useKiloFormat) }

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
            startAxisFormatter = stepsAxisFormatter,
            highlightX = payload.highlightIndex.toDouble()
        )
    }

    val statsFlow: Flow<StepStats> = chartDataFlow.map { it.stats }

    fun recent7DaysFlow(): Flow<List<DailyStepStat>> {
        val weekRange = resolveCurrentWeekRange()
        return repo.range(weekRange.startEpochDay, weekRange.endEpochDay)
    }

    private fun buildChartDataFlow(): Flow<ChartPayload> {
        val weekRange = resolveCurrentWeekRange()
        return repo.range(weekRange.startEpochDay, weekRange.endEpochDay)
            .map { records -> buildChartPayload(records, weekRange) }
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                replay = 1
            )
    }

    private fun buildChartPayload(
        records: List<DailyStepStat>,
        weekRange: WeekRange
    ): ChartPayload {
        val dateRange = (weekRange.startEpochDay..weekRange.endEpochDay).toList()
        val recordMap = records.associateBy { it.dateEpochDay }

        val xValues = mutableListOf<Float>()
        val yValues = mutableListOf<Float>()

        dateRange.forEachIndexed { index, epochDay ->
            val stat = recordMap[epochDay]
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
            labels = getText(R.array.ht_week_simple).toList(), // Use the centralized utility method
            xValues = xValues,
            yValues = yValues,
            stats = stats,
            highlightIndex = weekRange.highlightIndex
        )
    }

    private fun resolveCurrentWeekRange(): WeekRange {
        val todayStart = DateTimeUtils.getTodayRange().first
        val todayEpochDay = todayStart.toLocalEpochDay()
        val calendar = Calendar.getInstance().apply {
            time = todayStart
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromSunday = dayOfWeek - Calendar.SUNDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromSunday)
        val startEpochDay = Date(calendar.timeInMillis).toLocalEpochDay()
        val endEpochDay = startEpochDay + (DAYS_IN_WEEK - 1)
        val highlightIndex = (todayEpochDay - startEpochDay).toInt().coerceIn(0, DAYS_IN_WEEK - 1)
        return WeekRange(
            startEpochDay = startEpochDay,
            endEpochDay = endEpochDay,
            highlightIndex = highlightIndex
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

    private fun resolveInterval(maxSteps: Double, divisor: Int): Double {
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
    val stats: StepStats,
    val highlightIndex: Int
)

private data class WeekRange(
    val startEpochDay: Long,
    val endEpochDay: Long,
    val highlightIndex: Int
)

fun getText(stringRes:Int) = LanguageUtils.attachBaseContext(App.INSTANCE)?.resources?.getStringArray(stringRes) ?: App.INSTANCE.resources.getStringArray(stringRes)
