package com.daily.health.manager.face.compose

import android.R.attr.textColor
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import kotlin.math.roundToInt


private fun LayoutCoordinates.toAndroidWindowRect(): Rect {
    val bounds = boundsInWindow()
    return Rect(
        bounds.left.roundToInt(),
        bounds.top.roundToInt(),
        bounds.right.roundToInt(),
        bounds.bottom.roundToInt(),
    )
}

@Immutable
data class HomeHeroUi(
    val title: String,
    val subtitle: String,
    val cta: String,
    val value: String,
    val valueUnit: String,
    val footerText: String,
)

@Immutable
sealed class HomeFeatureCardUi(
    open val title: String,
    open val backgroundColor: Color,
    open val titleWidth: Dp,
    open val illustrationOffsetX: Dp,
    open val illustrationOffsetY: Dp,
    open val illustrationWidth: Dp,
    open val illustrationHeight: Dp,
    @DrawableRes open val illustrationRes: Int,
    open val buttonColor: Color,
    open val guideAnchorOnButton: Boolean = false,
) {
    data class BloodPressure(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFE2FEFF),
        titleWidth = 82.dp,
        illustrationOffsetX = 88.dp,
        illustrationOffsetY = 5.dp,
        illustrationWidth = 72.dp,
        illustrationHeight = 72.dp,
        illustrationRes = R.mipmap.ht_home_card_bp,
        buttonColor = Color(0xFF20B9BF),
    )

    data class BloodSugar(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFFFE4E3),
        titleWidth = 78.dp,
        illustrationOffsetX = 88.dp,
        illustrationOffsetY = 6.dp,
        illustrationWidth = 62.dp,
        illustrationHeight = 68.dp,
        illustrationRes = R.mipmap.ht_home_card_bs,
        buttonColor = Color(0xFFFE5D5E),
        guideAnchorOnButton = true,
    )

    data class Cholesterol(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFFFF4E2),
        titleWidth = 104.dp,
        illustrationOffsetX = 100.dp,
        illustrationOffsetY = 15.dp,
        illustrationWidth = 60.dp,
        illustrationHeight = 60.dp,
        illustrationRes = R.mipmap.ht_home_card_cholesterol,
        buttonColor = Color(0xFFFFAA22),
    )

    data class Bmi(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFE4E9FF),
        titleWidth = 96.dp,
        illustrationOffsetX = 101.dp,
        illustrationOffsetY = 20.dp,
        illustrationWidth = 48.dp,
        illustrationHeight = 51.dp,
        illustrationRes = R.mipmap.ht_home_card_weight,
        buttonColor = Color(0xFF7790FF),
    )

    data class Hydrate(
        override val title: String,
        val currentValue: String,
        val targetValue: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFDFF5FF),
        titleWidth = 84.dp,
        illustrationOffsetX = 109.dp,
        illustrationOffsetY = 16.dp,
        illustrationWidth = 39.dp,
        illustrationHeight = 55.dp,
        illustrationRes = R.mipmap.ht_home_card_water,
        buttonColor = Color(0xFF3CBAF0),
    )

    data class StepCount(
        override val title: String,
        val stepsValue: String,
        val stepsUnit: String,
        val kcalValue: String,
        val kcalUnit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFFFEDE3),
        titleWidth = 82.dp,
        illustrationOffsetX = 87.dp,
        illustrationOffsetY = 18.dp,
        illustrationWidth = 63.dp,
        illustrationHeight = 47.dp,
        illustrationRes = R.mipmap.ht_home_card_step,
        buttonColor = Color(0xFFFF6E20),
    )
}

@Composable
fun HomeDashboardScreen(
    hero: HomeHeroUi,
    cards: List<HomeFeatureCardUi>,
    onHeartRateClick: () -> Unit,
    onBloodSugarCardClick: () -> Unit,
    onBloodSugarRecordClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
    onGuideAnchorBoundsChanged: (HomeGuideTarget, Rect) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroCard(
                hero = hero,
                onClick = onHeartRateClick,
                onCardBoundsChanged = {
                    onGuideAnchorBoundsChanged(HomeGuideTarget.HERO_CARD, it)
                },
                onButtonBoundsChanged = {
                    onGuideAnchorBoundsChanged(HomeGuideTarget.HERO_CTA, it)
                },
            )
        }
        items(cards.size / 2) { rowIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val left = cards[rowIndex * 2]
                val right = cards[rowIndex * 2 + 1]
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    card = left,
                    onCardClick = actionForCard(left, onBloodSugarCardClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                    onRecordClick = recordActionForCard(left, onBloodSugarRecordClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                    onCardBoundsChanged = guideCardTargetForCard(left)?.let { target ->
                        { rect -> onGuideAnchorBoundsChanged(target, rect) }
                    },
                    onRecordBoundsChanged = guideRecordTargetForCard(left)?.let { target ->
                        { rect -> onGuideAnchorBoundsChanged(target, rect) }
                    },
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    card = right,
                    onCardClick = actionForCard(right, onBloodSugarCardClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                    onRecordClick = recordActionForCard(right, onBloodSugarRecordClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                    onCardBoundsChanged = guideCardTargetForCard(right)?.let { target ->
                        { rect -> onGuideAnchorBoundsChanged(target, rect) }
                    },
                    onRecordBoundsChanged = guideRecordTargetForCard(right)?.let { target ->
                        { rect -> onGuideAnchorBoundsChanged(target, rect) }
                    },
                )
            }
        }
    }
}


private fun guideCardTargetForCard(card: HomeFeatureCardUi): HomeGuideTarget? = when (card) {
    is HomeFeatureCardUi.BloodPressure -> HomeGuideTarget.BLOOD_PRESSURE_CARD
    is HomeFeatureCardUi.BloodSugar -> HomeGuideTarget.BLOOD_SUGAR_CARD
    is HomeFeatureCardUi.Cholesterol,
    is HomeFeatureCardUi.Bmi,
    is HomeFeatureCardUi.Hydrate,
    is HomeFeatureCardUi.StepCount -> null
}

private fun guideRecordTargetForCard(card: HomeFeatureCardUi): HomeGuideTarget? = when (card) {
    is HomeFeatureCardUi.BloodPressure -> HomeGuideTarget.BLOOD_PRESSURE_RECORD
    is HomeFeatureCardUi.BloodSugar -> HomeGuideTarget.BLOOD_SUGAR_RECORD
    is HomeFeatureCardUi.Cholesterol,
    is HomeFeatureCardUi.Bmi,
    is HomeFeatureCardUi.Hydrate,
    is HomeFeatureCardUi.StepCount -> null
}

private fun actionForCard(
    card: HomeFeatureCardUi,
    onBloodSugarCardClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
): () -> Unit = when (card) {
    is HomeFeatureCardUi.BloodPressure -> onBloodPressureClick
    is HomeFeatureCardUi.BloodSugar -> onBloodSugarCardClick
    is HomeFeatureCardUi.Cholesterol -> onCholesterolClick
    is HomeFeatureCardUi.Bmi -> onBmiClick
    is HomeFeatureCardUi.Hydrate -> onHydrateClick
    is HomeFeatureCardUi.StepCount -> onStepCountClick
}

private fun recordActionForCard(
    card: HomeFeatureCardUi,
    onBloodSugarRecordClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
): () -> Unit = when (card) {
    is HomeFeatureCardUi.BloodPressure -> onBloodPressureClick
    is HomeFeatureCardUi.BloodSugar -> onBloodSugarRecordClick
    is HomeFeatureCardUi.Cholesterol -> onCholesterolClick
    is HomeFeatureCardUi.Bmi -> onBmiClick
    is HomeFeatureCardUi.Hydrate -> onHydrateClick
    is HomeFeatureCardUi.StepCount -> onStepCountClick
}

@Composable
private fun HeroConcentricCircles(
    modifier: Modifier,
    scale: Float,
) {
    Box(
        modifier = modifier
            .size(247.dp * scale)
    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .clip(CircleShape)
//                .background(Color.White.copy(alpha = 0.06f))
//        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(185.dp * scale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(125.dp * scale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
    }
}

@Composable
private fun HeroCard(
    hero: HomeHeroUi,
    onClick: () -> Unit,
    onCardBoundsChanged: ((Rect) -> Unit)? = null,
    onButtonBoundsChanged: ((Rect) -> Unit)? = null,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val scale = maxWidth / 343.dp
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp * scale),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp * scale)
                    .clip(RoundedCornerShape(12.dp * scale))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF6868), Color(0xFFFFA08A))
                        )
                    )
                    .clickable(onClick = onClick)
                    .onGloballyPositioned { coordinates ->
                        onCardBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                    }
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ht_home_hero_ecg),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .offset(170.dp * scale, 18.dp * scale)
                        .size(width = 173.dp * scale, height = 119.dp * scale)
                )
                HeroConcentricCircles(
                    modifier = Modifier.offset((-70).dp * scale, (-14).dp * scale),
                    scale = scale,
                )
                Image(
                    painter = painterResource(id = R.mipmap.ht_home_hero_heart),
                    contentDescription = null,
                    modifier = Modifier
                        .offset((-10).dp * scale, 24.dp * scale)
                        .size(width = 130.dp * scale, height = 110.dp * scale)
                )
                Box(
                    modifier = Modifier
                        .offset(56.dp * scale, 82.dp * scale)
                        .size(width = 42.dp * scale, height = 43.dp * scale)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Image(
                    painter = painterResource(id = R.drawable.hr_measure_ic_fingerprint),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(59.dp * scale, 86.dp * scale)
                        .size(35.dp * scale)
                )
                Text(
                    text = hero.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(123.dp * scale, 24.dp * scale)
                )
                Text(
                    text = hero.subtitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.offset(123.dp * scale, 45.dp * scale)
                )
                Box(
                    modifier = Modifier
                        .offset(126.dp * scale, 73.dp * scale)
                        .size(width = 160.dp * scale, height = 36.dp * scale)
                        .clip(RoundedCornerShape(44.dp * scale))
                        .background(Color.White)
                        .clickable(onClick = onClick)
                        .onGloballyPositioned { coordinates ->
                            onButtonBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-1).dp * scale),
                        horizontalArrangement = Arrangement.spacedBy(8.dp * scale),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hr_measure_ic_fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp * scale)
                        )
                        Text(
                            text = hero.cta,
                            color = Color(0xFFEF7167),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .offset(12.dp * scale, 137.dp * scale)
                        .size(width = 320.dp * scale, height = 30.dp * scale)
                        .clip(RoundedCornerShape(8.dp * scale))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = hero.footerText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 11.dp * scale, end = 116.dp * scale)
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 11.dp * scale),
                        horizontalArrangement = Arrangement.spacedBy(4.dp * scale),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hero.value,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = hero.valueUnit,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.offset(y = 2.dp * scale)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ht_ic_blood_suger_status_arrow),
                            colorFilter = ColorFilter.tint(Color.White),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(y = 2.dp * scale)
                                .size(width = 4.dp * scale, height = 8.dp * scale)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    card: HomeFeatureCardUi,
    onCardClick: () -> Unit,
    onRecordClick: () -> Unit,
    onCardBoundsChanged: ((Rect) -> Unit)? = null,
    onRecordBoundsChanged: ((Rect) -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = card.backgroundColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(162.dp)
                .clickable(onClick = onCardClick)
                .onGloballyPositioned { coordinates ->
                    onCardBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                }
        ) {
            Text(
                text = card.title,
                color = Color(0xFF333333),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                modifier = Modifier
                    .offset(13.dp, 16.dp)
                    .size(width = card.titleWidth, height = 56.dp)
            )
            Image(
                painter = painterResource(id = card.illustrationRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(card.illustrationOffsetX, card.illustrationOffsetY)
                    .size(width = card.illustrationWidth, height = card.illustrationHeight)
            )

            when (card) {
                is HomeFeatureCardUi.BloodPressure -> {
                    MetricValue(
                        modifier = Modifier.offset(11.dp, 88.dp),
                        value = card.value,
                        valueColor = Color(0xFF20B9BF),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.BloodSugar -> {
                    MetricValue(
                        modifier = Modifier.offset(11.dp, 88.dp),
                        value = card.value,
                        valueColor = Color(0xFFFE5D5E),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.Cholesterol -> {
                    MetricValue(
                        modifier = Modifier.offset(11.dp, 88.dp),
                        value = card.value,
                        valueColor = Color(0xFFFFAA22),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.Bmi -> {
                    MetricValue(
                        modifier = Modifier.offset(11.dp, 88.dp),
                        value = card.value,
                        valueColor = Color(0xFF7790FF),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.Hydrate -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF3CBAF0), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)) {
                                append(card.currentValue)
                            }
                            withStyle(SpanStyle(color = Color(0xFFAAB0B2), fontSize = 15.sp, fontWeight = FontWeight.Normal)) {
                                append("/")
                            }
                            withStyle(SpanStyle(color = Color(0xFFAAB0B2), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)) {
                                append(card.targetValue)
                            }
                            append(" ")
                            withStyle(SpanStyle(color = Color(0xFF999999), fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                                append(card.unit)
                            }
                        },
                        modifier = Modifier.offset(11.dp, 88.dp)
                    )
                }
                is HomeFeatureCardUi.StepCount -> {
                    MetricValue(
                        modifier = Modifier.offset(11.dp, 88.dp),
                        value = card.stepsValue,
                        valueColor = Color(0xFFFF6E20),
                        unit = card.stepsUnit,
                    )
                    MetricValue(
                        modifier = Modifier.offset(99.dp, 88.dp),
                        value = card.kcalValue,
                        valueColor = Color(0xFFFF6E20),
                        unit = card.kcalUnit,
                    )
                }
            }

            RecordPill(
                modifier = Modifier.offset(12.dp, 120.dp),
                textColor = card.buttonColor,
                onClick = onRecordClick,
                onBoundsChanged = onRecordBoundsChanged,
            )
        }
    }
}

@Composable
private fun MetricValue(
    modifier: Modifier,
    value: String,
    valueColor: Color,
    unit: String,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
            append(" ")
            withStyle(SpanStyle(color = Color(0xFF999999), fontSize = 12.sp, fontWeight = FontWeight.Normal)) {
                append(unit)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun RecordPill(
    modifier: Modifier,
    textColor: Color,
    onClick: () -> Unit,
    onBoundsChanged: ((Rect) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(width = 140.dp, height = 32.dp)
            .clip(RoundedCornerShape(35.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
            }
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.ht_record),
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Image(
                painter = painterResource(id = R.drawable.ht_ic_feature_arrow),
                contentDescription = null,
                colorFilter = ColorFilter.tint(textColor),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(
    name = "Home Dashboard",
    showBackground = true,
    backgroundColor = 0xFFF5F8FD,
    widthDp = 375,
    heightDp = 812,
)
@Composable
private fun HomeDashboardScreenPreview() {
    com.daily.health.manager.face.theme.HealthTrackerTheme {
        HomeDashboardScreen(
            hero = HomeHeroUi(
                title = "Heart Rate",
                subtitle = "Take the first measurement today!",
                cta = "Measure Now",
                value = "39",
                valueUnit = "Bpm",
                footerText = "15:25 Tue,Jan 13",
            ),
            cards = listOf(
                HomeFeatureCardUi.BloodPressure(
                    title = stringResource(R.string.ht_blood_pressure),
                    value = "100/45",
                    unit = "mmHg",
                ),
                HomeFeatureCardUi.BloodSugar(
                    title = "Blood Sugar",
                    value = "8.1",
                    unit = "mg/dL",
                ),
                HomeFeatureCardUi.Cholesterol(
                    title = "Cholesterol",
                    value = "178",
                    unit = "mg/dL",
                ),
                HomeFeatureCardUi.Bmi(
                    title = "Weight & BMI",
                    value = "65",
                    unit = "kg",
                ),
                HomeFeatureCardUi.Hydrate(
                    title = "Drink Water",
                    currentValue = "0",
                    targetValue = "2000",
                    unit = "ml",
                ),
                HomeFeatureCardUi.StepCount(
                    title = "Step Count",
                    stepsValue = "1758",
                    stepsUnit = "steps",
                    kcalValue = "100",
                    kcalUnit = "kcal",
                ),
            ),
            onHeartRateClick = {},
            onBloodSugarCardClick = {},
            onBloodSugarRecordClick = {},
            onBloodPressureClick = {},
            onCholesterolClick = {},
            onBmiClick = {},
            onHydrateClick = {},
            onStepCountClick = {},
            onGuideAnchorBoundsChanged = { _, _ -> },
        )
    }
}
