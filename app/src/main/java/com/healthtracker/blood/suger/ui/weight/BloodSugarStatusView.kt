package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ViewBloodSugarStatusBinding
import com.healthtracker.blood.suger.enum.BloodSugarLevel
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit

/**
 * 血糖状态视图
 * 类似BloodPressureStatusView，显示血糖等级进度条
 */
class BloodSugarStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewBloodSugarStatusBinding =
        ViewBloodSugarStatusBinding.inflate(LayoutInflater.from(context), this, true)

    // 创建血糖等级进度条
    private var bloodSugarLevelBar: BloodSugarLevelBar = context.createBloodSugarLevelBar()

    // 当前血糖状态
    private var currentLevel: BloodSugarLevel = BloodSugarLevel.NORMAL
    private var currentStatus: BloodSugarStatus = BloodSugarStatus.DEFAULT
    private var currentUnit: BsUnit = BsUnit.MMOL_L

    // 等级文本映射
    private val levelTexts = mapOf(
        BloodSugarLevel.LOW to R.string.blood_sugar_level_low,
        BloodSugarLevel.NORMAL to R.string.blood_sugar_level_normal,
        BloodSugarLevel.PREDIABETES to R.string.blood_sugar_level_prediabetes,
        BloodSugarLevel.DIABETES to R.string.blood_sugar_level_diabetes
    )

    init {
        // 将进度条添加到布局中
        binding.levelBarContainer.addView(bloodSugarLevelBar)

        // 初始化UI
        updateUI()
    }

    /**
     * 更新UI显示
     */
    private fun updateUI() {
        updateLevelText()
        updateRangeText()
        updateLevelBar()
    }

    /**
     * 更新血糖等级文本
     */
    private fun updateLevelText() {
        val levelTextRes = levelTexts[currentLevel] ?: R.string.blood_sugar_level_normal
        val levelText = context.getString(levelTextRes)

        binding.levelText.text = levelText
        binding.levelText.setTextColor(ContextCompat.getColor(context, currentLevel.colorRes))
    }

    /**
     * 更新血糖范围文本
     */
    private fun updateRangeText() {
        val ranges = currentStatus.getRangesForUnit(currentUnit)
        val rangeText = when (currentLevel) {
            BloodSugarLevel.LOW -> "< ${BsUnit.formatValue(ranges.lowHigh, currentUnit)}"
            BloodSugarLevel.NORMAL -> "${BsUnit.formatValue(ranges.normalLow, currentUnit)}~${BsUnit.formatValue(ranges.normalHigh, currentUnit)}"
            BloodSugarLevel.PREDIABETES -> "${BsUnit.formatValue(ranges.prediabetesLow, currentUnit)}~${BsUnit.formatValue(ranges.prediabetesHigh, currentUnit)}"
            BloodSugarLevel.DIABETES -> "≥ ${BsUnit.formatValue(ranges.diabetesLow, currentUnit)}"
        }

        binding.rangeText.text = rangeText
    }

    /**
     * 更新等级进度条
     */
    private fun updateLevelBar() {
        bloodSugarLevelBar.setCategory(currentLevel)
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

        this.currentLevel = level
        this.currentStatus = status
        this.currentUnit = unit

        updateUI()
    }

    /**
     * 设置血糖等级
     * @param level 血糖等级
     */
    fun setLevel(level: BloodSugarLevel) {
        this.currentLevel = level
        updateUI()
    }

    /**
     * 获取当前血糖等级
     */
    fun getCurrentLevel(): BloodSugarLevel = currentLevel
}