package com.daily.health.manager.face.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.daily.health.manager.R

@Composable
fun HealthTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Wallpaper colors are opt-in: default controls must match the application's fixed brand.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val base = remember(darkTheme) {
        if (darkTheme) darkColorScheme() else lightColorScheme()
    }
    val brandScheme = brandedColorScheme(base, darkTheme)
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        brandScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

/** Palette-only wrapper for legacy Compose roots: retain their typography, shapes and neutral colors. */
@Composable
private fun HealthTrackerColorTheme(content: @Composable () -> Unit) {
    val inherited = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = brandedColorScheme(inherited, inherited.surface.luminance() < 0.5f),
        content = content,
    )
}

/** Theme all View-hosted Compose controls without changing composition lifetime or screen callbacks. */
fun ComposeView.setBrandedContent(content: @Composable () -> Unit) {
    setContent { HealthTrackerColorTheme(content) }
}

@Composable
private fun brandedColorScheme(base: ColorScheme, darkTheme: Boolean): ColorScheme {
    val primary = colorResource(R.color.brand_primary)
    val onPrimary = colorResource(R.color.brand_on_primary)
    val container = colorResource(R.color.brand_primary_container)
    val strong = colorResource(R.color.brand_primary_strong)
    val track = colorResource(R.color.brand_primary_track)
    return remember(base, darkTheme, primary, onPrimary, container, strong, track) {
        // Keep Material's light/dark surfaces and error roles; only normalize brand roles.
        val selectedSurface = if (darkTheme) primary.copy(alpha = 0.2f).compositeOver(base.surface) else container
        val onSelectedSurface = if (darkTheme) track else strong
        base.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = selectedSurface,
            onPrimaryContainer = onSelectedSurface,
            secondary = primary,
            onSecondary = onPrimary,
            secondaryContainer = selectedSurface,
            onSecondaryContainer = onSelectedSurface,
            tertiary = primary,
            onTertiary = onPrimary,
            tertiaryContainer = selectedSurface,
            onTertiaryContainer = onSelectedSurface,
            surfaceTint = primary,
        )
    }
}
