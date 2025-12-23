package com.daily.health.manager.config.registry

import com.daily.health.manager.config.models.PushConfig
import com.daily.health.manager.config.parsers.PushConfigParser
import com.daily.health.manager.config.parsers.PushMessageParser
import com.healthtracker.framework.config.core.ConfigRegistry

/**
 * 应用配置注册中心
 *
 * 负责注册所有业务配置的解析器
 * 在应用启动时调用 registerAllParsers() 完成注册
 *
 * 使用示例:
 * ```kotlin
 * // 在 Application.onCreate() 中
 * appConfigRegistry.registerAllParsers()
 * ```
 *
 * 添加新配置类型的步骤:
 * 1. 创建配置数据类 (例如: AdConfig.kt)
 * 2. 创建配置解析器 (例如: AdConfigParser.kt)
 * 3. 在此类的 registerAllParsers() 方法中注册新解析器
 *
 * @property registry 核心配置注册表
 * @property pushConfigParser 推送配置解析器
 * @property pushMessageParser 推送消息解析器
 */
class AppConfigRegistry(
    private val registry: ConfigRegistry,
    private val pushConfigParser: PushConfigParser,
    private val pushMessageParser: PushMessageParser
    // 未来添加更多解析器注入
    // private val adConfigParser: AdConfigParser,
    // private val featureConfigParser: FeatureConfigParser,
    // private val abTestConfigParser: AbTestConfigParser,
) {

    companion object {
        private const val TAG = "AppConfigRegistry"
    }

    /**
     * 注册所有配置解析器
     *
     * 应在应用启动时调用一次
     */
    fun registerAllParsers() {
        // 注册推送相关配置
        registerPushParsers()

        // 未来在此添加其他配置注册
        // registerAdParsers()
        // registerFeatureParsers()
        // registerAbTestParsers()
    }

    /**
     * 注册推送相关解析器
     */
    private fun registerPushParsers() {
        registry.register(PushConfig::class, pushConfigParser)
        // 注意：PushMessageParser 返回 List<PushMessage>
        // 由于泛型类型擦除，我们不能直接注册 List::class
        // PushMessage 列表已经包含在 PushConfig 中，所以这里不需要单独注册
    }

    // 未来添加其他配置注册方法示例:

    /**
     * 注册广告相关解析器
     */
    // private fun registerAdParsers() {
    //     registry.register(AdConfig::class, adConfigParser)
    // }

    /**
     * 注册功能开关相关解析器
     */
    // private fun registerFeatureParsers() {
    //     registry.register(FeatureFlags::class, featureConfigParser)
    // }

    /**
     * 注册 AB 测试相关解析器
     */
    // private fun registerAbTestParsers() {
    //     registry.register(AbTestConfig::class, abTestConfigParser)
    // }

    /**
     * 获取已注册的解析器数量
     *
     * 用于调试和监控
     */
    fun getRegisteredCount(): Int {
        return registry.getRegisteredCount()
    }
}
