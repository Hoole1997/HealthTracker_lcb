import com.android.build.api.dsl.DefaultConfig
import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

plugins {
    // 使用自定义插件
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.android.hilt.convention)
    alias(libs.plugins.android.room.convention)
    alias(libs.plugins.android.firebase.convention)
    // 特殊插件保留
    id("stringfog")
    alias(libs.plugins.kotlin.parcelize)
    kotlin("plugin.serialization")
}

// 引入统一的签名配置脚本
apply(from = "../scripts/sign.gradle")

// 使用默认配置，避免不同变种间的冲突
val isRelease = findProperty("app")?.let { (it as Map<*, *>)["stable_release"] as Boolean } ?: false


println("isRelease = $isRelease")
configure<StringFogExtension> {
    // 必要：加解密库的实现类路径，需和上面配置的加解密算法库一致。
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    // 可选：加密开关，默认开启。
    enable = isRelease
    // 可选：指定需加密的代码包路径，可配置多个，未指定将默认全部加密。
    // fogPackages = arrayOf("com.xxx.xxx")
    kg = RandomKeyGenerator()
    // base64或者bytes
    mode = StringFogMode.bytes
}




android {
    namespace = "com.healthtracker.blood.suger"

    defaultConfig {
        versionCode = 1
        versionName = "1.0"
        buildConfig {
            boolean("StableRelease", isRelease)
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

    }

    // Flavor 配置
    flavorDimensions += "distribution"

    productFlavors {
        // 内部测试版本
        create("internal") {
            dimension = "distribution"
            applicationId = "com.healthtracker.blood.suger.internal"
            versionNameSuffix = "-internal"
        }

        // Play 市场版本
        create("playstore") {
            dimension = "distribution"
            applicationId = "com.healthtracker.blood.suger"
            versionNameSuffix = ""
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
    }


    // 设置APK输出文件名
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "HealthTracker-${variant.baseName}-${variant.versionName}.apk"
                output.outputFileName = outputFileName
            }
    }

    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

// 调用统一签名配置脚本设置签名
apply<Any> {
    extensions.extraProperties["setupSigningConfigs"]?.let { setupFn ->
        if (setupFn is groovy.lang.Closure<*>) {
            setupFn.call(android)
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    api(project(":framework"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.xorLibrary)
    implementation(libs.multidex)
    implementation(libs.material)
    implementation(libs.lottie)
    implementation(libs.gson)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    // 其他特殊依赖
    implementation(libs.unPeekLiveData)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.webviewProgress)
    implementation(libs.spwaitkiller)
    implementation(libs.hiddenapibypass)
    implementation(libs.work.runtime)
    implementation(libs.lifecycle.process)
}

// BuildConfig 扩展函数
fun DefaultConfig.buildConfig(configure: BuildConfigFieldsBuilder.() -> Unit) {
    BuildConfigFieldsBuilder().apply(configure).create(this)
}

class BuildConfigFieldsBuilder {
    private val fields = mutableListOf<Triple<String, String, String>>()

    fun boolean(name: String, value: Boolean) {
        fields.add(Triple("boolean", name, value.toString()))
    }

    fun string(name: String, value: String) {
        fields.add(Triple("String", name, "\"$value\""))
    }

    fun int(name: String, value: Int) {
        fields.add(Triple("int", name, value.toString()))
    }

    fun long(name: String, value: Long) {
        fields.add(Triple("long", name, value.toString()))
    }

    fun create(config: DefaultConfig) {
        fields.forEach { (type, name, value) ->
            config.buildConfigField(type, name, value)
        }
    }
}