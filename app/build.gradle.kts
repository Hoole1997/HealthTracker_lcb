import com.android.build.api.dsl.DefaultConfig
import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import java.io.FileInputStream
import java.util.Properties

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

val configPropertiesFile = File(projectDir,"/assets/config.properties")
val configProperties = Properties().apply {
    load(FileInputStream(configPropertiesFile))
}

val isRelease = "true" == configProperties["stable_release"]

println("isRelease = $isRelease")

apply(from = "../config/sign.gradle")
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
        applicationId = "com.healthtracker.blood.suger"
        versionCode = 1
        versionName = "1.0"

        buildConfig {
            boolean("StableRelease", isRelease)
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
    implementation(libs.lottie)

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