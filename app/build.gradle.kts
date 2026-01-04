import com.android.build.api.dsl.DefaultConfig
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import kotlin.collections.get
import kotlin.collections.plusAssign

plugins {
    // 使用自定义插件
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.android.koin.convention)
    alias(libs.plugins.android.room.convention)
    alias(libs.plugins.android.firebase.convention)
    alias(libs.plugins.google.service)
    // StringFog 字符串混淆插件
    alias(libs.plugins.android.stringfog.convention)
    // 其他特殊插件
    alias(libs.plugins.kotlin.parcelize)
    kotlin("plugin.serialization")
    // Activity 混淆插件（仅 Release 构建启用；AGP 升级需重点回归 Manifest 是否正确更新）
    id("activityGuard")
}

// 引入统一的签名配置脚本
apply(from = "../scripts/sign.gradle")

// 引入动态混淆字典生成脚本
apply(from = "generate-dictionary.gradle.kts")

// 使用默认配置，避免不同变种间的冲突
val showLog = findProperty("app")?.let { (it as Map<*, *>)["show_log"] as Boolean } ?: false
val url = findProperty("url") as Map<*, *>
val adMobConfig = findProperty("admob") as Map<*, *>
val adMobUnitConfig = adMobConfig["adUnitIds"] as Map<*, *>
println("showLog = $showLog")

android {
    namespace = "com.daily.health.manager"

    defaultConfig {
        versionCode = 5
        versionName = "1.0.5"
        buildConfig {
            boolean("showLog", showLog)
        }

        buildConfigField("String", "PRIVACY_POLICY", "\"${url["privacyUrl"]}\"")
        buildConfigField("String", "FCM_URL", "\"${url["fcmUrl"]}\"")
        buildConfigField("String", "FCM_PKG", "\"${url["fcmPkg"]}\"")
        buildConfigField("String", "FEEDBACK_EMAIL", "\"${url["email"]}\"")
        buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${adMobConfig["applicationId"]}\"")
        buildConfigField("String", "ADMOB_SPLASH_ID", "\"${adMobUnitConfig["splash"]}\"")
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
        resConfigs("en","ja","ko")
    }

    sourceSets {
        getByName("main").java.srcDirs("build/generated/source/junk/kotlin")
    }

    // Ensure junk code is generated before compilation
    // Using afterEvaluate to ensure tasks are registered
    // Note: The script registers "generateJunkCode".
    // We hook it to preBuild

    



    // Flavor 配置
    flavorDimensions += "distribution"

    productFlavors {
        // 内部测试版本
        create("internal") {
            dimension = "distribution"
            applicationId = "com.daily.health.manager"
            versionNameSuffix = "-internal"
        }

        // Play 市场版本
        create("playstore") {
            dimension = "distribution"
            applicationId = "com.health.sugar.log.medication.pressure.manage.track.blood.tool"
            versionNameSuffix = ""
        }
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-obfuscation.pro"
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

configurations.all {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    api(project(":framework"))
    api(project(":monetize"))
    api(project(":core"))
    api(project(":metrics"))
    api(project(":appraise"))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.multidex)
    implementation(libs.material)
    implementation(libs.gson)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)

    // 其他特殊依赖
    implementation(libs.unPeekLiveData)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.webviewProgress)
    implementation(libs.work.runtime)
    implementation(libs.lifecycle.process)
    implementation(libs.imagepicker)
    implementation(libs.ucrop)

    // 毛玻璃模糊效果库
    implementation(libs.blur.veiw)

    implementation(libs.viewpagerindicator)
    implementation(libs.highlightpro)

    api(libs.flexbox)

    api(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.xxpermissions)
    implementation(libs.skeleton)
    implementation(libs.shimmerlayout)
    implementation(libs.views)
    implementation(libs.magicindicator)

    implementation(libs.play.review)
    implementation(libs.play.review.ktx)
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

// ==================== activityGuard 四大组件混淆配置 ====================
// 注意：历史上在部分 AGP 版本上出现过“类已混淆但 Manifest 未更新”的兼容性问题
// 当前策略：仅在 Release 构建任务时启用，便于集中回归验证
// 若后续升级 AGP/Gradle，请优先验证打包产物中的 Manifest 引用是否已同步更新
//actGuard {
//    isEnable = true
//    whiteClassList = hashSetOf(
//        "org.koin.*",
//        "com.google.firebase.*",
//        "com.google.android.gms.*",
//        "com.adjust.*",
//        "com.facebook.*",
//        "com.bytedance.*",
//        "cn.thinkingdata.*",
//    )
//    otherClassList = hashSetOf(
//        "com.daily.health.manager.ui.viewmodel.*",
//        "com.daily.health.manager.viewmodel.*",
//        "com.daily.health.manager.ui.weight.*",
//        "com.daily.health.manager.ui.widget.*",
//    )
//    changePackageList = hashSetOf(
//        "com.daily.health.manager.ui.viewmodel.*",
//        "com.daily.health.manager.viewmodel.*",
//    )
//    classNameCharPool = "abcdefghijklmnopqrstuvwxyz"
//    dirNameCharPool = "abcdefghijklmnopqrstuvwxyz"
//}
val enableActivityGuard = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

actGuard {
    isEnable = enableActivityGuard
    whiteClassList = hashSetOf(
        "androidx.core.content.FileProvider",
        "org.koin.*",
        "com.google.firebase.*",
        "com.google.android.gms.*",
        "com.adjust.*",
        "com.facebook.*",
        "com.bytedance.*",
        "cn.thinkingdata.*",
        "com.blankj.utilcode.util.*",
        "com.github.dhaval2404.imagepicker.*",
        "com.yalantis.ucrop.*",
        "com.daily.health.manager.face.act.*",
        "com.daily.health.manager.alarm.*",

    )
    otherClassList = hashSetOf(
        "com.daily.health.manager.App",
        "com.daily.health.manager.service.*",
        "com.daily.health.manager.provider.*",
        "com.daily.health.manager.ui.weight.*",
        "com.daily.health.manager.ui.widget.*",
        "com.daily.health.manager.receiver.*",
    )
    changePackageList = hashSetOf(
        "com.daily.health.manager.App",

        "com.daily.health.manager.service.*",
        "com.daily.health.manager.provider.*",
        "com.daily.health.manager.ui.weight.*",
        "com.daily.health.manager.ui.widget.*",
        "com.daily.health.manager.receiver.*",
    )
    classNameCharPool = "abcdefghijklmnopqrstuvwxyz"
    dirNameCharPool = "abcdefghijklmnopqrstuvwxyz"
}
