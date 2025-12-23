package com.daily.health.manager.util

/**
 * 星期枚举类
 * 提供星期的索引值和基本功能
 */
enum class WeekDay(
    val index: Int
) {
    MONDAY(0),
    TUESDAY(1),
    WEDNESDAY(2),
    THURSDAY(3),
    FRIDAY(4),
    SATURDAY(5),
    SUNDAY(6);

    /**
     * 获取位掩码
     * @return 对应的位掩码值
     */
    fun getMask(): Int {
        return 1 shl index
    }

    companion object {
        /**
         * 根据索引获取星期枚举
         * @param index 星期索引 (0=周一, 1=周二, ..., 6=周日)
         * @return 对应的星期枚举，如果索引无效则返回null
         */
        fun fromIndex(index: Int): WeekDay? {
            return values().find { it.index == index }
        }

        /**
         * 根据Calendar的星期值获取星期枚举
         * @param calendarDay Calendar的星期值 (1=周日, 2=周一, ..., 7=周六)
         * @return 对应的星期枚举，如果值无效则返回null
         */
        fun fromCalendarDay(calendarDay: Int): WeekDay? {
            return when (calendarDay) {
                2 -> MONDAY    // Calendar.MONDAY
                3 -> TUESDAY   // Calendar.TUESDAY
                4 -> WEDNESDAY // Calendar.WEDNESDAY
                5 -> THURSDAY  // Calendar.THURSDAY
                6 -> FRIDAY    // Calendar.FRIDAY
                7 -> SATURDAY  // Calendar.SATURDAY
                1 -> SUNDAY    // Calendar.SUNDAY
                else -> null
            }
        }

        /**
         * 根据自定义星期值获取星期枚举
         * @param dayOfWeek 自定义星期值 (1=周一, 2=周二, ..., 7=周日)
         * @return 对应的星期枚举，如果值无效则返回null
         */
        fun fromDayOfWeek(dayOfWeek: Int): WeekDay? {
            return when (dayOfWeek) {
                1 -> MONDAY
                2 -> TUESDAY
                3 -> WEDNESDAY
                4 -> THURSDAY
                5 -> FRIDAY
                6 -> SATURDAY
                7 -> SUNDAY
                else -> null
            }
        }

        /**
         * 获取所有工作日
         * @return 工作日列表
         */
        fun getWeekdays(): List<WeekDay> {
            return listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        }

        /**
         * 获取所有周末
         * @return 周末列表
         */
        fun getWeekends(): List<WeekDay> {
            return listOf(SATURDAY, SUNDAY)
        }

        /**
         * 获取所有星期
         * @return 所有星期列表
         */
        fun getAllDays(): List<WeekDay> {
            return values().toList()
        }
    }
}