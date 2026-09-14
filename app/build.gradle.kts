import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import convention.config.configureChannel
import convention.config.configureReleaseSigning
import convention.config.loadChannelConfig
import convention.config.registerReleaseMetadata
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension

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

// 引入动态混淆字典生成脚本
apply(from = "generate-dictionary.gradle.kts")

// 渠道参数和版本由各自的 config.gradle 管理，应用脚本只负责组装。
val localChannel = loadChannelConfig("local", project)
val googleChannel = loadChannelConfig("google", project)

android {
    namespace = "com.daily.health.manager"

    defaultConfig {
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

    flavorDimensions += "channel"
    productFlavors {
        create("local") {
            dimension = "channel"
            isDefault = true
            configureChannel(localChannel, project)
        }
        create("google") {
            dimension = "channel"
            configureChannel(googleChannel, project)
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
                serviceCredentialsFile = rootProject.file("signing/google-services-json-key.json").absolutePath
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

// 签名及 CI 元数据独立于业务依赖，避免构建文件继续堆积工具逻辑。
configureReleaseSigning(android)
registerReleaseMetadata(android)

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}


dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    api(project(":framework"))
    implementation(libs.remax.core)
    implementation(libs.remax.bill)
    add("localImplementation", "com.launcher.unity:com.leafmotivation.quizguessoncolor-Health4:1.0.0") {
        exclude(group = "com.unity3d.ads-mediation", module = "mediation-sdk")
    }
    add("googleImplementation", "com.launcher.unity:com.healthlab.heartrate.bloodpressuretracker-release:1.0.3") {
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
