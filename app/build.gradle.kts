import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.VariantDimension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

data class ChannelConfig(
    val name: String,
    val launcherUnityDependency: String,
    val admob: Map<*, *>,
    val admobUnit: Map<*, *>,
    val gam: Map<*, *>,
    val gamUnit: Map<*, *>,
    val pangle: Map<*, *>,
    val pangleUnit: Map<*, *>,
    val topon: Map<*, *>,
    val toponUnit: Map<*, *>,
    val app: Map<*, *>,
    val urls: Map<*, *>,
    val analytics: Map<*, *>,
)

fun loadChannelConfig(
    name: String,
    scriptPath: String,
    launcherUnityDependency: String,
): ChannelConfig {
    project.apply(from = scriptPath)

    val admob = extensions.extraProperties["admob"] as Map<*, *>
    val gam = extensions.extraProperties["gam"] as Map<*, *>
    val pangle = extensions.extraProperties["pangle"] as Map<*, *>
    val topon = extensions.extraProperties["topon"] as Map<*, *>

    return ChannelConfig(
        name = name,
        launcherUnityDependency = launcherUnityDependency,
        admob = admob,
        admobUnit = admob["adUnitIds"] as Map<*, *>,
        gam = gam,
        gamUnit = gam["adUnitIds"] as Map<*, *>,
        pangle = pangle,
        pangleUnit = pangle["adUnitIds"] as Map<*, *>,
        topon = topon,
        toponUnit = topon["adUnitIds"] as Map<*, *>,
        app = extensions.extraProperties["app"] as Map<*, *>,
        urls = extensions.extraProperties["url"] as Map<*, *>,
        analytics = extensions.extraProperties["analytics"] as Map<*, *>,
    )
}

val internalChannel = loadChannelConfig(
    name = "internal",
    scriptPath = "../scripts/internal.gradle",
    launcherUnityDependency = "com.launcher.unity:com.leafmotivation.quizguessoncolor-BloodPressureLog:1.0.5",
)
val officialChannel = loadChannelConfig(
    name = "official",
    scriptPath = "../scripts/official.gradle",
    launcherUnityDependency = "com.launcher.unity:com.healthlab.heartrate.bloodpressuretracker-release:1.0.2",
)

val semanticVersion = project.findProperty("internalVersionName")
    ?.toString()
    ?.takeIf { it.isNotEmpty() }
val defaultVersionName = "1.0.4"
val resolvedVersionName = semanticVersion?.removePrefix("v") ?: defaultVersionName
val buildTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
println("📦 [Flavor] Internal Package: ${internalChannel.app["applicationId"]}")
println("📦 [Flavor] Official Package: ${officialChannel.app["applicationId"]}")

android {
    namespace = "com.daily.health.manager"

    defaultConfig {
        versionCode = 5
        versionName = resolvedVersionName
        if (semanticVersion != null) {
            println("🏷️ [Flavor] Override VersionName: $resolvedVersionName")
        }

        setProperty("archivesBaseName", "${rootProject.name}-v${versionName}(${versionCode})_${buildTime}")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
//        resConfigs("en", "es", "pt-rBR", "ja", "ko", "hi", "tr", "de", "fr", "it")
    }

    sourceSets {
        getByName("main").java.srcDir("build/generated/source/junk/kotlin")
    }

    // Ensure junk code is generated before compilation
    // Using afterEvaluate to ensure tasks are registered
    // Note: The script registers "generateJunkCode".
    // We hook it to preBuild

    



    flavorDimensions += "channel"
    productFlavors {
        create("internal") {
            dimension = "channel"
            configureChannel(internalChannel)
            if (semanticVersion == null) {
                versionNameSuffix = "-internal"
            }
        }
        create("official") {
            dimension = "channel"
            configureChannel(officialChannel)
        }
    }

    buildTypes {
        release {
//            isShrinkResources = true
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
    implementation(libs.remax.core)
    implementation(libs.remax.bill)
    add("internalImplementation", internalChannel.launcherUnityDependency) {
        exclude(group = "com.unity3d.ads-mediation", module = "mediation-sdk")
    }
    add("officialImplementation", officialChannel.launcherUnityDependency) {
        exclude(group = "com.unity3d.ads-mediation", module = "mediation-sdk")
    }
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

fun ApplicationProductFlavor.configureChannel(config: ChannelConfig) {
    applicationId = config.app["applicationId"] as String
    addChannelBuildConfig(config)
}

fun VariantDimension.addChannelBuildConfig(config: ChannelConfig) {
    val defaultUserChannel = config.analytics["defaultUserChannel"] ?: "default"

    buildConfigField("boolean", "showLog", (config.app["show_log"] as Boolean).toString())
    buildConfigField("String", "PRIVACY_POLICY", "\"${config.urls["privacyUrl"]}\"")
    buildConfigField("String", "FCM_URL", "\"${config.urls["fcmUrl"]}\"")
    buildConfigField("String", "FCM_PKG", "\"${config.urls["fcmPkg"]}\"")
    buildConfigField("String", "FEEDBACK_EMAIL", "\"${config.urls["email"]}\"")
    buildConfigField("String", "DEFAULT_USER_CHANNEL", "\"$defaultUserChannel\"")
    buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${config.admob["applicationId"]}\"")
    buildConfigField("String", "ADMOB_SPLASH_ID", "\"${config.admobUnit["splash"]}\"")
    buildConfigField("String", "ADMOB_BANNER_ID", "\"${config.admobUnit["banner"]}\"")
    buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${config.admobUnit["interstitial"]}\"")
    buildConfigField("String", "ADMOB_NATIVE_ID", "\"${config.admobUnit["native"]}\"")
    buildConfigField("String", "ADMOB_FULL_NATIVE_ID", "\"${config.admobUnit["full_native"]}\"")
    buildConfigField("String", "ADMOB_REWARDED_ID", "\"${config.admobUnit["rewarded"]}\"")
    buildConfigField("String", "GAM_SPLASH_ID", "\"${config.gamUnit["splash"]}\"")
    buildConfigField("String", "GAM_BANNER_ID", "\"${config.gamUnit["banner"]}\"")
    buildConfigField("String", "GAM_INTERSTITIAL_ID", "\"${config.gamUnit["interstitial"]}\"")
    buildConfigField("String", "GAM_NATIVE_ID", "\"${config.gamUnit["native"]}\"")
    buildConfigField("String", "GAM_FULL_NATIVE_ID", "\"${config.gamUnit["full_native"]}\"")
    buildConfigField("String", "GAM_REWARDED_ID", "\"${config.gamUnit["rewarded"]}\"")
    buildConfigField("String", "PANGLE_APPLICATION_ID", "\"${config.pangle["applicationId"]}\"")
    buildConfigField("String", "PANGLE_SPLASH_ID", "\"${config.pangleUnit["splash"]}\"")
    buildConfigField("String", "PANGLE_BANNER_ID", "\"${config.pangleUnit["banner"]}\"")
    buildConfigField("String", "PANGLE_INTERSTITIAL_ID", "\"${config.pangleUnit["interstitial"]}\"")
    buildConfigField("String", "PANGLE_NATIVE_ID", "\"${config.pangleUnit["native"]}\"")
    buildConfigField("String", "PANGLE_FULL_NATIVE_ID", "\"${config.pangleUnit["full_native"]}\"")
    buildConfigField("String", "PANGLE_REWARDED_ID", "\"${config.pangleUnit["rewarded"]}\"")
    buildConfigField("String", "TOPON_APPLICATION_ID", "\"${config.topon["applicationId"]}\"")
    buildConfigField("String", "TOPON_APP_KEY", "\"${config.topon["appKey"]}\"")
    buildConfigField("String", "TOPON_SPLASH_ID", "\"${config.toponUnit["splash"]}\"")
    buildConfigField("String", "TOPON_BANNER_ID", "\"${config.toponUnit["banner"]}\"")
    buildConfigField("String", "TOPON_INTERSTITIAL_ID", "\"${config.toponUnit["interstitial"]}\"")
    buildConfigField("String", "TOPON_NATIVE_ID", "\"${config.toponUnit["native"]}\"")
    buildConfigField("String", "TOPON_FULL_NATIVE_ID", "\"${config.toponUnit["full_native"]}\"")
    buildConfigField("String", "TOPON_REWARDED_ID", "\"${config.toponUnit["rewarded"]}\"")
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
        "com.daily.health.manager.face.weight.*",
        "com.daily.health.manager.face.widget.*",
        "com.daily.health.manager.receiver.*",
    )
    changePackageList = hashSetOf(
        "com.daily.health.manager.App",

        "com.daily.health.manager.service.*",
        "com.daily.health.manager.provider.*",
        "com.daily.health.manager.face.weight.*",
        "com.daily.health.manager.face.widget.*",
        "com.daily.health.manager.receiver.*",
    )
    classNameCharPool = "abcdefghijklmnopqrstuvwxyz"
    dirNameCharPool = "abcdefghijklmnopqrstuvwxyz"
}
