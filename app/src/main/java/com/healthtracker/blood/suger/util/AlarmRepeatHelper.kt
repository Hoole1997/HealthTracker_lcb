package com.healthtracker.blood.suger.util

import android.content.Context
import com.healthtracker.blood.suger.R

/**
 * 闹钟重复模式工具类
 * 提供重复模式的常量定义、描述获取和模式检查等功能
 */
object AlarmRepeatHelper {
    
    // 重复模式常量
    const val REPEAT_ONCE = 0       // 单次
    
    // 使用WeekDay枚举计算的重复模式常量
    val REPEAT_DAILY by lazy { 
        createRepeatFlagFromWeekDays(WeekDay.getAllDays())
    }
    
    val REPEAT_WEEKDAYS by lazy { 
        createRepeatFlagFromWeekDays(WeekDay.getWeekdays())
    }
    
    val REPEAT_WEEKEND by lazy { 
        createRepeatFlagFromWeekDays(WeekDay.getWeekends())
    }
    
    /**
     * 获取重复模式描述
     * @param context Android上下文
     * @param repeatFlag 重复标志位
     * @return 重复模式的文字描述
     */
    fun getRepeatDescription(context: Context, repeatFlag: Int): String {
        if (!isRepeating(repeatFlag)) {
            return context.getString(R.string.ht_once)
        }
        
        val selectedWeekDays = getSelectedWeekDays(repeatFlag)
        val dayNames = context.resources.getStringArray(R.array.ht_week_full)
        
        return when {
            selectedWeekDays.size == 7 -> context.getString(R.string.ht_everyday)
            isWeekdays(repeatFlag) -> context.getString(R.string.ht_every_weekday)
            isWeekend(repeatFlag) -> context.getString(R.string.ht_every_weekend)
            else -> {
                val dayLabels = selectedWeekDays.map { dayNames[it.index] }
                dayLabels.joinToString(", ")
            }
        }
    }
    
    /**
     * 获取简短的重复模式描述
     * @param context Android上下文
     * @param repeatFlag 重复标志位
     * @return 简短的重复模式描述
     */
    fun getShortRepeatDescription(context: Context, repeatFlag: Int): String {
        if (!isRepeating(repeatFlag)) {
            return context.getString(R.string.ht_once)
        }
        
        val selectedWeekDays = getSelectedWeekDays(repeatFlag)
        val dayNames = context.resources.getStringArray(R.array.ht_week_simple)
        
        return when {
            selectedWeekDays.size == 7 -> context.getString(R.string.ht_everyday)
            isWeekdays(repeatFlag) -> context.getString(R.string.ht_every_weekday)
            isWeekend(repeatFlag) -> context.getString(R.string.ht_every_weekend)
            else -> {
                val dayLabels = selectedWeekDays.map { dayNames[it.index] }
                dayLabels.joinToString(", ")
            }
        }
    }
    
    /**
     * 检查是否为重复闹钟
     * @param repeatFlag 重复标志位
     * @return true表示重复，false表示单次
     */
    fun isRepeating(repeatFlag: Int): Boolean {
        return repeatFlag > 0
    }
    
    /**
     * 检查是否为工作日模式
     * @param repeatFlag 重复标志位
     * @return true表示工作日模式
     */
    fun isWeekdays(repeatFlag: Int): Boolean {
        return repeatFlag == REPEAT_WEEKDAYS
    }
    
    /**
     * 检查是否为周末模式
     * @param repeatFlag 重复标志位
     * @return true表示周末模式
     */
    fun isWeekend(repeatFlag: Int): Boolean {
        return repeatFlag == REPEAT_WEEKEND
    }
    
    /**
     * 检查是否为每天模式
     * @param repeatFlag 重复标志位
     * @return true表示每天模式
     */
    fun isDaily(repeatFlag: Int): Boolean {
        return repeatFlag == REPEAT_DAILY
    }
    
    /**
     * 检查指定星期是否应该响铃
     * @param repeatFlag 重复标志位
     * @param dayOfWeek 星期几 (1=周一, 2=周二, ..., 7=周日)
     * @return true表示应该响铃
     */
    fun shouldRingOnDay(repeatFlag: Int, dayOfWeek: Int): Boolean {
        if (!isRepeating(repeatFlag)) return false
        
        val weekDay = WeekDay.fromDayOfWeek(dayOfWeek) ?: return false
        return (repeatFlag and weekDay.getMask()) != 0
    }
    
    /**
     * 检查指定星期枚举是否应该响铃
     * @param repeatFlag 重复标志位
     * @param weekDay 星期枚举
     * @return true表示应该响铃
     */
    fun shouldRingOnWeekDay(repeatFlag: Int, weekDay: WeekDay): Boolean {
        if (!isRepeating(repeatFlag)) return false
        return (repeatFlag and weekDay.getMask()) != 0
    }
    
    /**
     * 获取选中的星期枚举列表
     * @param repeatFlag 重复标志位
     * @return 选中的星期枚举列表
     */
    fun getSelectedWeekDays(repeatFlag: Int): List<WeekDay> {
        return WeekDay.getAllDays().filter { weekDay ->
            (repeatFlag and weekDay.getMask()) != 0
        }
    }
    
    /**
     * 从星期枚举列表创建重复标志位
     * @param selectedWeekDays 选中的星期枚举列表
     * @return 重复标志位
     */
    fun createRepeatFlagFromWeekDays(selectedWeekDays: List<WeekDay>): Int {
        var repeatFlag = 0
        for (weekDay in selectedWeekDays) {
            repeatFlag = repeatFlag or weekDay.getMask()
        }
        return repeatFlag
    }
    
    /**
     * 切换指定星期的状态
     * @param repeatFlag 当前重复标志位
     * @param dayOfWeek 星期几 (1=周一, 2=周二, ..., 7=周日)
     * @return 新的重复标志位
     */
    fun toggleDay(repeatFlag: Int, dayOfWeek: Int): Int {
        val weekDay = WeekDay.fromDayOfWeek(dayOfWeek) ?: return repeatFlag
        return repeatFlag xor weekDay.getMask()
    }
    
    /**
     * 切换指定星期枚举的状态
     * @param repeatFlag 当前重复标志位
     * @param weekDay 星期枚举
     * @return 新的重复标志位
     */
    fun toggleWeekDay(repeatFlag: Int, weekDay: WeekDay): Int {
        return repeatFlag xor weekDay.getMask()
    }
    
    /**
     * 获取预定义的重复模式列表
     * @param context Android上下文
     * @return 重复模式列表，包含标志位和描述
     */
    fun getPredefinedPatterns(context: Context): List<Pair<Int, String>> {
        return listOf(
            REPEAT_ONCE to context.getString(R.string.ht_once),
            REPEAT_DAILY to context.getString(R.string.ht_everyday),
            REPEAT_WEEKDAYS to context.getString(R.string.ht_every_weekday),
            REPEAT_WEEKEND to context.getString(R.string.ht_every_weekend)
        )
    }
}