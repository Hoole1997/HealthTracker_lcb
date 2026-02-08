package com.daily.health.manager.face.dialog

import androidx.fragment.app.FragmentManager

/**
 * 通知权限重要性说明对话框
 *
 * 内部重构：使用 V2 组件实现，对外接口保持不变
 */
object NotificationPermissionDialog {

    private const val TAG = "NotificationPermissionDialog"

    /**
     * 显示通知权限说明对话框
     * 内部使用 V2 样式实现
     */
    fun show(
        fragmentManager: FragmentManager,
        onGoToSettings: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        // 内部重构：使用 V2 组件，通用场景 alarmType = -1（不匹配任何已定义类型）
        NotificationPermissionV2DialogFragment.show(
            fragmentManager = fragmentManager,
            alarmType = -1, // 通用场景，触发 else 分支
            isDoNotAsk = true, // 永久拒绝场景（Go to Settings）
            onGoToSettings = onGoToSettings,
            onRequestPermission = null,
            onCancelCallback = onCancel
        )
    }
}