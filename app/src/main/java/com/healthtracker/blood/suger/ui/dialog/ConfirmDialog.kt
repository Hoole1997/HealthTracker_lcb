package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.DialogConfirmBinding
import com.healthtracker.blood.suger.databinding.DialogDeleteConfirmBinding
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import net.corekit.monetize.ui.NativeAdStyle


class ConfirmDialog(
    private val title: String,
    private val message: String,
    private val onDialogListener: DialogListener?,
    private val leftText: String? = null, private val rightText: String? = null,
    private val titleColor: Int = 0,
    private val isShowNative: Boolean = false
) : BaseVbDialogFragment<DialogConfirmBinding>(){

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
            dismissAllowingStateLoss()
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogConfirmBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            btnOk.text = title
            tvConfirmMessage.text = message
            tvConfirmTitle.text = title

            if (titleColor != 0) {
                btnOk.setTextColor(titleColor)
            }
            leftText?.let {
                btnCancel.text = it
            }
            rightText?.let {
                btnOk.text = it
            }

            btnOk.click {
                onDialogListener?.onItemClick(this@ConfirmDialog, BUTTON_OK)
                dismissAllowingStateLoss()
            }
            btnCancel.click {
                onDialogListener?.onItemClick(this@ConfirmDialog, BUTTON_CANCEL)
                dismissAllowingStateLoss()
            }

            if(isShowNative){
                requireActivity().loadNative(adContainer, style = NativeAdStyle.CARD_5)
            }
        }
    }


}