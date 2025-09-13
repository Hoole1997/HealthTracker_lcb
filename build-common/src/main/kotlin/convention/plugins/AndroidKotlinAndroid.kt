@file:Suppress("UnstableApiUsage")

package convention.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * 获取版本目录
 */
internal val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * 配置 Kotlin 和 Android 通用设置
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = libs.findVersion("compileSdk").get().toString().toIntOrNull()

        defaultConfig {
            minSdk = libs.findVersion("minSdk").get().toString().toIntOrNull()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }

    configureKotlin()
}

/**
 * 配置 Kotlin
 */
private fun Project.configureKotlin() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)

            // 对非发布版本，将警告视为错误（更严格的编译）
            val warningsAsErrors = findProperty("warningsAsErrors")?.toString()
            allWarningsAsErrors.set(warningsAsErrors.toBoolean())

            freeCompilerArgs.set(
                freeCompilerArgs.get() + buildList {
                    add("-opt-in=kotlin.RequiresOptIn")
                    add("-Xjsr305=strict")
                }
            )
        }
    }
}

/**
 * 配置 Application 扩展
 */
internal fun Project.configureApplicationExtension(
    extension: ApplicationExtension
) {
    extension.apply {
        defaultConfig {
            targetSdk = libs.findVersion("targetSdk").get().toString().toIntOrNull()

            // 默认开启向量图支持
            vectorDrawables {
                useSupportLibrary = true
            }

            // 默认开启 MultiDex
            multiDexEnabled = true
        }

        buildFeatures {
            buildConfig = true
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }
}

/**
 * 配置 Library 扩展
 */
internal fun Project.configureLibraryExtension(
    extension: LibraryExtension
) {
    extension.apply {
        defaultConfig {
            consumerProguardFiles("consumer-rules.pro")
        }

        buildFeatures {
            buildConfig = false
        }
    }
}

/**
 * 配置测试依赖
 */
internal fun Project.configureAndroidTest() {
    dependencies {
        "testImplementation"(libs.findLibrary("junit").get())
        "androidTestImplementation"(libs.findLibrary("androidx.junit").get())
        "androidTestImplementation"(libs.findLibrary("androidx.espresso.core").get())
    }
}