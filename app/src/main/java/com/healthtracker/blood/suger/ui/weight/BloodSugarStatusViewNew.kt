package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.enum.BloodSugarLevel
import com.healthtracker.blood.suger.enum.BloodSugarStatus

/**
 * 血糖状态视图
 * 基于通用StatusView实现
 */
class BloodSugarStatusViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericStatusView<BloodSugarLevel>(context, attrs, defStyleAttr) {

    private var currentStatus: BloodSugarStatus = BloodSugarStatus.DEFAULT
    private var currentUnit: BsUnit = BsUnit.MMOL_L

    // 等级文本映射
    private val levelTexts = mapOf(
        BloodSugarLevel.LOW to R.string.blood_sugar_level_low,
        BloodSugarLevel.NORMAL to R.string.blood_sugar_level_normal,
        BloodSugarLevel.PREDIABETES to R.string.blood_sugar_level_prediabetes,
        BloodSugarLevel.DIABETES to R.string.blood_sugar_level_diabetes
    )

    private var bloodSugarLevelBar: BloodSugarLevelBar? = null

    override fun createLevelBar(): View {
        bloodSugarLevelBar = context.createBloodSugarLevelBar()
        return bloodSugarLevelBar!!
    }

    override fun getLevelTexts(): Map<BloodSugarLevel, Int> = levelTexts

    override fun getStatusTextRes(level: BloodSugarLevel): Int {
        return levelTexts[level] ?: R.string.blood_sugar_level_normal
    }

    override fun getRangeText(level: BloodSugarLevel): String {
        val ranges = currentStatus.getRangesForUnit(currentUnit)
        return when (level) {
            BloodSugarLevel.LOW -> "< ${BsUnit.formatValue(ranges.lowHigh, currentUnit)}"
            BloodSugarLevel.NORMAL -> "${BsUnit.formatValue(ranges.normalLow, currentUnit)}~${BsUnit.formatValue(ranges.normalHigh, currentUnit)}"
            BloodSugarLevel.PREDIABETES -> "${BsUnit.formatValue(ranges.prediabetesLow, currentUnit)}~${BsUnit.formatValue(ranges.prediabetesHigh, currentUnit)}"
            BloodSugarLevel.DIABETES -> "≥ ${BsUnit.formatValue(ranges.diabetesLow, currentUnit)}"
        }
    }

    override fun getDefaultLevel(): BloodSugarLevel = BloodSugarLevel.NORMAL

    override fun updateLevelBar() {
        currentLevelValue?.let { level ->
            bloodSugarLevelBar?.setCategory(level)
        }
    }

    /**
     * 更新血糖状态显示
     * @param value 血糖值
     * @param unit 单位
     * @param status 血糖状态
     */
    fun updateBloodSugarStatus(value: Float, unit: BsUnit, status: BloodSugarStatus) {
        // 根据血糖值和状态计算等级
        val level = status.getBloodSugarLevel(value, unit)

        this.currentStatus = status
        this.currentUnit = unit
        setLevel(level)
    }
}