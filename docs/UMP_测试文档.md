# UMP 同意管理流程测试文档

## 1. 概述

UMP (User Messaging Platform) 是 Google 提供的用户同意管理平台，用于在 GDPR 覆盖区域收集用户隐私同意。

### 1.1 核心流程

```
App 启动
    │
    ├─ 并行1: IP 国家代码预取（3秒超时，1小时缓存）
    ├─ 并行2: 通知权限检查（用户交互）
    └─ 并行3: 广告加载（后台，等待阻塞放开）
    │
    ↓ 等待权限和 IP 完成
    │
UMP 检查
    ├─ 非 GDPR 区域 → 跳过 UMP
    └─ GDPR 区域 → 展示 UMP 弹窗
    │
    ↓
放开广告展示阻塞
    │
    ↓ 超时计时开始
    │
广告展示 / 超时兜底
    │
    ↓
跳转首页
```

### 1.2 GDPR 覆盖区域

包含 31 个国家/地区：
- **欧盟 27 国**: AT, BE, BG, HR, CY, CZ, DK, EE, FI, FR, DE, GR, HU, IE, IT, LV, LT, LU, MT, NL, PL, PT, RO, SK, SI, ES, SE
- **欧洲经济区**: IS, LI, NO
- **其他**: GB (英国), CH (瑞士)

---

## 2. 关键日志 (Logcat Filter: `UmpController|GeoLocation|SplashScreen`)

### 2.1 IP 预取日志

```
# 首次查询（无缓存）
[UmpController] 开始预取国家代码...
[UmpController] 开始 IP 地理位置查询...
[GeoLocation] IP 查询成功，国家代码: US
[UmpController] 国家代码已缓存: US，有效期: 1小时
[UmpController] 预取国家代码完成: US

# 使用缓存
[UmpController] 开始预取国家代码...
[UmpController] 使用缓存的国家代码: US，剩余有效时间: 45分钟
[UmpController] 预取国家代码完成: US

# 缓存过期
[UmpController] 开始预取国家代码...
[UmpController] 国家代码缓存已过期，重新查询...
[UmpController] 开始 IP 地理位置查询...
[GeoLocation] IP 查询成功，国家代码: DE
[UmpController] 国家代码已缓存: DE，有效期: 1小时
[UmpController] 预取国家代码完成: DE

# IP 查询失败
[UmpController] 开始预取国家代码...
[UmpController] 开始 IP 地理位置查询...
[GeoLocation] IP 查询异常: timeout
[UmpController] IP 查询失败，无法获取国家代码
[UmpController] 预取国家代码完成: 未知
```

### 2.2 UMP 检查日志

```
# 非 GDPR 区域（跳过 UMP）
[SplashScreen] 开始 UMP 同意检查
[UmpController] ========== 开始 UMP 同意检查流程 ==========
[UmpController] 用户国家代码: US
[UmpController] 非 GDPR 区域，跳过 UMP 弹窗
[UmpController] ========== UMP 同意检查流程结束（跳过）==========
[SplashScreen] UMP 同意检查完成

# GDPR 区域（展示 UMP）
[SplashScreen] 开始 UMP 同意检查
[UmpController] ========== 开始 UMP 同意检查流程 ==========
[UmpController] 用户国家代码: DE
[UmpController] GDPR 区域用户，开始展示 UMP 弹窗
[UmpController] UMP 弹窗完成，canRequestAds: true
[UmpController] isPrivacyOptionsRequired: true
[UmpController] ========== UMP 同意检查流程结束 ==========
[SplashScreen] UMP 同意检查完成

# 无国家代码（跳过 UMP）
[SplashScreen] 开始 UMP 同意检查
[UmpController] ========== 开始 UMP 同意检查流程 ==========
[UmpController] 用户国家代码: 未知
[UmpController] 非 GDPR 区域，跳过 UMP 弹窗
[UmpController] ========== UMP 同意检查流程结束（跳过）==========
[SplashScreen] UMP 同意检查完成
```

### 2.3 广告阻塞日志

```
# 广告等待阻塞
[LaunchAds] 准备执行开屏拦截等待
[权限] 权限授权完成，取消拦截
[LaunchAds] 开屏拦截等待结束
```

---

## 3. 测试用例

### 3.1 非 GDPR 区域测试

| 测试项 | 预期结果 |
|--------|----------|
| 首次启动 | IP 查询 → 返回非 GDPR 国家代码 → 跳过 UMP → 直接展示广告 |
| 再次启动（1小时内） | 使用缓存国家代码 → 跳过 UMP → 直接展示广告 |
| 日志验证 | 包含 "非 GDPR 区域，跳过 UMP 弹窗" |

### 3.2 GDPR 区域测试（需 VPN 或调试模式）

| 测试项 | 预期结果 |
|--------|----------|
| 首次启动 | IP 查询 → 返回 GDPR 国家代码 → 展示 UMP 弹窗 → 用户选择后展示广告 |
| 用户同意 | UMP 弹窗关闭 → 广告正常展示 |
| 用户拒绝 | UMP 弹窗关闭 → 广告仍可展示（非个性化） |
| 日志验证 | 包含 "GDPR 区域用户，开始展示 UMP 弹窗" |

### 3.3 异常场景测试

| 测试项 | 预期结果 |
|--------|----------|
| 网络断开 | IP 查询失败 → 使用缓存或跳过 UMP → 广告正常展示 |
| IP 查询超时（3秒） | 超时后跳过 → 使用缓存或跳过 UMP → 广告正常展示 |
| UMP SDK 异常 | 捕获异常 → 跳过 UMP → 广告正常展示 |

### 3.4 缓存过期测试

| 测试项 | 预期结果 |
|--------|----------|
| 缓存未过期 | 日志显示 "使用缓存的国家代码: XX，剩余有效时间: YY分钟" |
| 缓存过期（1小时后） | 日志显示 "国家代码缓存已过期，重新查询..." |

---

## 4. 调试方法

### 4.1 强制模拟 GDPR 区域

在 `GoogleMobileAdsConsentManager.kt` 中启用调试模式：

```kotlin
val debugSettings = ConsentDebugSettings.Builder(activity)
    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    .addTestDeviceHashedId("YOUR_DEVICE_HASH_ID")  // 从 Logcat 获取
    .build()
```

### 4.2 清除缓存测试

```kotlin
// 清除国家代码缓存
UmpConsentController.clearCountryCodeCache()

// 重置 UMP 同意状态
UmpConsentController.resetUmpConsentState(context)

// 重置会话状态
UmpConsentController.resetSessionCheckState()
```

### 4.3 Logcat 过滤器

```
tag:UmpController | tag:GeoLocation | tag:SplashScreen | tag:LaunchAds
```

---

## 5. 关键文件

| 文件 | 职责 |
|------|------|
| `UmpConsentController.kt` | UMP 流程总控制器 |
| `GeoLocationService.kt` | IP 地理位置查询（3秒超时） |
| `GdprRegionChecker.kt` | GDPR 区域判断 |
| `GoogleMobileAdsConsentManager.kt` | UMP SDK 封装 |
| `SplashScreen.kt` | 启动页流程集成 |
| `LaunchAds.kt` | 开屏广告控制（含阻塞机制） |

---

## 6. 注意事项

1. **IP 缓存有效期**：1 小时，过期后自动重新查询
2. **广告不阻塞**：广告加载与 UMP 并行，仅展示时等待 UMP 完成
3. **超时计时**：从 UMP 完成后开始计时，不受 UMP 弹窗影响
4. **兜底策略**：任何异常都不阻塞广告展示

---

## 7. 版本信息

- **更新日期**: 2026-01-07
- **UMP SDK 版本**: 参见 `monetize/build.gradle.kts`
- **AdMob SDK 版本**: 参见 `monetize/build.gradle.kts`
