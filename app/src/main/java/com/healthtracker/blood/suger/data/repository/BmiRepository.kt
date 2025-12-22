package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BmiDao
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * BMI记录数据仓库（最简实现）
 * 提供BMI数据的业务逻辑封装和数据访问
 */
class BmiRepository(
    private val bmiDao: BmiDao
) : BaseRepository<BmiRecord, BmiDao>() {

    override val dao: BmiDao = bmiDao

    // 基础CRUD实现
    override suspend fun insertRecord(record: BmiRecord): Long {
        return bmiDao.insert(record)
    }

    override suspend fun updateRecord(record: BmiRecord): Int {
        // 确保更新时间戳更新
        return bmiDao.update(record.withUpdatedTimestamp())
    }

    override suspend fun deleteRecordById(recordId: Long): Int {
        // BMI 采用软删除以保留数据
        return bmiDao.softDeleteById(recordId)
    }

    override suspend fun getRecordById(recordId: Long): BmiRecord? {
        return bmiDao.getById(recordId)
    }

    public override fun getAllRecords(): Flow<List<BmiRecord>> {
        return bmiDao.getAllRecords()
    }

    override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BmiRecord>> {
        return bmiDao.getRecordsByTimeRange(startTime, endTime)
    }

    override fun getChartRecords(): Flow<List<BmiRecord>> {
        // BMI 暂不支持图表显示字段，直接返回全部记录
        return getAllRecords()
    }

    override fun getLatestRecords(limit: Int): Flow<List<BmiRecord>> {
        return bmiDao.getRecentRecords(limit)
    }

    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        // BMI 暂不支持图表显示字段，返回true表示调用成功但不做任何更改
        return true
    }

    override suspend fun insertRecords(records: List<BmiRecord>): List<Long> {
        return bmiDao.insertAll(records)
    }

    override suspend fun deleteAllRecords(): Int {
        return bmiDao.deleteAllRecords()
    }

    fun observerRecord(recordId:Long) = bmiDao.observeById(recordId)

    // 标签相关实现
    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = bmiDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val mergedTagIds = TagUtils.mergeTagIds(existingTagIds, tagIds)
        val tagIdsString = TagUtils.tagIdsToString(mergedTagIds)
        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bmiDao.update(updatedRecord.withUpdatedTimestamp()) > 0
    }

    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = bmiDao.getById(recordId) ?: return false
        val existingTagIds = record.getTagIdList()
        val updatedTagIds = TagUtils.removeTagId(existingTagIds, tagId)
        val tagIdsString = TagUtils.tagIdsToString(updatedTagIds)
        val updatedRecord = record.copy(tagIds = tagIdsString)
        return bmiDao.update(updatedRecord.withUpdatedTimestamp()) > 0
    }

    override suspend fun getRecordsByTag(tagId: Long): List<BmiRecord> {
        return bmiDao.getRecordsByTagId(tagId.toString())
    }

    // 公共API（委托给基类或本类实现）
    suspend fun addBmiRecord(
        heightCm: Double,
        weightKg: Double,
        recordTime: Date = Date(),
        tagIds: List<Long>? = null,
        ext1: String? = null,
        ext2: String? = null,
        ext3: String? = null
    ): Long {
        val tagIdsString = TagUtils.tagIdsToString(tagIds)
        val record = BmiRecord(
            recordTime = recordTime,
            heightCm = heightCm,
            weightKg = weightKg,
            tagIds = tagIdsString,
            ext1 = ext1,
            ext2 = ext2,
            ext3 = ext3
        )
        return insertRecord(record)
    }

    suspend fun updateBmiRecord(record: BmiRecord): Int = updateRecord(record)
    suspend fun deleteBmiRecord(recordId: Long): Int = deleteRecordById(recordId)
    suspend fun getBmiRecordById(recordId: Long): BmiRecord? = getRecordById(recordId)
    suspend fun insertBmiRecord(record: BmiRecord): Long = insertRecord(record)
    fun getAllBmiRecords(): Flow<List<BmiRecord>> = getAllRecords()
    fun getRecentBmiRecords(days: Int = 7): Flow<List<BmiRecord>> = getRecentRecords(days)
    fun getLatestBmiRecords(limit: Int = 7): Flow<List<BmiRecord>> = getLatestRecords(limit)
    fun getTodayBmiRecords(): Flow<List<BmiRecord>> = getTodayRecords()
    fun getThisWeekBmiRecords(): Flow<List<BmiRecord>> = getThisWeekRecords()
    fun getChartBmiRecords(): Flow<List<BmiRecord>> = getChartRecords()
    suspend fun getBmiRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<BmiRecord>> = getRecordsByTimeRange(startTime, endTime)
    suspend fun getBmiRecordsByTag(tagId: Long): List<BmiRecord> = getRecordsByTag(tagId)
    suspend fun insertBmiRecords(records: List<BmiRecord>): List<Long> = insertRecords(records)
    suspend fun deleteAllBmiRecords(): Int = deleteAllRecords()
}