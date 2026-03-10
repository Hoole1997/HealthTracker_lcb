package com.daily.health.manager.data.enums

import com.daily.health.manager.R
import com.daily.health.manager.face.weight.LevelCategory

/**
 * BMI 分类枚举（8 档）
 * 实现 LevelCategory 以复用通用状态视图与进度条
 */
enum class BMIEnum(
    val code: String,
    override val colorRes: Int,
    val statusTextRes: Int
) : LevelCategory {
    VERY_SEVERELY_UNDERWEIGHT("very_severely_underweight", R.color.color_E030DD, R.string.tr_bmi_level_very_severely_underweight),
    SEVERELY_UNDERWEIGHT("severely_underweight", R.color.color_942DE2, R.string.tr_bmi_level_severely_underweight),
    UNDERWEIGHT("underweight", R.color.color_3487FC, R.string.tr_bmi_level_underweight),
    NORMAL("normal", R.color.color_05BA7B, R.string.tr_bmi_level_normal),
    OVERWEIGHT("overweight", R.color.color_FFE902, R.string.tr_bmi_level_overweight),
    OBESITY_CLASS_I("obesity_class_1", R.color.color_FFB909, R.string.tr_bmi_level_obesity_class_1),
    OBESITY_CLASS_II("obesity_class_2", R.color.color_FF8000, R.string.tr_bmi_level_obesity_class_2),
    OBESITY_CLASS_III("obesity_class_3", R.color.color_FB0301, R.string.tr_bmi_level_obesity_class_3);

    companion object {
        /**
         * 根据 BMI 值返回分类
         */
        fun fromBmi(bmi: Float): BMIEnum {
            return when {
                bmi < 16.0f -> VERY_SEVERELY_UNDERWEIGHT
                bmi < 17.0f -> SEVERELY_UNDERWEIGHT // 16.0 - 16.9
                bmi < 18.5f -> UNDERWEIGHT // 17.0 - 18.4
                bmi < 25.0f -> NORMAL // 18.5 - 24.9
                bmi < 30.0f -> OVERWEIGHT // 25.0 - 29.9
                bmi < 35.0f -> OBESITY_CLASS_I // 30.0 - 34.9
                bmi < 40.0f -> OBESITY_CLASS_II // 35.0 - 39.9
                else -> OBESITY_CLASS_III // >= 40.0
            }
        }
    }
}