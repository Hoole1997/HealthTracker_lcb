package com.healthtracker.blood.suger.enum

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.BloodSugarRangeManager
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.ui.weight.LevelCategory

/**
 * 血糖测量状态枚举
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 5.5f,    // 5.5 mmol/L
            prediabetesLow = 5.5f,  // 5.5 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 5.5f,    // 5.5 mmol/L
            prediabetesLow = 5.5f,  // 5.5 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 5.5f,    // 5.5 mmol/L
            prediabetesLow = 5.5f,  // 5.5 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 5.5f,    // 5.5 mmol/L
            prediabetesLow = 5.5f,  // 5.5 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 5.5f,    // 5.5 mmol/L
            prediabetesLow = 5.5f,  // 5.5 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 7.8f,    // 7.8 mmol/L
            prediabetesLow = 7.8f,  // 7.8 mmol/L
            prediabetesHigh = 8.5f, // 8.5 mmol/L
            diabetesLow = 8.5f    // ≥ 8.5 mmol/L
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
            lowHigh = 5.3f,       // < 5.3 mmol/L
            normalLow = 5.3f,     // 5.3 mmol/L
            normalHigh = 5.5f,    // 5.5 mmol/L
            prediabetesLow = 5.5f,  // 5.5 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
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
            lowHigh = 4.0f,       // < 4.0 mmol/L
            normalLow = 4.0f,     // 4.0 mmol/L
            normalHigh = 4.7f,    // 4.7 mmol/L
            prediabetesLow = 4.7f,  // 4.7 mmol/L
            prediabetesHigh = 7.0f, // 7.0 mmol/L
            diabetesLow = 7.0f    // ≥ 7.0 mmol/L
        )
    );



    /**
     * 根据单位获取范围
     * 优先使用用户自定义值，否则使用默认值
     */
    fun getRangesForUnit(unit: BsUnit): BloodSugarRanges {
        // 检查是否有用户自定义范围
        val customRanges = BloodSugarRangeManager.getCustomRangesForStatus(this, unit)
        if (customRanges != null) {
            return customRanges
        }

        // 使用默认值：直接根据单位返回对应的边界值
        return when (unit) {
            BsUnit.MG_DL -> defaultMgdlRanges
            BsUnit.MMOL_L -> defaultMmolRanges
        }
    }

    /**
     * 判断血糖值属于哪个等级
     */
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
        /**
         * 根据状态类型获取状态
         */
        fun fromStatusType(statusType: Int): BloodSugarStatus {
            return entries.find { it.statusType == statusType } ?: DEFAULT
        }
    }
}

/**
 * 血糖范围数据类
 */
data class BloodSugarRanges(
    val low: Float,
    val lowHigh: Float,
    val normalLow: Float,
    val normalHigh: Float,
    val prediabetesLow: Float,
    val prediabetesHigh: Float,
    val diabetesLow: Float
)

/**
 * 血糖等级枚举
 */
enum class BloodSugarLevel(
    val level: Int,
    override val colorRes: Int,
    override val position: Float
) : LevelCategory {
    LOW(0, R.color.color_low, 0.15f),
    NORMAL(1, R.color.color_normal, 0.35f),
    PREDIABETES(2, R.color.color_prediabetes, 0.65f),
    DIABETES(3, R.color.color_diabetes, 0.85f)
}


fun getStatusStringRes(statusType: Int): Int {
    return when (statusType) {
        0 -> R.string.blood_sugar_status_default
        1 -> R.string.blood_sugar_status_fasting
        2 -> R.string.blood_sugar_status_before_meal
        3 -> R.string.blood_sugar_status_bedtime
        4 -> R.string.blood_sugar_status_after_exercise
        5 -> R.string.blood_sugar_status_one_hour_after_meal
        6 -> R.string.blood_sugar_status_before_exercise
        7 -> R.string.blood_sugar_status_two_hours_after_meal
        else -> R.string.blood_sugar_status_default
    }
}