package com.daily.health.manager.face.chart

import com.daily.health.manager.util.LineStyle

/**
 * 描述一条折线的数据及样式
 *
 * @param id 唯一标识，用于 marker 或调试
 * @param xValues 与 yValues 一一对应的横坐标集合（通常是递增索引）
 * @param yValues 真实数据值（已完成单位换算）
 * @param label 可选的展示名称，便于 marker 或图例使用
 * @param style 可选自定义样式，缺省使用 HealthLineChartManager 默认主题
 */
data class ChartDataSet(
    @ChartSeriesId val id: String,
    val xValues: List<Float>,
    val yValues: List<Float>,
    val label: String? = null,
    val style: LineStyle? = null
) {
    init {
        require(xValues.size == yValues.size) {
            "ChartDataSet($id) requires xValues and yValues to have the same size; " +
                "x=${xValues.size}, y=${yValues.size}"
        }
    }
}
