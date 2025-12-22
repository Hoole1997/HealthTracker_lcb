
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

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// 根据构建任务自动选择配置文件
val taskNames = gradle.startParameter.taskNames
val configFile = when {
    taskNames.any { it.contains("Playstore") && !it.contains("Internal") } -> file("app/src/playstore/config.gradle")
    taskNames.any { it.contains("Internal") && !it.contains("Playstore") } -> file("app/src/internal/config.gradle")
    // 如果同时包含多个变种或者是 assembleRelease，使用更通用的配置方式
    else -> file("app/src/internal/config.gradle") // 默认使用内部测试配置
}

apply {
    from(configFile)
}