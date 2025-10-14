package com.healthtracker.blood.suger.util

import android.content.Context
import android.util.TypedValue
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.ui.weight.RulerView

/**
 * 血糖刻度尺配置帮助类
 */
object BloodSugarScaleHelper {

    /**
     * 为不同单位配置刻度尺
     */
    fun configureRulerForUnit(rulerView: RulerView, unit: BsUnit) {
        val config = BsUnit.getScaleConfig(unit)

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
    private fun getScaleGapForUnit(unit: BsUnit, context: Context): Float {
        val dpValue = when (unit) {
            BsUnit.MMOL_L -> 12f  // mmol/L 使用较大间距
            BsUnit.MG_DL -> 1.5f  // mg/dL 使用很小间距，让刻度非常紧凑
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
    fun getScaleGapDpForUnit(unit: BsUnit): Float {
        return when (unit) {
            BsUnit.MMOL_L -> 12f  // mmol/L 使用较大间距
            BsUnit.MG_DL -> 1.5f  // mg/dL 使用很小间距
        }
    }

    /**
     * 获取推荐的首次显示值
     */
    fun getDefaultValueForUnit(unit: BsUnit): Float {
        return when (unit) {
            BsUnit.MMOL_L -> 4.2f    // 正常血糖值
            BsUnit.MG_DL -> 80f      // 对应的 mg/dL 值（按需求修正）
        }
    }
}