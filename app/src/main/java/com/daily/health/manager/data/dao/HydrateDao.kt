package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daily.health.manager.data.entity.HydrateRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 饮水记录数据访问对象
 */
@Dao
interface LocalDao09 {

    /** 插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HydrateRecord): Long

    /** 批量插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HydrateRecord>): List<Long>

    /** 更新 */
    @Update
    suspend fun update(record: HydrateRecord): Int

    /** 根据ID获取 */
    @Query("SELECT * FROM t09 WHERE c01 = :id")
    suspend fun getById(id: Long): HydrateRecord?

    /** 根据ID监听 */
    @Query("SELECT * FROM t09 WHERE c01 = :id")
    fun observeById(id: Long): Flow<HydrateRecord?>

    /** 获取最近一条记录 */
    @Query("SELECT * FROM t09 ORDER BY c02 DESC LIMIT 1")
    suspend fun getLatestRecord(): HydrateRecord?

    /** 获取全部记录（按时间倒序） */
    @Query("SELECT * FROM t09 ORDER BY c02 DESC")
    fun getAllRecords(): Flow<List<HydrateRecord>>

    /** 按时间范围查询 */
    @Query("SELECT * FROM t09 WHERE c02 BETWEEN :startTime AND :endTime ORDER BY c02 DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<HydrateRecord>>

    /** 按时间范围一次性查询（非 Flow） */
    @Query("SELECT * FROM t09 WHERE c02 BETWEEN :startTime AND :endTime ORDER BY c02 DESC")
    suspend fun getRecordsByTimeRangeOnce(startTime: Date, endTime: Date): List<HydrateRecord>

    /** 根据ID删除 */
    @Query("DELETE FROM t09 WHERE c01 = :id")
    suspend fun deleteById(id: Long): Int

    /** 清空记录 */
    @Query("DELETE FROM t09")
    suspend fun deleteAll(): Int

    /**
     * 获取最近N条BMI记录
     * @param limit 记录数量限制
     * @return Flow形式的BMI记录列表
     */
    @Query("SELECT * FROM t09 WHERE c07 = 0 ORDER BY c02 DESC, c08 DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<HydrateRecord>>

    /**
     * 批量更新指定时间范围内（通常为当天）所有饮水记录的设置快照字段
     * 仅更新未软删除的记录
     */
    @Query(
        "UPDATE t09 " +
        "SET c04 = :dailyGoalCups, " +
        "c05 = :cupVolumeMl, " +
        "c06 = :dailyGoalTotalMl, " +
        "c08 = :updatedAt " +
        "WHERE c07 = 0 AND c02 BETWEEN :startTime AND :endTime"
    )
    suspend fun updateRecordSettingsByTimeRange(
        startTime: Date,
        endTime: Date,
        dailyGoalCups: Int,
        cupVolumeMl: Int,
        dailyGoalTotalMl: Int,
        updatedAt: Long
    ): Int
}

typealias HydrateDao = LocalDao09