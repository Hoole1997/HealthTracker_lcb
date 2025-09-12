
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
    alias(libs.plugins.hilt) apply false
}

buildscript {
    dependencies {
        classpath(libs.stringfogPlugin)
        // 选用加解密算法库，默认实现了xor算法，也可以使用自己的加解密库。
        classpath(libs.xorLibrary)
//        classpath(libs.aabresguardPlugin)
        classpath(libs.androidx.navigation.safeargs.plugin)
        classpath(libs.firebase.crashlytics.gradle)

    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}