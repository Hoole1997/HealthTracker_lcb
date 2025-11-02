plugins {
    alias(libs.plugins.android.library.convention)
}

android {
    namespace = "net.corekit.adsdk"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // AdMob SDK (新版 API)
    api("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:0.21.0-beta01")

    // Kotlin 协程
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    // Android 基础库
    api(libs.androidx.appcompat)
    api(libs.androidx.lifecycle.runtime.ktx)

    // MMKV for persistent metrics storage
    api(libs.mmkv)

    // 可选：核心模块依赖（用于日志和配置）
    compileOnly(project(":core"))

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.13")
}
