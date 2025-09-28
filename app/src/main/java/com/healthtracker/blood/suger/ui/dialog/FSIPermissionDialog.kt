package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.DialogFsiPermissionBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click

/**
 * 全屏通知(FSI)权限重要性说明对话框
 *
 * 用于在添加服药提醒时，向用户说明全屏通知权限的重要性
 * 并提供跳转到设置页面的选项
 */
class FSIPermissionDialog(
    private val onAllowPermission: (() -> Unit)? = null,
    private val onDenyPermission: (() -> Unit)? = null
) : BaseBottomSheetDialogFragment<DialogFsiPermissionBinding>() {

    companion object {
        private const val TAG = "FSIPermissionDialog"

        /**
         * 显示FSI权限说明对话框
         */
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onAllowPermission: (() -> Unit)? = null,
            onDenyPermission: (() -> Unit)? = null
        ) {
            val dialog = FSIPermissionDialog(onAllowPermission, onDenyPermission)
            dialog.show(fragmentManager, TAG)
        }
    }

    // 恢复用，onCreate 时dismiss()
    constructor() : this(null, null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 恢复时直接关闭
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogFsiPermissionBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            // 设置按钮点击事件
            btnAllowPermission.click {
                onAllowPermission?.invoke()
                dismissAllowingStateLoss()
            }

            btnDenyPermission.click {
                onDenyPermission?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }

    override fun isCancelable(): Boolean = true
}