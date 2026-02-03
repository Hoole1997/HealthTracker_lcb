package com.daily.health.manager.face.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.AlarmRecord
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * 闹钟编辑弹窗 (Compose Redesign)
 */
@Composable
fun AlarmEditDialog(
    alarmRecord: AlarmRecord? = null,
    alarmType: Int,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, repeatFlag: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    CommonAlarmConfigDialog(
        initialHour = alarmRecord?.hour ?: 8,
        initialMinute = alarmRecord?.minute ?: 0,
        initialRepeatFlag = alarmRecord?.repeatFlag ?: AlarmRecord.REPEAT_DAILY,
        title = if (alarmRecord == null) stringResource(R.string.ht_add_record) else stringResource(R.string.ht_edit_record),
        confirmButtonText = stringResource(R.string.ht_save),
        onDismiss = onDismiss,
        onConfirm = onSave,
        showDelete = alarmRecord != null && onDelete != null,
        onDelete = onDelete,
        showCloseIcon = true // Enable close icon as per design
    )
}

/**
 * 通用闹钟配置弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAlarmConfigDialog(
    initialHour: Int,
    initialMinute: Int,
    initialRepeatFlag: Int,
    title: String,
    description: String? = null,
    confirmButtonText: String,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, repeatFlag: Int) -> Unit,
    showDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
    showCloseIcon: Boolean = false
) {
    var repeatFlag by remember { mutableStateOf(initialRepeatFlag) }
    val currentHour = remember { mutableStateOf(initialHour) }
    val currentMinute = remember { mutableStateOf(initialMinute) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null // We have our own handle or design doesn't use standard drag handle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                // Header (Title + Optional Close Icon)
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 顶部 Handle Indicator (仿 BottomSheet 视觉)
                    Box(
                         modifier = Modifier
                             .align(Alignment.TopCenter)
                             .width(66.dp)
                             .height(8.dp)
                             .clip(RoundedCornerShape(4.dp))
                             .background(Color(0xFFE0E0E0))
                     )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colorResource(R.color.t1)
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 19.dp),
                        textAlign = TextAlign.Center
                    )

                    if (showCloseIcon) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ht_ic_dialog_close),
                            contentDescription = "Close",
                            tint = colorResource(R.color.color_d3d3d3),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp) // Adjust for visual balance
                                .size(24.dp)
                                .clickable { onDismiss() }
                        )
                    }
                }

                if (description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        color = colorResource(R.color.color_666),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time Pickers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp).padding(horizontal = 32.dp), // Increased height for better spacing
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelTimePicker(
                        range = 0..23,
                        initialValue = initialHour,
                        onValueChange = { currentHour.value = it },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.t1),

                    )

                    WheelTimePicker(
                        range = 0..59,
                        initialValue = initialMinute,
                        format = { "%02d".format(it) },
                        onValueChange = { currentMinute.value = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Repeat
                Text(
                    text = "Repeat", // TODO: String resource
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colorResource(R.color.t1), // Darker title
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                WeekdaySelector(
                    repeatFlag = repeatFlag,
                    onFlagChanged = { repeatFlag = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val secondaryButtonText = if (showDelete) "Delete" else stringResource(R.string.ht_cancel)
                    val secondaryButtonColor = if (showDelete) Color(0xFFF9F9FA) else Color(0xFFF9F9FA)
                    val secondaryTextColor = if (showDelete) Color(0xFF999999) else Color(0xFF999999) // Design shows gray

                    Button(
                        onClick = {
                            if (showDelete && onDelete != null) onDelete() else onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryButtonColor,
                            contentColor = secondaryTextColor
                        ),
                        shape = RoundedCornerShape(24.dp), // Pill shape
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp), // Taller buttons
                        elevation = null
                    ) {
                        Text(
                            text = secondaryButtonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { onConfirm(currentHour.value, currentMinute.value, repeatFlag) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.c5),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        elevation = null
                    ) {
                        Text(
                            text = confirmButtonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

/**
 * 滚轮时间选择器 (Design Style)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePicker(
    range: IntRange,
    initialValue: Int,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit
) {
    val count = range.last - range.first + 1
    val infiniteCount = Int.MAX_VALUE
    val initialIndex = infiniteCount / 2 + (initialValue - range.first) - (infiniteCount / 2) % count

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val itemHeight = 56.dp // Taller items
    val visibleItemsCount = 3 // Standard picker view

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { index ->
                val centerIndex = index + visibleItemsCount / 2
                val actualValue = range.first + (centerIndex % count)
                actualValue
            }
            .distinctUntilChanged()
            .collect { onValueChange(it) }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Selection Background (Green Box covering 3 rows)
        Box(
            modifier = Modifier
                .width(98.dp)
                .height(itemHeight * 3) // Covers 3 rows (with slight padding)
                .background(colorResource(R.color.c5), RoundedCornerShape(12.dp))
        ) {
            // Upper Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = itemHeight - 4.dp) // Positioned above the middle item
                    .background(Color.White.copy(alpha = 0.3f))
            )
            // Lower Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(1.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = -(itemHeight - 4.dp)) // Positioned below the middle item
                    .background(Color.White.copy(alpha = 0.4f))
            )
        }

        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(infiniteCount) { index ->
                val actualValue = range.first + (index % count)
                val isSelected by remember {
                    derivedStateOf {
                         val layoutInfo = listState.layoutInfo
                         val centerOffset = layoutInfo.viewportEndOffset / 2
                         val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                         if (itemInfo != null) {
                             val itemCenter = itemInfo.offset + itemInfo.size / 2
                             abs(centerOffset - itemCenter) < itemInfo.size / 2
                         } else {
                             false
                         }
                    }
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = format(actualValue),
                        fontSize = if (isSelected) 24.sp else 20.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier
                            .alpha(if (isSelected) 1f else 0.46f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekdaySelector(
    repeatFlag: Int,
    onFlagChanged: (Int) -> Unit
) {
    // 星期标签：MON, TUE ...
    val days = stringArrayResource(R.array.ht_week_simple)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, dayLabel ->
            val mask = 1 shl index
            val isSelected = (repeatFlag and mask) != 0

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null // Disable ripple for cleaner look
                    ) {
                        val newFlag = if (isSelected) {
                            repeatFlag and mask.inv()
                        } else {
                            repeatFlag or mask
                        }
                        onFlagChanged(newFlag)
                    }
            ) {
                Text(
                    text = dayLabel.uppercase(),
                    color = colorResource(R.color.t1),
                    fontSize = 15.sp, // Slightly smaller
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Checkbox Icon
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(
                        id = if (isSelected) R.drawable.ht_ic_checked else R.drawable.ht_ic_uncheck
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun AlarmEditDialogPreview2() {
    MaterialTheme {
        AlarmEditDialog(
            alarmType = 0,
            onDismiss = {},
            onSave = { h, m, f -> }
        )
    }
}
