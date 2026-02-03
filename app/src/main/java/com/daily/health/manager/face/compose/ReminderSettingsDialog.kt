package com.daily.health.manager.face.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.daily.health.manager.R
import net.corekit.core.report.ReportDataManager
import java.util.Calendar

/**
 * 录入成功后的提醒设置弹窗
 */
@Composable
fun ReminderSettingsDialog(
    alarmType: Int,
    onAdd: (hour: Int, minute: Int, repeatFlag: Int) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        ReportDataManager.reportData("reminder_dialog_show", mapOf("type" to alarmType.toString()))
    }

    val calendar = Calendar.getInstance()
    val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = calendar.get(Calendar.MINUTE)
    
    // 默认每天重复
    val initialRepeatFlag = 0x7F

    CommonAlarmConfigDialog(
        initialHour = initialHour,
        initialMinute = initialMinute,
        initialRepeatFlag = initialRepeatFlag,
        title = stringResource(R.string.ht_alarm_default_title), // "Health Reminder"
        description = stringResource(R.string.ht_alarm_default_content), // "Please monitor your health on time"
        confirmButtonText = stringResource(R.string.ht_add_record), // "Add Reminder"
        onDismiss = {
            ReportDataManager.reportData("reminder_close_click", mapOf("type" to alarmType.toString()))
            onDismiss()
        },
        onConfirm = { h, m, f ->
            ReportDataManager.reportData("reminder_add_click", mapOf(
                "type" to alarmType.toString(),
                "time" to String.format("%02d:%02d", h, m)
            ))
            onAdd(h, m, f)
        },
        showDelete = false,
        showCloseIcon = true
    )
}
