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

}

typealias StepDao = LocalDao11
