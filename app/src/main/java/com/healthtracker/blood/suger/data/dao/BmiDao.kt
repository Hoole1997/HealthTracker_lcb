package com.healthtracker.blood.suger.data.dao

import androidx.room.*
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
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
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC")
    fun getAllRecords(): Flow<List<BmiRecord>>

    /** 获取最近一条记录（过滤已删除） */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC LIMIT 1")
    suspend fun getLatestRecord(): BmiRecord?

    /** 按时间范围查询（过滤已删除） */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 AND record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC, updated_at DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BmiRecord>>

    /**
     * 获取最近N条BMI记录
     * @param limit 记录数量限制
     * @return Flow形式的BMI记录列表
     */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<BmiRecord>>

    /** 按标签ID模糊查询（过滤已删除） */
    @Query("SELECT * FROM bmi_records WHERE is_delete = 0 AND tag_ids LIKE '%' || :tagId || '%' ORDER BY record_time DESC")
    suspend fun getRecordsByTagId(tagId: String): List<BmiRecord>

    /** 软删除：仅更新标记 */
    @Query("UPDATE bmi_records SET is_delete = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    /** 批量插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BmiRecord>): List<Long>

    /** 清空所有记录（硬删除） */
    @Query("DELETE FROM bmi_records")
    suspend fun deleteAllRecords(): Int


    /**
     * 根据ID监听血糖记录变化
     */
    @Query("SELECT * FROM bmi_records WHERE id = :id")
    fun observeById(id: Long): Flow<BmiRecord?>
}