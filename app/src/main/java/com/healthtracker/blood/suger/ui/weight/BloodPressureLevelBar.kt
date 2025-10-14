package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory

/**
 * 血压等级进度条自定义控件（兼容性包装）
 * 基于泛型GenericLevelBar实现，保持API兼容性
 */
class BloodPressureLevelBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericLevelBar(context, attrs, defStyleAttr) {

    init {
        // 使用颜色资源数组（排除 UNKNOWN）并设置默认索引为 NORMAL
        val filtered = BloodPressureCategory.entries.filter { it != BloodPressureCategory.UNKNOWN }
        setColorResArray(filtered.map { it.colorRes }.toIntArray())
        val normalIndex = filtered.indexOf(BloodPressureCategory.NORMAL).coerceAtLeast(0)
        setIndicatorIndex(normalIndex)
    }
}