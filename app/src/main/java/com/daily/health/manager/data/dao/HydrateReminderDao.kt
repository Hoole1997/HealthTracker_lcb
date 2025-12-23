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
interface LocalDao10 {

    /** 查询所有提醒时间（按小时、分钟排序） */
    @Query("SELECT * FROM t10 ORDER BY c02 ASC, c03 ASC")
    fun getAll(): Flow<List<HydrateReminder>>

    /** 查询所有启用的提醒时间（按小时、分钟排序） */
    @Query("SELECT * FROM t10 WHERE c04 = 1 ORDER BY c02 ASC, c03 ASC")
    fun getEnabled(): Flow<List<HydrateReminder>>

    /** 插入提醒时间（忽略重复时间） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reminder: HydrateReminder): Long

    /** 更新提醒时间 */
    @Update
    suspend fun update(reminder: HydrateReminder)

    /** 根据ID删除提醒 */
    @Query("DELETE FROM t10 WHERE c01 = :id")
    suspend fun deleteById(id: Long)

    /** 根据时间删除提醒（匹配同一小时与分钟的记录） */
    @Query("DELETE FROM t10 WHERE c02 = :hour AND c03 = :minute")
    suspend fun deleteByTime(hour: Int, minute: Int)

    /** 根据时间更新启用状态 */
    @Query("UPDATE t10 SET c04 = :enabled WHERE c02 = :hour AND c03 = :minute")
    suspend fun updateEnabledByTime(hour: Int, minute: Int, enabled: Boolean): Int
}

typealias HydrateReminderDao = LocalDao10