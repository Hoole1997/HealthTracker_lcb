package com.daily.health.manager.face.compose

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import kotlin.math.roundToInt
import java.util.Locale

private fun LayoutCoordinates.toAndroidWindowRect(): Rect {
    val bounds = boundsInWindow()
    return Rect(
        bounds.left.roundToInt(),
        bounds.top.roundToInt(),
        bounds.right.roundToInt(),
        bounds.bottom.roundToInt(),
    )
}

private fun Rect.centerRect(
    widthFraction: Float = 0.48f,
    heightFraction: Float = 0.46f,
): Rect {
    val rectWidth = (width() * widthFraction).roundToInt().coerceAtLeast(1)
    val rectHeight = (height() * heightFraction).roundToInt().coerceAtLeast(1)
    val left = centerX() - rectWidth / 2
    val top = centerY() - rectHeight / 2
    return Rect(left, top, left + rectWidth, top + rectHeight)
}

@Immutable
data class HomeHeroUi(
    val title: String,
    val cta: String,
    val value: String,
    val valueUnit: String,
    val footerText: String,
)

@Immutable
sealed class HomeFeatureCardUi(
    open val title: String,
    open val backgroundColor: Color,
    open val titleColor: Color,
    open val titleWidth: Dp,
    open val illustrationOffsetX: Dp,
    open val illustrationOffsetY: Dp,
    open val illustrationWidth: Dp,
    open val illustrationHeight: Dp,
) {
    data class BloodSugar(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFFFE9E8),
        titleColor = Color(0xFFEE4F74),
        titleWidth = 133.dp,
        illustrationOffsetX = 83.dp,
        illustrationOffsetY = 46.dp,
        illustrationWidth = 78.dp,
        illustrationHeight = 78.dp,
    )

    data class BloodPressure(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFDEFAFF),
        titleColor = Color(0xFF34CDD2),
        titleWidth = 141.dp,
        illustrationOffsetX = 80.dp,
        illustrationOffsetY = 46.dp,
        illustrationWidth = 78.dp,
        illustrationHeight = 78.dp,
    )

    data class Bmi(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFF7E9FF),
        titleColor = Color(0xFFBB89DE),
        titleWidth = 145.dp,
        illustrationOffsetX = 91.dp,
        illustrationOffsetY = 58.dp,
        illustrationWidth = 62.dp,
        illustrationHeight = 62.dp,
    )

    data class Cholesterol(
        override val title: String,
        val value: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFFFF4E2),
        titleColor = Color(0xFFFFAE2E),
        titleWidth = 133.dp,
        illustrationOffsetX = 83.dp,
        illustrationOffsetY = 50.dp,
        illustrationWidth = 78.dp,
        illustrationHeight = 78.dp,
    )

    data class StepCount(
        override val title: String,
        val stepsValue: String,
        val stepsUnit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFFFEBD8),
        titleColor = Color(0xFFFF6A00),
        titleWidth = 107.dp,
        illustrationOffsetX = 81.dp,
        illustrationOffsetY = 49.dp,
        illustrationWidth = 78.dp,
        illustrationHeight = 78.dp,
    )

    data class Hydrate(
        override val title: String,
        val currentValue: String,
        val targetValue: String,
        val unit: String,
    ) : HomeFeatureCardUi(
        title = title,
        backgroundColor = Color(0xFFE8F9FF),
        titleColor = Color(0xFF04B8FE),
        titleWidth = 137.dp,
        illustrationOffsetX = 83.dp,
        illustrationOffsetY = 51.dp,
        illustrationWidth = 78.dp,
        illustrationHeight = 78.dp,
    )
}

@Composable
fun HomeDashboardScreen(
    hero: HomeHeroUi,
    cards: List<HomeFeatureCardUi>,
    onHeartRateClick: () -> Unit,
    onBloodSugarClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
    onGuideAnchorBoundsChanged: (HomeGuideTarget, Rect) -> Unit,
) {
    val cardRows = cards.chunked(2)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
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

        items(cardRows.size) { rowIndex ->
            val row = cardRows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                row.forEach { card ->
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        card = card,
                        onCardClick = actionForCard(
                            card = card,
                            onBloodSugarClick = onBloodSugarClick,
                            onBloodPressureClick = onBloodPressureClick,
                            onCholesterolClick = onCholesterolClick,
                            onBmiClick = onBmiClick,
                            onHydrateClick = onHydrateClick,
                            onStepCountClick = onStepCountClick,
                        ),
                        onCardBoundsChanged = { rect ->
                            guideCardTargetForCard(card)?.let { target ->
                                onGuideAnchorBoundsChanged(target, rect)
                            }
                            guideActionTargetForCard(card)?.let { target ->
                                onGuideAnchorBoundsChanged(target, rect.centerRect())
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun guideCardTargetForCard(card: HomeFeatureCardUi): HomeGuideTarget? = when (card) {
    is HomeFeatureCardUi.BloodPressure -> HomeGuideTarget.BLOOD_PRESSURE_CARD
    is HomeFeatureCardUi.BloodSugar -> HomeGuideTarget.BLOOD_SUGAR_CARD
    is HomeFeatureCardUi.Bmi,
    is HomeFeatureCardUi.Cholesterol,
    is HomeFeatureCardUi.Hydrate,
    is HomeFeatureCardUi.StepCount -> null
}

private fun guideActionTargetForCard(card: HomeFeatureCardUi): HomeGuideTarget? = when (card) {
    is HomeFeatureCardUi.BloodPressure -> HomeGuideTarget.BLOOD_PRESSURE_RECORD
    is HomeFeatureCardUi.BloodSugar -> HomeGuideTarget.BLOOD_SUGAR_RECORD
    is HomeFeatureCardUi.Bmi,
    is HomeFeatureCardUi.Cholesterol,
    is HomeFeatureCardUi.Hydrate,
    is HomeFeatureCardUi.StepCount -> null
}

private fun actionForCard(
    card: HomeFeatureCardUi,
    onBloodSugarClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
): () -> Unit = when (card) {
    is HomeFeatureCardUi.BloodSugar -> onBloodSugarClick
    is HomeFeatureCardUi.BloodPressure -> onBloodPressureClick
    is HomeFeatureCardUi.Bmi -> onBmiClick
    is HomeFeatureCardUi.Cholesterol -> onCholesterolClick
    is HomeFeatureCardUi.StepCount -> onStepCountClick
    is HomeFeatureCardUi.Hydrate -> onHydrateClick
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
        val cardShape = RoundedCornerShape(20.dp * scale)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(184.dp * scale)
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFCE4C2), Color(0xFFFBCDB8)),
                            start = Offset.Zero,
                            end = Offset(343f, 184f),
                        )
                    )
                    .clickable(onClick = onClick)
                    .onGloballyPositioned { coordinates ->
                        onCardBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                    }
            ) {
                Box(
                    modifier = Modifier
                        .offset(125.dp * scale, 38.dp * scale)
                        .size(234.dp * scale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .offset((-6).dp * scale, (-129).dp * scale)
                        .size(234.dp * scale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                )
                Image(
                    painter = painterResource(id = R.mipmap.tr_home_hero_shadow_figma),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(37.dp * scale, 119.dp * scale)
                        .size(width = 47.dp * scale, height = 10.dp * scale)
                )
                HeartRateHeroIllustration(
                    modifier = Modifier
                        .offset(16.dp * scale, 13.dp * scale)
                        .size(96.dp * scale)
                )

                Text(
                    text = hero.title,
                    color = Color(0xFFFF7C3F),
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(126.dp * scale, 14.dp * scale)
                )
                HeroMetricLine(
                    modifier = Modifier
                        .offset(126.dp * scale, 45.dp * scale)
                        .width(98.dp * scale),
                    value = hero.value,
                    unit = hero.valueUnit,
                )

                Box(
                    modifier = Modifier
                        .offset(126.dp * scale, 75.dp * scale)
                        .size(width = 194.dp * scale, height = 50.dp * scale)
                        .clip(RoundedCornerShape(12.dp * scale))
                        .background(Color(0xFFFF7C3F))
                        .clickable(onClick = onClick)
                        .onGloballyPositioned { coordinates ->
                            onButtonBoundsChanged?.invoke(coordinates.toAndroidWindowRect())
                        }
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(8.dp * scale),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.hr_measure_ic_fingerprint),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(24.dp * scale)
                        )
                        Text(
                            text = hero.cta,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(12.dp * scale, 142.dp * scale)
                        .size(width = 320.dp * scale, height = 33.dp * scale)
                        .clip(RoundedCornerShape(26.dp * scale))
                        .background(Color.White)
                ) {
                    Text(
                        text = hero.footerText,
                        color = Color(0xFFC07D5E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 11.dp * scale, end = 116.dp * scale)
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp * scale),
                        horizontalArrangement = Arrangement.spacedBy(4.dp * scale),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = hero.value,
                            color = Color(0xFFEF8756),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = hero.valueUnit.asFooterUnit(),
                            color = Color(0xFFEF8756),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.offset(y = 2.dp * scale)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.tr_ic_blood_suger_status_arrow),
                            colorFilter = ColorFilter.tint(Color(0xFFEF8756)),
                            contentDescription = null,
                            modifier = Modifier
                                .offset(y = 1.dp * scale)
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
    onCardBoundsChanged: (Rect) -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = card.backgroundColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clickable(onClick = onCardClick)
                .onGloballyPositioned { coordinates ->
                    onCardBoundsChanged(coordinates.toAndroidWindowRect())
                }
        ) {
            Text(
                text = card.title,
                color = card.titleColor,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier
                    .offset(15.dp, 12.dp)
                    .width(card.titleWidth)
            )

            FeatureIllustration(
                card = card,
                modifier = Modifier
                    .offset(card.illustrationOffsetX, card.illustrationOffsetY)
                    .size(width = card.illustrationWidth, height = card.illustrationHeight)
            )

            when (card) {
                is HomeFeatureCardUi.BloodSugar -> {
                    MetricValue(
                        modifier = Modifier.offset(15.dp, 84.dp),
                        value = card.value,
                        valueColor = Color(0xFFEE4F74),
                        unit = card.unit,
                        unitColor = Color(0xFFD57B87),
                    )
                }

                is HomeFeatureCardUi.BloodPressure -> {
                    MetricValue(
                        modifier = Modifier.offset(15.dp, 84.dp),
                        value = card.value,
                        valueColor = Color(0xFF28A5B3),
                        unit = card.unit,
                        unitColor = Color(0xFF74C2C9),
                    )
                }

                is HomeFeatureCardUi.Bmi -> {
                    MetricValue(
                        modifier = Modifier.offset(15.dp, 84.dp),
                        value = card.value,
                        valueColor = Color(0xFFBB89DE),
                        unit = card.unit,
                        unitColor = Color(0xFF9F87B1),
                    )
                }

                is HomeFeatureCardUi.Cholesterol -> {
                    MetricValue(
                        modifier = Modifier.offset(15.dp, 84.dp),
                        value = card.value,
                        valueColor = Color(0xFFFFAE2E),
                        unit = card.unit,
                        unitColor = Color(0xFFD9B781),
                    )
                }

                is HomeFeatureCardUi.StepCount -> {
                    MetricValue(
                        modifier = Modifier.offset(15.dp, 84.dp),
                        value = card.stepsValue,
                        valueColor = Color(0xFFFF6A00),
                        unit = card.stepsUnit,
                        unitColor = Color(0xFFEA995F),
                    )
                }

                is HomeFeatureCardUi.Hydrate -> {
                    HydrateMetricValue(
                        modifier = Modifier.offset(15.dp, 84.dp),
                        currentValue = card.currentValue,
                        targetValue = card.targetValue,
                        unit = card.unit,
                    )
                }
            }
        }
    }
}

private fun String.asFooterUnit(): String {
    return lowercase(Locale.ROOT).replaceFirstChar { firstChar ->
        if (firstChar.isLowerCase()) {
            firstChar.titlecase(Locale.ROOT)
        } else {
            firstChar.toString()
        }
    }
}

@Composable
private fun HeroMetricLine(
    modifier: Modifier,
    value: String,
    unit: String,
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = Color(0xFFFF7C3F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(value)
                append(" ")
            }
            withStyle(
                SpanStyle(
                    color = Color(0xFFD7A07D),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            ) {
                append(unit)
            }
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FeatureIllustration(
    card: HomeFeatureCardUi,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = illustrationResForCard(card)),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
private fun HeartRateHeroIllustration(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.mipmap.tr_home_hero_heart_figma),
        contentDescription = null,
        modifier = modifier,
    )
}

private fun illustrationResForCard(card: HomeFeatureCardUi): Int = when (card) {
    is HomeFeatureCardUi.BloodSugar -> R.mipmap.tr_home_card_bs_figma
    is HomeFeatureCardUi.BloodPressure -> R.mipmap.tr_home_card_bp_figma
    is HomeFeatureCardUi.Bmi -> R.mipmap.tr_home_card_weight_figma
    is HomeFeatureCardUi.Cholesterol -> R.mipmap.tr_home_card_cholesterol_figma
    is HomeFeatureCardUi.StepCount -> R.mipmap.tr_home_card_step_figma
    is HomeFeatureCardUi.Hydrate -> R.mipmap.tr_home_card_water_figma
}

@Composable
private fun BloodSugarIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawPath(
            path = dropletPath(
                left = size.width * 0.10f,
                top = size.height * 0.32f,
                width = size.width * 0.34f,
                height = size.height * 0.44f,
            ),
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8B0F4E), Color(0xFFC81B69)),
                start = Offset(size.width * 0.10f, size.height * 0.32f),
                end = Offset(size.width * 0.44f, size.height * 0.78f),
            ),
        )
        drawPath(
            path = dropletPath(
                left = size.width * 0.32f,
                top = size.height * 0.08f,
                width = size.width * 0.54f,
                height = size.height * 0.82f,
            ),
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF4E8A), Color(0xFFC10059), Color(0xFF8E003B)),
                start = Offset(size.width * 0.42f, size.height * 0.12f),
                end = Offset(size.width * 0.82f, size.height * 0.86f),
            ),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(size.width * 0.47f, size.height * 0.18f),
            size = Size(size.width * 0.18f, size.height * 0.22f),
        )
    }
}

@Composable
private fun BloodPressureIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cable = Path().apply {
                moveTo(size.width * 0.55f, size.height * 0.18f)
                cubicTo(
                    size.width * 0.30f,
                    size.height * 0.02f,
                    size.width * 0.18f,
                    size.height * 0.30f,
                    size.width * 0.42f,
                    size.height * 0.40f,
                )
                cubicTo(
                    size.width * 0.62f,
                    size.height * 0.50f,
                    size.width * 0.70f,
                    size.height * 0.24f,
                    size.width * 0.58f,
                    size.height * 0.14f,
                )
            }
            drawPath(
                path = cable,
                color = Color(0xFF54D3DA),
                style = Stroke(width = size.minDimension * 0.07f, cap = StrokeCap.Round),
            )
            drawCircle(
                color = Color(0xFFA4EFF0),
                radius = size.minDimension * 0.10f,
                center = Offset(size.width * 0.52f, size.height * 0.13f),
            )
            drawCircle(
                color = Color(0xFFFFFFFF),
                radius = size.minDimension * 0.05f,
                center = Offset(size.width * 0.52f, size.height * 0.13f),
            )
        }

        Box(
            modifier = Modifier
                .offset(6.dp, 29.dp)
                .size(width = 26.dp, height = 31.dp)
                .rotate(-4f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF4A5664), Color(0xFF232C37)),
                    )
                )
        )

        Box(
            modifier = Modifier
                .offset(26.dp, 20.dp)
                .size(width = 30.dp, height = 31.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFF9FEFE), Color(0xFFD2F2F3)),
                    )
                )
                .border(1.5.dp, Color(0xFF93DFE4), RoundedCornerShape(9.dp))
        ) {
            Box(
                modifier = Modifier
                    .offset(4.dp, 3.dp)
                    .size(width = 20.dp, height = 15.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF273442))
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0xFF5CD7DE),
                    start = Offset(size.width * 0.32f, size.height * 0.80f),
                    end = Offset(size.width * 0.68f, size.height * 0.80f),
                    strokeWidth = size.minDimension * 0.06f,
                    cap = StrokeCap.Round,
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(48.dp, 32.dp)
                .size(width = 19.dp, height = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFC4F2F4), Color(0xFF93E4E8)),
                    )
                )
                .border(1.dp, Color(0xFF7DD6DB), RoundedCornerShape(6.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * 0.11f
                val x = size.width * 0.34f
                repeat(3) { index ->
                    val y = size.height * (0.22f + index * 0.24f)
                    drawLine(
                        color = Color(0xFF3E6773),
                        start = Offset(x, y),
                        end = Offset(size.width * 0.78f, y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
                repeat(3) { index ->
                    drawCircle(
                        color = Color(0xFF4CCFD6),
                        radius = size.minDimension * 0.10f,
                        center = Offset(size.width * (0.25f + index * 0.23f), size.height * 0.78f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BmiIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(x = 1.dp, y = 2.dp)
                .size(58.dp)
                .rotate(-8f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF2E3FF), Color(0xFFB58DFF)),
                    )
                )
                .border(2.dp, Color(0xFF6A556E), RoundedCornerShape(16.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 6.dp)
                    .size(width = 34.dp, height = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFDF8FF))
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0xFF7E5568),
                    start = Offset(size.width * 0.50f, size.height * 0.12f),
                    end = Offset(size.width * 0.56f, size.height * 0.04f),
                    strokeWidth = size.minDimension * 0.045f,
                    cap = StrokeCap.Round,
                )
                drawOval(
                    color = Color(0xFF825C8E),
                    topLeft = Offset(size.width * 0.20f, size.height * 0.58f),
                    size = Size(size.width * 0.14f, size.height * 0.22f),
                )
                drawOval(
                    color = Color(0xFF825C8E),
                    topLeft = Offset(size.width * 0.64f, size.height * 0.58f),
                    size = Size(size.width * 0.14f, size.height * 0.22f),
                )
                drawCircle(
                    color = Color(0xFF5E3F6C),
                    radius = size.minDimension * 0.045f,
                    center = Offset(size.width * 0.30f, size.height * 0.72f),
                )
                drawCircle(
                    color = Color(0xFF5E3F6C),
                    radius = size.minDimension * 0.045f,
                    center = Offset(size.width * 0.70f, size.height * 0.72f),
                )
            }
        }
    }
}

@Composable
private fun CholesterolIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val torso = torsoPath(size.width, size.height)
        drawPath(
            path = torso,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFF8D5BB), Color(0xFFF0B98D), Color(0xFFDA9A6E)),
                startY = size.height * 0.08f,
                endY = size.height * 0.94f,
            ),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(size.width * 0.28f, size.height * 0.15f),
            size = Size(size.width * 0.20f, size.height * 0.18f),
        )

        val organCenter = Offset(size.width * 0.60f, size.height * 0.67f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD86A), Color(0xFFF15A2C), Color(0xFFD22B22)),
                center = organCenter,
                radius = size.minDimension * 0.18f,
            ),
            radius = size.minDimension * 0.17f,
            center = organCenter,
        )
        val vessel = Path().apply {
            moveTo(size.width * 0.58f, size.height * 0.56f)
            cubicTo(
                size.width * 0.50f,
                size.height * 0.60f,
                size.width * 0.50f,
                size.height * 0.74f,
                size.width * 0.56f,
                size.height * 0.78f,
            )
            cubicTo(
                size.width * 0.60f,
                size.height * 0.71f,
                size.width * 0.66f,
                size.height * 0.62f,
                size.width * 0.70f,
                size.height * 0.60f,
            )
        }
        drawPath(
            path = vessel,
            color = Color(0xFFFFF1A5),
            style = Stroke(width = size.minDimension * 0.05f, cap = StrokeCap.Round),
        )
        drawPath(
            path = vessel,
            color = Color(0xFFD53B1C),
            style = Stroke(width = size.minDimension * 0.02f, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun HydrateIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset(x = 7.dp, y = 10.dp)
                .size(width = 33.dp, height = 49.dp)
                .rotate(14f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF75D8FF), Color(0xFF12A8F8)),
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = 18.dp, y = 1.dp)
                .size(width = 12.dp, height = 16.dp)
                .rotate(14f)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF56CCFF), Color(0xFF149CF3)),
                    )
                )
        )
        Box(
            modifier = Modifier
                .offset(x = 16.dp, y = 0.dp)
                .size(width = 15.dp, height = 5.dp)
                .rotate(14f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF1D9BF4))
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = Color(0xFF3CBFFF),
                start = Offset(size.width * 0.62f, size.height * 0.18f),
                end = Offset(size.width * 0.80f, size.height * 0.27f),
                strokeWidth = size.minDimension * 0.045f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.36f),
                start = Offset(size.width * 0.50f, size.height * 0.28f),
                end = Offset(size.width * 0.60f, size.height * 0.70f),
                strokeWidth = size.minDimension * 0.05f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF0D91EC).copy(alpha = 0.35f),
                start = Offset(size.width * 0.44f, size.height * 0.42f),
                end = Offset(size.width * 0.68f, size.height * 0.46f),
                strokeWidth = size.minDimension * 0.035f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0xFF0D91EC).copy(alpha = 0.35f),
                start = Offset(size.width * 0.46f, size.height * 0.54f),
                end = Offset(size.width * 0.70f, size.height * 0.58f),
                strokeWidth = size.minDimension * 0.035f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StepCountIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sole = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.72f)
            lineTo(size.width * 0.83f, size.height * 0.72f)
            cubicTo(
                size.width * 0.90f,
                size.height * 0.72f,
                size.width * 0.93f,
                size.height * 0.76f,
                size.width * 0.93f,
                size.height * 0.81f,
            )
            lineTo(size.width * 0.25f, size.height * 0.81f)
            cubicTo(
                size.width * 0.15f,
                size.height * 0.81f,
                size.width * 0.10f,
                size.height * 0.77f,
                size.width * 0.10f,
                size.height * 0.72f,
            )
            close()
        }
        drawPath(path = sole, color = Color(0xFFFCE7DB))

        val upper = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.68f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.48f,
                size.width * 0.41f,
                size.height * 0.34f,
                size.width * 0.56f,
                size.height * 0.36f,
            )
            lineTo(size.width * 0.72f, size.height * 0.40f)
            cubicTo(
                size.width * 0.80f,
                size.height * 0.42f,
                size.width * 0.86f,
                size.height * 0.48f,
                size.width * 0.89f,
                size.height * 0.56f,
            )
            lineTo(size.width * 0.84f, size.height * 0.68f)
            close()
        }
        drawPath(path = upper, color = Color.White)

        val toe = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.68f)
            cubicTo(
                size.width * 0.23f,
                size.height * 0.55f,
                size.width * 0.34f,
                size.height * 0.45f,
                size.width * 0.45f,
                size.height * 0.43f,
            )
            lineTo(size.width * 0.45f, size.height * 0.68f)
            close()
        }
        drawPath(path = toe, color = Color(0xFFFFA057))

        val heel = Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.36f)
            lineTo(size.width * 0.73f, size.height * 0.39f)
            cubicTo(
                size.width * 0.83f,
                size.height * 0.41f,
                size.width * 0.88f,
                size.height * 0.48f,
                size.width * 0.88f,
                size.height * 0.58f,
            )
            lineTo(size.width * 0.80f, size.height * 0.68f)
            lineTo(size.width * 0.62f, size.height * 0.68f)
            close()
        }
        drawPath(path = heel, color = Color(0xFFF68A41))

        val sideLogo = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.48f)
            cubicTo(
                size.width * 0.56f,
                size.height * 0.45f,
                size.width * 0.63f,
                size.height * 0.48f,
                size.width * 0.69f,
                size.height * 0.54f,
            )
            cubicTo(
                size.width * 0.61f,
                size.height * 0.56f,
                size.width * 0.54f,
                size.height * 0.58f,
                size.width * 0.48f,
                size.height * 0.56f,
            )
            close()
        }
        drawPath(path = sideLogo, color = Color(0xFFFFA65B))

        repeat(3) { index ->
            val startX = size.width * (0.42f + index * 0.06f)
            val startY = size.height * (0.44f + index * 0.05f)
            drawLine(
                color = Color(0xFFFFF6EF),
                start = Offset(startX, startY),
                end = Offset(startX - size.width * 0.10f, startY),
                strokeWidth = size.minDimension * 0.04f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun heartPath(width: Float, height: Float): Path {
    return Path().apply {
        moveTo(width * 0.50f, height * 0.82f)
        cubicTo(width * 0.18f, height * 0.60f, width * 0.08f, height * 0.34f, width * 0.21f, height * 0.18f)
        cubicTo(width * 0.32f, height * 0.06f, width * 0.47f, height * 0.12f, width * 0.50f, height * 0.25f)
        cubicTo(width * 0.53f, height * 0.12f, width * 0.68f, height * 0.06f, width * 0.79f, height * 0.18f)
        cubicTo(width * 0.92f, height * 0.34f, width * 0.82f, height * 0.60f, width * 0.50f, height * 0.82f)
        close()
    }
}

private fun dropletPath(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
): Path {
    return Path().apply {
        moveTo(left + width * 0.55f, top)
        cubicTo(
            left + width * 0.22f,
            top + height * 0.18f,
            left,
            top + height * 0.52f,
            left + width * 0.10f,
            top + height * 0.72f,
        )
        cubicTo(
            left + width * 0.22f,
            top + height * 0.92f,
            left + width * 0.38f,
            top + height,
            left + width * 0.55f,
            top + height,
        )
        cubicTo(
            left + width * 0.80f,
            top + height,
            left + width,
            top + height * 0.80f,
            left + width,
            top + height * 0.60f,
        )
        cubicTo(
            left + width,
            top + height * 0.36f,
            left + width * 0.86f,
            top + height * 0.14f,
            left + width * 0.55f,
            top,
        )
        close()
    }
}

private fun torsoPath(width: Float, height: Float): Path {
    return Path().apply {
        moveTo(width * 0.44f, height * 0.10f)
        cubicTo(width * 0.32f, height * 0.10f, width * 0.24f, height * 0.18f, width * 0.24f, height * 0.28f)
        lineTo(width * 0.24f, height * 0.40f)
        cubicTo(width * 0.17f, height * 0.46f, width * 0.13f, height * 0.56f, width * 0.13f, height * 0.70f)
        lineTo(width * 0.13f, height * 0.82f)
        cubicTo(width * 0.13f, height * 0.90f, width * 0.19f, height * 0.95f, width * 0.27f, height * 0.95f)
        lineTo(width * 0.73f, height * 0.95f)
        cubicTo(width * 0.81f, height * 0.95f, width * 0.87f, height * 0.90f, width * 0.87f, height * 0.82f)
        lineTo(width * 0.87f, height * 0.70f)
        cubicTo(width * 0.87f, height * 0.56f, width * 0.83f, height * 0.46f, width * 0.76f, height * 0.40f)
        lineTo(width * 0.76f, height * 0.28f)
        cubicTo(width * 0.76f, height * 0.18f, width * 0.68f, height * 0.10f, width * 0.56f, height * 0.10f)
        cubicTo(width * 0.54f, height * 0.10f, width * 0.50f, height * 0.14f, width * 0.50f, height * 0.20f)
        cubicTo(width * 0.50f, height * 0.14f, width * 0.46f, height * 0.10f, width * 0.44f, height * 0.10f)
        close()
    }
}

@Composable
private fun MetricValue(
    modifier: Modifier,
    value: String,
    valueColor: Color,
    unit: String,
    unitColor: Color,
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 18.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = unit,
            color = unitColor,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

@Composable
private fun HydrateMetricValue(
    modifier: Modifier,
    currentValue: String,
    targetValue: String,
    unit: String,
) {
    Column(modifier = modifier) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = Color(0xFF04B8FE),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                ) {
                    append(currentValue)
                }
                withStyle(
                    SpanStyle(
                        color = Color(0xFFB4DDEF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                    )
                ) {
                    append("/")
                }
                withStyle(
                    SpanStyle(
                        color = Color(0xFFB4DDEF),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                ) {
                    append(targetValue)
                }
            }
        )
        Text(
            text = unit,
            color = Color(0xFF97C4D0),
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
        )
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
                cta = "Measure",
                value = "84",
                valueUnit = "BPM",
                footerText = "15:25 Tue,Jan 13",
            ),
            cards = listOf(
                HomeFeatureCardUi.BloodSugar(
                    title = "Blood Sugar",
                    value = "8.1",
                    unit = "mmol/L",
                ),
                HomeFeatureCardUi.BloodPressure(
                    title = stringResource(R.string.tr_blood_pressure),
                    value = "108/78",
                    unit = "mmHg",
                ),
                HomeFeatureCardUi.Bmi(
                    title = stringResource(R.string.tr_weight_and_bmi),
                    value = "65",
                    unit = "kg",
                ),
                HomeFeatureCardUi.Cholesterol(
                    title = stringResource(R.string.tr_cholesterol),
                    value = "178",
                    unit = "mg/dL",
                ),
                HomeFeatureCardUi.StepCount(
                    title = stringResource(R.string.tr_step_count),
                    stepsValue = "1752",
                    stepsUnit = "steps",
                ),
                HomeFeatureCardUi.Hydrate(
                    title = stringResource(R.string.tr_hydrate),
                    currentValue = "0",
                    targetValue = "2000",
                    unit = "mL",
                ),
            ),
            onHeartRateClick = {},
            onBloodSugarClick = {},
            onBloodPressureClick = {},
            onCholesterolClick = {},
            onBmiClick = {},
            onHydrateClick = {},
            onStepCountClick = {},
            onGuideAnchorBoundsChanged = { _, _ -> },
        )
    }
}
