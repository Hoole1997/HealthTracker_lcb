package convention.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Firebase 配置插件
 */
class AndroidFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.gms.google-services")
                apply("com.google.firebase.crashlytics")
            }

            dependencies {
                val bom = libs.findLibrary("firebase.bom").get()
                "implementation"(platform(bom))

                "implementation"(libs.findLibrary("firebase.config").get())
                "implementation"(libs.findLibrary("firebase.analytics.ktx").get())
                "implementation"(libs.findLibrary("firebase.crashlytics.ktx").get())
                "implementation"(libs.findLibrary("firebase.perf.ktx").get())
            }
        }
    }
}