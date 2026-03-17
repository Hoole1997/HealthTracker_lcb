package com.daily.health.manager.face.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.face.act.reportGuide
import com.daily.health.manager.face.theme.HealthTrackerTheme
import kotlinx.coroutines.launch

private data class OnboardingPalette(
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val indicatorInactive: Color,
    val illustrationHalo: Color,
)

@Composable
private fun rememberOnboardingPalette() = OnboardingPalette(
    accent = colorResource(id = R.color.fc_home_tab_selected),
    textPrimary = colorResource(id = R.color.t1),
    textSecondary = colorResource(id = R.color.color_666),
    indicatorInactive = colorResource(id = R.color.fc_brand_indicator_inactive),
    illustrationHalo = colorResource(id = R.color.fc_brand_halo),
)

private data class OnboardingTextLayout(
    val isAdaptive: Boolean,
    val descriptionWidth: Dp,
    val descriptionHeight: Dp,
    val descriptionFontSize: Int,
    val descriptionLineHeight: Int,
)

private data class OnboardingPageUi(
    @DrawableRes val figureRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val buttonRes: Int,
    val showArrow: Boolean,
    val illustrationTop: Dp = 133.dp,
    val illustrationBoxWidth: Dp = 278.dp,
    val illustrationBoxHeight: Dp = 266.dp,
    val illustrationWidth: Dp = 278.dp,
    val illustrationHeight: Dp = 266.dp,
    val illustrationOffsetX: Dp = 0.dp,
    val illustrationOffsetY: Dp = 0.dp,
    val haloSize: Dp? = null,
    val haloOffsetY: Dp = 0.dp,
    val titleTop: Dp = 474.dp,
    val titleWidth: Dp = 319.dp,
    val descriptionTop: Dp = 513.dp,
    val descriptionWidth: Dp = 312.dp,
    val descriptionHeight: Dp = 66.dp,
    val adaptiveDescriptionWidth: Dp = 338.dp,
    val adaptiveDescriptionHeight: Dp = 110.dp,
    val indicatorTop: Dp = 607.dp,
    val actionTop: Dp = 660.dp,
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
        figureRes = R.mipmap.fc_onboarding_figure_1,
        titleRes = R.string.fc_onboarding_title_1,
        descriptionRes = R.string.fc_onboarding_desc_1,
        buttonRes = R.string.fc_next,
        showArrow = true,
        haloSize = 233.dp,
        haloOffsetY = 17.dp,
    ),
    OnboardingPageUi(
        figureRes = R.mipmap.fc_onboarding_figure_2,
        titleRes = R.string.fc_onboarding_title_2,
        descriptionRes = R.string.fc_onboarding_desc_2,
        buttonRes = R.string.fc_next,
        showArrow = true,
        illustrationWidth = 264.dp,
        illustrationHeight = 260.dp,
        illustrationOffsetY = (-5).dp,
        haloSize = 233.dp,
        descriptionWidth = 312.dp,
        descriptionHeight = 66.dp,
    ),
    OnboardingPageUi(
        figureRes = R.mipmap.fc_onboarding_figure_3,
        titleRes = R.string.fc_onboarding_title_3,
        descriptionRes = R.string.fc_onboarding_desc_3,
        buttonRes = R.string.fc_next,
        showArrow = true,
        descriptionWidth = 338.dp,
        descriptionHeight = 88.dp,
        adaptiveDescriptionHeight = 128.dp,
    ),
    OnboardingPageUi(
        figureRes = R.mipmap.fc_onboarding_figure_4,
        titleRes = R.string.fc_onboarding_title_4,
        descriptionRes = R.string.fc_onboarding_desc_4,
        buttonRes = R.string.fc_onboarding_start,
        showArrow = false,
        haloSize = 233.dp,
        haloOffsetY = 17.dp,
        descriptionWidth = 338.dp,
        descriptionHeight = 88.dp,
        adaptiveDescriptionHeight = 136.dp,
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
    val descriptionScrollState = rememberScrollState()
    val palette = rememberOnboardingPalette()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        content()

        Text(
            text = stringResource(current.titleRes),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = current.titleTop)
                .width(current.titleWidth),
            color = palette.textPrimary,
            fontSize = nonScaledSp(20),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

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
            color = palette.textSecondary,
            fontSize = nonScaledSp(textLayout.descriptionFontSize),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = nonScaledSp(textLayout.descriptionLineHeight),
            overflow = TextOverflow.Clip
        )

        OnboardingPagination(
            pageCount = pages.size,
            currentPage = currentPage,
            palette = palette,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = current.indicatorTop)
        )

        OnboardingActionButton(
            text = stringResource(current.buttonRes),
            showArrow = current.showArrow,
            palette = palette,
            onClick = onActionClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = current.actionTop)
        )
    }
}

@Composable
private fun OnboardingPage(page: OnboardingPageUi) {
    Box(modifier = Modifier.fillMaxSize()) {
        val palette = rememberOnboardingPalette()

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = page.illustrationTop)
                .width(page.illustrationBoxWidth)
                .height(page.illustrationBoxHeight)
        ) {
            if (page.haloSize != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = page.haloOffsetY)
                        .size(page.haloSize)
                        .background(
                            color = palette.illustrationHalo,
                            shape = CircleShape
                        )
                )
            }

            Image(
                painter = painterResource(page.figureRes),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = page.illustrationOffsetX, y = page.illustrationOffsetY)
                    .width(page.illustrationWidth)
                    .height(page.illustrationHeight),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun OnboardingPagination(
    pageCount: Int,
    currentPage: Int,
    palette: OnboardingPalette,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(6.dp)
                    .background(
                        color = if (index == currentPage) palette.accent else palette.indicatorInactive,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
private fun OnboardingActionButton(
    text: String,
    showArrow: Boolean,
    palette: OnboardingPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(116.dp)
            .height(44.dp),
        shape = RoundedCornerShape(37.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = nonScaledSp(16),
                fontWeight = FontWeight.SemiBold
            )
            if (showArrow) {
                OnboardingArrow(
                    modifier = Modifier
                        .offset(x = 7.dp)
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
        val shaftEndX = size.width * 0.62f
        drawLine(
            color = Color.White,
            start = Offset(0f, midY),
            end = Offset(shaftEndX, midY),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.44f, size.height * 0.14f),
            end = Offset(size.width, midY),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.44f, size.height * 0.86f),
            end = Offset(size.width, midY),
            strokeWidth = strokeWidth
        )
    }
}

@Preview(
    name = "Onboarding Page 1",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    widthDp = 375,
    heightDp = 812,
    showSystemUi = true,
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
