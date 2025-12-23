package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daily.health.manager.data.utils.DateTimeUtils
import java.util.Date

/**
 * 药物提醒实体 - 一个表解决所有需求
 */
@Entity(tableName = "t05")
data class LocalEntity05(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,

    /**
     * 药物名称
     */
    @ColumnInfo(name = "c02")
    val medicineName: String,

    /**
     * 药物封面图片路径
     */
    @ColumnInfo(name = "c03")
    val medicineCover: String = "",

    /**
     * 开始提醒时间列表（时间戳数组，逗号分隔）
     * 格式: "1758844800000,1758859200000,1758880800000"
     */
    @ColumnInfo(name = "c04")
    val startRemindTimes: String,

    /**
     * 备注信息
     */
    @ColumnInfo(name = "c05")
    val note: String = "",

    /**
     * 是否同步到系统日历
     * 0-不同步，1-同步
     */
    @ColumnInfo(name = "c06")
    val syncCalendar: Int = 0,

    /**
     * 时间相关配置（预留字段）
     */
    @ColumnInfo(name = "c07")
    val time: String = "",

    /**
     * 已服药时间记录（时间戳数组，逗号分隔）
     * 格式: "1758859200000,1758895200000"
     */
    @ColumnInfo(name = "c08")
    val takedTimes: String = "",

    /**
     * 真实提醒时间记录（时间戳数组，逗号分隔）
     * 格式: "1758844800000,1758931200000,1759017600000"
     * 系统实际推送提醒的时间记录
     */
    @ColumnInfo(name = "c09")
    val realRemindTimes: String = "",

    /**
     * 是否启用
     */
    @ColumnInfo(name = "c10")
    val isActive: Boolean = true,

    /**
     * 创建时间
     */
    @ColumnInfo(name = "c11")
    val createdAt: Date = DateTimeUtils.now()
) {



    /**
     * 获取开始提醒时间列表（时间戳转Date）
     */
    fun getStartRemindTimeList(): List<Date> {
        return if (startRemindTimes.isBlank()) emptyList()
        else startRemindTimes.split(TIME_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull {
                try { Date(it.toLong()) }
                catch (e: Exception) { null }
            }
    }

    /**
     * 获取开始提醒时间字符串列表（HH:mm格式）
     */
    fun getStartRemindTimeStrings(): List<String> {
        return getStartRemindTimeList().map { DateTimeUtils.formatTime24H(it) }
    }

    /**
     * 获取已服药时间列表（时间戳转Date）
     */
    fun getTakedTimeList(): List<Date> {
        return if (takedTimes.isBlank()) emptyList()
        else takedTimes.split(TIME_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull {
                try { Date(it.toLong()) }
                catch (e: Exception) { null }
            }
    }

    /**
     * 获取真实提醒时间列表（时间戳转Date）
     */
    fun getRealRemindTimeList(): List<Date> {
        return if (realRemindTimes.isBlank()) emptyList()
        else realRemindTimes.split(TIME_SEPARATOR)
            .filter { it.isNotBlank() }
            .mapNotNull {
                try { Date(it.toLong()) }
                catch (e: Exception) { null }
            }
    }


    /**
     * 添加服药记录
     */
    fun addTakedRecord(takenTime: Date = DateTimeUtils.now()): MedicineReminder {
        val newTimestamp = takenTime.time.toString()
        val updatedTimes = if (takedTimes.isBlank()) {
            newTimestamp
        } else {
            "$takedTimes$TIME_SEPARATOR$newTimestamp"
        }

        return this.copy(takedTimes = updatedTimes)
    }

    /**
     * 添加真实提醒时间记录
     */
    fun addRealRemindTime(remindTime: Date = DateTimeUtils.now()): MedicineReminder {
        val newTimestamp = remindTime.time.toString()
        val updatedTimes = if (realRemindTimes.isBlank()) {
            newTimestamp
        } else {
            "$realRemindTimes$TIME_SEPARATOR$newTimestamp"
        }

        return this.copy(realRemindTimes = updatedTimes)
    }

    /**
     * 是否同步到日历
     */
    fun isSyncToCalendar(): Boolean = syncCalendar == 1

    /**
     * 创建药物提醒的工厂方法
     */
    companion object {

        private const val TIME_SEPARATOR = ","

        /**
         * 创建药物提醒
         * @param medicineName 药物名称
         * @param startRemindTimes 开始提醒时间列表（Date对象）
         * @param medicineCover 药物封面图片路径
         * @param note 备注
         * @param syncCalendar 是否同步日历
         */
        fun create(
            medicineName: String,
            startRemindTimes: List<Date>,
            medicineCover: String = "",
            note: String = "",
            syncCalendar: Boolean = false
        ): MedicineReminder {
            return LocalEntity05(
                medicineName = medicineName.trim(),
                startRemindTimes = startRemindTimes.map { it.time.toString() }.joinToString(TIME_SEPARATOR),
                medicineCover = medicineCover,
                note = note,
                syncCalendar = if (syncCalendar) 1 else 0
            )
        }

        /**
         * 根据时间字符串创建药物提醒
         * @param medicineName 药物名称
         * @param timeStrings 时间字符串列表，格式"HH:mm"
         */
        fun createFromTimeStrings(
            medicineName: String,
            timeStrings: List<String>,
            medicineCover: String = "",
            note: String = "",
            syncCalendar: Boolean = false
        ): MedicineReminder {
            val today = DateTimeUtils.now()
            val todayComponents = DateTimeUtils.extractDateComponents(today)

            val dates = timeStrings.mapNotNull { timeStr ->
                try {
                    val (hour, minute) = DateTimeUtils.parseTimeString(timeStr) ?: return@mapNotNull null
                    
                    // 使用DateTimeUtils创建今天的指定时间
                    DateTimeUtils.createDate(
                        year = todayComponents.year,
                        month = todayComponents.month,
                        day = todayComponents.day,
                        hour = hour,
                        minute = minute
                    )
                } catch (e: Exception) { 
                    null 
                }
            }

            return create(medicineName, dates, medicineCover, note, syncCalendar)
        }

       
    }
}

typealias MedicineReminder = LocalEntity05

/**
 * 常用提醒时间组合
 */
object PresetTimes {
    val ONCE_DAILY = listOf("08:00")
    val TWICE_DAILY = listOf("08:00", "20:00")
    val THREE_TIMES_DAILY = listOf("08:00", "12:00", "18:00")
    val FOUR_TIMES_DAILY = listOf("08:00", "12:00", "18:00", "22:00")
    val FIVE_TIMES_DAILY = listOf("08:00", "12:00", "15:00","18:00", "23:00")
    val SIX_TIMES_DAILY = listOf("07:30", "10:00", "13:30","17:00", "20:30","23:30")
}
