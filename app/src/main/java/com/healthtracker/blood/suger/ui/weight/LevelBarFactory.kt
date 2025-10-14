package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import com.healthtracker.blood.suger.enum.BloodSugarLevel

/**
 * 血糖等级进度条类型别名
 */
typealias BloodSugarLevelBar = GenericLevelBar

/**
 * 等级进度条工厂类
 * 提供便捷的创建方法
 */
object LevelBarFactory {

    /**
     * 创建血压等级进度条
     */
    fun createBloodPressureLevelBar(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ): BloodPressureLevelBar {
        return BloodPressureLevelBar(context, attrs, defStyleAttr)
    }

    /**
     * 创建血糖等级进度条
     */
    fun createBloodSugarLevelBar(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ): BloodSugarLevelBar {
        return GenericLevelBar(context, attrs, defStyleAttr).apply {
            // 使用颜色资源数组与默认索引
            setColorResArray(BloodSugarLevel.values().map { it.colorRes }.toIntArray())
            setIndicatorIndex(BloodSugarLevel.NORMAL.ordinal)
        }
    }

    /**
     * 创建 BMI 等级进度条
     */
    fun createBmiLevelBar(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ): BMILevelBar {
        return BMILevelBar(context, attrs, defStyleAttr)
    }
}

/**
 * 扩展函数：为Context添加创建等级进度条的便捷方法
 */
fun Context.createBloodPressureLevelBar(
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): BloodPressureLevelBar {
    return LevelBarFactory.createBloodPressureLevelBar(this, attrs, defStyleAttr)
}

fun Context.createBloodSugarLevelBar(
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): BloodSugarLevelBar {
    return LevelBarFactory.createBloodSugarLevelBar(this, attrs, defStyleAttr)
}

fun Context.createBmiLevelBar(
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): BMILevelBar {
    return LevelBarFactory.createBmiLevelBar(this, attrs, defStyleAttr)
}