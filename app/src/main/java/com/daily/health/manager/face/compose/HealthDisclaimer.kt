package com.daily.health.manager.face.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R

/** 首页及健康功能页共用的免责声明；新页面默认收起，重建时保留当前阅读状态。 */
@Composable
fun HealthDisclaimer(
    modifier: Modifier = Modifier,
    @StringRes textRes: Int = R.string.tr_health_disclaimer,
) {
    // 由页面选择声明，组件不依赖业务类型；切换指标后，新声明从收起状态开始。
    var expanded by rememberSaveable(textRes) { mutableStateOf(false) }
    val actionLabel = stringResource(
        if (expanded) R.string.tr_health_disclaimer_collapse else R.string.tr_health_disclaimer_expand
    )
    val stateLabel = stringResource(
        if (expanded) R.string.tr_health_disclaimer_expanded else R.string.tr_health_disclaimer_collapsed
    )
    val textColor = colorResource(R.color.color_666)

    // 整行可点击，保留至少 48dp 的触控区域；小字号和两行省略仅影响视觉呈现。
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = stateLabel }
            .clickable(role = Role.Button, onClickLabel = actionLabel) { expanded = !expanded }
            .heightIn(min = 48.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            color = textColor,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            minLines = 2,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        // 图标保持在右上方，不随展开后的正文高度垂直居中；语义由整行统一提供。
        Icon(
            painter = painterResource(R.drawable.tr_ic_disclaimer_expand),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
        )
    }
}
