package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 饮水提醒实体
 * 对应数据表：hydrate_reminders
 * 仅记录一天24小时内的提醒时间：小时与分钟
 */
@Entity(
    tableName = "hydrate_reminders",
    indices = [Index(value = ["hour", "minute"], unique = true)]
)
data class HydrateReminder(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** 提醒小时（0-23） */
    @ColumnInfo(name = "hour")
    val hour: Int,

    /** 提醒分钟（0-59） */
    @ColumnInfo(name = "minute")
    val minute: Int,

    /** 是否启用该提醒（默认启用） */
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true
)