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
interface HydrateDao {

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
    @Query("SELECT * FROM hydrate_records WHERE id = :id")
    suspend fun getById(id: Long): HydrateRecord?

    /** 根据ID监听 */
    @Query("SELECT * FROM hydrate_records WHERE id = :id")
    fun observeById(id: Long): Flow<HydrateRecord?>

    /** 获取最近一条记录 */
    @Query("SELECT * FROM hydrate_records ORDER BY record_time DESC LIMIT 1")
    suspend fun getLatestRecord(): HydrateRecord?

    /** 获取全部记录（按时间倒序） */
    @Query("SELECT * FROM hydrate_records ORDER BY record_time DESC")
    fun getAllRecords(): Flow<List<HydrateRecord>>

    /** 按时间范围查询 */
    @Query("SELECT * FROM hydrate_records WHERE record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<HydrateRecord>>

    /** 根据ID删除 */
    @Query("DELETE FROM hydrate_records WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** 清空记录 */
    @Query("DELETE FROM hydrate_records")
    suspend fun deleteAll(): Int

    /**
     * 获取最近N条BMI记录
     * @param limit 记录数量限制
     * @return Flow形式的BMI记录列表
     */
    @Query("SELECT * FROM hydrate_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<HydrateRecord>>

    /**
     * 批量更新指定时间范围内（通常为当天）所有饮水记录的设置快照字段
     * 仅更新未软删除的记录
     */
    @Query(
        "UPDATE hydrate_records " +
        "SET daily_goal_cups = :dailyGoalCups, " +
        "cup_volume_ml = :cupVolumeMl, " +
        "daily_goal_total_ml = :dailyGoalTotalMl, " +
        "updated_at = :updatedAt " +
        "WHERE is_delete = 0 AND record_time BETWEEN :startTime AND :endTime"
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