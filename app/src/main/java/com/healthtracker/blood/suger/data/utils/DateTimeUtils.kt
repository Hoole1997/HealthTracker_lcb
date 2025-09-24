package com.healthtracker.blood.suger.data.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * 时间处理工具类
 * 兼容API 24+，支持多滚轮时间选择器
 */
object DateTimeUtils {

    /**
     * 从多滚轮选择器创建Date对象
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)
     * @param hour 小时 (0-23)
     * @param minute 分钟 (0-59)
     * @return Date对象
     */
    fun createDate(year: Int, month: Int, day: Int, hour: Int, minute: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day, hour, minute, 0) // 注意月份从0开始
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * 获取当前时间
     * @return 当前Date对象
     */
    fun now(): Date = Date()

    /**
     * 格式化显示完整日期时间
     * @param date Date对象
     * @return 格式化字符串，如 "2024-01-15 14:30"
     */
    fun formatDateTime(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 格式化显示完整日期时间（包含秒）
     * @param date Date对象
     * @return 格式化字符串，如 "2024-01-15 14:30:25"
     */
    fun formatDateTimeWithSeconds(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 格式化显示日期
     * @param date Date对象
     * @return 格式化字符串，如 "2024-01-15"
     */
    fun formatDate(date: Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 格式化显示时间
     * @param date Date对象
     * @return 格式化字符串，如 "14:30"
     */
    fun formatTime(date: Date): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 格式化显示中文日期时间
     * @param date Date对象
     * @return 格式化字符串，如 "2024年1月15日 14:30"
     */
    fun formatChineseDateTime(date: Date): String {
        val format = SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
        return format.format(date)
    }

    /**
     * 格式化显示月份年份（英文简写）
     * @param date Date对象
     * @return 格式化字符串，如 "Sep.2025"
     */
    fun formatMonthYear(date: Date): String {
        val format = SimpleDateFormat("MMM.yyyy", Locale.ENGLISH)
        return format.format(date)
    }

    /**
     * 获取当前月份年份（英文简写）
     * @return 格式化字符串，如 "Sep.2025"
     */
    fun getCurrentMonthYear(): String {
        return formatMonthYear(now())
    }

    /**
     * 获取日期范围
     * @param date 基准日期
     * @param days 天数（向前推算）
     * @return Pair<开始日期, 结束日期>
     */
    fun getDateRange(date: Date, days: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // 开始时间：N天前的0点
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time

        // 结束时间：当前时间
        val endDate = date

        return Pair(startDate, endDate)
    }

    /**
     * 获取今天的开始和结束时间
     * @return Pair<今天0点, 今天23:59:59>
     */
    fun getTodayRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()

        // 今天0点
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        // 今天23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.time

        return Pair(startOfDay, endOfDay)
    }

    /**
     * 获取本周的开始和结束时间
     * @return Pair<本周一0点, 本周日23:59:59>
     */
    fun getThisWeekRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()

        // 设置为本周一0点
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.time

        // 设置为本周日23:59:59
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.time

        return Pair(startOfWeek, endOfWeek)
    }

    /**
     * 从Date提取年月日时分用于设置滚轮默认值
     * @param date Date对象
     * @return DateComponents数据类
     */
    fun extractDateComponents(date: Date): DateComponents {
        val calendar = Calendar.getInstance()
        calendar.time = date
        return DateComponents(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1, // Calendar月份从0开始，转换为1-12
            day = calendar.get(Calendar.DAY_OF_MONTH),
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE)
        )
    }

    /**
     * 比较两个日期是否为同一天
     * @param date1 第一个日期
     * @param date2 第二个日期
     * @return 是否为同一天
     */
    fun isSameDay(date1: Date, date2: Date): Boolean {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar1.time = date1
        calendar2.time = date2

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 计算两个日期之间的天数差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差
     */
    fun daysBetween(startDate: Date, endDate: Date): Long {
        val diffInMillis = endDate.time - startDate.time
        return diffInMillis / (24 * 60 * 60 * 1000)
    }

    /**
     * 添加天数到指定日期
     * @param date 基准日期
     * @param days 要添加的天数（可以为负数）
     * @return 新的Date对象
     */
    fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.time
    }

    /**
     * 添加小时到指定日期
     * @param date 基准日期
     * @param hours 要添加的小时数（可以为负数）
     * @return 新的Date对象
     */
    fun addHours(date: Date, hours: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.HOUR_OF_DAY, hours)
        return calendar.time
    }
}

/**
 * 日期组件数据类
 * 用于存储从Date中提取的年月日时分信息
 */
data class DateComponents(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
) {
    /**
     * 转换为Date对象
     */
    fun toDate(): Date {
        return DateTimeUtils.createDate(year, month, day, hour, minute)
    }

    /**
     * 格式化显示
     */
    override fun toString(): String {
        return String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute)
    }
}