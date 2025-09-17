package com.healthtracker.blood.suger.enum

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.BloodSugarRangeManager

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
            normalHigh = 140.5f,
            prediabetesLow = 140.5f,
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
            lowHigh = 95.5f,
            normalLow = 95.5f,
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
            normalHigh = 84.7f,
            prediabetesLow = 84.7f,
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
    fun getRangesForUnit(unit: BloodSugarUnit): BloodSugarRanges {
        // 检查是否有用户自定义范围
        val customRanges = BloodSugarRangeManager.getCustomRangesForStatus(this, unit)
        if (customRanges != null) {
            return customRanges
        }

        // 使用默认值：直接根据单位返回对应的边界值
        return when (unit) {
            BloodSugarUnit.MG_DL -> defaultMgdlRanges
            BloodSugarUnit.MMOL_L -> defaultMmolRanges
        }
    }

    /**
     * 判断血糖值属于哪个等级
     */
    fun getBloodSugarLevel(value: Float, unit: BloodSugarUnit): BloodSugarLevel {
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
    val colorRes: Int
) {
    LOW(0, R.color.color_low),
    NORMAL(1, R.color.color_normal),
    PREDIABETES(2, R.color.color_prediabetes),
    DIABETES(3, R.color.color_diabetes)
}