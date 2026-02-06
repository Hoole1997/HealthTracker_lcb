package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.databinding.HtDialogNotificationPermissionV2Binding
import com.daily.health.manager.face.compose.AlarmEditDialog
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.DialogListener

class AlarmEditDialogFragment : BaseBottomSheetDialogFragment<HtDialogNotificationPermissionV2Binding>() {

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

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogNotificationPermissionV2Binding.inflate(inflater, parent, attachToParent)


    override fun initView(view: View, savedInstanceState: Bundle?) {
        // 恢复时直接关闭（回调已丢失）
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
            return
        }
        mViewBind?.composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var showDeleteConfirm by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                if (showDeleteConfirm) {
                    androidx.compose.ui.window.Dialog(onDismissRequest = { showDeleteConfirm = false }) {
                        // 使用已有的原生项目确认弹窗逻辑比较复杂（因为它是基于 Fragment 的）
                        // 根据用户需求，“用这个 ConfirmDialog.kt”，该文件是一个 BaseVbDialogFragment。
                        // 这里是在 Compose 内部，如果非要用那个 Fragment 弹窗，需要通过 Activity 展示。
                        // 但既然用户指明了要用那个文件，我理解是在需要确认时调用那个 Fragment 弹窗。
                        // 或者用户希望我把那个 Fragment 的 UI 逻辑迁移到这里。
                        // 不过通常在这里我们可以直接展示一个符合那个样式的 Compose 弹窗，或者调用 Fragment。
                        // 为了保持一致性，我将尝试直接弹出那个 Fragment。

                        LaunchedEffect(Unit) {
                            ConfirmDialog(
                                title = getString(R.string.ht_tips),
                                message = getString(R.string.ht_alarm_delete_confirm_title),
                                leftText = getString(R.string.ht_cancel),
                                rightText = getString(R.string.ht_confirm),
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
                } else {
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
        }
    }


    override fun isAutoNavigationBarsPadding() = false
}
