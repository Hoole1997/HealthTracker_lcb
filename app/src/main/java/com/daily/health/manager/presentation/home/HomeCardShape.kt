package com.daily.health.manager.presentation.home

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cards and guide cutouts must share their radii, otherwise the scrim reveals a corner fringe. */
internal object HomeCardShape {
    val featureCornerRadius = 16.dp

    fun heroCornerRadius(cardWidth: Dp): Dp = cardWidth * (20f / 342f)
}
