package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.databinding.FcDialogPhysicalPermissionRequestBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click

/**
 * ComingSoon 弹窗
 * 当用户选择今天之后的日期时提示，并提供返回到今天的按钮。
 */
class ActivityPerRequestDialog(
    private val onAllow: (() -> Unit)? = null
) : BaseBottomSheetDialogFragment<FcDialogPhysicalPermissionRequestBinding>() {

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
    ) = FcDialogPhysicalPermissionRequestBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            btnAllow.click {
                onAllow?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
}