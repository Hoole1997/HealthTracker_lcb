package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 饮水记录实体
 * 对应数据表：hydrate_records
 * 仅包含最核心字段：id、饮水量（ml）、时间
 */
@Entity(tableName = "t09")
data class LocalEntity09(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /** 记录时间 */
    @ColumnInfo(name = "c02")
    val recordTime: Date,

    /** 饮水量（毫升） */
    @ColumnInfo(name = "c03")
    val intakeMl: Int,

    /**
     * 本次饮水时，饮水设置中一天的目标杯数
     */
    @ColumnInfo(name = "c04")
    val dailyGoalCups: Int = 0,

    /**
     * 本次饮水时，饮水设置每杯水的容积（统一以 ml 保存）
     */
    @ColumnInfo(name = "c05")
    val cupVolumeMl: Int = 0,

    /**
     * 本次饮水时，当天的饮水目标总量（ml） = 当天杯数 * 当天每杯容积
     */
    @ColumnInfo(name = "c06")
    val dailyGoalTotalMl: Int = 0,
    /**
     * 软删除标记
     * true: 已删除, false: 正常
     */
    @ColumnInfo(name = "c07")
    val isDeleted: Boolean = false,

    /**
     * 更新时间戳（毫秒）。创建与更新时维护。
     */
    @ColumnInfo(name = "c08")
    val updatedAt: Long = System.currentTimeMillis(),

    /** 扩展字段（文本） */
    @ColumnInfo(name = "c09")
    val ext1: String? = null,

    @ColumnInfo(name = "c10")
    val ext2: String? = null,

    @ColumnInfo(name = "c11")
    val ext3: String? = null
)

typealias HydrateRecord = LocalEntity09