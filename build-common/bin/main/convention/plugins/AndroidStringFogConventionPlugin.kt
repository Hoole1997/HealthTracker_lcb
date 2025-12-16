package convention.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * StringFog 字符串混淆约定插件
 *
 * 提供统一的 StringFog 配置，模块只需应用此插件即可启用字符串混淆功能
 */
class AndroidStringFogConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 从 gradle.properties 读取 release 标志
            val isRelease = findProperty("app")?.let {
                (it as Map<*, *>)["string_fog"] as Boolean
            } ?: false

            logger.lifecycle("StringFog enabled: $isRelease")

            if (!isRelease) {
                return
            }

            // 应用 StringFog 插件
            pluginManager.apply("stringfog")

            // 配置 StringFog - 必须在插件应用后立即配置
            val stringfogExtension = extensions.getByName("stringfog")
            val extensionClass = stringfogExtension.javaClass

            try {
                // 设置 implementation
                extensionClass.getMethod("setImplementation", String::class.java)
                    .invoke(stringfogExtension, "com.github.megatronking.stringfog.xor.StringFogImpl")

                // 设置 enable
                extensionClass.getMethod("setEnable", Boolean::class.javaPrimitiveType)
                    .invoke(stringfogExtension, isRelease)

                // 设置 kg (RandomKeyGenerator)
                val kgClass = Class.forName("com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator")
                val kgInstance = kgClass.getDeclaredConstructor().newInstance()
                extensionClass.getMethod("setKg", Class.forName("com.github.megatronking.stringfog.IKeyGenerator"))
                    .invoke(stringfogExtension, kgInstance)

                // 设置 mode (bytes)
                val modeClass = Class.forName("com.github.megatronking.stringfog.plugin.StringFogMode")
                val bytesMode = modeClass.enumConstants.first { it.toString() == "base64" }
                extensionClass.getMethod("setMode", modeClass)
                    .invoke(stringfogExtension, bytesMode)

                logger.lifecycle("StringFog configuration completed successfully")
            } catch (e: Exception) {
                logger.error("Failed to configure StringFog", e)
                throw e
            }
        }
    }
}
