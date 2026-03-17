package com.daily.health.manager.face.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.data.utils.DateTimeUtils
import java.util.Date

/**
 * 心率测量入口卡片
 *
 * @param lastBpm 最近一次测量的心率值，null 表示无数据
 * @param lastDateFormatted 最近一次测量时间的格式化字符串，null 表示无数据（推荐上层预计算）
 * @param onMeasureClick 点击测量按钮的回调
 * @param modifier 外部传入的 Modifier
 */
@Composable
fun HeartRateMeasureEntry(
    lastBpm: Int?,
    lastDateFormatted: String?,
    onMeasureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val measureButtonLabel = stringResource(R.string.fc_measure_now)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorResource(R.color.color_E6FFF6))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        DecorativeIllustration(
            modifier = Modifier.align(Alignment.TopEnd)
        )

        Column {
            LastReportHeader()
            Spacer(modifier = Modifier.height(8.dp))
            ValueRow(lastBpm = lastBpm, lastDateFormatted = lastDateFormatted)
            Spacer(modifier = Modifier.height(10.dp))
            InstructionText()
            Spacer(modifier = Modifier.height(16.dp))
            MeasureButton(
                label = measureButtonLabel,
                onClick = onMeasureClick
            )
        }
    }
}

/**
 * 兼容旧版调用（lastDate: Long?）
 */
@Composable
fun HeartRateMeasureEntry(
    lastBpm: Int?,
    lastDate: Long?,
    onMeasureClick: () -> Unit
) {
    val formattedDate = remember(lastDate) {
        lastDate?.let { DateTimeUtils.formatDateTime(Date(it)) }
    }
    HeartRateMeasureEntry(
        lastBpm = lastBpm,
        lastDateFormatted = formattedDate,
        onMeasureClick = onMeasureClick
    )
}

@Composable
private fun DecorativeIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.mipmap.ic_measure_heart),
            contentDescription = null,
            modifier = Modifier
                .size(94.dp)
                .offset(y = (-10).dp)
        )

        Box(
            modifier = Modifier
                .offset(x = (-4).dp, y = 42.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(6.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.hr_measure_ic_fingerprint),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LastReportHeader() {
    Text(
        text = stringResource(R.string.fc_last_report),
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = colorResource(R.color.color_666)
    )
}

@Composable
private fun ValueRow(
    lastBpm: Int?,
    lastDateFormatted: String?
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = lastBpm?.toString() ?: stringResource(R.string.fc_no_data),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.c5)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.fc_bpm),
            fontSize = 14.sp,
            color = colorResource(R.color.t1),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (lastDateFormatted != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = lastDateFormatted,
                fontSize = 13.sp,
                color = colorResource(R.color.color_666),
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun InstructionText() {
    Text(
        text = stringResource(R.string.fc_test_and_record_hr),
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium,
        color = colorResource(R.color.t1),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 200.dp)
    )
}

@Composable
private fun MeasureButton(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colorResource(R.color.c5))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.hr_measure_bg_heart),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
