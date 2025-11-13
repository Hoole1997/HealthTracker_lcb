import com.android.build.api.dsl.DefaultConfig

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    // StringFog 字符串混淆插件
//    alias(libs.plugins.android.stringfog.convention)
}

val analyticsConfig = findProperty("analytics") as Map<*, *>

android {
    namespace = "net.corekit.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "DEFAULT_USER_CHANNEL", "\"${analyticsConfig["defaultUserChannel"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    api(libs.utilcodex)
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.fragment.ktx)
    implementation(libs.work.runtime)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    api(project(":framework"))



    api(libs.gson)
}