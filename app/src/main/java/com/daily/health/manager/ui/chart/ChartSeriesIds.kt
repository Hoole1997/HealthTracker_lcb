package com.daily.health.manager.ui.chart

import androidx.annotation.StringDef

@StringDef(
    ChartSeriesIds.BP_SYS,
    ChartSeriesIds.BP_DIA,
    ChartSeriesIds.BS_GLUCOSE,
    ChartSeriesIds.HR_MAIN,
    ChartSeriesIds.CHO_TG,
    ChartSeriesIds.CHO_LDL,
    ChartSeriesIds.CHO_HDL,
    ChartSeriesIds.BMI,
    ChartSeriesIds.WATER,
    ChartSeriesIds.STEP
)
@Retention(AnnotationRetention.SOURCE)
annotation class ChartSeriesId

/**
 * 统一维护图表折线 ID，避免散落的字符串常量。
 */
object ChartSeriesIds {
    const val BP_SYS = "BP_SYS"
    const val BP_DIA = "BP_DIA"

    const val BS_GLUCOSE = "BS_GLUCOSE"

    const val HR_MAIN = "HR_MAIN"

    const val CHO_TG = "CHO_TG"
    const val CHO_LDL = "CHO_LDL"
    const val CHO_HDL = "CHO_HDL"

    const val BMI = "BMI"
    const val WATER = "WATER"
    const val STEP = "STEP"
}
