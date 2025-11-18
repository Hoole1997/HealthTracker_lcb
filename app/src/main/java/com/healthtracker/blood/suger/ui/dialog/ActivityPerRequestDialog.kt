package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.DialogComingSoonBinding
import com.healthtracker.blood.suger.databinding.DialogPhysicalPermissionRequestBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.click

/**
 * ComingSoon 弹窗
 * 当用户选择今天之后的日期时提示，并提供返回到今天的按钮。
 */
class ActivityPerRequestDialog(
    private val onAllow: (() -> Unit)? = null
) : BaseBottomSheetDialogFragment<DialogPhysicalPermissionRequestBinding>() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            onAllow: (() -> Unit)? = null
        ) {
            ActivityPerRequestDialog(onAllow).show(fragmentManager)
        }
    }

    constructor() : this(null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogPhysicalPermissionRequestBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            btnAllow.click {
                onAllow?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
}