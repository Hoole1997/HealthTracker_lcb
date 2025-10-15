package com.healthtracker.blood.suger.util

import com.healthtracker.blood.suger.data.enums.CholesterolLevel
import kotlin.math.pow
import kotlin.math.round

/**
 * 胆固醇指标计算工具
 * 统一管理总胆固醇、非 HDL 以及比值的计算逻辑
 */
object CholesterolCalculator {

    private const val TG_DIVISOR = 5f

    /**
     * 计算总胆固醇
     * 采用 Friedewald 公式：TC = HDL + LDL + TG / 5
     */
    fun calculateTotalCholesterol(hdl: Float?, ldl: Float?, triglyceride: Float?): Float? {
        if (hdl == null || ldl == null || triglyceride == null) {
            return null
        }
        if (hdl <= 0f && ldl <= 0f && triglyceride <= 0f) {
            return null
        }
        return hdl + ldl + (triglyceride / TG_DIVISOR)
    }

    /**
     * 计算非 HDL 胆固醇
     */
    fun calculateNonHdl(totalCholesterol: Float?, hdl: Float?): Float? {
        if (totalCholesterol == null || hdl == null) {
            return null
        }
        return (totalCholesterol - hdl).coerceAtLeast(0f)
    }

    /**
     * 计算比值（通用方法）
     */
    fun calculateRatio(numerator: Float?, denominator: Float?): Float? {
        if (numerator == null || denominator == null) {
            return null
        }
        if (denominator == 0f) {
            return null
        }
        return numerator / denominator
    }

    /**
     * 构建完整的胆固醇指标结果
     */
    fun buildMetrics(hdl: Float?, ldl: Float?, triglyceride: Float?): CholesterolMetrics {
        val total = calculateTotalCholesterol(hdl, ldl, triglyceride)
        val nonHdl = calculateNonHdl(total, hdl)
        val tcHdl = calculateRatio(total, hdl)
        val ldlHdl = calculateRatio(ldl, hdl)
        return CholesterolMetrics(
            totalCholesterol = total?.let { roundTo(it, 2) },
            nonHdl = nonHdl?.let { roundTo(it, 2) },
            tcHdlRatio = tcHdl?.let { roundTo(it, 2) },
            ldlHdlRatio = ldlHdl?.let { roundTo(it, 2) },
            riskLevel = CholesterolLevel.fromMetrics(total, nonHdl, ldl, hdl)
        )
    }

    /**
     * 保留指定位小数
     */
    private fun roundTo(value: Float, scale: Int): Float {
        val factor = 10.0.pow(scale).toFloat()
        return (round(value * factor) / factor)
    }
}

/**
 * 胆固醇指标结果数据模型
 */
data class CholesterolMetrics(
    val totalCholesterol: Float?,
    val nonHdl: Float?,
    val tcHdlRatio: Float?,
    val ldlHdlRatio: Float?,
    val riskLevel: CholesterolLevel
)
