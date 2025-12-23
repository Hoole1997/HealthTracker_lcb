package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.daily.health.manager.data.entity.DailyStepStat
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao11 {
    @Query("SELECT * FROM t11 WHERE c01 = :dateEpochDay LIMIT 1")
    fun observeByDate(dateEpochDay: Long): Flow<DailyStepStat?>

    @Query("SELECT * FROM t11 WHERE c01 = :dateEpochDay LIMIT 1")
    suspend fun getByDate(dateEpochDay: Long): DailyStepStat?

    @Query("SELECT * FROM t11 WHERE c01 BETWEEN :start AND :end ORDER BY c01 ASC")
    fun getRange(start: Long, end: Long): Flow<List<DailyStepStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stat: DailyStepStat)

    @Query(
        "UPDATE t11 SET c02 = :steps, c03 = :distanceKm, c04 = :kcal, c05 = :durationSeconds, c07 = :lastRaw, c08 = :updatedAt WHERE c01 = :dateEpochDay"
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

typealias StepDao = LocalDao11