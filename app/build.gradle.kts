import com.android.build.api.dsl.DefaultConfig
import com.github.megatronking.stringfog.plugin.StringFogExtension
import com.github.megatronking.stringfog.plugin.StringFogMode
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import org.apache.commons.io.output.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.service)
    id("stringfog")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.devtools.ksp)
    id("com.google.firebase.crashlytics")
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
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.healthtracker.blood.suger"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }

        vectorDrawables {
            useSupportLibrary = true
        }
        multiDexEnabled = true


        buildConfig {
            boolean("StableRelease",isRelease)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11

    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }



}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.xorLibrary)
    implementation(libs.gson)
    implementation(libs.multidex)

    implementation(libs.lottie)
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)

    implementation(libs.unPeekLiveData)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.webviewProgress)

    implementation(libs.spwaitkiller)
    implementation(libs.hiddenapibypass)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics.ktx)

//    if(isRelease){
//        println("******************* [HIDE LOG] *******************")
//        releaseApi(libs.log.release)
//    }else{
//        println("******************** SHOW LOG ********************")
//    }
}

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

    // 可以根据需要添加更多类型

    fun create(config: DefaultConfig) {
        fields.forEach { (type, name, value) ->
            config.buildConfigField(type, name, value)
        }
    }
}
