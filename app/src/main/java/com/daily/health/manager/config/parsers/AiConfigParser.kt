package com.daily.health.manager.config.parsers

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.daily.health.manager.config.ConfigKeys
import com.daily.health.manager.config.models.AiConfig
import com.healthtracker.framework.config.core.ConfigParser

/**
 * AI 助手配置解析器
 *
 * 负责从 Remote Config 中解析 AI_Switch 参数
 */
class AiConfigParser(
    private val remoteConfig: FirebaseRemoteConfig
) : ConfigParser<AiConfig> {

    override val configKey: String = ConfigKeys.AI_SWITCH

    override fun parse(rawValue: String): AiConfig? {
        // 由于 AI_Switch 是一个简单的布尔/整数值，我们直接从 RemoteConfig 中读取更可靠
        // rawValue 在这里可能是由 RemoteConfigManager 自动传入的 getLong 结果的字符串表示
        return try {
            val value = rawValue.toIntOrNull() ?: 0
            AiConfig(aiSwitch = value)
        } catch (e: Exception) {
            AiConfig.createDefault()
        }
    }

    override fun getDefault(): AiConfig {
        return AiConfig.createDefault()
    }

    override fun validate(config: AiConfig): Boolean {
        return config.aiSwitch == 0 || config.aiSwitch == 1
    }
}
