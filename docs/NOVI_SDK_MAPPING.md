# Novi Launcher SDK 接入信息

> 本文档由 `novi-sdk-tool` 自动生成。AI 修改代码时应优先读取同目录的 `novi-sdk.lock.json`。

## 构建信息

- 包名：`com.healthlab.heartrate.bloodpressuretracker`
- SDK 分支：`v2.0.3.1`
- SDK 标识：`release`
- Jenkins Build：#1832
- 正式依赖：`com.launcher.unity:com.healthlab.heartrate.bloodpressuretracker-release:1.0.3`
- AAR SHA-256：`964376f643e623644cf894a8313747a2cc253382b1a8c2f7e2107cd3948b7bd6`
- 映射源码：`app/src/official/java/com/daily/health/manager/App.kt` (`official`)

## 类映射

| 原类 | 正式 SDK 类 |
|---|---|
| `org.oksp.launcher.App` | `com.healthlab.heartrate.bloodpressuretracker.Petgwi00m7` |

## 核心方法映射

| SDK 方法 | 正式 SDK 方法 | AAR 字节码验证 |
|---|---|---|
| `appShowAd` | `cleansmartmemory` | 通过 |
| `setNetworkEventListener` | `cleansmartmemory` | 通过 |
| `openMainActivity` | `safescanmedia` | 通过 |
| `getLauncherActivityClass` | `ultraprocalc` | 通过 |
| `getAppActivityClassArray` | `quickboostnet` | 通过 |

## AI 修改约束

1. 只修改配置指定渠道中的 Launcher Application。
2. `appShowAd` 必须保留 `(Activity, String, Int)` 参数。
3. `setNetworkEventListener` 与 `appShowAd` 可能映射为同名重载，不得合并调用。
4. 修改后必须通过 Local 标识的正式源码编译验证。
5. 编译验证后必须恢复 Google/Official 配置。
