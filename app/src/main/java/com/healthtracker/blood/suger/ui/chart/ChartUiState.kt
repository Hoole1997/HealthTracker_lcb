package com.healthtracker.blood.suger.ui.chart

import com.healthtracker.blood.suger.util.ChartConfigHelper
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * ViewModel 输出给图表组件的统一数据模型。
 *
 * 约定：
 * 1. 所有折线共享同一套 xLabels/xValues（即相同的横轴刻度）。
 * 2. yValues 应该已经完成单位换算，Chart 组件对单位无感。
 */
data class ChartUiState(
    val labels: List<String> = emptyList(),
    val dataSets: List<ChartDataSet> = emptyList(),
    val axisPaddingRatio: Double = DEFAULT_PADDING_RATIO,
    val emptyMessage: String? = null,
    val forceIntegerYAxis: Boolean = false
) {

    private val pointCount: Int? = dataSets.firstOrNull()?.xValues?.size

    init {
        pointCount?.let { expected ->
            require(dataSets.all { it.xValues.size == expected }) {
                val mismatchInfo = dataSets.joinToString(", ") { "${it.id}:${it.xValues.size}" }
                "All ChartDataSet instances must share the same xValues size ($expected). Actual: [$mismatchInfo]"
            }
            if (labels.isNotEmpty()) {
                require(labels.size == expected) {
                    "labels size (${labels.size}) must match data point count ($expected)"
                }
            }
        }
    }

    val hasData: Boolean
        get() = dataSets.any { it.xValues.isNotEmpty() && it.yValues.isNotEmpty() }

    /**
     * 计算当前数据所需的 y 轴范围，必要时会自动留白。
     */
    fun computeRange(axisSteps: Int = DEFAULT_AXIS_STEPS): Pair<Double, Double> {
        val series = dataSets
            .map { dataSet -> dataSet.yValues.map(Float::toDouble) }
            .filter { it.isNotEmpty() }
        val range = if (series.isEmpty()) {
            ChartConfigHelper.computeNiceRange(
                series = listOf(listOf(0.0)),
                axisSteps = axisSteps,
                paddingRatio = axisPaddingRatio
            )
        } else {
            ChartConfigHelper.computeNiceRange(
                series = series,
                axisSteps = axisSteps,
                paddingRatio = axisPaddingRatio
            )
        }

        return if (forceIntegerYAxis) {
            enforceIntegerFriendlyRange(range, axisSteps)
        } else {
            range
        }
    }

    /**
     * For integer-only axes (e.g. heart rate), expand the range so the tick marks
     * always cover a reasonable span (avoid repeated labels like 69/70/70/71) and
     * align the bounds to whole numbers.
     */
    private fun enforceIntegerFriendlyRange(
        range: Pair<Double, Double>,
        axisSteps: Int
    ): Pair<Double, Double> {
        var (min, max) = range
        var integerMin = floor(min)
        var integerMax = ceil(max)

        val minSpan = max(MIN_INTEGER_SPAN, (axisSteps - 1).coerceAtLeast(1).toDouble())
        var span = integerMax - integerMin

        if (span < minSpan) {
            val padding = (minSpan - span) / 2
            integerMin = floor(integerMin - padding)
            integerMax = ceil(integerMax + padding)
            span = integerMax - integerMin
        }

        if (integerMin == integerMax) {
            integerMax = integerMin + minSpan
        }

        return integerMin to integerMax
    }

    companion object {
        private const val DEFAULT_PADDING_RATIO = 0.1
        private const val DEFAULT_AXIS_STEPS = 6
        private const val MIN_INTEGER_SPAN = 10.0
    }
}
