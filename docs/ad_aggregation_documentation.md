# 广告多聚合竞价实现方案文档

> 本文档详细记录了当前项目中 **AdMob、Pangle、TopOn** 三平台广告聚合竞价的完整实现方案，可作为其他项目接入相同功能的参考。

---

## 目录

1. [架构概述](#1-架构概述)
2. [项目结构](#2-项目结构)
3. [依赖配置](#3-依赖配置)
4. [SDK 初始化](#4-sdk-初始化)
5. [平台配置机制](#5-平台配置机制)
6. [广告控制器设计](#6-广告控制器设计)
7. [竞价机制实现](#7-竞价机制实现)
8. [广告展示扩展层](#8-广告展示扩展层)
9. [预加载机制](#9-预加载机制)
10. [收益获取方式](#10-收益获取方式)
11. [接入流程清单](#11-接入流程清单)

---

## 1. 架构概述

### 1.1 设计目标

- **多平台竞价**：支持 AdMob、Pangle、TopOn 三个广告平台同时请求广告
- **实时竞价**：根据各平台返回的 eCPM 值，选择收益最高的广告进行展示
- **灵活配置**：可按广告类型独立控制各平台的启用/禁用状态
- **解耦设计**：各平台的广告控制器独立实现，通过统一的竞价管理层协调

### 1.2 支持的广告类型

| 广告类型     | 说明               | 代码标识             |
| ------------ | ------------------ | -------------------- |
| 开屏广告     | App Open           | `APP_OPEN`           |
| 插页广告     | Interstitial       | `INTERSTITIAL`       |
| 原生广告     | Native             | `NATIVE`             |
| 全屏原生广告 | Full Screen Native | `FULL_SCREEN_NATIVE` |
| Banner 广告  | Banner             | `BANNER`             |
| 激励视频广告 | Rewarded Video     | `REWARDED`           |

### 1.3 架构层次

```
┌─────────────────────────────────────────────────────────────┐
│                    业务调用层 (App Module)                    │
├─────────────────────────────────────────────────────────────┤
│                  AdShowExt (广告展示扩展)                     │
│            统一入口，封装竞价逻辑和平台切换                      │
├─────────────────────────────────────────────────────────────┤
│              BiddingManager (竞价管理器)                      │
│         InterstitialBiddingManager, AppOpenBiddingManager 等 │
├───────────────────┬─────────────────┬───────────────────────┤
│    AdMob 控制器    │   Pangle 控制器  │    TopOn 控制器        │
│   (原生广告SDK)    │  (Pangle SDK)   │   (TopOn SDK)         │
└───────────────────┴─────────────────┴───────────────────────┘
```

---

## 2. 项目结构

### 2.1 模块组织

广告聚合功能封装在独立的 `bill` 模块中，推荐在新项目中保持相同结构：

```
bill/
├── build.gradle.kts          # 依赖配置、BuildConfig 字段定义
├── src/main/
│   ├── AndroidManifest.xml   # 广告相关的权限和组件声明
│   └── java/com/xxx/bill/
│       ├── ads/
│       │   ├── AdMobManager.kt                    # AdMob SDK 管理器
│       │   ├── AdResult.kt                        # 通用广告结果封装
│       │   ├── PreloadController.kt               # 广告预加载控制器
│       │   ├── InterstitialAdController.kt        # AdMob 插页广告控制器
│       │   ├── AppOpenAdController.kt             # AdMob 开屏广告控制器
│       │   ├── NativeAdController.kt              # AdMob 原生广告控制器
│       │   ├── FullScreenNativeAdController.kt    # AdMob 全屏原生广告控制器
│       │   ├── BannerAdController.kt              # AdMob Banner 广告控制器
│       │   ├── RewardedAdController.kt            # AdMob 激励广告控制器
│       │   │
│       │   ├── pangle/                            # Pangle 平台实现
│       │   │   ├── PangleManager.kt               # Pangle SDK 管理器
│       │   │   ├── PangleInterstitialAdController.kt
│       │   │   ├── PangleAppOpenAdController.kt
│       │   │   ├── PangleNativeAdController.kt
│       │   │   ├── PangleFullScreenNativeAdController.kt
│       │   │   ├── PangleBannerAdController.kt
│       │   │   └── PangleRewardedAdController.kt
│       │   │
│       │   ├── topon/                             # TopOn 平台实现
│       │   │   ├── TopOnManager.kt                # TopOn SDK 管理器
│       │   │   ├── TopOnInterstitialAdController.kt
│       │   │   ├── TopOnSplashAdController.kt
│       │   │   ├── TopOnNativeAdController.kt
│       │   │   ├── TopOnFullScreenNativeAdController.kt
│       │   │   ├── TopOnBannerAdController.kt
│       │   │   └── TopOnRewardedAdController.kt
│       │   │
│       │   ├── bidding/                           # 竞价模块
│       │   │   ├── BiddingWinner.kt               # 竞价胜出者枚举
│       │   │   ├── BiddingPlatformController.kt   # 平台启用配置控制器
│       │   │   ├── AdSourceController.kt          # 广告源控制器
│       │   │   ├── AppOpenBiddingInitializer.kt   # SDK 初始化器
│       │   │   ├── InterstitialBiddingManager.kt  # 插页广告竞价管理器
│       │   │   ├── AppOpenBiddingManager.kt       # 开屏广告竞价管理器
│       │   │   ├── NativeBiddingManager.kt        # 原生广告竞价管理器
│       │   │   ├── FullScreenNativeBiddingManager.kt
│       │   │   ├── BannerBiddingManager.kt
│       │   │   └── RewardedBiddingManager.kt
│       │   │
│       │   ├── ext/                               # 扩展层
│       │   │   └── AdShowExt.kt                   # 统一广告展示入口
│       │   │
│       │   ├── log/                               # 日志
│       │   │   └── AdLogger.kt                    # 广告日志工具
│       │   │
│       │   └── util/                              # 工具类
│       │       └── AdmobReflectionUtil.kt         # AdMob 收益反射获取
│       │
│       └── ui/                                     # 广告 UI 组件
│           ├── NativeAdView.kt                     # AdMob 原生广告视图
│           ├── FullScreenNativeAdActivity.kt       # AdMob 全屏原生广告页面
│           ├── pangle/
│           │   ├── PangleNativeAdView.kt
│           │   └── PangleFullScreenNativeAdActivity.kt
│           └── topon/
│               ├── ToponNativeAdView.kt
│               └── ToponFullScreenNativeAdActivity.kt
```

---

## 3. 依赖配置

### 3.1 settings.gradle.kts - Maven 仓库配置

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // Pangle 仓库
        maven {
            url = uri("https://artifact.bytedance.com/repository/pangle/")
        }
        
        // Mintegral 仓库 (Pangle 适配器依赖)
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        }
        
        // TopOn 仓库
        maven {
            url = uri("https://jfrog.anythinktech.com/artifactory/overseas_sdk")
        }
    }
}
```

### 3.2 bill/build.gradle.kts - 完整依赖配置

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// 从 gradle.properties 读取配置
val appConfig = findProperty("app") as Map<*, *>
val adMobConfig = findProperty("admob") as Map<*, *>
val adMobUnitConfig = adMobConfig["adUnitIds"] as Map<*, *>
val pangleConfig = findProperty("pangle") as? Map<*, *>
val pangleUnitConfig = pangleConfig?.get("adUnitIds") as? Map<*, *>
val toponConfig = findProperty("topon") as? Map<*, *>
val toponUnitConfig = toponConfig?.get("adUnitIds") as? Map<*, *>

android {
    namespace = "com.xxx.bill"
    compileSdk = appConfig["compileSdk"] as Int

    defaultConfig {
        minSdk = appConfig["minSdk"] as Int
        
        // AdMob 配置
        manifestPlaceholders["ADMOB_APPLICATION_ID"] = adMobConfig["applicationId"] as String
        buildConfigField("String", "ADMOB_SPLASH_ID", "\"${adMobUnitConfig["splash"]}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${adMobUnitConfig["banner"]}\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${adMobUnitConfig["interstitial"]}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"${adMobUnitConfig["native"]}\"")
        buildConfigField("String", "ADMOB_FULL_NATIVE_ID", "\"${adMobUnitConfig["full_native"]}\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"${adMobUnitConfig["rewarded"]}\"")

        // Pangle 配置
        buildConfigField("String", "PANGLE_APPLICATION_ID", "\"${pangleConfig!!["applicationId"]}\"")
        buildConfigField("String", "PANGLE_SPLASH_ID", "\"${pangleUnitConfig!!["splash"] ?: ""}\"")
        buildConfigField("String", "PANGLE_BANNER_ID", "\"${pangleUnitConfig["banner"] ?: ""}\"")
        buildConfigField("String", "PANGLE_INTERSTITIAL_ID", "\"${pangleUnitConfig["interstitial"] ?: ""}\"")
        buildConfigField("String", "PANGLE_NATIVE_ID", "\"${pangleUnitConfig["native"] ?: ""}\"")
        buildConfigField("String", "PANGLE_FULL_NATIVE_ID", "\"${pangleUnitConfig["full_native"] ?: ""}\"")
        buildConfigField("String", "PANGLE_REWARDED_ID", "\"${pangleUnitConfig["rewarded"] ?: ""}\"")

        // TopOn 配置
        val toponAppId = (toponConfig?.get("applicationId") as? String).orEmpty()
        val toponAppKey = (toponConfig?.get("appKey") as? String).orEmpty()
        val toponInterstitialId = (toponUnitConfig?.get("interstitial") as? String).orEmpty()
        val toponRewardedId = (toponUnitConfig?.get("rewarded") as? String).orEmpty()
        val toponNativeId = (toponUnitConfig?.get("native") as? String).orEmpty()
        val toponSplashId = (toponUnitConfig?.get("splash") as? String).orEmpty()
        val toponFullNativeId = (toponUnitConfig?.get("full_native") as? String).orEmpty()
        val toponBannerId = (toponUnitConfig?.get("banner") as? String).orEmpty()
        
        buildConfigField("String", "TOPON_APPLICATION_ID", "\"$toponAppId\"")
        buildConfigField("String", "TOPON_APP_KEY", "\"$toponAppKey\"")
        buildConfigField("String", "TOPON_INTERSTITIAL_ID", "\"$toponInterstitialId\"")
        buildConfigField("String", "TOPON_REWARDED_ID", "\"$toponRewardedId\"")
        buildConfigField("String", "TOPON_NATIVE_ID", "\"$toponNativeId\"")
        buildConfigField("String", "TOPON_SPLASH_ID", "\"$toponSplashId\"")
        buildConfigField("String", "TOPON_FULL_NATIVE_ID", "\"$toponFullNativeId\"")
        buildConfigField("String", "TOPON_BANNER_ID", "\"$toponBannerId\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Kotlin 协程
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.lifecycle.runtime.ktx)

    // ==================== AdMob SDK ====================
    api(libs.play.services.ads)  // com.google.android.gms:play-services-ads:24.4.0

    // ==================== Pangle 聚合 SDK ====================
    api("com.pangle.global:pag-sdk-m:7.5.6.2")
    
    // Pangle 三方适配器
    api("com.pangle.global:admob-adapter:24.4.0.5")
    api("com.pangle.global:mintegral-adapter:16.9.91.1")
    api("com.pangle.global:google-ad-manager-adapter:24.5.0.3")

    // ==================== TopOn 聚合 SDK ====================
    api("com.thinkup.sdk:core-tpn:6.5.16")
    api("com.thinkup.sdk:interstitial-tpn:6.5.16")
    api("com.thinkup.sdk:rewardedvideo-tpn:6.5.16")
    api("com.thinkup.sdk:nativead-tpn:6.5.16")
    api("com.thinkup.sdk:banner-tpn:6.5.16")
    api("com.thinkup.sdk:splash-tpn:6.5.16")
    
    // TopOn 三方适配器
    api("androidx.appcompat:appcompat:1.6.1")
    api("androidx.browser:browser:1.4.0")
    
    // Vungle
    api("com.thinkup.sdk:adapter-tpn-vungle:6.5.16")
    api("com.vungle:vungle-ads:7.5.0")
    api("com.google.android.gms:play-services-basement:18.1.0")
    api("com.google.android.gms:play-services-ads-identifier:18.0.1")
    
    // Bigo
    api("com.thinkup.sdk:adapter-tpn-bigo:6.5.16.1")
    api("com.bigossp:bigo-ads:5.5.1")
    
    // Pangle (TopOn 适配器)
    api("com.thinkup.sdk:adapter-tpn-pangle:6.5.16.2")
    
    // Facebook
    api("com.thinkup.sdk:adapter-tpn-facebook:6.5.16")
    api("com.facebook.android:audience-network-sdk:6.20.0")
    api("androidx.annotation:annotation:1.0.0")
    
    // Admob (TopOn 适配器)
    api("com.thinkup.sdk:adapter-tpn-admob:6.5.16")
    
    // Mintegral
    api("com.thinkup.sdk:adapter-tpn-mintegral:6.5.16.1")
    api("com.mbridge.msdk.oversea:mbridge_android_sdk:16.9.91")
    api("androidx.recyclerview:recyclerview:1.1.0")
    
    // Tramini (TopOn 反作弊插件)
    api("com.thinkup.sdk:tramini-plugin-tpn:6.5.16")
}
```

### 3.3 AndroidManifest.xml 配置

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application>
        <uses-library android:name="org.apache.http.legacy" android:required="false"/>
        
        <!-- AdMob 配置 -->
        <meta-data
            android:name="com.google.android.gms.ads.AD_MANAGER_APP"
            android:value="true" />
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="${ADMOB_APPLICATION_ID}" />
        <meta-data 
            android:name="com.google.android.gms.ads.flag.NATIVE_AD_DEBUGGER_ENABLED"
            android:value="false" />
        
        <!-- 全屏原生广告 Activity -->
        <activity
            android:name=".ui.FullScreenNativeAdActivity"
            android:screenOrientation="portrait"
            android:exported="false"
            android:theme="@style/Theme.NativeFullScreen" />
        <activity
            android:name=".ui.pangle.PangleFullScreenNativeAdActivity"
            android:screenOrientation="portrait"
            android:exported="false"
            android:theme="@style/Theme.NativeFullScreen" />
        <activity
            android:name=".ui.topon.ToponFullScreenNativeAdActivity"
            android:screenOrientation="portrait"
            android:exported="false"
            android:theme="@style/Theme.NativeFullScreen" />
    </application>

</manifest>
```

---

## 4. SDK 初始化

### 4.1 统一初始化入口

```kotlin
// AppOpenBiddingInitializer.kt
object AppOpenBiddingInitializer {

    suspend fun initialize(context: Context, iconResId: Int): AdResult<Unit> {
        // 1. 初始化 AdMob
        val admobResult = AdMobManager.initialize(context)
        if (admobResult is AdResult.Failure) return admobResult

        // 2. 初始化 Pangle
        val pangleResult = PangleManager.initialize(
            context = context,
            appId = BuildConfig.PANGLE_APPLICATION_ID,
            appIconId = iconResId  // App Open 广告需要
        )
        if (pangleResult is AdResult.Failure) return pangleResult

        // 3. 初始化 TopOn
        val toponResult = TopOnManager.initialize(
            context = context,
            appId = BuildConfig.TOPON_APPLICATION_ID,
            appKey = BuildConfig.TOPON_APP_KEY
        )
        if (toponResult is AdResult.Failure) return toponResult

        return AdResult.Success(Unit)
    }
}
```

### 4.2 各平台 SDK 管理器

#### AdMob 管理器

```kotlin
// AdMobManager.kt
object AdMobManager {
    private val _initializationState = MutableStateFlow<AdResult<Unit>>(AdResult.Loading)
    private var isInitialized = false

    suspend fun initialize(context: Context): AdResult<Unit> {
        if (isInitialized) return AdResult.Success(Unit)

        return suspendCancellableCoroutine { continuation ->
            MobileAds.initialize(context) { initializationStatus ->
                isInitialized = true
                val result = AdResult.Success(Unit)
                _initializationState.value = result
                continuation.resume(result)
            }
        }
    }
}
```

#### Pangle 管理器

```kotlin
// PangleManager.kt
object PangleManager {
    private var isInitialized = false

    suspend fun initialize(
        context: Context, 
        appId: String, 
        appIconId: Int? = null
    ): AdResult<Unit> {
        if (isInitialized || PAGSdk.isInitSuccess()) {
            return AdResult.Success(Unit)
        }

        return suspendCancellableCoroutine { continuation ->
            val configBuilder = PAGMConfig.Builder()
                .appId(appId)
                .debugLog(BuildConfig.DEBUG)
                .supportMultiProcess(false)

            appIconId?.let { configBuilder.appIcon(it) }

            PAGMSdk.init(context, configBuilder.build(), object : PAGMSdk.PAGMInitCallback {
                override fun success(model: PAGMInitSuccessModel) {
                    isInitialized = true
                    continuation.resume(AdResult.Success(Unit))
                }

                override fun fail(error: PAGErrorModel) {
                    continuation.resume(AdResult.Failure(
                        AdException(AdException.ERROR_INTERNAL, error.errorMessage ?: "")
                    ))
                }
            })
        }
    }
}
```

#### TopOn 管理器

```kotlin
// TopOnManager.kt
object TopOnManager {
    @Volatile
    private var isInitialized = false

    suspend fun initialize(
        context: Context, 
        appId: String, 
        appKey: String
    ): AdResult<Unit> {
        if (isInitialized) return AdResult.Success(Unit)

        return suspendCancellableCoroutine { continuation ->
            TUSDK.setNetworkLogDebug(BuildConfig.DEBUG)
            TUSDK.init(context, appId, appKey, TUNetworkConfig(), object : TUSDKInitListener {
                override fun onSuccess() {
                    isInitialized = true
                    continuation.resume(AdResult.Success(Unit))
                }

                override fun onFail(errorMsg: String) {
                    continuation.resume(AdResult.Failure(
                        AdException(AdException.ERROR_INTERNAL, errorMsg)
                    ))
                }
            })
        }
    }
}
```

---

## 5. 平台配置机制

### 5.1 BiddingPlatformController

控制各广告类型中各平台的启用/禁用状态：

```kotlin
// BiddingPlatformController.kt
object BiddingPlatformController {

    enum class AdType {
        INTERSTITIAL,       // 插页广告
        BANNER,             // Banner广告
        APP_OPEN,           // 开屏广告
        NATIVE,             // 原生广告
        FULL_SCREEN_NATIVE, // 全屏原生广告
        REWARDED            // 激励广告
    }

    enum class Platform {
        ADMOB, PANGLE, TOPON
    }

    // ==================== 平台配置（可根据需求修改） ====================

    // 插页广告：AdMob + Pangle + TopOn
    private const val INTERSTITIAL_ADMOB = true
    private const val INTERSTITIAL_PANGLE = true
    private const val INTERSTITIAL_TOPON = true

    // Banner广告：AdMob + TopOn
    private const val BANNER_ADMOB = true
    private const val BANNER_PANGLE = false
    private const val BANNER_TOPON = true

    // 开屏广告：AdMob + TopOn
    private const val APP_OPEN_ADMOB = true
    private const val APP_OPEN_PANGLE = false
    private const val APP_OPEN_TOPON = true

    // 原生广告：AdMob + TopOn
    private const val NATIVE_ADMOB = true
    private const val NATIVE_PANGLE = false
    private const val NATIVE_TOPON = true

    // 全屏原生广告：AdMob + TopOn
    private const val FULL_SCREEN_NATIVE_ADMOB = true
    private const val FULL_SCREEN_NATIVE_PANGLE = false
    private const val FULL_SCREEN_NATIVE_TOPON = true

    // 激励广告：AdMob + Pangle + TopOn
    private const val REWARDED_ADMOB = true
    private const val REWARDED_PANGLE = true
    private const val REWARDED_TOPON = true

    fun isPlatformEnabled(adType: AdType, platform: Platform): Boolean {
        return when (adType) {
            AdType.INTERSTITIAL -> when (platform) {
                Platform.ADMOB -> INTERSTITIAL_ADMOB
                Platform.PANGLE -> INTERSTITIAL_PANGLE
                Platform.TOPON -> INTERSTITIAL_TOPON
            }
            // ... 其他广告类型类似
        }
    }

    // 便捷方法
    fun isAdmobEnabled(adType: AdType) = isPlatformEnabled(adType, Platform.ADMOB)
    fun isPangleEnabled(adType: AdType) = isPlatformEnabled(adType, Platform.PANGLE)
    fun isToponEnabled(adType: AdType) = isPlatformEnabled(adType, Platform.TOPON)
}
```

---

## 6. 广告控制器设计

### 6.1 通用结果封装

```kotlin
// AdResult.kt
sealed class AdResult<out T> {
    data class Success<T>(val data: T) : AdResult<T>()
    data class Failure(val error: AdException) : AdResult<Nothing>()
    object Loading : AdResult<Nothing>()
}

data class AdException(
    val code: Int,
    val message: String,
    val cause: Throwable? = null
) {
    companion object {
        const val ERROR_NETWORK = 1001
        const val ERROR_NO_FILL = 1002
        const val ERROR_INVALID_REQUEST = 1003
        const val ERROR_INTERNAL = 1004
        const val ERROR_TIMEOUT = 1005
        const val ERROR_AD_EXPIRED = 1006
        const val ERROR_AD_ALREADY_SHOWING = 1007
        const val ERROR_NOT_LOADED = 1008
    }
}
```

### 6.2 控制器通用接口模式

每个广告控制器遵循相同的接口模式：

```kotlin
class XxxAdController private constructor() {
    companion object {
        @Volatile
        private var instance: XxxAdController? = null

        fun getInstance(): XxxAdController {
            return instance ?: synchronized(this) {
                instance ?: XxxAdController().also { instance = it }
            }
        }
    }

    // 预加载广告
    suspend fun preloadAd(context: Context, adUnitId: String): AdResult<Unit>

    // 展示广告
    suspend fun showAd(activity: Activity, adUnitId: String): AdResult<Unit>

    // 获取当前广告对象（用于竞价获取 eCPM）
    fun getCurrentAd(): AdObject?

    // 销毁广告
    fun destroyAd()

    // 销毁控制器
    fun destroy()

    // 检查广告是否正在展示
    fun isAdShowing(): Boolean
}
```

---

## 7. 竞价机制实现

### 7.1 竞价胜出者枚举

```kotlin
// BiddingWinner.kt
enum class BiddingWinner {
    ADMOB, PANGLE, TOPON
}
```

### 7.2 竞价管理器实现（以插页广告为例）

```kotlin
// InterstitialBiddingManager.kt
object InterstitialBiddingManager {

    suspend fun bidding(
        activity: Activity,
        admobAdUnitId: String = BuildConfig.ADMOB_INTERSTITIAL_ID,
        pangleAdUnitId: String = BuildConfig.PANGLE_INTERSTITIAL_ID,
        toponPlacementId: String = BuildConfig.TOPON_INTERSTITIAL_ID,
    ): BiddingWinner {
        
        // 1. 检查是否设置了固定的聚合源（用于调试）
        val source = AdSourceController.getCurrentSource()
        if (source != AdSourceController.AdSource.BIDDING) {
            return when (source) {
                AdSourceController.AdSource.ADMOB -> BiddingWinner.ADMOB
                AdSourceController.AdSource.PANGLE -> BiddingWinner.PANGLE
                AdSourceController.AdSource.TOPON -> BiddingWinner.TOPON
                else -> performBidding(activity, admobAdUnitId, pangleAdUnitId, toponPlacementId)
            }
        }
        
        return performBidding(activity, admobAdUnitId, pangleAdUnitId, toponPlacementId)
    }

    private suspend fun performBidding(
        activity: Activity,
        admobAdUnitId: String,
        pangleAdUnitId: String,
        toponPlacementId: String,
    ): BiddingWinner {
        val context = activity.applicationContext
        val admobController = InterstitialAdController.getInstance()
        val pangleController = PangleInterstitialAdController.getInstance()
        val toponController = TopOnInterstitialAdController.getInstance()
        
        // 根据平台配置决定是否参与比价
        val admobEnabled = BiddingPlatformController.isAdmobEnabled(AdType.INTERSTITIAL)
        val pangleEnabled = BiddingPlatformController.isPangleEnabled(AdType.INTERSTITIAL)
        val toponEnabled = BiddingPlatformController.isToponEnabled(AdType.INTERSTITIAL)

        // 2. 异步并行加载所有启用的广告
        val (admobLoadResult, pangleLoadResult, toponLoadResult) = coroutineScope {
            val admobDeferred = async {
                if (admobEnabled) {
                    runCatching { admobController.loadAdToCache(context, admobAdUnitId) }.getOrNull()
                } else null
            }
            val pangleDeferred = async {
                if (pangleEnabled) {
                    runCatching { pangleController.preloadAd(context, pangleAdUnitId) }.getOrNull()
                } else null
            }
            val toponDeferred = async {
                if (toponEnabled) {
                    runCatching { toponController.preloadAd(context, toponPlacementId) }.getOrNull()
                } else null
            }
            Triple(admobDeferred.await(), pangleDeferred.await(), toponDeferred.await())
        }

        // 3. 获取各平台收益（eCPM）
        
        // AdMob 通过反射获取收益
        val admobValueUsd = if (admobEnabled && admobLoadResult is AdResult.Success<*>) {
            admobController.getCachedAdPeek(admobAdUnitId)?.ad?.let { ad ->
                AdmobReflectionUtil.getRevenue(ad)?.valueMicros?.toDouble()?.div(1_000_000.0)
            } ?: 0.0
        } else 0.0

        // Pangle 通过 winEcpm 获取收益
        val pangleValueUsd = if (pangleEnabled && pangleLoadResult is AdResult.Success<*>) {
            pangleController.getCurrentAd()?.pagRevenueInfo?.winEcpm?.revenue?.toDoubleOrNull() ?: 0.0
        } else 0.0

        // TopOn 通过 publisherRevenue 获取收益
        val toponValueUsd = if (toponEnabled && toponLoadResult is AdResult.Success<*>) {
            toponController.getCurrentAd(toponPlacementId)?.let { ad ->
                runCatching { ad.checkValidAdCaches().firstOrNull()?.publisherRevenue }.getOrNull() ?: 0.0
            } ?: 0.0
        } else 0.0

        AdLogger.d(
            "插页竞价结果 -> AdMob: %.8f 美元%s, Pangle: %.8f 美元%s, TopOn: %.8f 美元%s",
            admobValueUsd, if (admobEnabled) "" else "(禁用)",
            pangleValueUsd, if (pangleEnabled) "" else "(禁用)",
            toponValueUsd, if (toponEnabled) "" else "(禁用)"
        )

        // 4. 只在启用的平台中选择出价最高者
        return when {
            admobEnabled && admobValueUsd >= pangleValueUsd && admobValueUsd >= toponValueUsd -> BiddingWinner.ADMOB
            pangleEnabled && pangleValueUsd >= toponValueUsd && pangleValueUsd >= admobValueUsd -> BiddingWinner.PANGLE
            toponEnabled -> BiddingWinner.TOPON
            admobEnabled -> BiddingWinner.ADMOB
            pangleEnabled -> BiddingWinner.PANGLE
            else -> BiddingWinner.ADMOB  // 默认
        }
    }
}
```

---

## 8. 广告展示扩展层

### 8.1 AdShowExt - 统一调用入口

```kotlin
// AdShowExt.kt
object AdShowExt {

    // ==================== 开屏广告 ====================
    suspend fun showAppOpenAd(
        activity: Activity,
        onLoaded: ((Boolean) -> Unit)? = null
    ): AdResult<Unit> {
        val winner = AppOpenBiddingManager.bidding(activity)

        return when (winner) {
            BiddingWinner.ADMOB -> {
                AppOpenAdController.getInstance().showAd(activity, onLoaded = onLoaded)
            }
            BiddingWinner.PANGLE -> {
                onLoaded?.invoke(true)
                PangleAppOpenAdController.getInstance().showAd(activity, onLoaded = onLoaded)
            }
            BiddingWinner.TOPON -> {
                onLoaded?.invoke(true)
                TopOnSplashAdController.getInstance().showAd(activity, onLoaded = onLoaded)
            }
        }
    }

    // ==================== 插页广告 ====================
    suspend fun showInterstitialAd(
        activity: Activity,
        ignoreFullNative: Boolean = false
    ): AdResult<Unit> {
        val winner = InterstitialBiddingManager.bidding(activity)

        return when (winner) {
            BiddingWinner.ADMOB -> {
                InterstitialAdController.getInstance().showAd(activity, ignoreFullNative = ignoreFullNative)
            }
            BiddingWinner.PANGLE -> {
                PangleInterstitialAdController.getInstance().showAd(activity, ignoreFullNative = ignoreFullNative)
            }
            BiddingWinner.TOPON -> {
                TopOnInterstitialAdController.getInstance().showAd(activity, ignoreFullNative = ignoreFullNative)
            }
        }
    }

    // ==================== 激励广告 ====================
    suspend fun showRewardedAd(
        activity: Activity,
        onRewardEarned: ((rewardType: String, rewardAmount: Int) -> Unit)? = null
    ): AdResult<Unit> {
        val winner = RewardedBiddingManager.bidding(activity)

        return when (winner) {
            BiddingWinner.ADMOB -> {
                RewardedAdController.getInstance().showAd(activity) { rewardItem ->
                    onRewardEarned?.invoke(rewardItem.type, rewardItem.amount)
                }
            }
            BiddingWinner.PANGLE -> {
                PangleRewardedAdController.getInstance().showAd(activity) { rewardItem ->
                    onRewardEarned?.invoke(rewardItem.rewardName ?: "", rewardItem.rewardAmount)
                }
            }
            BiddingWinner.TOPON -> {
                TopOnRewardedAdController.getInstance().showAd(activity) { type, amount ->
                    onRewardEarned?.invoke(type, amount)
                }
            }
        }
    }

    // ==================== Banner 广告 ====================
    suspend fun showBannerAd(
        activity: Activity,
        container: ViewGroup
    ): AdResult<View> {
        val winner = BannerBiddingManager.bidding(activity)

        return when (winner) {
            BiddingWinner.ADMOB -> BannerAdController.getInstance().showAd(activity, container)
            BiddingWinner.PANGLE -> PangleBannerAdController.getInstance().showAd(activity, container)
            BiddingWinner.TOPON -> TopOnBannerAdController.getInstance().showAd(activity, container)
        }
    }

    // ==================== 原生广告 ====================
    suspend fun showNativeAdInContainer(
        context: Context,
        container: ViewGroup,
        style: NativeAdStyle
    ): Boolean {
        val winner = NativeBiddingManager.bidding(context)

        return when (winner) {
            BiddingWinner.ADMOB -> NativeAdController.getInstance().showAdInContainer(context, container, style)
            BiddingWinner.PANGLE -> PangleNativeAdController.getInstance().showAdInContainer(context, container, style)
            BiddingWinner.TOPON -> TopOnNativeAdController.getInstance().showAdInContainer(context, container, style)
        }
    }

    // ==================== 广告展示状态检查 ====================
    fun isAnyInterstitialOrFullScreenNativeShowing(): Boolean {
        return InterstitialAdController.getInstance().isAdShowing() ||
               FullScreenNativeAdController.getInstance().isAdShowing() ||
               PangleInterstitialAdController.getInstance().isAdShowing() ||
               PangleFullScreenNativeAdController.getInstance().isAdShowing() ||
               TopOnInterstitialAdController.getInstance().isAdShowing() ||
               TopOnFullScreenNativeAdController.getInstance().isAdShowing()
    }
}
```

---

## 9. 预加载机制

### 9.1 PreloadController

在应用启动后预加载各平台广告，提升首次展示体验：

```kotlin
// PreloadController.kt
object PreloadController {

    // 预加载 AdMob 广告
    fun preload(context: Context) {
        MainScope().launch {
            try {
                InterstitialAdController.getInstance().preloadAd(context)
            } catch (e: Exception) {
                AdLogger.e("Admob插页预加载失败", e)
            }
        }
        // ... 其他 AdMob 广告类型
    }

    // 预加载 Pangle 广告（根据配置）
    fun preloadPangle(context: Context) {
        if (BiddingPlatformController.isPlatformEnabled(AdType.INTERSTITIAL, Platform.PANGLE)) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    PangleInterstitialAdController.getInstance().preloadAd(context)
                } catch (e: Exception) {
                    AdLogger.e("Pangle插页预加载失败", e)
                }
            }
        }
        // ... 其他 Pangle 广告类型
    }

    // 预加载 TopOn 广告（根据配置）
    fun preloadTopOn(context: Activity) {
        if (BiddingPlatformController.isPlatformEnabled(AdType.INTERSTITIAL, Platform.TOPON)) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    TopOnInterstitialAdController.getInstance().preloadAd(context)
                } catch (e: Exception) {
                    AdLogger.e("TopOn插页预加载失败", e)
                }
            }
        }
        // ... 其他 TopOn 广告类型
    }
}
```

---

## 10. 收益获取方式

### 10.1 各平台 eCPM 获取方法

| 平台       | 获取方式             | 代码示例                                                            |
| ---------- | -------------------- | ------------------------------------------------------------------- |
| **AdMob**  | 反射获取内部 AdValue | `AdmobReflectionUtil.getRevenue(ad)?.valueMicros?.div(1_000_000.0)` |
| **Pangle** | SDK 直接提供         | `ad.pagRevenueInfo?.winEcpm?.revenue?.toDoubleOrNull()`             |
| **TopOn**  | SDK 直接提供         | `ad.checkValidAdCaches().firstOrNull()?.publisherRevenue`           |

### 10.2 AdMob 反射工具（关键代码）

由于 AdMob SDK 没有直接暴露 eCPM 接口，需要通过反射获取：

```kotlin
// AdmobReflectionUtil.kt
object AdmobReflectionUtil {

    // 不同广告类型的反射路径
    private val ivStackV1 = arrayOf("zzc", "zzj", "zzf", "zzd", "zzae")
    private val ivStackV2 = arrayOf("zzc", "zza", "a", "a", "d", "d", "ae")
    // ... 其他广告类型的反射路径

    fun getRevenue(ad: Any?): AdValue? {
        if (ad == null) return null
        val stackList = when (ad) {
            is InterstitialAd -> listOf(ivStackV1, ivStackV2)
            is RewardedAd -> listOf(rvStackV1, rvStackV2)
            is NativeAd -> listOf(nativeStackV1, nativeStackV2)
            is BaseAdView, is AdManagerAdView -> listOf(bannerStackV1, bannerStackV2)
            is AppOpenAd -> listOf(spStackV1, spStackV2)
            else -> emptyList()
        }
        stackList.forEach { stack ->
            val leaf = traverse(ad, stack) ?: return@forEach
            parseLeaf(leaf)?.let { return it }
        }
        return null
    }

    private fun traverse(target: Any, stack: Array<String>): Any? {
        var current: Any? = target
        stack.forEach { fieldName ->
            current = current.getValue(fieldName) ?: return null
        }
        return current
    }

    // ... 其他辅助方法
}
```

> ⚠️ **注意**：反射路径可能随 AdMob SDK 版本更新而变化，需要在升级 SDK 后验证。

---

## 11. 接入流程清单

### 11.1 新项目接入步骤

1. **创建 bill 模块**
   - [ ] 复制整个 `bill` 模块目录结构
   - [ ] 修改包名为项目对应的包名

2. **配置 Maven 仓库**
   - [ ] 在 `settings.gradle.kts` 中添加 Pangle、Mintegral、TopOn 仓库

3. **配置广告 ID**
   - [ ] 在 `gradle.properties` 或单独的配置文件中定义各平台的 App ID 和广告位 ID
   - [ ] 在 `bill/build.gradle.kts` 中通过 `buildConfigField` 生成 BuildConfig 字段

4. **配置平台启用状态**
   - [ ] 修改 `BiddingPlatformController.kt` 中各广告类型的平台启用配置

5. **初始化 SDK**
   - [ ] 在 Application 或 SplashActivity 中调用 `AppOpenBiddingInitializer.initialize()`

6. **预加载广告**
   - [ ] 在合适的时机调用 `PreloadController.preload()` / `preloadPangle()` / `preloadTopOn()`

7. **展示广告**
   - [ ] 使用 `AdShowExt` 的各方法展示广告

### 11.2 API 快速参考

```kotlin
// 初始化
lifecycleScope.launch {
    val result = AppOpenBiddingInitializer.initialize(context, R.mipmap.ic_launcher)
}

// 预加载
PreloadController.preload(context)
PreloadController.preloadPangle(context)
PreloadController.preloadTopOn(activity)

// 展示广告
lifecycleScope.launch {
    // 开屏
    AdShowExt.showAppOpenAd(activity)
    
    // 插页
    AdShowExt.showInterstitialAd(activity)
    
    // 激励
    AdShowExt.showRewardedAd(activity) { type, amount ->
        // 奖励回调
    }
    
    // Banner
    AdShowExt.showBannerAd(activity, bannerContainer)
    
    // 原生
    AdShowExt.showNativeAdInContainer(context, container, NativeAdStyle.STANDARD)
}
```

---

## 附录

### A. 版本兼容性

| 依赖              | 版本    | 备注                     |
| ----------------- | ------- | ------------------------ |
| AdMob SDK         | 24.4.0  | Google Play Services Ads |
| Pangle SDK        | 7.5.6.2 | PAG SDK Mediation        |
| TopOn SDK         | 6.5.16  | Core + 各广告类型模块    |
| Kotlin Coroutines | Latest  | 用于异步竞价             |

### B. 日志标签

所有广告相关日志统一使用 `AdModule` 标签，可通过 `adb logcat -s AdModule` 过滤查看。

### C. 常见问题

1. **Q: 竞价时某个平台总是返回 0 收益？**
   - 检查该平台的广告位 ID 是否正确配置
   - 检查是否在 `BiddingPlatformController` 中启用了该平台
   - 检查 SDK 初始化是否成功

2. **Q: 如何切换到固定广告源进行调试？**
   - 使用 `AdSourceController.setCurrentSource(AdSource.PANGLE)` 设置固定源
   - 设置回 `AdSource.BIDDING` 恢复竞价模式

3. **Q: 升级 AdMob SDK 后反射获取收益失败？**
   - 需要更新 `AdmobReflectionUtil` 中的反射路径
   - 使用 jadx 等工具反编译新版 SDK 验证字段路径

---

> **文档版本**: v1.0  
> **生成日期**: 2026-01-11  
> **基于项目**: ReMax_PhotoRecovery
