package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daily.health.manager.data.enums.BmiUnit
import java.util.Date

/**
 * BMI记录数据实体（最简实现）
 * 对应数据表：bmi_records
 *
 * 仅存储原始身高与体重，不保存BMI数值与分类，也不包含图表显示字段。
 */
@Entity(tableName = "t06")
data class LocalEntity06(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /**
     * 记录时间 - 使用Date类型，项目已有DateTimeConverter支持
     */
    @ColumnInfo(name = "c02")
    val recordTime: Date,

    /**
     * 身高（cm）
     */
    @ColumnInfo(name = "c03")
    val heightCm: Double,

    /**
     * 体重（kg）
     */
    @ColumnInfo(name = "c04")
    val weightKg: Double,

    /**
     * 关联的标签ID列表（逗号分隔），如 "1,3,5"
     */
    @ColumnInfo(name = "c05")
    val tagIds: String? = null,

    /**
     * 软删除标记
     * true: 已删除, false: 正常
     */
    @ColumnInfo(name = "c06")
    val isDeleted: Boolean = false,

    /**
     * 更新时间戳（毫秒）。创建与更新时维护。
     */
    @ColumnInfo(name = "c07")
    val updatedAt: Long = System.currentTimeMillis(),

    /** 扩展字段（文本） */
    @ColumnInfo(name = "c08")
    val ext1: String? = null,

    @ColumnInfo(name = "c09")
    val ext2: String? = null,

    @ColumnInfo(name = "c10")
    val ext3: String? = null
) {
    /**
     * 获取关联的标签ID列表
     */
    fun getTagIdList(): List<Long> {
        return if (tagIds.isNullOrBlank()) {
            emptyList()
        } else {
            tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
    }

    /**
     * 判断是否包含指定标签
     */
    fun hasTag(tagId: Long): Boolean {
        return getTagIdList().contains(tagId)
    }

    /**
     * 创建更新后的记录副本，自动更新时间戳
     */
    fun withUpdatedTimestamp(): BmiRecord {
        return this.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * 获取用于显示的体重值（根据用户偏好单位转换，不含单位）
     */
    fun getDisplayWeightValue(preferredUnit: BmiUnit = BmiUnit.getPreferredWeightUnit()): String {
        return BmiUnit.formatDisplayWeight(weightKg.toFloat(), preferredUnit)
    }
}

typealias BmiRecord = LocalEntity06
