package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BloodPressureDao
import com.healthtracker.blood.suger.data.dao.BloodPressureStatistics
import com.healthtracker.blood.suger.data.dao.BloodPressureTagDao
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodPressureTag
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.data.enums.PulseCategory
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 血压记录数据仓库
 * 提供血压数据的业务逻辑封装和数据访问
 */
@Singleton
class BloodPressureRepository @Inject constructor(
    private val bloodPressureDao: BloodPressureDao,
    private val bloodPressureTagDao: BloodPressureTagDao
) {

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

    /**
     * 更新血压记录
     * @param record 血压记录
     * @return 影响的行数
     */
    suspend fun updateBloodPressureRecord(record: BloodPressureRecord): Int {
        return bloodPressureDao.update(record)
    }

    /**
     * 删除血压记录
     * @param recordId 记录ID
     * @return 影响的行数
     */
    suspend fun deleteBloodPressureRecord(recordId: Long): Int {
        return bloodPressureDao.deleteById(recordId)
    }

    /**
     * 根据ID获取血压记录
     * @param recordId 记录ID
     * @return 血压记录，可能为null
     */
    suspend fun getBloodPressureRecordById(recordId: Long): BloodPressureRecord? {
        return bloodPressureDao.getById(recordId)
    }

    /**
     * 获取所有血压记录
     * @return Flow形式的血压记录列表
     */
    fun getAllBloodPressureRecords(): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getAllRecords()
    }

    /**
     * 获取最近N天的血压记录
     * @param days 天数（默认7天）
     * @return Flow形式的血压记录列表
     */
    fun getRecentBloodPressureRecords(days: Int = 7): Flow<List<BloodPressureRecord>> {
        val (startDate, endDate) = DateTimeUtils.getDateRange(DateTimeUtils.now(), days)
        return bloodPressureDao.getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取今天的血压记录
     * @return Flow形式的血压记录列表
     */
    fun getTodayBloodPressureRecords(): Flow<List<BloodPressureRecord>> {
        val (startDate, endDate) = DateTimeUtils.getTodayRange()
        return bloodPressureDao.getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取本周的血压记录
     * @return Flow形式的血压记录列表
     */
    fun getThisWeekBloodPressureRecords(): Flow<List<BloodPressureRecord>> {
        val (startDate, endDate) = DateTimeUtils.getThisWeekRange()
        return bloodPressureDao.getRecordsByTimeRange(startDate, endDate)
    }

    /**
     * 获取图表显示的血压记录
     * @return Flow形式的血压记录列表
     */
    fun getChartBloodPressureRecords(): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getChartRecords()
    }

    /**
     * 根据时间范围获取血压记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Flow形式的血压记录列表
     */
    fun getBloodPressureRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BloodPressureRecord>> {
        return bloodPressureDao.getRecordsByTimeRange(startTime, endTime)
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

    /**
     * 获取记录关联的标签
     * @param record 血压记录
     * @return 标签列表
     */
    suspend fun getRecordTags(record: BloodPressureRecord): List<BloodPressureTag> {
        val tagIds = TagUtils.stringToTagIds(record.tagIds)
        return if (tagIds.isNotEmpty()) {
            bloodPressureTagDao.getByIds(tagIds)
        } else {
            emptyList()
        }
    }

    /**
     * 根据标签获取血压记录
     * @param tagId 标签ID
     * @return 包含该标签的血压记录列表
     */
    suspend fun getBloodPressureRecordsByTag(tagId: Long): List<BloodPressureRecord> {
        return bloodPressureDao.getRecordsByTagId(tagId.toString())
    }

    /**
     * 为记录添加标签
     * @param recordId 记录ID
     * @param tagIds 要添加的标签ID列表
     * @return 是否成功
     */
    suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = bloodPressureDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val mergedTagIds = TagUtils.mergeTagIds(existingTagIds, tagIds)
        val tagIdsString = TagUtils.tagIdsToString(mergedTagIds)

        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bloodPressureDao.update(updatedRecord) > 0
    }

    /**
     * 从记录中移除标签
     * @param recordId 记录ID
     * @param tagId 要移除的标签ID
     * @return 是否成功
     */
    suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = bloodPressureDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val updatedTagIds = TagUtils.removeTagId(existingTagIds, tagId)
        val tagIdsString = TagUtils.tagIdsToString(updatedTagIds)

        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bloodPressureDao.update(updatedRecord) > 0
    }

    /**
     * 更新记录的图表显示状态
     * @param recordId 记录ID
     * @param showInChart 是否显示在图表中
     * @return 是否成功
     */
    suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        return bloodPressureDao.updateChartVisibility(recordId, showInChart) > 0
    }

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

    /**
     * 批量插入血压记录
     * @param records 血压记录列表
     * @return 插入记录的ID列表
     */
    suspend fun insertBloodPressureRecords(records: List<BloodPressureRecord>): List<Long> {
        return bloodPressureDao.insertAll(records)
    }

    /**
     * 清空所有血压记录（谨慎使用）
     * @return 影响的行数
     */
    suspend fun deleteAllBloodPressureRecords(): Int {
        return bloodPressureDao.deleteAllRecords()
    }

}