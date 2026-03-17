package com.daily.health.manager.face.card

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R

/**
 * 心率测量原理与建议说明弹窗 (纯 Compose 实现)
 * 对应 FIGMA “问号弹窗”
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PpgExplainBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            // 自定义 Handle 对齐截图中的灰色胶囊
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(66.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(colorResource(R.color.color_EAEAEA))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 标题
            Text(
                text = stringResource(R.string.fc_ppg_tips_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.t1)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 副标题 (支持颜色高亮)
            SubtitleWithHighlight()

            Spacer(modifier = Modifier.height(16.dp))

            // 插图区域
            IllustrationArea()

            Spacer(modifier = Modifier.height(20.dp))

            // 步骤列表
            StepItem(1, stringResource(R.string.fc_ppg_tips_step1))
            Spacer(modifier = Modifier.height(20.dp))
            StepItem(2, stringResource(R.string.fc_ppg_tips_step2))
            Spacer(modifier = Modifier.height(20.dp))
            StepItem(3, stringResource(R.string.fc_ppg_tips_step3))

            Spacer(modifier = Modifier.height(40.dp))

            // Got it 按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.c5)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.fc_got_it),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SubtitleWithHighlight() {
    val fullText = stringResource(R.string.fc_ppg_tips_subtitle)
    
    // 使用正则提取被 <font color="#FB4248">...</font> 包裹的关键词
    // 注意处理可能存在的转义字符
    val regex = """([^<]*)<font color="#FB4248">([^<]*)</font>([^<]*)""".toRegex()
    val matchResult = regex.find(fullText)

    val annotatedString = if (matchResult != null) {
        val (before, highlight, after) = matchResult.destructured
        buildAnnotatedString {
            append(before)
            withStyle(style = SpanStyle(color = Color(0xFFFB4248), fontWeight = FontWeight.Bold)) {
                append(highlight)
            }
            append(after)
        }
    } else {
        // 如果没有找到标签，降级为普通文本（去除所有标签）
        buildAnnotatedString {
            append(fullText.replace(Regex("<[^>]*>"), ""))
        }
    }

    Text(
        text = annotatedString,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = colorResource(R.color.t1),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp)
    )
}

@Composable
private fun IllustrationArea() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0FAF8)),
        contentAlignment = Alignment.Center
    ) {
        // 左侧背景波纹
        Image(
            painter = painterResource(R.mipmap.ic_heartbeat_dialog_left),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.5f),
            contentScale = ContentScale.Fit
        )

        // 右侧背景波纹
        Image(
            painter = painterResource(R.mipmap.ic_heartbeat_dialog_right),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.5f),
            contentScale = ContentScale.Fit
        )

        // 手机
        Image(
            painter = painterResource(R.mipmap.ic_phone),
            contentDescription = null,
            modifier = Modifier.height(140.dp)
        )

        // 闪光灯红色扩散光圈 (组合 ic_flash_big_red)
        Image(
            painter = painterResource(R.mipmap.ic_flash_big_red),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .offset(x = (-4).dp, y = (-54).dp)
        )
        
        // 可选：如果效果需要更强，叠加 ic_flash_small_red
        Image(
            painter = painterResource(R.mipmap.ic_flash_small_red),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .offset(x = (-4).dp, y = (-56).dp)
        )

        // 手指
        Image(
            painter = painterResource(R.mipmap.ic_hand),
            contentDescription = null,
            modifier = Modifier
                .height(180.dp)
                .offset(x = 22.dp, y = 32.dp)
        )
    }
}

@Composable
private fun StepItem(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 数字圆圈
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(colorResource(R.color.c5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 说明文本
        Text(
            text = text,
            fontSize = 13.sp,
            color = colorResource(R.color.t1),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium
        )
    }
}
