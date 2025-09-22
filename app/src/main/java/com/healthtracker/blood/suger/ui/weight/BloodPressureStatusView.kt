package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.databinding.BloodPressureStatusViewBinding
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.dialog.BpLeveDialog
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 血压状态显示自定义View
 * 显示血压分类状态、范围描述和可视化指示条
 * 使用ViewBinding和布局文件实现
 */
class BloodPressureStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    // 初始化ViewBinding
    private val binding: BloodPressureStatusViewBinding =
        BloodPressureStatusViewBinding.inflate(LayoutInflater.from(context), this, true)

    // 当前血压状态
    private var currentCategory: BloodPressureCategory = BloodPressureCategory.NORMAL
    private var systolicPressure: Int = 120
    private var diastolicPressure: Int = 80

    // 颜色映射
    private val categoryColors = mapOf(
        BloodPressureCategory.LOW to R.color.color_3487FC,
        BloodPressureCategory.NORMAL to R.color.color_05BA7B,
        BloodPressureCategory.ELEVATED to R.color.color_FFE902,
        BloodPressureCategory.HIGH_STAGE_1 to R.color.color_FFB909,
        BloodPressureCategory.HIGH_STAGE_2 to R.color.color_FF8000,
        BloodPressureCategory.HYPERTENSIVE_CRISIS to R.color.color_FB0301
    )

    // 分类文本映射
    private val categoryTexts = mapOf(
        BloodPressureCategory.LOW to R.string.blood_pressure_level_low,
        BloodPressureCategory.NORMAL to R.string.blood_pressure_level_normal,
        BloodPressureCategory.ELEVATED to R.string.blood_pressure_level_elevated,
        BloodPressureCategory.HIGH_STAGE_1 to R.string.blood_pressure_level_high_stage_1,
        BloodPressureCategory.HIGH_STAGE_2 to R.string.blood_pressure_level_high_stage_2,
        BloodPressureCategory.HYPERTENSIVE_CRISIS to R.string.blood_pressure_level_hypertensive_crisis
    )

    // 范围描述映射（状态显示用）
    private val categoryRanges = mapOf(
        BloodPressureCategory.LOW to R.string.blood_pressure_range_low_short,
        BloodPressureCategory.NORMAL to R.string.blood_pressure_range_normal_short,
        BloodPressureCategory.ELEVATED to R.string.blood_pressure_range_elevated_short,
        BloodPressureCategory.HIGH_STAGE_1 to R.string.blood_pressure_range_high_stage_1_short,
        BloodPressureCategory.HIGH_STAGE_2 to R.string.blood_pressure_range_high_stage_2_short,
        BloodPressureCategory.HYPERTENSIVE_CRISIS to R.string.blood_pressure_range_hypertensive_crisis_short
    )

    // 收缩压范围映射
    private val systolicRanges = mapOf(
        BloodPressureCategory.LOW to R.string.bp_range_low_sys,
        BloodPressureCategory.NORMAL to R.string.bp_range_normal_sys,
        BloodPressureCategory.ELEVATED to R.string.bp_range_elevated_sys,
        BloodPressureCategory.HIGH_STAGE_1 to R.string.bp_range_high_stage_1_sys,
        BloodPressureCategory.HIGH_STAGE_2 to R.string.bp_range_high_stage_2_sys,
        BloodPressureCategory.HYPERTENSIVE_CRISIS to R.string.bp_range_hypertensive_crisis_sys
    )

    // 舒张压范围映射
    private val diastolicRanges = mapOf(
        BloodPressureCategory.LOW to R.string.bp_range_low_dia,
        BloodPressureCategory.NORMAL to R.string.bp_range_normal_dia,
        BloodPressureCategory.ELEVATED to R.string.bp_range_elevated_dia,
        BloodPressureCategory.HIGH_STAGE_1 to R.string.bp_range_high_stage_1_dia,
        BloodPressureCategory.HIGH_STAGE_2 to R.string.bp_range_high_stage_2_dia,
        BloodPressureCategory.HYPERTENSIVE_CRISIS to R.string.bp_range_hypertensive_crisis_dia
    )

    init {

        // 初始化UI
        updateUI()
    }

    /**
     * 更新UI显示
     */
    private fun updateUI() {
        updateStatusDot()
        updateStatusText()
        updateRangeText()
        updateLevelBar()
    }

    /**
     * 更新状态圆点
     */
    private fun updateStatusDot() {
        val color = categoryColors[currentCategory] ?: R.color.color_05BA7B
        val drawable = binding.statusDot.background as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(context, color))
    }

    /**
     * 更新状态文本
     */
    private fun updateStatusText() {
        val statusTextRes = categoryTexts[currentCategory] ?: R.string.blood_pressure_level_normal
        val statusText = context.getString(statusTextRes)
        val color = categoryColors[currentCategory] ?: R.color.color_05BA7B
        
        binding.statusText.text = statusText
        binding.statusText.setTextColor(ContextCompat.getColor(context, color))
    }

    /**
     * 更新范围描述文本
     */
    private fun updateRangeText() {
        val rangeTextRes = categoryRanges[currentCategory] ?: R.string.blood_pressure_range_normal_short
        val systolicRangeRes = systolicRanges[currentCategory] ?: R.string.bp_range_normal_sys
        val diastolicRangeRes = diastolicRanges[currentCategory] ?: R.string.bp_range_normal_dia
        
        val systolicRange = context.getString(systolicRangeRes)
        val diastolicRange = context.getString(diastolicRangeRes)
        val rangeText = context.getString(rangeTextRes, systolicRange, diastolicRange)
        
        binding.rangeText.text = rangeText
        binding.rangeText.clickWithDuration {
            BpLeveDialog.show((context as BpRecordActivity).supportFragmentManager)
        }
    }

    /**
     * 更新等级进度条
     */
    private fun updateLevelBar() {
        binding.levelBar.setCategory(currentCategory)
    }

    /**
     * 更新血压数据
     * @param systolic 收缩压
     * @param diastolic 舒张压
     */
    fun updateDiastolic(diastolic: Int) {

        this.diastolicPressure = diastolic
        updateStatus()

    }

    fun updateSystolic(systolic: Int){
        this.systolicPressure = systolic
        updateStatus()
    }

    private fun updateStatus(){
        this.currentCategory = BloodPressureCategory.fromBloodPressure(this.systolicPressure, this.diastolicPressure)
        updateUI()
    }

    /**
     * 获取当前血压分类
     */
    fun getCurrentCategory(): BloodPressureCategory = currentCategory

    /**
     * 设置血压分类
     * @param category 血压分类
     */
    fun setCategory(category: BloodPressureCategory) {
        this.currentCategory = category
        updateUI()
    }
}