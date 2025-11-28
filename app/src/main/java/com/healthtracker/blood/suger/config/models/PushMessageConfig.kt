package com.healthtracker.blood.suger.config.models

import android.content.Context
import com.healthtracker.framework.util.LanguageUtils

/**
 * 推送消息远程配置模型
 *
 * 用于解析支持多语言的远程配置 JSON
 * Remote Config JSON 格式示例:
 * ```json
 * {
 *   "id": "push_001",
 *   "iconType": 1,
 *   "actionType": 1,
 *   "en": {
 *     "title": "💖 Your Daily Health Check-in!",
 *     "content": "Tracking your blood sugar...",
 *     "buttonText": "RECOVER NOW"
 *   },
 *   "ja": {
 *     "title": "💖 デイリー健康チェック！",
 *     "content": "血糖値、血圧、体重の記録は...",
 *     "buttonText": "今すぐ記録"
 *   },
 *   "ko": {
 *     "title": "💖 데일리 건강 체크인!",
 *     "content": "혈당, 혈압, 체중 추적은...",
 *     "buttonText": "지금 기록하기"
 *   }
 * }
 * ```
 *
 * @property id 消息唯一标识
 * @property iconType 图标类型 (1-10)
 * @property actionType 点击行为类型 (1-10)
 * @property en 英文内容（必需）
 * @property ja 日文内容（可选）
 * @property ko 韩文内容（可选）
 */
data class PushMessageConfig(
    val id: String,
    val iconType: Int,
    val actionType: Int,
    val en: LocalizedPushContent,
    val ja: LocalizedPushContent? = null,
    val ko: LocalizedPushContent? = null
) : java.io.Serializable {

    companion object {
        /**
         * 将应用语言 ID 映射到配置键
         * 
         * 应用使用的语言 ID（LanguageUtils）:
         * - "jp" (日语)
         * - "kr" (韩语)
         * - "en" (英语)
         * 
         * Remote Config 使用的标准语言代码:
         * - "ja" (日语)
         * - "ko" (韩语)
         * - "en" (英语)
         */
        private fun mapAppLanguageToConfigKey(appLangId: String): String {
            return when (appLangId) {
                "jp" -> "ja"  // 日语
                "kr" -> "ko"  // 韩语
                else -> "en"  // 默认英语
            }
        }
    }

    /**
     * 根据应用设置的语言转换为 PushMessage
     *
     * 语言选择逻辑：
     * - 优先使用应用内设置的语言（通过 LanguageUtils.getAppLanguage）
     * - 日语环境 (jp) -> 使用日文内容，若无则降级到英文
     * - 韩语环境 (kr) -> 使用韩文内容，若无则降级到英文
     * - 其他语言 -> 使用英文内容
     *
     * @param context 上下文，用于获取应用语言设置
     * @return 本地化后的 PushMessage
     */
    fun toPushMessage(context: Context): PushMessage {
        // 获取应用设置的语言 ID（如 "jp", "kr", "en"）
        val appLangId = LanguageUtils.getAppLanguage(context)
        
        // 映射到配置键（如 "ja", "ko", "en"）
        val configKey = mapAppLanguageToConfigKey(appLangId)
        
        val localizedContent = when (configKey) {
            "ja" -> ja ?: en  // 日文，降级到英文
            "ko" -> ko ?: en  // 韩文，降级到英文
            else -> en        // 其他语言使用英文
        }

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
        return id.isNotBlank() &&
                iconType in 1..10 &&
                actionType in 1..10 &&
                en.isValid() &&
                (ja == null || ja.isValid()) &&
                (ko == null || ko.isValid())
    }
}
