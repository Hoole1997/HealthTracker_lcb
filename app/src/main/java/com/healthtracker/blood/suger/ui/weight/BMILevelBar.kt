package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import com.healthtracker.blood.suger.data.enums.BMICategory

/**
 * BMI 等级进度条，继承 GenericLevelBar
 */
class BMILevelBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericLevelBar<BMICategory>(context, attrs, defStyleAttr) {

    init {
        // 设置可用的 BMI 分类（全部枚举值）
        setAvailableCategories(BMICategory.values())
        // 默认分类为 NORMAL
        setCategory(BMICategory.NORMAL)
    }
}