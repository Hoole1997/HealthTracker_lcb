package com.healthtracker.blood.suger.data.dao

import androidx.room.*
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 血压记录数据访问对象(DAO)
 * 提供血压数据的CRUD操作
 */
@Dao
interface BloodPressureDao {

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
    @Query("DELETE FROM blood_pressure_records WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * 根据ID获取血压记录
     * @param id 记录ID
     * @return 血压记录对象，可能为null
     */
    @Query("SELECT * FROM blood_pressure_records WHERE id = :id")
    suspend fun getById(id: Long): BloodPressureRecord?

    /**
     * 获取所有血压记录，按时间倒序排列
     * @return Flow形式的血压记录列表，支持数据变化监听
     */
    @Query("SELECT * FROM blood_pressure_records ORDER BY record_time DESC")
    fun getAllRecords(): Flow<List<BloodPressureRecord>>

    /**
     * 获取可在图表中显示的血压记录，按时间排序
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE show_in_chart = 1 ORDER BY record_time ASC")
    fun getChartRecords(): Flow<List<BloodPressureRecord>>

    /**
     * 根据时间范围获取血压记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodPressureRecord>>

    /**
     * 根据测量标签获取血压记录
     * @param tag 测量标签
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE measurement_tag = :tag ORDER BY record_time DESC")
    fun getRecordsByTag(tag: String): Flow<List<BloodPressureRecord>>

    /**
     * 根据阶段分类获取血压记录
     * @param stage 阶段分类
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE stage = :stage ORDER BY record_time DESC")
    fun getRecordsByStage(stage: String): Flow<List<BloodPressureRecord>>

    /**
     * 根据收缩压范围获取血压记录
     * @param minSystolic 最小收缩压
     * @param maxSystolic 最大收缩压
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE systolic_pressure BETWEEN :minSystolic AND :maxSystolic ORDER BY record_time DESC")
    fun getRecordsBySystolicRange(minSystolic: Int, maxSystolic: Int): Flow<List<BloodPressureRecord>>

    /**
     * 根据舒张压范围获取血压记录
     * @param minDiastolic 最小舒张压
     * @param maxDiastolic 最大舒张压
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE diastolic_pressure BETWEEN :minDiastolic AND :maxDiastolic ORDER BY record_time DESC")
    fun getRecordsByDiastolicRange(minDiastolic: Int, maxDiastolic: Int): Flow<List<BloodPressureRecord>>

    /**
     * 根据脉搏范围获取血压记录
     * @param minPulse 最小脉搏
     * @param maxPulse 最大脉搏
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records WHERE pulse_rate BETWEEN :minPulse AND :maxPulse ORDER BY record_time DESC")
    fun getRecordsByPulseRange(minPulse: Int, maxPulse: Int): Flow<List<BloodPressureRecord>>

    /**
     * 获取最近N条血压记录
     * @param limit 记录数量限制
     * @return Flow形式的血压记录列表
     */
    @Query("SELECT * FROM blood_pressure_records ORDER BY record_time DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<BloodPressureRecord>>

    /**
     * 获取血压记录总数
     * @return 记录总数
     */
    @Query("SELECT COUNT(*) FROM blood_pressure_records")
    suspend fun getRecordCount(): Int

    /**
     * 获取平均收缩压
     * @return 平均收缩压
     */
    @Query("SELECT AVG(systolic_pressure) FROM blood_pressure_records")
    suspend fun getAverageSystolic(): Double?

    /**
     * 获取平均舒张压
     * @return 平均舒张压
     */
    @Query("SELECT AVG(diastolic_pressure) FROM blood_pressure_records")
    suspend fun getAverageDiastolic(): Double?

    /**
     * 获取平均脉搏
     * @return 平均脉搏
     */
    @Query("SELECT AVG(pulse_rate) FROM blood_pressure_records")
    suspend fun getAveragePulse(): Double?

    /**
     * 获取最高血压记录
     * @return 最高血压记录
     */
    @Query("SELECT * FROM blood_pressure_records ORDER BY systolic_pressure DESC LIMIT 1")
    suspend fun getHighestBloodPressureRecord(): BloodPressureRecord?

    /**
     * 获取最低血压记录
     * @return 最低血压记录
     */
    @Query("SELECT * FROM blood_pressure_records ORDER BY systolic_pressure ASC LIMIT 1")
    suspend fun getLowestBloodPressureRecord(): BloodPressureRecord?

    /**
     * 获取血压统计信息
     * @return 血压统计数据
     */
    @Query("""
        SELECT
            COUNT(*) as total_count,
            AVG(systolic_pressure) as avg_systolic,
            AVG(diastolic_pressure) as avg_diastolic,
            AVG(pulse_rate) as avg_pulse,
            MAX(systolic_pressure) as max_systolic,
            MIN(systolic_pressure) as min_systolic,
            MAX(diastolic_pressure) as max_diastolic,
            MIN(diastolic_pressure) as min_diastolic
        FROM blood_pressure_records
    """)
    suspend fun getBloodPressureStatistics(): BloodPressureStatistics?

    /**
     * 清空所有血压记录
     * @return 影响的行数
     */
    @Query("DELETE FROM blood_pressure_records")
    suspend fun deleteAllRecords(): Int

    /**
     * 更新记录的图表显示状态
     * @param id 记录ID
     * @param showInChart 是否显示在图表中
     * @return 影响的行数
     */
    @Query("UPDATE blood_pressure_records SET show_in_chart = :showInChart WHERE id = :id")
    suspend fun updateChartVisibility(id: Long, showInChart: Boolean): Int
}

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