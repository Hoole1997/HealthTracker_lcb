package com.daily.health.manager.presentation.home

import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import kotlin.math.roundToInt

private val HomeText = Color(0xFF18212D)
private val HomeMuted = Color(0xFF68717D)

private fun LayoutCoordinates.toAndroidWindowRect(): Rect {
    val bounds = boundsInWindow()
    return Rect(bounds.left.roundToInt(), bounds.top.roundToInt(), bounds.right.roundToInt(), bounds.bottom.roundToInt())
}

@Composable
fun HomeOverviewContent(
    hero: HeartSummaryUi,
    cards: List<MetricTileUi>,
    onHeartRateClick: () -> Unit,
    onBloodSugarCardClick: () -> Unit,
    onBloodSugarRecordClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
    onGuideAnchorBoundsChanged: (HomeGuideTarget, Rect) -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val orderedCards = remember(cards) { cards.inHomeDisplayOrder() }
    // 先绘制背景再避让系统栏，让渐变覆盖状态栏，滚动内容始终位于安全区域内。
    // 底部间距由宿主导航布局负责，这里只处理顶部及横向刘海区域。
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(0f to Color(0xFFF1EFFD), 0.5f to Color.White, 1f to Color.White)
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        // Keep two columns, but let card height grow for small screens and long translations.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "greeting", span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader(onSettingsClick)
            }
            item(key = "heart", span = { GridItemSpan(maxLineSpan) }) {
                HeroCard(hero, onHeartRateClick,
                    { onGuideAnchorBoundsChanged(HomeGuideTarget.HERO_CARD, it) },
                    { onGuideAnchorBoundsChanged(HomeGuideTarget.HERO_CTA, it) })
            }
            item(key = "overview", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.tr_home_health_overview), color = HomeText,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp))
            }
            items(orderedCards, key = { it.homeCardKey() }) { card ->
                FeatureCard(
                    card,
                    actionForCard(card, onBloodSugarCardClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                    recordActionForCard(card, onBloodSugarRecordClick, onBloodPressureClick, onCholesterolClick, onBmiClick, onHydrateClick, onStepCountClick),
                    guideCardTargetForCard(card)?.let { target -> { rect -> onGuideAnchorBoundsChanged(target, rect) } },
                    guideRecordTargetForCard(card)?.let { target -> { rect -> onGuideAnchorBoundsChanged(target, rect) } },
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(onSettingsClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(currentHomeGreeting(), color = HomeText, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.tr_home_greeting_subtitle), color = HomeMuted, fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp))
        }
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
            Image(painterResource(R.drawable.tr_home_settings), stringResource(R.string.tr_settings), Modifier.size(44.dp))
        }
    }
}

@Composable
private fun HeroCard(hero: HeartSummaryUi, onClick: () -> Unit, onBounds: (Rect) -> Unit, onActionBounds: (Rect) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth
        val accent = colorResource(R.color.tr_home_heart_accent)
        Card(shape = RoundedCornerShape(HomeCardShape.heroCornerRadius(cardWidth)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDF1)),
            modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().heightIn(min = 216.dp)
                .clickable(onClick = onClick).onGloballyPositioned { onBounds(it.toAndroidWindowRect()) }) {
                // Reuse the masked Figma export. The uncropped upload has a black frame.
                Image(painterResource(R.drawable.img_home_heart_background), null,
                    Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                Image(painterResource(R.drawable.img_home_heart_rate), null,
                    Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)
                        .size(width = cardWidth * 0.41f, height = 140.dp), contentScale = ContentScale.Fit)
                Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 12.dp, bottom = 12.dp)) {
                    Column(Modifier.fillMaxWidth(0.58f)) {
                        Text(hero.title, color = HomeText, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(hero.value, color = accent, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text(hero.valueUnit, color = accent, fontSize = 12.sp)
                            hero.statusLabel?.let { label ->
                                val statusColor = hero.statusColorRes?.let { colorResource(it) } ?: HomeMuted
                                val statusSurface = if (hero.statusColorRes == R.color.color_05BA7B) Color(0xFFE9FAF2)
                                    else statusColor.copy(alpha = .1f).compositeOver(Color.White)
                                Text(label, color = statusColor, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(statusSurface).padding(4.dp))
                            }
                        }
                        Row(Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(24.dp)).background(accent).clickable(role = Role.Button, onClick = onClick)
                            .onGloballyPositioned { onActionBounds(it.toAndroidWindowRect()) }
                            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(hero.cta, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Image(painterResource(R.drawable.tr_home_measure_arrow), null, Modifier.size(24.dp))
                        }
                    }
                    Row(Modifier.padding(top = 24.dp).fillMaxWidth().heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(hero.footerText, color = accent, fontSize = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(hero.value, color = Color(0xFFFF8084), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(hero.valueUnit, color = accent, fontSize = 11.sp)
                        Image(painterResource(R.drawable.ic_home_chevron_right), null, Modifier.size(8.dp, 12.dp),
                            colorFilter = ColorFilter.tint(accent))
                    }
                }
            }
        }
    }
}

private data class MetricVisuals(val background: Color, @DrawableRes val image: Int, @DrawableRes val ring: Int)
private fun MetricTileUi.visuals(): MetricVisuals = when (this) {
    is MetricTileUi.BloodPressure -> MetricVisuals(Color(0xFFF4F8FE), R.drawable.img_home_blood_pressure, R.drawable.tr_home_ring_bp)
    is MetricTileUi.BloodSugar -> MetricVisuals(Color(0xFFFFF5F6), R.drawable.img_home_blood_sugar, R.drawable.tr_home_ring_bs)
    is MetricTileUi.Cholesterol -> MetricVisuals(Color(0xFFFFFAF0), R.drawable.img_home_cholesterol, R.drawable.tr_home_ring_cholesterol)
    is MetricTileUi.Bmi -> MetricVisuals(Color(0xFFF7F4FF), R.drawable.img_home_weight_bmi, R.drawable.tr_home_ring_weight)
    is MetricTileUi.Hydrate -> MetricVisuals(Color(0xFFF2F9FF), R.drawable.img_home_drink_water, R.drawable.tr_home_ring_water)
    is MetricTileUi.StepCount -> MetricVisuals(Color(0xFFF1FCF8), R.drawable.img_home_step_count, R.drawable.tr_home_ring_step)
}

@Composable
private fun FeatureCard(card: MetricTileUi, onClick: () -> Unit, onRecordClick: () -> Unit,
                        onBounds: ((Rect) -> Unit)?, onActionBounds: ((Rect) -> Unit)?) {
    val visual = card.visuals()
    val recordDescription = "${stringResource(R.string.tr_add_now)} ${card.title}"
    Card(shape = RoundedCornerShape(HomeCardShape.featureCornerRadius),
        colors = CardDefaults.cardColors(containerColor = visual.background),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(min = 102.dp).clickable(onClick = onClick)
            .onGloballyPositioned { onBounds?.invoke(it.toAndroidWindowRect()) }.padding(10.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.Top) {
                Text(card.title, color = HomeText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (card is MetricTileUi.BloodPressure || card is MetricTileUi.BloodSugar) {
                    Image(painterResource(if (card is MetricTileUi.BloodPressure) R.drawable.tr_home_bp_arrow else R.drawable.tr_home_bs_arrow),
                        null, Modifier.padding(start = 4.dp).size(16.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                    val value = when (card) {
                        is MetricTileUi.BloodPressure -> card.value
                        is MetricTileUi.BloodSugar -> card.value
                        is MetricTileUi.Cholesterol -> card.value
                        is MetricTileUi.Bmi -> card.value
                        is MetricTileUi.Hydrate -> card.currentValue
                        is MetricTileUi.StepCount -> card.stepsValue
                    }
                    val unit = when (card) {
                        is MetricTileUi.BloodPressure -> card.unit
                        is MetricTileUi.BloodSugar -> card.unit
                        is MetricTileUi.Cholesterol -> card.unit
                        is MetricTileUi.Bmi -> card.unit
                        is MetricTileUi.Hydrate -> stringResource(R.string.tr_home_water_target, card.targetValue, card.unit)
                        is MetricTileUi.StepCount -> card.stepsUnit
                    }
                    Text(value, color = HomeText, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(unit, color = HomeMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(56.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRecordClick)
                    .semantics { contentDescription = recordDescription }
                    .onGloballyPositioned { onActionBounds?.invoke(it.toAndroidWindowRect()) }, contentAlignment = Alignment.Center) {
                    Image(painterResource(visual.ring), null, Modifier.matchParentSize())
                    Image(painterResource(visual.image), null, Modifier.size(32.dp), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}

private fun MetricTileUi.homeCardKey(): String = when (this) {
    is MetricTileUi.BloodPressure -> "blood_pressure"
    is MetricTileUi.BloodSugar -> "blood_sugar"
    is MetricTileUi.Bmi -> "bmi"
    is MetricTileUi.Cholesterol -> "cholesterol"
    is MetricTileUi.StepCount -> "step_count"
    is MetricTileUi.Hydrate -> "hydrate"
}

private fun List<MetricTileUi>.inHomeDisplayOrder(): List<MetricTileUi> {
    return sortedBy { card ->
        when (card) {
            is MetricTileUi.BloodPressure -> 0
            is MetricTileUi.BloodSugar -> 1
            is MetricTileUi.Bmi -> 3
            is MetricTileUi.Cholesterol -> 2
            is MetricTileUi.StepCount -> 5
            is MetricTileUi.Hydrate -> 4
        }
    }
}


private fun guideCardTargetForCard(card: MetricTileUi): HomeGuideTarget? = when (card) {
    is MetricTileUi.BloodPressure -> HomeGuideTarget.BLOOD_PRESSURE_CARD
    is MetricTileUi.BloodSugar -> HomeGuideTarget.BLOOD_SUGAR_CARD
    is MetricTileUi.Cholesterol,
    is MetricTileUi.Bmi,
    is MetricTileUi.Hydrate,
    is MetricTileUi.StepCount -> null
}

private fun guideRecordTargetForCard(card: MetricTileUi): HomeGuideTarget? = when (card) {
    is MetricTileUi.BloodPressure -> HomeGuideTarget.BLOOD_PRESSURE_RECORD
    is MetricTileUi.BloodSugar -> HomeGuideTarget.BLOOD_SUGAR_RECORD
    is MetricTileUi.Cholesterol,
    is MetricTileUi.Bmi,
    is MetricTileUi.Hydrate,
    is MetricTileUi.StepCount -> null
}

private fun actionForCard(
    card: MetricTileUi,
    onBloodSugarCardClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
): () -> Unit = when (card) {
    is MetricTileUi.BloodPressure -> onBloodPressureClick
    is MetricTileUi.BloodSugar -> onBloodSugarCardClick
    is MetricTileUi.Cholesterol -> onCholesterolClick
    is MetricTileUi.Bmi -> onBmiClick
    is MetricTileUi.Hydrate -> onHydrateClick
    is MetricTileUi.StepCount -> onStepCountClick
}

private fun recordActionForCard(
    card: MetricTileUi,
    onBloodSugarRecordClick: () -> Unit,
    onBloodPressureClick: () -> Unit,
    onCholesterolClick: () -> Unit,
    onBmiClick: () -> Unit,
    onHydrateClick: () -> Unit,
    onStepCountClick: () -> Unit,
): () -> Unit = when (card) {
    is MetricTileUi.BloodPressure -> onBloodPressureClick
    is MetricTileUi.BloodSugar -> onBloodSugarRecordClick
    is MetricTileUi.Cholesterol -> onCholesterolClick
    is MetricTileUi.Bmi -> onBmiClick
    is MetricTileUi.Hydrate -> onHydrateClick
    is MetricTileUi.StepCount -> onStepCountClick
}
