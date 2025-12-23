package com.daily.health.manager.util

import android.content.Context
import android.util.TypedValue
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.ui.weight.RulerView

/**
 * BMI 身高/体重刻度尺配置辅助类
 * 负责根据单位与类型（身高/体重）快速配置 RulerView，保持刻度体验一致。
 */
object BmiScaleHelper {

    /**
     * 刻度类型区分：体重或身高
     */
    enum class ScaleType { WEIGHT, HEIGHT }

    private const val DEFAULT_WEIGHT_KG = 65f
    private const val DEFAULT_HEIGHT_CM = 170f

    /**
     * 为指定类型与单位配置刻度尺属性
     * @return 对应的刻度配置，方便调用方复用
     */
    fun configureRuler(
        rulerView: RulerView,
        unit: BmiUnit,
        type: ScaleType
    ): BmiUnit.ScaleConfig {
        val config = resolveConfig(unit, type)

        with(rulerView) {
            setScaleRange(config.minScale, config.maxScale)
            setScrollableRange(config.scrollableMinScale, config.scrollableMaxScale)
            setScaleStep(config.scaleStep)
            setDecimalPlaces(config.decimalPlaces)
            setScaleCount(config.scaleCount)
            setScaleGap(getScaleGapPx(unit, type, context))

            val rulerUnit = if (unit == BmiUnit.IMPERIAL && type == ScaleType.HEIGHT) {
                RulerView.RulerUnit.DECIMAL_PRECISION
            } else {
                RulerView.RulerUnit.INTEGER_PRECISION
            }
            setRulerUnit(rulerUnit)
        }

        return config
    }

    /**
     * 推荐的默认显示值
     */
    fun getDefaultDisplayValue(unit: BmiUnit, type: ScaleType): Float {
        return when (type) {
            ScaleType.WEIGHT -> BmiUnit.toDisplayWeight(DEFAULT_WEIGHT_KG, unit)
            ScaleType.HEIGHT -> BmiUnit.toDisplayHeight(DEFAULT_HEIGHT_CM, unit)
        }
    }

    /**
     * 将值限制在可滚动范围内
     */
    fun clampValue(value: Float, config: BmiUnit.ScaleConfig): Float {
        return value.coerceIn(config.scrollableMinScale, config.scrollableMaxScale)
    }

    private fun resolveConfig(unit: BmiUnit, type: ScaleType): BmiUnit.ScaleConfig {
        return when (type) {
            ScaleType.WEIGHT -> unit.getWeightScaleConfig()
            ScaleType.HEIGHT -> unit.getHeightScaleConfig()
        }
    }

    private fun getScaleGapPx(unit: BmiUnit, type: ScaleType, context: Context): Float {
        val gapDp = when (type) {
            ScaleType.WEIGHT -> 1.5f

            ScaleType.HEIGHT -> when (unit) {
                BmiUnit.METRIC -> 1.5f  // cm：常见刻度，保持适中文本间隔
                BmiUnit.IMPERIAL -> 12f // in：数值跨度小，放大间隔便于阅读
            }
        }
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            gapDp,
            context.resources.displayMetrics
        )
    }
}
