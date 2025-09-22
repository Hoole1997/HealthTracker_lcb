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
) : GenericLevelBar<BloodPressureCategory>(context, attrs, defStyleAttr) {

    init {
        // 设置所有血压等级（排除UNKNOWN）
        setAvailableCategories(BloodPressureCategory.entries.filter { it != BloodPressureCategory.UNKNOWN }.toTypedArray())
        // 默认设置为正常
        setCategory(BloodPressureCategory.NORMAL)
    }

    /**
     * 获取当前血压分类（保持原有API兼容性）
     */
    override fun getCurrentCategory(): BloodPressureCategory = super.getCurrentCategory() ?: BloodPressureCategory.NORMAL
}