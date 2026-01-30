# UMP (User Messaging Platform) 集成实施方案

> 用于 GDPR 合规的用户隐私同意管理  
> 创建时间：2026年1月7日

---

## 📋 需求概述

### 业务需求

1. **启动流程**：App启动 → 通知权限询问 → 是否展示UMP（欧盟+英国+瑞士需要展示）→ 首页
2. **IP地理位置检测**：根据用户IP判定是否展示UMP弹窗
3. **GDPR国家判定**：
   - **需要展示UMP**：欧盟27国 + 英国 + 瑞士
   - **不需要展示**：其他国家

### GDPR 覆盖国家列表

```json
{
  "gdpr_countries": [
    "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR",
    "DE", "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL",
    "PL", "PT", "RO", "SK", "SI", "ES", "SE",
    "GB",
    "CH", "IS", "LI", "NO"
  ]
}
```

| 区域 | 国家代码 |
|------|----------|
| 欧盟27国 | AT, BE, BG, HR, CY, CZ, DK, EE, FI, FR, DE, GR, HU, IE, IT, LV, LT, LU, MT, NL, PL, PT, RO, SK, SI, ES, SE |
| 英国 | GB |
| 瑞士及欧洲经济区 | CH, IS, LI, NO |

### 技术参考

- [Google AdMob 隐私权文档](https://developers.google.com/admob/android/privacy?hl=zh-cn)
- IP查询API：https://api.country.is/

---

## 🔍 现有代码分析

### 已有实现

| 组件 | 路径 | 状态 |
|------|------|------|
| GoogleMobileAdsConsentManager | `monetize/src/main/java/net/corekit/monetize/ump/` | ✅ 已存在，但未使用 |
| AdsManager | `monetize/src/main/java/net/corekit/monetize/ads/AdsManager.kt` | ❌ 直接初始化，未集成UMP |
| UMP SDK依赖 | build.gradle | ⚠️ 需确认是否已添加 |

### 现有初始化流程

```
AppInitializer.initialize()
    └── initializeCoreServices()
        └── AdsManager.init(application)  // 直接初始化 MobileAds，缺少 UMP 流程
```

### 问题点

1. `GoogleMobileAdsConsentManager` 已实现但未被调用
2. 缺少 IP 地理位置检测逻辑
3. 缺少 GDPR 国家判定逻辑
4. 广告初始化未等待 UMP 同意结果

---

## 🏗️ 架构设计

### 整体流程

```
┌─────────────────────────────────────────────────────────────────┐
│                         App 启动                                │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   GeoLocationService                            │
│              查询 api.country.is 获取国家代码                    │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   GdprRegionChecker                             │
│              判断是否属于 GDPR 覆盖区域                          │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    ▼                       ▼
            ┌───────────────┐       ┌───────────────┐
            │  GDPR 区域    │       │  非 GDPR 区域  │
            └───────┬───────┘       └───────┬───────┘
                    │                       │
                    ▼                       │
┌─────────────────────────────────────┐     │
│  GoogleMobileAdsConsentManager      │     │
│  展示 UMP 同意弹窗                   │     │
│  等待用户选择                        │     │
└───────────────────┬─────────────────┘     │
                    │                       │
                    ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     AdsManager.init()                           │
│                   初始化 AdMob SDK                              │
└───────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                       进入首页                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 组件设计

```
monetize/src/main/java/net/corekit/monetize/
├── ump/
│   ├── GoogleMobileAdsConsentManager.kt  (已有，需微调)
│   ├── GeoLocationService.kt             (新增)
│   ├── GdprRegionChecker.kt              (新增)
│   └── UmpConsentController.kt           (新增，统一入口)
└── ads/
    └── AdsManager.kt                     (修改，集成UMP流程)
```

---

## 📝 实现代码

### 1. GeoLocationService - IP地理位置服务

```kotlin
// monetize/src/main/java/net/corekit/monetize/ump/GeoLocationService.kt

package net.corekit.monetize.ump

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.corekit.monetize.ads.log.AdLogger
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * IP地理位置检测服务
 * 使用 api.country.is 查询用户所在国家
 */
object GeoLocationService {
    
    private const val TAG = "GeoLocation"
    private const val API_URL = "https://api.country.is/"
    private const val TIMEOUT_MS = 5000L
    
    /**
     * 查询当前用户所在国家代码
     * @return 国家代码（如 "US", "DE", "CN"），失败返回 null
     */
    suspend fun getCountryCode(): String? = withContext(Dispatchers.IO) {
        try {
            withTimeoutOrNull(TIMEOUT_MS) {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 3000
                    setRequestProperty("Accept", "application/json")
                }
                
                try {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val countryCode = json.optString("country", null)
                        AdLogger.d("[$TAG] IP查询成功，国家代码: $countryCode")
                        countryCode
                    } else {
                        AdLogger.w("[$TAG] IP查询失败，HTTP状态码: ${connection.responseCode}")
                        null
                    }
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] IP查询异常: ${e.message}")
            null
        }
    }
}
```

### 2. GdprRegionChecker - GDPR区域检测器

```kotlin
// monetize/src/main/java/net/corekit/monetize/ump/GdprRegionChecker.kt

package net.corekit.monetize.ump

import net.corekit.monetize.ads.log.AdLogger

/**
 * GDPR 区域检测器
 * 判断给定国家代码是否属于 GDPR 覆盖区域
 */
object GdprRegionChecker {
    
    private const val TAG = "GdprChecker"
    
    /**
     * GDPR 覆盖的国家代码列表
     * 包括：欧盟27国 + 英国 + 瑞士 + 欧洲经济区(冰岛、列支敦士登、挪威)
     */
    private val GDPR_COUNTRIES = setOf(
        // 欧盟27国
        "AT", // 奥地利
        "BE", // 比利时
        "BG", // 保加利亚
        "HR", // 克罗地亚
        "CY", // 塞浦路斯
        "CZ", // 捷克
        "DK", // 丹麦
        "EE", // 爱沙尼亚
        "FI", // 芬兰
        "FR", // 法国
        "DE", // 德国
        "GR", // 希腊
        "HU", // 匈牙利
        "IE", // 爱尔兰
        "IT", // 意大利
        "LV", // 拉脱维亚
        "LT", // 立陶宛
        "LU", // 卢森堡
        "MT", // 马耳他
        "NL", // 荷兰
        "PL", // 波兰
        "PT", // 葡萄牙
        "RO", // 罗马尼亚
        "SK", // 斯洛伐克
        "SI", // 斯洛文尼亚
        "ES", // 西班牙
        "SE", // 瑞典
        // 英国 (脱欧后仍适用UK GDPR)
        "GB",
        // 欧洲经济区 (EEA)
        "IS", // 冰岛
        "LI", // 列支敦士登
        "NO", // 挪威
        // 瑞士 (适用类似GDPR的数据保护法)
        "CH"
    )
    
    /**
     * 检查国家代码是否属于GDPR覆盖区域
     * @param countryCode ISO 3166-1 alpha-2 国家代码
     * @return true 表示需要展示UMP同意弹窗
     */
    fun isGdprRegion(countryCode: String?): Boolean {
        if (countryCode.isNullOrBlank()) {
            AdLogger.w("[$TAG] 国家代码为空，默认不显示UMP")
            return false
        }
        
        val isGdpr = countryCode.uppercase() in GDPR_COUNTRIES
        AdLogger.d("[$TAG] 国家 $countryCode 是否属于GDPR区域: $isGdpr")
        return isGdpr
    }
    
    /**
     * 获取所有GDPR国家代码（用于调试）
     */
    fun getAllGdprCountries(): Set<String> = GDPR_COUNTRIES.toSet()
}
```

### 3. UmpConsentController - 统一入口控制器

```kotlin
// monetize/src/main/java/net/corekit/monetize/ump/UmpConsentController.kt

package net.corekit.monetize.ump

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.corekit.core.ext.DataStoreBoolDelegate
import net.corekit.core.ext.DataStoreStringDelegate
import net.corekit.monetize.ads.log.AdLogger

/**
 * UMP 同意管理统一控制器
 * 
 * 职责：
 * 1. 判断是否需要展示UMP同意弹窗
 * 2. 协调IP检测、区域判断、同意收集流程
 * 3. 缓存用户选择结果
 */
object UmpConsentController {
    
    private const val TAG = "UmpController"
    
    // 缓存的国家代码（避免每次都查询IP）
    private var cachedCountryCode by DataStoreStringDelegate("ump_country_code", "")
    
    // 是否已经完成过同意流程
    private var hasCompletedConsentFlow by DataStoreBoolDelegate("ump_consent_completed", false)
    
    // 是否可以请求广告（UMP SDK返回的结果）
    private var canRequestAdsCache by DataStoreBoolDelegate("ump_can_request_ads", true)
    
    /**
     * 检查并处理UMP同意流程
     * 
     * @param activity 当前Activity（用于展示UMP弹窗）
     * @return true 表示可以请求广告，false 表示不可以
     */
    suspend fun checkAndHandleConsent(activity: Activity): Boolean = withContext(Dispatchers.Main) {
        try {
            AdLogger.d("[$TAG] ========== 开始UMP同意检查流程 ==========")
            
            // 1. 检测用户地理位置
            val countryCode = detectUserLocation()
            
            // 2. 判断是否需要UMP
            if (!GdprRegionChecker.isGdprRegion(countryCode)) {
                AdLogger.d("[$TAG] 非GDPR区域，跳过UMP流程，直接允许广告请求")
                canRequestAdsCache = true
                return@withContext true
            }
            
            AdLogger.d("[$TAG] GDPR区域用户，需要展示UMP同意弹窗")
            
            // 3. 调用UMP SDK收集同意
            val consentManager = GoogleMobileAdsConsentManager.getInstance(activity)
            val canRequestAds = consentManager.gatherConsent(activity)
            
            // 4. 缓存结果
            canRequestAdsCache = canRequestAds
            hasCompletedConsentFlow = true
            
            AdLogger.d("[$TAG] UMP同意流程完成，canRequestAds: $canRequestAds")
            AdLogger.d("[$TAG] ========== UMP同意检查流程结束 ==========")
            
            canRequestAds
            
        } catch (e: Exception) {
            AdLogger.e("[$TAG] UMP同意检查异常: ${e.message}")
            // 出错时默认允许广告请求（避免影响收益）
            true
        }
    }
    
    /**
     * 检测用户地理位置
     */
    private suspend fun detectUserLocation(): String? {
        // 优先使用缓存
        if (cachedCountryCode.isNotBlank()) {
            AdLogger.d("[$TAG] 使用缓存的国家代码: $cachedCountryCode")
            return cachedCountryCode
        }
        
        // 查询IP
        val countryCode = GeoLocationService.getCountryCode()
        if (!countryCode.isNullOrBlank()) {
            cachedCountryCode = countryCode
        }
        
        return countryCode
    }
    
    /**
     * 检查是否可以请求广告（快速检查，不触发网络请求）
     */
    fun canRequestAds(): Boolean {
        return canRequestAdsCache
    }
    
    /**
     * 检查是否需要展示隐私选项入口
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            GoogleMobileAdsConsentManager.getInstance(context).isPrivacyOptionsRequired
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 显示隐私选项表单（供设置页面调用）
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        try {
            if (activity is androidx.fragment.app.FragmentActivity) {
                GoogleMobileAdsConsentManager.getInstance(activity)
                    .showPrivacyOptionsForm(activity) { _ ->
                        onDismissed()
                    }
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 显示隐私选项表单异常: ${e.message}")
            onDismissed()
        }
    }
    
    /**
     * 重置同意状态（用于测试）
     */
    fun resetConsentState(context: Context) {
        cachedCountryCode = ""
        hasCompletedConsentFlow = false
        canRequestAdsCache = true
        AdLogger.d("[$TAG] 同意状态已重置")
    }
    
    /**
     * 强制刷新国家代码缓存
     */
    suspend fun refreshCountryCode(): String? {
        cachedCountryCode = ""
        return detectUserLocation()
    }
}
```

### 4. 修改 AdsManager - 集成UMP流程

```kotlin
// 在 AdsManager.kt 中添加以下修改

/**
 * 初始化 AdMob SDK（包含UMP同意流程）
 * @param context 上下文
 * @param activity 用于展示UMP弹窗的Activity（可选，如果为null则跳过UMP）
 */
suspend fun initWithConsent(context: Context, activity: Activity?): AdResult<Unit> {
    if (isInitialized) {
        return AdResult.Success(Unit)
    }
    
    // 1. 如果提供了Activity，先处理UMP同意流程
    if (activity != null) {
        try {
            val canRequestAds = UmpConsentController.checkAndHandleConsent(activity)
            if (!canRequestAds) {
                AdLogger.w("用户未同意广告，跳过广告SDK初始化")
                return AdResult.Failure(
                    AdException(
                        code = AdException.ERROR_CONSENT_REQUIRED,
                        message = "用户未同意广告请求"
                    )
                )
            }
        } catch (e: Exception) {
            AdLogger.e("UMP同意检查失败，继续初始化广告SDK", e)
        }
    }
    
    // 2. 初始化广告SDK
    return init(context)
}
```

### 5. 修改 SplashScreen - 集成启动流程

```kotlin
// 在 SplashScreen.kt 中修改 initView 方法

override fun initView(savedInstanceState: Bundle?) {
    // ... 现有代码 ...
    
    lifecycleScope.launch {
        try {
            // 1. 权限流程
            stateMachine.onPermissionCheckCompleted()
            
            // 2. UMP同意流程（新增）
            val canRequestAds = UmpConsentController.checkAndHandleConsent(this@SplashScreen)
            AdLogger.d("UMP同意结果: canRequestAds=$canRequestAds")
            
            // 3. 广告流程（仅在同意后执行）
            if (canRequestAds) {
                val adJob = async {
                    initializeAndShowAd()
                }
                // ... 现有超时逻辑 ...
            } else {
                AdLogger.d("用户未同意广告，跳过开屏广告")
            }
            
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            stateMachine.onAdCompleted()
        }
    }
}
```

---

## 📦 依赖配置

### Gradle 依赖

```gradle
// monetize/build.gradle

dependencies {
    // UMP SDK (User Messaging Platform)
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    
    // 网络请求（如果项目中没有）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")  // 可选，用于替代原生HttpURLConnection
}
```

### 版本兼容性

| SDK | 最低版本 | 推荐版本 |
|-----|---------|---------|
| UMP SDK | 2.0.0 | 3.1.0 |
| Android API | 21 | 24+ |
| Google Play Services Ads | 22.0.0 | 23.0.0+ |

---

## 🧪 测试方案

### 1. 调试模式配置

```kotlin
// 在 GoogleMobileAdsConsentManager.kt 中启用调试

fun gatherConsent(
    activity: Activity,
    onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener,
    forceEeaForTesting: Boolean = false  // 新增参数
) {
    val paramsBuilder = ConsentRequestParameters.Builder()
    
    // 测试模式：强制设置为EEA地区
    if (forceEeaForTesting || BuildConfig.DEBUG) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId("YOUR_TEST_DEVICE_HASHED_ID") // 替换为实际设备ID
            .build()
        paramsBuilder.setConsentDebugSettings(debugSettings)
    }
    
    val params = paramsBuilder.build()
    // ... 后续代码
}
```

### 2. 测试用例

| 测试场景 | 预期结果 |
|---------|---------|
| 中国用户 (CN) | 跳过UMP，直接加载广告 |
| 美国用户 (US) | 跳过UMP，直接加载广告 |
| 德国用户 (DE) | 展示UMP同意弹窗 |
| 英国用户 (GB) | 展示UMP同意弹窗 |
| 瑞士用户 (CH) | 展示UMP同意弹窗 |
| IP查询失败 | 默认不展示UMP，允许广告 |
| 用户拒绝同意 | 不加载广告 |
| 用户同意 | 正常加载广告 |

### 3. 设备ID获取

```kotlin
// 在Logcat中搜索以下日志获取测试设备ID
// "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("XXXXXX") to set this as a debug device."
```

---

## 📅 实施计划

### 阶段一：基础集成（1-2天）

| 任务 | 优先级 | 预估时间 |
|------|--------|---------|
| 创建 GeoLocationService | P0 | 2h |
| 创建 GdprRegionChecker | P0 | 1h |
| 创建 UmpConsentController | P0 | 3h |
| 添加 UMP SDK 依赖 | P0 | 0.5h |
| 单元测试 | P1 | 2h |

### 阶段二：流程集成（1-2天）

| 任务 | 优先级 | 预估时间 |
|------|--------|---------|
| 修改 AdsManager 集成 UMP | P0 | 2h |
| 修改 SplashScreen 启动流程 | P0 | 3h |
| 设置页面添加隐私选项入口 | P1 | 2h |
| 集成测试 | P0 | 3h |

### 阶段三：优化与上线（1天）

| 任务 | 优先级 | 预估时间 |
|------|--------|---------|
| 性能优化（IP缓存策略） | P1 | 1h |
| 错误处理完善 | P1 | 1h |
| AdMob 后台配置隐私消息 | P0 | 1h |
| 灰度测试 | P0 | 2h |

---

## ⚠️ 注意事项

### AdMob 后台配置

1. 登录 [AdMob 控制台](https://apps.admob.com/)
2. 进入 **隐私权和消息** 标签页
3. 创建 **GDPR 消息** 或 **IDFA 消息**
4. 配置消息内容和显示规则
5. 发布消息

### 重要提醒

1. **首次启动**：UMP SDK 会自动判断是否需要展示同意弹窗
2. **同意状态持久化**：SDK 会自动缓存用户选择，无需手动处理
3. **每次启动检查**：Google 建议每次启动都调用 `requestConsentInfoUpdate()`
4. **异常处理**：IP查询失败时默认允许广告，避免影响非GDPR用户
5. **隐私选项入口**：在设置页面提供"隐私设置"入口，让用户可以修改选择

---

## 📊 监控指标

### 关键埋点

```kotlin
// UMP流程埋点
ReportDataManager.reportData("ump_flow_start", mapOf(
    "country_code" to countryCode,
    "is_gdpr_region" to isGdprRegion
))

ReportDataManager.reportData("ump_consent_result", mapOf(
    "can_request_ads" to canRequestAds,
    "consent_status" to consentStatus
))
```

### 监控指标

| 指标 | 说明 |
|------|------|
| UMP展示率 | GDPR区域用户中UMP弹窗展示比例 |
| 同意率 | 用户点击同意的比例 |
| IP检测成功率 | api.country.is 查询成功率 |
| 同意流程耗时 | 从检测到完成的平均时间 |

---

## 📝 总结

本方案通过以下步骤实现 UMP 合规集成：

1. **IP地理位置检测**：使用 `api.country.is` API 获取用户国家代码
2. **GDPR区域判断**：根据国家代码判断是否需要展示UMP
3. **同意收集**：调用 UMP SDK 展示同意弹窗
4. **广告控制**：根据同意结果决定是否初始化和展示广告

**核心原则**：
- 仅对 GDPR 区域用户展示同意弹窗
- 非 GDPR 区域用户直接加载广告
- 失败时默认允许广告（保护收益）
- 提供隐私选项入口（合规要求）

---

*本文档将根据 Google 政策变化和测试反馈持续更新。*
