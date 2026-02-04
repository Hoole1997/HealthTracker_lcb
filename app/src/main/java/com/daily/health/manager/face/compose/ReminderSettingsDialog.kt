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

    val typeNameRes = when (alarmType) {
        com.daily.health.manager.data.entity.AlarmRecord.TYPE_BLOOD_SUGAR -> R.string.ht_blood_suger
        com.daily.health.manager.data.entity.AlarmRecord.TYPE_BLOOD_PRESSURE -> R.string.ht_blood_pressure
        com.daily.health.manager.data.entity.AlarmRecord.TYPE_HEART_RATE -> R.string.ht_heart_rate
        com.daily.health.manager.data.entity.AlarmRecord.TYPE_BMI -> R.string.ht_bmi
        com.daily.health.manager.data.entity.AlarmRecord.TYPE_CHOLESTEROL -> R.string.ht_cholesterol
        else -> R.string.ht_alarm_default_title
    }
    val typeName = stringResource(typeNameRes)
    val dialogTitle = stringResource(R.string.ht_reminder_for, typeName)

    CommonAlarmConfigDialog(
        initialHour = initialHour,
        initialMinute = initialMinute,
        initialRepeatFlag = initialRepeatFlag,
        title = dialogTitle,
        description = null, // Sync with AlarmEditDialog: No description
        confirmButtonText = stringResource(R.string.ht_save), // Sync with AlarmEditDialog: "Save"
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
