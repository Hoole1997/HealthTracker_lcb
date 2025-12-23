package com.daily.health.manager.data.enums

import com.daily.health.manager.App
import com.daily.health.manager.R
import com.healthtracker.framework.util.SpUtils
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * BMI单位系统：公制(METRIC) 与 英制(IMPERIAL)
 * 负责：
 * 1. 标尺（RulerView）配置：身高、体重分别维护刻度范围/步长/精度
 * 2. 单位换算：界面展示值与基础存储（cm/kg）之间的互转
 * 3. 用户偏好：分别记忆身高/体重所用的显示单位
 */
enum class BmiUnit(
    val value: Int,
    private val heightConfig: ScaleConfig,
    private val weightConfig: ScaleConfig
) {
    METRIC(
        value = 0,
        heightConfig = ScaleConfig(
            minScale = 0f,
            maxScale = 255f,
            scrollableMinScale = 1f,
            scrollableMaxScale = 250f,
            scaleStep = 0.1f,
            decimalPlaces = 1,
            scaleCount = 10
        ),
        weightConfig = ScaleConfig(
            minScale = 0f,
            maxScale = 255f,
            scrollableMinScale = 1f,
            scrollableMaxScale = 250f,
            scaleStep = 0.1f,
            decimalPlaces = 1,
            scaleCount = 10
        )
    ),
    IMPERIAL(
        value = 1,
        heightConfig = ScaleConfig(
            minScale = 0f,
            maxScale = 9f,
            scrollableMinScale = 1f,
            scrollableMaxScale = 8f,
            scaleStep = 0.1f,
            decimalPlaces = 1,
            scaleCount = 5
        ),
        weightConfig = ScaleConfig(
            minScale = 0f,
            maxScale = 555f,
            scrollableMinScale = 1f,
            scrollableMaxScale = 550f,
            scaleStep = 0.1f,
            decimalPlaces = 1,
            scaleCount = 10
        )
    );

    fun getHeightScaleConfig(): ScaleConfig = heightConfig
    fun getWeightScaleConfig(): ScaleConfig = weightConfig

    companion object {
        // 新增：分别偏好键
        private const val KEY_PREFERRED_HEIGHT_UNIT = "bmi_preferred_height_unit"
        private const val KEY_PREFERRED_WEIGHT_UNIT = "bmi_preferred_weight_unit"

        fun fromValue(value: Int): BmiUnit {
            return entries.firstOrNull { it.value == value } ?: METRIC
        }

        // 独立偏好：身高
        fun getPreferredHeightUnit(): BmiUnit {
            val saved = SpUtils.getInt(KEY_PREFERRED_HEIGHT_UNIT, METRIC.value)
            return fromValue(saved)
        }

        fun savePreferredHeightUnit(unit: BmiUnit) {
            SpUtils.putInt(KEY_PREFERRED_HEIGHT_UNIT, unit.value)
        }

        // 独立偏好：体重
        fun getPreferredWeightUnit(): BmiUnit {
            val saved = SpUtils.getInt(KEY_PREFERRED_WEIGHT_UNIT, METRIC.value)
            return fromValue(saved)
        }

        fun savePreferredWeightUnit(unit: BmiUnit) {
            SpUtils.putInt(KEY_PREFERRED_WEIGHT_UNIT, unit.value)
        }

        // 转换：显示值 -> 基础存储（cm/kg）
        fun toBaseHeightCm(displayHeight: Float, unit: BmiUnit): Float {
            return when (unit) {
                METRIC -> displayHeight
                IMPERIAL -> displayHeight * 30.48f // ft -> cm
            }
        }

        fun toBaseWeightKg(displayWeight: Float, unit: BmiUnit): Float {
            return when (unit) {
                METRIC -> displayWeight
                IMPERIAL -> displayWeight / 2.20462f // lb -> kg
            }
        }

        // 转换：基础存储（cm/kg） -> 显示值
        fun toDisplayHeight(baseHeightCm: Float, unit: BmiUnit): Float {
            val display = when (unit) {
                METRIC -> baseHeightCm
                IMPERIAL -> baseHeightCm / 30.48f // cm -> ft
            }
            return roundByConfig(display, unit.heightConfig.decimalPlaces)
        }

        fun toDisplayWeight(baseWeightKg: Float, unit: BmiUnit): Float {
            val display = when (unit) {
                METRIC -> baseWeightKg
                IMPERIAL -> baseWeightKg * 2.20462f // kg -> lb
            }
            return roundByConfig(display, unit.weightConfig.decimalPlaces)
        }

        fun formatDisplayHeight(baseHeightCm: Float, unit: BmiUnit): String {
            return when (unit) {
                METRIC -> {
                    val displayValue = toDisplayHeight(baseHeightCm, unit)
                    formatValue(displayValue, unit.heightConfig.decimalPlaces)
                }
                IMPERIAL -> formatFeetInches(baseHeightCm)
            }
        }

        fun formatDisplayWeight(baseWeightKg: Float, unit: BmiUnit): String {
            val displayValue = toDisplayWeight(baseWeightKg, unit)
            return formatValue(displayValue, unit.weightConfig.decimalPlaces)
        }

        private fun roundByConfig(value: Float, decimalPlaces: Int): Float {
            if (decimalPlaces <= 0) {
                return value.roundToInt().toFloat()
            }
            val factor = 10.0.pow(decimalPlaces.toDouble()).toFloat()
            return (value * factor).roundToInt() / factor
        }

        private fun formatValue(value: Float, decimalPlaces: Int): String {
            return if (decimalPlaces <= 0) {
                value.roundToInt().toString()
            } else {
                String.format(Locale.ROOT, "%.${decimalPlaces}f", value)
            }
        }

        private fun formatFeetInches(baseHeightCm: Float): String {
            if (baseHeightCm <= 0f) {
                return "0\'0\""
            }
            val totalInches = baseHeightCm / 2.54f
            var feet = (totalInches / 12f).toInt()
            var inches = (totalInches - feet * 12).roundToInt()

            if (inches == 12) {
                feet += 1
                inches = 0
            }

            if (feet < 0) feet = 0
            if (inches < 0) inches = 0

            return "${feet}'${inches}\""
        }

        fun getWeightUnitLabel() = if (getPreferredWeightUnit() == METRIC) {
            App.INSTANCE.getString(R.string.ht_unit_kg)
        } else {
            App.INSTANCE.getString(R.string.ht_unit_lb)
        }

        fun getHeightUnitLabel() = if (getPreferredWeightUnit() == METRIC) {
            App.INSTANCE.getString(R.string.ht_unit_cm)
        } else {
            App.INSTANCE.getString(R.string.ht_unit_ft_in)
        }
    }

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
