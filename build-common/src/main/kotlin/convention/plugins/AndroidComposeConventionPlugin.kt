package convention.plugins

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

/**
 * Compose 配置插件
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            // 等待 Android 插件应用后再配置
            afterEvaluate {
                val appExtension = extensions.findByType<BaseAppModuleExtension>()
                val libraryExtension = extensions.findByType<LibraryExtension>()

                when {
                    appExtension != null -> configureAndroidCompose(appExtension)
                    libraryExtension != null -> configureAndroidCompose(libraryExtension)
                    else -> throw IllegalStateException(
                        "AndroidComposeConventionPlugin requires either Android Application or Library plugin to be applied first"
                    )
                }
            }
        }
    }
}

/**
 * 配置 Compose
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        buildFeatures {
            compose = true
        }

        dependencies {
            val bom = libs.findLibrary("androidx.compose.bom").get()
            "implementation"(platform(bom))
            "androidTestImplementation"(platform(bom))

            // Compose 基础依赖
            "implementation"(libs.findLibrary("androidx.compose.ui").get())
            "implementation"(libs.findLibrary("androidx.compose.ui.graphics").get())
            "implementation"(libs.findLibrary("androidx.compose.ui.tooling.preview").get())
            "implementation"(libs.findLibrary("androidx.compose.material3").get())

            // Activity Compose
            "implementation"(libs.findLibrary("androidx.activity.compose").get())

            // ViewModel Compose
            "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())

            // Debug 工具
            "debugImplementation"(libs.findLibrary("androidx.compose.ui.tooling").get())
            "debugImplementation"(libs.findLibrary("androidx.compose.ui.test.manifest").get())

            // 测试
            "androidTestImplementation"(libs.findLibrary("androidx.compose.ui.test.junit4").get())
        }
    }
}