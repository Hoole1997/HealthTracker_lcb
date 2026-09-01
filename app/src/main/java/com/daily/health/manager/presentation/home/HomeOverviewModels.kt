package com.daily.health.manager.presentation.home

import androidx.annotation.ColorRes
import androidx.compose.runtime.Immutable

/** 仅用于首页展示；不承担持久化、页面导航或 SDK 注册职责。 */
@Immutable
data class HeartSummaryUi(
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
sealed class MetricTileUi(open val title: String) {
    data class BloodPressure(override val title: String, val value: String, val unit: String) : MetricTileUi(title)
    data class BloodSugar(override val title: String, val value: String, val unit: String) : MetricTileUi(title)
    data class Cholesterol(override val title: String, val value: String, val unit: String) : MetricTileUi(title)
    data class Bmi(override val title: String, val value: String, val unit: String) : MetricTileUi(title)
    data class Hydrate(override val title: String, val currentValue: String, val targetValue: String, val unit: String) : MetricTileUi(title)
    data class StepCount(override val title: String, val stepsValue: String, val stepsUnit: String, val kcalValue: String, val kcalUnit: String) : MetricTileUi(title)
}
