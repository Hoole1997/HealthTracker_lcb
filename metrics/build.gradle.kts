plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // StringFog 字符串混淆插件
    alias(libs.plugins.android.stringfog.convention)
}

val analyticsConfig = findProperty("analytics") as Map<*, *>

android {
    namespace = "net.corekit.metrics"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"${analyticsConfig["adjustAppToken"]}\"")
        buildConfigField("String", "THINKING_DATA_APP_ID", "\"${analyticsConfig["thinkingDataAppId"]}\"")
        buildConfigField("String", "THINKING_DATA_SERVER_URL", "\"${analyticsConfig["thinkingDataServerUrl"]}\"")
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
    compileOnly(libs.remax.core)
    api(project(":framework"))
    // Adjust SDK
    api("com.adjust.sdk:adjust-android:5.4.3")
    api("com.android.installreferrer:installreferrer:2.2")
    api("com.google.android.gms:play-services-ads-identifier:18.0.1")
    
    // ThinkingData SDK
    api("cn.thinkingdata.android:ThinkingAnalyticsSDK:3.2.3")
}
