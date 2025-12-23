package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daily.health.manager.data.enums.BsUnit
import java.util.Date

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
     * 更新时间戳（毫秒）。创建与更新时维护。
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * 血糖值 (mg/dL)
     * 有效范围: 18.0~630.0 mg/dL
     */
    @ColumnInfo(name = "glucose_value")
    val glucoseValue: Double,

    /**
     * 状态类型
     * 存储枚举的code值，支持国际化
     */
    @ColumnInfo(name = "status")
    val satus: Int,


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
    val ext3: String? = null,

    /**
     * 用户新增时选择的单位类型
     * 0: mg/dL, 1: mmol/L
     * 默认为 mg/dL
     */
    @ColumnInfo(name = "selected_unit", defaultValue = "0")
    val selectedUnit: Int = BsUnit.MG_DL.value
) {
    /**
     * 获取用户选择的单位类型
     * @return GlucoseUnit枚举
     */
    fun getSelectedUnitEnum(): BsUnit {
        return BsUnit.fromValue(selectedUnit)
    }


    /**
     * 根据用户选择的单位获取血糖数值（不含单位）
     * @return 根据选择单位转换后的数值
     */
    fun getDisplayGlucoseValue(): Double {
        return getSelectedUnitEnum().convertFromMgdl(glucoseValue)
    }

    /**
     * 获取格式化的血糖值字符串（保留一位小数）
     * @return 格式化的血糖值字符串，不含单位
     */
    fun getFormattedDisplayValue(): String {
        val displayValue = getDisplayGlucoseValue()
        return String.format(java.util.Locale.ROOT, "%.1f", displayValue)
    }

    /**
     * 获取血糖值的mmol/L单位表示（保持向后兼容）
     * @return mmol/L单位的血糖值
     */
    @Deprecated("使用 getDisplayGlucoseValue() 和 getSelectedUnitEnum() 替代")
    fun getGlucoseInMmol(): Double {
        return glucoseValue / BsUnit.CONVERSION_FACTOR
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
         * @param recordTime 记录时间
         * @param glucoseValue 血糖值（总是以mg/dL存储）
         * @param status 测量状态
         * @param selectedUnit 用户选择的单位类型
         * @param tagIds 关联标签ID列表
         * @param showInChart 是否在图表中显示
         * @param ext1 扩展字段1
         * @param ext2 扩展字段2
         * @param ext3 扩展字段3
         */
        fun create(
            recordTime: Date,
            glucoseValue: Double,
            status: Int,
            selectedUnit: BsUnit = BsUnit.MG_DL,
            tagIds: List<Long>? = null,
            showInChart: Boolean = true,
            ext1: String? = null,
            ext2: String? = null,
            ext3: String? = null
        ): BloodSugarRecord {
            val tagIdsString = tagIds?.joinToString(",")
            return BloodSugarRecord(
                recordTime = recordTime,
                glucoseValue = glucoseValue,
                satus = status,
                tagIds = tagIdsString,
                showInChart = showInChart,
                selectedUnit = selectedUnit.value,
                ext1 = ext1,
                ext2 = ext2,
                ext3 = ext3
            )
        }
    }
}