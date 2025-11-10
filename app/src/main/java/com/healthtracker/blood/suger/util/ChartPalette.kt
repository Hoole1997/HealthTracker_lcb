package com.healthtracker.blood.suger.util

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R

object ChartPalette {

    @ColorInt
    val lineBpSystolic: Int = color(R.color.chart_line_bp_systolic)

    @ColorInt
    val lineBpDiastolic: Int = color(R.color.chart_line_bp_diastolic)

    @ColorInt
    val lineBloodSugar: Int = color(R.color.chart_line_bs)

    @ColorInt
    val lineHeartRate: Int = color(R.color.chart_line_heart_rate)

    @ColorInt
    val lineBmi: Int = color(R.color.chart_line_bmi)

    @ColorInt
    val lineCholesterolTg: Int = color(R.color.chart_line_cholesterol_tg)

    @ColorInt
    val lineCholesterolLdl: Int = color(R.color.chart_line_cholesterol_ldl)

    @ColorInt
    val lineCholesterolHdl: Int = color(R.color.chart_line_cholesterol_hdl)

    @ColorInt
    val pointStroke: Int = color(R.color.chart_point_stroke)

    @ColorInt
    val grid: Int = color(R.color.chart_grid)

    @ColorInt
    val axisLabel: Int = color(R.color.chart_axis_label)

    private fun color(@ColorRes resId: Int): Int =
        ContextCompat.getColor(App.INSTANCE, resId)
}
