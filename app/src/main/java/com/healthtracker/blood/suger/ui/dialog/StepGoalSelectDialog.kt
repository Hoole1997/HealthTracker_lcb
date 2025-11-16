package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.healthtracker.blood.suger.constants.KEY_STEP_COUNT_GOLE
import com.healthtracker.blood.suger.databinding.DialogDosesTimesBinding
import com.healthtracker.blood.suger.databinding.DialogSelectStepGoalBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.getRobotoBold

class StepGoalSelectDialog(private val callBack: ((Int) -> Unit)? = null) :
    BaseBottomSheetDialogFragment<DialogSelectStepGoalBinding>() {

    constructor() : this(null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogSelectStepGoalBinding.inflate(layoutInflater)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            btnSave.click {
                callBack?.invoke(numberPicker.contentByCurrValue.toInt())
                dismissAllowingStateLoss()
            }
            val font = getRobotoBold(view.context)
            numberPicker.setContentSelectedTextTypeface(font)
            numberPicker.displayedValues = Array(80) { i -> ((i + 1) * 500).toString() }
            numberPicker.minValue = 0
            numberPicker.maxValue = 79
            numberPicker.value = numberPicker.displayedValues.indexOf(SpUtils.getInt(
                KEY_STEP_COUNT_GOLE,6000
            ).toString())

        }
    }
}