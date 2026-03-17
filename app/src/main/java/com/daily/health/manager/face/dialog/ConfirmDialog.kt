package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daily.health.manager.R
import com.daily.health.manager.databinding.FcDialogConfirmBinding
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.gone
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle


class ConfirmDialog(
    private val title: String,
    private val message: String,
    private val onDialogListener: DialogListener?,
    private val leftText: String? = null, private val rightText: String? = null,
    private val titleColor: Int = 0,
    private val isShowNative: Boolean = false
) : BaseVbDialogFragment<FcDialogConfirmBinding>(){

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
    ) = FcDialogConfirmBinding.inflate(inflater, parent, attachToParent)

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
                requireActivity().loadNative(adContainer, AdPosition.NA_DETAIL_CONFIRM_DIALOG, style = NativeAdStyle.CARD_5)
            }else{
                adContainer.gone()
            }
        }
    }


}