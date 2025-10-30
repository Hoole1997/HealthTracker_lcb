plugins {
    // 使用自定义插件
    alias(libs.plugins.android.library.convention)
    alias(libs.plugins.android.hilt.convention)
}

android {
    namespace = "com.healthtracker.framework"

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.process)

    // Activity
    api(libs.androidx.activity.ktx)

    // 数据存储
    implementation(libs.mmkv)
    implementation(libs.gson)

    // Firebase - API 导出给其他模块使用
    api(platform(libs.firebase.bom))
    api(libs.firebase.config)
    api(libs.firebase.analytics.ktx)
    api(libs.firebase.crashlytics.ktx)
    api(libs.firebase.perf.ktx)
    api(libs.firebase.messaging)
    api(libs.utilcodex)
    api(libs.xorLibrary)
}