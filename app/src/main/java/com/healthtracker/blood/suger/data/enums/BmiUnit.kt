package com.healthtracker.blood.suger.data.enums

import com.healthtracker.framework.util.SpUtils

/**
 * BMI单位系统：公制(METRIC) 与 英制(IMPERIAL)
 * 显示单位影响身高/体重的展示与输入解析；存储统一使用公制：cm / kg
 */
enum class BmiUnit(val value: Int, val heightLabel: String, val weightLabel: String) {
    METRIC(0, "cm", "kg"),
    IMPERIAL(1, "in", "lbs");

    companion object {
        // 兼容旧键：整体单位
        private const val KEY_PREFERRED_UNIT = "bmi_preferred_unit"
        // 新增：分别偏好键
        private const val KEY_PREFERRED_HEIGHT_UNIT = "bmi_preferred_height_unit"
        private const val KEY_PREFERRED_WEIGHT_UNIT = "bmi_preferred_weight_unit"

        fun fromValue(value: Int): BmiUnit {
            return entries.firstOrNull { it.value == value } ?: METRIC
        }

        // 整体单位（向后兼容）
        fun getPreferredUnit(): BmiUnit {
            val saved = SpUtils.getInt(KEY_PREFERRED_UNIT, METRIC.value)
            return fromValue(saved)
        }

        fun savePreferredUnit(unit: BmiUnit) {
            SpUtils.putInt(KEY_PREFERRED_UNIT, unit.value)
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
                IMPERIAL -> displayHeight * 2.54f // inch -> cm
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
            return when (unit) {
                METRIC -> baseHeightCm
                IMPERIAL -> baseHeightCm / 2.54f // cm -> inch
            }
        }

        fun toDisplayWeight(baseWeightKg: Float, unit: BmiUnit): Float {
            return when (unit) {
                METRIC -> baseWeightKg
                IMPERIAL -> baseWeightKg * 2.20462f // kg -> lb
            }
        }
    }
}