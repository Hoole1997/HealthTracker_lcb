import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.android.build.api.dsl.DefaultConfig
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import kotlin.collections.get
import kotlin.collections.plusAssign

plugins {
    // 使用自定义插件
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.firebase.appdistribution)
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

// ==========================================
// 🚀 三层解耦配置加载器 (The Shifter)
// ==========================================
val remoteOverride = project.hasProperty("remoteOverride")
val shifterFile = if (remoteOverride) "../scripts/official.gradle" else "../scripts/internal.gradle"

// 1. 加载本地默认配置 (Internal 第一副本)
apply(from = "../scripts/internal.gradle")

// 2. 尝试加载远程补丁 (外网/官方) - 本地路径作为演示，实际 CI 会动态下载
if (remoteOverride) {
    println("📡 [Shifter] Detected Remote Override Mode. Applying official config...")
    apply(from = shifterFile)
}

// 提取 ext 变量以供后续引用
val admob = extensions.extraProperties["admob"] as Map<*, *>
val admobUnit = admob["adUnitIds"] as Map<*, *>
val appConfig = extensions.extraProperties["app"] as Map<*, *>
val urls = extensions.extraProperties["url"] as Map<*, *>
val analytics = extensions.extraProperties["analytics"] as Map<*, *>

val showLog = appConfig["show_log"] as Boolean
val shifterMode = appConfig["shifter_mode"] ?: "internal"
println("📦 [Shifter] Build Mode: $shifterMode | Package: ${appConfig["applicationId"]}")

android {
    namespace = "com.daily.health.manager"

    defaultConfig {
        applicationId = appConfig["applicationId"] as String
        versionCode = 7
        versionName = "1.0.7"
        
        // 🚀 动态支持从属性注入 versionName (用于 CI Tag 构建)
        val semanticVersion = project.findProperty("internalVersionName")?.toString()
        if (semanticVersion != null && semanticVersion.isNotEmpty()) {
            println("🏷️ [Shifter] Override VersionName: $semanticVersion")
            versionName = semanticVersion.removePrefix("v")
        }

        buildConfig {
            boolean("showLog", showLog)
        }

        buildConfigField("String", "PRIVACY_POLICY", "\"${urls["privacyUrl"]}\"")
        buildConfigField("String", "FCM_URL", "\"${urls["fcmUrl"]}\"")
        buildConfigField("String", "FCM_PKG", "\"${urls["fcmPkg"]}\"")
        buildConfigField("String", "FEEDBACK_EMAIL", "\"${urls["email"]}\"")
        buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${admob["applicationId"]}\"")
        buildConfigField("String", "ADMOB_SPLASH_ID", "\"${admobUnit["splash"]}\"")
        
        // 动态设置版本名后缀 (仅内网模式且没有注入 semanticVersion 时)
        if (shifterMode == "internal" && semanticVersion == null) {
            versionNameSuffix = "-internal"
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
        resConfigs("en", "es", "pt-rBR", "ja", "ko", "hi", "tr", "de", "fr", "it")
    }

    sourceSets {
        getByName("main").java.srcDirs("build/generated/source/junk/kotlin")
    }

    // Ensure junk code is generated before compilation
    // Using afterEvaluate to ensure tasks are registered
    // Note: The script registers "generateJunkCode".
    // We hook it to preBuild

    



    // 🚀 已移除旧有的 productFlavors，实现零变种构建



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

            // 统一配置 Firebase App Distribution
            firebaseAppDistribution {
                // 🚀 同时兼容 FIREBASE_APP_ID 和 INTERNAL_FIREBASE_APP_ID (CI 中使用的名称)
                appId = System.getenv("FIREBASE_APP_ID") ?: System.getenv("INTERNAL_FIREBASE_APP_ID") ?: ""
                serviceCredentialsFile = rootProject.file("scripts/google-services-json-key.json").absolutePath
                releaseNotesFile = rootProject.file("release_notes.txt").absolutePath
                groups = "internal-testers"
            }
        }
    }


    // 设置APK输出文件名
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "${rootProject.name}-${variant.baseName}-${variant.versionName}.apk"
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

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}


dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    api(project(":framework"))
    api(project(":monetize"))
    api(project(":core"))
    api(project(":metrics"))
    api(project(":earthquake"))
    api(project(":weather"))
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

    // CameraX (PPG 心率测量)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    
    // Lottie Compose (心率测量动画)
    implementation(libs.lottie.compose)
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
    isEnable = false
    whiteClassList = hashSetOf(
        "androidx.**",
        "org.koin.**",
        "com.google.**",
        "com.adjust.**",
        "com.facebook.**",
        "com.bytedance.**",
        "cn.thinkingdata.**",
        "com.blankj.utilcode.**",
        "com.github.dhaval2404.imagepicker.**",
        "com.yalantis.ucrop.**",
        "com.thinkup.**",
        "sg.bigo.**",
        "com.applovin.**",
        "com.pangle.**",
        "com.mbridge.**",
        "com.tradplusad.**",
        "com.vungle.**",
    )
    otherClassList = hashSetOf(
        "com.daily.health.manager.App",
        "com.daily.health.manager.service.**",
        "com.daily.health.manager.provider.**",
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
