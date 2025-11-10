package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 胆固醇记录实体
 * 默认仅记录总胆固醇值（mg/dL）
 */
@Entity(tableName = "cholesterol_records")
data class CholesterolRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * 记录时间
     */
    @ColumnInfo(name = "record_time")
    val recordTime: Date,

    /** 高密度脂蛋白 HDL（mg/dL） */
    @ColumnInfo(name = "hdl")
    val hdl: Int,

    /** 低密度脂蛋白 LDL（mg/dL） */
    @ColumnInfo(name = "ldl")
    val ldl: Int,

    /** 甘油三酯 TG（mg/dL） */
    @ColumnInfo(name = "triglyceride")
    val triglyceride: Int,

    /** 总胆固醇 TC（mg/dL） */
    @ColumnInfo(name = "tc")
    val tc: Float,

    /** 非高密度脂蛋白 Non-HDL（mg/dL） */
    @ColumnInfo(name = "non_hdl")
    val nonHdl: Float,

    /** TC/HDL 比值 */
    @ColumnInfo(name = "tc_hdl_ratio")
    val tcHdlRatio: Float,

    /** LDL/HDL 比值 */
    @ColumnInfo(name = "ldl_hdl_ratio")
    val ldlHdlRatio: Float,

    /**
     * 标签ID列表（逗号分隔）
     */
    @ColumnInfo(name = "tag_ids")
    val tagIds: String? = null,

    /**
     * 软删除标记
     */
    @ColumnInfo(name = "is_delete")
    val isDeleted: Boolean = false,

    /**
     * 更新时间戳
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "ext1")
    val ext1: String? = null,

    @ColumnInfo(name = "ext2")
    val ext2: String? = null,

    @ColumnInfo(name = "ext3")
    val ext3: String? = null
) {
    fun getTagIdList(): List<Long> {
        if (tagIds.isNullOrBlank()) return emptyList()
        return tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    fun hasTag(tagId: Long): Boolean = getTagIdList().contains(tagId)

    fun withUpdatedTimestamp(): CholesterolRecord = copy(updatedAt = System.currentTimeMillis())
}
