package com.daily.health.manager.face.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.face.act.reportGuide
import com.daily.health.manager.face.theme.HealthTrackerTheme
import kotlinx.coroutines.launch

private val OnboardingBlue = Color(0xFF1D6BF2)
private val OnboardingTextPrimary = Color(0xFF333333)
private val OnboardingTextSecondary = Color(0xFF666666)
private val OnboardingIndicatorInactive = Color(0xFFD2E2FF)
private val OnboardingPanel = Color(0xFFF4F8FF)

private val OnboardingPanelShape = GenericShape { size, _ ->
    val sideY = size.height * (29.8833f / 394f)
    moveTo(0f, sideY)
    cubicTo(
        0f,
        sideY,
        size.width * (89f / 375f),
        0f,
        size.width * (187f / 375f),
        0f
    )
    cubicTo(
        size.width * (285f / 375f),
        0f,
        size.width,
        sideY,
        size.width,
        sideY
    )
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

private data class OnboardingTextLayout(
    val isAdaptive: Boolean,
    val descriptionWidth: Dp,
    val descriptionHeight: Dp,
    val descriptionFontSize: Int,
    val descriptionLineHeight: Int,
)

private data class OnboardingPageUi(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val buttonRes: Int,
    val showArrow: Boolean,
    val imageTop: Dp,
    val imageWidth: Dp,
    val imageHeight: Dp,
    val titleWidth: Dp = 319.dp,
    val descriptionWidth: Dp,
    val descriptionHeight: Dp = 66.dp,
    val adaptiveDescriptionWidth: Dp = 344.dp,
    val adaptiveDescriptionHeight: Dp = 128.dp,
    val imageScale: ContentScale = ContentScale.Fit,
    val titleTop: Dp = 63.dp,
    val descriptionTop: Dp = 101.dp,
    val actionTop: Dp = 223.dp,
    val indicatorTop: Dp = 242.dp,
)

@Composable
private fun nonScaledSp(value: Int) = with(LocalDensity.current) { (value / fontScale).sp }

@Composable
private fun rememberTextLayout(page: OnboardingPageUi): OnboardingTextLayout {
    val configuration = LocalConfiguration.current
    val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        configuration.locale
    }
    val isEnglish = locale?.language.equals("en", ignoreCase = true)
    return if (isEnglish) {
        OnboardingTextLayout(
            isAdaptive = false,
            descriptionWidth = page.descriptionWidth,
            descriptionHeight = page.descriptionHeight,
            descriptionFontSize = 14,
            descriptionLineHeight = 22,
        )
    } else {
        OnboardingTextLayout(
            isAdaptive = true,
            descriptionWidth = page.adaptiveDescriptionWidth,
            descriptionHeight = page.adaptiveDescriptionHeight,
            descriptionFontSize = 13,
            descriptionLineHeight = 20,
        )
    }
}

private val PreviewOnboardingPages = listOf(
    OnboardingPageUi(
        imageRes = R.mipmap.ht_ic_guide_1,
        titleRes = R.string.ht_onboarding_title_1,
        descriptionRes = R.string.ht_onboarding_desc_1,
        buttonRes = R.string.ht_next,
        showArrow = true,
        imageTop = 139.dp,
        imageWidth = 264.2.dp,
        imageHeight = 216.9.dp,
        descriptionWidth = 312.dp,
        descriptionHeight = 66.dp,
        imageScale = ContentScale.Fit
    ),
    OnboardingPageUi(
        imageRes = R.mipmap.ht_ic_guide_2,
        titleRes = R.string.ht_onboarding_title_2,
        descriptionRes = R.string.ht_onboarding_desc_2,
        buttonRes = R.string.ht_next,
        showArrow = true,
        imageTop = 183.dp,
        imageWidth = 264.2.dp,
        imageHeight = 173.2.dp,
        descriptionWidth = 312.dp,
        descriptionHeight = 66.dp,
        imageScale = ContentScale.Fit
    ),
    OnboardingPageUi(
        imageRes = R.mipmap.ht_ic_guide_3,
        titleRes = R.string.ht_onboarding_title_3,
        descriptionRes = R.string.ht_onboarding_desc_3,
        buttonRes = R.string.ht_next,
        showArrow = true,
        imageTop = 144.dp,
        imageWidth = 264.2.dp,
        imageHeight = 221.3.dp,
        descriptionWidth = 338.dp,
        descriptionHeight = 66.dp,
        imageScale = ContentScale.Fit
    ),
    OnboardingPageUi(
        imageRes = R.mipmap.ht_ic_guide_4,
        titleRes = R.string.ht_onboarding_title_4,
        descriptionRes = R.string.ht_onboarding_desc_4,
        buttonRes = R.string.ht_onboarding_start,
        showArrow = false,
        imageTop = 107.dp,
        imageWidth = 264.2.dp,
        imageHeight = 267.2.dp,
        descriptionWidth = 338.dp,
        descriptionHeight = 110.dp,
        imageScale = ContentScale.Fit
    )
)

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit
) {
    val pages = remember { PreviewOnboardingPages }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        reportGuide(pagerState.currentPage + 2)
    }

    OnboardingScreen(
        pages = pages,
        currentPage = pagerState.currentPage,
        content = {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = true
            ) { page ->
                OnboardingPage(page = pages[page])
            }
        },
        onActionClick = {
            if (pagerState.currentPage == pages.lastIndex) {
                onFinish()
            } else {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }
    )
}

@Composable
private fun OnboardingScreen(
    pages: List<OnboardingPageUi>,
    currentPage: Int,
    content: @Composable () -> Unit,
    onActionClick: () -> Unit
) {
    val current = pages[currentPage]
    val textLayout = rememberTextLayout(current)
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFEAF2FF)),
                    startY = screenHeight * 0.15f,
                    endY = screenHeight
                )
            )
    ) {
        content()

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(375.dp)
                .height(394.dp)
                .clip(OnboardingPanelShape)
                .background(OnboardingPanel)
        ) {
            Text(
                text = stringResource(current.titleRes),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = current.titleTop)
                    .width(current.titleWidth),
                color = OnboardingTextPrimary,
                fontSize = nonScaledSp(20),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = nonScaledSp(23)
            )
            val descriptionScrollState = rememberScrollState()
            Text(
                text = stringResource(current.descriptionRes),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = current.descriptionTop)
                    .width(textLayout.descriptionWidth)
                    .height(textLayout.descriptionHeight)
                    .then(
                        if (textLayout.isAdaptive) {
                            Modifier.verticalScroll(descriptionScrollState)
                        } else {
                            Modifier
                        }
                    ),
                color = OnboardingTextSecondary,
                fontSize = nonScaledSp(textLayout.descriptionFontSize),
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = nonScaledSp(textLayout.descriptionLineHeight),
                overflow = TextOverflow.Clip
            )
            OnboardingPagination(
                pageCount = pages.size,
                currentPage = currentPage,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 17.dp, y = current.indicatorTop)
            )
            OnboardingActionButton(
                text = stringResource(current.buttonRes),
                showArrow = current.showArrow,
                onClick = onActionClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 242.dp, y = current.actionTop)
            )
        }
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPageUi) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = page.imageTop)
                .width(page.imageWidth)
                .height(page.imageHeight),
            contentScale = page.imageScale
        )
    }
}

@Composable
private fun OnboardingPagination(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.width(113.dp).height(6.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .offset(x = (29 * index).dp)
                    .width(26.dp)
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .background(if (index == currentPage) OnboardingBlue else OnboardingIndicatorInactive)
            )
        }
    }
}

@Composable
private fun OnboardingActionButton(
    text: String,
    showArrow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(116.dp)
            .height(44.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(37.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = OnboardingBlue,
            contentColor = Color.White
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                color = Color.White,
                fontSize = nonScaledSp(16),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = if (showArrow) (-9).dp else 0.dp)
            )
            if (showArrow) {
                OnboardingArrow(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 87.dp, y = 17.dp)
                        .size(width = 13.dp, height = 10.dp)
                )
            }
        }
    }
}


@Composable
private fun OnboardingArrow(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 1.6.dp.toPx()
        val midY = size.height / 2f
        val startX = 0f
        val tipX = size.width
        val shaftEndX = size.width * 0.62f
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(startX, midY),
            end = androidx.compose.ui.geometry.Offset(shaftEndX, midY),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.14f),
            end = androidx.compose.ui.geometry.Offset(tipX, midY),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.44f, size.height * 0.86f),
            end = androidx.compose.ui.geometry.Offset(tipX, midY),
            strokeWidth = strokeWidth
        )
    }
}

@Preview(
    name = "Onboarding Flow",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 375,
    heightDp = 812,
    showSystemUi = true,
    locale = "it"
)
@Composable
private fun OnboardingScreenPreview() {
    HealthTrackerTheme {
        OnboardingScreen(
            pages = PreviewOnboardingPages,
            currentPage = 0,
            content = { OnboardingPage(page = PreviewOnboardingPages.first()) },
            onActionClick = {}
        )
    }
}

@Preview(
    name = "Onboarding Page 2",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 375,
    heightDp = 812,
    showSystemUi = true
)
@Composable
private fun OnboardingSecondPagePreview() {
    HealthTrackerTheme {
        OnboardingScreen(
            pages = PreviewOnboardingPages,
            currentPage = 1,
            content = { OnboardingPage(page = PreviewOnboardingPages[1]) },
            onActionClick = {}
        )
    }
}

@Preview(
    name = "Onboarding Page 3",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 375,
    heightDp = 812,
    showSystemUi = true
)
@Composable
private fun OnboardingThirdPagePreview() {
    HealthTrackerTheme {
        OnboardingScreen(
            pages = PreviewOnboardingPages,
            currentPage = 2,
            content = { OnboardingPage(page = PreviewOnboardingPages[2]) },
            onActionClick = {}
        )
    }
}

@Preview(
    name = "Onboarding Final Page",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 375,
    heightDp = 812,
    showSystemUi = true,
    locale = "it"
)
@Composable
private fun OnboardingLastPagePreview() {
    HealthTrackerTheme {
        OnboardingScreen(
            pages = PreviewOnboardingPages,
            currentPage = PreviewOnboardingPages.lastIndex,
            content = { OnboardingPage(page = PreviewOnboardingPages.last()) },
            onActionClick = {}
        )
    }
}
