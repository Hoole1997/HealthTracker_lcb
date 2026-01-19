package com.daily.health.manager.face.history

import android.content.Context
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.BmiRecord
import com.daily.health.manager.data.enums.BMIEnum
import com.daily.health.manager.App
import com.daily.health.manager.data.enums.BmiUnit
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import java.util.Date

/**
 * BMI 历史记录项
 * 包装 BmiRecord 并实现统一的显示接口
 */
class BmiHistoryItem(private val record: BmiRecord) : HistoryRecordItem() {

    override fun getId(): Long = record.id

    override fun getRecordTime(): Date = record.recordTime

    override fun getPrimaryValue(): String {
        // 计算并显示 BMI 值（与详情页格式一致）
        val bmi = calculateBmi()
        return NumberFormatter.formatNumber(bmi.toDouble(), LanguageUtils.getAppLocale(App.INSTANCE), 1)
    }

    override fun getSecondaryValue(): String? {
        // BMI 记录没有次要数值
        return null
    }

    override fun getUnit(): String {
        return "BMI"
    }

    override fun getLevel(context: Context): String {
        val bmi = calculateBmi()
        val category = BMIEnum.fromBmi(bmi)
        return context.getString(category.statusTextRes)
    }

    override fun getStatus(context: Context): String? {
        val heightStr = context.getString(R.string.ht_height)
        val weightStr = context.getString(R.string.ht_weight)

        val heightUnit = BmiUnit.getPreferredHeightUnit()
        val weightUnit = BmiUnit.getPreferredWeightUnit()

        val displayHeight = BmiUnit.formatDisplayHeight(record.heightCm.toFloat(), heightUnit)
        val displayWeight = BmiUnit.formatDisplayWeight(record.weightKg.toFloat(), weightUnit)

        val heightUnitLabel = if (heightUnit == BmiUnit.METRIC) context.getString(
            R.string.ht_unit_cm) else context.getString(R.string.ht_unit_ft_in)
        val weightUnitLabel = if (weightUnit == BmiUnit.METRIC) context.getString(
            R.string.ht_unit_kg) else context.getString(R.string.ht_unit_lb)

        return "$heightStr : $displayHeight$heightUnitLabel  $weightStr : $displayWeight$weightUnitLabel"
    }

    override fun getLeveColorRes(): Int {
        val bmi = calculateBmi()
        val category = BMIEnum.fromBmi(bmi)
        return category.colorRes
    }

    override fun getRecordType(): RecordType {
        return RecordType.BMI_RECORD
    }

    /**
     * 计算 BMI 值
     * BMI = weight(kg) / height(m)^2
     */
    private fun calculateBmi(): Float {
        val heightM = record.heightCm / 100.0 // 转换为米
        return (record.weightKg / (heightM * heightM)).toFloat()
    }

    /**
     * 获取原始的 BmiRecord 对象
     * 用于需要访问完整数据的场景
     */
    fun getOriginalRecord(): BmiRecord = record
}
