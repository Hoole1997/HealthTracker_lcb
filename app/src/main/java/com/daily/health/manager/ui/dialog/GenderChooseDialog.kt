package com.daily.health.manager.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtDialogChooseGenderBinding
import com.daily.health.manager.isMale
import com.daily.health.manager.saveUserGender
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 性别选择弹窗
 */
class GenderChooseDialog : BaseVbDialogFragment<HtDialogChooseGenderBinding>() {

    private var onGenderConfirmed: (() -> Unit)? = null
    private var selectedGender: Int = if (isMale()) GENDER_MALE else GENDER_FEMALE

    companion object {
        private const val GENDER_MALE = 0
        private const val GENDER_FEMALE = 1

        fun show(
            fragmentManager: FragmentManager,
            onConfirmed: (() -> Unit)? = null
        ) {
            GenderChooseDialog().apply {
                this.onGenderConfirmed = onConfirmed
            }.show(fragmentManager)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): HtDialogChooseGenderBinding = HtDialogChooseGenderBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        setupGenderGroup()
        mViewBind?.apply {
            btnCancel.clickWithDuration { dismissAllowingStateLoss() }
            btnOk.clickWithDuration {
                saveUserGender(selectedGender)
                onGenderConfirmed?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    private fun setupGenderGroup() {
        val binding = mViewBind ?: return
        binding.rgGender.setOnCheckedChangeListener { _, checkedId ->
            selectedGender = if (checkedId == binding.rbMale.id) GENDER_MALE else GENDER_FEMALE
            binding.tvMale.isChecked = selectedGender == GENDER_MALE
            binding.tvFemale.isChecked = selectedGender == GENDER_FEMALE
        }

        binding.rgGender.check(if (selectedGender == GENDER_MALE) binding.rbMale.id else binding.rbFemale.id)
        binding.tvMale.isChecked = selectedGender == GENDER_MALE
        binding.tvFemale.isChecked = selectedGender == GENDER_FEMALE

        binding.tvMale.clickWithDuration { binding.rgGender.check(binding.rbMale.id) }
        binding.tvFemale.clickWithDuration { binding.rgGender.check(binding.rbFemale.id) }
    }
}
