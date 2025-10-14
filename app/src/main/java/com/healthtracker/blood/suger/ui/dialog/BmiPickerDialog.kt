package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BmiUnit
import com.healthtracker.blood.suger.databinding.DialogBmiPickerBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment

class BmiPickerDialog(private val style: Int = STYLE_WEIGHT,private val onSave: ((Float) -> Unit)? = null) :
    BaseBottomSheetDialogFragment<DialogBmiPickerBinding>() {

    constructor() : this(STYLE_WEIGHT,null)

    companion object {

        private const val STYLE_WEIGHT = 0
        private const val STYLE_HEIGHT = 1

        fun showWeightPicker(fragmentManager: FragmentManager, onSave: (Float) -> Unit) {
            BmiPickerDialog(STYLE_WEIGHT,onSave).show(fragmentManager)

        }

        fun showHeightPicker(fragmentManager: FragmentManager, onSave: (Float) -> Unit) {
            BmiPickerDialog(STYLE_HEIGHT,onSave).show(fragmentManager)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogBmiPickerBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            when(style){
                STYLE_WEIGHT -> {
                    rb1.text = BmiUnit.IMPERIAL.weightLabel
                    rb2.text = BmiUnit.METRIC.weightLabel
                }
                else -> {
                    rb1.text = BmiUnit.IMPERIAL.heightLabel
                    rb2.text = BmiUnit.METRIC.heightLabel
                }
            }
            rgUnit.setOnCheckedChangeListener { _,checkedId ->
                val unit = when(checkedId){
                    R.id.rb_1 -> BmiUnit.IMPERIAL
                    else -> BmiUnit.METRIC
                }
            }
        }



    }
}