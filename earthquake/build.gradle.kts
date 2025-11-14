plugins {
    // 使用项目的约定库插件，统一 Android/Kotlin 配置
    alias(libs.plugins.android.library.convention)
}

android {
    namespace = "com.healthtracker.earthquake"

    // 保持默认构建类型即可；消费者规则见 consumer-rules.pro
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // 基础依赖，保持模块独立且可编译
    implementation(libs.androidx.core.ktx)
    // WorkManager 用于在广播触发时执行后台任务
    implementation(libs.work.runtime)
    // 协程支持（CoroutineWorker 以及 IO 切换）
    implementation(libs.kotlinx.coroutines.android)
}
