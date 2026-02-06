package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import com.blankj.utilcode.util.SnackbarUtils.dismiss
import com.daily.health.manager.databinding.HtDialogNotificationPermissionV2Binding
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.daily.health.manager.face.compose.ReminderSettingsDialog
import com.daily.health.manager.face.viewmodel.AlarmViewModel
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment

class ReminderSettingsDialogFragment :
    BaseBottomSheetDialogFragment<HtDialogNotificationPermissionV2Binding>() {

    private var alarmType: Int = 0
    private val alarmViewModel: AlarmViewModel by viewModel()
    private var dismissListener: (() -> Unit)? = null

    fun setOnDismissListener(listener: (() -> Unit)) {
        this.dismissListener = listener
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.invoke()
    }

    companion object {
        private const val ARG_ALARM_TYPE = "arg_alarm_type"

        fun newInstance(alarmType: Int): ReminderSettingsDialogFragment {
            val fragment = ReminderSettingsDialogFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_ALARM_TYPE, alarmType)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmType = arguments?.getInt(ARG_ALARM_TYPE) ?: 0
        // 恢复时直接关闭（回调已丢失）
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // 设置窗口 Gravity 为底部，防止漂移
            setGravity(android.view.Gravity.BOTTOM)
            // 彻底禁用窗口动画，动画将由 Compose 接管
            setWindowAnimations(0)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogNotificationPermissionV2Binding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ReminderSettingsDialog(
                    alarmType = alarmType,
                    onAdd = { hour, minute, repeatFlag ->
                        // 1. 保存闹钟
                        alarmViewModel.addAlarmByType(alarmType, hour, minute, repeatFlag)
                        // 2. 关闭弹窗
                        dismiss()
                    },
                    onDismiss = {
                        dismiss()
                    }
                )
            }
        }
    }

    override fun isAutoNavigationBarsPadding() = false


}
