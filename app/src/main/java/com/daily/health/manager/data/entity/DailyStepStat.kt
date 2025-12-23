package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "t11")
data class LocalEntity11(
    @PrimaryKey
    @ColumnInfo(name = "c01")
    val dateEpochDay: Long,
    @ColumnInfo(name = "c02")
    val steps: Int,
    @ColumnInfo(name = "c03")
    val distanceKm: Double,
    @ColumnInfo(name = "c04")
    val kcal: Double,
    @ColumnInfo(name = "c05")
    val durationSeconds: Int,
    @ColumnInfo(name = "c06")
    val baselineRaw: Int,
    @ColumnInfo(name = "c07")
    val lastRaw: Int,
    @ColumnInfo(name = "c08")
    val updatedAt: Long
)

typealias DailyStepStat = LocalEntity11