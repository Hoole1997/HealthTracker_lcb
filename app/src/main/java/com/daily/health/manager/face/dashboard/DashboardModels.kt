package com.daily.health.manager.face.dashboard

import androidx.annotation.ColorRes
import androidx.compose.runtime.Immutable

/** Display data only. Layout and artwork stay in the dashboard presentation layer. */
@Immutable
data class HomeHeroUi(
    val title: String,
    val subtitle: String,
    val cta: String,
    val value: String,
    val valueUnit: String,
    val footerText: String,
    val statusLabel: String? = null,
    @ColorRes val statusColorRes: Int? = null,
)

@Immutable
sealed class HomeFeatureCardUi(open val title: String) {
    data class BloodPressure(override val title: String, val value: String, val unit: String) : HomeFeatureCardUi(title)
    data class BloodSugar(override val title: String, val value: String, val unit: String) : HomeFeatureCardUi(title)
    data class Cholesterol(override val title: String, val value: String, val unit: String) : HomeFeatureCardUi(title)
    data class Bmi(override val title: String, val value: String, val unit: String) : HomeFeatureCardUi(title)
    data class Hydrate(override val title: String, val currentValue: String, val targetValue: String, val unit: String) : HomeFeatureCardUi(title)
    data class StepCount(override val title: String, val stepsValue: String, val stepsUnit: String, val kcalValue: String, val kcalUnit: String) : HomeFeatureCardUi(title)
}
