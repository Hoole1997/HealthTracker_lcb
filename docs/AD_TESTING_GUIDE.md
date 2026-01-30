# 广告业务测试指南

> 本文档为测试人员提供广告模块的完整测试场景、验证方法和关键日志说明

---

## 目录

1. [测试环境准备](#一测试环境准备)
2. [广告平台与类型概览](#二广告平台与类型概览)
3. [平台级配置测试](#三平台级配置测试)
4. [广告类型配置测试](#四广告类型配置测试)
5. [竞价流程测试](#五竞价流程测试)
6. [频控功能测试](#六频控功能测试)
7. [各广告类型专项测试](#七各广告类型专项测试)
8. [异常场景测试](#八异常场景测试)
9. [配置优先级测试](#九配置优先级测试)
10. [日志速查表](#十日志速查表)
11. [测试检查清单](#十一测试检查清单)
12. [常见问题排查](#十二常见问题排查)

---

## 一、测试环境准备

### 1.1 配置切换方式

通过 **Firebase Remote Config** 修改 `biddingConfigJson` 字段来切换配置。

**Remote Config 字段名**: `biddingConfigJson`

**配置生效方式**:
1. 在 Firebase Console 修改并发布配置
2. 重启应用（或等待 Remote Config 自动刷新，默认 12 小时）
3. 强制刷新：清除应用数据后重启

### 1.2 日志过滤命令

```bash
# ==================== 基础过滤 ====================

# 过滤所有广告相关日志（主 TAG）
adb logcat -s AdModule

# 清空日志并实时查看
adb logcat -c && adb logcat -s AdModule

# ==================== 模块过滤 ====================

# 配置相关日志
adb logcat | grep -E "\[BiddingConfig\]|\[PlatformConfig\]"

# 频控相关日志
adb logcat | grep "\[PlatformFrequency\]"

# ==================== 预加载过滤 ====================

# 开屏预加载
adb logcat | grep -E "\[AppOpenPreload\]|\[SplashTwoLayerPreload\]"

# 插屏预加载
adb logcat | grep "\[InterstitialPreload\]"

# 原生广告预加载
adb logcat | grep "\[NativePreload\]"

# 全屏原生预加载
adb logcat | grep "\[FullNativePreload\]"

# Banner 预加载
adb logcat | grep "\[BannerPreload\]"

# 激励广告预加载
adb logcat | grep -E "\[RewardedPreload\]|\[RewardTwoLayerPreload\]"

# ==================== 竞价过滤 ====================

# 开屏竞价
adb logcat | grep -E "\[SplashBidding\]|\[SplashTwoLayerPreload\].*竞价"

# 激励广告竞价
adb logcat | grep "\[RewardBidding\]"

# 插屏智能竞价
adb logcat | grep "\[InterstitialSmartBidding\]"

# 原生广告智能竞价
adb logcat | grep "\[NativeSmartBidding\]"

# Banner 智能竞价
adb logcat | grep "\[BannerSmartBidding\]"

# ==================== 平台过滤 ====================

# AdMob 相关
adb logcat | grep -i "admob"

# Pangle 相关
adb logcat | grep -i "pangle"

# TopOn 相关
adb logcat | grep -i "topon"

# ==================== 组合过滤 ====================

# 所有跳过/拦截日志
adb logcat | grep -E "跳过|拦截|skip"

# 所有错误日志
adb logcat | grep -E "失败|error|Error|ERROR"

# 所有竞价结果
adb logcat | grep -E "胜出|winner|Winner"
```

### 1.3 关键日志 TAG 说明

| TAG | 模块 | 说明 |
|-----|------|------|
| `AdModule` | 全局 | 广告主日志 TAG |
| `BiddingConfig` | 配置 | 竞价配置加载与解析 |
| `PlatformConfig` | 配置 | 平台/广告类型启用判断 |
| `PlatformFrequency` | 频控 | 平台级频控检查 |
| `SplashTwoLayerPreload` | 预加载 | 开屏两层预加载编排 |
| `RewardTwoLayerPreload` | 预加载 | 激励两层预加载编排 |
| `AppOpenPreload` | 预加载 | 开屏广告预加载 |
| `InterstitialPreload` | 预加载 | 插屏广告预加载 |
| `NativePreload` | 预加载 | 原生广告预加载 |
| `FullNativePreload` | 预加载 | 全屏原生预加载 |
| `BannerPreload` | 预加载 | Banner 预加载 |
| `RewardedPreload` | 预加载 | 激励广告预加载 |
| `SplashBidding` | 竞价 | 开屏广告竞价 |
| `RewardBidding` | 竞价 | 激励广告竞价 |
| `InterstitialSmartBidding` | 竞价 | 插屏智能竞价 |
| `NativeSmartBidding` | 竞价 | 原生智能竞价 |
| `BannerSmartBidding` | 竞价 | Banner 智能竞价 |
| `PangleAppOpen` | Pangle | Pangle 开屏 |
| `PangleInterstitial` | Pangle | Pangle 插屏 |
| `PangleRewarded` | Pangle | Pangle 激励 |
| `TopOnSplash` | TopOn | TopOn 开屏 |
| `TopOnInterstitial` | TopOn | TopOn 插屏 |
| `TopOnRewarded` | TopOn | TopOn 激励 |

---

## 二、广告平台与类型概览

### 2.1 支持的广告平台

| 平台 | 配置 Key | 说明 |
|-----|----------|------|
| AdMob | `admob` | Google AdMob |
| Pangle | `pangle` | 穿山甲（字节跳动） |
| TopOn | `topon` | TopOn 聚合平台 |

### 2.2 支持的广告类型

| 广告类型 | 配置 Key | 说明 | 使用场景 |
|---------|----------|------|----------|
| 开屏广告 | `splash` | App Open Ad | 应用启动时展示 |
| 插屏广告 | `interstitial` | Interstitial Ad | 页面切换时展示 |
| 激励广告 | `rewarded` | Rewarded Ad | 用户主动触发获取奖励 |
| 插页激励广告 | `rewarded_interstitial` | Rewarded Interstitial | 结合插屏和激励 |
| 原生广告 | `native` | Native Ad | 融入内容流展示 |
| 全屏原生广告 | `full_native` | Full Screen Native | 全屏原生样式 |
| 横幅广告 | `banner` | Banner Ad | 页面底部/顶部展示 |

### 2.3 平台支持矩阵

| 广告类型 | AdMob | Pangle | TopOn |
|---------|-------|--------|-------|
| splash | ✅ | ✅ | ✅ |
| interstitial | ✅ | ✅ | ✅ |
| rewarded | ✅ | ✅ | ✅ |
| rewarded_interstitial | ✅ | ❌ | ❌ |
| native | ✅ | ✅ | ✅ |
| full_native | ✅ | ✅ | ✅ |
| banner | ✅ | ✅ | ✅ |

---

## 三、平台级配置测试

### 3.1 禁用单个平台

#### 测试场景 P1: 禁用 AdMob 平台

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 0 },
            "pangle": { "enabled": 1 },
            "topon": { "enabled": 1 }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] ADMOB 平台未启用，跳过预加载
```

**验证点**:
- [ ] AdMob 所有广告类型都不加载
- [ ] AdMob 不参与任何竞价
- [ ] Pangle 和 TopOn 正常加载和竞价

---

#### 测试场景 P2: 禁用 Pangle 平台

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1 },
            "pangle": { "enabled": 0 },
            "topon": { "enabled": 1 }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] PANGLE 平台未启用，跳过预加载
```

**验证点**:
- [ ] Pangle 所有广告类型都不加载
- [ ] Pangle 不参与任何竞价
- [ ] AdMob 和 TopOn 正常加载和竞价

---

#### 测试场景 P3: 禁用 TopOn 平台

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1 },
            "pangle": { "enabled": 1 },
            "topon": { "enabled": 0 }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] TOPON 平台未启用，跳过预加载
```

**验证点**:
- [ ] TopOn 所有广告类型都不加载
- [ ] TopOn 不参与任何竞价
- [ ] AdMob 和 Pangle 正常加载和竞价

---

### 3.2 仅启用单个平台

#### 测试场景 P4: 仅启用 AdMob

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1 },
            "pangle": { "enabled": 0 },
            "topon": { "enabled": 0 }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] PANGLE 平台未启用，跳过预加载
D/AdModule: [PlatformConfig] TOPON 平台未启用，跳过预加载
```

**验证点**:
- [ ] 仅 AdMob 广告加载
- [ ] 所有竞价结果均为 AdMob

---

#### 测试场景 P5: 仅启用 Pangle

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 0 },
            "pangle": { "enabled": 1 },
            "topon": { "enabled": 0 }
        }
    }
}
```

**验证点**:
- [ ] 仅 Pangle 广告加载
- [ ] 所有竞价结果均为 Pangle

---

#### 测试场景 P6: 仅启用 TopOn

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 0 },
            "pangle": { "enabled": 0 },
            "topon": { "enabled": 1 }
        }
    }
}
```

**验证点**:
- [ ] 仅 TopOn 广告加载
- [ ] 所有竞价结果均为 TopOn

---

### 3.3 禁用所有平台

#### 测试场景 P7: 禁用所有平台

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 0 },
            "pangle": { "enabled": 0 },
            "topon": { "enabled": 0 }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] ADMOB 平台未启用，跳过预加载
D/AdModule: [PlatformConfig] PANGLE 平台未启用，跳过预加载
D/AdModule: [PlatformConfig] TOPON 平台未启用，跳过预加载
W/AdModule: [SplashTwoLayerPreload] 没有可用的广告参与竞价
```

**验证点**:
- [ ] 无广告加载
- [ ] 竞价返回失败

---

## 四、广告类型配置测试

### 4.1 开屏广告 (splash) 配置测试

#### 测试场景 A1: 禁用所有平台的 splash

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1, "ad_types": { "splash": { "enabled": 0 } } },
            "pangle": { "enabled": 1, "ad_types": { "splash": { "enabled": 0 } } },
            "topon": { "enabled": 1, "ad_types": { "splash": { "enabled": 0 } } }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] ADMOB splash 广告类型未启用 (enabled=0)，跳过预加载
D/AdModule: [PlatformConfig] PANGLE splash 广告类型未启用 (enabled=0)，跳过预加载
D/AdModule: [PlatformConfig] TOPON splash 广告类型未启用 (enabled=0)，跳过预加载
```

**验证点**:
- [ ] 开屏广告不加载
- [ ] 插屏广告仍可正常加载（如已启用）

---

#### 测试场景 A2: splash 仅 AdMob 参与竞价

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1, "ad_types": { "splash": { "enabled": 1, "participate_bidding": 1 } } },
            "pangle": { "enabled": 1, "ad_types": { "splash": { "enabled": 1, "participate_bidding": 0 } } },
            "topon": { "enabled": 1, "ad_types": { "splash": { "enabled": 1, "participate_bidding": 0 } } }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] PANGLE splash 不参与竞价 (participate_bidding=0)，跳过竞价
D/AdModule: [PlatformConfig] TOPON splash 不参与竞价 (participate_bidding=0)，跳过竞价
```

**验证点**:
- [ ] 三个平台的 splash 都预加载
- [ ] 只有 AdMob splash 参与竞价
- [ ] Pangle/TopOn splash 可作为兜底

---

### 4.2 插屏广告 (interstitial) 配置测试

#### 测试场景 A3: 禁用 Pangle 的 interstitial

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "pangle": { "enabled": 1, "ad_types": { "interstitial": { "enabled": 0 } } }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] PANGLE interstitial 广告类型未启用 (enabled=0)，跳过预加载
```

**验证点**:
- [ ] Pangle 插屏不加载
- [ ] Pangle 其他广告类型正常
- [ ] AdMob/TopOn 插屏正常

---

### 4.3 激励广告 (rewarded) 配置测试

#### 测试场景 A4: 禁用所有平台的 rewarded

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1, "ad_types": { "rewarded": { "enabled": 0 } } },
            "pangle": { "enabled": 1, "ad_types": { "rewarded": { "enabled": 0 } } },
            "topon": { "enabled": 1, "ad_types": { "rewarded": { "enabled": 0 } } }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] ADMOB rewarded 广告类型未启用 (enabled=0)，跳过预加载
D/AdModule: [PlatformConfig] PANGLE rewarded 广告类型未启用 (enabled=0)，跳过预加载
D/AdModule: [PlatformConfig] TOPON rewarded 广告类型未启用 (enabled=0)，跳过预加载
```

**验证点**:
- [ ] 激励广告功能不可用
- [ ] 用户点击激励入口时无广告可展示

---

### 4.4 插页激励广告 (rewarded_interstitial) 配置测试

#### 测试场景 A5: 启用 rewarded_interstitial (仅 AdMob 支持)

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1, "ad_types": { "rewarded_interstitial": { "enabled": 1, "participate_bidding": 1 } } }
        }
    }
}
```

**验证点**:
- [ ] AdMob 插页激励广告正常加载
- [ ] Pangle/TopOn 不支持此类型，无相关日志

---

### 4.5 原生广告 (native) 配置测试

#### 测试场景 A6: 禁用 TopOn 的 native

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "topon": { "enabled": 1, "ad_types": { "native": { "enabled": 0 } } }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] TOPON native 广告类型未启用 (enabled=0)，跳过预加载
```

**验证点**:
- [ ] TopOn 原生广告不加载
- [ ] AdMob/Pangle 原生广告正常

---

### 4.6 全屏原生广告 (full_native) 配置测试

#### 测试场景 A7: full_native 仅 Pangle 参与竞价

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1, "ad_types": { "full_native": { "enabled": 1, "participate_bidding": 0 } } },
            "pangle": { "enabled": 1, "ad_types": { "full_native": { "enabled": 1, "participate_bidding": 1 } } },
            "topon": { "enabled": 1, "ad_types": { "full_native": { "enabled": 1, "participate_bidding": 0 } } }
        }
    }
}
```

**预期日志**:
```
D/AdModule: [PlatformConfig] ADMOB full_native 不参与竞价 (participate_bidding=0)，跳过竞价
D/AdModule: [PlatformConfig] TOPON full_native 不参与竞价 (participate_bidding=0)，跳过竞价
```

**验证点**:
- [ ] 三个平台的 full_native 都预加载
- [ ] 只有 Pangle 参与竞价

---

### 4.7 横幅广告 (banner) 配置测试

#### 测试场景 A8: 禁用所有 banner

**Remote Config 配置**:
```json
{
    "free_user": {
        "platforms": {
            "admob": { "enabled": 1, "ad_types": { "banner": { "enabled": 0 } } },
            "pangle": { "enabled": 1, "ad_types": { "banner": { "enabled": 0 } } },
            "topon": { "enabled": 1, "ad_types": { "banner": { "enabled": 0 } } }
        }
    }
}
```

**验证点**:
- [ ] 所有 Banner 广告不加载
- [ ] 页面底部/顶部无 Banner 展示

---

## 五、竞价流程测试

### 5.1 开屏两层竞价测试

#### 测试场景 B1: 正常两层竞价（开屏）

**触发条件**: 应用启动

**预期日志序列**:
```
D/AdModule: [SplashTwoLayerPreload] 开始两层预加载
D/AdModule: [AppOpenPreload] 开始预加载所有平台开屏广告
D/AdModule: [InterstitialPreload] 开始预加载所有平台插屏广告
D/AdModule: [SplashTwoLayerPreload] 两层预加载完成
D/AdModule: [SplashTwoLayerPreload] ============ 开始两层竞价 ============
D/AdModule: [AppOpenPreload] 开始开屏广告竞价
D/AdModule: [InterstitialPreload] 开始插屏广告竞价
D/AdModule: [SplashTwoLayerPreload] ============ 两层竞价结束 ============
D/AdModule: [SplashTwoLayerPreload] 最终胜出: ADMOB - SPLASH, eCPM: 0.005000 USD
```

**验证点**:
- [ ] 预加载阶段：三个平台的 splash 和 interstitial 都发起加载
- [ ] 竞价阶段：所有已加载的广告参与竞价
- [ ] 最终选出 eCPM 最高的广告
- [ ] 展示胜出的广告

---

#### 测试场景 B2: 开屏竞价 - Pangle 胜出

**测试条件**: Pangle 返回更高 eCPM

**预期日志**:
```
D/AdModule: [SplashTwoLayerPreload] 最终胜出: PANGLE - SPLASH, eCPM: 0.008000 USD
```

**验证点**:
- [ ] 展示 Pangle 开屏广告
- [ ] Pangle 广告样式正确

---

#### 测试场景 B3: 开屏竞价 - 插屏胜出

**测试条件**: 插屏广告 eCPM 高于开屏

**预期日志**:
```
D/AdModule: [SplashTwoLayerPreload] 最终胜出: ADMOB - INTERSTITIAL, eCPM: 0.010000 USD
```

**验证点**:
- [ ] 展示插屏广告而非开屏广告
- [ ] 插屏广告正常展示和关闭

---

### 5.2 激励两层竞价测试

#### 测试场景 B4: 正常两层竞价（激励）

**触发条件**: 用户点击激励入口

**预期日志序列**:
```
D/AdModule: [RewardTwoLayerPreload] 开始两层预加载
D/AdModule: [RewardedPreload] 开始预加载所有平台激励广告
D/AdModule: [RewardTwoLayerPreload] 两层预加载完成
D/AdModule: [RewardTwoLayerPreload] ============ 开始两层竞价 ============
D/AdModule: [RewardTwoLayerPreload] 最终胜出: ADMOB - REWARDED, eCPM: 0.015000 USD
```

**验证点**:
- [ ] 激励广告正常加载
- [ ] 竞价选出最优广告
- [ ] 用户完成观看后获得奖励

---

### 5.3 智能竞价测试

#### 测试场景 B5: 插屏智能竞价

**触发条件**: 页面切换时展示插屏

**预期日志**:
```
D/AdModule: [InterstitialSmartBidding] 开始智能竞价
D/AdModule: [InterstitialSmartBidding] 竞价结果: PANGLE, eCPM: 0.006000 USD
```

**验证点**:
- [ ] 插屏广告正常展示
- [ ] 关闭后返回原页面

---

#### 测试场景 B6: 原生广告智能竞价

**触发条件**: 内容流加载原生广告

**预期日志**:
```
D/AdModule: [NativeSmartBidding] 开始智能竞价
D/AdModule: [NativeSmartBidding] 竞价结果: ADMOB, eCPM: 0.003000 USD
```

**验证点**:
- [ ] 原生广告正确渲染
- [ ] 融入内容流样式

---

#### 测试场景 B7: Banner 智能竞价

**触发条件**: 页面加载 Banner

**预期日志**:
```
D/AdModule: [BannerSmartBidding] 开始智能竞价
D/AdModule: [BannerSmartBidding] 竞价结果: TOPON, eCPM: 0.001500 USD
```

**验证点**:
- [ ] Banner 正确展示在指定位置
- [ ] 尺寸适配正确

---

### 5.4 竞价配置开关测试

#### 测试场景 B8: 关闭竞价功能

**Remote Config 配置**:
```json
{
    "free_user": {
        "bidding_enabled": 0
    }
}
```

**预期行为**:
- [ ] 不执行多平台竞价
- [ ] 直接使用 fallback 平台广告

---

#### 测试场景 B9: 关闭两层竞价

**Remote Config 配置**:
```json
{
    "free_user": {
        "bidding_enabled": 1,
        "two_layer_bidding_enabled": 0
    }
}
```

**预期行为**:
- [ ] 仅执行单层竞价
- [ ] 开屏场景仅在 splash 类型中竞价，不与 interstitial 竞价

---

#### 测试场景 B10: 设置渠道全局超时时间

**Remote Config 配置**:
```json
{
    "free_user": {
        "bidding_timeout_seconds": 5
    }
}
```

**预期行为**:
- [ ] 所有场景竞价超过 5 秒后超时
- [ ] 使用已加载的广告或返回失败

**预期日志**:
```
D/AdModule: [场景] 超时时间: 5 秒
```

---

#### 测试场景 B10.1: 设置场景级超时时间 ⭐ 新增

**Remote Config 配置**:
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

**测试步骤**:
1. 应用启动触发开屏广告
2. 点击激励入口触发激励广告
3. 对比两个场景的超时日志

**预期行为**:
- [ ] 开屏场景：5 秒超时（快速响应）
- [ ] 激励场景：15 秒超时
- [ ] 插屏场景（未配置场景超时）：使用渠道全局 10 秒

**预期日志**:
```
# 开屏场景
D/AdModule: [SplashBidding] 超时时间: 5 秒

# 激励场景
D/AdModule: [RewardBidding] 超时时间: 15 秒

# 插屏场景（无场景超时配置，使用渠道全局）
D/AdModule: [InterstitialSmartBidding] 超时时间: 10 秒
```

**超时优先级验证**:
| 场景 | 场景超时 | 渠道全局 | 实际使用 |
|------|---------|----------|----------|
| splash | 5s | 10s | **5s** (场景优先) |
| reward | 15s | 10s | **15s** (场景优先) |
| interstitial | 未配置 | 10s | **10s** (渠道全局) |

---

#### 测试场景 B10.2: 场景超时覆盖渠道超时 ⭐ 新增

**Remote Config 配置**:
```json
{
    "free_user": {
        "bidding_timeout_seconds": 20,
        "scene_config": {
            "splash": {
                "timeout_seconds": 3
            }
        }
    }
}
```

**测试步骤**:
1. 重启应用
2. 观察开屏广告加载日志

**预期行为**:
- [ ] 开屏场景使用 3 秒超时（而非渠道的 20 秒）
- [ ] 3 秒后如未加载完成，使用兜底广告

**预期日志**:
```
D/AdModule: [SplashBidding] 超时时间: 3 秒
W/AdModule: [SplashBidding] 竞价超时，使用兜底广告
```

---

### 5.5 竞价超时与异常测试

#### 测试场景 B11: 竞价超时处理

**测试条件**: 网络较慢或广告加载超时

**预期日志**:
```
D/AdModule: [AppOpenPreload] 竞价超时，使用当前已加载的广告
W/AdModule: [SplashTwoLayerPreload] 没有可用的广告参与竞价
```

**验证点**:
- [ ] 超时后使用已加载的广告
- [ ] 若无可用广告，返回失败结果
- [ ] 应用不崩溃

---

#### 测试场景 B12: 所有平台加载失败

**测试条件**: 无网络或所有广告 ID 无效

**预期日志**:
```
E/AdModule: [AppOpenPreload] ADMOB splash 加载失败
E/AdModule: [AppOpenPreload] PANGLE splash 加载失败
E/AdModule: [AppOpenPreload] TOPON splash 加载失败
W/AdModule: [SplashTwoLayerPreload] 没有可用的广告参与竞价
```

**验证点**:
- [ ] 错误日志正确输出
- [ ] 竞价返回失败
- [ ] 应用正常运行，不展示广告

---

## 六、频控功能测试

### 6.1 频控全局开关测试

#### 测试场景 F1: 启用频控

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": true
}
```

**验证点**:
- [ ] 频控限制生效
- [ ] 达到限制后广告不参与竞价

---

#### 测试场景 F2: 关闭频控

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": false
}
```

**预期行为**:
- [ ] 无频控拦截日志
- [ ] 广告展示不受次数和间隔限制

---

### 6.2 展示次数限制测试

#### 测试场景 F3: AdMob splash 展示次数限制

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": true,
    "platform_frequency": {
        "admob": {
            "splash": {
                "max_daily_show": 3,
                "max_daily_click": 2,
                "min_show_interval_seconds": 60
            }
        }
    }
}
```

**预期日志（展示次数超限）**:
```
W/AdModule: [PlatformFrequency] 频控拦截 | 平台: ADMOB | 类型: SPLASH | 原因: 展示次数超限 (3/3)
```

**预期日志（点击次数超限）**:
```
W/AdModule: [PlatformFrequency] 频控拦截 | 平台: ADMOB | 类型: SPLASH | 原因: 点击次数超限 (2/2)
```

**预期日志（展示间隔不足）**:
```
W/AdModule: [PlatformFrequency] 频控拦截 | 平台: ADMOB | 类型: SPLASH | 原因: 展示间隔不足 (30s < 60s)
```

**验证点**:
- [ ] 展示 3 次后，AdMob splash 不再参与竞价
- [ ] 点击 2 次后，AdMob splash 不再参与竞价
- [ ] 两次展示间隔不足 60 秒时，AdMob splash 不参与竞价

---

#### 测试场景 F4: Pangle interstitial 展示次数限制

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": true,
    "platform_frequency": {
        "pangle": {
            "interstitial": {
                "max_daily_show": 5,
                "max_daily_click": 3,
                "min_show_interval_seconds": 120
            }
        }
    }
}
```

**预期日志（展示次数超限）**:
```
W/AdModule: [PlatformFrequency] 频控拦截 | 平台: PANGLE | 类型: INTERSTITIAL | 原因: 展示次数超限 (5/5)
```

**验证点**:
- [ ] 展示 5 次后，Pangle interstitial 不再参与竞价
- [ ] 其他平台 interstitial 不受影响

---

#### 测试场景 F5: TopOn rewarded 点击次数限制

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": true,
    "platform_frequency": {
        "topon": {
            "rewarded": {
                "max_daily_show": 20,
                "max_daily_click": 3,
                "min_show_interval_seconds": 30
            }
        }
    }
}
```

**预期日志（点击次数超限）**:
```
W/AdModule: [PlatformFrequency] 频控拦截 | 平台: TOPON | 类型: REWARDED | 原因: 点击次数超限 (3/3)
```

**验证点**:
- [ ] 点击 3 次后，TopOn rewarded 不再参与竞价

---

### 6.3 展示间隔限制测试

#### 测试场景 F6: banner 展示间隔限制

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": true,
    "platform_frequency": {
        "admob": {
            "banner": {
                "max_daily_show": 200,
                "max_daily_click": 30,
                "min_show_interval_seconds": 30
            }
        }
    }
}
```

**预期日志（展示间隔不足）**:
```
W/AdModule: [PlatformFrequency] 频控拦截 | 平台: ADMOB | 类型: BANNER | 原因: 展示间隔不足 (15s < 30s)
```

**验证点**:
- [ ] 两次 Banner 展示间隔不足 30 秒时被拦截

---

### 6.4 频控重置测试

#### 测试场景 F7: 每日 0 点频控重置

**测试步骤**:
1. 展示广告达到每日上限
2. 修改设备系统时间到次日 00:01
3. 重新启动应用

**预期行为**:
- [ ] 频控计数重置为 0
- [ ] 广告可正常展示
- [ ] 日志无频控拦截

---

### 6.5 多平台多类型频控组合测试

#### 测试场景 F8: 全平台全类型频控配置

**Remote Config 配置**:
```json
{
    "platform_frequency_enabled": true,
    "platform_frequency": {
        "admob": {
            "splash": { "max_daily_show": 50, "max_daily_click": 10, "min_show_interval_seconds": 30 },
            "interstitial": { "max_daily_show": 30, "max_daily_click": 8, "min_show_interval_seconds": 60 },
            "rewarded": { "max_daily_show": 20, "max_daily_click": 5, "min_show_interval_seconds": 30 },
            "native": { "max_daily_show": 100, "max_daily_click": 20, "min_show_interval_seconds": 10 },
            "banner": { "max_daily_show": 200, "max_daily_click": 30, "min_show_interval_seconds": 5 }
        },
        "pangle": {
            "splash": { "max_daily_show": 50, "max_daily_click": 10, "min_show_interval_seconds": 30 },
            "interstitial": { "max_daily_show": 30, "max_daily_click": 8, "min_show_interval_seconds": 60 },
            "rewarded": { "max_daily_show": 20, "max_daily_click": 5, "min_show_interval_seconds": 30 }
        },
        "topon": {
            "splash": { "max_daily_show": 50, "max_daily_click": 10, "min_show_interval_seconds": 30 },
            "interstitial": { "max_daily_show": 30, "max_daily_click": 8, "min_show_interval_seconds": 60 },
            "rewarded": { "max_daily_show": 20, "max_daily_click": 5, "min_show_interval_seconds": 30 }
        }
    }
}
```

**验证点**:
- [ ] 各平台各类型频控独立计数
- [ ] AdMob splash 达到上限不影响 Pangle splash
- [ ] splash 达到上限不影响 interstitial

---

## 七、各广告类型专项测试

### 7.1 开屏广告专项测试

#### 测试场景 S1: 冷启动开屏广告

**触发条件**: 应用完全关闭后重新启动

**验证点**:
- [ ] 开屏广告正常展示
- [ ] 广告展示时长符合预期
- [ ] 点击跳过按钮可关闭广告
- [ ] 广告关闭后进入主界面

---

#### 测试场景 S2: 热启动开屏广告

**触发条件**: 应用切换到后台超过一定时间后返回

**验证点**:
- [ ] 根据配置决定是否展示开屏广告
- [ ] 广告展示正常

---

### 7.2 插屏广告专项测试

#### 测试场景 S3: 页面切换插屏

**触发条件**: 特定页面切换时

**验证点**:
- [ ] 插屏广告正常展示
- [ ] 关闭广告后返回正确页面
- [ ] 不影响页面导航栈

---

### 7.3 激励广告专项测试

#### 测试场景 S4: 激励广告完整观看

**触发条件**: 用户点击激励入口

**验证点**:
- [ ] 广告加载并展示
- [ ] 完整观看后获得奖励
- [ ] 奖励数值正确

---

#### 测试场景 S5: 激励广告中途退出

**触发条件**: 用户中途关闭激励广告

**验证点**:
- [ ] 提示用户未完成观看
- [ ] 不发放奖励

---

### 7.4 原生广告专项测试

#### 测试场景 S6: 原生广告渲染

**触发条件**: 内容流加载

**验证点**:
- [ ] 原生广告样式正确
- [ ] 图片/视频正常加载
- [ ] 点击响应正确

---

### 7.5 Banner 广告专项测试

#### 测试场景 S7: Banner 展示与刷新

**触发条件**: 页面加载

**验证点**:
- [ ] Banner 在指定位置展示
- [ ] 尺寸适配正确
- [ ] 自动刷新正常（如配置）

---

## 八、异常场景测试

### 8.1 网络异常测试

#### 测试场景 E1: 网络断开

**测试条件**: 关闭设备网络

**预期日志**:
```
E/AdModule: [AppOpenPreload] AdMob 开屏广告加载失败: Network error
E/AdModule: [PangleAppOpen] 开屏广告加载失败: No network
E/AdModule: [TopOnSplash] 开屏广告加载失败: No network connection
```

**验证点**:
- [ ] 各平台错误日志正确输出
- [ ] 应用不崩溃，优雅降级
- [ ] 无广告展示

---

#### 测试场景 E2: 网络恢复后重新加载

**测试步骤**:
1. 断开网络，广告加载失败
2. 恢复网络
3. 触发预加载（如切换页面或返回前台）

**预期行为**:
- [ ] 网络恢复后可正常加载广告
- [ ] 下次展示机会广告正常

---

#### 测试场景 E3: 弱网环境

**测试条件**: 使用网络限速工具模拟弱网

**验证点**:
- [ ] 广告加载有超时机制
- [ ] 超时后使用已缓存的广告或跳过
- [ ] 不阻塞用户操作

---

### 8.2 SDK 初始化测试

#### 测试场景 E4: AdMob 初始化失败

**预期日志**:
```
E/AdModule: [AdsManager] AdMob 初始化失败: ...
```

**验证点**:
- [ ] 初始化失败有明确日志
- [ ] Pangle 和 TopOn 仍可正常初始化
- [ ] 应用正常运行

---

#### 测试场景 E5: Pangle 初始化失败

**预期日志**:
```
E/AdModule: [PangleManager] Pangle SDK 初始化失败: ...
```

**验证点**:
- [ ] 初始化失败有明确日志
- [ ] AdMob 和 TopOn 仍可正常初始化

---

#### 测试场景 E6: TopOn 初始化失败

**预期日志**:
```
E/AdModule: [TopOnManager] TopOn SDK 初始化失败: ...
```

**验证点**:
- [ ] 初始化失败有明确日志
- [ ] AdMob 和 Pangle 仍可正常初始化

---

### 8.3 广告加载失败回退测试

#### 测试场景 E7: 单平台加载失败

**测试条件**: 配置无效的 Pangle 广告 ID

**预期日志**:
```
E/AdModule: [AppOpenPreload] PANGLE splash 加载失败: Invalid ad unit ID
D/AdModule: [SplashTwoLayerPreload] 最终胜出: ADMOB - SPLASH
```

**验证点**:
- [ ] Pangle 失败不影响其他平台
- [ ] 使用其他平台的广告

---

#### 测试场景 E8: 多平台加载失败

**测试条件**: Pangle 和 TopOn 广告 ID 都无效

**预期日志**:
```
E/AdModule: [AppOpenPreload] PANGLE splash 加载失败
E/AdModule: [AppOpenPreload] TOPON splash 加载失败
D/AdModule: [SplashTwoLayerPreload] 最终胜出: ADMOB - SPLASH
```

**验证点**:
- [ ] AdMob 作为最后可用平台
- [ ] 竞价正常完成

---

#### 测试场景 E9: 所有平台加载失败

**测试条件**: 所有广告 ID 都无效或网络不可用

**预期日志**:
```
E/AdModule: [AppOpenPreload] ADMOB splash 加载失败
E/AdModule: [AppOpenPreload] PANGLE splash 加载失败
E/AdModule: [AppOpenPreload] TOPON splash 加载失败
W/AdModule: [SplashTwoLayerPreload] 没有可用的广告参与竞价
```

**验证点**:
- [ ] 竞价返回失败
- [ ] 应用正常运行，跳过广告展示

---

## 九、配置优先级测试

### 9.1 配置来源优先级

**优先级**: Remote Config > 本地缓存 > Assets 默认配置 > 硬编码默认值

#### 测试场景 C1: 使用远程配置

**测试条件**: Remote Config 有有效配置

**预期日志**:
```
D/AdModule: [BiddingConfig] 成功获取远程竞价配置，已缓存
```

---

#### 测试场景 C2: 使用缓存配置

**测试条件**: 无网络，但之前有缓存

**预期日志**:
```
D/AdModule: [BiddingConfig] 使用缓存的竞价配置
```

---

#### 测试场景 C3: 使用 Assets 配置

**测试条件**: 无网络，无缓存，但 assets 有配置文件

**预期日志**:
```
D/AdModule: [BiddingConfig] 使用 assets 默认竞价配置
```

---

#### 测试场景 C4: 使用硬编码默认配置

**测试条件**: 无网络，无缓存，assets 无配置

**预期日志**:
```
D/AdModule: [BiddingConfig] 使用硬编码默认竞价配置
```

---

### 9.2 渠道配置测试

#### 测试场景 C5: 免费用户配置

**测试条件**: 用户为免费用户

**验证点**:
- [ ] 使用 `free_user` 渠道配置
- [ ] 广告频率符合免费用户配置

---

#### 测试场景 C6: 付费用户配置

**测试条件**: 用户为付费用户

**验证点**:
- [ ] 使用 `premium_user` 渠道配置
- [ ] 广告频率符合付费用户配置（通常更少）

---

## 十、日志速查表

| 关键字 | 含义 | 级别 |
|-------|------|------|
| `平台未启用` | 平台 enabled=0，跳过加载 | DEBUG |
| `广告类型未启用` | 广告类型 enabled=0，跳过加载 | DEBUG |
| `不参与竞价` | participate_bidding=0，跳过竞价 | DEBUG |
| `无广告类型配置` | 配置缺失，默认跳过 | DEBUG |
| `无有效广告ID` | 广告 ID 未配置或无效 | DEBUG |
| `频控拦截` | 频控限制触发 | WARNING |
| `开始两层预加载` | 预加载流程开始 | DEBUG |
| `两层预加载完成` | 预加载流程结束 | DEBUG |
| `开始两层竞价` | 竞价流程开始 | DEBUG |
| `最终胜出` | 竞价结果输出 | DEBUG |
| `加载失败` | 广告加载失败 | ERROR |
| `初始化失败` | SDK 初始化失败 | ERROR |

---

## 十一、测试检查清单

### 平台配置检查
- [ ] AdMob enabled=0 时，AdMob 所有广告不加载
- [ ] Pangle enabled=0 时，Pangle 所有广告不加载
- [ ] TopOn enabled=0 时，TopOn 所有广告不加载
- [ ] 仅启用单个平台时，只有该平台广告加载

### 广告类型配置检查
- [ ] splash enabled=0 时，开屏广告不加载
- [ ] interstitial enabled=0 时，插屏广告不加载
- [ ] rewarded enabled=0 时，激励广告不加载
- [ ] rewarded_interstitial enabled=0 时，插页激励不加载
- [ ] native enabled=0 时，原生广告不加载
- [ ] full_native enabled=0 时，全屏原生不加载
- [ ] banner enabled=0 时，Banner 不加载

### 竞价配置检查
- [ ] participate_bidding=0 时，广告预加载但不参与竞价
- [ ] bidding_enabled=0 时，不执行多平台竞价
- [ ] two_layer_bidding_enabled=0 时，仅执行单层竞价
- [ ] bidding_timeout_seconds（渠道全局超时）设置生效
- [ ] scene_config.{scene}.timeout_seconds（场景超时）优先级高于渠道全局
- [ ] 开屏场景使用 splash 场景超时配置
- [ ] 激励场景使用 reward 场景超时配置

### 竞价流程检查
- [ ] 多平台广告正确参与竞价
- [ ] eCPM 最高的广告胜出
- [ ] 竞价超时有正确处理
- [ ] 兜底逻辑正确执行
- [ ] 各平台各广告类型竞价结果正确

### 频控检查
- [ ] platform_frequency_enabled=true 时，频控生效
- [ ] platform_frequency_enabled=false 时，无频控限制
- [ ] 展示次数超限后不再参与竞价
- [ ] 点击次数超限后不再参与竞价
- [ ] 展示间隔不足时不参与竞价
- [ ] 每日 0 点计数重置
- [ ] 各平台各类型频控独立

### 广告类型检查
- [ ] 开屏广告：冷启动、热启动展示正常
- [ ] 插屏广告：展示和关闭正常
- [ ] 激励广告：完整观看获得奖励
- [ ] 原生广告：样式渲染正确
- [ ] Banner 广告：位置和尺寸正确

### 异常场景检查
- [ ] 网络断开时有错误日志
- [ ] 网络恢复后可正常加载
- [ ] 弱网环境有超时处理
- [ ] SDK 初始化失败不影响其他模块
- [ ] 单平台加载失败不影响其他平台
- [ ] 所有平台失败时优雅降级

### 配置优先级检查
- [ ] Remote Config 优先于本地缓存
- [ ] 缓存优先于 Assets 配置
- [ ] Assets 优先于硬编码默认值

---

## 十二、常见问题排查

### Q1: 配置修改后不生效？

**检查步骤**:
1. 确认 Remote Config 已发布
2. 重启应用（或清除应用数据）
3. 查看日志确认配置来源：
   ```bash
   adb logcat | grep "\[BiddingConfig\]"
   ```

### Q2: 某平台广告不加载？

**检查步骤**:
1. 查看平台是否启用：
   ```bash
   adb logcat | grep "平台未启用"
   ```
2. 查看是否有有效广告 ID：
   ```bash
   adb logcat | grep "无有效广告ID"
   ```

### Q3: 某广告类型不加载？

**检查步骤**:
1. 查看广告类型是否启用：
   ```bash
   adb logcat | grep "广告类型未启用"
   ```
2. 查看配置是否存在：
   ```bash
   adb logcat | grep "无广告类型配置"
   ```

### Q4: 广告不参与竞价？

**检查步骤**:
1. 查看 participate_bidding 配置：
   ```bash
   adb logcat | grep "不参与竞价"
   ```
2. 查看频控是否拦截：
   ```bash
   adb logcat | grep "频控拦截"
   ```

### Q5: 竞价结果不符合预期？

**检查步骤**:
1. 查看完整竞价日志：
   ```bash
   adb logcat | grep -E "开始.*竞价|最终胜出|eCPM"
   ```
2. 确认各平台返回的 eCPM 值

### Q6: 频控不生效？

**检查步骤**:
1. 确认 platform_frequency_enabled=true：
   ```bash
   adb logcat | grep "platform_frequency_enabled"
   ```
2. 查看频控配置是否正确下发

### Q7: 广告加载失败？

**检查步骤**:
1. 查看具体失败原因：
   ```bash
   adb logcat | grep -E "加载失败|error|Error"
   ```
2. 检查网络连接
3. 确认广告 ID 有效

---

## 附录：测试用例编号索引

| 编号 | 场景 | 章节 |
|-----|------|------|
| P1-P7 | 平台级配置测试 | 三 |
| A1-A8 | 广告类型配置测试 | 四 |
| B1-B12 | 竞价流程测试 | 五 |
| F1-F8 | 频控功能测试 | 六 |
| S1-S7 | 广告类型专项测试 | 七 |
| E1-E9 | 异常场景测试 | 八 |
| C1-C6 | 配置优先级测试 | 九 |

---

*文档版本: 2.0*
*更新时间: 2026-01-20*
*测试用例总数: 49 个*
