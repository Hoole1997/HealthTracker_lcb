plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // StringFog 字符串混淆插件
    alias(libs.plugins.android.stringfog.convention)
}

fun loadAnalyticsConfig(scriptPath: String): Map<*, *> {
    project.apply(from = scriptPath)
    return extensions.extraProperties["analytics"] as Map<*, *>
}

val internalAnalyticsConfig = loadAnalyticsConfig("../scripts/internal.gradle")
val officialAnalyticsConfig = loadAnalyticsConfig("../scripts/official.gradle")

android {
    namespace = "net.corekit.metrics"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    flavorDimensions += "channel"
    productFlavors {
        create("internal") {
            dimension = "channel"
            configureAnalytics(internalAnalyticsConfig)
        }
        create("official") {
            dimension = "channel"
            configureAnalytics(officialAnalyticsConfig)
        }
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

fun com.android.build.api.dsl.LibraryProductFlavor.configureAnalytics(analyticsConfig: Map<*, *>) {
    buildConfigField("String", "ADJUST_APP_TOKEN", "\"${analyticsConfig["adjustAppToken"]}\"")
    buildConfigField("String", "THINKING_DATA_APP_ID", "\"${analyticsConfig["thinkingDataAppId"]}\"")
    buildConfigField("String", "THINKING_DATA_SERVER_URL", "\"${analyticsConfig["thinkingDataServerUrl"]}\"")
}
