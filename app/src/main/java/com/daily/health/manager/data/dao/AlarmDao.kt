package com.daily.health.manager.data.dao

import androidx.room.*
import com.daily.health.manager.data.entity.AlarmRecord
import kotlinx.coroutines.flow.Flow

/**
 * 闹钟记录数据访问对象(DAO)
 * 提供闹钟数据的CRUD操作
 */
@Dao
interface LocalDao04 {

    /**
     * 插入闹钟记录
     * @param record 闹钟记录对象
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AlarmRecord): Long

    /**
     * 批量插入闹钟记录
     * @param records 闹钟记录列表
     * @return 插入记录的ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AlarmRecord>): List<Long>

    /**
     * 更新闹钟记录
     * @param record 闹钟记录对象
     * @return 影响的行数
     */
    @Update
    suspend fun update(record: AlarmRecord): Int

    /**
     * 删除闹钟记录
     * @param record 闹钟记录对象
     * @return 影响的行数
     */
    @Delete
    suspend fun delete(record: AlarmRecord): Int

    /**
     * 根据ID删除闹钟记录
     * @param id 记录ID
     * @return 影响的行数
     */
    @Query("DELETE FROM t04 WHERE c01 = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * 软删除闹钟记录
     * @param id 记录ID
     * @return 影响的行数
     */
    @Query("UPDATE t04 SET c09 = 1 WHERE c01 = :id")
    suspend fun softDeleteById(id: Long): Int

    /**
     * 根据ID获取闹钟记录
     * @param id 记录ID
     * @return 闹钟记录对象，可能为null
     */
    @Query("SELECT * FROM t04 WHERE c01 = :id AND c09 = 0")
    suspend fun getById(id: Long): AlarmRecord?

    /**
     * 获取所有有效的闹钟记录，按时间排序
     * @return Flow形式的闹钟记录列表，支持数据变化监听
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 ORDER BY c03 ASC, c04 ASC")
    fun getAllRecords(): Flow<List<AlarmRecord>>

    /**
     * 获取所有启用的闹钟记录
     * @return Flow形式的启用闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 AND c07 = 1 ORDER BY c03 ASC, c04 ASC")
    fun getEnabledRecords(): Flow<List<AlarmRecord>>

    /**
     * 根据类型获取闹钟记录
     * @param type 闹钟类型
     * @return Flow形式的闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 AND c02 = :type ORDER BY c03 ASC, c04 ASC")
    fun getRecordsByType(type: Int): Flow<List<AlarmRecord>>

    /**
     * 根据类型与具体时间获取闹钟记录
     * @param type 闹钟类型
     * @param hour 小时
     * @param minute 分钟
     * @return 匹配的闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 AND c02 = :type AND c03 = :hour AND c04 = :minute")
    suspend fun getRecordsByTypeAndTime(type: Int, hour: Int, minute: Int): List<AlarmRecord>

    /**
     * 根据时间范围获取闹钟记录
     * @param startHour 开始小时
     * @param endHour 结束小时
     * @return Flow形式的闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 AND c03 BETWEEN :startHour AND :endHour ORDER BY c03 ASC, c04 ASC")
    fun getRecordsByTimeRange(startHour: Int, endHour: Int): Flow<List<AlarmRecord>>

    /**
     * 获取重复闹钟记录
     * @return Flow形式的重复闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 AND c05 > 0 ORDER BY c03 ASC, c04 ASC")
    fun getRepeatingRecords(): Flow<List<AlarmRecord>>

    /**
     * 获取单次闹钟记录
     * @return Flow形式的单次闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 AND c05 = 0 ORDER BY c03 ASC, c04 ASC")
    fun getOnceRecords(): Flow<List<AlarmRecord>>

    /**
     * 更新闹钟启用状态
     * @param id 记录ID
     * @param isEnabled 是否启用
     * @return 影响的行数
     */
    @Query("UPDATE t04 SET c07 = :isEnabled WHERE c01 = :id")
    suspend fun updateEnabledStatus(id: Long, isEnabled: Boolean): Int

    /**
     * 更新最后触发时间
     * @param id 记录ID
     * @param lastTriggerTime 最后触发时间戳
     * @return 影响的行数
     */
    @Query("UPDATE t04 SET c17 = :lastTriggerTime WHERE c01 = :id")
    suspend fun updateLastTriggerTime(id: Long, lastTriggerTime: Long): Int

    /**
     * 获取记录总数
     * @return 记录总数
     */
    @Query("SELECT COUNT(*) FROM t04 WHERE c09 = 0")
    suspend fun getRecordCount(): Int

    /**
     * 同步获取所有有效的闹钟记录，按时间排序
     * @return 闹钟记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 0 ORDER BY c03 ASC, c04 ASC")
    suspend fun getAllRecordsSync(): List<AlarmRecord>

    /**
     * 获取启用的闹钟记录总数
     * @return 启用记录总数
     */
    @Query("SELECT COUNT(*) FROM t04 WHERE c09 = 0 AND c07 = 1")
    suspend fun getEnabledRecordCount(): Int

    /**
     * 根据类型获取闹钟记录总数
     * @param type 闹钟类型
     * @return 指定类型的记录总数
     */
    @Query("SELECT COUNT(*) FROM t04 WHERE c09 = 0 AND c02 = :type")
    suspend fun getRecordCountByType(type: Int): Int

    /**
     * 检查指定时间是否已存在闹钟
     * @param hour 小时
     * @param minute 分钟
     * @param excludeId 排除的记录ID（用于更新时检查）
     * @return 是否存在相同时间的闹钟
     */
    @Query("SELECT COUNT(*) > 0 FROM t04 WHERE c09 = 0 AND c03 = :hour AND c04 = :minute AND c01 != :excludeId")
    suspend fun existsAtTime(hour: Int, minute: Int, excludeId: Long = -1): Boolean

    /**
     * 检查指定类型在指定时间是否已存在闹钟
     * 允许不同类型在同一时间共存
     *
     * @param type 闹钟类型
     * @param hour 小时
     * @param minute 分钟
     * @param excludeId 排除的记录ID（用于更新时检查）
     */
    @Query("SELECT COUNT(*) > 0 FROM t04 WHERE c09 = 0 AND c02 = :type AND c03 = :hour AND c04 = :minute AND c01 != :excludeId")
    suspend fun existsAtTypeAndTime(type: Int, hour: Int, minute: Int, excludeId: Long = -1): Boolean

    /**
     * 获取下一个即将触发的闹钟
     * @param currentHour 当前小时
     * @param currentMinute 当前分钟
     * @return 下一个闹钟记录，可能为null
     */
    @Query("""
        SELECT * FROM t04 
        WHERE c09 = 0 AND c07 = 1 
        AND (c03 > :currentHour OR (c03 = :currentHour AND c04 > :currentMinute))
        ORDER BY c03 ASC, c04 ASC 
        LIMIT 1
    """)
    suspend fun getNextAlarm(currentHour: Int, currentMinute: Int): AlarmRecord?

    /**
     * 删除所有记录（慎用）
     * @return 影响的行数
     */
    @Query("DELETE FROM t04")
    suspend fun deleteAllRecords(): Int

    /**
     * 软删除所有记录
     * @return 影响的行数
     */
    @Query("UPDATE t04 SET c09 = 1")
    suspend fun softDeleteAllRecords(): Int

    /**
     * 恢复软删除的记录
     * @param id 记录ID
     * @return 影响的行数
     */
    @Query("UPDATE t04 SET c09 = 0 WHERE c01 = :id")
    suspend fun restoreRecord(id: Long): Int

    /**
     * 获取已删除的记录（回收站功能）
     * @return Flow形式的已删除记录列表
     */
    @Query("SELECT * FROM t04 WHERE c09 = 1 ORDER BY c03 ASC, c04 ASC")
    fun getDeletedRecords(): Flow<List<AlarmRecord>>

    /**
     * 永久删除已软删除的记录
     * @return 影响的行数
     */
    @Query("DELETE FROM t04 WHERE c09 = 1")
    suspend fun permanentlyDeleteSoftDeletedRecords(): Int
}

typealias AlarmDao = LocalDao04