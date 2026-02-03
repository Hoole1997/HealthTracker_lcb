package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.daily.health.manager.face.compose.ReminderSettingsDialog
import com.daily.health.manager.face.viewmodel.AlarmViewModel

class ReminderSettingsDialogFragment : DialogFragment() {

    private var alarmType: Int = 0
    private val alarmViewModel: AlarmViewModel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
