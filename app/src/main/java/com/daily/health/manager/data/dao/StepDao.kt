package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daily.health.manager.data.entity.DailyStepStat
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {
    @Query("SELECT * FROM daily_step_stats WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeByDate(dateEpochDay: Long): Flow<DailyStepStat?>

    @Query("SELECT * FROM daily_step_stats WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getByDate(dateEpochDay: Long): DailyStepStat?

    @Query("SELECT * FROM daily_step_stats WHERE dateEpochDay BETWEEN :start AND :end ORDER BY dateEpochDay ASC")
    fun getRange(start: Long, end: Long): Flow<List<DailyStepStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStepStat)

    @Query(
        "UPDATE daily_step_stats SET steps = :steps, distanceKm = :distanceKm, kcal = :kcal, durationSeconds = :durationSeconds, lastRaw = :lastRaw, updatedAt = :updatedAt WHERE dateEpochDay = :dateEpochDay"
    )
    suspend fun updateMetrics(
        dateEpochDay: Long,
        steps: Int,
        distanceKm: Double,
        kcal: Double,
        durationSeconds: Int,
        lastRaw: Int,
        updatedAt: Long
    )
}