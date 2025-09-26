package com.healthtracker.blood.suger.data.dao

import androidx.room.*
import com.healthtracker.blood.suger.data.entity.MedicineReminder
import kotlinx.coroutines.flow.Flow

/**
 * 药物提醒DAO - 只包含必需功能
 */
@Dao
interface MedicineReminderDao {

    /**
     * 获取所有启用的药物提醒
     */
    @Query("SELECT * FROM medicine_reminders WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveReminders(): Flow<List<MedicineReminder>>

    /**
     * 根据ID获取药物提醒
     */
    @Query("SELECT * FROM medicine_reminders WHERE id = :id")
    suspend fun getById(id: Long): MedicineReminder?

    /**
     * 插入药物提醒
     */
    @Insert
    suspend fun insert(reminder: MedicineReminder): Long

    /**
     * 更新药物提醒
     */
    @Update
    suspend fun update(reminder: MedicineReminder)

    /**
     * 更新已服药时间记录（最常用的操作）
     */
    @Query("UPDATE medicine_reminders SET taked_times = :takedTimes WHERE id = :id")
    suspend fun updateTakedTimes(id: Long, takedTimes: String)

    /**
     * 更新真实提醒时间记录
     */
    @Query("UPDATE medicine_reminders SET real_remind_times = :realRemindTimes WHERE id = :id")
    suspend fun updateRealRemindTimes(id: Long, realRemindTimes: String)

    /**
     * 更新备注
     */
    @Query("UPDATE medicine_reminders SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    /**
     * 更新日历同步设置
     */
    @Query("UPDATE medicine_reminders SET sync_calendar = :syncCalendar WHERE id = :id")
    suspend fun updateSyncCalendar(id: Long, syncCalendar: Int)

    /**
     * 启用/禁用提醒
     */
    @Query("UPDATE medicine_reminders SET is_active = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    /**
     * 删除提醒
     */
    @Query("DELETE FROM medicine_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有药物提醒（包括禁用的）
     */
    @Query("SELECT * FROM medicine_reminders ORDER BY is_active DESC, created_at DESC")
    fun getAllReminders(): Flow<List<MedicineReminder>>

    /**
     * 搜索药物
     */
    @Query("SELECT * FROM medicine_reminders WHERE medicine_name LIKE '%' || :query || '%' AND is_active = 1")
    suspend fun searchByName(query: String): List<MedicineReminder>

    /**
     * 获取有备注的药物提醒
     */
    @Query("SELECT * FROM medicine_reminders WHERE note != '' AND is_active = 1")
    suspend fun getRemindersWithNotes(): List<MedicineReminder>

    /**
     * 获取同步到日历的药物提醒
     */
    @Query("SELECT * FROM medicine_reminders WHERE sync_calendar = 1 AND is_active = 1")
    suspend fun getSyncedReminders(): List<MedicineReminder>
}