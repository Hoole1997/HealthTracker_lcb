package com.healthtracker.blood.suger.enum

import kotlin.math.roundToInt

enum class BloodSugarUnit(
    val displayName: String,
    val minValue: Float,
    val maxValue: Float,
    val scrollableMinValue: Float,
    val scrollableMaxValue: Float,
    val step: Float,
    val decimalPlaces: Int,
    val scaleCount: Int
) {
    MMOL_L(
        displayName = "mmol/L",
        minValue = 0f,
        maxValue = 37f,
        scrollableMinValue = 1f,
        scrollableMaxValue = 35f,
        step = 0.1f,
        decimalPlaces = 1,
        scaleCount = 5  // 每个大刻度之间有5个小刻度，显示 1.0, 1.5, 2.0, 2.5, 3.0...
    ),

    MG_DL(
        displayName = "mg/dL",
        minValue = 0f,
        maxValue = 700f,  // 显示范围可以更大
        scrollableMinValue = 18f,  // 1.0 * 18.0182
        scrollableMaxValue = 630f,  // 可滚动的最大上限630
        step = 0.1f,
        decimalPlaces = 1,  // 允许选择一位小数，如72.2
        scaleCount = 10  // 每个大刻度之间10个小刻度，每0.5显示一个刻度线
    );

    companion object {
        const val CONVERSION_FACTOR = 18.0182f

        /**
         * 从 mmol/L 转换为 mg/dL
         */
        fun mmolToMgdl(mmolValue: Float): Float {
            return mmolValue * CONVERSION_FACTOR
        }

        /**
         * 从 mg/dL 转换为 mmol/L
         */
        fun mgdlToMmol(mgdlValue: Float): Float {
            return mgdlValue / CONVERSION_FACTOR
        }

        /**
         * 在两个单位之间转换数值
         */
        fun convertValue(value: Float, fromUnit: BloodSugarUnit, toUnit: BloodSugarUnit): Float {
            return when {
                fromUnit == toUnit -> value
                fromUnit == MMOL_L && toUnit == MG_DL -> {
                    // mmol/L -> mg/dL，保持一位小数
                    (mmolToMgdl(value) * 10).roundToInt() / 10f
                }
                fromUnit == MG_DL && toUnit == MMOL_L -> {
                    // mg/dL -> mmol/L，保持一位小数
                    (mgdlToMmol(value) * 10).roundToInt() / 10f
                }
                else -> value
            }
        }

        /**
         * 格式化显示值
         */
        fun formatValue(value: Float, unit: BloodSugarUnit): String {
            return when (unit.decimalPlaces) {
                0 -> value.roundToInt().toString()
                else -> String.format("%.${unit.decimalPlaces}f", value)
            }
        }

        /**
         * 获取刻度配置
         */
        fun getScaleConfig(unit: BloodSugarUnit): ScaleConfig {
            return ScaleConfig(
                minScale = unit.minValue,
                maxScale = unit.maxValue,
                scrollableMinScale = unit.scrollableMinValue,
                scrollableMaxScale = unit.scrollableMaxValue,
                scaleStep = unit.step,
                decimalPlaces = unit.decimalPlaces,
                scaleCount = unit.scaleCount
            )
        }
    }

    /**
     * 刻度配置数据类
     */
    data class ScaleConfig(
        val minScale: Float,
        val maxScale: Float,
        val scrollableMinScale: Float,
        val scrollableMaxScale: Float,
        val scaleStep: Float,
        val decimalPlaces: Int,
        val scaleCount: Int
    )
}