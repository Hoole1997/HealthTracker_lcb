package com.daily.health.manager.ui.history

import android.content.Context
import com.daily.health.manager.data.entity.CholesterolRecord
import com.daily.health.manager.data.enums.CholesterolLevel
import java.text.DecimalFormat
import java.util.Date

/**
 * 胆固醇历史记录项
 * 包装 CholesterolRecord 并实现统一的显示接口
 */
class CholesterolHistoryItem(private val record: CholesterolRecord) : HistoryRecordItem() {

    private val decimalFormat = DecimalFormat("#.##")

    override fun getId(): Long = record.id

    override fun getRecordTime(): Date = record.recordTime

    override fun getPrimaryValue(): String {
        // 胆固醇主值为 HDL
        return record.hdl.toString()
    }

    override fun getSecondaryValue(): String? {
        // 胆固醇在自定义布局中展示，返回 null
        return null
    }

    override fun getUnit(): String {
        return "mg/dL"
    }

    override fun getLevel(context: Context): String {
        val level = getCholesterolLevel()
        return context.getString(level.labelRes)
    }

    override fun getStatus(context: Context): String? {
        // 胆固醇没有传统的状态字段
        return null
    }

    override fun getLeveColorRes(): Int {
        val level = getCholesterolLevel()
        return level.colorRes
    }

    override fun getRecordType(): RecordType {
        return RecordType.CHOLESTEROL
    }

    /**
     * 获取胆固醇风险等级
     */
    private fun getCholesterolLevel(): CholesterolLevel {
        return CholesterolLevel.fromMetrics(
            totalCholesterol = record.tc,
            nonHdl = record.nonHdl,
            ldl = record.ldl.toFloat(),
            hdl = record.hdl.toFloat()
        )
    }

    /**
     * 获取原始的 CholesterolRecord 对象
     */
    fun getOriginalRecord(): CholesterolRecord = record

    // ========== 胆固醇专用方法（用于自定义布局） ==========

    fun getHdlValue(): String = record.hdl.toString()
    fun getLdlValue(): String = record.ldl.toString()
    fun getTgValue(): String = record.triglyceride.toString()
    fun getNonHdlValue(): String = decimalFormat.format(record.nonHdl)
    fun getTcHdlRatioValue(): String = decimalFormat.format(record.tcHdlRatio)
    fun getLdlHdlRatioValue(): String = decimalFormat.format(record.ldlHdlRatio)
}
