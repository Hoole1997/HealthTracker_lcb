package com.healthtracker.framework.config.core

/**
 * 配置解析器接口
 *
 * 每种业务配置需要实现此接口，提供解析和默认值逻辑
 *
 * 实现示例:
 * ```kotlin
 * class PushConfigParser(private val gson: Gson) : ConfigParser<PushConfig> {
 *     override val configKey = ConfigKey.PUSH_CONFIG_JSON
 *
 *     override fun parse(rawValue: String): PushConfig? {
 *         return try {
 *             gson.fromJson(rawValue, PushConfig::class.java)
 *         } catch (e: Exception) {
 *             null
 *         }
 *     }
 *
 *     override fun getDefault(): PushConfig {
 *         return PushConfig.createDefault()
 *     }
 * }
 * ```
 *
 * @param T 配置数据类型
 */
interface ConfigParser<T> {

    /**
     * 配置键
     * 对应 Firebase Remote Config 中的 key
     */
    val configKey: String

    /**
     * 解析配置
     *
     * @param rawValue Remote Config 返回的原始字符串
     * @return 解析后的配置对象，解析失败返回 null
     */
    fun parse(rawValue: String): T?

    /**
     * 获取默认配置
     *
     * 当远程配置不可用或解析失败时使用
     * 应返回一个安全的默认配置，确保应用正常运行
     *
     * @return 默认配置对象
     */
    fun getDefault(): T

    /**
     * 验证配置有效性（可选实现）
     *
     * 在配置解析成功后调用，用于验证配置数据的合理性
     * 例如：检查必填字段、数值范围、逻辑一致性等
     *
     * @param config 待验证的配置对象
     * @return true 配置有效，false 配置无效（将使用默认配置）
     */
    fun validate(config: T): Boolean = true
}
