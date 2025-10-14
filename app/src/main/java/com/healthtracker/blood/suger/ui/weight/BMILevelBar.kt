package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import com.healthtracker.blood.suger.data.enums.BMIEnum

/**
 * BMI 等级进度条，继承 GenericLevelBar
 */
class BMILevelBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericLevelBar(context, attrs, defStyleAttr) {

    init {
        // 使用颜色资源数组与索引初始化
        setColorResArray(BMIEnum.values().map { it.colorRes }.toIntArray())
        setIndicatorIndex(BMIEnum.NORMAL.ordinal)
    }
}