
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.devtools.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.service) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

buildscript {
    repositories {
        maven("https://jitpack.io") // activityGuard
    }
    dependencies {
        classpath(libs.stringfogPlugin)
        // 选用加解密算法库，默认实现了xor算法，也可以使用自己的加解密库。
        classpath(libs.xorLibrary)
//        classpath(libs.aabresguardPlugin) // AABResGuard 与 AGP 8.10.1 不兼容
//        classpath(libs.andresguardPlugin) // AndResGuard 与 AGP 8.10.1 不兼容
        classpath(libs.androidx.navigation.safeargs.plugin)
        classpath(libs.firebase.crashlytics.gradle)
        classpath("com.github.denglongfei:activityGuard:1.3.0") // Activity 混淆
    }
}

val taskNames = gradle.startParameter.taskNames
val hasInternalTask = taskNames.any { it.contains("Internal", ignoreCase = true) }
val hasOfficialTask = taskNames.any { it.contains("Official", ignoreCase = true) }
val hasAmbiguousReleaseTask = taskNames.any {
    val normalized = it.lowercase()
    normalized == "assemblerelease" ||
        normalized == "bundlerelease" ||
        normalized == "appdistributionuploadrelease"
}

check(!(hasAmbiguousReleaseTask && !hasInternalTask && !hasOfficialTask)) {
    "Ambiguous release task detected. Use channel-specific tasks such as assembleInternalRelease, assembleOfficialRelease, bundleOfficialRelease, or appDistributionUploadInternalRelease."
}

val selectedChannel = when {
    hasOfficialTask && !hasInternalTask -> "official"
    else -> "internal"
}

apply(from = file("app/src/$selectedChannel/config.gradle.kts"))
extra["selectedChannel"] = selectedChannel
logger.lifecycle("📦 [Channel] Selected channel config: $selectedChannel")

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    configurations.all {
        resolutionStrategy {
            force(libs.androidx.core.ktx)
            // 同时也强制核心 core 库，因为 core-ktx 依赖 core
            force("androidx.core:core:1.13.1")
        }
    }
}
