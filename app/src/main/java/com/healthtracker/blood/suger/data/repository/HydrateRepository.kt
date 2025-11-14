package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.HydrateDao
import com.healthtracker.blood.suger.data.entity.HydrateRecord
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 饮水记录数据仓库
 * 提供面向业务的饮水记录CRUD与时间范围查询
 */
@Singleton
class HydrateRepository @Inject constructor(
    private val hydrateDao: HydrateDao
) : BaseRepository<HydrateRecord, HydrateDao>() {

    override val dao: HydrateDao = hydrateDao

    override suspend fun insertRecord(record: HydrateRecord): Long {
        return hydrateDao.insert(record)
    }

    override suspend fun updateRecord(record: HydrateRecord): Int {
        return hydrateDao.update(record)
    }

    override suspend fun deleteRecordById(recordId: Long): Int {
        return hydrateDao.deleteById(recordId)
    }

    override suspend fun getRecordById(recordId: Long): HydrateRecord? {
        return hydrateDao.getById(recordId)
    }

    public override fun getAllRecords(): Flow<List<HydrateRecord>> {
        return hydrateDao.getAllRecords()
    }

    public override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<HydrateRecord>> {
        return hydrateDao.getRecordsByTimeRange(startTime, endTime)
    }

    override fun getChartRecords(): Flow<List<HydrateRecord>> {
        // 饮水暂不区分图表记录，直接返回全部
        return getAllRecords()
    }

    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        // 无图表显隐字段，直接返回成功
        return true
    }

    override suspend fun insertRecords(records: List<HydrateRecord>): List<Long> {
        return hydrateDao.insertAll(records)
    }

    override suspend fun deleteAllRecords(): Int {
        return hydrateDao.deleteAll()
    }

    override fun getLatestRecords(limit: Int): Flow<List<HydrateRecord>> {
       return hydrateDao.getRecentRecords(limit)
    }

    // 标签相关：HydrateRecord不支持标签，提供空实现
    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean = false

    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean = false

    override suspend fun getRecordsByTag(tagId: Long): List<HydrateRecord> = emptyList()

    /**
     * 便捷新增饮水记录
     */
    suspend fun addHydrateRecord(
        intakeMl: Int,
        recordTime: Date = Date()
    ): Long {
        // 读取当前饮水设置，并在保存时将杯子容积统一转换为ml
        val dailyCups = HydrateSettingManager.getDailyCups()
        val displayVolume = HydrateSettingManager.getCupDisplayVolume()
        val unit = HydrateSettingManager.getCupUnit()
        val cupVolumeMl = HydrateSettingManager.toMl(displayVolume, unit)
        val dailyTotalMl = dailyCups * cupVolumeMl

        return insertRecord(
            HydrateRecord(
                recordTime = recordTime,
                intakeMl = intakeMl,
                dailyGoalCups = dailyCups,
                cupVolumeMl = cupVolumeMl,
                dailyGoalTotalMl = dailyTotalMl
            )
        )
    }

    /**
     * 便捷删除饮水记录（按ID）
     */
    suspend fun deleteHydrateRecordById(id: Long): Boolean {
        return deleteRecordById(id) > 0
    }

    /**
     * 同步“当天内所有饮水记录”的设置快照字段（目标杯数、杯容积ml、目标总量ml）
     * 在设置页面调整 DailyCups 或 CupVolume 时调用
     */
    suspend fun syncTodayHydrateRecordSettings(): Int {
        val (start, end) = DateTimeUtils.getTodayRange()
        val dailyCups = HydrateSettingManager.getDailyCups()
        val displayVolume = HydrateSettingManager.getCupDisplayVolume()
        val unit = HydrateSettingManager.getCupUnit()
        val cupVolumeMl = HydrateSettingManager.toMl(displayVolume, unit)
        val dailyTotalMl = dailyCups * cupVolumeMl
        val nowTs = System.currentTimeMillis()

        return hydrateDao.updateRecordSettingsByTimeRange(
            start, end, dailyCups, cupVolumeMl, dailyTotalMl, nowTs
        )
    }
}