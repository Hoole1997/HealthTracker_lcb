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
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)

    // Kotlin Coroutines
    api(libs.kotlinx.coroutines.android)

    // Lifecycle
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.lifecycle.process)

    api(libs.androidx.constraintlayout)
    // Activity
    api(libs.androidx.activity.ktx)

    // 数据存储
    api(libs.mmkv)
    api(libs.gson)

    // Firebase - API 导出给其他模块使用
    api(platform(libs.firebase.bom))
    api(libs.firebase.config)
    api(libs.firebase.analytics.ktx)
    api(libs.firebase.crashlytics.ktx)
    api(libs.firebase.perf.ktx)
    api(libs.firebase.messaging)
    api(libs.utilcodex)
    api(libs.xorLibrary)
    implementation(libs.xxpermissions)
    api("com.github.getActivity:ShapeDrawable:3.3")
    api("com.github.getActivity:Toaster:13.8")
}