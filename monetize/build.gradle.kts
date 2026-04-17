plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // StringFog 字符串混淆插件
    alias(libs.plugins.android.stringfog.convention)
}

// AdMob 配置
val adMobConfig = findProperty("admob") as Map<*, *>
val adMobUnitConfig = adMobConfig["adUnitIds"] as Map<*, *>

// Pangle 配置（从 config.gradle 的 ext 读取）
val pangleConfig = findProperty("pangle") as? Map<*, *>
val pangleUnitConfig = pangleConfig?.get("adUnitIds") as? Map<*, *>

// TopOn 配置（从 config.gradle 的 ext 读取）
val toponConfig = findProperty("topon") as? Map<*, *>
val toponUnitConfig = toponConfig?.get("adUnitIds") as? Map<*, *>


android {
    namespace = "net.corekit.monetize"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        
        // ==================== AdMob BuildConfig ====================
        buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${adMobConfig["applicationId"]}\"")
        buildConfigField("String", "ADMOB_SPLASH_ID", "\"${adMobUnitConfig["splash"]}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${adMobUnitConfig["banner"]}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${adMobUnitConfig["interstitial"]}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"${adMobUnitConfig["native"]}\"")
        buildConfigField("String", "ADMOB_FULL_NATIVE_ID", "\"${adMobUnitConfig["full_native"]}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${adMobUnitConfig["rewarded"]}\"")
        buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"${adMobUnitConfig["rewarded_interstitial"]}\"")
        
        // ==================== Pangle BuildConfig ====================
        buildConfigField("String", "PANGLE_APPLICATION_ID", "\"${pangleConfig?.get("applicationId") ?: ""}\"")
        buildConfigField("String", "PANGLE_SPLASH_ID", "\"${pangleUnitConfig?.get("splash") ?: ""}\"")
        buildConfigField("String", "PANGLE_BANNER_ID", "\"${pangleUnitConfig?.get("banner") ?: ""}\"")
        buildConfigField("String", "PANGLE_INTERSTITIAL_ID", "\"${pangleUnitConfig?.get("interstitial") ?: ""}\"")
        buildConfigField("String", "PANGLE_NATIVE_ID", "\"${pangleUnitConfig?.get("native") ?: ""}\"")
        buildConfigField("String", "PANGLE_FULL_NATIVE_ID", "\"${pangleUnitConfig?.get("full_native") ?: ""}\"")
        buildConfigField("String", "PANGLE_REWARDED_ID", "\"${pangleUnitConfig?.get("rewarded") ?: ""}\"")
        
        // ==================== TopOn BuildConfig ====================
        buildConfigField("String", "TOPON_APPLICATION_ID", "\"${toponConfig?.get("applicationId") ?: ""}\"")
        buildConfigField("String", "TOPON_APP_KEY", "\"${toponConfig?.get("appKey") ?: ""}\"")
        buildConfigField("String", "TOPON_SPLASH_ID", "\"${toponUnitConfig?.get("splash") ?: ""}\"")
        buildConfigField("String", "TOPON_BANNER_ID", "\"${toponUnitConfig?.get("banner") ?: ""}\"")
        buildConfigField("String", "TOPON_INTERSTITIAL_ID", "\"${toponUnitConfig?.get("interstitial") ?: ""}\"")
        buildConfigField("String", "TOPON_NATIVE_ID", "\"${toponUnitConfig?.get("native") ?: ""}\"")
        buildConfigField("String", "TOPON_FULL_NATIVE_ID", "\"${toponUnitConfig?.get("full_native") ?: ""}\"")
        buildConfigField("String", "TOPON_REWARDED_ID", "\"${toponUnitConfig?.get("rewarded") ?: ""}\"")
        
        
        // 将 AdMob APPLICATION_ID 传递到 AndroidManifest.xml
        manifestPlaceholders["ADMOB_APPLICATION_ID"] = adMobConfig["applicationId"] as String
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

    api("com.launcher.unity:com.blood.pressure.health.monitor.tool-health-release:1.0.0")

    // ==================== 核心依赖 ====================
    api(libs.play.services.ads)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.lottie)
    compileOnly(project(":core"))
    api(project(":framework"))
    
    // ==================== AdMob 相关 ====================
//    api("com.google.ads.mediation:facebook:6.20.0.0")
//    api("com.google.ads.mediation:pangle:7.2.0.6.0")
    // UMP
    api("com.google.android.ump:user-messaging-platform:3.1.0")
    
    // Glide - 用于原生广告图标加载
    api(libs.glide)
    
    // ==================== Pangle 聚合 SDK ====================
    api("com.pangle.global:pag-sdk-m:7.5.6.2")
    api("com.pangle.global:admob-adapter:24.4.0.5")
    api("com.pangle.global:mintegral-adapter:16.9.91.1")
    api("com.pangle.global:google-ad-manager-adapter:24.5.0.3")
    
    // ==================== TopOn 聚合 SDK ====================
//    api("com.thinkup.sdk:core-tpn:6.5.16")
//    api("com.thinkup.sdk:interstitial-tpn:6.5.16")
//    api("com.thinkup.sdk:rewardedvideo-tpn:6.5.16")
//    api("com.thinkup.sdk:nativead-tpn:6.5.16")
//    api("com.thinkup.sdk:banner-tpn:6.5.16")
//    api("com.thinkup.sdk:splash-tpn:6.5.16")
    
    // TopOn 三方适配器
    api("com.thinkup.sdk:adapter-tpn-vungle:6.5.16")
    api("com.vungle:vungle-ads:7.5.0")
    api("com.thinkup.sdk:adapter-tpn-bigo:6.5.16.1")
    api("com.bigossp:bigo-ads:5.5.1")
    api("com.thinkup.sdk:adapter-tpn-pangle:6.5.16.2")
    api("com.thinkup.sdk:adapter-tpn-facebook:6.5.16")
    api("com.facebook.android:audience-network-sdk:6.20.0")
    // 已移除: TopOn AdMob Adapter 与项目 AdMob SDK 版本冲突 (当前项目统一回退到经典 GMA 24.9.0)
    // 项目已直接使用 AdMob SDK，TopOn 使用其他广告源即可
    // api("com.thinkup.sdk:adapter-tpn-admob:6.5.16")
    api("com.thinkup.sdk:adapter-tpn-mintegral:6.5.16.1")
    api("com.mbridge.msdk.oversea:mbridge_android_sdk:16.9.91")
    api("com.thinkup.sdk:tramini-plugin-tpn:6.5.16")
    api("com.thinkup.sdk:adapter-tpn-ironsource:8.10.0.1.0")
}
