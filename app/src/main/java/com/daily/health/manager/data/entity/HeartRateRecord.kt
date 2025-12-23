package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 心率记录数据实体
 * 存储基础心率信息及标签关联
 */
@Entity(tableName = "heart_rate_records")
data class HeartRateRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * 记录时间
     */
    @ColumnInfo(name = "record_time")
    val recordTime: Date,

    /**
     * 心率值（BPM）
     */
    @ColumnInfo(name = "heart_rate_bpm")
    val heartRateBpm: Int,

    /**
     * 关联的标签ID列表（逗号分隔）
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
