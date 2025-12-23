package com.daily.health.manager.data.repository

import com.daily.health.manager.data.dao.AlarmDao
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.data.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 闹钟记录数据仓库
 * 提供闹钟数据的业务逻辑封装和数据访问
 */
class AlarmRepository(
    private val alarmDao: AlarmDao
) : BaseRepository<AlarmRecord, AlarmDao>() {
    
    override val dao: AlarmDao = alarmDao

    // 实现BaseRepository的抽象方法
    override suspend fun insertRecord(record: AlarmRecord): Long {
        return alarmDao.insert(record)
    }
    
    override suspend fun updateRecord(record: AlarmRecord): Int {
        return alarmDao.update(record)
    }
    
    override suspend fun deleteRecordById(recordId: Long): Int {
        return alarmDao.deleteById(recordId)
    }
    
    // 重写并公开getRecordById方法供外部使用
    override suspend fun getRecordById(recordId: Long): AlarmRecord? {
        return alarmDao.getById(recordId)
    }
    
    override fun getAllRecords(): Flow<List<AlarmRecord>> {
        return alarmDao.getAllRecords()
    }

    /**
     * 同步获取所有闹钟记录
     * @return 闹钟记录列表
     */
    suspend fun getAllRecordsSync(): List<AlarmRecord> {
        return alarmDao.getAllRecordsSync()
    }
    
    /**
     * 获取所有闹钟记录的Flow
     * 公共方法，供ViewModel使用
     * @return Flow形式的闹钟记录列表
     */
    fun getAllRecordsFlow(): Flow<List<AlarmRecord>> {
        return getAllRecords()
    }

    /**
     * 批量插入闹钟记录
     * @param records 闹钟记录列表
     * @return 插入记录的ID列表
     */
    suspend fun batchInsertRecords(records: List<AlarmRecord>): List<Long> {
        return alarmDao.insertAll(records)
    }
    
    override fun getRecordsByTimeRange(startTime: Date, endTime: Date): Flow<List<AlarmRecord>> {
        // 闹钟不需要按日期范围查询，返回所有记录
        return getAllRecords()
    }
    
    override fun getChartRecords(): Flow<List<AlarmRecord>> {
        // 闹钟不需要图表显示，返回启用的记录
        return getEnabledRecords()
    }

    override fun getLatestRecords(limit: Int): Flow<List<AlarmRecord>> {
        // 闹钟不需要按记录数量查询，返回所有记录
        return getAllRecords()
    }

    override suspend fun updateChartVisibility(recordId: Long, showInChart: Boolean): Boolean {
        // 闹钟不需要图表显示功能，直接返回true
        return true
    }
    
    override suspend fun insertRecords(records: List<AlarmRecord>): List<Long> {
        return alarmDao.insertAll(records)
    }
    
    override suspend fun deleteAllRecords(): Int {
        return alarmDao.deleteAllRecords()
    }
    
    override suspend fun addTagsToRecord(recordId: Long, tagIds: List<Long>): Boolean {
        // 闹钟暂不支持标签功能，可在扩展字段中实现
        return true
    }
    
    override suspend fun removeTagFromRecord(recordId: Long, tagId: Long): Boolean {
        // 闹钟暂不支持标签功能，可在扩展字段中实现
        return true
    }
    
    override suspend fun getRecordsByTag(tagId: Long): List<AlarmRecord> {
        // 闹钟暂不支持标签功能，返回空列表
        return emptyList()
    }

    // === 闹钟特有的业务方法 ===

    /**
     * 添加闹钟记录
     * @param type 闹钟类型
     * @param hour 小时
     * @param minute 分钟
     * @param repeatFlag 重复标志
     * @param soundId 铃声ID
     * @param isEnabled 是否启用
     * @param vibrateTime 振动时长
     * @param other 其他信息
     * @return 插入记录的ID
     */
    suspend fun addAlarmRecord(
        type: Int,
        hour: Int,
        minute: Int,
        repeatFlag: Int = AlarmRecord.REPEAT_ONCE,
        soundId: Long = 0,
        isEnabled: Boolean = true,
        vibrateTime: Long = 0,
        other: String? = null
    ): Long {
        // 验证时间格式
        require(hour in 0..23) { "Hour must be between 0-23" }
        require(minute in 0..59) { "Minute must be between 0-59" }
        
        val record = AlarmRecord.create(
            type = type,
            hour = hour,
            minute = minute,
            repeatFlag = repeatFlag,
            soundId = soundId,
            isEnabled = isEnabled,
            vibrateTime = vibrateTime,
            other = other
        )
        return alarmDao.insert(record)
    }

    /**
     * 添加血糖测量提醒
     * @param hour 小时
     * @param minute 分钟
     * @param repeatFlag 重复标志
     * @return 插入记录的ID
     */
    suspend fun addBloodSugarReminder(
        hour: Int,
        minute: Int,
        repeatFlag: Int = AlarmRecord.REPEAT_DAILY
    ): Long {
        val record = AlarmRecord.createBloodSugarReminder(hour, minute, repeatFlag)
        return alarmDao.insert(record)
    }

    /**
     * 添加血压测量提醒
     * @param hour 小时
     * @param minute 分钟
     * @param repeatFlag 重复标志
     * @return 插入记录的ID
     */
    suspend fun addBloodPressureReminder(
        hour: Int,
        minute: Int,
        repeatFlag: Int = AlarmRecord.REPEAT_DAILY
    ): Long {
        val record = AlarmRecord.createBloodPressureReminder(hour, minute, repeatFlag)
        return alarmDao.insert(record)
    }

    /**
     * 添加服药提醒
     * @param hour 小时
     * @param minute 分钟
     * @param repeatFlag 重复标志
     * @return 插入记录的ID
     */
    suspend fun addMedicationReminder(
        hour: Int,
        minute: Int,
        repeatFlag: Int = AlarmRecord.REPEAT_DAILY
    ): Long {
        val record = AlarmRecord.createMedicationReminder(hour, minute, repeatFlag)
        return alarmDao.insert(record)
    }

    /**
     * 插入闹钟记录
     * @param record 闹钟记录
     * @return 插入记录的ID
     */
    suspend fun insertAlarmRecord(record: AlarmRecord): Long {
        return alarmDao.insert(record)
    }

    /**
     * 获取启用的闹钟记录
     * @return Flow形式的启用闹钟记录列表
     */
    fun getEnabledRecords(): Flow<List<AlarmRecord>> {
        return alarmDao.getEnabledRecords()
    }

    /**
     * 根据类型获取闹钟记录
     * @param type 闹钟类型
     * @return Flow形式的闹钟记录列表
     */
    fun getRecordsByType(type: Int): Flow<List<AlarmRecord>> {
        return alarmDao.getRecordsByType(type)
    }

    /**
     * 获取血糖测量提醒
     * @return Flow形式的血糖测量提醒列表
     */
    fun getBloodSugarReminders(): Flow<List<AlarmRecord>> {
        return getRecordsByType(AlarmRecord.TYPE_BLOOD_SUGAR)
    }

    /**
     * 获取血压测量提醒
     * @return Flow形式的血压测量提醒列表
     */
    fun getBloodPressureReminders(): Flow<List<AlarmRecord>> {
        return alarmDao.getRecordsByType(AlarmRecord.TYPE_BLOOD_PRESSURE)
    }

    /**
     * 获取服药提醒
     * @return Flow形式的服药提醒列表
     */
    fun getMedicationReminders(): Flow<List<AlarmRecord>> {
        return alarmDao.getRecordsByType(AlarmRecord.TYPE_MEDICATION)
    }

    /**
     * 获取饮水提醒
     * @return Flow形式的饮水提醒列表
     */
    fun getHydrationReminders(): Flow<List<AlarmRecord>> {
        return alarmDao.getRecordsByType(AlarmRecord.TYPE_HYDRATION)
    }

    /**
     * 获取重复闹钟记录
     * @return Flow形式的重复闹钟记录列表
     */
    fun getRepeatingRecords(): Flow<List<AlarmRecord>> {
        return alarmDao.getRepeatingRecords()
    }

    /**
     * 获取单次闹钟记录
     * @return Flow形式的单次闹钟记录列表
     */
    fun getOnceRecords(): Flow<List<AlarmRecord>> {
        return alarmDao.getOnceRecords()
    }

    /**
     * 切换闹钟启用状态
     * @param recordId 记录ID
     * @return 操作是否成功
     */
    suspend fun toggleAlarmEnabled(recordId: Long): Boolean {
        val record = alarmDao.getById(recordId) ?: return false
        val newStatus = !record.isEnabled
        return alarmDao.updateEnabledStatus(recordId, newStatus) > 0
    }

    /**
     * 启用闹钟
     * @param recordId 记录ID
     * @return 操作是否成功
     */
    suspend fun enableAlarm(recordId: Long): Boolean {
        return alarmDao.updateEnabledStatus(recordId, true) > 0
    }

    /**
     * 禁用闹钟
     * @param recordId 记录ID
     * @return 操作是否成功
     */
    suspend fun disableAlarm(recordId: Long): Boolean {
        return alarmDao.updateEnabledStatus(recordId, false) > 0
    }

    /**
     * 软删除闹钟记录
     * @param recordId 记录ID
     * @return 操作是否成功
     */
    suspend fun softDeleteRecord(recordId: Long): Boolean {
        return alarmDao.softDeleteById(recordId) > 0
    }

    /**
     * 恢复已删除的闹钟记录
     * @param recordId 记录ID
     * @return 操作是否成功
     */
    suspend fun restoreRecord(recordId: Long): Boolean {
        return alarmDao.restoreRecord(recordId) > 0
    }

    /**
     * 获取已删除的记录（回收站功能）
     * @return Flow形式的已删除记录列表
     */
    fun getDeletedRecords(): Flow<List<AlarmRecord>> {
        return alarmDao.getDeletedRecords()
    }

    /**
     * 更新闹钟最后触发时间
     * @param recordId 记录ID
     * @param triggerTime 触发时间戳，默认为当前时间
     * @return 操作是否成功
     */
    suspend fun updateLastTriggerTime(recordId: Long, triggerTime: Long = System.currentTimeMillis()): Boolean {
        return alarmDao.updateLastTriggerTime(recordId, triggerTime) > 0
    }

    /**
     * 检查指定时间是否已存在闹钟
     * @param hour 小时
     * @param minute 分钟
     * @param excludeId 排除的记录ID（用于更新时检查）
     * @return 是否存在相同时间的闹钟
     */
    suspend fun existsAtTime(hour: Int, minute: Int, excludeId: Long = -1): Boolean {
        return alarmDao.existsAtTime(hour, minute, excludeId)
    }

    /**
     * 按类型与时间获取闹钟记录
     */
    suspend fun getRecordsByTypeAndTime(type: Int, hour: Int, minute: Int): List<AlarmRecord> {
        return alarmDao.getRecordsByTypeAndTime(type, hour, minute)
    }

    /**
     * 获取下一个即将触发的闹钟
     * @return 下一个闹钟记录，可能为null
     */
    suspend fun getNextAlarm(): AlarmRecord? {
        val now = DateTimeUtils.now()
        val calendar = java.util.Calendar.getInstance()
        calendar.time = now
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        
        return alarmDao.getNextAlarm(currentHour, currentMinute)
    }

    /**
     * 获取闹钟统计信息
     * @return 闹钟统计数据
     */
    suspend fun getAlarmStatistics(): AlarmStatistics {
        val totalCount = alarmDao.getRecordCount()
        val enabledCount = alarmDao.getEnabledRecordCount()
        val bloodSugarCount = alarmDao.getRecordCountByType(AlarmRecord.TYPE_BLOOD_SUGAR)
        val bloodPressureCount = alarmDao.getRecordCountByType(AlarmRecord.TYPE_BLOOD_PRESSURE)
        val medicationCount = alarmDao.getRecordCountByType(AlarmRecord.TYPE_MEDICATION)
        // 饮水提醒统计暂不用于现有UI，如后续需要可在此处扩展
        
        return AlarmStatistics(
            totalCount = totalCount,
            enabledCount = enabledCount,
            disabledCount = totalCount - enabledCount,
            bloodSugarReminderCount = bloodSugarCount,
            bloodPressureReminderCount = bloodPressureCount,
            medicationReminderCount = medicationCount
        )
    }

    /**
     * 添加饮水提醒
     */
    suspend fun addHydrationReminder(
        hour: Int,
        minute: Int,
        repeatFlag: Int = AlarmRecord.REPEAT_DAILY
    ): Long {
        val record = AlarmRecord.createHydrationReminder(hour, minute, repeatFlag)
        return alarmDao.insert(record)
    }

    /**
     * 批量启用/禁用闹钟
     * @param recordIds 记录ID列表
     * @param isEnabled 是否启用
     * @return 成功操作的记录数
     */
    suspend fun batchUpdateEnabledStatus(recordIds: List<Long>, isEnabled: Boolean): Int {
        var successCount = 0
        recordIds.forEach { recordId ->
            if (alarmDao.updateEnabledStatus(recordId, isEnabled) > 0) {
                successCount++
            }
        }
        return successCount
    }

    /**
     * 批量软删除闹钟
     * @param recordIds 记录ID列表
     * @return 成功操作的记录数
     */
    suspend fun batchSoftDelete(recordIds: List<Long>): Int {
        var successCount = 0
        recordIds.forEach { recordId ->
            if (alarmDao.softDeleteById(recordId) > 0) {
                successCount++
            }
        }
        return successCount
    }

    /**
     * 清理回收站（永久删除已软删除的记录）
     * @return 删除的记录数
     */
    suspend fun clearRecycleBin(): Int {
        return alarmDao.permanentlyDeleteSoftDeletedRecords()
    }
}

/**
 * 闹钟统计数据类
 */
data class AlarmStatistics(
    val totalCount: Int,
    val enabledCount: Int,
    val disabledCount: Int,
    val bloodSugarReminderCount: Int,
    val bloodPressureReminderCount: Int,
    val medicationReminderCount: Int
)