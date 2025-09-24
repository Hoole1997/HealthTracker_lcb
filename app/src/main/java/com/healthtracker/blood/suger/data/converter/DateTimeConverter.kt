package com.healthtracker.blood.suger.data.converter

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*
import com.healthtracker.blood.suger.data.utils.DateTimeUtils

/**
 * Room数据库类型转换器
 * 兼容API 24+，使用Date和SimpleDateFormat进行时间处理
 */
class DateTimeConverter {

    companion object {
        /**
         * 日期时间格式化器
         * 使用ISO 8601格式: yyyy-MM-dd HH:mm:ss
         */
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    /**
     * 将Date转换为Long时间戳存储到数据库
     * @param date Date对象
     * @return 时间戳，如果为null则返回null
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    /**
     * 将数据库中的Long时间戳转换为Date
     * @param timestamp 时间戳
     * @return Date对象，如果时间戳为null则返回null
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    /**
     * 将Date转换为格式化字符串（用于调试和显示）
     * @param date Date对象
     * @return 格式化的日期时间字符串
     */
    fun formatDate(date: Date?): String? {
        return date?.let { DateTimeUtils.formatDateTimeWithSeconds(it) }
    }

    /**
     * 将格式化字符串解析为Date（用于测试和调试）
     * @param dateTimeString 日期时间字符串
     * @return Date对象，解析失败则返回null
     */
    fun parseDate(dateTimeString: String?): Date? {
        return if (dateTimeString.isNullOrBlank()) {
            null
        } else {
            try {
                dateFormat.parse(dateTimeString)
            } catch (e: Exception) {
                // 如果解析失败，记录日志并返回null
                println("Failed to parse datetime: $dateTimeString, error: ${e.message}")
                null
            }
        }
    }
}