plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // StringFog 字符串混淆插件
    alias(libs.plugins.android.stringfog.convention)
}

val adMobConfig = findProperty("admob") as Map<*, *>
val adMobUnitConfig = adMobConfig["adUnitIds"] as Map<*, *>

android {
    namespace = "net.corekit.monetize"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${adMobConfig["applicationId"]}\"")
        buildConfigField("String", "ADMOB_SPLASH_ID", "\"${adMobUnitConfig["splash"]}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${adMobUnitConfig["banner"]}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${adMobUnitConfig["interstitial"]}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"${adMobUnitConfig["native"]}\"")
        buildConfigField("String", "ADMOB_FULL_NATIVE_ID", "\"${adMobUnitConfig["full_native"]}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${adMobUnitConfig["rewarded"]}\"")
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
    api(libs.play.services.ads)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.lottie)
    compileOnly(project(":core"))
    api(project(":framework"))
    
    // 广告平台
    api("com.google.ads.mediation:facebook:6.20.0.0")
    api("com.google.ads.mediation:pangle:7.5.0.4.0")
    // ump
    api("com.google.android.ump:user-messaging-platform:3.1.0")
    api("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:0.21.0-beta01")
}
