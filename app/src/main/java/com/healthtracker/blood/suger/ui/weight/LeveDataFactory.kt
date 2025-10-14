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
            return listOf(
                LevelItem(
                    name = context.getString(R.string.blood_pressure_level_low),
                    rangeDesc = context.getString(
                        R.string.blood_pressure_range_low_short,
                        context.getString(R.string.bp_range_low_sys),
                        context.getString(R.string.bp_range_low_dia)
                    ),
                    colorInt = ContextCompat.getColor(context, R.color.color_3487FC)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_pressure_level_normal),
                    rangeDesc = context.getString(
                        R.string.blood_pressure_range_normal_short,
                        context.getString(R.string.bp_range_normal_sys),
                        context.getString(R.string.bp_range_normal_dia)
                    ),
                    colorInt = ContextCompat.getColor(context, R.color.color_05BA7B)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_pressure_level_elevated),
                    rangeDesc = context.getString(
                        R.string.blood_pressure_range_elevated_short,
                        context.getString(R.string.bp_range_elevated_sys),
                        context.getString(R.string.bp_range_elevated_dia)
                    ),
                    colorInt = ContextCompat.getColor(context, R.color.color_FFE902)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_pressure_level_high_stage_1),
                    rangeDesc = context.getString(
                        R.string.blood_pressure_range_high_stage_1_short,
                        context.getString(R.string.bp_range_high_stage_1_sys),
                        context.getString(R.string.bp_range_high_stage_1_dia)
                    ),
                    colorInt = ContextCompat.getColor(context, R.color.color_FFB909)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_pressure_level_high_stage_2),
                    rangeDesc = context.getString(
                        R.string.blood_pressure_range_high_stage_2_short,
                        context.getString(R.string.bp_range_high_stage_2_sys),
                        context.getString(R.string.bp_range_high_stage_2_dia)
                    ),
                    colorInt = ContextCompat.getColor(context, R.color.color_FF8000)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_pressure_level_hypertensive_crisis),
                    rangeDesc = context.getString(
                        R.string.blood_pressure_range_hypertensive_crisis_short,
                        context.getString(R.string.bp_range_hypertensive_crisis_sys),
                        context.getString(R.string.bp_range_hypertensive_crisis_dia)
                    ),
                    colorInt = ContextCompat.getColor(context, R.color.color_FB0301)
                )
            )
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
            return listOf(
                LevelItem(
                    name = context.getString(R.string.blood_sugar_level_low),
                    rangeDesc = "< ${BsUnit.formatValue(ranges.lowHigh, unit)}",
                    colorInt = ContextCompat.getColor(context, BloodSugarLevel.LOW.colorRes)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_sugar_level_normal),
                    rangeDesc = "${BsUnit.formatValue(ranges.normalLow, unit)}~${BsUnit.formatValue(ranges.normalHigh, unit)}",
                    colorInt = ContextCompat.getColor(context, BloodSugarLevel.NORMAL.colorRes)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_sugar_level_prediabetes),
                    rangeDesc = "${BsUnit.formatValue(ranges.prediabetesLow, unit)}~${BsUnit.formatValue(ranges.prediabetesHigh, unit)}",
                    colorInt = ContextCompat.getColor(context, BloodSugarLevel.PREDIABETES.colorRes)
                ),
                LevelItem(
                    name = context.getString(R.string.blood_sugar_level_diabetes),
                    rangeDesc = "≥ ${BsUnit.formatValue(ranges.diabetesLow, unit)}",
                    colorInt = ContextCompat.getColor(context, BloodSugarLevel.DIABETES.colorRes)
                )
            )
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
            val names = context.resources.getStringArray(R.array.bmi_level_names)
            val ranges = context.resources.getStringArray(R.array.bmi_level_ranges)
            val categories = BMICategory.values()
            return categories.map { cat ->
                LevelItem(
                    name = names[cat.ordinal],
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