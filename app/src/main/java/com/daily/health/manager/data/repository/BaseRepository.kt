package com.daily.health.manager.data.repository

import com.daily.health.manager.data.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 基础Repository抽象类
 * 提供通用的CRUD操作和数据访问模式
 * @param T 记录实体类型
 * @param D DAO接口类型
 */
abstract class BaseRepository<T, D> {
    
    /**
     * 获取DAO实例
     */
    protected abstract val dao: D
    
    /**
     * 插入记录的抽象方法
     * @param record 记录实体
     * @return 插入记录的ID
     */
    protected abstract suspend fun insertRecord(record: T): Long
    
    /**
     * 更新记录的抽象方法
     * @param record 记录实体
     * @return 影响的行数
     */
    protected abstract suspend fun updateRecord(record: T): Int
    
    /**
     * 根据ID删除记录的抽象方法
     * @param recordId 记录ID
     * @return 影响的行数
     */
    protected abstract suspend fun deleteRecordById(recordId: Long): Int
    
    /**
     * 根据ID获取记录的抽象方法
     * @param recordId 记录ID
     * @return 记录实体，可能为null
     */
    abstract suspend fun getRecordById(recordId: Long): T?
    
    /**
     * 获取所有记录的抽象方法
     * @return Flow形式的记录列表
     */
    protected abstract fun getAllRecords(): Flow<List<T>>
    
    /**
     * 根据时间范围获取记录的抽象方法
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Flow形式的记录列表
     */
    protected abstract fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<T>>
    
    /**
     * 获取图表显示记录的抽象方法
     * @return Flow形式的记录列表
     */
    protected abstract fun getChartRecords(): Flow<List<T>>
    
    /**
     * 更新图表可见性的抽象方法
     * @param recordId 记录ID
     * @param showInChart 是否在图表中显示
     * @return 是否更新成功
     */
    protected abstract suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean
    
    /**
     * 批量插入记录的抽象方法
     * @param records 记录列表
     * @return 插入记录的ID列表
     */
    protected abstract suspend fun insertRecords(records: List<T>): List<Long>
    
    /**
     * 删除所有记录的抽象方法
     * @return 影响的行数
     */
    protected abstract suspend fun deleteAllRecords(): Int
    
    /**
     * 获取最近N天的记录
     * @param days 天数，默认7天
     * @return Flow形式的记录列表
     */
    open fun getRecentRecords(days: Int = 7): Flow<List<T>> {
        val (startDate, endDate) = DateTimeUtils.getDateRange(DateTimeUtils.now(), days)
        return getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取最近N条记录（按记录数量而非天数）
     * @param limit 记录数量限制
     * @return Flow形式的记录列表
     */
    abstract fun getLatestRecords(limit: Int): Flow<List<T>>

    /**
     * 获取今天的记录
     * @return Flow形式的记录列表
     */
    open fun getTodayRecords(): Flow<List<T>> {
        val (startDate, endDate) = DateTimeUtils.getTodayRange()
        return getRecordsByTimeRange(startDate, endDate)
    }
    
    /**
     * 获取本周的记录
     * @return Flow形式的记录列表
     */
    open fun getThisWeekRecords(): Flow<List<T>> {
        val (startDate, endDate) = DateTimeUtils.getThisWeekRange()
        return getRecordsByTimeRange(startDate, endDate)
    }
    
    /**
     * 标签相关操作的抽象方法
     */
    
    /**
     * 为记录添加标签
     * @param recordId 记录ID
     * @param tagIds 标签ID列表
     * @return 是否添加成功
     */
    abstract suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean
    
    /**
     * 从记录中移除标签
     * @param recordId 记录ID
     * @param tagId 标签ID
     * @return 是否移除成功
     */
    abstract suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean
    
    /**
     * 根据标签获取记录
     * @param tagId 标签ID
     * @return 记录列表
     */
    abstract suspend fun getRecordsByTag(tagId: Long): List<T>
}