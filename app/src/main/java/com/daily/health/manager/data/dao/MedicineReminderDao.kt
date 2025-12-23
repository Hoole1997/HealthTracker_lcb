package com.daily.health.manager.data.dao

import androidx.room.*
import com.daily.health.manager.data.entity.MedicineReminder
import kotlinx.coroutines.flow.Flow

/**
 * 药物提醒DAO - 只包含必需功能
 */
@Dao
interface LocalDao05 {

    /**
     * 获取所有启用的药物提醒
     */
    @Query("SELECT * FROM t05 WHERE c10 = 1 ORDER BY c11 DESC")
    fun getActiveReminders(): Flow<List<MedicineReminder>>

    /**
     * 根据ID获取药物提醒
     */
    @Query("SELECT * FROM t05 WHERE c01 = :id")
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
    @Query("UPDATE t05 SET c08 = :takedTimes WHERE c01 = :id")
    suspend fun updateTakedTimes(id: Long, takedTimes: String)

    /**
     * 更新真实提醒时间记录
     */
    @Query("UPDATE t05 SET c09 = :realRemindTimes WHERE c01 = :id")
    suspend fun updateRealRemindTimes(id: Long, realRemindTimes: String)

    /**
     * 更新备注
     */
    @Query("UPDATE t05 SET c05 = :note WHERE c01 = :id")
    suspend fun updateNote(id: Long, note: String)

    /**
     * 更新日历同步设置
     */
    @Query("UPDATE t05 SET c06 = :syncCalendar WHERE c01 = :id")
    suspend fun updateSyncCalendar(id: Long, syncCalendar: Int)

    /**
     * 启用/禁用提醒
     */
    @Query("UPDATE t05 SET c10 = :isActive WHERE c01 = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    /**
     * 删除提醒
     */
    @Query("DELETE FROM t05 WHERE c01 = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有药物提醒（包括禁用的）
     */
    @Query("SELECT * FROM t05 ORDER BY c10 DESC, c11 DESC")
    fun getAllReminders(): Flow<List<MedicineReminder>>

    /**
     * 搜索药物
     */
    @Query("SELECT * FROM t05 WHERE c02 LIKE '%' || :query || '%' AND c10 = 1")
    suspend fun searchByName(query: String): List<MedicineReminder>

    /**
     * 获取有备注的药物提醒
     */
    @Query("SELECT * FROM t05 WHERE c05 != '' AND c10 = 1")
    suspend fun getRemindersWithNotes(): List<MedicineReminder>

    /**
     * 获取同步到日历的药物提醒
     */
    @Query("SELECT * FROM t05 WHERE c06 = 1 AND c10 = 1")
    suspend fun getSyncedReminders(): List<MedicineReminder>
}

typealias MedicineReminderDao = LocalDao05