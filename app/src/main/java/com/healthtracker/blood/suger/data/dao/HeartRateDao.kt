package com.healthtracker.blood.suger.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 心率记录数据访问对象
 */
@Dao
interface HeartRateDao {

    /** 插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: HeartRateRecord): Long

    /** 批量插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HeartRateRecord>): List<Long>

    /** 更新 */
    @Update
    suspend fun update(record: HeartRateRecord): Int

    /** 根据ID获取 */
    @Query("SELECT * FROM heart_rate_records WHERE id = :id AND is_delete = 0")
    suspend fun getById(id: Long): HeartRateRecord?

    /** 根据ID监听 */
    @Query("SELECT * FROM heart_rate_records WHERE id = :id AND is_delete = 0")
    fun observeById(id: Long): Flow<HeartRateRecord?>

    /** 获取最近一条记录 */
    @Query("SELECT * FROM heart_rate_records WHERE is_delete = 0 ORDER BY record_time DESC LIMIT 1")
    suspend fun getLatestRecord(): HeartRateRecord?

    /** 获取全部记录（按时间倒序） */
    @Query("SELECT * FROM heart_rate_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC")
    fun getAllRecords(): Flow<List<HeartRateRecord>>

    /** 按时间范围查询 */
    @Query(
        "SELECT * FROM heart_rate_records WHERE is_delete = 0 " +
            "AND record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC, updated_at DESC"
    )
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<HeartRateRecord>>

    /** 软删除 */
    @Query("UPDATE heart_rate_records SET is_delete = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    /** 按标签查询 */
    @Query(
        "SELECT * FROM heart_rate_records WHERE is_delete = 0 " +
            "AND tag_ids LIKE '%' || :tagId || '%' ORDER BY record_time DESC, updated_at DESC"
    )
    suspend fun getRecordsByTagId(tagId: String): List<HeartRateRecord>

    /** 清空记录 */
    @Query("DELETE FROM heart_rate_records")
    suspend fun clearAll(): Int
}
