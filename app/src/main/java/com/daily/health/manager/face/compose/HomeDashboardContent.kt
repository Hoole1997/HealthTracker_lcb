package com.daily.health.manager.face.compose

import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
        illustrationRes = R.mipmap.tr_home_card_bp,
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
        illustrationRes = R.mipmap.tr_home_card_bs,
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
        illustrationRes = R.mipmap.tr_home_card_cholesterol,
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
        illustrationRes = R.mipmap.tr_home_card_weight,
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
        illustrationRes = R.mipmap.tr_home_card_water,
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
        illustrationRes = R.mipmap.tr_home_card_step,
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
    val orderedCards = remember(cards) { cards.inHomeDisplayOrder() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F3FC)),
        contentPadding = PaddingValues(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        items(
            items = orderedCards,
            key = { it.homeCardKey() },
        ) { card ->
            FeatureCard(
                modifier = Modifier.fillMaxWidth(),
                card = card,
                onCardClick = actionForCard(card, onBloodSugarCardClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                onRecordClick = recordActionForCard(card, onBloodSugarRecordClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                onCardBoundsChanged = guideCardTargetForCard(card)?.let { target ->
                    { rect -> onGuideAnchorBoundsChanged(target, rect) }
                },
                onRecordBoundsChanged = guideRecordTargetForCard(card)?.let { target ->
                    { rect -> onGuideAnchorBoundsChanged(target, rect) }
                },
            )
        }
    }
}

private fun HomeFeatureCardUi.homeCardKey(): String = when (this) {
    is HomeFeatureCardUi.BloodPressure -> "blood_pressure"
    is HomeFeatureCardUi.BloodSugar -> "blood_sugar"
    is HomeFeatureCardUi.Bmi -> "bmi"
    is HomeFeatureCardUi.Cholesterol -> "cholesterol"
    is HomeFeatureCardUi.StepCount -> "step_count"
    is HomeFeatureCardUi.Hydrate -> "hydrate"
}

private fun List<HomeFeatureCardUi>.inHomeDisplayOrder(): List<HomeFeatureCardUi> {
    return sortedBy { card ->
        when (card) {
            is HomeFeatureCardUi.BloodPressure -> 0
            is HomeFeatureCardUi.BloodSugar -> 1
            is HomeFeatureCardUi.Bmi -> 2
            is HomeFeatureCardUi.Cholesterol -> 3
            is HomeFeatureCardUi.StepCount -> 4
            is HomeFeatureCardUi.Hydrate -> 5
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
        val cardWidth = maxWidth
        val scale = cardWidth / 342.dp
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp * scale),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFD)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp * scale)
                    .clickable(onClick = onClick)
                    .onGloballyPositioned { coordinates ->
                        onCardBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_home_heart_rate),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .offset(23.dp * scale, 27.dp * scale)
                        .size(width = 99.dp * scale, height = 84.dp * scale)
                )
                Column(
                    modifier = Modifier
                        .offset(140.dp * scale, 15.dp * scale)
                        .width((cardWidth - 156.dp * scale).coerceAtLeast(120.dp)),
                    verticalArrangement = Arrangement.spacedBy(5.dp * scale),
                ) {
                    Text(
                        text = hero.title,
                        color = Color(0xFF333333),
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MetricValue(
                        modifier = Modifier,
                        value = hero.value,
                        valueColor = Color(0xFF333333),
                        unit = hero.valueUnit,
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(126.dp * scale, 75.dp * scale)
                        .size(width = 194.dp * scale, height = 50.dp * scale)
                        .clip(RoundedCornerShape(43.dp * scale))
                        .background(Color(0xFF1B1D2C))
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
                            painter = painterResource(id = R.drawable.ic_home_fingerprint),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp * scale)
                        )
                        Text(
                            text = stringResource(id = R.string.tr_measure_title),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .offset(12.dp * scale, 142.dp * scale)
                        .size(width = 318.dp * scale, height = 33.dp * scale)
                        .clip(RoundedCornerShape(8.dp * scale))
                        .background(Color(0xFFEBF0FF))
                        .clickable(onClick = onClick)
                ) {
                    Text(
                        text = hero.footerText,
                        color = Color(0xFF333333),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 9.dp * scale, end = 112.dp * scale)
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp * scale),
                        horizontalArrangement = Arrangement.spacedBy(4.dp * scale),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hero.value,
                            color = Color(0xFF333333),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = hero.valueUnit,
                            color = Color(0xFF333333),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.offset(y = 2.dp * scale)
                        )
                    }
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_home_chevron_right),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(316.dp * scale, 154.dp * scale)
                        .size(width = 4.dp * scale, height = 8.dp * scale)
                )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clickable(onClick = onCardClick)
                .onGloballyPositioned { coordinates ->
                    onCardBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                }
        ) {
            Image(
                painter = painterResource(id = card.iconRes()),
                contentDescription = null,
                modifier = Modifier
                    .offset(16.dp, 16.dp)
                    .size(24.dp)
            )
            Text(
                text = card.title,
                color = Color(0xFF333333),
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(46.dp, 17.dp)
                    .width(160.dp)
            )
            MetricIllustration(card = card)

            when (card) {
                is HomeFeatureCardUi.BloodPressure -> {
                    MetricValue(
                        modifier = Modifier.offset(16.dp, 58.dp),
                        value = card.value,
                        valueColor = Color(0xFF333333),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.BloodSugar -> {
                    MetricValue(
                        modifier = Modifier.offset(16.dp, 58.dp),
                        value = card.value,
                        valueColor = Color(0xFF333333),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.Cholesterol -> {
                    MetricValue(
                        modifier = Modifier.offset(16.dp, 58.dp),
                        value = card.value,
                        valueColor = Color(0xFF333333),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.Bmi -> {
                    MetricValue(
                        modifier = Modifier.offset(16.dp, 58.dp),
                        value = card.value,
                        valueColor = Color(0xFF333333),
                        unit = card.unit,
                    )
                }
                is HomeFeatureCardUi.Hydrate -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF333333), fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                                append(card.currentValue)
                            }
                            withStyle(SpanStyle(color = Color(0xFF333333), fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                                append("/")
                            }
                            withStyle(SpanStyle(color = Color(0xFF333333), fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                                append(card.targetValue)
                            }
                            append(" ")
                            withStyle(SpanStyle(color = Color(0xFF999999), fontSize = 13.sp, fontWeight = FontWeight.Normal)) {
                                append(card.unit)
                            }
                        },
                        modifier = Modifier.offset(16.dp, 58.dp)
                    )
                }
                is HomeFeatureCardUi.StepCount -> {
                    MetricValue(
                        modifier = Modifier.offset(16.dp, 58.dp),
                        value = card.stepsValue,
                        valueColor = Color(0xFF333333),
                        unit = card.stepsUnit,
                    )
                }
            }

            RecordPill(
                modifier = Modifier.offset(16.dp, 90.dp),
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
            withStyle(SpanStyle(color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                append(value)
            }
            append(" ")
            withStyle(SpanStyle(color = Color(0xFF999999), fontSize = 13.sp, fontWeight = FontWeight.Normal)) {
                append(unit)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun RecordPill(
    modifier: Modifier,
    onClick: () -> Unit,
    onBoundsChanged: ((Rect) -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .size(width = 86.dp, height = 30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF1B1D2C))
            .clickable(onClick = onClick)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
            }
    ) {
        Text(
            text = stringResource(id = R.string.tr_record),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@DrawableRes
private fun HomeFeatureCardUi.iconRes(): Int = when (this) {
    is HomeFeatureCardUi.BloodPressure -> R.drawable.ic_home_blood_pressure
    is HomeFeatureCardUi.BloodSugar -> R.drawable.ic_home_blood_sugar
    is HomeFeatureCardUi.Cholesterol -> R.drawable.ic_home_cholesterol
    is HomeFeatureCardUi.Bmi -> R.drawable.ic_home_weight
    is HomeFeatureCardUi.Hydrate -> R.drawable.ic_home_water
    is HomeFeatureCardUi.StepCount -> R.drawable.ic_home_steps
}

@Composable
private fun BoxScope.MetricIllustration(card: HomeFeatureCardUi) {
    val spec = card.illustrationSpec()
    if (card is HomeFeatureCardUi.StepCount) {
        Image(
            painter = painterResource(id = R.drawable.ic_home_step_curve),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 34.dp)
                .size(width = 120.dp, height = 81.dp)
        )
    }
    Image(
        painter = painterResource(id = spec.resId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = (-12).dp, y = spec.top)
            .size(width = spec.width, height = spec.height)
    )
}

private data class MetricIllustrationSpec(
    @DrawableRes val resId: Int,
    val width: Dp,
    val height: Dp,
    val top: Dp,
)

private fun HomeFeatureCardUi.illustrationSpec(): MetricIllustrationSpec = when (this) {
    is HomeFeatureCardUi.BloodPressure -> MetricIllustrationSpec(R.drawable.img_home_blood_pressure, 135.dp, 104.dp, 14.dp)
    is HomeFeatureCardUi.BloodSugar -> MetricIllustrationSpec(R.drawable.img_home_blood_sugar, 118.dp, 101.dp, 16.dp)
    is HomeFeatureCardUi.Cholesterol -> MetricIllustrationSpec(R.drawable.img_home_cholesterol, 119.dp, 116.dp, 7.dp)
    is HomeFeatureCardUi.Bmi -> MetricIllustrationSpec(R.drawable.img_home_weight_bmi, 133.dp, 112.dp, 9.dp)
    is HomeFeatureCardUi.Hydrate -> MetricIllustrationSpec(R.drawable.img_home_drink_water, 124.dp, 104.dp, 15.dp)
    is HomeFeatureCardUi.StepCount -> MetricIllustrationSpec(R.drawable.img_home_step_count, 116.dp, 121.dp, 0.dp)
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
                    title = stringResource(R.string.tr_blood_pressure),
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
