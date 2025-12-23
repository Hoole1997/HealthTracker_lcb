package com.daily.health.manager.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.databinding.HtDialogAddTagBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click

class AddTagDialog(private val onSave: ((String) -> Unit)? = null) :
    BaseBottomSheetDialogFragment<HtDialogAddTagBinding>() {
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogAddTagBinding.inflate(layoutInflater, parent, attachToParent)


    companion object{
        fun show(fragmentManager: FragmentManager,onSave: (String) -> Unit){
            AddTagDialog(onSave).show(fragmentManager)
        }
    }

    constructor() : this(null)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            btnCancel.click {
                dismissAllowingStateLoss()
            }
            btnOk.click {
                etLabel.text.toString().trim().apply {
                    onSave?.invoke(this)
                }
                dismissAllowingStateLoss()
            }
            etLabel.requestFocus()
            switchSaveStatue(false)
            etLabel.addTextChangedListener {
                it?.let { editable ->
                    switchSaveStatue(!editable.toString().trim().isEmpty())
                } ?: kotlin.run {
                    switchSaveStatue(false)
                }
            }


        }
    }

    private fun switchSaveStatue(enable: Boolean) {
        mViewBind?.apply {
            btnOk.isEnabled = enable
            btnOk.alpha = if (enable) 1.0f else 0.3f
        }
    }
}