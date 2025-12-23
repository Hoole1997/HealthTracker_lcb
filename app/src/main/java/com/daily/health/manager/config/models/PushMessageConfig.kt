package com.daily.health.manager.config.models

import android.content.Context
import com.healthtracker.framework.util.LanguageUtils

/**
 * 推送消息远程配置模型（动态多语言支持）
 *
 * 用于解析支持多语言的远程配置 JSON
 * 
 * Remote Config JSON 格式示例:
 * ```json
 * {
 *   "id": "push_001",
 *   "iconType": 1,
 *   "actionType": 1,
 *   "localizations": {
 *     "en": {
 *       "title": "💖 Your Daily Health Check-in!",
 *       "content": "Tracking your blood sugar...",
 *       "buttonText": "RECOVER NOW"
 *     },
 *     "ja": {
 *       "title": "💖 デイリー健康チェック！",
 *       "content": "血糖値、血圧、体重の記録は...",
 *       "buttonText": "今すぐ記録"
 *     },
 *     "ko": {
 *       "title": "💖 데일리 건강 체크인!",
 *       "content": "혈당, 혈압, 체중 추적은...",
 *       "buttonText": "지금 기록하기"
 *     },
 *     "es": {
 *       "title": "💖 ¡Tu chequeo diario de salud!",
 *       "content": "Seguimiento de glucosa...",
 *       "buttonText": "REGISTRAR AHORA"
 *     }
 *   }
 * }
 * ```
 *
 * **动态语言支持**：
 * - 可在 Firebase Remote Config 中添加任意语言代码
 * - 无需修改应用代码即可支持新语言
 * - 自动降级到英语（如果目标语言不存在）
 *
 * @property id 消息唯一标识
 * @property iconType 图标类型 (1-10)
 * @property actionType 点击行为类型 (1-10)
 * @property localizations 多语言内容映射表，键为语言代码（如 "en", "ja", "ko", "es"）
 */
data class PushMessageConfig(
    val id: String,
    val iconType: Int,
    val actionType: Int,
    val localizations: Map<String, LocalizedPushContent>
) : java.io.Serializable {

    companion object {
        /**
         * 默认降级语言（英语）
         */
        private const val DEFAULT_LANGUAGE = "en"
    }

    /**
     * 根据应用设置的语言转换为 PushMessage
     *
     * 语言选择逻辑（支持动态语言）：
     * 1. 获取应用当前语言 ID
     * 2. 映射为标准语言代码
     * 3. 在 localizations 中查找对应语言
     * 4. 如果不存在，降级到英语
     * 5. 如果英语也不存在，使用第一个可用语言
     *
     * @param context 上下文，用于获取应用语言设置
     * @return 本地化后的 PushMessage
     */
    fun toPushMessage(context: Context): PushMessage {
        val appLangId = LanguageUtils.getAppLanguage(context)

        
        // 3. 选择本地化内容（支持动态降级）
        val localizedContent = localizations[appLangId]  // 优先使用目标语言
            ?: localizations[DEFAULT_LANGUAGE]  // 降级到英语
            ?: localizations.values.firstOrNull()  // 最后使用任意可用语言
            ?: throw IllegalStateException("PushMessageConfig must have at least one localization")

        return PushMessage(
            id = id,
            title = localizedContent.title,
            desc = localizedContent.content,
            buttonText = localizedContent.buttonText,
            iconType = iconType,
            actionType = actionType
        )
    }

    /**
     * 验证配置有效性
     */
    fun isValid(): Boolean {
        if (id.isBlank()) return false
        if (iconType !in 1..12) return false
        if (actionType !in 1..12) return false
        if (localizations.isEmpty()) return false

        if (iconType == 11 || actionType == 11) return false

        return localizations.values.all { it.isValid() }
    }
}
