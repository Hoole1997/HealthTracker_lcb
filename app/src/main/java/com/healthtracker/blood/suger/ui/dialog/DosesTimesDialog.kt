package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.healthtracker.blood.suger.databinding.DialogDosesTimesBinding
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.util.getRobotoBold

class DosesTimesDialog(private val def: Int = 3,private val callBack: ((Int) -> Unit)? = null) :
    BaseVbDialogFragment<DialogDosesTimesBinding>() {

    constructor() : this(3,null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogDosesTimesBinding.inflate(layoutInflater)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            btnCancel.click {
                dismissAllowingStateLoss()
            }

            btnOk.click {
                callBack?.invoke(numberPicker.contentByCurrValue.toInt())
                dismissAllowingStateLoss()
            }
            val font = getRobotoBold(view.context)
            numberPicker.setContentSelectedTextTypeface(font)
            numberPicker.displayedValues = Array(6) { i -> (i + 1).toString() }
            numberPicker.minValue = 0
            numberPicker.maxValue = 5
            numberPicker.value = numberPicker.displayedValues.indexOf(def.toString())

        }
    }
}