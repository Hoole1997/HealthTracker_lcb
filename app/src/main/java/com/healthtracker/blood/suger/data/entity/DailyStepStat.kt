package com.healthtracker.blood.suger.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_step_stats")
data class DailyStepStat(
    @PrimaryKey val dateEpochDay: Long,
    val steps: Int,
    val distanceKm: Double,
    val kcal: Double,
    val durationSeconds: Int,
    val baselineRaw: Int,
    val lastRaw: Int,
    val updatedAt: Long
)