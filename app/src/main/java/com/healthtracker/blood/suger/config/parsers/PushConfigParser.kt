package com.healthtracker.blood.suger.config.parsers

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.healthtracker.blood.suger.config.ConfigKeys
import com.healthtracker.blood.suger.config.models.ChannelConfig
import com.healthtracker.blood.suger.config.models.PushConfig
import com.healthtracker.framework.config.core.ConfigParser
import javax.inject.Inject

/**
 * 推送配置解析器
 *
 * 负责解析 Remote Config 中的推送配置 JSON
 *
 * JSON 格式示例:
 * ```json
 * {
 *   "paid_channel": {
 *     "total_push_count": 999,
 *     "unlock_push_interval": "10",
 *     ...
 *   },
 *   "organic_channel": {
 *     "total_push_count": 3,
 *     "unlock_push_interval": "10",
 *     ...
 *   }
 * }
 * ```
 */
class PushConfigParser @Inject constructor(
    private val gson: Gson,
    private val pushMessageParser: PushMessageParser
) : ConfigParser<PushConfig> {

    override val configKey: String = ConfigKeys.PUSH_CONFIG_JSON

    override fun parse(rawValue: String): PushConfig? {
        return try {
            // 解析 JSON 对象
            val jsonObject = gson.fromJson(rawValue, JsonObject::class.java)

            // 解析付费渠道配置
            val paidChannel = parseChannelConfig(jsonObject.getAsJsonObject("paid_channel"))
                ?: return null

            // 解析自然量渠道配置
            val organicChannel = parseChannelConfig(jsonObject.getAsJsonObject("organic_channel"))
                ?: return null

            // 获取推送消息列表（从另一个配置键）
            // 注意：这里为了完整性，我们依赖 pushMessageParser 的默认值
            // 实际使用时，应该先获取推送消息配置，再组合成 PushConfig
            val pushMessages = pushMessageParser.getDefault()

            PushConfig(
                paidChannel = paidChannel,
                organicChannel = organicChannel,
                pushMessages = pushMessages
            )
        } catch (e: JsonSyntaxException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析渠道配置
     */
    private fun parseChannelConfig(jsonObject: JsonObject?): ChannelConfig? {
        return try {
            jsonObject?.let {
                ChannelConfig(
                    totalPushCount = it.get("total_push_count")?.asInt ?: 0,
                    unlockPushInterval = it.get("unlock_push_interval")?.asString ?: "10",
                    backgroundPushInterval = it.get("background_push_interval")?.asString ?: "10",
                    hoverDurationStrategySwitch = it.get("hover_duration_strategy_switch")?.asInt ?: 0,
                    hoverDurationLoopCount = it.get("hover_duration_loop_count")?.asInt ?: 0,
                    newUserCooldown = it.get("new_user_cooldown")?.asString ?: "24",
                    doNotDisturbStart = it.get("do_not_disturb_start")?.asString ?: "02:00",
                    doNotDisturbEnd = it.get("do_not_disturb_end")?.asString ?: "08:00",
                    notificationEnabled = it.get("notification_enabled")?.asInt ?: 1,
                    keepalivePollingIntervalMinutes = it.get("keepalive_polling_interval_minutes")?.asInt ?: 15
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getDefault(): PushConfig {
        return PushConfig.createDefault()
    }

    override fun validate(config: PushConfig): Boolean {
        return config.isValid()
    }
}
