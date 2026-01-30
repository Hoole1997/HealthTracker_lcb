# AdMob 客户端竞价逻辑与 eCPM 获取分析

> 本文档分析 PDF_Launcher 项目中 AdMob 广告的竞价逻辑实现，重点关注 eCPM 的获取方式及其在不同广告类型间的复用性。

---

## 一、整体架构概述

### 1.1 广告模块结构

```
monetize/src/main/java/net/corekit/monetize/ads/
├── LaunchAds.kt              # 开屏广告（AppOpenAd）
├── InterstitialAds.kt        # 插屏广告（InterstitialAd）
├── RewardedAds.kt            # 激励视频广告（RewardedAd）
├── NativeAds.kt              # 原生广告（NativeAd）
├── FullNativeAds.kt          # 全屏原生广告（NativeAd）
├── BannerAds.kt              # Banner广告（BannerAd）
├── util/
│   └── AdmobNextGenReflectionUtil.kt  # eCPM反射获取工具（核心）
└── interceptor/
    └── AdInterceptor.kt      # 广告拦截器链
```

### 1.2 使用的 AdMob SDK

项目使用的是 **AdMob Next-Gen SDK**（新一代SDK），包名为：
```
com.google.android.libraries.ads.mobile.sdk
```

---

## 二、eCPM 获取机制分析

### 2.1 eCPM 获取的两种方式

#### 方式一：通过 `onAdPaid` 回调（展示后获取）

这是 AdMob 官方推荐的方式，在广告展示时通过回调获取收益信息：

```kotlin
// 以开屏广告为例
appOpenAd.adEventCallback = object : AppOpenAdEventCallback {
    override fun onAdPaid(value: AdValue) {
        // value.valueMicros - 收益值（微元，需除以 1,000,000）
        // value.currencyCode - 货币代码（如 "USD"）
        // value.precisionType - 精度类型
    }
}
```

**特点：**
- ✅ 官方支持，稳定可靠
- ✅ 所有广告类型统一接口
- ❌ 只能在展示后获取，**无法用于展示前竞价**

#### 方式二：通过反射提前获取（展示前获取）★ 核心竞价能力

项目实现了 `AdmobNextGenReflectionUtil` 工具类，通过反射在广告**加载完成后、展示前**获取 eCPM：

```kotlin
// 获取缓存广告的价格（展示前）
suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
    val cachedAd = peekCachedAd(finalAdUnitId)
    // 使用反射获取价格
    val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd.ad)
    return adValue?.valueMicros?.div(1_000_000.0)
}
```

### 2.2 反射获取 eCPM 的核心实现

#### 2.2.1 反射工具类结构

`AdmobNextGenReflectionUtil` 提供两种反射方式：

| 方法 | 描述 | 性能 | 稳定性 |
|------|------|------|--------|
| `getRevenue(ad)` | 递归遍历查找 AdValue | 较慢 | 更通用 |
| `getRevenueByPath(ad)` | 固定路径直接访问 | 更快 | 依赖SDK版本 |

#### 2.2.2 各广告类型的反射路径

```kotlin
// 插屏广告路径
private val ivStackV1 = arrayOf("b", "k", "L", "e", "b", "j", "a", "M", "c", "m")
private val ivStackV2 = arrayOf("b", "k", "M", "c", "m")

// 开屏广告路径
private val spStack = arrayOf("b", "k", "M", "c", "m")

// 原生广告路径
private val nativeStackV1 = arrayOf("b", "l", "j", "e", "b", "j", "a", "M", "c", "m")
private val nativeStackV2 = arrayOf("b", "l", "s", "e", "m")

// Banner广告路径
private val bannerStack = arrayOf("b", "k", "a", "d", "d", "a", "m")

// 激励视频路径
private val rvStack = arrayOf("c", "a", "a", "k", "M", "c", "m")
```

#### 2.2.3 反射查找 AdValue 的核心逻辑

```kotlin
fun getRevenueByPath(ad: Any?): AdValue? {
    if (ad == null) return null
    return when (ad) {
        is InterstitialAd -> findAdValueByPath(ad, "插页", listOf(ivStackV1, ivStackV2))
        is AppOpenAd -> findAdValueByPath(ad, "开屏", listOf(spStack))
        is RewardedAd -> findAdValueByPath(ad, "激励", listOf(rvStack))
        is NativeAd -> findAdValueByPath(ad, "原生", listOf(nativeStackV1, nativeStackV2))
        is BannerAd -> findAdValueByPath(ad, "Banner", listOf(bannerStack))
        else -> null
    }
}
```

#### 2.2.4 AdValue 数据结构解析

通过反射查找包含以下特征的对象：
- `PrecisionType` 枚举字段 - 精度类型
- `Long` 类型字段 - valueMicros（收益微元值）
- `String` 类型字段 - currencyCode（货币代码）

```kotlin
private fun checkAndCreateAdValue(obj: Any, path: String, adType: String): AdValue? {
    var precision: PrecisionType? = null
    var valueMicros: Long? = null
    var currencyCode: String? = null
    
    // 遍历字段查找特征值...
    
    if (precision != null && valueMicros != null && currencyCode != null) {
        return createAdValue(precision, valueMicros, currencyCode)
    }
    return null
}
```

---

## 三、客户端竞价流程

### 3.1 开屏广告竞价流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        开屏广告竞价流程                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 预加载阶段                                                   │
│     loadInAdvance() → loadAd() → 缓存到 adCachePool             │
│                                                                 │
│  2. 竞价阶段（展示前）                                            │
│     getCachedAdPrice() → AdmobNextGenReflectionUtil             │
│                          .getRevenueByPath(cachedAd.ad)         │
│     返回 eCPM 值 → 与其他广告源比价                               │
│                                                                 │
│  3. 展示阶段                                                     │
│     displayAd() → 拦截器检查 → showAdInternal()                  │
│                                                                 │
│  4. 收益确认                                                     │
│     onAdPaid() 回调 → 上报真实收益                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 关键代码示例

```kotlin
// LaunchAds.kt - 获取缓存广告价格用于竞价
suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
    val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_SPLASH_ID
    
    // 1. 尝试从缓存获取广告（不移除）
    var cachedAd = peekCachedAd(finalAdUnitId)
    
    // 2. 如果缓存为空，立即加载
    if (cachedAd == null) {
        loadAdToCache(context, finalAdUnitId)
        cachedAd = peekCachedAd(finalAdUnitId)
    }
    
    // 3. 使用反射获取价格
    val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd?.ad)
    
    return adValue?.valueMicros?.div(1_000_000.0)
}
```

---

## 四、各广告类型 eCPM 获取对比

### 4.1 支持情况汇总

| 广告类型 | 展示后获取 (onAdPaid) | 展示前获取 (反射) | getCachedAdPrice() |
|----------|:---------------------:|:-----------------:|:------------------:|
| 开屏广告 (AppOpenAd) | ✅ | ✅ | ✅ 已实现 |
| 插屏广告 (InterstitialAd) | ✅ | ✅ | ✅ 已实现 |
| 激励视频 (RewardedAd) | ✅ | ✅ | ❌ 未实现 |
| 原生广告 (NativeAd) | ✅ | ✅ | ❌ 未实现 |
| 全屏原生 (NativeAd) | ✅ | ✅ | ❌ 未实现 |
| Banner广告 (BannerAd) | ✅ | ✅ | ❌ 未实现 |

### 4.2 复用性分析

**可完全复用的部分：**
1. `AdmobNextGenReflectionUtil` - 核心反射工具已支持所有广告类型
2. `AdValue` 数据结构 - 所有广告类型统一
3. 缓存池机制 - 所有广告类型都实现了 `adCachePool` + `peekCachedAd()`

**需要为每个广告类型添加的：**
```kotlin
// 示例：为激励视频添加 getCachedAdPrice()
suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
    val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID
    var cachedAd = peekCachedAd(finalAdUnitId)
    
    if (cachedAd == null) {
        load(context, finalAdUnitId)
        cachedAd = peekCachedAd(finalAdUnitId)
    }
    
    val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd?.ad)
    return adValue?.valueMicros?.div(1_000_000.0)
}

private fun peekCachedAd(adUnitId: String): CachedRewardedAd? {
    synchronized(cacheLock) {
        return adCachePool.firstOrNull { it.adUnitId == adUnitId && !it.isExpired() }
    }
}
```

---

## 五、拦截器机制

### 5.1 拦截器链结构

```kotlin
private val interceptorChain = InterceptorChain(
    interceptors = listOf(
        GlobalAdSwitchInterceptor(),    // 全局开关检查
        ShowCountLimitInterceptor(),    // 日展示次数限制
        ShowIntervalLimitInterceptor(), // 展示间隔限制
        ClickLimitInterceptor()         // 日点击次数限制
    )
)
```

### 5.2 拦截器执行流程

```
展示请求 → GlobalAdSwitch → ShowCountLimit → ShowIntervalLimit → ClickLimit → 展示广告
              ↓ 拦截              ↓ 拦截           ↓ 拦截            ↓ 拦截
           返回 Failure        返回 Failure     返回 Failure      返回 Failure
```

---

## 六、在其他工程中实现竞价逻辑

### 6.1 实现步骤

1. **复制反射工具类**
   - 将 `AdmobNextGenReflectionUtil.kt` 复制到新工程
   - 注意：反射路径可能随 SDK 版本变化，需要验证

2. **实现广告缓存池**
   ```kotlin
   private val adCachePool = mutableListOf<CachedAd>()
   
   private data class CachedAd(
       val ad: AppOpenAd,  // 或其他广告类型
       val adUnitId: String,
       val loadTime: Long = System.currentTimeMillis()
   )
   ```

3. **实现 peekCachedAd（不移除）**
   ```kotlin
   private fun peekCachedAd(adUnitId: String): CachedAd? {
       synchronized(adCachePool) {
           return adCachePool.firstOrNull { 
               it.adUnitId == adUnitId && !it.isExpired() 
           }
       }
   }
   ```

4. **实现 getCachedAdPrice**
   ```kotlin
   suspend fun getCachedAdPrice(context: Context, adUnitId: String): Double? {
       var cachedAd = peekCachedAd(adUnitId)
       if (cachedAd == null) {
           loadAdToCache(context, adUnitId)
           cachedAd = peekCachedAd(adUnitId)
       }
       val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd?.ad)
       return adValue?.valueMicros?.div(1_000_000.0)
   }
   ```

5. **竞价比价逻辑**
   ```kotlin
   suspend fun bidAndShow(context: Context) {
       // 获取各广告源的 eCPM
       val admobPrice = getCachedAdPrice(context, adUnitId)
       val otherPrice = otherAdSource.getPrice()
       
       // 选择最高价
       if (admobPrice != null && admobPrice > (otherPrice ?: 0.0)) {
           displayAd(context)
       } else {
           otherAdSource.show()
       }
   }
   ```

### 6.2 注意事项

1. **反射路径维护**
   - SDK 更新可能导致反射路径变化
   - 建议同时保留递归查找方式作为备选
   - 定期验证反射是否正常工作

2. **精度类型**
   - `PRECISE` - 精确值
   - `ESTIMATED` - 估算值
   - `PUBLISHER_PROVIDED` - 发布者提供
   - `UNKNOWN` - 未知
   
   竞价时应考虑精度类型的影响

3. **缓存过期**
   - 开屏广告：4小时
   - 其他广告：1小时
   - 过期广告应重新加载

4. **线程安全**
   - 缓存操作使用 `synchronized` 保护
   - 反射操作在主线程执行

---

## 七、eCPM 数据流

```
                    ┌──────────────────┐
                    │   广告加载完成    │
                    └────────┬─────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
    ┌─────────────────┐          ┌─────────────────┐
    │  缓存到内存池    │          │  反射获取 eCPM  │
    │  (adCachePool)  │          │  (用于竞价)     │
    └────────┬────────┘          └────────┬────────┘
             │                            │
             │                            ▼
             │                   ┌─────────────────┐
             │                   │   与其他广告源   │
             │                   │   比价竞争      │
             │                   └────────┬────────┘
             │                            │
             ▼                            ▼
    ┌─────────────────┐          ┌─────────────────┐
    │   展示广告      │◄─────────│  胜出则展示     │
    └────────┬────────┘          └─────────────────┘
             │
             ▼
    ┌─────────────────┐
    │  onAdPaid 回调  │
    │  确认真实收益   │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │   上报收益数据   │
    │  (RevenueAdManager)│
    └─────────────────┘
```

---

## 八、总结

### 8.1 核心能力

1. **展示前获取 eCPM** - 通过反射工具 `AdmobNextGenReflectionUtil` 实现
2. **统一接口** - 所有广告类型使用相同的反射工具
3. **缓存机制** - 预加载 + 缓存池确保快速获取价格
4. **完整的拦截器链** - 支持频控、间隔控制等

### 8.2 复用建议

| 组件 | 复用难度 | 说明 |
|------|----------|------|
| AdmobNextGenReflectionUtil | 低 | 直接复制即可 |
| 缓存池机制 | 低 | 通用模式，易于实现 |
| getCachedAdPrice | 低 | 模板代码，改广告类型即可 |
| 拦截器链 | 中 | 需根据业务调整配置 |
| 反射路径 | 高 | 需随 SDK 版本维护 |

### 8.3 风险提示

- 反射方式依赖 SDK 内部实现，**非官方支持**
- SDK 版本升级可能导致反射路径失效
- 建议添加日志监控反射成功率
- 保留递归查找作为降级方案

---

## 九、在其他工程中实现所需的依赖

### 9.1 必需的依赖库

#### Gradle 依赖 (build.gradle.kts)

```kotlin
dependencies {
    // ========== 核心必需 ==========
    
    // 1. AdMob Next-Gen SDK（竞价核心）★ 必需
    api("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:0.22.0-beta04")
    
    // 2. OkHttp（ads-mobile-sdk 的传递依赖）
    api("com.squareup.okhttp3:okhttp:4.12.0")
    
    // 3. Kotlin 协程（用于 suspend 函数）
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    
    // ========== 可选依赖 ==========
    
    // 4. AndroidX AppCompat（基础UI）
    api("androidx.appcompat:appcompat:1.7.0")
    
    // 5. Material Design（UI组件）
    api("com.google.android.material:material:1.13.0")
    
    // 6. UMP（用户隐私同意，GDPR合规）
    api("com.google.android.ump:user-messaging-platform:3.1.0")
    
    // ========== 聚合平台（可选）==========
    
    // 7. Facebook Audience Network 聚合
    api("com.google.ads.mediation:facebook:6.20.0.0")
    
    // 8. Pangle（穿山甲）聚合
    api("com.google.ads.mediation:pangle:7.5.0.4.0")
}

// 如果需要排除旧版 SDK，添加：
configurations.all {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}
```

### 9.2 需要复制的工具类

| 文件 | 路径 | 用途 | 是否必需 |
|------|------|------|----------|
| **AdmobNextGenReflectionUtil.kt** | `ads/util/` | eCPM反射获取核心 | ✅ 必需 |
| **AdLogger.kt** | `ads/log/` | 日志工具 | ⚠️ 可替换 |

#### 9.2.1 核心工具类：AdmobNextGenReflectionUtil

```kotlin
// 完整文件路径：monetize/src/main/java/net/corekit/monetize/ads/util/AdmobNextGenReflectionUtil.kt
// 该文件约 415 行，包含：
// - 各广告类型的反射路径
// - getRevenue() - 递归查找方式
// - getRevenueByPath() - 固定路径方式（推荐）
// - 辅助方法：traverse(), parseLeaf(), checkAndCreateAdValue() 等
```

#### 9.2.2 日志工具类：AdLogger（可选）

可以用以下简化版替代：

```kotlin
object AdLogger {
    private const val TAG = "AdModule"
    private var isEnabled = BuildConfig.DEBUG
    
    fun d(message: String, vararg args: Any?) {
        if (isEnabled) Log.d(TAG, message.format(*args))
    }
    
    fun w(message: String, vararg args: Any?) {
        if (isEnabled) Log.w(TAG, message.format(*args))
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled) Log.e(TAG, message, throwable)
    }
}
```

### 9.3 需要定义的数据类

```kotlin
// 广告结果封装
sealed class AdResult<out T> {
    data class Success<T>(val data: T) : AdResult<T>()
    data class Failure(val error: AdException) : AdResult<Nothing>()
    object Loading : AdResult<Nothing>()
}

// 广告异常
data class AdException(
    val code: Int,
    val message: String,
    val cause: Throwable? = null
)

// 缓存广告数据类（每种广告类型需要定义一个）
data class CachedAppOpenAd(
    val ad: AppOpenAd,
    val adUnitId: String,
    val loadTime: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - loadTime > 4 * 60 * 60 * 1000L
    }
}
```

### 9.4 最小化实现示例

如果只需要**获取 eCPM 进行竞价**，最小化实现如下：

```kotlin
class SplashAdBidding {
    private val adCache = mutableListOf<CachedAppOpenAd>()
    
    // 1. 加载广告
    suspend fun loadAd(context: Context, adUnitId: String): AppOpenAd? {
        return suspendCancellableCoroutine { continuation ->
            val adRequest = AdRequest.Builder(adUnitId).build()
            AppOpenAd.load(adRequest, object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    synchronized(adCache) {
                        adCache.add(CachedAppOpenAd(ad, adUnitId))
                    }
                    continuation.resume(ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    continuation.resume(null)
                }
            })
        }
    }
    
    // 2. 获取 eCPM（竞价核心）
    suspend fun getEcpm(context: Context, adUnitId: String): Double? {
        var cached = peekCachedAd(adUnitId)
        if (cached == null) {
            loadAd(context, adUnitId)
            cached = peekCachedAd(adUnitId)
        }
        val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cached?.ad)
        return adValue?.valueMicros?.div(1_000_000.0)
    }
    
    // 3. 查看缓存（不移除）
    private fun peekCachedAd(adUnitId: String): CachedAppOpenAd? {
        synchronized(adCache) {
            return adCache.firstOrNull { it.adUnitId == adUnitId && !it.isExpired() }
        }
    }
}
```

### 9.5 AndroidManifest 配置

```xml
<manifest>
    <application>
        <!-- AdMob Application ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
    </application>
</manifest>
```

### 9.6 ProGuard 规则（如启用混淆）

```proguard
# AdMob Next-Gen SDK
-keep class com.google.android.libraries.ads.mobile.sdk.** { *; }
-keepclassmembers class com.google.android.libraries.ads.mobile.sdk.** { *; }

# 反射相关 - 保留 AdValue 类
-keep class com.google.android.libraries.ads.mobile.sdk.common.AdValue { *; }
-keep class com.google.android.libraries.ads.mobile.sdk.common.PrecisionType { *; }
```

### 9.7 依赖关系图

```
┌─────────────────────────────────────────────────────────┐
│                    你的竞价模块                          │
├─────────────────────────────────────────────────────────┤
│  AdmobNextGenReflectionUtil.kt  ◄──── 核心反射工具      │
│  AdLogger.kt                    ◄──── 日志工具（可选）   │
│  AdResult.kt / AdException.kt   ◄──── 数据类            │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│           ads-mobile-sdk:0.22.0-beta04                  │
│           (AdMob Next-Gen SDK)                          │
├─────────────────────────────────────────────────────────┤
│  AppOpenAd, InterstitialAd, RewardedAd, NativeAd...    │
│  AdValue, PrecisionType, AdRequest, AdLoadCallback...   │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   OkHttp 4.12.0                         │
│           (SDK 网络请求依赖)                             │
└─────────────────────────────────────────────────────────┘
```

---

## 十、激励广告价格获取方案

### 10.1 实现原理

激励广告（RewardedAd）的 eCPM 获取方式与开屏、插屏广告完全一致：
1. 使用缓存池预加载广告
2. 通过 `peekCachedAd()` 获取缓存广告（不移除）
3. 使用反射工具 `AdmobNextGenReflectionUtil.getRevenueByPath()` 获取 eCPM

### 10.2 反射路径

```kotlin
// 激励视频的反射路径
private val rvStack = arrayOf("c", "a", "a", "k", "M", "c", "m")
```

### 10.3 实现代码模板

```kotlin
class RewardedAds {
    
    private val cacheLock = Any()
    private val adCachePool = mutableListOf<CachedRewardedAd>()
    
    private data class CachedRewardedAd(
        val ad: RewardedAd,
        val adUnitId: String,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - loadTime > 1 * 60 * 60 * 1000L
        }
    }
    
    /**
     * 查看缓存中的广告（不移除）
     * 用于获取价格进行竞价
     */
    private fun peekCachedAd(adUnitId: String): CachedRewardedAd? {
        synchronized(cacheLock) {
            return adCachePool.firstOrNull { it.adUnitId == adUnitId && !it.isExpired() }
        }
    }

    /**
     * 获取当前缓存广告的价格（用于竞价）
     * 如果缓存不存在则调用加载，使用反射获取价格后返回
     * @param context 上下文
     * @param adUnitId 广告位ID，如果为空则使用默认ID
     * @return 广告价格（已除以1000000），如果获取失败返回null
     */
    suspend fun getCachedAdPrice(context: Context, adUnitId: String? = null): Double? {
        val finalAdUnitId = adUnitId ?: BuildConfig.ADMOB_REWARDED_ID

        // 尝试从缓存获取广告（不移除）
        var cachedAd = peekCachedAd(finalAdUnitId)

        // 如果缓存为空，立即加载
        if (cachedAd == null) {
            AdLogger.d("获取价格时缓存为空，立即加载激励广告，广告位ID: %s", finalAdUnitId)
            load(context, finalAdUnitId)
            cachedAd = peekCachedAd(finalAdUnitId)
        }

        if (cachedAd == null) {
            AdLogger.w("获取激励广告价格失败：缓存为空")
            return null
        }

        // 使用反射获取价格
        val adValue = AdmobNextGenReflectionUtil.getRevenueByPath(cachedAd.ad)

        return if (adValue != null) {
            val price = adValue.valueMicros / 1_000_000.0
            AdLogger.d("获取激励广告价格成功: %f", price)
            price
        } else {
            AdLogger.w("获取激励广告价格失败：反射获取AdValue为空")
            null
        }
    }
}
```

### 10.4 使用示例

```kotlin
// 获取激励广告的 eCPM 用于竞价
val rewardedEcpm = RewardedAds.getInstance().getCachedAdPrice(context)

// 与其他广告源比价
if (rewardedEcpm != null && rewardedEcpm > otherAdSourcePrice) {
    RewardedAds.getInstance().show(activity)
} else {
    otherAdSource.show()
}
```

---

## 附录A：AdmobNextGenReflectionUtil 完整代码

```kotlin
package net.corekit.monetize.ads.util

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import java.lang.reflect.Field

/**
 * AdMob next-gen 相关的反射工具，统一获取广告的AdValue。
 */
object AdmobNextGenReflectionUtil {

    // 各广告类型的固定路径
    private val ivStackV1 = arrayOf("b", "k", "L", "e", "b", "j", "a", "M", "c", "m")
    private val ivStackV2 = arrayOf("b", "k", "M", "c", "m")

    private val spStack = arrayOf("b", "k", "M", "c", "m")

    private val nativeStackV1 = arrayOf("b", "l", "j", "e", "b", "j", "a", "M", "c", "m")
    private val nativeStackV2 = arrayOf("b", "l", "s", "e", "m")

    private val bannerStack = arrayOf("b", "k", "a", "d", "d", "a", "m")

    private val rvStack = arrayOf("c", "a", "a", "k", "M", "c", "m")

    /**
     * 通过反射获取任意 AdMob 广告收益信息，当前支持 Banner、开屏、插页、激励、原生。
     * 使用递归查找方式，适用于未知路径的情况。
     * @param ad 广告对象
     * @return [AdValue]，未获取到返回 null
     */
    fun getRevenue(ad: Any?): AdValue? {
        if (ad == null) return null
        return when (ad) {
            is InterstitialAd -> findAdValueRecursively(ad, "插页")
            is AppOpenAd -> findAdValueRecursively(ad, "开屏")
            is RewardedAd -> findAdValueRecursively(ad, "激励")
            is NativeAd -> findAdValueRecursively(ad, "原生")
            is BannerAd -> findAdValueRecursively(ad, "Banner")
            else -> null
        } ?: run {
            logW("AdmobReflectionUtil: 未能通过反射解析到收益信息，ad=${ad::class.java.simpleName}")
            null
        }
    }

    /**
     * 通过固定路径获取任意 AdMob 广告收益信息，当前支持 Banner、开屏、插页、激励、原生。
     * 使用固定路径方式，性能更好，适用于已知路径的情况。
     * @param ad 广告对象
     * @return [AdValue]，未获取到返回 null
     */
    fun getRevenueByPath(ad: Any?): AdValue? {
        if (ad == null) return null
        return when (ad) {
            is InterstitialAd -> findAdValueByPath(ad, "插页", listOf(ivStackV1, ivStackV2))
            is AppOpenAd -> findAdValueByPath(ad, "开屏", listOf(spStack))
            is RewardedAd -> findAdValueByPath(ad, "激励", listOf(rvStack))
            is NativeAd -> findAdValueByPath(ad, "原生", listOf(nativeStackV1, nativeStackV2))
            is BannerAd -> findAdValueByPath(ad, "Banner", listOf(bannerStack))
            else -> null
        } ?: run {
            logW("AdmobReflectionUtil: 未能通过固定路径解析到收益信息，ad=${ad::class.java.simpleName}")
            null
        }
    }

    /**
     * 通过固定路径查找 AdValue
     * 如果第一个路径的价格为0，则尝试第二个路径
     */
    private fun findAdValueByPath(ad: Any, adType: String, pathList: List<Array<String>>): AdValue? {
        var lastAdValue: AdValue? = null
        val hasMultiplePaths = pathList.size > 1
        
        pathList.forEachIndexed { index, stack ->
            val leaf = traverse(ad, stack, adType)
            if (leaf != null) {
                val adValue = parseLeaf(leaf, stack, adType)
                if (adValue != null) {
                    // 如果价格不为0，直接返回
                    if (adValue.valueMicros > 0) {
                        logD("AdmobReflectionUtil: [$adType] 通过路径获取到有效价格: ${adValue.valueMicros}")
                        return adValue
                    }
                    // 如果价格为0，保存并继续尝试下一个路径
                    lastAdValue = adValue
                    if (hasMultiplePaths && index < pathList.size - 1) {
                        logD("AdmobReflectionUtil: [$adType] 路径价格为0，尝试下一个路径")
                    }
                }
            }
        }
        return lastAdValue
    }

    /**
     * 根据路径遍历获取对象
     */
    private fun traverse(target: Any, stack: Array<String>, adType: String): Any? {
        var current: Any? = target
        stack.forEach { fieldName ->
            val fieldValue = current?.getValue(fieldName)
            if (fieldValue == null) {
                return null
            }
            current = fieldValue
        }
        return current
    }

    /**
     * 解析叶子节点
     */
    private fun parseLeaf(leaf: Any, stack: Array<String>, adType: String): AdValue? {
        // 如果是 AdValue 类型，直接返回
        if (leaf is AdValue) {
            return leaf
        }
        // 检查当前对象是否包含 AdValue 的特征字段
        return checkAndCreateAdValue(leaf, adType)
    }

    /**
     * 递归查找 AdValue 对象
     */
    private fun findAdValueRecursively(
        obj: Any?, 
        adType: String, 
        visited: MutableSet<Any> = mutableSetOf(), 
        depth: Int = 0
    ): AdValue? {
        if (obj == null || depth > 10) return null
        
        val identity = System.identityHashCode(obj)
        if (visited.any { System.identityHashCode(it) == identity }) return null
        visited.add(obj)

        return try {
            if (obj is AdValue) return obj
            
            checkAndCreateAdValue(obj, adType)?.let { return it }

            var clazz: Class<*>? = obj::class.java
            while (clazz != null) {
                val fields = clazz.declaredFields
                for (field in fields) {
                    try {
                        field.isAccessible = true
                        val fieldValue = field.get(obj) ?: continue
                        if (isPrimitiveOrBasicType(field.type)) continue
                        findAdValueRecursively(fieldValue, adType, visited, depth + 1)?.let { return it }
                    } catch (e: Exception) {
                        continue
                    }
                }
                clazz = clazz.superclass
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查对象是否包含 AdValue 的特征字段，并尝试创建 AdValue
     */
    private fun checkAndCreateAdValue(obj: Any, adType: String): AdValue? {
        return try {
            var precision: PrecisionType? = null
            var valueMicros: Long? = null
            var currencyCode: String? = null

            var clazz: Class<*>? = obj::class.java
            while (clazz != null) {
                val fields = clazz.declaredFields
                for (field in fields) {
                    try {
                        field.isAccessible = true
                        val fieldValue = field.get(obj) ?: continue

                        when {
                            field.type == PrecisionType::class.java && fieldValue is PrecisionType -> {
                                precision = fieldValue
                            }
                            (field.type == Long::class.javaPrimitiveType || field.type == Long::class.javaObjectType) 
                                    && fieldValue is Long -> {
                                if (valueMicros == null || (fieldValue > 0 && fieldValue > (valueMicros ?: 0))) {
                                    valueMicros = fieldValue
                                }
                            }
                            field.type == String::class.java && fieldValue is String && fieldValue.isNotBlank() -> {
                                if (currencyCode == null || (fieldValue.length <= 5 && fieldValue.length >= 2)) {
                                    currencyCode = fieldValue
                                }
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                clazz = clazz.superclass
            }

            if (precision != null && valueMicros != null && currencyCode != null) {
                createAdValue(precision, valueMicros, currencyCode)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通过字段名获取对象的值
     */
    private fun Any?.getValue(fieldName: String): Any? {
        if (this == null) return null
        return try {
            var clazz: Class<*>? = this::class.java
            var field: Field? = null
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName).apply { isAccessible = true }
                    break
                } catch (ignored: NoSuchFieldException) {
                    clazz = clazz.superclass
                }
            }
            field?.get(this)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 判断是否为基础类型
     */
    private fun isPrimitiveOrBasicType(type: Class<*>): Boolean {
        return when {
            type.isPrimitive -> true
            type == Boolean::class.javaObjectType || type == Boolean::class.javaPrimitiveType -> true
            type == Byte::class.javaObjectType || type == Byte::class.javaPrimitiveType -> true
            type == Character::class.javaObjectType || type == Char::class.javaPrimitiveType -> true
            type == Short::class.javaObjectType || type == Short::class.javaPrimitiveType -> true
            type == Int::class.javaObjectType || type == Int::class.javaPrimitiveType -> true
            type == Long::class.javaObjectType || type == Long::class.javaPrimitiveType -> true
            type == Float::class.javaObjectType || type == Float::class.javaPrimitiveType -> true
            type == Double::class.javaObjectType || type == Double::class.javaPrimitiveType -> true
            type == String::class.java -> true
            type.isArray && isPrimitiveOrBasicType(type.componentType) -> true
            type.name.startsWith("java.lang.") -> true
            else -> false
        }
    }

    /**
     * 通过反射创建 AdValue 实例
     */
    private fun createAdValue(precision: PrecisionType, valueMicros: Long, currencyCode: String): AdValue? {
        return try {
            val constructor = AdValue::class.java.getDeclaredConstructor(
                PrecisionType::class.java,
                Long::class.javaPrimitiveType,
                String::class.java
            )
            constructor.isAccessible = true
            constructor.newInstance(precision, valueMicros, currencyCode) as AdValue
        } catch (e: Exception) {
            null
        }
    }
    
    // 日志方法（可替换为项目中的日志工具）
    private fun logD(msg: String) { android.util.Log.d("AdModule", msg) }
    private fun logW(msg: String) { android.util.Log.w("AdModule", msg) }
}
```

### 反射路径汇总表

| 广告类型 | 反射路径 | 备注 |
|----------|----------|------|
| 开屏 (AppOpenAd) | `b→k→M→c→m` | 单一路径 |
| 插屏 (InterstitialAd) | `b→k→L→e→b→j→a→M→c→m` | 优先路径 |
| 插屏 (InterstitialAd) | `b→k→M→c→m` | 备用路径 |
| 激励 (RewardedAd) | `c→a→a→k→M→c→m` | 单一路径 |
| 原生 (NativeAd) | `b→l→j→e→b→j→a→M→c→m` | 优先路径 |
| 原生 (NativeAd) | `b→l→s→e→m` | 备用路径 |
| Banner (BannerAd) | `b→k→a→d→d→a→m` | 单一路径 |
