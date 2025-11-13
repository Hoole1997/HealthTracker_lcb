package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BloodPressureDao
import com.healthtracker.blood.suger.data.dao.BloodPressureStatistics
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.data.enums.PulseCategory
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 血压记录数据仓库
 * 提供血压数据的业务逻辑封装和数据访问
 */
@Singleton
class BloodPressureRepository @Inject constructor(
    private val bloodPressureDao: BloodPressureDao
) : BaseRepository<BloodPressureRecord, BloodPressureDao>() {
    
    override val dao: BloodPressureDao = bloodPressureDao

    // 实现BaseRepository的抽象方法
    override suspend fun insertRecord(record: BloodPressureRecord): Long {
        return bloodPressureDao.insert(record)
    }
    
    override suspend fun updateRecord(record: BloodPressureRecord): Int {
        return bloodPressureDao.update(record)
    }
    
    override suspend fun deleteRecordById(recordId: Long): Int {
        return bloodPressureDao.deleteById(recordId)
    }
    
    override suspend fun getRecordById(recordId: Long): BloodPressureRecord? {
        return bloodPressureDao.getById(recordId)
    }
    
    override fun getAllRecords(): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getAllRecords()
    }
    
    override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsByTimeRange(startTime, endTime)
    }
    
    override fun getChartRecords(): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getChartRecords()
    }

    override fun getLatestRecords(limit: Int): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecentRecords(limit)
    }

    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        return bloodPressureDao.updateChartVisibility(recordId, showInChart) > 0
    }
    
    override suspend fun insertRecords(records: List<BloodPressureRecord>): List<Long> {
        return bloodPressureDao.insertAll(records)
    }
    
    override suspend fun deleteAllRecords(): Int {
        return bloodPressureDao.deleteAllRecords()
    }
    
    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = bloodPressureDao.getById(recordId) ?: return false
        val existingTagIds = TagUtils.stringToTagIds(record.tagIds).toMutableList()
        existingTagIds.addAll(tagIds)
        val updatedRecord = record.copy(tagIds = TagUtils.tagIdsToString(existingTagIds.distinct()))
        return bloodPressureDao.update(updatedRecord) > 0
    }
    
    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = bloodPressureDao.getById(recordId) ?: return false
        val existingTagIds = TagUtils.stringToTagIds(record.tagIds).toMutableList()
        existingTagIds.remove(tagId)
        val updatedRecord = record.copy(tagIds = TagUtils.tagIdsToString(existingTagIds))
        return bloodPressureDao.update(updatedRecord) > 0
    }
    
    override suspend fun getRecordsByTag(tagId: Long): List<BloodPressureRecord> {
        return bloodPressureDao.getRecordsByTagId(tagId.toString())
    }

    /**
     * 添加血压记录（支持标签）
     * @param systolic 收缩压
     * @param diastolic 舒张压
     * @param pulse 脉搏
     * @param selectedTime 记录时间
     * @param tagIds 关联的标签ID列表
     * @param showInChart 是否在图表中显示
     * @param ext1 扩展字段1
     * @param ext2 扩展字段2
     * @param ext3 扩展字段3
     * @return 插入记录的ID
     */
    suspend fun addBloodPressureRecord(
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        selectedTime: Date,
        tagIds: List<Long>? = null,
        showInChart: Boolean = true,
        ext1: String? = null,
        ext2: String? = null,
        ext3: String? = null
    ): Long {

        val record = BloodPressureRecord.create(
            recordTime = selectedTime,
            systolicPressure = systolic,
            diastolicPressure = diastolic,
            pulseRate = pulse,
            tagIds = tagIds,
            showInChart = showInChart,
            ext1 = ext1,
            ext2 = ext2,
            ext3 = ext3
        )
        return bloodPressureDao.insert(record)
    }

    // 基础CRUD操作的公共API方法，委托给基类实现
    suspend fun updateBloodPressureRecord(record: BloodPressureRecord): Int = updateRecord(record)
    suspend fun deleteBloodPressureRecord(recordId: Long): Int = deleteRecordById(recordId)
    suspend fun getBloodPressureRecordById(recordId: Long): BloodPressureRecord? = getRecordById(recordId)
    
    /**
     * 根据ID获取血压记录（Flow响应式）
     * @param recordId 记录ID
     * @return Flow形式的血压记录对象，支持数据变化监听
     */
    fun getBloodPressureRecordByIdFlow(recordId: Long): Flow<BloodPressureRecord?> {
        return bloodPressureDao.getByIdFlow(recordId)
    }
    
    fun getAllBloodPressureRecords(): Flow<List<BloodPressureRecord>> = getAllRecords()

    // 时间范围查询的公共API方法，委托给基类实现
    fun getRecentBloodPressureRecords(days: Int = 7): Flow<List<BloodPressureRecord>> = getRecentRecords(days)
    fun getLatestBloodPressureRecords(limit: Int = 7): Flow<List<BloodPressureRecord>> = getLatestRecords(limit)
    fun getTodayBloodPressureRecords(): Flow<List<BloodPressureRecord>> = getTodayRecords()
    fun getThisWeekBloodPressureRecords(): Flow<List<BloodPressureRecord>> = getThisWeekRecords()
    fun getChartBloodPressureRecords(): Flow<List<BloodPressureRecord>> = getChartRecords()
    suspend fun getBloodPressureRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodPressureRecord>> =
        withContext(IO){
            getRecordsByTimeRange(startTime, endTime)
        }

    /**
     * 根据血压分类获取记录
     * @param category 血压分类
     * @return Flow形式的血压记录列表
     */
    fun getBloodPressureRecordsByCategory(category: BloodPressureCategory): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsByBloodPressureCategory(category.code)
    }

    /**
     * 根据脉搏分类获取记录
     * @param category 脉搏分类
     * @return Flow形式的血压记录列表
     */
    fun getBloodPressureRecordsByPulseCategory(category: PulseCategory): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsByPulseCategory(category.code)
    }

    /**
     * 根据收缩压范围获取记录
     * @param minSystolic 最小收缩压
     * @param maxSystolic 最大收缩压
     * @return Flow形式的血压记录列表
     */
    fun getBloodPressureRecordsBySystolicRange(minSystolic: Int, maxSystolic: Int): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsBySystolicRange(minSystolic, maxSystolic)
    }

    /**
     * 根据舒张压范围获取记录
     * @param minDiastolic 最小舒张压
     * @param maxDiastolic 最大舒张压
     * @return Flow形式的血压记录列表
     */
    fun getBloodPressureRecordsByDiastolicRange(minDiastolic: Int, maxDiastolic: Int): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsByDiastolicRange(minDiastolic, maxDiastolic)
    }

    /**
     * 根据脉搏范围获取记录
     * @param minPulse 最小脉搏
     * @param maxPulse 最大脉搏
     * @return Flow形式的血压记录列表
     */
    fun getBloodPressureRecordsByPulseRange(minPulse: Int, maxPulse: Int): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsByPulseRange(minPulse, maxPulse)
    }

    // 注意：标签功能现在由统一的HealthTag系统处理
    // 如需获取记录关联的标签，请使用HealthTagRepository

    // 标签相关的公共API方法，委托给基类实现
    suspend fun getBloodPressureRecordsByTag(tagId: Long): List<BloodPressureRecord> = getRecordsByTag(tagId)
    suspend fun addTagsToBloodPressureRecord(recordId: Long, tagIds: List<Long>): Boolean = addTagsToRecord(recordId, tagIds)
    suspend fun removeTagFromBloodPressureRecord(recordId: Long, tagId: Long): Boolean = removeTagFromRecord(recordId, tagId)
    suspend fun updateBloodPressureChartVisibility(recordId: Long, showInChart: Boolean): Boolean = updateChartVisibility(recordId, showInChart)

    /**
     * 获取血压统计信息
     * @return 血压统计数据
     */
    suspend fun getBloodPressureStatistics(): BloodPressureStatistics? {
        return bloodPressureDao.getBloodPressureStatistics()
    }

    /**
     * 获取异常血压记录（高血压）
     * @return Flow形式的异常血压记录列表
     */
    fun getAbnormalBloodPressureRecords(): Flow<List<BloodPressureRecord>> {
        // 高血压：收缩压 >= 140 或舒张压 >= 90
        return bloodPressureDao.getRecordsBySystolicRange(140, 300)
    }

    /**
     * 获取高血压记录
     * @return Flow形式的高血压记录列表
     */
    fun getHypertensionRecords(): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsBySystolicRange(130, 300) // 包括1期和2期高血压
    }

    /**
     * 获取最近N条血压记录
     * @param limit 记录数量限制
     * @return Flow形式的血压记录列表
     */
    fun getRecentBloodPressureRecordsWithLimit(limit: Int): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecentRecords(limit)
    }

    // 批量操作的公共API方法，委托给基类实现
    suspend fun insertBloodPressureRecords(records: List<BloodPressureRecord>): List<Long> = insertRecords(records)
    suspend fun deleteAllBloodPressureRecords(): Int = deleteAllRecords()

}