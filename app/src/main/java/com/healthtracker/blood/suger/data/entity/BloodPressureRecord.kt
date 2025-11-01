package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.data.enums.PulseCategory
import java.util.*

/**
 * 血压记录数据实体
 * 对应数据表：blood_pressure_records
 */
@Entity(tableName = "blood_pressure_records")
data class BloodPressureRecord(
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
     * 收缩压 (mmHg)
     * 正常范围: 90-140 mmHg
     */
    @ColumnInfo(name = "systolic_pressure")
    val systolicPressure: Int,

    /**
     * 舒张压 (mmHg)
     * 正常范围: 60-90 mmHg
     */
    @ColumnInfo(name = "diastolic_pressure")
    val diastolicPressure: Int,

    /**
     * 脉搏/心率 (次/分钟)
     * 正常范围: 60-100 次/分钟
     */
    @ColumnInfo(name = "pulse_rate")
    val pulseRate: Int,



    /**
     * 血压分类
     * 存储枚举的code值，支持国际化
     */
    @ColumnInfo(name = "bp_category")
    val bpCategory: String,

    /**
     * 脉搏分类
     * 存储枚举的code值，支持国际化
     */
    @ColumnInfo(name = "pulse_category")
    val pulseCategory: String,

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
     * 获取血压分类枚举
     * @return BloodPressureCategory枚举
     */
    fun getBloodPressureCategoryEnum(): BloodPressureCategory {
        return BloodPressureCategory.entries.find { it.code == bpCategory } ?: BloodPressureCategory.UNKNOWN
    }

    /**
     * 获取脉搏分类枚举
     * @return PulseCategory枚举
     */
    fun getPulseCategoryEnum(): PulseCategory {
        return PulseCategory.entries.find { it.code == pulseCategory } ?: PulseCategory.UNKNOWN
    }

    /**
     * 判断是否为正常血压
     * @return 是否正常
     */
    fun isNormalBloodPressure(): Boolean {
        return getBloodPressureCategoryEnum() == BloodPressureCategory.NORMAL
    }

    /**
     * 判断是否为正常脉搏
     * @return 是否正常
     */
    fun isNormalPulse(): Boolean {
        return getPulseCategoryEnum() == PulseCategory.NORMAL
    }

    /**
     * 判断是否为高血压
     * @return 是否高血压
     */
    fun isHypertension(): Boolean {
        val category = getBloodPressureCategoryEnum()
        return category == BloodPressureCategory.HIGH_STAGE_1 ||
               category == BloodPressureCategory.HIGH_STAGE_2 ||
               category == BloodPressureCategory.HYPERTENSIVE_CRISIS
    }

    /**
     * 获取平均动脉压 (MAP)
     * MAP = 舒张压 + 1/3(收缩压 - 舒张压)
     * @return 平均动脉压
     */
    fun getMeanArterialPressure(): Double {
        return diastolicPressure + (systolicPressure - diastolicPressure) / 3.0
    }

    /**
     * 获取脉压差
     * 脉压差 = 收缩压 - 舒张压
     * @return 脉压差
     */
    fun getPulsePressure(): Int {
        return systolicPressure - diastolicPressure
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
         * 创建血压记录的工厂方法
         * 自动计算血压分类和脉搏分类
         */
        fun create(
            recordTime: Date,
            systolicPressure: Int,
            diastolicPressure: Int,
            pulseRate: Int,
            tagIds: List<Long>? = null,
            showInChart: Boolean = true,
            ext1: String? = null,
            ext2: String? = null,
            ext3: String? = null
        ): BloodPressureRecord {
            val bpCategory = BloodPressureCategory.fromBloodPressure(systolicPressure, diastolicPressure)
            val pulseCategory = PulseCategory.fromPulseRate(pulseRate)

            val tagIdsString = tagIds?.joinToString(",")
            return BloodPressureRecord(
                recordTime = recordTime,
                systolicPressure = systolicPressure,
                diastolicPressure = diastolicPressure,
                pulseRate = pulseRate,
                bpCategory = bpCategory.code,
                pulseCategory = pulseCategory.code,
                tagIds = tagIdsString,
                showInChart = showInChart,
                ext1 = ext1,
                ext2 = ext2,
                ext3 = ext3
            )
        }
    }
}