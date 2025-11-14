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
    val intakeMl: Int,
    /**
     * 软删除标记
     * true: 已删除, false: 正常
     */
    @ColumnInfo(name = "is_delete")
    val isDeleted: Boolean = false,

    /**
     * 更新时间戳（毫秒）。创建与更新时维护。
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /** 扩展字段（文本） */
    @ColumnInfo(name = "ext1")
    val ext1: String? = null,

    @ColumnInfo(name = "ext2")
    val ext2: String? = null,

    @ColumnInfo(name = "ext3")
    val ext3: String? = null
)