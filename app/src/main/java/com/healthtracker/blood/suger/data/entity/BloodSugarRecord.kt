package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healthtracker.blood.suger.data.enums.GlucoseLevel
import com.healthtracker.blood.suger.data.enums.MeasurementTag
import java.util.*

/**
 * 血糖记录数据实体
 * 对应数据表：blood_sugar_records
 */
@Entity(tableName = "blood_sugar_records")
data class BloodSugarRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * 记录时间 - 通过多滚轮控件选择年月日时分钟生成
     * 兼容API 24+，使用Date类型
     */
    @ColumnInfo(name = "record_time")
    val recordTime: Date,

    /**
     * 血糖值 (mg/dL)
     * 有效范围: 18.0~630.0 mg/dL
     */
    @ColumnInfo(name = "glucose_value")
    val glucoseValue: Double,

    /**
     * 测量标签/类型
     * 存储枚举的code值，支持国际化
     */
    @ColumnInfo(name = "measurement_tag")
    val measurementTag: String,

    /**
     * 血糖等级分类
     * 存储枚举的code值，支持国际化
     */
    @ColumnInfo(name = "glucose_level")
    val glucoseLevel: String,

    /**
     * 是否在图表中显示此数据点
     */
    @ColumnInfo(name = "show_in_chart")
    val showInChart: Boolean = true,

    /**
     * 关联的标签ID列表
     * 格式：以逗号分隔的标签ID，如 "1,3,5"
     */
    @ColumnInfo(name = "tag_ids")
    val tagIds: String? = null,

    /**
     * 预留扩展字段1
     * 可用于存储备注、用药情况等
     */
    @ColumnInfo(name = "ext1")
    val ext1: String? = null,

    /**
     * 预留扩展字段2
     * 可用于存储运动情况、情绪状态等
     */
    @ColumnInfo(name = "ext2")
    val ext2: String? = null,

    /**
     * 预留扩展字段3
     * 可用于存储其他相关信息
     */
    @ColumnInfo(name = "ext3")
    val ext3: String? = null
) {
    /**
     * 获取血糖值的mmol/L单位表示
     * @return mmol/L单位的血糖值
     */
    fun getGlucoseInMmol(): Double {
        return glucoseValue * 0.0555
    }

    /**
     * 获取测量标签枚举
     * @return MeasurementTag枚举
     */
    fun getMeasurementTagEnum(): MeasurementTag {
        return MeasurementTag.fromString(measurementTag)
    }

    /**
     * 获取血糖等级枚举
     * @return GlucoseLevel枚举
     */
    fun getGlucoseLevelEnum(): GlucoseLevel {
        return GlucoseLevel.entries.find { it.code == glucoseLevel } ?: GlucoseLevel.NORMAL
    }

    /**
     * 判断是否为正常血糖
     * @return 是否正常
     */
    fun isNormal(): Boolean {
        return getGlucoseLevelEnum() == GlucoseLevel.NORMAL
    }

    /**
     * 判断是否为低血糖
     * @return 是否低血糖
     */
    fun isHypoglycemia(): Boolean {
        return getGlucoseLevelEnum() == GlucoseLevel.HYPOGLYCEMIA
    }

    /**
     * 判断是否为高血糖（糖尿病或糖尿病前期）
     * @return 是否高血糖
     */
    fun isHyperglycemia(): Boolean {
        val level = getGlucoseLevelEnum()
        return level == GlucoseLevel.PREDIABETES ||
               level == GlucoseLevel.DIABETES ||
               level == GlucoseLevel.SEVERE_HYPERGLYCEMIA
    }

    /**
     * 获取关联的标签ID列表
     * @return 标签ID列表
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
     * @param tagId 标签ID
     * @return 是否包含该标签
     */
    fun hasTag(tagId: Long): Boolean {
        return getTagIdList().contains(tagId)
    }

    companion object {
        /**
         * 创建血糖记录的工厂方法
         * 自动计算血糖等级
         */
        fun create(
            recordTime: Date,
            glucoseValue: Double,
            measurementTag: String,
            tagIds: List<Long>? = null,
            showInChart: Boolean = true,
            ext1: String? = null,
            ext2: String? = null,
            ext3: String? = null
        ): BloodSugarRecord {
            val glucoseLevel = GlucoseLevel.fromGlucoseValue(glucoseValue, measurementTag)
            val tagIdsString = tagIds?.joinToString(",")
            return BloodSugarRecord(
                recordTime = recordTime,
                glucoseValue = glucoseValue,
                measurementTag = measurementTag,
                glucoseLevel = glucoseLevel.code,
                tagIds = tagIdsString,
                showInChart = showInChart,
                ext1 = ext1,
                ext2 = ext2,
                ext3 = ext3
            )
        }
    }
}