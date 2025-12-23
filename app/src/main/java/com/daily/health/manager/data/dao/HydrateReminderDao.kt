package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daily.health.manager.data.entity.HydrateReminder
import kotlinx.coroutines.flow.Flow

/**
 * 饮水提醒数据访问对象
 * 仅提供最核心的增、删、查、改
 */
@Dao
interface HydrateReminderDao {

    /** 查询所有提醒时间（按小时、分钟排序） */
    @Query("SELECT * FROM hydrate_reminders ORDER BY hour ASC, minute ASC")
    fun getAll(): Flow<List<HydrateReminder>>

    /** 查询所有启用的提醒时间（按小时、分钟排序） */
    @Query("SELECT * FROM hydrate_reminders WHERE enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabled(): Flow<List<HydrateReminder>>

    /** 插入提醒时间（忽略重复时间） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reminder: HydrateReminder): Long

    /** 更新提醒时间 */
    @Update
    suspend fun update(reminder: HydrateReminder)

    /** 根据ID删除提醒 */
    @Query("DELETE FROM hydrate_reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 根据时间删除提醒（匹配同一小时与分钟的记录） */
    @Query("DELETE FROM hydrate_reminders WHERE hour = :hour AND minute = :minute")
    suspend fun deleteByTime(hour: Int, minute: Int)

    /** 根据时间更新启用状态 */
    @Query("UPDATE hydrate_reminders SET enabled = :enabled WHERE hour = :hour AND minute = :minute")
    suspend fun updateEnabledByTime(hour: Int, minute: Int, enabled: Boolean): Int
}