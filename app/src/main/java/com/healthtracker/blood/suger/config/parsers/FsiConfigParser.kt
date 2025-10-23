package com.healthtracker.blood.suger.config.parsers

import com.healthtracker.blood.suger.config.ConfigKeys
import com.healthtracker.blood.suger.config.models.FsiConfig
import com.healthtracker.framework.config.core.ConfigParser
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

    // FSI配置不使用单一的JSON key，而是使用多个独立的配置项
    override val configKey: String = ConfigKeys.FSI_ENABLED

    /**
     * 解析FSI配置
     *
     * 注意：此方法不使用，因为FSI配置由多个独立的key组成
     * 实际解析通过 parseFsiConfig() 方法完成
     */
    override fun parse(rawValue: String): FsiConfig? {
        // 不使用此方法，因为FSI配置不是单一JSON
        return null
    }

    /**
     * 从 RemoteConfigManager 解析完整的 FSI 配置
     *
     * @param getBoolean 获取布尔值的函数
     * @param getString 获取字符串值的函数
     * @param getInt 获取整数值的函数
     * @return FsiConfig 配置对象
     */
    fun parseFsiConfig(
        getBoolean: (String, Boolean) -> Boolean,
        getString: (String, String) -> String,
        getInt: (String, Int) -> Int
    ): FsiConfig {
        val enabled = getBoolean(ConfigKeys.FSI_ENABLED, true)

        // 兼容 String 和 Int 两种格式
        val quietPeriodStr = getString(ConfigKeys.FSI_QUIET_PERIOD, "24")
        val quietPeriodHours = quietPeriodStr.toIntOrNull() ?: 24

        val timeWindow = getInt(ConfigKeys.FSI_TIME_WINDOW, 23)  // 23 = 0023 (0-23点)
        val maxTriggerCount = getInt(ConfigKeys.FSI_MAX_TRIGGER_COUNT, 3)

        return FsiConfig(
            enabled = enabled,
            quietPeriodHours = quietPeriodHours,
            timeWindow = timeWindow,
            maxTriggerCount = maxTriggerCount
        )
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
}
