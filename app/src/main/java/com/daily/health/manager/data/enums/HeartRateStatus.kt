package com.daily.health.manager.data.enums

import com.daily.health.manager.R
import com.daily.health.manager.ui.weight.LevelCategory

/**
 * 静息心率等级
 */
enum class HeartRateStatus(
    private val minInclusive: Int?,
    private val maxInclusive: Int?,
    val statusTextRes: Int,
    val descriptionRes: Int,
    override val colorRes: Int
) : LevelCategory {
    SLOW(
        minInclusive = null,
        maxInclusive = 59,
        statusTextRes = R.string.ht_heart_rate_status_slow,
        descriptionRes = R.string.ht_heart_rate_desc_slow,
        colorRes = R.color.color_low
    ),
    NORMAL(
        minInclusive = 60,
        maxInclusive = 100,
        statusTextRes = R.string.ht_heart_rate_status_normal,
        descriptionRes = R.string.ht_heart_rate_desc_normal,
        colorRes = R.color.color_05BA7B
    ),
    FAST(
        minInclusive = 101,
        maxInclusive = null,
        statusTextRes = R.string.ht_heart_rate_status_fast,
        descriptionRes = R.string.ht_heart_rate_desc_fast,
        colorRes = R.color.color_FF8000
    );

    fun contains(bpm: Int): Boolean {
        val minSatisfied = minInclusive?.let { bpm >= it } ?: true
        val maxSatisfied = maxInclusive?.let { bpm <= it } ?: true
        return minSatisfied && maxSatisfied
    }

    companion object {
        fun fromHeartRate(bpm: Int): HeartRateStatus {
            return entries.firstOrNull { it.contains(bpm) } ?: NORMAL
        }
    }
}
