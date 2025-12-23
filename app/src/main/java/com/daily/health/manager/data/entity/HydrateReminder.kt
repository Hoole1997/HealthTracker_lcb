package com.daily.health.manager.data.entity

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
    tableName = "t10",
    indices = [Index(value = ["c02", "c03"], unique = true)]
)
data class LocalEntity10(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /** 提醒小时（0-23） */
    @ColumnInfo(name = "c02")
    val hour: Int,

    /** 提醒分钟（0-59） */
    @ColumnInfo(name = "c03")
    val minute: Int,

    /** 是否启用该提醒（默认启用） */
    @ColumnInfo(name = "c04")
    val enabled: Boolean = true
)

typealias HydrateReminder = LocalEntity10