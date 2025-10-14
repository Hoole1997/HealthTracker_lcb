package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.data.enums.BMICategory
import com.healthtracker.blood.suger.enum.BloodSugarLevel
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit

/**
 * 通用等级数据工厂
 * 为 LeveStatusView 提供等级列表与索引计算，便于在不同场景复用。
 */
object LeveDataFactory {

    /** 血压等级数据工厂 */
    object BloodPressure {
        /** 构建 LeveStatusView 需要的等级项列表（顺序与 UI 一致） */
        fun buildItems(context: Context): List<LevelItem> {
            val order = BloodPressureCategory.entries.filter { it != BloodPressureCategory.UNKNOWN }
            return order.map { cat ->
                val shortRangeRes = when (cat) {
                    BloodPressureCategory.LOW -> R.string.blood_pressure_range_low_short
                    BloodPressureCategory.NORMAL -> R.string.blood_pressure_range_normal_short
                    BloodPressureCategory.ELEVATED -> R.string.blood_pressure_range_elevated_short
                    BloodPressureCategory.HIGH_STAGE_1 -> R.string.blood_pressure_range_high_stage_1_short
                    BloodPressureCategory.HIGH_STAGE_2 -> R.string.blood_pressure_range_high_stage_2_short
                    BloodPressureCategory.HYPERTENSIVE_CRISIS -> R.string.blood_pressure_range_hypertensive_crisis_short
                    BloodPressureCategory.UNKNOWN -> R.string.blood_pressure_range_normal_short // 不会使用到
                }

                val sysRes = when (cat) {
                    BloodPressureCategory.LOW -> R.string.bp_range_low_sys
                    BloodPressureCategory.NORMAL -> R.string.bp_range_normal_sys
                    BloodPressureCategory.ELEVATED -> R.string.bp_range_elevated_sys
                    BloodPressureCategory.HIGH_STAGE_1 -> R.string.bp_range_high_stage_1_sys
                    BloodPressureCategory.HIGH_STAGE_2 -> R.string.bp_range_high_stage_2_sys
                    BloodPressureCategory.HYPERTENSIVE_CRISIS -> R.string.bp_range_hypertensive_crisis_sys
                    BloodPressureCategory.UNKNOWN -> R.string.bp_range_normal_sys
                }
                val diaRes = when (cat) {
                    BloodPressureCategory.LOW -> R.string.bp_range_low_dia
                    BloodPressureCategory.NORMAL -> R.string.bp_range_normal_dia
                    BloodPressureCategory.ELEVATED -> R.string.bp_range_elevated_dia
                    BloodPressureCategory.HIGH_STAGE_1 -> R.string.bp_range_high_stage_1_dia
                    BloodPressureCategory.HIGH_STAGE_2 -> R.string.bp_range_high_stage_2_dia
                    BloodPressureCategory.HYPERTENSIVE_CRISIS -> R.string.bp_range_hypertensive_crisis_dia
                    BloodPressureCategory.UNKNOWN -> R.string.bp_range_normal_dia
                }

                LevelItem(
                    name = context.getString(cat.statusTextRes!!),
                    rangeDesc = context.getString(
                        shortRangeRes,
                        context.getString(sysRes),
                        context.getString(diaRes)
                    ),
                    colorInt = ContextCompat.getColor(context, cat.colorRes)
                )
            }
        }

        /** 根据收缩压/舒张压计算索引（排除 UNKNOWN） */
        fun indexFor(systolic: Int, diastolic: Int): Int {
            val order = listOf(
                BloodPressureCategory.LOW,
                BloodPressureCategory.NORMAL,
                BloodPressureCategory.ELEVATED,
                BloodPressureCategory.HIGH_STAGE_1,
                BloodPressureCategory.HIGH_STAGE_2,
                BloodPressureCategory.HYPERTENSIVE_CRISIS
            )
            val category = BloodPressureCategory.fromBloodPressure(systolic, diastolic)
            val idx = order.indexOf(category)
            return if (idx < 0) 0 else idx
        }

        /** 默认索引（Normal） */
        fun defaultIndex(): Int = buildDefaultIndex(orderSize = 6, normalIndex = 1)
    }

    /** 血糖等级数据工厂 */
    object BloodSugar {
        /** 构建等级项列表（根据状态与单位生成范围文案） */
        fun buildItems(context: Context, unit: BsUnit, status: BloodSugarStatus): List<LevelItem> {
            val ranges = status.getRangesForUnit(unit)
            return BloodSugarLevel.entries.map { level ->
                val rangeDesc = when (level) {
                    BloodSugarLevel.LOW -> "< ${BsUnit.formatValue(ranges.lowHigh, unit)}"
                    BloodSugarLevel.NORMAL -> "${BsUnit.formatValue(ranges.normalLow, unit)}~${BsUnit.formatValue(ranges.normalHigh, unit)}"
                    BloodSugarLevel.PREDIABETES -> "${BsUnit.formatValue(ranges.prediabetesLow, unit)}~${BsUnit.formatValue(ranges.prediabetesHigh, unit)}"
                    BloodSugarLevel.DIABETES -> "≥ ${BsUnit.formatValue(ranges.diabetesLow, unit)}"
                }
                LevelItem(
                    name = context.getString(level.statusTextRes),
                    rangeDesc = rangeDesc,
                    colorInt = ContextCompat.getColor(context, level.colorRes)
                )
            }
        }

        /** 根据血糖值/单位/状态计算索引 */
        fun indexFor(value: Float, unit: BsUnit, status: BloodSugarStatus): Int {
            val level = status.getBloodSugarLevel(value, unit)
            return level.ordinal
        }

        /** 默认索引（Normal） */
        fun defaultIndex(): Int = 1
    }

    /** BMI 等级数据工厂 */
    object BMI {
        /** 构建等级项列表（名称与范围来自资源数组） */
        fun buildItems(context: Context): List<LevelItem> {
            val ranges = context.resources.getStringArray(R.array.bmi_level_ranges)
            val categories = BMICategory.values()
            return categories.map { cat ->
                LevelItem(
                    name = context.getString(cat.statusTextRes),
                    rangeDesc = ranges[cat.ordinal],
                    colorInt = ContextCompat.getColor(context, cat.colorRes)
                )
            }
        }

        /** 根据 BMI 数值计算索引 */
        fun indexFor(bmi: Float): Int = BMICategory.fromBmi(bmi).ordinal

        /** 默认索引（Normal） */
        fun defaultIndex(): Int = BMICategory.NORMAL.ordinal
    }

    /** 内部工具：根据总档数与 normalIndex 返回默认索引 */
    private fun buildDefaultIndex(orderSize: Int, normalIndex: Int): Int {
        return if (orderSize > normalIndex && normalIndex >= 0) normalIndex else 0
    }
}