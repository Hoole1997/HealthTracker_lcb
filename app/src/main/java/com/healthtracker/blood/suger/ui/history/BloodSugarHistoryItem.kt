package com.healthtracker.blood.suger.ui.history

import android.R.attr.level
import android.content.Context
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.BloodSugarLevel
import com.healthtracker.blood.suger.enum.getStatusStringRes
import java.text.DecimalFormat
import java.util.Date

/**
 * 血糖历史记录项
 * 包装BloodSugarRecord并实现统一的显示接口
 */
class BloodSugarHistoryItem(private val record: BloodSugarRecord) : HistoryRecordItem() {
    
    private val decimalFormat = DecimalFormat("#.#")
    
    override fun getId(): Long = record.id
    
    override fun getRecordTime(): Date = record.recordTime
    
    override fun getPrimaryValue(): String {
        return record.getFormattedDisplayValue()
    }
    
    override fun getSecondaryValue(): String? {
        // 血糖记录没有次要数值
        return null
    }
    
    override fun getUnit(): String {
        return record.getSelectedUnitEnum().displayName
    }
    
    override fun getLevel(context: Context): String {

        return when (getLevel()) {
            BloodSugarLevel.LOW -> context.getString(R.string.blood_sugar_level_low)
            BloodSugarLevel.NORMAL -> context.getString(R.string.blood_sugar_level_normal)
            BloodSugarLevel.PREDIABETES -> context.getString(R.string.blood_sugar_level_prediabetes)
            BloodSugarLevel.DIABETES -> context.getString(R.string.blood_sugar_level_diabetes)
        }
    }
    
    override fun getStatus(context: Context): String {
        val status = BloodSugarStatus.fromStatusType(record.satus)
        val statusRes = getStatusStringRes(status.statusType)
        return context.getString(statusRes)
    }

    private fun getLevel(): BloodSugarLevel {
        val status = BloodSugarStatus.fromStatusType(record.satus)
        val unit = record.getSelectedUnitEnum()
        val level = status.getBloodSugarLevel(record.getDisplayGlucoseValue().toFloat(), unit)
        return level
    }

    override fun getLeveColorRes() = getLevel().colorRes

    override fun getRecordType(): RecordType {
        return RecordType.BLOOD_SUGAR
    }
    
    /**
     * 获取原始的BloodSugarRecord对象
     * 用于需要访问完整数据的场景
     */
    fun getOriginalRecord(): BloodSugarRecord = record
}