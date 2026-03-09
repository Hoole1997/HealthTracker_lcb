package com.daily.health.manager.face.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.AlarmRecord
import kotlinx.coroutines.delay

/** 主题绿色 c5 */
private val C5Green = Color(0xFF1D6BF2)
/** 灰色预览区域背景 */
private val GrayPreviewBg = Color(0xFFE8E8EC)
/** 手机示意框内部背景 */
private val PhoneInnerBg = Color(0xFFF0F0F5)
/** 拖拽指示器颜色 */
private val DragHandleColor = Color(0xFFC0C0C4)
/** 标题文字颜色 */
private val TitleColor = Color(0xFF333333)
/** 描述文字颜色 */
private val DescColor = Color(0xFF666666)
/** 时间文字颜色 */
private val TimeColor = Color(0xFF1A1918)

/**
 * 通知权限请求弹窗 V2 — Compose 主内容
 *
 * @param alarmType AlarmRecord.TYPE_* 常量，决定通知预览卡片的文案和图标
 * @param isDoNotAsk 是否永久拒绝，决定按钮文案
 * @param onButtonClick 按钮点击回调
 */
@Composable
fun NotificationPermissionV2Content(
    alarmType: Int,
    isDoNotAsk: Boolean,
    onButtonClick: () -> Unit
) {
    // 整体入场动画控制
    var fullContentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 微小延迟确保 Window 定位稳定
        delay(30)
        fullContentVisible = true
    }

    AnimatedVisibility(
        visible = fullContentVisible,
        enter = slideInVertically(
            initialOffsetY = { it }, // 从底部向上滑入
            animationSpec = tween(durationMillis = 350)
        ) + fadeIn(animationSpec = tween(durationMillis = 200))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 上半灰色预览区域
            GrayPreviewArea(alarmType = alarmType)
            // 下半白色内容区域
            WhiteContentArea(alarmType = alarmType, isDoNotAsk = isDoNotAsk, onButtonClick = onButtonClick)
        }
    }
}

/**
 * 灰色预览区域：拖拽指示器 + 手机示意框 + 通知预览卡片
 */
@Composable
private fun GrayPreviewArea(alarmType: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(GrayPreviewBg)
            .padding(top = 12.dp, bottom = 0.dp, start = 32.dp, end = 32.dp)
    ) {
        // 拖拽指示器横条
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .width(66.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DragHandleColor)
        )

        // 手机示意框 + 通知卡片
        PhoneBorderFrame(
            alarmType = alarmType,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
        )
    }
}

/**
 * 手机示意框：c5 绿色圆角边框（底部不闭合），内部放通知预览卡片
 *
 * 底部不闭合的实现方式：使用 drawBehind 手动绘制三条边（顶部圆弧 + 左右竖线），
 * 跳过底部边，形成开口向下的 U 形效果。
 */
@Composable
private fun PhoneBorderFrame(
    alarmType: Int,
    modifier: Modifier = Modifier
) {
    val borderWidth = 5.dp
    val cornerRadius = 22.dp

    // 动画状态：卡片可见性 + 边框渐显透明度
    var cardVisible by remember { mutableStateOf(false) }
    val frameAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (cardVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "FrameAlpha"
    )

    LaunchedEffect(Unit) {
        delay(150) // 方案 A：缩短等待时间，让入场更紧凑
        cardVisible = true
    }

    Box(
        modifier = modifier.defaultMinSize(minHeight = 200.dp)
            .drawBehind {
                val strokeWidthPx = borderWidth.toPx()
                val cornerRadiusPx = cornerRadius.toPx()
                val halfStroke = strokeWidthPx / 2

                // 绘制 U 形边框
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(halfStroke, size.height)
                    lineTo(halfStroke, cornerRadiusPx + halfStroke)
                    quadraticTo(halfStroke, halfStroke, cornerRadiusPx + halfStroke, halfStroke)
                    lineTo(size.width - cornerRadiusPx - halfStroke, halfStroke)
                    quadraticTo(size.width - halfStroke, halfStroke, size.width - halfStroke, cornerRadiusPx + halfStroke)
                    lineTo(size.width - halfStroke, size.height)
                }

                drawPath(
                    path = path,
                    color = C5Green.copy(alpha = frameAlpha), // 方案 B：边框渐显
                    style = Stroke(width = strokeWidthPx)
                )
            }
            .padding(start = 5.dp, end = 5.dp, top = 5.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(PhoneInnerBg.copy(alpha = frameAlpha)) // 方案 B：内部背景渐显
            .padding(start = 14.dp, end = 14.dp, top = 20.dp, bottom = 24.dp)
    ) {
        AnimatedVisibility(
            visible = cardVisible,
            enter = slideInVertically(
                initialOffsetY = { it }, // 从下方滑入
                animationSpec = tween(
                    durationMillis = 250, // 方案 A：加速滑入过程
                    easing = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
                )
            ) + fadeIn(
                animationSpec = tween(durationMillis = 200)
            )
        ) {
            NotificationPreviewCard(alarmType = alarmType)
        }
    }
}

/**
 * 通知预览卡片：模拟真实通知的外观
 */
@Composable
private fun NotificationPreviewCard(alarmType: Int) {
    val previewData = getNotificationPreviewData(alarmType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        // 背景水印：闹钟 2D 装饰（方案 A）
        Icon(
            painter = painterResource(id = R.drawable.ht_ic_setting_alarm),
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-30).dp, y = 10.dp)
                .alpha(0.05f),
            tint = colorResource(R.color.color_666)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧内容
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 时间
                Text(
                    text = "08:00",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimeColor,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 通知文案
                Text(
                    text = stringResource(id = previewData.contentResId),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                // 模拟 "Record Now" 按钮（60% 透明度，纯展示）
                Box(
                    modifier = Modifier
                        .alpha(0.6f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(C5Green)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = previewData.buttonTextResId),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧拟物图标
            Image(
                painter = painterResource(id = previewData.decorIconResId),
                contentDescription = null,
                modifier = Modifier.size(64.dp).alpha(0.8f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * 白色内容区域：标题 + 描述 + 操作按钮
 */
@Composable
private fun WhiteContentArea(
    alarmType: Int,
    isDoNotAsk: Boolean,
    onButtonClick: () -> Unit
) {
    val previewData = getNotificationPreviewData(alarmType)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            // 内容区域 padding，底部增加导航栏高度以延伸白色背景
            .navigationBarsPadding()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(id = R.string.ht_notification_grant_permissions),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TitleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 描述 - 使用场景化动态文案
        Text(
            text = stringResource(id = previewData.descriptionResId),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = DescColor,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        // 操作按钮
        Button(
            onClick = onButtonClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = C5Green,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(
                    id = if (isDoNotAsk) R.string.ht_go_to_settings else R.string.ht_turn_on
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PreviewBloodSugarTurnOn() {
    NotificationPermissionV2Content(
        alarmType = AlarmRecord.TYPE_BLOOD_SUGAR,
        isDoNotAsk = false,
        onButtonClick = {}
    )
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PreviewBloodSugarGoToSettings() {
    NotificationPermissionV2Content(
        alarmType = AlarmRecord.TYPE_BLOOD_SUGAR,
        isDoNotAsk = true,
        onButtonClick = {}
    )
}

@Preview(showBackground = true, widthDp = 402)
@Composable
private fun PreviewBloodPressureTurnOn() {
    NotificationPermissionV2Content(
        alarmType = AlarmRecord.TYPE_BLOOD_PRESSURE,
        isDoNotAsk = false,
        onButtonClick = {}
    )
}
