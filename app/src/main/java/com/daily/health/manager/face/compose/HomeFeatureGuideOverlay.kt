package com.daily.health.manager.face.compose

import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
    val messageLeft: Float,
    val messageWidth: Float,
    val messageTopFromHighlightBottom: Float,
    val buttonLeft: Float,
    val buttonTopFromHighlightBottom: Float,
    val arrowLeft: Float,
    val arrowTopFromHighlightBottom: Float,
    val arrowWidth: Float,
    val arrowHeight: Float,
    val handLeftFromActionLeft: Float,
    val handTopFromActionTop: Float,
    val handSize: Float,
    val spotlightCornerRadius: Float = 12f,
    @DrawableRes val arrowRes: Int,
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
        messageLeft = 55.5f,
        messageWidth = 264f,
        messageTopFromHighlightBottom = 100f,
        buttonLeft = 216f,
        buttonTopFromHighlightBottom = 140f,
        arrowLeft = 27f,
        arrowTopFromHighlightBottom = 4f,
        arrowWidth = 77f,
        arrowHeight = 75f,
        handLeftFromActionLeft = 75f,
        handTopFromActionTop = 58f,
        handSize = 123f,
        arrowRes = R.mipmap.ht_ic_guide_arrow_1,
        messageRes = R.string.ht_guide_hr_des,
    )

    HomeGuideStep.BLOOD_PRESSURE -> HomeGuideLayoutSpec(
        messageLeft = 76f,
        messageWidth = 240f,
        messageTopFromHighlightBottom = 90f,
        buttonLeft = 216f,
        buttonTopFromHighlightBottom = 140f,
        arrowLeft = 45f,
        arrowTopFromHighlightBottom = 4f,
        arrowWidth = 77f,
        arrowHeight = 75f,
        handLeftFromActionLeft = 95f,
        handTopFromActionTop = -8f,
        handSize = 114f,
        arrowRes = R.mipmap.ht_ic_guide_arrow_1,
        messageRes = R.string.ht_guide_bp_des,
    )

    HomeGuideStep.BLOOD_SUGAR -> HomeGuideLayoutSpec(
        messageLeft = 81f,
        messageWidth = 213f,
        messageTopFromHighlightBottom = 60f,
        buttonLeft = 200f,
        buttonTopFromHighlightBottom = 110f,
        arrowLeft = 136f,
        arrowTopFromHighlightBottom = -40f,
        arrowWidth = 73f,
        arrowHeight = 75f,
        handLeftFromActionLeft = 80f,
        handTopFromActionTop = 0f,
        handSize = 114f,
        arrowRes = R.mipmap.ht_ic_guide_arrow_2,
        messageRes = R.string.ht_guide_bs_des,
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
        val arrowOffsetX = with(density) { (layout.arrowLeft.dp * screenWidthScale).roundToPx() }
        val arrowOffsetY = with(density) { (layout.arrowTopFromHighlightBottom.dp * screenHeightScale).roundToPx() }
        val messageOffsetX = with(density) { (layout.messageLeft.dp * screenWidthScale).roundToPx() }
        val messageOffsetY = with(density) { (layout.messageTopFromHighlightBottom.dp * screenHeightScale).roundToPx() }
        val contentWidth = layout.messageWidth.dp * screenWidthScale
        val buttonOffsetX = with(density) { (layout.buttonLeft.dp * screenWidthScale).roundToPx() }
        val buttonOffsetY = with(density) { (layout.buttonTopFromHighlightBottom.dp * screenHeightScale).roundToPx() }
        val handOffsetX = with(density) { (layout.handLeftFromActionLeft.dp * screenWidthScale).roundToPx() }
        val handOffsetY = with(density) { (layout.handTopFromActionTop.dp * screenHeightScale).roundToPx() }
        val spotlightCorner = with(density) { (layout.spotlightCornerRadius.dp * screenWidthScale).toPx() }

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

        Image(
            painter = painterResource(id = layout.arrowRes),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = arrowOffsetX,
                        y = highlightRect.bottom + arrowOffsetY,
                    )
                }
                .size(layout.arrowWidth.dp * assetScale, layout.arrowHeight.dp * assetScale)
                .graphicsLayer { alpha = arrowAlpha.value },
            contentScale = ContentScale.Fit,
        )

        Text(
            text = stringResource(id = layout.messageRes),
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = messageOffsetX,
                        y = highlightRect.bottom + messageOffsetY,
                    )
                }
                .width(contentWidth),
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = buttonOffsetX,
                        y = highlightRect.bottom + buttonOffsetY,
                    )
                }
                .size(width = 92.dp * screenWidthScale, height = 32.dp * screenHeightScale)
                .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(35.dp))
                .background(color = Color.Transparent, shape = RoundedCornerShape(35.dp))
                .pointerInput(step) {
                    detectTapGestures(onTap = { onNextClick() })
                }
        ) {
            Text(
                text = stringResource(id = R.string.ht_next),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Image(
            painter = painterResource(id = R.mipmap.ht_ic_guide_hand),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = actionRect.left + handOffsetX,
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
