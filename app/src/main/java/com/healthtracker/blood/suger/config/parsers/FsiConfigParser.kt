package com.healthtracker.blood.suger.config.parsers

import com.healthtracker.blood.suger.config.ConfigKeys
import com.healthtracker.blood.suger.config.models.FsiConfig
import com.healthtracker.framework.config.core.ConfigParser
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FSI锁屏推送配置解析器
 *
 * 从 Remote Config 读取并解析 FSI 相关配置
 * 支持多种数据格式的兼容性解析
 */
@Singleton
class FsiConfigParser @Inject constructor() : ConfigParser<FsiConfig> {

    override val configKey: String = ConfigKeys.FSI_CONFIG_JSON

    /**
     * 解析FSI配置
     *
     * 注意：此方法不使用，因为FSI配置由多个独立的key组成
     * 实际解析通过 parseFsiConfig() 方法完成
     */
    override fun parse(rawValue: String): FsiConfig? {
        if (rawValue.isBlank()) {
            return null
        }
        return parseFromJson(rawValue)
    }

    /**
     * 获取默认FSI配置
     */
    override fun getDefault(): FsiConfig {
        return FsiConfig.createDefault()
    }

    /**
     * 验证FSI配置有效性
     */
    override fun validate(config: FsiConfig): Boolean {
        return config.isValid()
    }

    private fun parseFromJson(rawValue: String): FsiConfig? {
        return runCatching {
            val json = JSONObject(rawValue)

            val enabled = json.optBooleanCompat("fsi_enable", true)
            val quietPeriodHours = json.optIntCompat("quiet_period", 24)
            val maxPrompt = json.optIntCompat("max_prompts", 3)
            val timeWindow = parseTimeWindow(json.optString("time_window", "00:00-23:00"))

            FsiConfig(
                enabled = enabled,
                quietPeriodHours = quietPeriodHours,
                timeWindow = timeWindow,
                maxTriggerCount = maxPrompt
            )
        }.getOrNull()
    }

    private fun parseTimeWindow(raw: String): Int {
        val defaultWindow = 23 // 0-23
        if (raw.isBlank()) {
            return defaultWindow
        }

        val parts = raw.split("-")
        if (parts.size != 2) {
            return defaultWindow
        }

        val startHour = parseHour(parts[0])
        val endHour = parseHour(parts[1])

        if (startHour == null || endHour == null) {
            return defaultWindow
        }

        if (startHour !in 0..23 || endHour !in 0..23 || startHour > endHour) {
            return defaultWindow
        }

        return startHour * 100 + endHour
    }

    private fun parseHour(segment: String): Int? {
        val normalized = segment.trim().substringBefore(":")
        return normalized.toIntOrNull()
    }

    private fun JSONObject.optBooleanCompat(key: String, default: Boolean): Boolean {
        if (!has(key)) return default
        val value = opt(key)
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> value.toString().equals("true", ignoreCase = true)
        }
    }

    private fun JSONObject.optIntCompat(key: String, default: Int): Int {
        if (!has(key)) return default
        val value = opt(key)
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }
}
