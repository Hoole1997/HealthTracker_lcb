package com.daily.health.manager.ui.history

import android.content.Context
import java.util.Date

/**
 * 历史记录项基类
 * 为血糖和血压记录提供统一的显示接口
 */
abstract class HistoryRecordItem {
    
    /**
     * 获取记录的唯一标识
     */
    abstract fun getId(): Long
    
    /**
     * 获取记录时间
     */
    abstract fun getRecordTime(): Date
    
    /**
     * 获取主要数值显示文本（如血糖值或收缩压）
     */
    abstract fun getPrimaryValue(): String
    
    /**
     * 获取次要数值显示文本（血糖为空，血压为舒张压）
     */
    abstract fun getSecondaryValue(): String?
    
    /**
     * 获取单位显示文本
     */
    abstract fun getUnit(): String
    
    /**
     * 获取等级显示文本（如"正常"、"偏高"等）
     */
    abstract fun getLevel(context: Context): String
    
    /**
     * 获取状态显示文本（显示在tvStatus中）
     */
    abstract fun getStatus(context: Context): String?


    abstract fun getLeveColorRes(): Int
    
    /**
     * 获取记录类型（用于区分血糖和血压）
     */
    abstract fun getRecordType(): RecordType
    
    /**
     * 记录类型枚举
     */
    enum class RecordType {
        BLOOD_SUGAR,     // 血糖记录
        BLOOD_PRESSURE,  // 血压记录
        CHOLESTEROL,     // 胆固醇记录
        HEART_RATE,      // 心率记录
        BMI_RECORD       // BMI记录
    }
}