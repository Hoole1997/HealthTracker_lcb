package com.healthtracker.blood.suger.config.parsers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.healthtracker.blood.suger.config.ConfigKeys
import com.healthtracker.blood.suger.config.models.PushMessage
import com.healthtracker.blood.suger.config.models.PushMessageConfig
import com.healthtracker.framework.config.core.ConfigParser
import com.healthtracker.framework.ext.logd

/**
 * 推送消息内容解析器（动态多语言支持）
 *
 * 负责解析 Remote Config 中的推送消息数组 JSON，支持动态多语言配置
 *
 * JSON 格式示例:
 * ```json
 * [
 *   {
 *     "id": "push_001",
 *     "iconType": 1,
 *     "actionType": 1,
 *     "localizations": {
 *       "en": {
 *         "title": "💖 Your Daily Health Check-in!",
 *         "content": "Tracking your blood sugar...",
 *         "buttonText": "RECOVER NOW"
 *       },
 *       "ja": {
 *         "title": "💖 デイリー健康チェック！",
 *         "content": "血糖値、血圧、体重の記録は...",
 *         "buttonText": "今すぐ記録"
 *       },
 *       "ko": {
 *         "title": "💖 데일리 건강 체크인!",
 *         "content": "혈당, 혈압, 체중 추적은...",
 *         "buttonText": "지금 기록하기"
 *       },
 *       "es": {
 *         "title": "💖 ¡Tu chequeo diario de salud!",
 *         "content": "Seguimiento de glucosa...",
 *         "buttonText": "REGISTRAR AHORA"
 *       }
 *     }
 *   }
 * ]
 * ```
 *
 * **动态语言支持**：
 * - 可在 Firebase Remote Config 中添加任意语言代码（如 "es", "fr", "de"）
 * - 无需修改应用代码即可支持新语言
 * - 应用会自动根据当前语言设置选择对应内容
 * - 如果目标语言不存在，自动降级到英语
 */
class PushMessageParser(
    private val gson: Gson,
    private val context: Context
) : ConfigParser<List<PushMessage>> {

    override val configKey: String = ConfigKeys.PUSH_ARRAY

    override fun parse(rawValue: String): List<PushMessage>? {
        return try {
            "原始参数：$rawValue".logd("PushMessageParser")
            // 解析为多语言配置列表
            val type = object : TypeToken<List<PushMessageConfig>>() {}.type
            val configList = gson.fromJson<List<PushMessageConfig>>(rawValue, type)
            
            // 根据应用当前语言转换为 PushMessage 列表
            configList?.map { it.toPushMessage(context) }
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    override fun getDefault(): List<PushMessage> {
        return PushMessage.createDefaultList()
    }

    override fun validate(config: List<PushMessage>): Boolean {
        // 至少要有一条消息
        if (config.isEmpty()) {
            return false
        }

        // 所有消息都必须有效
        return config.all { it.isValid() }
    }
}
