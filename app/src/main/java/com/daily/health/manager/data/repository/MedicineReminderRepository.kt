package com.daily.health.manager.data.repository

import com.daily.health.manager.alarm.AlarmScheduler
import com.daily.health.manager.data.dao.MedicineReminderDao
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.data.entity.MedicineReminder
import com.daily.health.manager.data.entity.PresetTimes
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import kotlinx.coroutines.flow.Flow
import java.util.*
import com.daily.health.manager.data.utils.DateTimeUtils

/**
 * 药物提醒仓库 - 包含核心功能和闹钟调度
 */
class MedicineReminderRepository(
    private val dao: MedicineReminderDao,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) {

    companion object {
        private const val TAG = "MedicineReminderRepository"
    }

    /**
     * 解析时间字符串
     * @param timeStr 时间字符串，格式如 "08:00", "14:30"
     * @return Pair<hour, minute>
     */
    private fun parseTimeString(timeStr: String): Pair<Int, Int> {
        return DateTimeUtils.parseTimeString(timeStr) ?: run {
            "Failed to parse time string: $timeStr".loge(TAG)
            throw IllegalArgumentException("Invalid time format: $timeStr")
        }
    }

    /**
     * 获取所有启用的提醒
     */
    fun getActiveReminders(): Flow<List<MedicineReminder>> {
        return dao.getActiveReminders()
    }

    /**
     * 添加新的药物提醒
     * @param medicineName 药物名称
     * @param reminderTimes 提醒时间列表，如 ["08:00", "12:00", "18:00"]
     * @param medicineCover 药物封面图片路径
     * @param note 备注信息
     * @param syncCalendar 是否同步到系统日历
     */
    suspend fun addMedicine(
        medicineName: String,
        reminderTimes: List<String>,
        medicineCover: String = "",
        note: String = "",
        syncCalendar: Boolean = false
    ): Long {
        try {
            // 1. 保存药物提醒到数据库
            val reminder = MedicineReminder.createFromTimeStrings(
                medicineName, reminderTimes, medicineCover, note, syncCalendar
            )
            val medicineId = dao.insert(reminder)

            "Medicine reminder saved: ID=$medicineId, Name=$medicineName".logd(TAG)

            // 2. 为每个提醒时间创建系统闹钟
            reminderTimes.forEach { timeStr ->
                try {
                    val (hour, minute) = parseTimeString(timeStr)

                    // 创建服药提醒闹钟记录（简化版）
                    val alarmRecord = AlarmRecord.createMedicationAlarm(
                        medicineId = medicineId,
                        hour = hour,
                        minute = minute,
                        repeatFlag = AlarmRecord.REPEAT_DAILY // 每天重复
                    )

                    // 保存闹钟记录到数据库
                    val alarmId = alarmRepository.insertAlarmRecord(alarmRecord)

                    // 调度系统闹钟
                    val scheduleSuccess = alarmScheduler.scheduleAlarm(alarmRecord.copy(id = alarmId))

                    if (scheduleSuccess) {
                        "Medication alarm scheduled: MedicineID=$medicineId, AlarmID=$alarmId, Time=$timeStr".logd(TAG)
                    } else {
                        "Failed to schedule medication alarm: MedicineID=$medicineId, Time=$timeStr".loge(TAG)
                    }

                } catch (e: Exception) {
                    "Error creating alarm for time $timeStr: ${e.message}".loge(TAG)
                }
            }

            return medicineId

        } catch (e: Exception) {
            "Error adding medicine reminder: ${e.message}".loge(TAG)
            throw e
        }
    }

    /**
     * 记录服药 - 最核心的功能
     * @param id 药物提醒ID
     * @param takenTime 服药时间，默认为当前时间
     */
    suspend fun recordMedication(id: Long, takenTime: Date = Date()) {
        val current = dao.getById(id) ?: return
        val updated = current.addTakedRecord(takenTime)
        dao.updateTakedTimes(id, updated.takedTimes)
    }

    /**
     * 记录真实提醒时间 - 系统推送提醒时调用
     * @param id 药物提醒ID
     * @param remindTime 提醒时间，默认为当前时间
     */
    suspend fun recordRealRemind(id: Long, remindTime: Date = Date()) {
        val current = dao.getById(id) ?: return
        val updated = current.addRealRemindTime(remindTime)
        dao.updateRealRemindTimes(id, updated.realRemindTimes)
    }


    /**
     * 更新药物信息
     */
    suspend fun updateMedicine(
        id: Long,
        medicineName: String? = null,
        reminderTimes: List<String>? = null,
        medicineCover: String? = null,
        note: String? = null,
        syncCalendar: Boolean? = null,
    ) {
        try {
            val current = dao.getById(id) ?: return
            "Updating medicine: ID=$id, Name=${medicineName ?: current.medicineName}".logd(TAG)

            // 如果提醒时间发生变化，需要更新闹钟
            if (reminderTimes != null) {
                // 1. 取消所有旧的药物闹钟
                cancelMedicationAlarms(id)

                // 2. 为新的提醒时间创建闹钟
                val actualMedicineName = medicineName ?: current.medicineName
                val actualNote = note ?: current.note

                reminderTimes.forEach { timeStr ->
                    try {
                        val (hour, minute) = parseTimeString(timeStr)

                        // 创建新的服药提醒闹钟（简化版）
                        val alarmRecord = AlarmRecord.createMedicationAlarm(
                            medicineId = id,
                            hour = hour,
                            minute = minute,
                            repeatFlag = AlarmRecord.REPEAT_DAILY
                        )

                        // 保存闹钟记录到数据库
                        val alarmId = alarmRepository.insertAlarmRecord(alarmRecord)

                        // 调度系统闹钟
                        val scheduleSuccess = alarmScheduler.scheduleAlarm(alarmRecord.copy(id = alarmId))

                        if (scheduleSuccess) {
                            "Updated medication alarm: MedicineID=$id, AlarmID=$alarmId, Time=$timeStr".logd(TAG)
                        } else {
                            "Failed to schedule updated alarm: MedicineID=$id, Time=$timeStr".loge(TAG)
                        }

                    } catch (e: Exception) {
                        "Error updating alarm for time $timeStr: ${e.message}".loge(TAG)
                    }
                }
            }

            // 处理提醒时间更新
            val newStartRemindTimes = reminderTimes?.let { times ->
                val today = Calendar.getInstance()

                times.mapNotNull { timeStr ->
                    try {
                        val (hour, minute) = parseTimeString(timeStr)
                        val calendar = Calendar.getInstance()
                        calendar.time = today.time
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        calendar.time.time.toString()
                    } catch (e: Exception) { null }
                }.joinToString(",")
            }

            // 更新药物提醒记录
            val updated = current.copy(
                medicineName = medicineName ?: current.medicineName,
                startRemindTimes = newStartRemindTimes ?: current.startRemindTimes,
                medicineCover = medicineCover ?: current.medicineCover,
                note = note ?: current.note,
                syncCalendar = syncCalendar?.let { if (it) 1 else 0 } ?: current.syncCalendar
            )
            dao.update(updated)

            "Medicine updated successfully: ID=$id".logd(TAG)

        } catch (e: Exception) {
            "Error updating medicine: ID=$id, Error=${e.message}".loge(TAG)
            throw e
        }
    }

    /**
     * 取消指定药物的所有闹钟
     * @param medicineId 药物ID
     */
    private suspend fun cancelMedicationAlarms(medicineId: Long) {
        try {
            // 1. 获取该药物关联的所有闹钟记录
            val alarms = alarmRepository.getAlarmsByMedicineId(medicineId)
            if (alarms.isEmpty()) {
                "No medication alarms found for MedicineID=$medicineId".logd(TAG)
                return
            }

            // 2. 逐个取消系统闹钟
            alarms.forEach { alarm ->
                alarmScheduler.cancelAlarm(alarm)
            }

            // 3. 批量软删除数据库记录
            val deletedCount = alarmRepository.softDeleteAlarmsByMedicineId(medicineId)
            "Cancelled $deletedCount medication alarms for MedicineID=$medicineId".logd(TAG)
        } catch (e: Exception) {
            "Error cancelling medication alarms: MedicineID=$medicineId, Error=${e.message}".loge(TAG)
        }
    }

    /**
     * 启用/禁用药物提醒
     */
    suspend fun setMedicineActive(id: Long, isActive: Boolean) {
        try {
            dao.setActive(id, isActive)

            // 联动闹钟启用/禁用
            val alarms = alarmRepository.getAlarmsByMedicineId(id)
            alarms.forEach { alarm ->
                if (isActive) {
                    alarmRepository.enableAlarm(alarm.id)
                    alarmScheduler.scheduleAlarm(alarm.copy(isEnabled = true))
                } else {
                    alarmScheduler.cancelAlarm(alarm)
                    alarmRepository.disableAlarm(alarm.id)
                }
            }
            "Medicine active state updated: ID=$id, active=$isActive, alarms=${alarms.size}".logd(TAG)
        } catch (e: Exception) {
            "Error updating medicine active state: ID=$id, Error=${e.message}".loge(TAG)
        }
    }

    /**
     * 删除药物提醒
     */
    suspend fun deleteMedicine(id: Long) {
        try {
            // 先取消关联的系统闹钟
            cancelMedicationAlarms(id)
            // 再删除药物提醒记录
            dao.deleteById(id)
            "Medicine deleted with alarms cancelled: ID=$id".logd(TAG)
        } catch (e: Exception) {
            "Error deleting medicine: ID=$id, Error=${e.message}".loge(TAG)
            throw e
        }
    }

    /**
     * 搜索药物
     */
    suspend fun searchMedicines(query: String): List<MedicineReminder> {
        return dao.searchByName(query)
    }

    /**
     * 获取药物详情
     */
    suspend fun getMedicineById(id: Long): MedicineReminder? {
        return dao.getById(id)
    }


    /**
     * 更新备注
     */
    suspend fun updateNote(id: Long, note: String) {
        dao.updateNote(id, note)
    }

    /**
     * 更新日历同步设置
     */
    suspend fun updateSyncCalendar(id: Long, syncCalendar: Boolean) {
        dao.updateSyncCalendar(id, if (syncCalendar) 1 else 0)
    }

    /**
     * 获取有备注的药物提醒
     */
    suspend fun getRemindersWithNotes(): List<MedicineReminder> {
        return dao.getRemindersWithNotes()
    }

    /**
     * 获取同步到日历的药物提醒
     */
    suspend fun getSyncedReminders(): List<MedicineReminder> {
        return dao.getSyncedReminders()
    }

    /**
     * 使用预设时间快速创建提醒
     */
    suspend fun addMedicineWithPreset(
        medicineName: String,
        preset: List<String>,
        medicineCover: String = "",
        note: String = "",
        syncCalendar: Boolean = false
    ): Long {
        return addMedicine(medicineName, preset, medicineCover, note, syncCalendar)
    }

}

/**
 * 快速创建方法扩展函数
 */
suspend fun MedicineReminderRepository.addOnceDailyMedicine(
    name: String,
    cover: String = "",
    note: String = "",
    sync: Boolean = false
) = addMedicineWithPreset(name, PresetTimes.ONCE_DAILY, cover, note, sync)

suspend fun MedicineReminderRepository.addTwiceDailyMedicine(
    name: String,
    cover: String = "",
    note: String = "",
    sync: Boolean = false
) = addMedicineWithPreset(name, PresetTimes.TWICE_DAILY, cover, note, sync)

suspend fun MedicineReminderRepository.addThreeTimesDailyMedicine(
    name: String,
    cover: String = "",
    note: String = "",
    sync: Boolean = false
) = addMedicineWithPreset(name, PresetTimes.THREE_TIMES_DAILY, cover, note, sync)