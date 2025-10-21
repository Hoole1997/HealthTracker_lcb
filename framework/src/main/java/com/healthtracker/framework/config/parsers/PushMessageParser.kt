package com.healthtracker.framework.config.parsers

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.healthtracker.framework.config.core.ConfigKey
import com.healthtracker.framework.config.core.ConfigParser
import com.healthtracker.framework.config.models.PushMessage
import javax.inject.Inject

/**
 * 推送消息内容解析器
 *
 * 负责解析 Remote Config 中的推送消息数组 JSON
 *
 * JSON 格式示例:
 * ```json
 * [
 *   {
 *     "id": "push_001",
 *     "title": "💖 Your Daily Health Check-in!",
 *     "desc": "Tracking your blood sugar...",
 *     "buttonText": "RECOVER NOW",
 *     "iconType": 1,
 *     "actionType": 1
 *   }
 * ]
 * ```
 */
class PushMessageParser @Inject constructor(
    private val gson: Gson
) : ConfigParser<List<PushMessage>> {

    override val configKey: String = ConfigKey.PUSH_CONTENT_ARRAY

    override fun parse(rawValue: String): List<PushMessage>? {
        return try {
            val type = object : TypeToken<List<PushMessage>>() {}.type
            gson.fromJson<List<PushMessage>>(rawValue, type)
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    override fun getDefault(): List<PushMessage> {
        return listOf(PushMessage.createDefault())
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
