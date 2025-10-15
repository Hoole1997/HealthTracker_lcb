package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.databinding.DialogChooseAgeBinding
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.saveUserAge
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.util.FontUtils

/**
 * 年龄选择弹窗
 */
class AgeChooseDialog : BaseVbDialogFragment<DialogChooseAgeBinding>() {

    private var onAgeConfirmed: (() -> Unit)? = null
    private var selectedAge: Int = getUserAge()

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            onConfirmed: (() -> Unit)? = null
        ) {
            AgeChooseDialog().apply {
                this.onAgeConfirmed = onConfirmed
            }.show(fragmentManager)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): DialogChooseAgeBinding = DialogChooseAgeBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        setupNumberPicker()
        mViewBind?.apply {
            btnCancel.clickWithDuration { dismissAllowingStateLoss() }
            btnOk.clickWithDuration {
                saveUserAge(selectedAge)
                onAgeConfirmed?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    private fun setupNumberPicker() {
        val ages = (2..110).map { it.toString() }.toTypedArray()
        val picker = mViewBind?.numberPicker ?: return

        picker.displayedValues = ages
        picker.minValue = 0
        picker.maxValue = ages.lastIndex
        val normalizedAge = selectedAge.coerceIn(2, 110)
        val currentIndex = ages.indexOf(normalizedAge.toString())
        picker.value = if (currentIndex >= 0) currentIndex else 0
        selectedAge = if (currentIndex >= 0) normalizedAge else ages.first().toInt()

        val fontUtils = FontUtils.getInstance()
        picker.setContentSelectedTextTypeface(fontUtils.robotoBold)
        picker.setContentNormalTextTypeface(fontUtils.robotoLight)

        picker.setOnValueChangedListener { _, _, _ ->
            val value = picker.contentByCurrValue.toIntOrNull()
            if (value != null) {
                selectedAge = value
            }
        }
    }
}
