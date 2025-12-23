package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daily.health.manager.data.entity.CholesterolRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CholesterolDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CholesterolRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CholesterolRecord>): List<Long>

    @Update
    suspend fun update(record: CholesterolRecord): Int

    @Query("SELECT * FROM cholesterol_records WHERE id = :id AND is_delete = 0")
    suspend fun getById(id: Long): CholesterolRecord?

    @Query("SELECT * FROM cholesterol_records WHERE id = :id AND is_delete = 0")
    fun observeById(id: Long): Flow<CholesterolRecord?>

    @Query("SELECT * FROM cholesterol_records WHERE is_delete = 0 ORDER BY record_time DESC LIMIT 1")
    suspend fun getLatestRecord(): CholesterolRecord?

    @Query("SELECT * FROM cholesterol_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC")
    fun getAllRecords(): Flow<List<CholesterolRecord>>

    @Query(
        "SELECT * FROM cholesterol_records WHERE is_delete = 0 " +
            "AND record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC, updated_at DESC"
    )
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<CholesterolRecord>>

    @Query(
        "SELECT * FROM cholesterol_records WHERE is_delete = 0 " +
            "AND tag_ids LIKE '%' || :tagId || '%' ORDER BY record_time DESC, updated_at DESC"
    )
    suspend fun getRecordsByTagId(tagId: String): List<CholesterolRecord>

    /**
     * 获取最近N条胆固醇记录
     * @param limit 记录数量限制
     * @return Flow形式的胆固醇记录列表
     */
    @Query("SELECT * FROM cholesterol_records WHERE is_delete = 0 ORDER BY record_time DESC, updated_at DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<CholesterolRecord>>

    @Query("UPDATE cholesterol_records SET is_delete = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM cholesterol_records")
    suspend fun deleteAll(): Int
}
