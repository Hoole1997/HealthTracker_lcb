package com.daily.health.manager.data.dao

import androidx.room.*
import com.daily.health.manager.data.entity.BloodPressureRecord
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 血压记录数据访问对象(DAO)
 * 提供血压数据的CRUD操作
 */
@Dao
interface LocalDao02 {

    /**
     * 插入血压记录
     * @param record 血压记录对象
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BloodPressureRecord): Long

    /**
     * 批量插入血压记录
     * @param records 血压记录列表
     * @return 插入记录的ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BloodPressureRecord>): List<Long>

    /**
     * 更新血压记录
     * @param record 血压记录对象
     * @return 影响的行数
     */
    @Update
    suspend fun update(record: BloodPressureRecord): Int

    /**
     * 删除血压记录
     * @param record 血压记录对象
     * @return 影响的行数
     */
    @Delete
    suspend fun delete(record: BloodPressureRecord): Int

    /**
     * 根据ID删除血压记录
     * @param id 记录ID
     * @return 影响的行数
     */
    @Query("DELETE FROM t02 WHERE c01 = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * 根据ID获取血压记录
     * @param id 记录ID
     * @return 血压记录对象，可能为null
     */
    @Query("SELECT * FROM t02 WHERE c01 = :id")
    suspend fun getById(id: Long): BloodPressureRecord?

    /**
     * 根据ID获取血压记录（Flow响应式）
     * @param id 记录ID
     * @return Flow形式的血压记录对象，支持数据变化监听
     */
    @Query("SELECT * FROM t02 WHERE c01 = :id")
    fun getByIdFlow(id: Long): Flow<BloodPressureRecord?>

    /**
     * 获取所有血压记录，按时间倒序排列
     * @return Flow形式的血压记录列表，支持数据变化监听
     */
    @Query("SELECT * FROM t02 ORDER BY c02 DESC, c03 DESC")
    fun getAllRecords(): Flow<List<BloodPressureRecord>>

    /**
     * 获取可在图表中显示的血压记录，按时间排序
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c09 = 1 ORDER BY c02 ASC")
    fun getChartRecords(): Flow<List<BloodPressureRecord>>

    /**
     * 根据时间范围获取血压记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c02 BETWEEN :startTime AND :endTime ORDER BY c02 DESC, c03 DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodPressureRecord>>



    /**
     * 根据血压分类获取血压记录
     * @param bpCategory 血压分类code
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c07 = :bpCategory ORDER BY c02 DESC, c03 DESC")
    fun getRecordsByBloodPressureCategory(bpCategory: String): Flow<List<BloodPressureRecord>>

    /**
     * 根据脉搏分类获取血压记录
     * @param pulseCategory 脉搏分类code
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c08 = :pulseCategory ORDER BY c02 DESC, c03 DESC")
    fun getRecordsByPulseCategory(pulseCategory: String): Flow<List<BloodPressureRecord>>

    /**
     * 根据收缩压范围获取血压记录
     * @param minSystolic 最小收缩压
     * @param maxSystolic 最大收缩压
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c04 BETWEEN :minSystolic AND :maxSystolic ORDER BY c02 DESC, c03 DESC")
    fun getRecordsBySystolicRange(minSystolic: Int, maxSystolic: Int): Flow<List<BloodPressureRecord>>

    /**
     * 根据舒张压范围获取血压记录
     * @param minDiastolic 最小舒张压
     * @param maxDiastolic 最大舒张压
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c05 BETWEEN :minDiastolic AND :maxDiastolic ORDER BY c02 DESC, c03 DESC")
    fun getRecordsByDiastolicRange(minDiastolic: Int, maxDiastolic: Int): Flow<List<BloodPressureRecord>>

    /**
     * 根据脉搏范围获取血压记录
     * @param minPulse 最小脉搏
     * @param maxPulse 最大脉搏
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c06 BETWEEN :minPulse AND :maxPulse ORDER BY c02 DESC, c03 DESC")
    fun getRecordsByPulseRange(minPulse: Int, maxPulse: Int): Flow<List<BloodPressureRecord>>

    /**
     * 获取最近N条血压记录
     * @param limit 记录数量限制
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM t02 ORDER BY c02 DESC, c03 DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<BloodPressureRecord>>

    /**
     * 获取血压记录总数
     * @return 记录总数
     */
    @Query("SELECT COUNT(*) FROM t02")
    suspend fun getRecordCount(): Int

    /**
     * 获取平均收缩压
     * @return 平均收缩压
     */
    @Query("SELECT AVG(c04) FROM t02")
    suspend fun getAverageSystolic(): Double?

    /**
     * 获取平均舒张压
     * @return 平均舒张压
     */
    @Query("SELECT AVG(c05) FROM t02")
    suspend fun getAverageDiastolic(): Double?

    /**
     * 获取平均脉搏
     * @return 平均脉搏
     */
    @Query("SELECT AVG(c06) FROM t02")
    suspend fun getAveragePulse(): Double?

    /**
     * 获取最高血压记录
     * @return 最高血压记录
     */
    @Query("SELECT * FROM t02 ORDER BY c04 DESC LIMIT 1")
    suspend fun getHighestBloodPressureRecord(): BloodPressureRecord?

    /**
     * 获取最低血压记录
     * @return 最低血压记录
     */
    @Query("SELECT * FROM t02 ORDER BY c04 ASC LIMIT 1")
    suspend fun getLowestBloodPressureRecord(): BloodPressureRecord?

    /**
     * 获取血压统计信息
     * @return 血压统计数据
     */
    @Query("""
       SELECT
           COUNT(*) as total_count,
           AVG(c04) as avg_systolic,
           AVG(c05) as avg_diastolic,
           AVG(c06) as avg_pulse,
           MAX(c04) as max_systolic,
           MIN(c04) as min_systolic,
           MAX(c05) as max_diastolic,
           MIN(c05) as min_diastolic
       FROM t02
   """)
    suspend fun getBloodPressureStatistics(): BloodPressureStatistics?

    /**
     * 清空所有血压记录
     * @return 影响的行数
     */
    @Query("DELETE FROM t02")
    suspend fun deleteAllRecords(): Int

    /**
     * 更新记录的图表显示状态
     * @param id 记录ID
     * @param showInChart 是否显示在图表中
     * @return 影响的行数
     */
    @Query("UPDATE t02 SET c09 = :showInChart WHERE c01 = :id")
    suspend fun updateChartVisibility(id: Long, showInChart: Boolean): Int

    /**
     * 根据标签ID获取血压记录
     * @param tagId 标签ID
     * @return 包含该标签的血压记录列表
     */
    @Query("SELECT * FROM t02 WHERE c10 LIKE '%' || :tagId || '%' ORDER BY c02 DESC, c03 DESC")
    suspend fun getRecordsByTagId(tagId: String): List<BloodPressureRecord>

    /**
     * 获取包含标签的血压记录数量
     * @return 包含标签的记录数量
     */
    @Query("SELECT COUNT(*) FROM t02 WHERE c10 IS NOT NULL AND c10 != ''")
    suspend fun getRecordsWithTagsCount(): Int
}

 typealias BloodPressureDao = LocalDao02

/**
 * 血压统计数据类
 */
data class BloodPressureStatistics(
    @ColumnInfo(name = "total_count")
    val totalCount: Int,
    @ColumnInfo(name = "avg_systolic")
    val avgSystolic: Double?,
    @ColumnInfo(name = "avg_diastolic")
    val avgDiastolic: Double?,
    @ColumnInfo(name = "avg_pulse")
    val avgPulse: Double?,
    @ColumnInfo(name = "max_systolic")
    val maxSystolic: Int,
    @ColumnInfo(name = "min_systolic")
    val minSystolic: Int,
    @ColumnInfo(name = "max_diastolic")
    val maxDiastolic: Int,
    @ColumnInfo(name = "min_diastolic")
    val minDiastolic: Int
)