package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 心率记录数据实体
 * 存储基础心率信息及标签关联
 */
@Entity(tableName = "t07")
data class LocalEntity07(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /**
     * 记录时间
     */
    @ColumnInfo(name = "c02")
    val recordTime: Date,

    /**
     * 心率值（BPM）
     */
    @ColumnInfo(name = "c03")
    val heartRateBpm: Int,

    /**
     * 关联的标签ID列表（逗号分隔）
     */
    @ColumnInfo(name = "c04")
    val tagIds: String? = null,

    /**
     * 软删除标记
     */
    @ColumnInfo(name = "c05")
    val isDeleted: Boolean = false,

    /**
     * 更新时间戳
     */
    @ColumnInfo(name = "c06")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "c07")
    val ext1: String? = null,

    @ColumnInfo(name = "c08")
    val ext2: String? = null,

    @ColumnInfo(name = "c09")
    val ext3: String? = null
) {
    /**
     * 获取标签ID列表
     */
    fun getTagIdList(): List<Long> {
        if (tagIds.isNullOrBlank()) return emptyList()
        return tagIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    /**
     * 判断是否包含指定标签
     */
    fun hasTag(tagId: Long): Boolean = getTagIdList().contains(tagId)

    /**
     * 复制并更新时间戳
     */
    fun withUpdatedTimestamp(): HeartRateRecord {
        return copy(updatedAt = System.currentTimeMillis())
    }
}

typealias HeartRateRecord = LocalEntity07
