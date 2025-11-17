package com.healthtracker.blood.suger.data.preferences

import com.healthtracker.blood.suger.constants.KEY_USER_HEIGHT_CM
import com.healthtracker.blood.suger.constants.KEY_USER_WEIGHT_KG
import com.healthtracker.blood.suger.data.constants.BodyMetricsDefaults
import com.healthtracker.framework.util.SpUtils

/**
 * 统一管理身高/体重在 MMKV 中的持久化读写，方便计步与设置页复用。
 */
object BodyMetricsPreferences {

    fun save(heightCm: Double?, weightKg: Double?) {
        heightCm?.takeIf { it > 0 }?.let { SpUtils.putDouble(KEY_USER_HEIGHT_CM, it) }
        weightKg?.takeIf { it > 0 }?.let { SpUtils.putDouble(KEY_USER_WEIGHT_KG, it) }
    }

    fun getHeightCm(): Double =
        SpUtils.getDouble(KEY_USER_HEIGHT_CM, BodyMetricsDefaults.DEFAULT_HEIGHT_CM).coerceAtLeast(1.0)

    fun getWeightKg(): Double =
        SpUtils.getDouble(KEY_USER_WEIGHT_KG, BodyMetricsDefaults.DEFAULT_WEIGHT_KG).coerceAtLeast(1.0)
}
