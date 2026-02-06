package com.daily.health.manager.face.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.face.compose.NotificationPermissionV2Content

/**
 * 通知权限请求弹窗 V2 — 继承 ComposeBottomSheetFragment
 *
 * 用于 HealthDetailScreen 返回拦截场景。
 * 使用 XML 容器布局承载 ComposeView，Compose 绘制 UI。
 * 继承基类以复用 BottomSheet 展开、导航栏适配、insets 处理等已验证逻辑。
 *
 * 方案B：先弹窗再请求
 * - 默认按钮显示 "Turn on"，点击后触发系统权限请求
 * - 永久拒绝时按钮显示 "Go to Settings"，点击后跳转设置页
 */
class NotificationPermissionV2DialogFragment : ComposeBottomSheetFragment() {

    private var alarmType: Int = 0
    private var isDoNotAsk: Boolean = false

    /** 跳转设置页回调（永久拒绝场景） */
    private var onGoToSettings: (() -> Unit)? = null
    /** 请求系统通知权限回调（首次拒绝场景） */
    private var onRequestPermission: (() -> Unit)? = null
    /** 取消/关闭回调 */
    private var onCancel: (() -> Unit)? = null

    /** 标记是否已点击按钮（用于 onDismiss 判断） */
    private var isButtonClicked = false

    companion object {
        private const val ARG_ALARM_TYPE = "arg_alarm_type"
        private const val ARG_IS_DO_NOT_ASK = "arg_is_do_not_ask"

        /**
         * 显示通知权限请求弹窗 V2
         *
         * @param fragmentManager FragmentManager
         * @param alarmType AlarmRecord.TYPE_* 常量
         * @param isDoNotAsk 是否永久拒绝
         * @param onGoToSettings 跳转设置页回调
         * @param onRequestPermission 请求系统权限回调
         * @param onCancel 取消回调
         */
        fun show(
            fragmentManager: FragmentManager,
            alarmType: Int,
            isDoNotAsk: Boolean,
            onGoToSettings: (() -> Unit)? = null,
            onRequestPermission: (() -> Unit)? = null,
            onCancelCallback: (() -> Unit)? = null
        ) {
            val fragment = NotificationPermissionV2DialogFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_ALARM_TYPE, alarmType)
                putBoolean(ARG_IS_DO_NOT_ASK, isDoNotAsk)
            }
            fragment.onGoToSettings = onGoToSettings
            fragment.onRequestPermission = onRequestPermission
            fragment.onCancel = onCancelCallback
            fragment.show(fragmentManager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmType = arguments?.getInt(ARG_ALARM_TYPE) ?: 0
        isDoNotAsk = arguments?.getBoolean(ARG_IS_DO_NOT_ASK) ?: false
    }

    @Composable
    override fun ComposeContent() {
        NotificationPermissionV2Content(
            alarmType = alarmType,
            isDoNotAsk = isDoNotAsk,
            onButtonClick = {
                isButtonClicked = true
                if (isDoNotAsk) {
                    onGoToSettings?.invoke()
                } else {
                    onRequestPermission?.invoke()
                }
                dismissAllowingStateLoss()
            }
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isButtonClicked) {
            onCancel?.invoke()
        }
    }
}

