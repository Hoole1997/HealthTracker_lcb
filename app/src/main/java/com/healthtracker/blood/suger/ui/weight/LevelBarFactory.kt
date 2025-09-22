package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import com.healthtracker.blood.suger.enum.BloodSugarLevel

/**
 * 血糖等级进度条类型别名
 */
typealias BloodSugarLevelBar = GenericLevelBar<BloodSugarLevel>

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
        return GenericLevelBar<BloodSugarLevel>(context, attrs, defStyleAttr).apply {
            // 设置所有血糖等级
            setAvailableCategories(BloodSugarLevel.values())
            // 默认设置为正常
            setCategory(BloodSugarLevel.NORMAL)
        }
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