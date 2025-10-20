package com.healthtracker.blood.suger.data.enums

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.BloodSugarRangeManager
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.blood.suger.ui.weight.LevelCategory
import kotlin.math.roundToInt

/**
 * 血糖测量状态枚举与范围定义
 */
enum class BloodSugarStatus(
    val statusType: Int,
    val defaultMgdlRanges: BloodSugarRanges,
    val defaultMmolRanges: BloodSugarRanges
) {
    DEFAULT(
        statusType = 0,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 99.0f,
            prediabetesLow = 99.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 5.5f,
            prediabetesLow = 5.5f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    ),

    FASTING(
        statusType = 1,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 99.0f,
            prediabetesLow = 99.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 5.5f,
            prediabetesLow = 5.5f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    ),

    BEFORE_MEAL(
        statusType = 2,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 99.0f,
            prediabetesLow = 99.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 5.5f,
            prediabetesLow = 5.5f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    ),

    BEDTIME(
        statusType = 3,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 99.0f,
            prediabetesLow = 99.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 5.5f,
            prediabetesLow = 5.5f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    ),

    AFTER_EXERCISE(
        statusType = 4,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 99.0f,
            prediabetesLow = 99.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 5.5f,
            prediabetesLow = 5.5f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    ),

    ONE_HOUR_AFTER_MEAL(
        statusType = 5,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 140.0f,
            prediabetesLow = 140.0f,
            prediabetesHigh = 153.0f,
            diabetesLow = 153.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 7.8f,
            prediabetesLow = 7.8f,
            prediabetesHigh = 8.5f,
            diabetesLow = 8.5f
        )
    ),

    BEFORE_EXERCISE(
        statusType = 6,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 95.0f,
            normalLow = 95.0f,
            normalHigh = 99.0f,
            prediabetesLow = 99.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 5.3f,
            normalLow = 5.3f,
            normalHigh = 5.5f,
            prediabetesLow = 5.5f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    ),

    TWO_HOURS_AFTER_MEAL(
        statusType = 7,
        defaultMgdlRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 72.0f,
            normalLow = 72.0f,
            normalHigh = 85.0f,
            prediabetesLow = 85.0f,
            prediabetesHigh = 126.0f,
            diabetesLow = 126.0f
        ),
        defaultMmolRanges = BloodSugarRanges(
            low = 0f,
            lowHigh = 4.0f,
            normalLow = 4.0f,
            normalHigh = 4.7f,
            prediabetesLow = 4.7f,
            prediabetesHigh = 7.0f,
            diabetesLow = 7.0f
        )
    );

    fun getRangesForUnit(unit: BsUnit): BloodSugarRanges {
        val customRanges = BloodSugarRangeManager.getCustomRangesForStatus(this, unit)
        if (customRanges != null) return customRanges
        return when (unit) {
            BsUnit.MG_DL -> defaultMgdlRanges
            BsUnit.MMOL_L -> defaultMmolRanges
        }
    }

    /**
     * 获取默认范围值（不考虑自定义）
     */
    fun getDefaultRanges(unit: BsUnit): BloodSugarRanges {
        return when (unit) {
            BsUnit.MG_DL -> defaultMgdlRanges
            BsUnit.MMOL_L -> defaultMmolRanges
        }
    }

    fun getBloodSugarLevel(value: Float, unit: BsUnit): BloodSugarLevel {
        val ranges = getRangesForUnit(unit)
        return when {
            value < ranges.lowHigh -> BloodSugarLevel.LOW
            value < ranges.normalHigh -> BloodSugarLevel.NORMAL
            value < ranges.prediabetesHigh -> BloodSugarLevel.PREDIABETES
            else -> BloodSugarLevel.DIABETES
        }
    }

    companion object {
        fun fromStatusType(statusType: Int): BloodSugarStatus {
            return entries.find { it.statusType == statusType } ?: DEFAULT
        }
    }
}

data class BloodSugarRanges(
    val low: Float,
    val lowHigh: Float,
    val normalLow: Float,
    val normalHigh: Float,
    val prediabetesLow: Float,
    val prediabetesHigh: Float,
    val diabetesLow: Float
)

enum class BloodSugarLevel(
    val level: Int,
    override val colorRes: Int,
    val statusTextRes: Int
) : LevelCategory {
    LOW(0, R.color.color_low, R.string.blood_sugar_level_low),
    NORMAL(1, R.color.color_normal, R.string.blood_sugar_level_normal),
    PREDIABETES(2, R.color.color_prediabetes, R.string.blood_sugar_level_prediabetes),
    DIABETES(3, R.color.color_diabetes, R.string.blood_sugar_level_diabetes)
}

fun getStatusStringRes(statusType: Int): Int {
    return when (statusType) {
        BloodSugarStatus.FASTING.statusType -> R.string.blood_sugar_status_fasting
        BloodSugarStatus.BEFORE_MEAL.statusType -> R.string.blood_sugar_status_before_meal
        BloodSugarStatus.BEDTIME.statusType -> R.string.blood_sugar_status_bedtime
        BloodSugarStatus.AFTER_EXERCISE.statusType -> R.string.blood_sugar_status_after_exercise
        BloodSugarStatus.ONE_HOUR_AFTER_MEAL.statusType -> R.string.blood_sugar_status_one_hour_after_meal
        BloodSugarStatus.BEFORE_EXERCISE.statusType -> R.string.blood_sugar_status_before_exercise
        BloodSugarStatus.TWO_HOURS_AFTER_MEAL.statusType -> R.string.blood_sugar_status_two_hours_after_meal
        else -> R.string.blood_sugar_status_default
    }
}

/**
 * 统一的血糖单位枚举类
 */
enum class BsUnit(
    val value: Int,
    val displayName: String,
    val minValue: Float,
    val maxValue: Float,
    val scrollableMinValue: Float,
    val scrollableMaxValue: Float,
    val step: Float,
    val decimalPlaces: Int,
    val scaleCount: Int
) {
    MG_DL(
        value = 0,
        displayName = "mg/dL",
        minValue = 0f,
        maxValue = 700f,
        scrollableMinValue = 18f,
        scrollableMaxValue = 630f,
        step = 0.1f,
        decimalPlaces = 1,
        scaleCount = 10
    ),
    MMOL_L(
        value = 1,
        displayName = "mmol/L",
        minValue = 0f,
        maxValue = 37f,
        scrollableMinValue = 1f,
        scrollableMaxValue = 35f,
        step = 0.1f,
        decimalPlaces = 1,
        scaleCount = 5
    );

    companion object {
        const val CONVERSION_FACTOR = 18.0182
        private const val KEY_PREFERRED_UNIT = "blood_sugar_preferred_unit"

        fun fromValue(value: Int): BsUnit = entries.find { it.value == value } ?: MG_DL

        fun mmolToMgdl(mmolValue: Float): Float = mmolValue * CONVERSION_FACTOR.toFloat()
        fun mgdlToMmol(mgdlValue: Float): Float = mgdlValue / CONVERSION_FACTOR.toFloat()

        fun convertValue(value: Float, fromUnit: BsUnit, toUnit: BsUnit): Float {
            return when {
                fromUnit == toUnit -> value
                fromUnit == MMOL_L && toUnit == MG_DL -> ((mmolToMgdl(value) * 10).roundToInt() / 10f)
                fromUnit == MG_DL && toUnit == MMOL_L -> ((mgdlToMmol(value) * 10).roundToInt() / 10f)
                else -> value
            }
        }

        fun formatValue(value: Float, unit: BsUnit): String {
            return when (unit.decimalPlaces) {
                0 -> value.roundToInt().toString()
                else -> String.format("%.${unit.decimalPlaces}f", value)
            }
        }

        /**
         * 转换 BloodSugarRanges 对象的单位
         * @param ranges 原始范围对象
         * @param fromUnit 原始单位
         * @param toUnit 目标单位
         * @return 转换后的范围对象
         */
        fun convertRanges(
            ranges: BloodSugarRanges,
            fromUnit: BsUnit,
            toUnit: BsUnit
        ): BloodSugarRanges {
            if (fromUnit == toUnit) return ranges

            return BloodSugarRanges(
                low = convertValue(ranges.low, fromUnit, toUnit),
                lowHigh = convertValue(ranges.lowHigh, fromUnit, toUnit),
                normalLow = convertValue(ranges.normalLow, fromUnit, toUnit),
                normalHigh = convertValue(ranges.normalHigh, fromUnit, toUnit),
                prediabetesLow = convertValue(ranges.prediabetesLow, fromUnit, toUnit),
                prediabetesHigh = convertValue(ranges.prediabetesHigh, fromUnit, toUnit),
                diabetesLow = convertValue(ranges.diabetesLow, fromUnit, toUnit)
            )
        }

        fun getScaleConfig(unit: BsUnit): ScaleConfig {
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

        fun savePreferredUnit(unit: BsUnit) {
            SpUtils.putString(KEY_PREFERRED_UNIT, unit.name)
        }

        fun getPreferredUnit(): BsUnit {
            val savedUnitName = SpUtils.getString(KEY_PREFERRED_UNIT)
            return if (savedUnitName.isNotEmpty()) {
                try {
                    valueOf(savedUnitName)
                } catch (e: IllegalArgumentException) {
                    MMOL_L
                }
            } else {
                MMOL_L
            }
        }

        fun hasPreferredUnit(): Boolean = SpUtils.contain(KEY_PREFERRED_UNIT)
        fun clearPreferredUnit() { SpUtils.remove(KEY_PREFERRED_UNIT) }
    }

    fun convertFromMgdl(mgdlValue: Double): Double {
        return when (this) {
            MG_DL -> mgdlValue
            MMOL_L -> mgdlValue / CONVERSION_FACTOR
        }
    }

    fun convertToMgdl(value: Double): Double {
        return when (this) {
            MG_DL -> value
            MMOL_L -> value * CONVERSION_FACTOR
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