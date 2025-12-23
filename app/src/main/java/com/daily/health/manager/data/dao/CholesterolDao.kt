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
interface LocalDao08 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CholesterolRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CholesterolRecord>): List<Long>

    @Update
    suspend fun update(record: CholesterolRecord): Int

    @Query("SELECT * FROM t08 WHERE c01 = :id AND c11 = 0")
    suspend fun getById(id: Long): CholesterolRecord?

    @Query("SELECT * FROM t08 WHERE c01 = :id AND c11 = 0")
    fun observeById(id: Long): Flow<CholesterolRecord?>

    @Query("SELECT * FROM t08 WHERE c11 = 0 ORDER BY c02 DESC LIMIT 1")
    suspend fun getLatestRecord(): CholesterolRecord?

    @Query("SELECT * FROM t08 WHERE c11 = 0 ORDER BY c02 DESC, c12 DESC")
    fun getAllRecords(): Flow<List<CholesterolRecord>>

    @Query(
        "SELECT * FROM t08 WHERE c11 = 0 " +
            "AND c02 BETWEEN :startTime AND :endTime ORDER BY c02 DESC, c12 DESC"
    )
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<CholesterolRecord>>

    @Query(
        "SELECT * FROM t08 WHERE c11 = 0 " +
            "AND c10 LIKE '%' || :tagId || '%' ORDER BY c02 DESC, c12 DESC"
    )
    suspend fun getRecordsByTagId(tagId: String): List<CholesterolRecord>

    /**
     * 获取最近N条胆固醇记录
     * @param limit 记录数量限制
     * @return Flow形式的胆固醇记录列表
     */
    @Query("SELECT * FROM t08 WHERE c11 = 0 ORDER BY c02 DESC, c12 DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<CholesterolRecord>>

    @Query("UPDATE t08 SET c11 = 1, c12 = :updatedAt WHERE c01 = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    @Query("DELETE FROM t08")
    suspend fun deleteAll(): Int
}

typealias CholesterolDao = LocalDao08
