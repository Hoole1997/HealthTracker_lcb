package com.healthtracker.blood.suger.data.enums

import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.weight.LevelCategory

/**
 * BMI 分类枚举（8 档）
 * 实现 LevelCategory 以复用通用状态视图与进度条
 */
enum class BMICategory(
    val code: String,
    override val position: Float,
    override val colorRes: Int
) : LevelCategory {
    VERY_SEVERELY_UNDERWEIGHT("very_severely_underweight", 0.06f, R.color.color_E030DD),
    SEVERELY_UNDERWEIGHT("severely_underweight", 0.15f, R.color.color_942DE2),
    UNDERWEIGHT("underweight", 0.26f, R.color.color_3487FC),
    NORMAL("normal", 0.38f, R.color.color_05BA7B),
    OVERWEIGHT("overweight", 0.50f, R.color.color_FFE902),
    OBESITY_CLASS_I("obesity_class_1", 0.66f, R.color.color_FFB909),
    OBESITY_CLASS_II("obesity_class_2", 0.82f, R.color.color_FF8000),
    OBESITY_CLASS_III("obesity_class_3", 0.94f, R.color.color_FB0301);

    companion object {
        /**
         * 根据 BMI 值返回分类
         */
        fun fromBmi(bmi: Float): BMICategory {
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