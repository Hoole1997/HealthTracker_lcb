package com.healthtracker.blood.suger.ui.chart

import com.healthtracker.blood.suger.util.ChartConfigHelper

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
    val emptyMessage: String? = null
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
        return if (series.isEmpty()) {
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
    }

    companion object {
        private const val DEFAULT_PADDING_RATIO = 0.1
        private const val DEFAULT_AXIS_STEPS = 6
    }
}
