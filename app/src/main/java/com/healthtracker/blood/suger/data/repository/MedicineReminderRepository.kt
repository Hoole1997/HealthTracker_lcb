package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.MedicineReminderDao
import com.healthtracker.blood.suger.data.entity.MedicineReminder
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 药物提醒仓库 - 只包含核心功能
 */
@Singleton
class MedicineReminderRepository @Inject constructor(
    private val dao: MedicineReminderDao
) {

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
        val reminder = MedicineReminder.createFromTimeStrings(
            medicineName, reminderTimes, medicineCover, note, syncCalendar
        )
        return dao.insert(reminder)
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
        syncCalendar: Boolean? = null
    ) {
        val current = dao.getById(id) ?: return

        // 处理提醒时间更新
        val newStartRemindTimes = reminderTimes?.let { times ->
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val today = java.util.Calendar.getInstance()

            times.mapNotNull { timeStr ->
                try {
                    val time = timeFormat.parse(timeStr)
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = today.time
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, java.util.Calendar.getInstance().apply { time = time }.get(java.util.Calendar.HOUR_OF_DAY))
                    calendar.set(java.util.Calendar.MINUTE, java.util.Calendar.getInstance().apply { time = time }.get(java.util.Calendar.MINUTE))
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    calendar.time.time.toString()
                } catch (e: Exception) { null }
            }.joinToString(",")
        }

        val updated = current.copy(
            medicineName = medicineName ?: current.medicineName,
            startRemindTimes = newStartRemindTimes ?: current.startRemindTimes,
            medicineCover = medicineCover ?: current.medicineCover,
            note = note ?: current.note,
            syncCalendar = syncCalendar?.let { if (it) 1 else 0 } ?: current.syncCalendar
        )
        dao.update(updated)
    }

    /**
     * 启用/禁用药物提醒
     */
    suspend fun setMedicineActive(id: Long, isActive: Boolean) {
        dao.setActive(id, isActive)
    }

    /**
     * 删除药物提醒
     */
    suspend fun deleteMedicine(id: Long) {
        dao.deleteById(id)
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

    companion object {
        /**
         * 快速创建方法
         */
        suspend fun MedicineReminderRepository.addOnceDailyMedicine(
            name: String,
            cover: String = "",
            note: String = "",
            sync: Boolean = false
        ) = addMedicineWithPreset(name, MedicineReminder.PresetTimes.ONCE_DAILY, cover, note, sync)

        suspend fun MedicineReminderRepository.addTwiceDailyMedicine(
            name: String,
            cover: String = "",
            note: String = "",
            sync: Boolean = false
        ) = addMedicineWithPreset(name, MedicineReminder.PresetTimes.TWICE_DAILY, cover, note, sync)

        suspend fun MedicineReminderRepository.addThreeTimesDailyMedicine(
            name: String,
            cover: String = "",
            note: String = "",
            sync: Boolean = false
        ) = addMedicineWithPreset(name, MedicineReminder.PresetTimes.THREE_TIMES_DAILY, cover, note, sync)
    }
}