package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BMIEnum
import com.healthtracker.blood.suger.databinding.DialogBmiLeveBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration

/**
 * BMI 等级说明弹窗
 * 参考 BpLeveDialog 实现，展示 8 档 BMI 等级与范围
 */
class BmiLeveDialog : BaseBottomSheetDialogFragment<DialogBmiLeveBinding>() {

    companion object {
        fun show(fragmentManager: FragmentManager) {
            BmiLeveDialog().show(fragmentManager)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogBmiLeveBinding.inflate(layoutInflater)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            val names = resources.getStringArray(R.array.bmi_level_names)
            val ranges = resources.getStringArray(R.array.bmi_level_ranges)

            // VERY_SEVERELY_UNDERWEIGHT
            itemVerySeverelyUnderweight.tvLeve.text = names[BMIEnum.VERY_SEVERELY_UNDERWEIGHT.ordinal]
            itemVerySeverelyUnderweight.tvValueRange.text = ranges[BMIEnum.VERY_SEVERELY_UNDERWEIGHT.ordinal]
            itemVerySeverelyUnderweight.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.VERY_SEVERELY_UNDERWEIGHT.colorRes)

            // SEVERELY_UNDERWEIGHT
            itemSeverelyUnderweight.tvLeve.text = names[BMIEnum.SEVERELY_UNDERWEIGHT.ordinal]
            itemSeverelyUnderweight.tvValueRange.text = ranges[BMIEnum.SEVERELY_UNDERWEIGHT.ordinal]
            itemSeverelyUnderweight.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.SEVERELY_UNDERWEIGHT.colorRes)

            // UNDERWEIGHT
            itemUnderweight.tvLeve.text = names[BMIEnum.UNDERWEIGHT.ordinal]
            itemUnderweight.tvValueRange.text = ranges[BMIEnum.UNDERWEIGHT.ordinal]
            itemUnderweight.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.UNDERWEIGHT.colorRes)

            // NORMAL
            itemNormal.tvLeve.text = names[BMIEnum.NORMAL.ordinal]
            itemNormal.tvValueRange.text = ranges[BMIEnum.NORMAL.ordinal]
            itemNormal.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.NORMAL.colorRes)

            // OVERWEIGHT
            itemOverweight.tvLeve.text = names[BMIEnum.OVERWEIGHT.ordinal]
            itemOverweight.tvValueRange.text = ranges[BMIEnum.OVERWEIGHT.ordinal]
            itemOverweight.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.OVERWEIGHT.colorRes)

            // OBESITY_CLASS_I
            itemObesityClass1.tvLeve.text = names[BMIEnum.OBESITY_CLASS_I.ordinal]
            itemObesityClass1.tvValueRange.text = ranges[BMIEnum.OBESITY_CLASS_I.ordinal]
            itemObesityClass1.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.OBESITY_CLASS_I.colorRes)

            // OBESITY_CLASS_II
            itemObesityClass2.tvLeve.text = names[BMIEnum.OBESITY_CLASS_II.ordinal]
            itemObesityClass2.tvValueRange.text = ranges[BMIEnum.OBESITY_CLASS_II.ordinal]
            itemObesityClass2.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.OBESITY_CLASS_II.colorRes)

            // OBESITY_CLASS_III
            itemObesityClass3.tvLeve.text = names[BMIEnum.OBESITY_CLASS_III.ordinal]
            itemObesityClass3.tvValueRange.text = ranges[BMIEnum.OBESITY_CLASS_III.ordinal]
            itemObesityClass3.vFlag.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), BMIEnum.OBESITY_CLASS_III.colorRes)

            btnSave.clickWithDuration {
                dismissAllowingStateLoss()
            }
        }
    }
}