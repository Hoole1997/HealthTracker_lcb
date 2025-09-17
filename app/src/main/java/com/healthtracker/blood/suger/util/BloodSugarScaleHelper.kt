package com.healthtracker.blood.suger.util

import android.content.Context
import android.util.TypedValue
import com.healthtracker.blood.suger.enum.BloodSugarUnit
import com.healthtracker.blood.suger.ui.weight.BloodSugarRulerView

/**
 * 血糖刻度尺配置帮助类
 */
object BloodSugarScaleHelper {

    /**
     * 为不同单位配置刻度尺
     */
    fun configureRulerForUnit(rulerView: BloodSugarRulerView, unit: BloodSugarUnit) {
        val config = BloodSugarUnit.getScaleConfig(unit)

        with(rulerView) {
            // 设置当前单位（必须在其他配置之前设置）
            setCurrentUnit(unit)

            // 基础刻度配置
            setScaleRange(config.minScale, config.maxScale)
            setScrollableRange(config.scrollableMinScale, config.scrollableMaxScale)
            setScaleStep(config.scaleStep)
            setDecimalPlaces(config.decimalPlaces)
            setScaleCount(config.scaleCount)

            // 动态设置刻度间距
            val scaleGapPx = getScaleGapForUnit(unit, context)
            setScaleGap(scaleGapPx)
        }
    }

    /**
     * 获取单位对应的刻度间距（像素值）
     */
    private fun getScaleGapForUnit(unit: BloodSugarUnit, context: Context): Float {
        val dpValue = when (unit) {
            BloodSugarUnit.MMOL_L -> 12f  // mmol/L 使用较大间距
            BloodSugarUnit.MG_DL -> 1.5f  // mg/dL 使用很小间距，让刻度非常紧凑
        }
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            context.resources.displayMetrics
        )
    }

    /**
     * 获取单位对应的刻度间距（dp值）
     */
    fun getScaleGapDpForUnit(unit: BloodSugarUnit): Float {
        return when (unit) {
            BloodSugarUnit.MMOL_L -> 12f  // mmol/L 使用较大间距
            BloodSugarUnit.MG_DL -> 1.5f  // mg/dL 使用很小间距
        }
    }

    /**
     * 获取推荐的首次显示值
     */
    fun getDefaultValueForUnit(unit: BloodSugarUnit): Float {
        return when (unit) {
            BloodSugarUnit.MMOL_L -> 4.2f    // 正常血糖值
            BloodSugarUnit.MG_DL -> 80f      // 对应的 mg/dL 值（按需求修正）
        }
    }

    /**
     * 验证值是否在合理范围内
     */
    fun isValueInValidRange(value: Float, unit: BloodSugarUnit): Boolean {
        return value >= unit.scrollableMinValue && value <= unit.scrollableMaxValue
    }

    /**
     * 将值限制在有效范围内
     */
    fun clampValueToValidRange(value: Float, unit: BloodSugarUnit): Float {
        return value.coerceIn(unit.scrollableMinValue, unit.scrollableMaxValue)
    }
}