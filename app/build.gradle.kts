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

@Suppress("UNCHECKED_CAST")
fun configMap(name: String): Map<String, Any?> =
    rootProject.extra[name] as? Map<String, Any?>
        ?: error("Missing '$name' config. Expected root build script to load app/src/<channel>/config.gradle.kts")

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.nestedMap(name: String): Map<String, Any?> = this[name] as? Map<String, Any?> ?: emptyMap()

fun Map<String, Any?>.stringValue(name: String): String = this[name] as? String ?: ""

val admob = configMap("admob")
val admobUnit = admob.nestedMap("adUnitIds")
val gam = configMap("gam")
val gamUnit = gam.nestedMap("adUnitIds")
val pangle = configMap("pangle")
val pangleUnit = pangle.nestedMap("adUnitIds")
val topon = configMap("topon")
val toponUnit = topon.nestedMap("adUnitIds")
val appConfig = configMap("app")
val urls = configMap("url")
val analytics = configMap("analytics")

val showLog = appConfig["show_log"] as? Boolean ?: false
val defaultUserChannel = analytics["defaultUserChannel"] ?: "default"
val selectedChannel = rootProject.extra["selectedChannel"]?.toString() ?: "internal"
val defaultVersionName = "1.0.2"
val semanticVersion = project.findProperty("internalVersionName")?.toString()?.takeIf { it.isNotBlank() }
val resolvedVersionName = semanticVersion?.removePrefix("v") ?: defaultVersionName
val officialReleaseAabName = "${rootProject.name}_official_release_${resolvedVersionName}.aab"

println("📦 [Channel] Building '$selectedChannel' | Package: ${appConfig["applicationId"]}")

android {
    namespace = "com.daily.health.manager"

    defaultConfig {
        applicationId = appConfig.stringValue("applicationId")
        versionCode = 3
        versionName = resolvedVersionName

        if (semanticVersion != null) {
            println("🏷️ [Version] Override VersionName: $semanticVersion")
        }

        buildConfig {
            boolean("showLog", showLog)
        }

        buildConfigField("String", "PRIVACY_POLICY", "\"${urls["privacyUrl"]}\"")
        buildConfigField("String", "FCM_URL", "\"${urls["fcmUrl"]}\"")
        buildConfigField("String", "FCM_PKG", "\"${urls["fcmPkg"]}\"")
        buildConfigField("String", "FEEDBACK_EMAIL", "\"${urls["email"]}\"")
        buildConfigField("String", "DEFAULT_USER_CHANNEL", "\"$defaultUserChannel\"")
        buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${admob["applicationId"]}\"")
        buildConfigField("String", "ADMOB_SPLASH_ID", "\"${admobUnit["splash"]}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${admobUnit["banner"]}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${admobUnit["interstitial"]}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"${admobUnit["native"]}\"")
        buildConfigField("String", "ADMOB_FULL_NATIVE_ID", "\"${admobUnit["full_native"]}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${admobUnit["rewarded"]}\"")
        buildConfigField("String", "GAM_SPLASH_ID", "\"${gamUnit["splash"]}\"")
        buildConfigField("String", "GAM_BANNER_ID", "\"${gamUnit["banner"]}\"")
        buildConfigField("String", "GAM_INTERSTITIAL_ID", "\"${gamUnit["interstitial"]}\"")
        buildConfigField("String", "GAM_NATIVE_ID", "\"${gamUnit["native"]}\"")
        buildConfigField("String", "GAM_FULL_NATIVE_ID", "\"${gamUnit["full_native"]}\"")
        buildConfigField("String", "GAM_REWARDED_ID", "\"${gamUnit["rewarded"]}\"")
        buildConfigField("String", "PANGLE_APPLICATION_ID", "\"${pangle["applicationId"]}\"")
        buildConfigField("String", "PANGLE_SPLASH_ID", "\"${pangleUnit["splash"]}\"")
        buildConfigField("String", "PANGLE_BANNER_ID", "\"${pangleUnit["banner"]}\"")
        buildConfigField("String", "PANGLE_INTERSTITIAL_ID", "\"${pangleUnit["interstitial"]}\"")
        buildConfigField("String", "PANGLE_NATIVE_ID", "\"${pangleUnit["native"]}\"")
        buildConfigField("String", "PANGLE_FULL_NATIVE_ID", "\"${pangleUnit["full_native"]}\"")
        buildConfigField("String", "PANGLE_REWARDED_ID", "\"${pangleUnit["rewarded"]}\"")
        buildConfigField("String", "TOPON_APPLICATION_ID", "\"${topon["applicationId"]}\"")
        buildConfigField("String", "TOPON_APP_KEY", "\"${topon["appKey"]}\"")
        buildConfigField("String", "TOPON_SPLASH_ID", "\"${toponUnit["splash"]}\"")
        buildConfigField("String", "TOPON_BANNER_ID", "\"${toponUnit["banner"]}\"")
        buildConfigField("String", "TOPON_INTERSTITIAL_ID", "\"${toponUnit["interstitial"]}\"")
        buildConfigField("String", "TOPON_NATIVE_ID", "\"${toponUnit["native"]}\"")
        buildConfigField("String", "TOPON_FULL_NATIVE_ID", "\"${toponUnit["full_native"]}\"")
        buildConfigField("String", "TOPON_REWARDED_ID", "\"${toponUnit["rewarded"]}\"")

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
        getByName("main").java.srcDirs("build/generated/source/junk/kotlin")
    }

    flavorDimensions += "channel"

    productFlavors {
        create("internal") {
            dimension = "channel"
            if (semanticVersion == null) {
                versionNameSuffix = "-internal"
            }
        }
        create("official") {
            dimension = "channel"
        }
    }

    buildTypes {
        release {
            isShrinkResources = false
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

tasks.register("printOfficialReleaseVersionName") {
    group = "help"
    description = "Prints the versionName used for official release builds."
    inputs.property("resolvedVersionName", resolvedVersionName)
    doLast {
        println(inputs.properties["resolvedVersionName"])
    }
}

tasks.register("printOfficialReleaseAabName") {
    group = "help"
    description = "Prints the expected output file name for official release AAB builds."
    inputs.property("officialReleaseAabName", officialReleaseAabName)
    doLast {
        println(inputs.properties["officialReleaseAabName"])
    }
}


dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    api(project(":framework"))
    implementation(libs.remax.core)
    implementation(libs.remax.bill)
    add("internalImplementation", "com.launcher.unity:com.leafmotivation.quizguessoncolor-health-variants-1:1.0.5") {
        exclude(group = "com.unity3d.ads-mediation", module = "mediation-sdk")
    }
    add("officialImplementation", "com.launcher.unity:com.blood.pressure.health.monitor.tool-health-release:1.0.5") {
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
