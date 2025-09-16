package com.healthtracker.blood.suger.data.utils

import android.content.Context
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.data.enums.GlucoseLevel
import com.healthtracker.blood.suger.data.enums.MeasurementTag
import com.healthtracker.blood.suger.data.enums.PulseCategory

/**
 * 健康数据国际化工具类
 * 提供枚举值到本地化字符串的转换
 */
object HealthLocalizationUtils {

    /**
     * 获取血糖等级的本地化显示名称
     * @param level 血糖等级枚举
     * @param context Android上下文
     * @return 本地化字符串
     */
    fun getGlucoseLevelDisplayName(level: GlucoseLevel, context: Context): String {
        return when (level) {
            GlucoseLevel.HYPOGLYCEMIA -> getStringResource(context, "glucose_level_hypoglycemia", "低血糖")
            GlucoseLevel.NORMAL -> getStringResource(context, "glucose_level_normal", "正常")
            GlucoseLevel.PREDIABETES -> getStringResource(context, "glucose_level_prediabetes", "糖尿病前期")
            GlucoseLevel.DIABETES -> getStringResource(context, "glucose_level_diabetes", "糖尿病")
            GlucoseLevel.SEVERE_HYPERGLYCEMIA -> getStringResource(context, "glucose_level_severe_hyperglycemia", "严重高血糖")
        }
    }

    /**
     * 获取血压分类的本地化显示名称
     * @param category 血压分类枚举
     * @param context Android上下文
     * @return 本地化字符串
     */
    fun getBloodPressureCategoryDisplayName(category: BloodPressureCategory, context: Context): String {
        return when (category) {
            BloodPressureCategory.NORMAL -> getStringResource(context, "bp_category_normal", "正常")
            BloodPressureCategory.ELEVATED -> getStringResource(context, "bp_category_elevated", "血压偏高")
            BloodPressureCategory.HIGH_STAGE_1 -> getStringResource(context, "bp_category_high_stage_1", "高血压1期")
            BloodPressureCategory.HIGH_STAGE_2 -> getStringResource(context, "bp_category_high_stage_2", "高血压2期")
            BloodPressureCategory.HYPERTENSIVE_CRISIS -> getStringResource(context, "bp_category_hypertensive_crisis", "高血压危象")
            BloodPressureCategory.UNKNOWN -> getStringResource(context, "bp_category_unknown", "未知")
        }
    }

    /**
     * 获取脉搏分类的本地化显示名称
     * @param category 脉搏分类枚举
     * @param context Android上下文
     * @return 本地化字符串
     */
    fun getPulseCategoryDisplayName(category: PulseCategory, context: Context): String {
        return when (category) {
            PulseCategory.BRADYCARDIA -> getStringResource(context, "pulse_category_bradycardia", "心动过缓")
            PulseCategory.NORMAL -> getStringResource(context, "pulse_category_normal", "正常")
            PulseCategory.TACHYCARDIA -> getStringResource(context, "pulse_category_tachycardia", "心动过速")
            PulseCategory.UNKNOWN -> getStringResource(context, "pulse_category_unknown", "未知")
        }
    }

    /**
     * 获取测量标签的本地化显示名称
     * @param tag 测量标签枚举
     * @param context Android上下文
     * @return 本地化字符串
     */
    fun getMeasurementTagDisplayName(tag: MeasurementTag, context: Context): String {
        return when (tag) {
            MeasurementTag.FASTING -> getStringResource(context, "measurement_tag_fasting", "空腹")
            MeasurementTag.BEFORE_BREAKFAST -> getStringResource(context, "measurement_tag_before_breakfast", "早餐前")
            MeasurementTag.AFTER_BREAKFAST -> getStringResource(context, "measurement_tag_after_breakfast", "早餐后")
            MeasurementTag.BEFORE_LUNCH -> getStringResource(context, "measurement_tag_before_lunch", "午餐前")
            MeasurementTag.AFTER_LUNCH -> getStringResource(context, "measurement_tag_after_lunch", "午餐后")
            MeasurementTag.BEFORE_DINNER -> getStringResource(context, "measurement_tag_before_dinner", "晚餐前")
            MeasurementTag.AFTER_DINNER -> getStringResource(context, "measurement_tag_after_dinner", "晚餐后")
            MeasurementTag.BEDTIME -> getStringResource(context, "measurement_tag_bedtime", "睡前")
            MeasurementTag.MORNING -> getStringResource(context, "measurement_tag_morning", "晨起")
            MeasurementTag.AFTER_EXERCISE -> getStringResource(context, "measurement_tag_after_exercise", "运动后")
            MeasurementTag.BEFORE_MEDICATION -> getStringResource(context, "measurement_tag_before_medication", "服药前")
            MeasurementTag.AFTER_MEDICATION -> getStringResource(context, "measurement_tag_after_medication", "服药后")
            MeasurementTag.RANDOM -> getStringResource(context, "measurement_tag_random", "随机")
            MeasurementTag.OTHER -> getStringResource(context, "measurement_tag_other", "其他")
        }
    }

    /**
     * 获取血糖等级的颜色资源ID（用于图表显示）
     * @param level 血糖等级枚举
     * @return 颜色资源名称字符串
     */
    fun getGlucoseLevelColorResource(level: GlucoseLevel): String {
        return when (level) {
            GlucoseLevel.HYPOGLYCEMIA -> "color_glucose_hypoglycemia"
            GlucoseLevel.NORMAL -> "color_glucose_normal"
            GlucoseLevel.PREDIABETES -> "color_glucose_prediabetes"
            GlucoseLevel.DIABETES -> "color_glucose_diabetes"
            GlucoseLevel.SEVERE_HYPERGLYCEMIA -> "color_glucose_severe_hyperglycemia"
        }
    }

    /**
     * 获取血压分类的颜色资源ID（用于图表显示）
     * @param category 血压分类枚举
     * @return 颜色资源名称字符串
     */
    fun getBloodPressureCategoryColorResource(category: BloodPressureCategory): String {
        return when (category) {
            BloodPressureCategory.NORMAL -> "color_bp_normal"
            BloodPressureCategory.ELEVATED -> "color_bp_elevated"
            BloodPressureCategory.HIGH_STAGE_1 -> "color_bp_high_stage_1"
            BloodPressureCategory.HIGH_STAGE_2 -> "color_bp_high_stage_2"
            BloodPressureCategory.HYPERTENSIVE_CRISIS -> "color_bp_hypertensive_crisis"
            BloodPressureCategory.UNKNOWN -> "color_bp_unknown"
        }
    }

    /**
     * 获取脉搏分类的颜色资源ID（用于图表显示）
     * @param category 脉搏分类枚举
     * @return 颜色资源名称字符串
     */
    fun getPulseCategoryColorResource(category: PulseCategory): String {
        return when (category) {
            PulseCategory.BRADYCARDIA -> "color_pulse_bradycardia"
            PulseCategory.NORMAL -> "color_pulse_normal"
            PulseCategory.TACHYCARDIA -> "color_pulse_tachycardia"
            PulseCategory.UNKNOWN -> "color_pulse_unknown"
        }
    }

    /**
     * 安全获取字符串资源，如果资源不存在则返回默认值
     * @param context Android上下文
     * @param resourceName 资源名称
     * @param defaultValue 默认值
     * @return 本地化字符串
     */
    private fun getStringResource(context: Context, resourceName: String, defaultValue: String): String {
        return try {
            val resourceId = context.resources.getIdentifier(resourceName, "string", context.packageName)
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                defaultValue
            }
        } catch (e: Exception) {
            defaultValue
        }
    }

    /**
     * 获取所有血糖等级的列表（用于选择器或配置界面）
     * @return 血糖等级枚举列表
     */
    fun getAllGlucoseLevels(): List<GlucoseLevel> {
        return GlucoseLevel.entries.toList()
    }

    /**
     * 获取所有血压分类的列表（用于选择器或配置界面）
     * @return 血压分类枚举列表
     */
    fun getAllBloodPressureCategories(): List<BloodPressureCategory> {
        return BloodPressureCategory.entries.toList()
    }

    /**
     * 获取所有脉搏分类的列表（用于选择器或配置界面）
     * @return 脉搏分类枚举列表
     */
    fun getAllPulseCategories(): List<PulseCategory> {
        return PulseCategory.entries.toList()
    }

    /**
     * 获取所有测量标签的列表（用于选择器或配置界面）
     * @return 测量标签枚举列表
     */
    fun getAllMeasurementTags(): List<MeasurementTag> {
        return MeasurementTag.entries.toList()
    }

    /**
     * 验证血糖等级是否需要关注
     * @param level 血糖等级
     * @return true表示需要关注（异常状态）
     */
    fun isGlucoseLevelConcerning(level: GlucoseLevel): Boolean {
        return level != GlucoseLevel.NORMAL
    }

    /**
     * 验证血压分类是否需要关注
     * @param category 血压分类
     * @return true表示需要关注（高血压或危象）
     */
    fun isBloodPressureCategoryConcerning(category: BloodPressureCategory): Boolean {
        return category in listOf(
            BloodPressureCategory.HIGH_STAGE_1,
            BloodPressureCategory.HIGH_STAGE_2,
            BloodPressureCategory.HYPERTENSIVE_CRISIS
        )
    }

    /**
     * 验证脉搏分类是否需要关注
     * @param category 脉搏分类
     * @return true表示需要关注（心动过缓或过速）
     */
    fun isPulseCategoryConcerning(category: PulseCategory): Boolean {
        return category in listOf(
            PulseCategory.BRADYCARDIA,
            PulseCategory.TACHYCARDIA
        )
    }
}