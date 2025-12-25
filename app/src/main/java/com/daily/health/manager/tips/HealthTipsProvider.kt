package com.daily.health.manager.tips

import com.daily.health.manager.R
import com.daily.health.manager.face.act.getText
import kotlin.random.Random

/**
 * 统一的健康建议数据模型（跨页面复用）
 */
data class HealthTips(val title: String, val description: String)

/**
 * 健康建议提供者：按健康指标从资源字符串数组随机挑选一条建议。
 * 使用 res/values/health_tips_arrays.xml 中的 string-array，避免硬编码。
 */
object HealthTipsProvider {

    /**
     * 从对应指标的 string-array 中随机选择一条健康建议。
     * - 仅依赖资源数组，不与具体数值等级绑定
     * - 若数组缺失或长度不匹配，将返回通用的兜底文案
     */
    fun pickRandom(metric: HealthMetric): HealthTips {
        val (titlesRes, textsRes) = when (metric) {
            HealthMetric.BLOOD_PRESSURE -> R.array.ht_health_tips_blood_pressure_titles to R.array.ht_health_tips_blood_pressure_texts
            HealthMetric.BLOOD_SUGAR -> R.array.ht_health_tips_blood_sugar_titles to R.array.ht_health_tips_blood_sugar_texts
            HealthMetric.CHOLESTEROL -> R.array.ht_health_tips_cholesterol_titles to R.array.ht_health_tips_cholesterol_texts
            HealthMetric.HEART_RATE -> R.array.ht_health_tips_heart_rate_titles to R.array.ht_health_tips_heart_rate_texts
            HealthMetric.BMI -> R.array.ht_health_tips_bmi_titles to R.array.ht_health_tips_bmi_texts
            HealthMetric.STEPS -> R.array.ht_health_tips_steps_titles to R.array.ht_health_tips_steps_texts
            HealthMetric.HYDRATION -> R.array.ht_health_tips_hydration_titles to R.array.ht_health_tips_hydration_texts
        }

        val titles = safeGetStringArray(titlesRes)
        val texts = safeGetStringArray(textsRes)

        val size = minOf(titles.size, texts.size)
        if (size <= 0) {
            return HealthTips(
                title = "Health Tip",
                description = "Stay mindful of balanced meals and regular activity."
            )
        }

        val index = Random.nextInt(size)
        return HealthTips(
            title = titles.getOrElse(index) { "Health Tip" },
            description = texts.getOrElse(index) { "Stay mindful of balanced meals and regular activity." }
        )
    }

    private fun safeGetStringArray(resId: Int): Array<String> = try {
        getText(resId)
    } catch (e: Exception) {
        // 资源丢失时返回空数组，交由调用方走兜底逻辑
        emptyArray()
    }
}