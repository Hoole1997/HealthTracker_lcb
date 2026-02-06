package com.daily.health.manager.face.dialog

import android.os.Bundle
import androidx.compose.runtime.Composable
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.daily.health.manager.face.compose.ReminderSettingsDialog
import com.daily.health.manager.face.viewmodel.AlarmViewModel

class ReminderSettingsDialogFragment : ComposeBottomSheetFragment() {

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
    }

    @Composable
    override fun ComposeContent() {
        ReminderSettingsDialog(
            alarmType = alarmType,
            onAdd = { hour, minute, repeatFlag ->
                // 1. 保存闹钟
                alarmViewModel.addAlarmByType(alarmType, hour, minute, repeatFlag)
                // 2. 关闭弹窗
                dismiss()
            },
            onDismiss = { dismiss() }
        )
    }
}
