package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.DialogBloodPressureLeveBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration

class BpLeveDialog : BaseBottomSheetDialogFragment<DialogBloodPressureLeveBinding>() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            BpLeveDialog().show(fragmentManager)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogBloodPressureLeveBinding.inflate(layoutInflater)


    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            // 低血压 (hypotension) - 蓝色
            itemLow.tvLeve.text = getString(R.string.blood_pressure_level_low)
            itemLow.tvValueRange.text = getString(
                R.string.blood_pressure_range_low_full,
                getString(R.string.bp_range_low_sys),
                getString(R.string.bp_range_low_dia)
            )
            itemLow.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(itemLow.vFlag.context, R.color.color_3487FC)

            // 正常血压 (Normal) - 绿色
            itemNormal.tvLeve.text = getString(R.string.blood_pressure_level_normal)
            itemNormal.tvValueRange.text = getString(
                R.string.blood_pressure_range_normal_full,
                getString(R.string.bp_range_normal_sys),
                getString(R.string.bp_range_normal_dia)
            )
            itemNormal.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(itemLow.vFlag.context, R.color.color_05BA7B)

            // 血压升高 (Elevated) - 黄色
            itemElevated.tvLeve.text = getString(R.string.blood_pressure_level_elevated)
            itemElevated.tvValueRange.text = getString(
                R.string.blood_pressure_range_elevated_full,
                getString(R.string.bp_range_elevated_sys),
                getString(R.string.bp_range_elevated_dia)
            )
            itemElevated.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(itemLow.vFlag.context, R.color.color_FFE902)

            // 高血压1期 (Hypertension Stage 1) - 橙色
            itemStage1.tvLeve.text = getString(R.string.blood_pressure_level_high_stage_1)
            itemStage1.tvValueRange.text = getString(
                R.string.blood_pressure_range_high_stage_1_full,
                getString(R.string.bp_range_high_stage_1_sys),
                getString(R.string.bp_range_high_stage_1_dia)
            )
            itemStage1.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(itemLow.vFlag.context, R.color.color_FFB909)

            // 高血压2期 (Hypertension Stage 2) - 深橙色
            itemStage2.tvLeve.text = getString(R.string.blood_pressure_level_high_stage_2)
            itemStage2.tvValueRange.text = getString(
                R.string.blood_pressure_range_high_stage_2_full,
                getString(R.string.bp_range_high_stage_2_sys),
                getString(R.string.bp_range_high_stage_2_dia)
            )
            itemStage2.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(itemLow.vFlag.context, R.color.color_FF8000)

            // 高血压危象 (Hypertensive Crisis) - 红色
            itemStageHypertensiveCrisis.tvLeve.text =
                getString(R.string.blood_pressure_level_hypertensive_crisis)
            itemStageHypertensiveCrisis.tvValueRange.text = getString(
                R.string.blood_pressure_range_hypertensive_crisis_full,
                getString(R.string.bp_range_hypertensive_crisis_sys),
                getString(R.string.bp_range_hypertensive_crisis_dia)
            )
            itemStageHypertensiveCrisis.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(itemLow.vFlag.context, R.color.color_FB0301)

            btnSave.clickWithDuration {
                dismissAllowingStateLoss()
            }
        }
    }
}