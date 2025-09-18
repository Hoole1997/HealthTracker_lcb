package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BloodSugarDao
import com.healthtracker.blood.suger.data.dao.HealthTagDao
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
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
    private val bloodSugarDao: BloodSugarDao,
    private val healthTagDao: HealthTagDao
) {

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

    /**
     * 更新血糖记录
     * @param record 血糖记录
     * @return 影响的行数
     */
    suspend fun updateBloodSugarRecord(record: BloodSugarRecord): Int {
        return bloodSugarDao.update(record)
    }

    /**
     * 删除血糖记录
     * @param recordId 记录ID
     * @return 影响的行数
     */
    suspend fun deleteBloodSugarRecord(recordId: Long): Int {
        return bloodSugarDao.deleteById(recordId)
    }

    /**
     * 根据ID获取血糖记录
     * @param recordId 记录ID
     * @return 血糖记录，可能为null
     */
    suspend fun getBloodSugarRecordById(recordId: Long): BloodSugarRecord? {
        return bloodSugarDao.getById(recordId)
    }

    /**
     * 获取所有血糖记录
     * @return Flow形式的血糖记录列表
     */
    fun getAllBloodSugarRecords(): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getAllRecords()
    }

    /**
     * 获取最近N天的血糖记录
     * @param days 天数（默认7天）
     * @return Flow形式的血糖记录列表
     */
    fun getRecentBloodSugarRecords(days: Int = 7): Flow<List<BloodSugarRecord>> {
        val (startDate, endDate) = DateTimeUtils.getDateRange(DateTimeUtils.now(), days)
        return bloodSugarDao.getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取今天的血糖记录
     * @return Flow形式的血糖记录列表
     */
    fun getTodayBloodSugarRecords(): Flow<List<BloodSugarRecord>> {
        val (startDate, endDate) = DateTimeUtils.getTodayRange()
        return bloodSugarDao.getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取本周的血糖记录
     * @return Flow形式的血糖记录列表
     */
    fun getThisWeekBloodSugarRecords(): Flow<List<BloodSugarRecord>> {
        val (startDate, endDate) = DateTimeUtils.getThisWeekRange()
        return bloodSugarDao.getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取图表显示的血糖记录
     * @return Flow形式的血糖记录列表
     */
    fun getChartBloodSugarRecords(): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getChartRecords()
    }

    /**
     * 根据时间范围获取血糖记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Flow形式的血糖记录列表
     */
    fun getBloodSugarRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecordsByTimeRange(startTime, endTime)
    }

    /**
     * 根据血糖值范围获取记录
     * @param minValue 最小血糖值
     * @param maxValue 最大血糖值
     * @return Flow形式的血糖记录列表
     */
    fun getBloodSugarRecordsByRange(minValue: Double, maxValue: Double): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecordsByGlucoseRange(minValue, maxValue)
    }

    /**
     * 获取记录关联的标签
     * @param record 血糖记录
     * @return 标签列表
     */
    suspend fun getRecordTags(record: BloodSugarRecord): List<HealthTag> {
        val tagIds = record.getTagIdList()
        return if (tagIds.isNotEmpty()) {
            healthTagDao.getByIds(tagIds)
        } else {
            emptyList()
        }
    }

    /**
     * 根据标签获取血糖记录
     * @param tagId 标签ID
     * @return 包含该标签的血糖记录列表
     */
    suspend fun getBloodSugarRecordsByTag(tagId: Long): List<BloodSugarRecord> {
        return bloodSugarDao.getRecordsByTagId(tagId.toString())
    }

    /**
     * 为记录添加标签
     * @param recordId 记录ID
     * @param tagIds 要添加的标签ID列表
     * @return 是否成功
     */
    suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = bloodSugarDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val mergedTagIds = TagUtils.mergeTagIds(existingTagIds, tagIds)
        val tagIdsString = TagUtils.tagIdsToString(mergedTagIds)

        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bloodSugarDao.update(updatedRecord) > 0
    }

    /**
     * 从记录中移除标签
     * @param recordId 记录ID
     * @param tagId 要移除的标签ID
     * @return 是否成功
     */
    suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = bloodSugarDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val updatedTagIds = TagUtils.removeTagId(existingTagIds, tagId)
        val tagIdsString = TagUtils.tagIdsToString(updatedTagIds)

        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bloodSugarDao.update(updatedRecord) > 0
    }

    /**
     * 更新记录的图表显示状态
     * @param recordId 记录ID
     * @param showInChart 是否显示在图表中
     * @return 是否成功
     */
    suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        return bloodSugarDao.updateChartVisibility(recordId, showInChart) > 0
    }

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

    /**
     * 批量插入血糖记录
     * @param records 血糖记录列表
     * @return 插入记录的ID列表
     */
    suspend fun insertBloodSugarRecords(records: List<BloodSugarRecord>): List<Long> {
        return bloodSugarDao.insertAll(records)
    }

    /**
     * 获取最近N条血糖记录
     * @param limit 记录数量限制
     * @return Flow形式的血糖记录列表
     */
    fun getRecentBloodSugarRecordsWithLimit(limit: Int): Flow<List<BloodSugarRecord>> {
        return bloodSugarDao.getRecentRecords(limit)
    }

    /**
     * 清空所有血糖记录（谨慎使用）
     * @return 影响的行数
     */
    suspend fun deleteAllBloodSugarRecords(): Int {
        return bloodSugarDao.deleteAllRecords()
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