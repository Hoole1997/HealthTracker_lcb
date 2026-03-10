package com.daily.health.manager.face.history

import android.content.Context
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.BloodPressureRecord
import com.daily.health.manager.data.enums.BloodPressureCategory
import java.util.Date

/**
 * 血压历史记录项
 * 包装BloodPressureRecord并实现统一的显示接口
 */
class BloodPressureHistoryItem(private val record: BloodPressureRecord) : HistoryRecordItem() {
    
    override fun getId(): Long = record.id
    
    override fun getRecordTime(): Date = record.recordTime
    
    override fun getPrimaryValue(): String {
        // 收缩压作为主要数值
        return record.systolicPressure.toString()
    }
    
    override fun getSecondaryValue(): String {
        // 舒张压作为次要数值
        return record.diastolicPressure.toString()
    }
    
    override fun getUnit(): String {
        return App.INSTANCE.getString(R.string.tr_mmHg)
    }
    
    override fun getLevel(context: Context): String {
        val category = record.getBloodPressureCategoryEnum()
        return when (category) {
            BloodPressureCategory.LOW -> context.getString(R.string.tr_blood_pressure_level_low)
            BloodPressureCategory.NORMAL -> context.getString(R.string.tr_blood_pressure_level_normal)
            BloodPressureCategory.ELEVATED -> context.getString(R.string.tr_blood_pressure_level_elevated)
            BloodPressureCategory.HIGH_STAGE_1 -> context.getString(R.string.tr_blood_pressure_level_high_stage_1)
            BloodPressureCategory.HIGH_STAGE_2 -> context.getString(R.string.tr_blood_pressure_level_high_stage_2)
            BloodPressureCategory.HYPERTENSIVE_CRISIS -> context.getString(R.string.tr_blood_pressure_level_hypertensive_crisis)
            BloodPressureCategory.UNKNOWN -> "UnKnow"
        }
    }
    
    override fun getStatus(context: Context) = "${record.pulseRate} BPM"
    override fun getLeveColorRes() = record.getBloodPressureCategoryEnum().colorRes

    override fun getRecordType(): RecordType {
        return RecordType.BLOOD_PRESSURE
    }
    
    /**
     * 获取原始的BloodPressureRecord对象
     * 用于需要访问完整数据的场景
     */
    fun getOriginalRecord(): BloodPressureRecord = record
}