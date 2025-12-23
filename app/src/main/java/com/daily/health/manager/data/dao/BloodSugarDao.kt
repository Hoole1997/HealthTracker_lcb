package com.daily.health.manager.data.dao

import androidx.room.*
import com.daily.health.manager.data.entity.BloodSugarRecord
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * 血糖记录数据访问对象(DAO)
 * 提供血糖数据的CRUD操作
 */
@Dao
interface BloodSugarDao {

    /**
     * 插入血糖记录
     * @param record 血糖记录对象
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BloodSugarRecord): Long

    /**
     * 批量插入血糖记录
     * @param records 血糖记录列表
     * @return 插入记录的ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BloodSugarRecord>): List<Long>

    /**
     * 更新血糖记录
     * @param record 血糖记录对象
     * @return 影响的行数
     */
    @Update
    suspend fun update(record: BloodSugarRecord): Int

    /**
     * 删除血糖记录
     * @param record 血糖记录对象
     * @return 影响的行数
     */
    @Delete
    suspend fun delete(record: BloodSugarRecord): Int

    /**
     * 根据ID删除血糖记录
     * @param id 记录ID
     * @return 影响的行数
     */
    @Query("DELETE FROM blood_sugar_records WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /**
     * 根据ID获取血糖记录
     * @param id 记录ID
     * @return 血糖记录对象，可能为null
     */
    @Query("SELECT * FROM blood_sugar_records WHERE id = :id")
    suspend fun getById(id: Long): BloodSugarRecord?

    /**
     * 根据ID监听血糖记录变化
     */
    @Query("SELECT * FROM blood_sugar_records WHERE id = :id")
    fun observeById(id: Long): Flow<BloodSugarRecord?>

    /**
     * 获取所有血糖记录，按时间倒序排列
     * @return Flow形式的血糖记录列表，支持数据变化监听
     */
    @Query("SELECT * FROM blood_sugar_records ORDER BY record_time DESC, updated_at DESC")
    fun getAllRecords(): Flow<List<BloodSugarRecord>>

    /**
     * 获取可在图表中显示的血糖记录，按时间排序
     * @return Flow形式的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records WHERE show_in_chart = 1 ORDER BY record_time ASC, updated_at DESC")
    fun getChartRecords(): Flow<List<BloodSugarRecord>>

    /**
     * 根据时间范围获取血糖记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Flow形式的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records WHERE record_time BETWEEN :startTime AND :endTime ORDER BY record_time DESC, updated_at DESC")
    fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodSugarRecord>>

    /**
     * 根据时间范围与状态筛选血糖记录
     */
    @Query(
        """
        SELECT * FROM blood_sugar_records 
        WHERE record_time BETWEEN :startTime AND :endTime
        AND (:status IS NULL OR status = :status)
        ORDER BY record_time DESC, updated_at DESC
        """
    )
    fun getRecordsByRangeAndStatus(
        startTime: Date,
        endTime: Date,
        status: Int?
    ): Flow<List<BloodSugarRecord>>

    /**
     * 根据测量标签获取血糖记录
     * @param tag 测量标签
     * @return Flow形式的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records WHERE status = :status ORDER BY record_time DESC")
    fun getRecordsByTag(status: Int): Flow<List<BloodSugarRecord>>

    /**
     * 获取血糖值范围内的记录
     * @param minValue 最小血糖值
     * @param maxValue 最大血糖值
     * @return Flow形式的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records WHERE glucose_value BETWEEN :minValue AND :maxValue ORDER BY record_time DESC")
    fun getRecordsByGlucoseRange(minValue: Double, maxValue: Double): Flow<List<BloodSugarRecord>>

    /**
     * 获取最近N条血糖记录
     * @param limit 记录数量限制
     * @return Flow形式的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records ORDER BY record_time DESC, updated_at DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<BloodSugarRecord>>

    /**
     * 获取血糖记录总数
     * @return 记录总数
     */
    @Query("SELECT COUNT(*) FROM blood_sugar_records")
    suspend fun getRecordCount(): Int

    /**
     * 获取平均血糖值
     * @return 平均血糖值
     */
    @Query("SELECT AVG(glucose_value) FROM blood_sugar_records")
    suspend fun getAverageGlucose(): Double?

    /**
     * 获取最高血糖记录
     * @return 最高血糖记录
     */
    @Query("SELECT * FROM blood_sugar_records ORDER BY glucose_value DESC LIMIT 1")
    suspend fun getHighestGlucoseRecord(): BloodSugarRecord?

    /**
     * 获取最低血糖记录
     * @return 最低血糖记录
     */
    @Query("SELECT * FROM blood_sugar_records ORDER BY glucose_value ASC LIMIT 1")
    suspend fun getLowestGlucoseRecord(): BloodSugarRecord?

    /**
     * 清空所有血糖记录
     * @return 影响的行数
     */
    @Query("DELETE FROM blood_sugar_records")
    suspend fun deleteAllRecords(): Int

    /**
     * 更新记录的图表显示状态
     * @param id 记录ID
     * @param showInChart 是否显示在图表中
     * @return 影响的行数
     */
    @Query("UPDATE blood_sugar_records SET show_in_chart = :showInChart WHERE id = :id")
    suspend fun updateChartVisibility(id: Long, showInChart: Boolean): Int

    /**
     * 根据标签ID获取血糖记录
     * @param tagId 标签ID
     * @return 包含该标签的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records WHERE tag_ids LIKE '%' || :tagId || '%' ORDER BY record_time DESC")
    suspend fun getRecordsByTagId(tagId: String): List<BloodSugarRecord>

    /**
     * 获取所有包含标签的血糖记录
     * @return 包含标签的血糖记录列表
     */
    @Query("SELECT * FROM blood_sugar_records WHERE tag_ids IS NOT NULL AND tag_ids != '' ORDER BY record_time DESC")
    suspend fun getAllRecordsWithTags(): List<BloodSugarRecord>

    /**
     * 获取包含标签的血糖记录数量
     * @return 包含标签的记录数量
     */
    @Query("SELECT COUNT(*) FROM blood_sugar_records WHERE tag_ids IS NOT NULL AND tag_ids != ''")
    suspend fun getRecordsWithTagsCount(): Int
}
