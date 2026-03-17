package com.daily.health.manager.face.history

import android.content.Context
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.HeartRateStatus
import java.util.Date

/**
 * 心率历史记录项
 * 包装 HeartRateRecord 并实现统一的显示接口
 */
class HeartRateHistoryItem(private val record: HeartRateRecord) : HistoryRecordItem() {

    override fun getId(): Long = record.id

    override fun getRecordTime(): Date = record.recordTime

    override fun getPrimaryValue(): String {
        // 心率值作为主要数值
        return record.heartRateBpm.toString()
    }

    override fun getSecondaryValue(): String? {
        // 心率记录没有次要数值
        return null
    }

    override fun getUnit(): String {
        return App.INSTANCE.getString(R.string.fc_bpm)
    }

    override fun getLevel(context: Context): String {
        val status = HeartRateStatus.fromHeartRate(record.heartRateBpm)
        return context.getString(status.statusTextRes)
    }

    override fun getStatus(context: Context): String? {
        // 心率不显示状态
        return null
    }

    override fun getLeveColorRes(): Int {
        val status = HeartRateStatus.fromHeartRate(record.heartRateBpm)
        return status.colorRes
    }

    override fun getRecordType(): RecordType {
        return RecordType.HEART_RATE
    }

    /**
     * 获取原始的 HeartRateRecord 对象
     * 用于需要访问完整数据的场景
     */
    fun getOriginalRecord(): HeartRateRecord = record
}
