package com.daily.health.manager.config.models

import androidx.annotation.Keep

/**
 * AI 助手配置模型
 *
 * @property aiSwitch 0=关闭; 1=开启; 默认=0
 */
@Keep
data class AiConfig(
    val aiSwitch: Int = 0
) {
    companion object {
        fun createDefault() = AiConfig(aiSwitch = 0)
    }

    fun isEnabled() = aiSwitch == 1
}
