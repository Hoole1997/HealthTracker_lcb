package com.daily.health.manager.presentation.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.daily.health.manager.R
import kotlinx.coroutines.delay
import java.time.LocalTime

/** Device-local periods: 05–10 morning, 11–12 noon, 13–17 afternoon, otherwise evening. */
internal enum class HomeDayPeriod(@StringRes val label: Int) {
    MORNING(R.string.tr_home_greeting_morning),
    NOON(R.string.tr_home_greeting_noon),
    AFTERNOON(R.string.tr_home_greeting_afternoon),
    EVENING(R.string.tr_home_greeting_evening);

    companion object {
        fun forHour(hour: Int): HomeDayPeriod {
            require(hour in 0..23)
            return when (hour) {
                in 5..10 -> MORNING
                in 11..12 -> NOON
                in 13..17 -> AFTERNOON
                else -> EVENING
            }
        }
    }
}

@Composable
internal fun currentHomeGreeting(): String {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        // Refresh on every resume and while visible; stopped screens do not retain a timer.
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                hour = LocalTime.now().hour
                delay(60_000)
            }
        }
    }
    return stringResource(HomeDayPeriod.forHour(hour).label)
}
