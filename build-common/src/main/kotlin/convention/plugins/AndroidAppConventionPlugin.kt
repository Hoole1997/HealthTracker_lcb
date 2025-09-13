package convention.plugins

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android Application 模块的约定插件
 */
class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureApplicationExtension(this)

                @Suppress("UnstableApiUsage")
                buildFeatures {
                    buildConfig = true
                    viewBinding = true
                }
            }

            // 配置测试依赖
            configureAndroidTest()
        }
    }
}