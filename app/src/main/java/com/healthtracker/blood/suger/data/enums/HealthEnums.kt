package com.healthtracker.blood.suger.data.enums


/**
 * 血压分类枚举
 * 基于美国心脏协会(AHA)标准
 */
enum class BloodPressureCategory(val code: String) {
    NORMAL("normal"),                    // 正常: <120/80
    ELEVATED("elevated"),                // 血压偏高: 120-129/<80
    HIGH_STAGE_1("high_stage_1"),       // 高血压1期: 130-139/80-89
    HIGH_STAGE_2("high_stage_2"),       // 高血压2期: ≥140/≥90
    HYPERTENSIVE_CRISIS("hypertensive_crisis"), // 高血压危象: >180/>120
    UNKNOWN("unknown");                  // 未知

    companion object {
        /**
         * 根据收缩压和舒张压判断血压分类
         * @param systolic 收缩压
         * @param diastolic 舒张压
         * @return 血压分类
         */
        fun fromBloodPressure(systolic: Int, diastolic: Int): BloodPressureCategory {
            return when {
                systolic > 180 || diastolic > 120 -> HYPERTENSIVE_CRISIS
                systolic >= 140 || diastolic >= 90 -> HIGH_STAGE_2
                (systolic in 130..139) || (diastolic in 80..89) -> HIGH_STAGE_1
                systolic < 120 && diastolic < 80 -> NORMAL
                systolic < 130 && diastolic < 80 -> ELEVATED
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

/**
 * 测量标签枚举
 * 标准化的测量时机标签
 */
enum class MeasurementTag(val code: String) {
    FASTING("fasting"),                  // 空腹
    BEFORE_BREAKFAST("before_breakfast"), // 早餐前
    AFTER_BREAKFAST("after_breakfast"),   // 早餐后
    BEFORE_LUNCH("before_lunch"),        // 午餐前
    AFTER_LUNCH("after_lunch"),          // 午餐后
    BEFORE_DINNER("before_dinner"),      // 晚餐前
    AFTER_DINNER("after_dinner"),        // 晚餐后
    BEDTIME("bedtime"),                  // 睡前
    MORNING("morning"),                  // 晨起
    AFTER_EXERCISE("after_exercise"),    // 运动后
    BEFORE_MEDICATION("before_medication"), // 服药前
    AFTER_MEDICATION("after_medication"),   // 服药后
    RANDOM("random"),                    // 随机
    OTHER("other");                      // 其他

    companion object {
        /**
         * 从字符串获取测量标签枚举
         * @param tagString 标签字符串
         * @return 测量标签枚举
         */
        fun fromString(tagString: String): MeasurementTag {
            return entries.find {
                it.code.equals(tagString, ignoreCase = true) ||
                tagString.contains(it.code, ignoreCase = true)
            } ?: OTHER
        }
    }
}