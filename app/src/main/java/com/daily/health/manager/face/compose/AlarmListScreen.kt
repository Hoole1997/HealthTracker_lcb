package com.daily.health.manager.face.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.face.dialog.AlarmEditDialogFragment
import com.daily.health.manager.face.viewmodel.AlarmViewModel
import java.util.Locale

/**
 * 闹钟管理页面主要内容 (Compose)
 */
@SuppressLint("ContextCastToActivity")
@Composable
fun AlarmListContent(
    viewModel: AlarmViewModel,
    onBack: () -> Unit
) {
    val bsAlarms by viewModel.bloodSugarAlarms.collectAsState(initial = emptyList())
    val bpAlarms by viewModel.bloodPressureAlarms.collectAsState(initial = emptyList())
    val hrAlarms by viewModel.heartRateAlarms.collectAsState(initial = emptyList())
    val bmiAlarms by viewModel.bmiAlarms.collectAsState(initial = emptyList())
    val cholAlarms by viewModel.cholesterolAlarms.collectAsState(initial = emptyList())

    val context = LocalContext.current as? FragmentActivity
    val bsTitle = stringResource(R.string.tr_blood_suger)
    val bpTitle = stringResource(R.string.tr_blood_pressure)
    val hrTitle = stringResource(R.string.tr_heart_rate)
    val bmiTitle = stringResource(R.string.tr_bmi)
    val cholTitle = stringResource(R.string.tr_cholesterol)

    Scaffold(
        topBar = {
            HealthTopBar(
                title = stringResource(R.string.tr_alarm_management),
                onBack = onBack
            )
        },
        containerColor = colorResource(R.color.bg_window)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Blood Sugar
            alarmCategorySection(
                title = bsTitle,
                alarms = bsAlarms,
                onAddClick = { context?.let { showEditDialog(it, viewModel, null, AlarmRecord.TYPE_BLOOD_SUGAR) } },
                onToggle = { alarm, enabled -> viewModel.updateAlarmEnabled(alarm.id, enabled, AlarmRecord.TYPE_BLOOD_SUGAR) },
                onItemClick = { alarm -> context?.let { showEditDialog(it, viewModel, alarm, AlarmRecord.TYPE_BLOOD_SUGAR) } }
            )

            // Blood Pressure
            alarmCategorySection(
                title = bpTitle,
                alarms = bpAlarms,
                onAddClick = { context?.let { showEditDialog(it, viewModel, null, AlarmRecord.TYPE_BLOOD_PRESSURE) } },
                onToggle = { alarm, enabled -> viewModel.updateAlarmEnabled(alarm.id, enabled, AlarmRecord.TYPE_BLOOD_PRESSURE) },
                onItemClick = { alarm -> context?.let { showEditDialog(it, viewModel, alarm, AlarmRecord.TYPE_BLOOD_PRESSURE) } },
                showSpacer = true
            )

            // Heart Rate
            alarmCategorySection(
                title = hrTitle,
                alarms = hrAlarms,
                onAddClick = { context?.let { showEditDialog(it, viewModel, null, AlarmRecord.TYPE_HEART_RATE) } },
                onToggle = { alarm, enabled -> viewModel.updateAlarmEnabled(alarm.id, enabled, AlarmRecord.TYPE_HEART_RATE) },
                onItemClick = { alarm -> context?.let { showEditDialog(it, viewModel, alarm, AlarmRecord.TYPE_HEART_RATE) } },
                showSpacer = true
            )

            // BMI
            alarmCategorySection(
                title = bmiTitle,
                alarms = bmiAlarms,
                onAddClick = { context?.let { showEditDialog(it, viewModel, null, AlarmRecord.TYPE_BMI) } },
                onToggle = { alarm, enabled -> viewModel.updateAlarmEnabled(alarm.id, enabled, AlarmRecord.TYPE_BMI) },
                onItemClick = { alarm -> context?.let { showEditDialog(it, viewModel, alarm, AlarmRecord.TYPE_BMI) } },
                showSpacer = true
            )

            // Cholesterol
            alarmCategorySection(
                title = cholTitle,
                alarms = cholAlarms,
                onAddClick = { context?.let { showEditDialog(it, viewModel, null, AlarmRecord.TYPE_CHOLESTEROL) } },
                onToggle = { alarm, enabled -> viewModel.updateAlarmEnabled(alarm.id, enabled, AlarmRecord.TYPE_CHOLESTEROL) },
                onItemClick = { alarm -> context?.let { showEditDialog(it, viewModel, alarm, AlarmRecord.TYPE_CHOLESTEROL) } },
                showSpacer = true
            )
            
            // Padding for Ad Container
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 分类列表段落 (LazyColumn 内部扩展)
 */
fun androidx.compose.foundation.lazy.LazyListScope.alarmCategorySection(
    title: String,
    alarms: List<AlarmRecord>,
    onAddClick: () -> Unit,
    onToggle: (AlarmRecord, Boolean) -> Unit,
    onItemClick: (AlarmRecord) -> Unit,
    showSpacer: Boolean = false
) {
    if (showSpacer) {
        item(key = "spacer_$title") {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
    
    item(key = "header_$title") {
        AlarmCategoryHeader(
            title = title,
            onAddClick = onAddClick
        )
    }
    
    if (alarms.isEmpty()) {

    } else {
        items(
            items = alarms,
            key = { it.id } // 关键优化：使用唯一 ID 提升列表增删和单项刷新性能
        ) { alarm ->
            AlarmItemCard(
                alarm = alarm,
                onToggle = { enabled -> onToggle(alarm, enabled) },
                onClick = { onItemClick(alarm) }
            )
        }
    }
}

@Composable
fun AlarmCategoryHeader(
    title: String,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.t1)
            )
        )
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.tr_ic_alarm_add),
                contentDescription = "Add Alarm",
                tint = colorResource(R.color.c5) // Use brand secondary/primary color
            )
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: AlarmRecord,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val contentAlpha = if (alarm.isEnabled) 1f else 0.38f
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.US,"%02d:%02d", alarm.hour, alarm.minute),
                    fontSize = 24.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.t1).copy(alpha = contentAlpha)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.getShortRepeatDescription(LocalContext.current),
                    fontSize = 12.sp,
                    color = colorResource(R.color.color_999).copy(alpha = contentAlpha)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 44.dp, height = 24.dp)
                    .scale(0.8f)
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    thumbContent = {
                        // Fix M3 default behavior where thumb size changes between states
                        Spacer(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Transparent, CircleShape)
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colorResource(R.color.c5).copy(alpha = 0.8f),
                        uncheckedThumbColor = colorResource(R.color.color_f5f5f5),
                        uncheckedTrackColor = colorResource(R.color.color_E0E0E0),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

/**
 * 弹出编辑弹窗 (Bridge to Fragment)
 */
private fun showEditDialog(
    activity: FragmentActivity,
    viewModel: AlarmViewModel,
    record: AlarmRecord?,
    type: Int
) {
    val dialog = AlarmEditDialogFragment.newInstance(
        alarmRecord = record,
        alarmType = type,
        onSave = { h, m, f ->
            if (record == null) {
                viewModel.addAlarmByType(type, h, m, f)
            } else {
                viewModel.updateAlarm(record.id, h, m, f)
            }
        },
        onDelete = if (record != null) {
            { viewModel.deleteAlarm(record.id) }
        } else null
    )
    dialog.show(activity.supportFragmentManager, "AlarmEditDialog")
}

