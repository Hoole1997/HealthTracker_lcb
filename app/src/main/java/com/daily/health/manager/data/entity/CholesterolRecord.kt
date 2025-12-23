package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 胆固醇记录实体
 * 默认仅记录总胆固醇值（mg/dL）
 */
@Entity(tableName = "t08")
data class LocalEntity08(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /**
     * 记录时间
     */
    @ColumnInfo(name = "c02")
    val recordTime: Date,

    /** 高密度脂蛋白 HDL（mg/dL） */
    @ColumnInfo(name = "c03")
    val hdl: Int,

    /** 低密度脂蛋白 LDL（mg/dL） */
    @ColumnInfo(name = "c04")
    val ldl: Int,

    /** 甘油三酯 TG（mg/dL） */
    @ColumnInfo(name = "c05")
    val triglyceride: Int,

    /** 总胆固醇 TC（mg/dL） */
    @ColumnInfo(name = "c06")
    val tc: Float,

    /** 非高密度脂蛋白 Non-HDL（mg/dL） */
    @ColumnInfo(name = "c07")
    val nonHdl: Float,

    /** TC/HDL 比值 */
    @ColumnInfo(name = "c08")
    val tcHdlRatio: Float,

    /** LDL/HDL 比值 */
    @ColumnInfo(name = "c09")
    val ldlHdlRatio: Float,

    /**
     * 标签ID列表（逗号分隔）
     */
    @ColumnInfo(name = "c10")
    val tagIds: String? = null,

    /**
     * 软删除标记
     */
    @ColumnInfo(name = "c11")
    val isDeleted: Boolean = false,

    /**
     * 更新时间戳
     */
    @ColumnInfo(name = "c12")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "c13")
    val ext1: String? = null,

    @ColumnInfo(name = "c14")
    val ext2: String? = null,

    @ColumnInfo(name = "c15")
    val ext3: String? = null
) {
    fun getTagIdList(): List<Long> {
        if (tagIds.isNullOrBlank()) return emptyList()
        return tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun hasTag(tagId: Long): Boolean = getTagIdList().contains(tagId)

    fun withUpdatedTimestamp(): CholesterolRecord = copy(updatedAt = System.currentTimeMillis())
}

typealias CholesterolRecord = LocalEntity08
