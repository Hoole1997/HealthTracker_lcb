package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.CholesterolDao
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CholesterolRepository @Inject constructor(
    private val cholesterolDao: CholesterolDao
) : BaseRepository<CholesterolRecord, CholesterolDao>() {

    override val dao: CholesterolDao = cholesterolDao

    override suspend fun insertRecord(record: CholesterolRecord): Long {
        return cholesterolDao.insert(record)
    }

    override suspend fun updateRecord(record: CholesterolRecord): Int {
        return cholesterolDao.update(record.withUpdatedTimestamp())
    }

    override suspend fun deleteRecordById(recordId: Long): Int {
        return cholesterolDao.softDeleteById(recordId)
    }

    override suspend fun getRecordById(recordId: Long): CholesterolRecord? {
        return cholesterolDao.getById(recordId)
    }

    override fun getAllRecords(): Flow<List<CholesterolRecord>> {
        return cholesterolDao.getAllRecords()
    }

    override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<CholesterolRecord>> {
        return cholesterolDao.getRecordsByTimeRange(startTime, endTime)
    }

    override fun getChartRecords(): Flow<List<CholesterolRecord>> {
        return getAllRecords()
    }

    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        return true
    }

    override suspend fun insertRecords(records: List<CholesterolRecord>): List<Long> {
        return cholesterolDao.insertAll(records)
    }

    override suspend fun deleteAllRecords(): Int {
        return cholesterolDao.deleteAll()
    }

    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        val record = cholesterolDao.getById(recordId) ?: return false
        val merged = TagUtils.mergeTagIds(record.getTagIdList(), tagIds)
        val updated = record.copy(tagIds = TagUtils.tagIdsToString(merged))
        return cholesterolDao.update(updated.withUpdatedTimestamp()) > 0
    }

    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        val record = cholesterolDao.getById(recordId) ?: return false
        val newTags = TagUtils.removeTagId(record.getTagIdList(), tagId)
        val updated = record.copy(tagIds = TagUtils.tagIdsToString(newTags))
        return cholesterolDao.update(updated.withUpdatedTimestamp()) > 0
    }

    override suspend fun getRecordsByTag(tagId: Long): List<CholesterolRecord> {
        return cholesterolDao.getRecordsByTagId(tagId.toString())
    }

    suspend fun addCholesterolRecord(
        hdl: Int? = null,
        ldl: Int? = null,
        triglyceride: Int? = null,
        tc: Float? = null,
        nonHdl: Float? = null,
        tcHdlRatio: Float? = null,
        ldlHdlRatio: Float? = null,
        recordTime: Date = Date(),
        tagIds: List<Long>? = null,
        ext1: String? = null,
        ext2: String? = null,
        ext3: String? = null
    ): Long {
        val record = CholesterolRecord(
            recordTime = recordTime,
            hdl = hdl,
            ldl = ldl,
            triglyceride = triglyceride,
            tc = tc,
            nonHdl = nonHdl,
            tcHdlRatio = tcHdlRatio,
            ldlHdlRatio = ldlHdlRatio,
            tagIds = TagUtils.tagIdsToString(tagIds),
            ext1 = ext1,
            ext2 = ext2,
            ext3 = ext3
        )
        return insertRecord(record)
    }

    suspend fun updateCholesterolRecord(record: CholesterolRecord): Int = updateRecord(record)
    suspend fun deleteCholesterolRecord(recordId: Long): Int = deleteRecordById(recordId)
    suspend fun getLatestRecord(): CholesterolRecord? = dao.getLatestRecord()
}
