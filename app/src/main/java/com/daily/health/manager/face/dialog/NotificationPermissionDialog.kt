package com.daily.health.manager.face.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtDialogNotificationPermissionBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
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
) : BaseBottomSheetDialogFragment<HtDialogNotificationPermissionBinding>() {

    private var isGoToSettingsClicked = false

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
        isCancelable = false
        // 恢复时直接关闭
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogNotificationPermissionBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            // 设置对话框内容
            tvTitle.text = getString(R.string.ht_notification_permission_title)
            tvContent.text = getString(R.string.ht_notification_permission_des)

            // 设置按钮文本
            btnAllow.text = getString(R.string.ht_turn_on)
            // 设置按钮点击事件
            btnAllow.click {
                isGoToSettingsClicked = true
                onGoToSettings?.invoke()
                dismissAllowingStateLoss()
            }
            ivClose.click {
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isGoToSettingsClicked) {
            onCancel?.invoke()
        }
    }

}