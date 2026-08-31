package com.daily.health.manager.face.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.daily.health.manager.R
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.face.history.HistoryRecordItem
import com.healthtracker.framework.R as FrameworkR
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

// Shared by the launcher entry and the legacy splash host, so their visuals cannot drift.
private val SplashTitle = Color(0xFF333333)
private val SplashMuted = Color(0xFF999999)
private val SplashRecordCard = Color.White

@Composable
private fun AutoSizeSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        modifier = modifier,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (!result.didOverflowWidth || fontSize <= minFontSize) {
                return@Text
            }

            val availableWidth = result.size.width.toFloat()
            val lineRight = try {
                result.getLineRight(0)
            } catch (_: Throwable) {
                0f
            }
            if (availableWidth <= 0f || lineRight <= 0f) {
                return@Text
            }

            val ratio = (availableWidth / lineRight).coerceIn(0f, 1f)
            val next = (fontSize.value * ratio).coerceAtLeast(minFontSize.value)
            if (next < fontSize.value - 0.1f) {
                fontSize = next.sp
            }
        }
    )
}

@Composable
internal fun LaunchContent(
    startAnimationFlow: StateFlow<Boolean>,
    recentRecordFlow: StateFlow<HistoryRecordItem?>,
    onAnimationCompleted: () -> Unit
) {
    val startAnimation by startAnimationFlow.collectAsState()
    val recentRecord by recentRecordFlow.collectAsState()
    val onAnimationCompletedState by rememberUpdatedState(onAnimationCompleted)

    val contentAlpha = remember { Animatable(0f) }
    var hasSentAnimationCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(startAnimation) {
        if (!startAnimation) {
            return@LaunchedEffect
        }
        hasSentAnimationCompleted = false
        contentAlpha.snapTo(0f)

        delay(200)
        contentAlpha.animateTo(1f, animationSpec = tween(durationMillis = 1000))

        if (!hasSentAnimationCompleted) {
            hasSentAnimationCompleted = true
            onAnimationCompletedState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // Use the downloaded Figma background rather than approximating it with generated gradients.
        Image(
            painter = painterResource(R.drawable.tr_bg_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            val (logo, appName, loadingBar, loadingText, recentCard) = createRefs()
            val logoTopGuideline = createGuidelineFromTop(0.198f)
            val bottomGuideline = createGuidelineFromBottom(0.12f)

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .constrainAs(logo) {
                        top.linkTo(logoTopGuideline)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .alpha(contentAlpha.value)
            ) {
                Image(
                    painter = painterResource(R.mipmap.tr_ic_logo_sq),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Image(
                    painter = painterResource(R.drawable.tr_splash_year_badge),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.BottomEnd).size(51.dp, 18.dp),
                    contentScale = ContentScale.FillBounds,
                )
            }

            Text(
                text = stringResource(id = R.string.app_name),
                color = SplashTitle,
                fontSize = 24.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_bold)),
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.constrainAs(appName) {
                    top.linkTo(logo.bottom, margin = 18.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.percent(0.78f)
                }.alpha(contentAlpha.value),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            recentRecord?.let {
                RecentRecordCard(
                    modifier = Modifier.constrainAs(recentCard) {
                        bottom.linkTo(loadingBar.top, margin = 26.dp)
                        start.linkTo(loadingBar.start)
                        end.linkTo(loadingBar.end)
                        width = Dimension.fillToConstraints
                    },
                    item = it
                )
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .height(6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .constrainAs(loadingBar) {
                        bottom.linkTo(loadingText.top, margin = 14.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.percent(0.81f)
                    },
                color = colorResource(R.color.tr_entry_primary),
                trackColor = colorResource(R.color.tr_entry_progress_track)
            )

            Text(
                text = stringResource(id = R.string.tr_loading_pure),
                color = colorResource(R.color.tr_entry_primary),
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_medium)),
                modifier = Modifier.constrainAs(loadingText) {
                    bottom.linkTo(bottomGuideline)
                    start.linkTo(loadingBar.start)
                    end.linkTo(loadingBar.end)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentRecordCard(
    modifier: Modifier = Modifier,
    item: HistoryRecordItem
) {
    val context = LocalContext.current

    val typeName = when (item.getRecordType()) {
        HistoryRecordItem.RecordType.BLOOD_PRESSURE -> stringResource(id = R.string.tr_blood_pressure)
        HistoryRecordItem.RecordType.BLOOD_SUGAR -> stringResource(id = R.string.tr_blood_suger)
        HistoryRecordItem.RecordType.HEART_RATE -> stringResource(id = R.string.tr_heart_rate)
        HistoryRecordItem.RecordType.BMI_RECORD -> stringResource(id = R.string.tr_bmi)
        else -> ""
    }

    val title = stringResource(id = R.string.tr_last_measurement, typeName)
    val levelText = item.getLevel(context)
    val timeText = DateTimeUtils.formatDateTime(item.getRecordTime())
    val statusValue = item.getStatus(context)

    val statusText = when {
        statusValue.isNullOrBlank() -> null
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
            "${stringResource(id = R.string.tr_pulse)}:$statusValue"
        }
        item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
            "${stringResource(id = R.string.tr_status)}:$statusValue"
        }
        else -> statusValue
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SplashRecordCard)
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, top = 10.dp, end = 16.dp, bottom = 12.dp)) {
            Text(
                text = title,
                color = SplashTitle,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.width(45.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.getPrimaryValue(),
                        color = SplashTitle,
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                        fontWeight = FontWeight.Black
                    )
                    val secondary = item.getSecondaryValue()
                    if (!secondary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = secondary,
                            color = SplashTitle,
                            fontSize = 18.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.inter_black)),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.getUnit(),
                        color = SplashMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_regular))
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(colorResource(R.color.tr_entry_primary))
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = levelText,
                        color = SplashTitle,
                        fontSize = 18.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_bold)),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!statusText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        AutoSizeSingleLineText(
                            text = statusText,
                            modifier = Modifier.fillMaxWidth(),
                            color = SplashTitle,
                            maxFontSize = 14.sp,
                            minFontSize = 6.sp,
                            fontFamily = FontFamily(Font(FrameworkR.font.inter_regular))
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeText,
                        color = SplashMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily(Font(FrameworkR.font.inter_regular)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
