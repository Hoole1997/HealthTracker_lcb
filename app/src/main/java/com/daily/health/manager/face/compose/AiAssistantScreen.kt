package com.daily.health.manager.face.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R

/**
 * AI 助手空状态页面 (Compose)
 */
@Composable
fun AiAssistantScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 使用项目通用的 TopBar
        HealthTopBar(
            title = stringResource(id = R.string.ht_ai_assistant_title),
            onBack = onBack
        )

        // 空状态内容居中
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(140.dp))
            Image(
                painter = painterResource(id = R.mipmap.ht_ic_ai_empty),
                contentDescription = null,
                modifier = Modifier.size(182.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = stringResource(id = R.string.ht_ai_empty_desc),
                style = androidx.compose.ui.text.TextStyle(
                    color = colorResource(R.color.color_999),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            )
        }
    }
}
