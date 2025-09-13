package convention.plugins

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android Library 模块的约定插件
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                configureLibraryExtension(this)

                @Suppress("UnstableApiUsage")
                buildFeatures {
                    viewBinding = true
                }
            }

            // 配置测试依赖
            configureAndroidTest()
        }
    }
}