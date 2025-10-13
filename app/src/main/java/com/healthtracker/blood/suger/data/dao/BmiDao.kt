package com.healthtracker.blood.suger.data.dao

import androidx.room.*
import com.healthtracker.blood.suger.data.entity.BmiRecord
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * BMI记录数据访问对象（最简实现）
 */
@Dao
interface BmiDao {

    /** 插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BmiRecord): Long

    /** 更新（建议在调用处使用 record.withUpdatedTimestamp() 保持时间戳更新） */
    @Update
    suspend fun update(record: BmiRecord): Int

    /** 根据ID获取 */
    @Query("SELECT * FROM bmi_records WHERE id = :id AND is_delete = 0")
    suspend fun getById(id: Long): BmiRecord?

    /** 获取所有（过滤已删除），按时间倒序 */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 ORDER BY record_time DESC")
    fun getAllRecords(): Flow<List<BmiRecord>>

    /** 获取最近一条记录（过滤已删除） */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 ORDER BY record_time DESC LIMIT 1")
    suspend fun getLatestRecord(): BmiRecord?

    /** 按时间范围查询（过滤已删除） */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 AND record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BmiRecord>>

    /** 按标签ID模糊查询（过滤已删除） */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 AND tag_ids LIKE '%' || :tagId || '%' ORDER BY record_time DESC")
    suspend fun getRecordsByTagId(tagId: String): List<BmiRecord>

    /** 软删除：仅更新标记 */
    @Query("UPDATE bmi_records SET is_delete = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int
}