package com.daily.health.manager.data.entity

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daily.health.manager.util.AlarmRepeatHelper
import com.daily.health.manager.data.utils.DateTimeUtils
import java.util.Date

/**
 * 闹钟记录数据实体
 * 对应数据表：alarm_records
 */
@Entity(tableName = "t04")
data class LocalEntity04(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /**
     * 闹钟类型
     * 0: 普通闹钟, 1: 健康提醒, 2: 用药提醒, 3: 运动提醒等
     */
    @ColumnInfo(name = "c02")
    val type: Int,

    /**
     * 小时 (0-23)
     */
    @ColumnInfo(name = "c03")
    val hour: Int,

    /**
     * 分钟 (0-59)
     */
    @ColumnInfo(name = "c04")
    val minute: Int,

    /**
     * 重复标志
     * 使用位运算表示星期重复模式
     * 例如: 127表示每天, 31表示工作日, 96表示周末
     */
    @ColumnInfo(name = "c05")
    val repeatFlag: Int,

    /**
     * 铃声ID
     * 关联系统铃声或自定义铃声的ID
     */
    @ColumnInfo(name = "c06")
    val soundId: Long,

    /**
     * 启用状态
     * true: 启用, false: 禁用
     */
    @ColumnInfo(name = "c07")
    val isEnabled: Boolean = true,

    /**
     * 振动时长 (毫秒)
     * 0表示不振动
     */
    @ColumnInfo(name = "c08")
    val vibrateTime: Long = 0,

    /**
     * 删除标记
     * true: 已删除, false: 正常
     */
    @ColumnInfo(name = "c09")
    val isDeleted: Boolean = false,

    /**
     * 其他信息
     * 可用于存储闹钟标签、备注等
     */
    @ColumnInfo(name = "c10")
    val other: String? = null,

    /**
     * 整型扩展字段1
     * 可用于存储优先级、重要程度等
     */
    @ColumnInfo(name = "c11")
    val intExt1: Int? = null,

    /**
     * 整型扩展字段2
     * 可用于存储提前提醒时间等
     */
    @ColumnInfo(name = "c12")
    val intExt2: Int? = null,

    /**
     * 整型扩展字段3
     * 预留扩展字段
     */
    @ColumnInfo(name = "c13")
    val intExt3: Int? = null,

    /**
     * 浮点型扩展字段1
     * 可用于存储音量大小等
     */
    @ColumnInfo(name = "c14")
    val floatExt1: Float? = null,

    /**
     * 浮点型扩展字段2
     * 预留扩展字段
     */
    @ColumnInfo(name = "c15")
    val floatExt2: Float? = null,

    /**
     * 长整型扩展字段1
     * 可用于存储创建时间戳等
     */
    @ColumnInfo(name = "c16")
    val longExt1: Long? = null,

    /**
     * 长整型扩展字段2
     * 可用于存储最后触发时间等
     */
    @ColumnInfo(name = "c17")
    val longExt2: Long? = null,

    /**
     * 文本扩展字段1
     * 可用于存储闹钟标签、名称等
     */
    @ColumnInfo(name = "c18")
    val textExt1: String? = null,

    /**
     * 文本扩展字段2
     * 可用于存储备注信息等
     */
    @ColumnInfo(name = "c19")
    val textExt2: String? = null,

    /**
     * 文本扩展字段3
     * 预留扩展字段
     */
    @ColumnInfo(name = "c20")
    val textExt3: String? = null,

    /**
     * 更新时间戳
     * 记录数据最后修改的时间，以毫秒为单位
     * 在创建和更新时自动维护
     */
    @ColumnInfo(name = "c21")
    val updatedAt: Long = System.currentTimeMillis()
) {

    /**
     * 获取闹钟显示时间
     * @return 格式化的时间字符串 (HH:mm)
     */
    fun getFormattedTime(): String {
        return DateTimeUtils.formatTimeComponents(hour, minute)
    }

    /**
     * 检查是否为重复闹钟
     * @return true: 重复闹钟, false: 单次闹钟
     */
    fun isRepeating(): Boolean {
        return AlarmRepeatHelper.isRepeating(repeatFlag)
    }

    /**
     * 检查指定星期几是否需要响铃
     * @param dayOfWeek 星期几 (1=周一, 7=周日)
     * @return true: 需要响铃, false: 不需要响铃
     */
    fun shouldRingOnDay(dayOfWeek: Int): Boolean {
        return AlarmRepeatHelper.shouldRingOnDay(repeatFlag, dayOfWeek)
    }

    /**
     * 获取重复模式描述
     * @param context Android上下文，用于获取字符串资源
     * @return 重复模式的文字描述
     */
    fun getRepeatDescription(context: Context): String {
        return AlarmRepeatHelper.getRepeatDescription(context, repeatFlag)
    }

    /**
     * 获取简短的重复模式描述
     * @param context Android上下文，用于获取字符串资源
     * @return 简短的重复模式描述
     */
    fun getShortRepeatDescription(context: Context): String {
        return AlarmRepeatHelper.getShortRepeatDescription(context, repeatFlag)
    }

    /**
     * 检查是否有振动
     * @return true: 有振动, false: 无振动
     */
    fun hasVibration(): Boolean {
        return vibrateTime > 0
    }

    /**
     * 创建更新后的记录副本
     * 自动更新updatedAt字段为当前时间戳
     * @return 更新了时间戳的新AlarmRecord实例
     */
    fun withUpdatedTimestamp(): AlarmRecord {
        return this.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * 获取格式化的更新时间
     * @return 格式化的更新时间字符串
     */
    fun getFormattedUpdatedTime(): String {
        val date = Date(updatedAt)
        return DateTimeUtils.formatDateTimeWithSeconds(date)
    }

    /**
     * 检查是否为服药提醒类型
     * @return true: 服药提醒, false: 其他类型
     */
    fun isMedicationReminder(): Boolean {
        return type == TYPE_MEDICATION
    }

    /**
     * 获取药物ID（仅服药提醒有效）
     * @return 药物ID，如果不是服药提醒则返回null
     */
    fun getMedicineId(): Long? {
        return if (isMedicationReminder()) longExt1 else null
    }

    companion object {
        /**
         * 闹钟类型常量
         */
        const val TYPE_BLOOD_SUGAR = 0     // 血糖测量提醒
        const val TYPE_BLOOD_PRESSURE = 1  // 血压测量提醒
        const val TYPE_MEDICATION = 2      // 服药提醒
        const val TYPE_HYDRATION = 3       // 饮水提醒
        const val TYPE_HEART_RATE = 4      // 心率提醒
        const val TYPE_BMI = 5             // BMI提醒
        const val TYPE_CHOLESTEROL = 6     // 胆固醇提醒

        /**
         * 重复模式常量
         */
        const val REPEAT_ONCE = 0       // 单次
        const val REPEAT_DAILY = 127    // 每天 (1111111)
        const val REPEAT_WEEKDAYS = 31  // 工作日 (0011111)
        const val REPEAT_WEEKEND = 96   // 周末 (1100000)

        /**
         * 创建测量提醒闹钟记录
         * @param type 提醒类型（血糖或血压）
         * @param hour 小时
         * @param minute 分钟
         * @param repeatFlag 重复标志
         * @param soundId 铃声ID
         * @param isEnabled 是否启用
         * @param vibrateTime 振动时长
         * @param other 其他信息
         * @return AlarmRecord实例
         */
        fun create(
            type: Int,
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY,
            soundId: Long = 0,
            isEnabled: Boolean = true,
            vibrateTime: Long = 0,
            other: String? = null
        ): AlarmRecord {
            val currentTime = System.currentTimeMillis()
            return LocalEntity04(
                type = type,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                soundId = soundId,
                isEnabled = isEnabled,
                vibrateTime = vibrateTime,
                other = other,
                longExt1 = null, // 创建时间
                longExt2 = null, // 最后触发时间
                updatedAt = currentTime // 更新时间
            )
        }

        /**
         * 创建血糖测量提醒
         * @param hour 小时
         * @param minute 分钟
         * @param repeatFlag 重复标志
         * @return AlarmRecord实例
         */
        fun createBloodSugarReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_BLOOD_SUGAR,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 1000 // 默认振动1秒
            )
        }

        /**
         * 创建血压测量提醒
         * @param hour 小时
         * @param minute 分钟
         * @param repeatFlag 重复标志
         * @return AlarmRecord实例
         */
        fun createBloodPressureReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_BLOOD_PRESSURE,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 2000 // 默认振动2秒
            )
        }

        /**
         * 创建服药提醒
         * @param hour 小时
         * @param minute 分钟
         * @param repeatFlag 重复标志
         * @return 服药提醒闹钟记录
         */
        fun createMedicationReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_MEDICATION,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 3000 // 默认振动3秒
            )
        }

        /**
         * 创建饮水提醒
         * @param hour 小时
         * @param minute 分钟
         * @param repeatFlag 重复标志
         * @return 饮水提醒闹钟记录
         */
        fun createHydrationReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_HYDRATION,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 1500 // 默认振动1.5秒
            )
        }

        /**
         * 创建心率测量提醒
         */
        fun createHeartRateReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_HEART_RATE,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 1000
            )
        }

        /**
         * 创建BMI测量提醒
         */
        fun createBmiReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_BMI,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 1000
            )
        }

        /**
         * 创建胆固醇测量提醒
         */
        fun createCholesterolReminder(
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            return create(
                type = TYPE_CHOLESTEROL,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                vibrateTime = 1000
            )
        }

        /**
         * 创建服药提醒闹钟（简化版）
         * @param medicineId 药物提醒ID，用于关联查询药物信息
         * @param hour 小时
         * @param minute 分钟
         * @param repeatFlag 重复标志
         * @return 仅包含基本调度信息的服药提醒闹钟记录
         */
        fun createMedicationAlarm(
            medicineId: Long,
            hour: Int,
            minute: Int,
            repeatFlag: Int = REPEAT_DAILY
        ): AlarmRecord {
            val currentTime = System.currentTimeMillis()
            return LocalEntity04(
                type = TYPE_MEDICATION,
                hour = hour,
                minute = minute,
                repeatFlag = repeatFlag,
                soundId = 0,
                isEnabled = true,
                vibrateTime = 3000,
                other = "Medication Reminder",
                longExt1 = medicineId,     // 存储药物提醒ID
                longExt2 = null,           // 最后触发时间
                textExt1 = null,           // 不再存储药物名称
                textExt2 = null,           // 不再存储剂量
                textExt3 = null,           // 不再存储备注
                updatedAt = currentTime
            )
        }
    }
}

typealias AlarmRecord = LocalEntity04