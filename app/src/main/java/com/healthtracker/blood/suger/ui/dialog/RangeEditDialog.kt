package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.databinding.DialogRangeEditBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration

class RangeEditDialog: BaseBottomSheetDialogFragment<DialogRangeEditBinding>() {
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogRangeEditBinding.inflate(inflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            setupUnitSwitcher()
            btnCancel.click {
                dismissAllowingStateLoss()
            }

            btnSave.clickWithDuration {
                //获取当前值和原始值对比，有变化则回调给Activity，没变化则直接关闭对话框
                dismissAllowingStateLoss()
            }

        }
    }


    private fun setupUnitSwitcher() {
       mViewBind?.apply {
           rgUnit.setOnCheckedChangeListener { _, checkedId ->
               val newUnit = when (checkedId) {
                   rbMgdl.id -> BsUnit.MG_DL
                   rbMmol.id -> BsUnit.MMOL_L
                   else -> return@setOnCheckedChangeListener
               }

           }
       }
    }
}