package com.daily.health.manager.face.compose

import android.graphics.Rect
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.face.theme.HealthTrackerTheme
import kotlin.math.max
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
    BLOOD_SUGAR,
    BLOOD_PRESSURE,
    ;

    fun nextStep(): HomeGuideStep? = when (this) {
        HEART_RATE -> BLOOD_SUGAR
        BLOOD_SUGAR -> BLOOD_PRESSURE
        BLOOD_PRESSURE -> null
    }
}

@Immutable
private data class HomeGuideLayoutSpec(
    val messageLeft: Float,
    val messageWidth: Float,
    val messageTopFromHighlightBottom: Float,
    val buttonLeft: Float,
    val buttonTopFromHighlightBottom: Float,
    val buttonSpacingFromMessageBottom: Float = 14f,
    val buttonTextRes: Int,
    val connectorHeight: Float = 60f,
    val connectorDotRadius: Float = 4f,
    val buttonWidth: Dp = 92.dp,
    val buttonHeight: Dp = 32.dp,
    val spotlightCornerRadius: Float = 20f,
    @StringRes val messageRes: Int,
    @StringRes val accentRes: Int,
)

private val HomeGuideStep.highlightTarget: HomeGuideTarget
    get() = when (this) {
        HomeGuideStep.HEART_RATE -> HomeGuideTarget.HERO_CARD
        HomeGuideStep.BLOOD_PRESSURE -> HomeGuideTarget.BLOOD_PRESSURE_CARD
        HomeGuideStep.BLOOD_SUGAR -> HomeGuideTarget.BLOOD_SUGAR_CARD
    }

private fun HomeGuideStep.layoutSpec(): HomeGuideLayoutSpec = when (this) {
    HomeGuideStep.HEART_RATE -> HomeGuideLayoutSpec(
        messageLeft = 55.5f,
        messageWidth = 264f,
        messageTopFromHighlightBottom = 73f,
        buttonLeft = 141f,
        buttonTopFromHighlightBottom = 101f,
        buttonTextRes = R.string.tr_next,
        messageRes = R.string.tr_guide_hr_des,
        accentRes = R.string.tr_heart_rate,
    )

    HomeGuideStep.BLOOD_SUGAR -> HomeGuideLayoutSpec(
        messageLeft = 31f,
        messageWidth = 313f,
        messageTopFromHighlightBottom = 69f,
        buttonLeft = 131f,
        buttonTopFromHighlightBottom = 99f,
        buttonTextRes = R.string.tr_next,
        messageRes = R.string.tr_guide_bs_des,
        accentRes = R.string.tr_blood_suger,
    )

    HomeGuideStep.BLOOD_PRESSURE -> HomeGuideLayoutSpec(
        messageLeft = 66f,
        messageWidth = 263f,
        messageTopFromHighlightBottom = 69f,
        buttonLeft = 177f,
        buttonTopFromHighlightBottom = 99f,
        buttonTextRes = R.string.tr_onboarding_start,
        messageRes = R.string.tr_guide_bp_des,
        accentRes = R.string.tr_blood_pressure,
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

    var windowOffset by remember { mutableStateOf(IntOffset.Zero) }

    val highlightRect = Rect(
        highlightTargetRect.left - windowOffset.x,
        highlightTargetRect.top - windowOffset.y,
        highlightTargetRect.right - windowOffset.x,
        highlightTargetRect.bottom - windowOffset.y,
    )

    val layout = step.layoutSpec()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                windowOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt())
            }
            .pointerInput(step, highlightRect) {
                detectTapGestures { offset ->
                    when {
                        highlightRect.contains(offset.x.roundToInt(), offset.y.roundToInt()) -> onNextClick()
                        else -> onNextClick()
                    }
                }
            }
    ) {
        val screenScale = maxWidth / 375.dp
        val density = LocalDensity.current
        var messageHeightPx by remember(step, highlightRect) { mutableIntStateOf(0) }
        val messageOffsetX = with(density) { (layout.messageLeft.dp * screenScale).roundToPx() }
        val messageOffsetY = with(density) { (layout.messageTopFromHighlightBottom.dp * screenScale).roundToPx() }
        val contentWidth = layout.messageWidth.dp * screenScale
        val buttonOffsetX = with(density) { (layout.buttonLeft.dp * screenScale).roundToPx() }
        val buttonDefaultOffsetY = with(density) { (layout.buttonTopFromHighlightBottom.dp * screenScale).roundToPx() }
        val buttonSpacingFromMessageBottomPx = with(density) {
            (layout.buttonSpacingFromMessageBottom.dp * screenScale).roundToPx()
        }
        val spotlightCorner = with(density) { (layout.spotlightCornerRadius.dp * screenScale).toPx() }
        val connectorHeightPx = with(density) { (layout.connectorHeight.dp * screenScale).toPx() }
        val connectorDotRadiusPx = with(density) { (layout.connectorDotRadius.dp * screenScale).toPx() }
        val connectorStrokePx = with(density) { (2.dp * screenScale).toPx() }
        val connectorDashPx = with(density) { (4.dp * screenScale).toPx() }
        val connectorGapPx = with(density) { (4.dp * screenScale).toPx() }
        val messageText = stringResource(id = layout.messageRes)
        val accentText = stringResource(id = layout.accentRes)
        val guideMessage = remember(messageText, accentText) {
            buildGuideAnnotatedMessage(messageText, accentText)
        }
        val messageTop = highlightRect.bottom + messageOffsetY
        val buttonOffsetY = max(
            buttonDefaultOffsetY,
            messageOffsetY + messageHeightPx + buttonSpacingFromMessageBottomPx,
        )

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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val connectorX = highlightRect.exactCenterX()
            val connectorTop = highlightRect.bottom.toFloat()
            val connectorBottom = connectorTop + connectorHeightPx
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(connectorX, connectorTop),
                end = androidx.compose.ui.geometry.Offset(connectorX, connectorBottom),
                strokeWidth = connectorStrokePx,
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(connectorDashPx, connectorGapPx),
                ),
            )
            drawCircle(
                color = Color.White,
                radius = connectorDotRadiusPx,
                center = androidx.compose.ui.geometry.Offset(connectorX, connectorBottom),
            )
        }

        Text(
            text = guideMessage,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = messageOffsetX,
                        y = messageTop,
                    )
                }
                .width(contentWidth),
            maxLines = 3,
            overflow = TextOverflow.Clip,
            onTextLayout = { messageHeightPx = it.size.height },
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = buttonOffsetX,
                        y = highlightRect.bottom + buttonOffsetY,
                    )
                }
                .size(width = layout.buttonWidth * screenScale, height = layout.buttonHeight * screenScale)
                .background(color = Color(0xFFFF7C3F), shape = RoundedCornerShape(35.dp))
                .clickable(onClick = onNextClick)
        ) {
            Text(
                text = stringResource(id = layout.buttonTextRes),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private fun buildGuideAnnotatedMessage(
    message: String,
    accentText: String,
) = buildAnnotatedString {
    append(message)
    val accentStart = message.indexOf(accentText, ignoreCase = true)
    if (accentStart >= 0) {
        addStyle(
            style = SpanStyle(color = Color(0xFFFF7C3F)),
            start = accentStart,
            end = accentStart + accentText.length,
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

@Preview(name = "Guide BS", showBackground = true, widthDp = 375, heightDp = 812, backgroundColor = 0xFFF5F7FB)
@Composable
private fun HomeFeatureGuideBloodSugarPreview() {
    HealthTrackerTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FB))) {
            HomeFeatureGuideOverlay(
                step = HomeGuideStep.BLOOD_SUGAR,
                anchorRects = mapOf(
                    HomeGuideTarget.BLOOD_SUGAR_CARD to Rect(16, 299, 182, 431),
                    HomeGuideTarget.BLOOD_SUGAR_RECORD to Rect(59, 349, 138, 409),
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
                    HomeGuideTarget.BLOOD_PRESSURE_CARD to Rect(194, 299, 360, 431),
                    HomeGuideTarget.BLOOD_PRESSURE_RECORD to Rect(238, 349, 317, 409),
                ),
                onNextClick = {},
                onDismiss = {},
                onTargetClick = {},
            )
        }
    }
}
