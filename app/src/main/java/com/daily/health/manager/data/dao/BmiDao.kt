package com.daily.health.manager.data.dao
 
 import androidx.room.*
 import com.daily.health.manager.data.entity.BmiRecord
 import kotlinx.coroutines.flow.Flow
 import java.util.*

 /**
  * BMI记录数据访问对象（最简实现）
  */
 @Dao
 interface LocalDao06 {

    /** 插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BmiRecord): Long

    /** 更新（建议在调用处使用 record.withUpdatedTimestamp() 保持时间戳更新） */
    @Update
    suspend fun update(record: BmiRecord): Int

     /** 根据ID获取 */
     @Query("SELECT * FROM t06 WHERE c01 = :id AND c06 = 0")
     suspend fun getById(id: Long): BmiRecord?

     /** 获取所有（过滤已删除），按时间倒序 */
     @Query("SELECT * FROM t06 WHERE c06 = 0 ORDER BY c02 DESC, c07 DESC")
     fun getAllRecords(): Flow<List<BmiRecord>>

     /** 获取最近一条记录（过滤已删除） */
     @Query("SELECT * FROM t06 WHERE c06 = 0 ORDER BY c02 DESC, c07 DESC LIMIT 1")
     suspend fun getLatestRecord(): BmiRecord?

     /** 按时间范围查询（过滤已删除） */
     @Query("SELECT * FROM t06 WHERE c06 = 0 AND c02 BETWEEN :startTime AND :endTime ORDER BY c02 DESC, c07 DESC")
     fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BmiRecord>>

    /**
     * 获取最近N条BMI记录
     * @param limit 记录数量限制
     * @return Flow形式的BMI记录列表
     */
     @Query("SELECT * FROM t06 WHERE c06 = 0 ORDER BY c02 DESC, c07 DESC LIMIT :limit")
     fun getRecentRecords(limit: Int): Flow<List<BmiRecord>>

     /** 按标签ID模糊查询（过滤已删除） */
     @Query("SELECT * FROM t06 WHERE c06 = 0 AND c05 LIKE '%' || :tagId || '%' ORDER BY c02 DESC")
     suspend fun getRecordsByTagId(tagId: String): List<BmiRecord>

     /** 软删除：仅更新标记 */
     @Query("UPDATE t06 SET c06 = 1, c07 = :updatedAt WHERE c01 = :id")
     suspend fun softDeleteById(id: Long, updatedAt: Long = System.currentTimeMillis()): Int

    /** 批量插入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BmiRecord>): List<Long>

     /** 清空所有记录（硬删除） */
     @Query("DELETE FROM t06")
     suspend fun deleteAllRecords(): Int


     /**
      * 根据ID监听血糖记录变化
      */
     @Query("SELECT * FROM t06 WHERE c01 = :id")
     fun observeById(id: Long): Flow<BmiRecord?>
 }

 typealias BmiDao = LocalDao06