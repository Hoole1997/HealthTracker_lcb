# 广告竞价配置使用指南

> 本文档基于代码实现说明各配置项的作用和影响

## 配置来源与优先级

配置按以下优先级加载（高 → 低）：
1. **Firebase Remote Config** (`biddingConfigJson`)
2. **本地 assets** (`bidding_config_default.json`)
3. **代码硬编码默认值**

**代码位置**: `BiddingConfigManager.kt` → `loadConfig()`

---

## 配置结构总览

```
bidding_config
├── platform_frequency_enabled     # 全局频控开关
├── platform_frequency             # 平台级频控配置
├── free_user                      # 免费用户渠道配置
│   ├── bidding_enabled
│   ├── two_layer_bidding_enabled
│   ├── bidding_timeout_seconds
│   ├── platforms
│   │   └── {platform}
│   │       ├── enabled
│   │       ├── priority
│   │       └── ad_types
│   │           └── {ad_type}
│   │               ├── enabled
│   │               └── participate_bidding
│   └── scene_config
└── premium_user                   # 付费用户渠道配置（结构同上）
```

---

## 一、全局配置

### 1.1 `platform_frequency_enabled`

| 属性 | 值 |
|-----|-----|
| **类型** | `Boolean` |
| **默认值** | `false` |
| **作用域** | 全局（所有渠道） |

**作用**：控制是否启用平台级频控限制。

| 值 | 行为 |
|---|------|
| `true` | 启用频控，广告展示/点击受 `platform_frequency` 配置限制 |
| `false` | 禁用频控，所有广告不受展示次数和间隔限制 |

**代码实现**：
```kotlin
// PlatformFrequencyManager.kt
fun canParticipate(platform: BiddingPlatform, adType: BiddingAdType): Boolean {
    if (!isEnabled()) {  // 检查 platform_frequency_enabled
        return true      // 频控未启用，直接允许
    }
    // ... 检查展示次数、点击次数、时间间隔
}
```

**⚠️ 注意**：此开关仅控制频控，**不影响广告加载和竞价**。要禁止特定平台加载广告，需设置 `platforms.{platform}.enabled = 0`。

---

### 1.2 `platform_frequency`

平台级频控的具体限制配置。

```json
"platform_frequency": {
    "{platform}": {
        "{ad_type}": {
            "max_daily_show": 50,
            "max_daily_click": 10,
            "min_show_interval_seconds": 30
        }
    }
}
```

| 字段 | 类型 | 说明 |
|-----|------|-----|
| `max_daily_show` | `Int` | 每日最大展示次数（0点重置） |
| `max_daily_click` | `Int` | 每日最大点击次数（0点重置） |
| `min_show_interval_seconds` | `Int` | 两次展示的最小间隔秒数 |

**支持的平台**: `admob`, `pangle`, `topon`

**支持的广告类型**: `splash`, `interstitial`, `rewarded`, `rewarded_interstitial`, `native`, `full_native`, `banner`

**代码实现**：
```kotlin
// PlatformFrequencyManager.kt
fun canParticipate(platform, adType): Boolean {
    val config = getFrequencyConfig(platform, adType)
    
    // 检查每日展示上限
    if (getDailyShowCount(platform, adType) >= config.maxDailyShow) return false
    
    // 检查每日点击上限
    if (getDailyClickCount(platform, adType) >= config.maxDailyClick) return false
    
    // 检查展示间隔
    if (getSecondsSinceLastShow(platform, adType) < config.minShowIntervalSeconds) return false
    
    return true
}
```

---

## 二、渠道配置 (`free_user` / `premium_user`)

渠道通过用户类型区分，代码中通过 `BiddingConfigManager.getCurrentChannel()` 获取当前渠道。

### 2.1 `bidding_enabled`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` (0/1) |
| **默认值** | `1` |
| **作用域** | 当前渠道 |

**作用**：控制当前渠道是否启用多平台竞价。

| 值 | 行为 |
|---|------|
| `1` | 启用竞价，多平台广告参与价格比较 |
| `0` | 禁用竞价，仅使用 fallback 平台广告 |

**代码实现**：
```kotlin
// BiddingConfigManager.kt
fun isBiddingEnabled(): Boolean {
    return getCurrentChannelConfig()?.biddingEnabled == 1
}
```

---

### 2.2 `two_layer_bidding_enabled`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` (0/1) |
| **默认值** | `1` |
| **作用域** | 当前渠道 |

**作用**：控制是否启用两层竞价策略。

| 值 | 行为 |
|---|------|
| `1` | 启用两层竞价（平台内竞价 + 跨平台竞价） |
| `0` | 仅使用单层竞价 |

**两层竞价流程**：
```
第一层（平台内）：splash vs interstitial
       ↓ 选出各平台最优
第二层（跨平台）：AdMob winner vs Pangle winner vs TopOn winner
       ↓ 选出全局最优
       展示
```

---

### 2.3 `bidding_timeout_seconds`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` |
| **默认值** | `10` |
| **作用域** | 当前渠道（全局） |

**作用**：渠道级别的竞价超时时间（秒）。当场景未配置独立超时时使用此值。

**⚠️ 注意**：场景配置 `scene_config.{scene}.timeout_seconds` 优先级更高，会覆盖此值。

**超时优先级**：
```
场景超时 (scene_config.{scene}.timeout_seconds)
    ↓ 未配置
渠道全局超时 (bidding_timeout_seconds)
    ↓ 未配置
默认值 (10 秒)
```

---

## 三、平台配置 (`platforms.{platform}`)

### 3.1 `enabled`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` (0/1) |
| **默认值** | `1` |
| **作用域** | 指定平台 |

**作用**：控制该平台是否启用。

| 值 | 行为 |
|---|------|
| `1` | 平台启用，允许加载广告和参与竞价 |
| `0` | **平台禁用，不加载广告，不参与竞价** |

**代码实现**：
```kotlin
// BiddingPlatformController.kt
fun isPlatformEnabled(platform: BiddingWinner): Boolean {
    val platformConfig = getPlatformConfig(platform)
    return platformConfig?.enabled == 1
}

fun shouldParticipateInPreload(platform, adType): Boolean {
    // 1. 检查平台是否启用
    if (!isPlatformEnabled(platform)) {
        AdLogger.d("[$TAG] %s 平台未启用，跳过预加载", platform.name)
        return false
    }
    // ...
}
```

**💡 如何仅启用 AdMob**：
```json
"platforms": {
    "admob": { "enabled": 1 },
    "pangle": { "enabled": 0 },
    "topon": { "enabled": 0 }
}
```

---

### 3.2 `priority`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` |
| **默认值** | 无 |
| **作用域** | 指定平台 |

**作用**：平台优先级，数值越小优先级越高。当多个平台竞价结果相同时，选择优先级高的。

---

## 四、广告类型配置 (`ad_types.{ad_type}`)

### 4.1 `enabled`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` (0/1) |
| **默认值** | `1` |
| **作用域** | 指定平台的指定广告类型 |

**作用**：控制该广告类型是否启用（影响预加载）。

| 值 | 行为 |
|---|------|
| `1` | 广告类型启用，允许预加载 |
| `0` | **广告类型禁用，不预加载，不参与竞价** |

---

### 4.2 `participate_bidding`

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` (0/1) |
| **默认值** | `1` |
| **作用域** | 指定平台的指定广告类型 |

**作用**：控制该广告类型是否参与竞价（仅影响竞价，不影响预加载）。

| 值 | 行为 |
|---|------|
| `1` | 参与竞价 |
| `0` | **不参与竞价，但仍会预加载（可作为兜底广告）** |

**两个配置的区别**：

| 配置组合 | 预加载 | 参与竞价 | 典型用途 |
|---------|--------|---------|----------|
| `enabled=1, participate_bidding=1` | ✅ | ✅ | 正常参与竞价 |
| `enabled=1, participate_bidding=0` | ✅ | ❌ | 仅作为兜底广告预缓存 |
| `enabled=0, participate_bidding=*` | ❌ | ❌ | 完全禁用该广告类型 |

**代码实现**：
```kotlin
// BiddingPlatformController.kt

// 预加载检查：仅检查 enabled
fun shouldParticipateInPreload(platform, adType): Boolean {
    if (!isPlatformEnabled(platform)) return false
    if (!AdIdHelper.hasValidAdId(platform, adType)) return false
    
    val adTypeConfig = platformConfig?.adTypes?.get(adType)
    if (adTypeConfig == null) return false  // 配置缺失，默认不参与
    if (adTypeConfig.enabled != 1) return false
    
    return true  // 允许预加载，不检查 participate_bidding
}

// 竞价检查：额外检查 participate_bidding
fun shouldParticipateInBidding(platform, adType): Boolean {
    if (!shouldParticipateInPreload(platform, adType)) return false
    
    val adTypeConfig = platformConfig?.adTypes?.get(adType)
    if (adTypeConfig?.participateBidding != 1) return false  // 检查竞价配置
    
    // ... 频控检查
    return true
}
```

---

## 五、场景配置 (`scene_config`)

场景配置用于针对不同业务场景定制竞价行为。

**支持的场景 Key**：
| 场景 Key | 说明 |
|----------|------|
| `splash` | 开屏场景 |
| `reward` | 激励场景 |
| `interstitial` | 插屏场景 |

### 5.1 `bidding_mode`

| 值 | 说明 |
|---|------|
| `"two_layer"` | 使用两层竞价 |
| `"single"` | 使用单层竞价 |

---

### 5.2 `internal_bidding_types`

参与平台内竞价的广告类型列表。

```json
"internal_bidding_types": ["splash", "interstitial"]
```

表示在 splash 场景中，会让 `splash` 和 `interstitial` 两种广告类型在平台内先竞价。

---

### 5.3 `timeout_seconds` ⭐ 新增

| 属性 | 值 |
|-----|-----|
| **类型** | `Int` |
| **默认值** | `null`（使用渠道全局超时） |
| **作用域** | 指定场景 |

**作用**：场景级别的竞价超时时间（秒），优先级高于渠道全局超时。

**典型配置**：
```json
"scene_config": {
    "splash": {
        "timeout_seconds": 5    // 开屏场景 5 秒快速响应
    },
    "reward": {
        "timeout_seconds": 10   // 激励场景 10 秒，允许更长加载时间
    }
}
```

**设计考量**：
- 开屏场景需要快速响应，避免用户等待过久 → 建议 5 秒
- 激励场景用户主动触发，可接受更长等待 → 建议 10 秒

**代码实现**：
```kotlin
// BiddingConfigManager.kt
fun getBiddingTimeoutMs(scene: String? = null): Long {
    val channelConfig = getCurrentChannelConfig() ?: return 10000L
    
    // 1. 优先取场景独立超时
    if (scene != null) {
        val sceneTimeout = channelConfig.sceneConfig?.get(scene)?.timeoutSeconds
        if (sceneTimeout != null) {
            return sceneTimeout * 1000L
        }
    }
    
    // 2. 取渠道全局超时
    return channelConfig.biddingTimeoutSeconds * 1000L
}
```

---

### 5.4 `fallback_platform` / `fallback_ad_type`

竞价失败或超时时的兜底配置。

| 字段 | 说明 |
|-----|------|
| `fallback_platform` | 兜底平台（如 `"admob"`） |
| `fallback_ad_type` | 兜底广告类型（如 `"splash"`） |

---

## 六、配置决策流程图

### 预加载流程 (`shouldParticipateInPreload`)

```
预加载请求
    │
    ▼
┌─────────────────────────────┐
│ 1. isPlatformEnabled()      │ ──No──► 跳过该平台
│    platforms.xxx.enabled=1? │
└─────────────────────────────┘
    │ Yes
    ▼
┌─────────────────────────────┐
│ 2. hasValidAdId()           │ ──No──► 跳过该广告类型
│    有有效的广告 ID?          │
└─────────────────────────────┘
    │ Yes
    ▼
┌─────────────────────────────┐
│ 3. adTypeConfig != null?    │ ──No──► 跳过（配置缺失）
│    广告类型有配置?           │
└─────────────────────────────┘
    │ Yes
    ▼
┌─────────────────────────────┐
│ 4. adTypeConfig.enabled=1?  │ ──No──► 跳过该广告类型
│    广告类型启用?             │
└─────────────────────────────┘
    │ Yes
    ▼
  ✅ 允许预加载（不检查 participate_bidding）
```

### 竞价流程 (`shouldParticipateInBidding`)

```
竞价请求
    │
    ▼
┌─────────────────────────────┐
│ 1-4. 预加载检查通过?         │ ──No──► 跳过竞价
│      shouldParticipateIn    │
│      Preload() == true      │
└─────────────────────────────┘
    │ Yes
    ▼
┌─────────────────────────────┐
│ 5. participateBidding=1?    │ ──No──► 跳过竞价（仍可作为兜底）
│    参与竞价?                 │
└─────────────────────────────┘
    │ Yes
    ▼
┌─────────────────────────────┐
│ 6. canParticipate()         │ ──No──► 频控限制，跳过竞价
│    (仅当 platform_frequency │
│     _enabled=true 时检查)   │
└─────────────────────────────┘
    │ Yes
    ▼
  ✅ 参与竞价
```

---

## 七、常见配置场景

### 场景 1：仅使用 AdMob

```json
{
    "free_user": {
        "bidding_enabled": 1,
        "platforms": {
            "admob": { "enabled": 1 },
            "pangle": { "enabled": 0 },
            "topon": { "enabled": 0 }
        }
    }
}
```

### 场景 2：禁用所有竞价，直接使用 AdMob

```json
{
    "free_user": {
        "bidding_enabled": 0,
        "platforms": {
            "admob": { "enabled": 1 }
        }
    }
}
```

### 场景 3：AdMob 仅展示 splash，Pangle 仅展示 interstitial

```json
{
    "free_user": {
        "platforms": {
            "admob": {
                "enabled": 1,
                "ad_types": {
                    "splash": { "enabled": 1, "participate_bidding": 1 },
                    "interstitial": { "enabled": 0, "participate_bidding": 0 }
                }
            },
            "pangle": {
                "enabled": 1,
                "ad_types": {
                    "splash": { "enabled": 0, "participate_bidding": 0 },
                    "interstitial": { "enabled": 1, "participate_bidding": 1 }
                }
            }
        }
    }
}
```

### 场景 4：关闭频控限制

```json
{
    "platform_frequency_enabled": false
}
```

### 场景 5：限制 Pangle 每日最多展示 10 次 splash

```json
{
    "platform_frequency_enabled": true,
    "platform_frequency": {
        "pangle": {
            "splash": {
                "max_daily_show": 10,
                "max_daily_click": 5,
                "min_show_interval_seconds": 60
            }
        }
    }
}
```

### 场景 6：为不同场景设置不同超时时间

```json
{
    "free_user": {
        "bidding_timeout_seconds": 10,
        "scene_config": {
            "splash": {
                "bidding_mode": "two_layer",
                "timeout_seconds": 5,
                "fallback_platform": "admob",
                "fallback_ad_type": "splash"
            },
            "reward": {
                "bidding_mode": "two_layer",
                "timeout_seconds": 15,
                "fallback_platform": "admob",
                "fallback_ad_type": "rewarded"
            }
        }
    }
}
```

**效果**：
- 开屏场景：5 秒超时（快速响应）
- 激励场景：15 秒超时（允许更长加载）
- 其他场景：使用渠道全局 10 秒超时

---

## 八、配置优先级总结

| 层级 | 配置项 | 效果 |
|-----|--------|-----|
| **全局** | `platform_frequency_enabled` | 控制频控开关 |
| **渠道** | `bidding_enabled` | 控制是否启用竞价 |
| **渠道** | `two_layer_bidding_enabled` | 控制竞价策略 |
| **渠道** | `bidding_timeout_seconds` | 渠道全局超时 |
| **场景** | `scene_config.xxx.timeout_seconds` | **场景级超时（优先级最高）** |
| **平台** | `platforms.xxx.enabled` | **控制平台是否加载广告** |
| **广告类型** | `ad_types.xxx.enabled` | **控制广告类型是否加载** |
| **广告类型** | `ad_types.xxx.participate_bidding` | **控制是否参与竞价** |

**关键结论**：
- 要禁止某平台加载广告 → 设置 `platforms.xxx.enabled = 0`
- 要禁止某广告类型加载 → 设置 `ad_types.xxx.enabled = 0`
- 要禁止某广告类型竞价 → 设置 `ad_types.xxx.participate_bidding = 0`
- `platform_frequency_enabled` 仅控制频控，不影响广告加载

---

## 九、相关代码文件

| 文件 | 作用 |
|-----|------|
| `BiddingConfigData.kt` | 配置数据类定义 |
| `BiddingConfigManager.kt` | 配置加载与访问 |
| `BiddingPlatformController.kt` | 平台/广告类型启用判断 |
| `PlatformFrequencyManager.kt` | 频控逻辑实现 |
| `AppOpenPreloadManager.kt` | 开屏广告预加载 |
| `InterstitialPreloadManager.kt` | 插屏广告预加载 |
| `SplashTwoLayerPreloadManager.kt` | 两层竞价编排 |

---

*文档生成时间: 2026-01-20*
