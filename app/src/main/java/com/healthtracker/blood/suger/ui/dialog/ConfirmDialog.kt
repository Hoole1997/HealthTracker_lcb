package com.mercury.docreader.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.frame.arch.base.fragment.BaseVbDialogFragment
import com.frame.arch.base.fragment.DialogListener
import com.mercury.docreader.R
import com.mercury.docreader.databinding.DlgConfirmBinding
import com.mercury.docreader.ui.util.click

class ConfirmDlg(
    private val title: String,
    private val message: String,
    private val onDialogListener: DialogListener?,
    private val leftText: String? = null, private val rightText: String? = null,
    private val titleColor: Int = 0
) : BaseVbDialogFragment<DlgConfirmBinding>(){

    companion object {
        val BUTTON_OK = R.id.btn_ok
        val BUTTON_CANCEL = R.id.btn_cancel
    }

    // 恢复用，onCreate 时dismiss()
    constructor() : this("", "", null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 恢复时直接关闭
        if (savedInstanceState != null || onDialogListener == null) {
            dismiss()
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DlgConfirmBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            tvConfirmTitle.text = title
            tvConfirmMessage.text = message

            if (titleColor != 0) {
                tvConfirmTitle.setTextColor(titleColor)
            }
            leftText?.let {
                btnCancel.text = it
            }
            rightText?.let {
                btnOk.text = it
            }

            btnOk.click {
                onDialogListener?.onItemClick(this@ConfirmDlg, BUTTON_OK)
            }
            btnCancel.click {
                onDialogListener?.onItemClick(this@ConfirmDlg, BUTTON_CANCEL)
            }
        }
    }
}