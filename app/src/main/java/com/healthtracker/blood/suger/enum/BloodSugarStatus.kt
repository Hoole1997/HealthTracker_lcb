package com.healthtracker.blood.suger.enum

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.BloodSugarRangeManager

/**
 * 血糖测量状态枚举
 */
enum class BloodSugarStatus(
    val statusType: Int,
    val defaultMgdlRanges: BloodSugarRanges
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
        )
    );

    /**
     * 根据单位获取范围
     * 优先使用用户自定义值，否则使用默认值
     */
    fun getRangesForUnit(unit: BloodSugarUnit): BloodSugarRanges {
        val ranges = BloodSugarRangeManager.getRangesForStatus(this)
        return when (unit) {
            BloodSugarUnit.MG_DL -> ranges
            BloodSugarUnit.MMOL_L -> ranges.convertToMmolL()
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
) {
    /**
     * 转换为mmol/L单位
     */
    fun convertToMmolL(): BloodSugarRanges {
        val factor = BloodSugarUnit.CONVERSION_FACTOR
        return BloodSugarRanges(
            low = (low / factor * 10).toInt() / 10f,
            lowHigh = (lowHigh / factor * 10).toInt() / 10f,
            normalLow = (normalLow / factor * 10).toInt() / 10f,
            normalHigh = (normalHigh / factor * 10).toInt() / 10f,
            prediabetesLow = (prediabetesLow / factor * 10).toInt() / 10f,
            prediabetesHigh = (prediabetesHigh / factor * 10).toInt() / 10f,
            diabetesLow = (diabetesLow / factor * 10).toInt() / 10f
        )
    }
}

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