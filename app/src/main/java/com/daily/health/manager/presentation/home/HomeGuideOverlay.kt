package com.daily.health.manager.presentation.home

import android.graphics.Rect
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.face.theme.HealthTrackerTheme
import kotlin.math.roundToInt

enum class HomeGuideTarget {
    HERO_CARD,
    HERO_CTA,
    BLOOD_PRESSURE_CARD,
    BLOOD_PRESSURE_RECORD,
    BLOOD_SUGAR_CARD,
    BLOOD_SUGAR_RECORD,
}

@Immutable
data class HomeGuideOverlayUi(
    val step: HomeGuideStep,
    val anchorRects: Map<HomeGuideTarget, Rect>,
)

enum class HomeGuideStep {
    HEART_RATE,
    BLOOD_PRESSURE,
    BLOOD_SUGAR,
    ;

    fun nextStep(): HomeGuideStep? = when (this) {
        HEART_RATE -> BLOOD_PRESSURE
        BLOOD_PRESSURE -> BLOOD_SUGAR
        BLOOD_SUGAR -> null
    }
}

@Immutable
private data class HomeGuideLayoutSpec(
    val messageWidth: Float,
    val messageTopFromHighlightBottom: Float,
    val handLeftFromActionLeft: Float,
    val handTopFromActionTop: Float,
    val handSize: Float,
    @StringRes val messageRes: Int,
)

private val HomeGuideStep.highlightTarget: HomeGuideTarget
    get() = when (this) {
        HomeGuideStep.HEART_RATE -> HomeGuideTarget.HERO_CARD
        HomeGuideStep.BLOOD_PRESSURE -> HomeGuideTarget.BLOOD_PRESSURE_CARD
        HomeGuideStep.BLOOD_SUGAR -> HomeGuideTarget.BLOOD_SUGAR_CARD
    }

private val HomeGuideStep.actionTarget: HomeGuideTarget
    get() = when (this) {
        HomeGuideStep.HEART_RATE -> HomeGuideTarget.HERO_CTA
        HomeGuideStep.BLOOD_PRESSURE -> HomeGuideTarget.BLOOD_PRESSURE_RECORD
        HomeGuideStep.BLOOD_SUGAR -> HomeGuideTarget.BLOOD_SUGAR_RECORD
    }

private fun HomeGuideStep.layoutSpec(): HomeGuideLayoutSpec = when (this) {
    HomeGuideStep.HEART_RATE -> HomeGuideLayoutSpec(
        messageWidth = 264f,
        messageTopFromHighlightBottom = 68f,
        handLeftFromActionLeft = 75f,
        handTopFromActionTop = 24f,
        handSize = 80f,
        messageRes = R.string.tr_guide_hr_des,
    )

    HomeGuideStep.BLOOD_PRESSURE -> HomeGuideLayoutSpec(
        messageWidth = 240f,
        messageTopFromHighlightBottom = 80f,
        handLeftFromActionLeft = 50f,
        handTopFromActionTop = 8f,
        handSize = 80f,
        messageRes = R.string.tr_guide_bp_des,
    )

    HomeGuideStep.BLOOD_SUGAR -> HomeGuideLayoutSpec(
        messageWidth = 213f,
        messageTopFromHighlightBottom = 80f,
        handLeftFromActionLeft = 50f,
        handTopFromActionTop = 8f,
        handSize = 80f,
        messageRes = R.string.tr_guide_bs_des,
    )
}

@Composable
internal fun HomeFeatureGuideOverlay(
    step: HomeGuideStep,
    anchorRects: Map<HomeGuideTarget, Rect>,
    onNextClick: () -> Unit,
    onDismiss: () -> Unit,
    onTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlightTargetRect = anchorRects[step.highlightTarget] ?: return
    val actionTargetRect = anchorRects[step.actionTarget] ?: return

    var windowOffset by remember { mutableStateOf(IntOffset.Zero) }

    val highlightRect = Rect(
        highlightTargetRect.left - windowOffset.x,
        highlightTargetRect.top - windowOffset.y,
        highlightTargetRect.right - windowOffset.x,
        highlightTargetRect.bottom - windowOffset.y,
    )
    val actionRect = Rect(
        actionTargetRect.left - windowOffset.x,
        actionTargetRect.top - windowOffset.y,
        actionTargetRect.right - windowOffset.x,
        actionTargetRect.bottom - windowOffset.y,
    )

    val layout = step.layoutSpec()
    val transition = rememberInfiniteTransition(label = "homeGuide")
    val handFloatY = transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "handFloatY",
    )
    val handScale = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "handScale",
    )
    val arrowAlpha = transition.animateFloat(
        initialValue = 0.76f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "arrowAlpha",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                windowOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt())
            }
            .pointerInput(step, highlightRect, actionRect) {
                detectTapGestures { offset ->
                    when {
                        actionRect.contains(offset.x.roundToInt(), offset.y.roundToInt()) -> onTargetClick()
                        highlightRect.contains(offset.x.roundToInt(), offset.y.roundToInt()) -> Unit
                        else -> onDismiss()
                    }
                }
            }
    ) {
        val screenWidthScale = maxWidth / 375.dp
        val screenHeightScale = maxHeight / 903.dp
        val assetScale = screenWidthScale
        val density = LocalDensity.current
        val messageOffsetY = with(density) { (layout.messageTopFromHighlightBottom.dp * screenHeightScale).roundToPx() }
        val contentWidth = layout.messageWidth.dp * screenWidthScale
        // Grid targets occupy half the screen. Keep the bubble and pointer inside the viewport,
        // near the highlighted tile, rather than reusing full-width-card coordinates.
        val viewportWidth = with(density) { maxWidth.roundToPx() }
        val contentWidthPx = with(density) { contentWidth.roundToPx() }
        val edgeMargin = with(density) { 12.dp.roundToPx() }
        val messageOffsetX = (highlightRect.centerX() - contentWidthPx / 2)
            .coerceIn(edgeMargin, (viewportWidth - contentWidthPx - edgeMargin).coerceAtLeast(edgeMargin))
        val handWidthPx = with(density) { (layout.handSize.dp * assetScale).roundToPx() }
        val handOffsetX = with(density) { (layout.handLeftFromActionLeft.dp * screenWidthScale).roundToPx() }
        val handOffsetY = with(density) { (layout.handTopFromActionTop.dp * screenHeightScale).roundToPx() }
        val spotlightCorner = with(density) {
            if (step == HomeGuideStep.HEART_RATE) {
                HomeCardShape.heroCornerRadius(highlightRect.width().toDp()).toPx()
            } else {
                HomeCardShape.featureCornerRadius.toPx()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawRect(color = Color.Black.copy(alpha = 0.6f))
                    val left = highlightRect.left.toFloat()
                    val top = highlightRect.top.toFloat()
                    val width = highlightRect.width().toFloat()
                    val height = highlightRect.height().toFloat()
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(width, height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(spotlightCorner, spotlightCorner),
                        blendMode = BlendMode.Clear,
                    )
                }
        )

        // The exported connector and bubble follow the measured card anchor, so changing
        // artwork does not change the existing spotlight or target click handling.
        Image(
            painter = painterResource(R.mipmap.tr_ic_guide_arrow_1),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset(
                        messageOffsetX + with(density) { (contentWidth / 2 - 3.dp).roundToPx() },
                        highlightRect.bottom,
                    )
                }
                .size(6.dp, with(density) { messageOffsetY.toDp() }.coerceAtLeast(1.dp))
                .graphicsLayer { alpha = arrowAlpha.value },
            contentScale = ContentScale.FillBounds,
        )

        Column(
            modifier = Modifier
                .offset { IntOffset(messageOffsetX, highlightRect.bottom + messageOffsetY) }
                .width(contentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(R.drawable.tr_home_guide_bubble),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Text(
                    text = stringResource(layout.messageRes),
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
                )
            }
            // Place the action after the measured message, allowing translations to wrap safely.
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(width = 104.dp, height = 48.dp)
                    .border(1.dp, Color.White, RoundedCornerShape(35.dp))
                    .clickable(onClick = onNextClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        R.string.tr_guide_step_action,
                        stringResource(R.string.tr_next),
                        step.ordinal + 1,
                        HomeGuideStep.entries.size,
                    ),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Image(
            painter = painterResource(id = R.mipmap.tr_ic_guide_hand),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (actionRect.left + handOffsetX).coerceIn(edgeMargin, (viewportWidth - handWidthPx - edgeMargin).coerceAtLeast(edgeMargin)),
                        y = actionRect.top + handOffsetY + handFloatY.value.roundToInt(),
                    )
                }
                .size(layout.handSize.dp * assetScale)
                .graphicsLayer {
                    scaleX = handScale.value
                    scaleY = handScale.value
                },
            contentScale = ContentScale.Fit,
        )
    }
}

@Preview(name = "Guide Heart", showBackground = true, widthDp = 375, heightDp = 812, backgroundColor = 0xFFF5F7FB)
@Composable
private fun HomeFeatureGuideHeartPreview() {
    HealthTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FB))) {
            HomeFeatureGuideOverlay(
                step = HomeGuideStep.HEART_RATE,
                anchorRects = mapOf(
                    HomeGuideTarget.HERO_CARD to Rect(16, 108, 359, 284),
                    HomeGuideTarget.HERO_CTA to Rect(142, 181, 302, 217),
                ),
                onNextClick = {},
                onDismiss = {},
                onTargetClick = {},
            )
        }
    }
}

@Preview(name = "Guide BP", showBackground = true, widthDp = 375, heightDp = 812, backgroundColor = 0xFFF5F7FB)
@Composable
private fun HomeFeatureGuideBloodPressurePreview() {
    HealthTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FB))) {
            HomeFeatureGuideOverlay(
                step = HomeGuideStep.BLOOD_PRESSURE,
                anchorRects = mapOf(
                    HomeGuideTarget.BLOOD_PRESSURE_CARD to Rect(16, 298, 181, 460),
                    HomeGuideTarget.BLOOD_PRESSURE_RECORD to Rect(28, 418, 168, 450),
                ),
                onNextClick = {},
                onDismiss = {},
                onTargetClick = {},
            )
        }
    }
}

@Preview(name = "Guide BS", showBackground = true, widthDp = 375, heightDp = 812, backgroundColor = 0xFFF5F7FB)
@Composable
private fun HomeFeatureGuideBloodSugarPreview() {
    HealthTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FB))) {
            HomeFeatureGuideOverlay(
                step = HomeGuideStep.BLOOD_SUGAR,
                anchorRects = mapOf(
                    HomeGuideTarget.BLOOD_SUGAR_CARD to Rect(195, 298, 360, 460),
                    HomeGuideTarget.BLOOD_SUGAR_RECORD to Rect(207, 418, 347, 450),
                ),
                onNextClick = {},
                onDismiss = {},
                onTargetClick = {},
            )
        }
    }
}
