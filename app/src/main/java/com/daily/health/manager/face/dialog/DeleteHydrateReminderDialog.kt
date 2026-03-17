package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daily.health.manager.R
import com.daily.health.manager.databinding.FcDialogDeleteConfirmBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click


class DeleteHydrateReminderDialog(
    private val message: String,
    private val onDialogListener: DialogListener?,
    private val leftText: String? = null, private val rightText: String? = null
) : BaseBottomSheetDialogFragment<FcDialogDeleteConfirmBinding>(onDialogListener){

    companion object {
        val BUTTON_OK = R.id.btn_ok
        val BUTTON_CANCEL = R.id.btn_cancel
    }

    // 恢复用，onCreate 时dismiss()
    constructor() : this("", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 恢复时直接关闭
        if (savedInstanceState != null || onDialogListener == null) {
            dismissAllowingStateLoss()
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FcDialogDeleteConfirmBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            tvConfirmMessage.text = message

            leftText?.let {
                btnCancel.text = it
            }
            rightText?.let {
                btnOk.text = it
            }

            btnOk.click {
                onDialogListener?.onItemClick(this@DeleteHydrateReminderDialog, BUTTON_OK)
                dismissAllowingStateLoss()
            }
            btnCancel.click {
                onDialogListener?.onItemClick(this@DeleteHydrateReminderDialog, BUTTON_CANCEL)
                dismissAllowingStateLoss()
            }
        }
    }

    // 使用 BaseBottomSheetDialogFragment 的底部弹出与拖拽行为，无需手动设置窗口参数


}