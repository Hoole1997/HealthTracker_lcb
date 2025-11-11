package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 饮水记录实体
 * 对应数据表：hydrate_records
 * 仅包含最核心字段：id、饮水量（ml）、时间
 */
@Entity(tableName = "hydrate_records")
data class HydrateRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** 记录时间 */
    @ColumnInfo(name = "record_time")
    val recordTime: Date,

    /** 饮水量（毫升） */
    @ColumnInfo(name = "intake_ml")
    val intakeMl: Int
)