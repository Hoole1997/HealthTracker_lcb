package com.healthtracker.blood.suger.util

import com.healthtracker.blood.suger.data.enums.CholesterolLevel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 胆固醇计算逻辑单元测试，确保关键阈值与风险等级判定正确
 */
class CholesterolCalculatorTest {

    @Test
    fun `normal inputs produce normal level`() {
        val metrics = CholesterolCalculator.buildMetrics(
            hdl = 60f,
            ldl = 90f,
            triglyceride = 100f
        )
        assertEquals(178.0f, metrics.totalCholesterol!!, 0.01f)
        assertEquals(CholesterolLevel.NORMAL, metrics.riskLevel)
    }

    @Test
    fun `ldl near optimal downgrades level`() {
        val metrics = CholesterolCalculator.buildMetrics(
            hdl = 55f,
            ldl = 110f,
            triglyceride = 80f
        )
        assertEquals(CholesterolLevel.NEAR_OPTIMAL, metrics.riskLevel)
    }

    @Test
    fun `borderline metrics detected`() {
        val metrics = CholesterolCalculator.buildMetrics(
            hdl = 50f,
            ldl = 135f,
            triglyceride = 120f
        )
        assertEquals(CholesterolLevel.BORDERLINE, metrics.riskLevel)
    }

    @Test
    fun `high non hdl escalates risk`() {
        val metrics = CholesterolCalculator.buildMetrics(
            hdl = 42f,
            ldl = 150f,
            triglyceride = 180f
        )
        assertEquals(CholesterolLevel.HIGH, metrics.riskLevel)
    }

    @Test
    fun `very high ldl escalates to top tier`() {
        val metrics = CholesterolCalculator.buildMetrics(
            hdl = 40f,
            ldl = 195f,
            triglyceride = 160f
        )
        assertEquals(CholesterolLevel.VERY_HIGH, metrics.riskLevel)
    }
}
