package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daily.health.manager.data.entity.HeartRateRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 心率记录数据访问对象
 */
@Dao
interface LocalDao07 {

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
    @Query("SELECT * FROM t07 WHERE c01 = :id AND c05 = 0")
    suspend fun getById(id: Long): HeartRateRecord?

    /** 根据ID监听 */
    @Query("SELECT * FROM t07 WHERE c01 = :id AND c05 = 0")
    fun observeById(id: Long): Flow<HeartRateRecord?>

    /** 获取最近一条记录 */
    @Query("SELECT * FROM t07 WHERE c05 = 0 ORDER BY c02 DESC LIMIT 1")
    suspend fun getLatestRecord(): HeartRateRecord?

    /** 获取最新的 PPG 记录 (ext2 == 'camera') */
    @Query("SELECT * FROM t07 WHERE c05 = 0 AND c08 = 'camera' ORDER BY c02 DESC LIMIT 1")
    fun observeLatestPpgRecord(): Flow<HeartRateRecord?>

    /** 获取全部记录（按时间倒序） */
    @Query("SELECT * FROM t07 WHERE c05 = 0 ORDER BY c02 DESC, c06 DESC")
    fun getAllRecords(): Flow<List<HeartRateRecord>>

    /** 按时间范围查询 */
    @Query(
        "SELECT * FROM t07 WHERE c05 = 0 " +
            "AND c02 BETWEEN :startTime AND :endTime ORDER BY c02 DESC, c06 DESC"
    )
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<HeartRateRecord>>

    /**
     * 获取最近N条心率记录
     * @param limit 记录数量限制
     * @return Flow形式的心率记录列表
     */
    @Query("SELECT * FROM t07 WHERE c05 = 0 ORDER BY c02 DESC, c06 DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<HeartRateRecord>>

    /** 软删除 */
    @Query("UPDATE t07 SET c05 = 1, c06 = :updatedAt WHERE c01 = :id")
    suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    /** 按标签查询 */
    @Query(
        "SELECT * FROM t07 WHERE c05 = 0 " +
            "AND c04 LIKE '%' || :tagId || '%' ORDER BY c02 DESC, c06 DESC"
    )
    suspend fun getRecordsByTagId(tagId: String): List<HeartRateRecord>

    /** 清空记录 */
    @Query("DELETE FROM t07")
    suspend fun clearAll(): Int
}

typealias HeartRateDao = LocalDao07
