package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BloodSugarDao
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 血糖记录数据仓库
 * 提供血糖数据的业务逻辑封装和数据访问
 */
@Singleton
class BloodSugarRepository @Inject constructor(
    private val bloodSugarDao: BloodSugarDao
) : BaseRepository<BloodSugarRecord, BloodSugarDao>() {
    
    override val dao: BloodSugarDao = bloodSugarDao

    /**
     * 添加血糖记录（支持标签）
     * @param glucoseValue 血糖值
     * @param status 测量标签
     * @param selectedTime 记录时间
     * @param selectedUnit 血糖单位
     * @param tagIds 关联的标签ID列表
     * @param showInChart 是否在图表中显示
     * @param ext1 扩展字段1
     * @param ext2 扩展字段2
     * @param ext3 扩展字段3
     * @return 插入记录的ID
     */
    suspend fun addBloodSugarRecord(
        glucoseValue: Double,
        status: Int,
        selectedTime: Date,
        selectedUnit: BsUnit,
        tagIds: List<Long>? = null,
        showInChart: Boolean = true,
        ext1: String? = null,
        ext2: String? = null,
        ext3: String? = null
    ): Long {

        val record = BloodSugarRecord.create(
            recordTime = selectedTime,
            glucoseValue = glucoseValue,
            status = status,
            selectedUnit = selectedUnit,
            tagIds = tagIds,
            showInChart = showInChart,
            ext1 = ext1,
            ext2 = ext2,
            ext3 = ext3
        )
        return bloodSugarDao.insert(record)
    }

    // 实现BaseRepository的抽象方法
    override suspend fun insertRecord(record: BloodSugarRecord): Long {
        return bloodSugarDao.insert(record)
    }
    
    override suspend fun updateRecord(record: BloodSugarRecord): Int {
        return bloodSugarDao.update(record)
    }
    
    override suspend fun deleteRecordById(recordId: Long): Int {
        return bloodSugarDao.deleteById(recordId)
    }
    
    override suspend fun getRecordById(recordId: Long): BloodSugarRecord? {
        return bloodSugarDao.getById(recordId)
    }
    
    override fun getAllRecords(): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getAllRecords()
    }
    
    override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecordsByTimeRange(startTime, endTime)
    }

    fun getRecordsByRangeAndStatus(
        startTime: Date,
        endTime: Date,
        status: Int?
    ): Flow<List<BloodSugarRecord>> {
        return if(status == null) bloodSugarDao.getRecordsByTimeRange(startTime, endTime) else bloodSugarDao.getRecordsByRangeAndStatus(startTime, endTime, status)
    }
    
    override fun getChartRecords(): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getChartRecords()
    }
    
    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        return bloodSugarDao.updateChartVisibility(recordId, showInChart) > 0
    }
    
    override suspend fun insertRecords(records: List<BloodSugarRecord>): List<Long> {
        return bloodSugarDao.insertAll(records)
    }
    
    override suspend fun deleteAllRecords(): Int {
        return bloodSugarDao.deleteAllRecords()
    }
    
    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = bloodSugarDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val mergedTagIds = TagUtils.mergeTagIds(existingTagIds, tagIds)
        val tagIdsString = TagUtils.tagIdsToString(mergedTagIds)

        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bloodSugarDao.update(updatedRecord) > 0
    }
    
    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = bloodSugarDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val updatedTagIds = TagUtils.removeTagId(existingTagIds, tagId)
        val tagIdsString = TagUtils.tagIdsToString(updatedTagIds)

        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bloodSugarDao.update(updatedRecord) > 0
    }
    
    override suspend fun getRecordsByTag(tagId: Long): List<BloodSugarRecord> {
        return bloodSugarDao.getRecordsByTagId(tagId.toString())
    }
    
    // 公共API方法，委托给基类或实现方法
    suspend fun updateBloodSugarRecord(record: BloodSugarRecord): Int = updateRecord(record)
    suspend fun deleteBloodSugarRecord(recordId: Long): Int = deleteRecordById(recordId)
    suspend fun getBloodSugarRecordById(recordId: Long): BloodSugarRecord? = getRecordById(recordId)
    fun getAllBloodSugarRecords(): Flow<List<BloodSugarRecord>> = getAllRecords()
    fun observeBloodSugarRecordById(recordId: Long): Flow<BloodSugarRecord?> = bloodSugarDao.observeById(recordId)

    // 公共API方法，委托给基类实现
    fun getRecentBloodSugarRecords(days: Int = 7): Flow<List<BloodSugarRecord>> = getRecentRecords(days)
    fun getTodayBloodSugarRecords(): Flow<List<BloodSugarRecord>> = getTodayRecords()
    fun getThisWeekBloodSugarRecords(): Flow<List<BloodSugarRecord>> = getThisWeekRecords()
    fun getChartBloodSugarRecords(): Flow<List<BloodSugarRecord>> = getChartRecords()
    suspend fun getBloodSugarRecordsByTimeRange(startTime: Date, endTime: Date) = getRecordsByTimeRange(startTime, endTime)

    /**
     * 根据血糖值范围获取记录
     * @param minValue 最小血糖值
     * @param maxValue 最大血糖值
     * @return Flow形式的血糖记录列表
     */
    fun getBloodSugarRecordsByRange(minValue: Double, maxValue: Double): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecordsByGlucoseRange(minValue, maxValue)
    }

    // 注意：标签功能现在由统一的HealthTag系统处理
    // 如需获取记录关联的标签，请使用HealthTagRepository

    // 标签相关的公共API方法，委托给基类实现
    suspend fun getBloodSugarRecordsByTag(tagId: Long): List<BloodSugarRecord> = getRecordsByTag(tagId)

    /**
     * 获取血糖记录统计信息
     * @return 统计信息
     */
    suspend fun getBloodSugarStatistics(): BloodSugarStatistics {
        val totalCount = bloodSugarDao.getRecordCount()
        val averageGlucose = bloodSugarDao.getAverageGlucose() ?: 0.0
        val highestRecord = bloodSugarDao.getHighestGlucoseRecord()
        val lowestRecord = bloodSugarDao.getLowestGlucoseRecord()
        val recordsWithTags = bloodSugarDao.getRecordsWithTagsCount()

        return BloodSugarStatistics(
            totalCount = totalCount,
            averageGlucose = averageGlucose,
            highestGlucose = highestRecord?.glucoseValue ?: 0.0,
            lowestGlucose = lowestRecord?.glucoseValue ?: 0.0,
            recordsWithTagsCount = recordsWithTags
        )
    }

    /**
     * 获取异常血糖记录（高血糖和低血糖）
     * @return Flow形式的异常血糖记录列表
     */
    fun getAbnormalBloodSugarRecords(): Flow<List<BloodSugarRecord>> {
        // 高血糖 > 140 mg/dL 或低血糖 < 70 mg/dL
        return bloodSugarDao.getRecordsByGlucoseRange(0.0, 70.0) // 可以结合多个查询
    }

    // 批量操作的公共API方法，委托给基类实现
    suspend fun insertBloodSugarRecords(records: List<BloodSugarRecord>): List<Long> = insertRecords(records)
    suspend fun deleteAllBloodSugarRecords(): Int = deleteAllRecords()
    
    /**
     * 获取最近N条血糖记录
     * @param limit 记录数量限制
     * @return Flow形式的血糖记录列表
     */
    fun getRecentBloodSugarRecordsWithLimit(limit: Int): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecentRecords(limit)
    }

}

/**
 * 血糖统计信息数据类
 */
data class BloodSugarStatistics(
    val totalCount: Int,
    val averageGlucose: Double,
    val highestGlucose: Double,
    val lowestGlucose: Double,
    val recordsWithTagsCount: Int
)
