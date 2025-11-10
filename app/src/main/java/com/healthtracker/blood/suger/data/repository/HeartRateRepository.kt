package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.HeartRateDao
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 心率记录数据仓库
 * 封装心率相关的业务操作
 */
@Singleton
class HeartRateRepository @Inject constructor(
    private val heartRateDao: HeartRateDao
) : BaseRepository<HeartRateRecord, HeartRateDao>() {

    override val dao: HeartRateDao = heartRateDao

    override suspend fun insertRecord(record: HeartRateRecord): Long {
        return heartRateDao.insert(record)
    }

    override suspend fun updateRecord(record: HeartRateRecord): Int {
        return heartRateDao.update(record.withUpdatedTimestamp())
    }

    override suspend fun deleteRecordById(recordId: Long): Int {
        return heartRateDao.softDeleteById(recordId)
    }

    override suspend fun getRecordById(recordId: Long): HeartRateRecord? {
        return heartRateDao.getById(recordId)
    }

    public override fun getAllRecords(): Flow<List<HeartRateRecord>> {
        return heartRateDao.getAllRecords()
    }

    override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<HeartRateRecord>> {
        return heartRateDao.getRecordsByTimeRange(startTime, endTime)
    }

    override fun getChartRecords(): Flow<List<HeartRateRecord>> {
        // 心率暂不区分图表展示，直接返回全部
        return getAllRecords()
    }

    fun getChartHeartRateRecords(): Flow<List<HeartRateRecord>> = getChartRecords()

    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        // 无图表显隐字段，直接返回成功
        return true
    }

    override suspend fun insertRecords(records: List<HeartRateRecord>): List<Long> {
        return heartRateDao.insertAll(records)
    }

    override suspend fun deleteAllRecords(): Int {
        return heartRateDao.clearAll()
    }

    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = heartRateDao.getById(recordId) ?: return false
        val merged = TagUtils.mergeTagIds(record.getTagIdList(), tagIds)
        val updated = record.copy(tagIds = TagUtils.tagIdsToString(merged))
        return heartRateDao.update(updated.withUpdatedTimestamp()) > 0
    }

    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = heartRateDao.getById(recordId) ?: return false
        val updatedTags = TagUtils.removeTagId(record.getTagIdList(), tagId)
        val updated = record.copy(tagIds = TagUtils.tagIdsToString(updatedTags))
        return heartRateDao.update(updated.withUpdatedTimestamp()) > 0
    }

    override suspend fun getRecordsByTag(tagId: Long): List<HeartRateRecord> {
        return heartRateDao.getRecordsByTagId(tagId.toString())
    }

    /**
     * 新增心率记录
     */
    suspend fun addHeartRateRecord(
        heartRateBpm: Int,
        recordTime: Date = Date(),
        tagIds: List<Long>? = null,
        ext1: String? = null,
        ext2: String? = null,
        ext3: String? = null
    ): Long {
        val record = HeartRateRecord(
            recordTime = recordTime,
            heartRateBpm = heartRateBpm,
            tagIds = TagUtils.tagIdsToString(tagIds),
            ext1 = ext1,
            ext2 = ext2,
            ext3 = ext3
        )
        return insertRecord(record)
    }

    suspend fun updateHeartRateRecord(record: HeartRateRecord): Int = updateRecord(record)
    suspend fun deleteHeartRateRecord(recordId: Long): Int = deleteRecordById(recordId)
    suspend fun getHeartRateRecordById(recordId: Long): HeartRateRecord? = getRecordById(recordId)
    suspend fun insertHeartRateRecords(records: List<HeartRateRecord>): List<Long> = insertRecords(records)
    fun getAllHeartRateRecords(): Flow<List<HeartRateRecord>> = getAllRecords()
    fun getRecentHeartRateRecords(days: Int = 7): Flow<List<HeartRateRecord>> = getRecentRecords(days)
    fun getTodayHeartRateRecords(): Flow<List<HeartRateRecord>> = getTodayRecords()
    fun getThisWeekHeartRateRecords(): Flow<List<HeartRateRecord>> = getThisWeekRecords()

    fun observeHeartRateRecordById(recordId: Long): Flow<HeartRateRecord?> {
        return heartRateDao.observeById(recordId)
    }
}
