package com.healthtracker.blood.suger.data.enums

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.weight.LevelCategory


/**
 * 血压分类枚举
 * 基于美国心脏协会(AHA)标准
 */
enum class BloodPressureCategory(
    val code: String,
    override val colorRes: Int,
    val statusTextRes: Int?
) : LevelCategory {
    LOW("low", R.color.color_3487FC, R.string.blood_pressure_level_low),                         // 低血压: <90/60
    NORMAL("normal", R.color.color_05BA7B, R.string.blood_pressure_level_normal),                // 正常: 90-119/60-79
    ELEVATED("elevated", R.color.color_FFE902, R.string.blood_pressure_level_elevated),          // 血压偏高: 120-129/<80
    HIGH_STAGE_1("high_stage_1", R.color.color_FFB909, R.string.blood_pressure_level_high_stage_1), // 高血压1期: 130-139/80-89
    HIGH_STAGE_2("high_stage_2", R.color.color_FF8000, R.string.blood_pressure_level_high_stage_2), // 高血压2期: 140-179/90-119
    HYPERTENSIVE_CRISIS("hypertensive_crisis", R.color.color_FB0301, R.string.blood_pressure_level_hypertensive_crisis), // 高血压危象: ≥180/≥120
    UNKNOWN("unknown", R.color.color_05BA7B, null);                  // 未知

    companion object {
        /**
         * 根据收缩压和舒张压判断血压分类
         * @param systolic 收缩压
         * @param diastolic 舒张压
         * @return 血压分类
         */
        fun fromBloodPressure(systolic: Int, diastolic: Int): BloodPressureCategory {
            return when {
                systolic >= 180 || diastolic >= 120 -> HYPERTENSIVE_CRISIS
                (systolic in 140..189) || (diastolic in 90..120) -> HIGH_STAGE_2
                (systolic in 130..139) || (diastolic in 80..89) -> HIGH_STAGE_1
                (systolic in 120..129) && diastolic in 60..79-> ELEVATED
                (systolic in 90..119) && (diastolic in 60..79) -> NORMAL
                systolic < 90 || diastolic < 60 -> LOW
                else -> UNKNOWN
            }
        }
    }
}

/**
 * 脉搏/心率分类枚举
 */
enum class PulseCategory(val code: String) {
    BRADYCARDIA("bradycardia"),      // 心动过缓 < 60 bpm
    NORMAL("normal"),                // 正常 60-100 bpm
    TACHYCARDIA("tachycardia"),      // 心动过速 > 100 bpm
    UNKNOWN("unknown");              // 未知

    companion object {
        /**
         * 根据脉搏值判断分类
         * @param pulseRate 脉搏值 (次/分钟)
         * @return 脉搏分类
         */
        fun fromPulseRate(pulseRate: Int): PulseCategory {
            return when {
                pulseRate < 60 -> BRADYCARDIA
                pulseRate in 60..100 -> NORMAL
                pulseRate > 100 -> TACHYCARDIA
                else -> UNKNOWN
            }
        }
    }
}