package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.DialogFragment
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.face.compose.AlarmEditDialog
import com.healthtracker.framework.base.fragment.DialogListener

class AlarmEditDialogFragment : ComposeBottomSheetFragment() {

    private var alarmRecord: AlarmRecord? = null
    private var alarmType: Int = 0
    private var onSave: ((Int, Int, Int) -> Unit)? = null
    private var onDelete: (() -> Unit)? = null

    companion object {
        fun newInstance(
            alarmRecord: AlarmRecord? = null,
            alarmType: Int,
            onSave: (Int, Int, Int) -> Unit,
            onDelete: (() -> Unit)? = null
        ): AlarmEditDialogFragment {
            val fragment = AlarmEditDialogFragment()
            fragment.alarmRecord = alarmRecord
            fragment.alarmType = alarmType
            fragment.onSave = onSave
            fragment.onDelete = onDelete
            return fragment
        }
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        // 恢复时直接关闭（回调已丢失）
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
            return
        }
        super.initView(view, savedInstanceState)
    }

    @Composable
    override fun ComposeContent() {
        var showDeleteConfirm by remember { mutableStateOf(false) }

        // 显示删除确认弹窗（使用 Fragment Dialog）
        if (showDeleteConfirm) {
            LaunchedEffect(Unit) {
                ConfirmDialog(
                    title = getString(R.string.fc_tips),
                    message = getString(R.string.fc_alarm_delete_confirm_title),
                    leftText = getString(R.string.fc_cancel),
                    rightText = getString(R.string.fc_confirm),
                    onDialogListener = object : DialogListener {
                        override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                            if (which == ConfirmDialog.BUTTON_OK) {
                                onDelete?.invoke()
                                dismiss()
                            }
                            showDeleteConfirm = false
                        }
                    }
                ).show(parentFragmentManager, "confirm")
            }
        }

        AlarmEditDialog(
            alarmRecord = alarmRecord,
            alarmType = alarmType,
            onDismiss = { dismiss() },
            onSave = { h, m, f ->
                onSave?.invoke(h, m, f)
                dismiss()
            },
            onDelete = if (alarmRecord != null && onDelete != null) {
                { showDeleteConfirm = true }
            } else null
        )
    }
}

