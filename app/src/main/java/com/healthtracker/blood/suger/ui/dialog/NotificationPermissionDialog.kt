package com.healthtracker.blood.suger.ui.dialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.DialogNotificationPermissionBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.click

/**
 * 通知权限重要性说明对话框
 *
 * 用于在用户未授权通知权限时，向用户说明通知权限的重要性
 * 并提供跳转到设置页面的选项
 */
class NotificationPermissionDialog(
    private val onGoToSettings: (() -> Unit)? = null,
    private val onCancel: (() -> Unit)? = null
) : BaseBottomSheetDialogFragment<DialogNotificationPermissionBinding>() {

    companion object {
        private const val TAG = "NotificationPermissionDialog"

        /**
         * 显示通知权限说明对话框
         */
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onGoToSettings: (() -> Unit)? = null,
            onCancel: (() -> Unit)? = null
        ) {
            val dialog = NotificationPermissionDialog(onGoToSettings, onCancel)
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
    ) = DialogNotificationPermissionBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            // 设置对话框内容
            tvTitle.text = getString(R.string.notification_permission_title)
            tvMessage.text = getString(R.string.notification_permission_message)

            // 设置按钮文本
            btnGoToSet.text = getString(R.string.go_to_settings)
            // 设置按钮点击事件
            btnGoToSet.click {
                onGoToSettings?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }


    override fun isCancelable(): Boolean = true
}