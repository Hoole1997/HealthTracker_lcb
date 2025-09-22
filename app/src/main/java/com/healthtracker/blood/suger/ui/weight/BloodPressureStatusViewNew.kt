package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.dialog.BpLeveDialog
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 血压状态视图
 * 基于通用StatusView实现
 */
class BloodPressureStatusViewNew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericStatusView<BloodPressureCategory>(context, attrs, defStyleAttr) {

    private var systolicPressure: Int = 120
    private var diastolicPressure: Int = 80

    // 分类文本映射
    private val categoryTexts = mapOf(
        BloodPressureCategory.LOW to R.string.blood_pressure_level_low,
        BloodPressureCategory.NORMAL to R.string.blood_pressure_level_normal,
        BloodPressureCategory.ELEVATED to R.string.blood_pressure_level_elevated,
        BloodPressureCategory.HIGH_STAGE_1 to R.string.blood_pressure_level_high_stage_1,
        BloodPressureCategory.HIGH_STAGE_2 to R.string.blood_pressure_level_high_stage_2,
        BloodPressureCategory.HYPERTENSIVE_CRISIS to R.string.blood_pressure_level_hypertensive_crisis
    )

    // 范围描述映射
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

    private var bloodPressureLevelBar: BloodPressureLevelBar? = null

    override fun createLevelBar(): View {
        bloodPressureLevelBar = BloodPressureLevelBar(context)
        return bloodPressureLevelBar!!
    }

    override fun getLevelTexts(): Map<BloodPressureCategory, Int> = categoryTexts

    override fun getStatusTextRes(level: BloodPressureCategory): Int {
        return categoryTexts[level] ?: R.string.blood_pressure_level_normal
    }

    override fun getRangeText(level: BloodPressureCategory): String {
        val rangeTextRes = categoryRanges[level] ?: R.string.blood_pressure_range_normal_short
        val systolicRangeRes = systolicRanges[level] ?: R.string.bp_range_normal_sys
        val diastolicRangeRes = diastolicRanges[level] ?: R.string.bp_range_normal_dia

        val systolicRange = context.getString(systolicRangeRes)
        val diastolicRange = context.getString(diastolicRangeRes)
        return context.getString(rangeTextRes, systolicRange, diastolicRange)
    }

    override fun getDefaultLevel(): BloodPressureCategory = BloodPressureCategory.NORMAL

    override fun updateLevelBar() {
        currentLevelValue?.let { category ->
            bloodPressureLevelBar?.setCategory(category)
        }
    }

    /**
     * 更新血压数据
     * @param systolic 收缩压
     * @param diastolic 舒张压
     */
    fun updateBloodPressure(systolic: Int, diastolic: Int) {
        this.systolicPressure = systolic
        this.diastolicPressure = diastolic
        val category = BloodPressureCategory.fromBloodPressure(systolic, diastolic)
        setLevel(category)
    }

    /**
     * 更新舒张压
     */
    fun updateDiastolic(diastolic: Int) {
        this.diastolicPressure = diastolic
        updateStatus()
    }

    /**
     * 更新收缩压
     */
    fun updateSystolic(systolic: Int) {
        this.systolicPressure = systolic
        updateStatus()
    }

    private fun updateStatus() {
        val category = BloodPressureCategory.fromBloodPressure(this.systolicPressure, this.diastolicPressure)
        setLevel(category)
    }
}