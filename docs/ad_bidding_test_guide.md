# 广告多平台聚合竞价测试指南

> **版本**: 1.0
> **日期**: 2026-01-12
> **适用对象**: 测试人员、开发人员

## 1. 测试目标
验证多平台（AdMob, Pangle, TopOn）聚合竞价逻辑的正确性，确保：
1.  各平台广告 SDK 初始化正常。
2.  竞价逻辑能正确比较 eCPM 并选择价格最高的广告。
3.  收益上报（Revenue Reporting）数据准确。
4.  广告展示流程（展示、点击、关闭、奖励）回调正常。

## 2. 环境准备
*   **测试设备**: Android 真机（建议安装 Google Play 服务）。
*   **网络环境**: 能够访问 Google, Pangle, TopOn 广告服务的网络（可能需要 VPN）。
*   **工具**: Android Studio (Logcat) 或 `adb logcat` 命令行工具。

## 3. 日志过滤配置
为了清晰地查看竞价流程，请在 Logcat 中使用以下过滤关键字：

```
package:mine tag:PlatformConfig|Bidding|AdMob|Pangle|TopOn|Revenue
```

或者使用命令行：
```bash
adb logcat -v time | grep -E "PlatformConfig|Bidding|AdMob|Pangle|TopOn|Revenue"
```

## 4. 测试场景与用例

### 场景一：SDK 初始化验证
**目标**: 确保应用启动时，所有配置的广告 SDK 均已初始化。

1.  **操作**: 冷启动应用。
2.  **期望日志**:
    *   `BiddingInitializer`: 开始初始化多平台竞价系统
    *   `PangleManager`: ✅ Pangle SDK 初始化成功
    *   `TopOnManager`: ✅ TopOn SDK 初始化成功
    *   `BiddingConfigManager`: 配置数据已更新 (或加载默认配置)

### 场景二：单平台兜底测试 (默认配置)
**目标**: 验证在默认配置下（通常仅开启 AdMob），应用能正常展示广告。

1.  **前置**: 确保本地 `assets/bidding_config_default.json` 默认配置为 AdMob 开启，其他关闭（或不配置）。
2.  **操作**: 进入触发广告的页面（如点击"查看激励视频"）。
3.  **期望日志**:
    *   `BiddingPlatformController`: 判断各平台启用状态。
    *   `RewardBiddingManager`: (可能跳过竞价，直接使用 AdMob) 或者显示 AdMob 胜出。
    *   `RewardedAds`: 激励广告展示成功。

### 场景三：多平台竞价验证
**目标**: 验证当多个平台都有广告填充时，系统能选择 eCPM 最高者。

1.  **前置**:
    *   需要通过 Firebase Remote Config 下发开启 Pangle/TopOn 的配置。
    *   或者临时修改代码/本地配置开启全平台。
2.  **操作**: 触发广告加载。
3.  **期望日志**:
    *   `BiddingInitializer`: 多平台 SDK 初始化请求已发出。
    *   `RewardedBiddingManager`: ============ 开始激励广告竞价 ============
    *   `RewardedBiddingManager`: [Pangle] 获取竞价结果: Success, eCPM: x.xx USD
    *   `RewardedBiddingManager`: [TopOn] 获取竞价结果: Success, eCPM: y.yy USD
    *   `RewardedBiddingManager`: [AdMob] 获取竞价结果: Success, eCPM: z.zz USD
    *   `RewardedBiddingManager`: 最终胜出: [平台名] - eCPM: [最高价]
    *   展示广告日志显示胜出平台的广告被展示。

### 场景四：eCPM 转换验证
**目标**: 验证不同平台的 eCPM 单位是否被正确转换为美元。

*   **Pangle**: 此时假设 SDK 返回单位为 **分 (Cent)**，系统除以 100。
    *   *验证点*: 日志中 Pangle eCPM 应符合预期范围（例如 SDK 返回 1500，日志显示 15.0 USD）。
*   **TopOn**: 此时假设 SDK 返回单位为 **微美元 (Micro)** ?? (需确认)，系统除以 1,000,000。
    *   *注意*: 请特别留意 TopOn 的 eCPM 数值量级，如果日志显示数值极小（如 0.000015）或极大，说明单位转换可能有误，需记录并反馈。

### 场景五：回调与收益上报
**目标**: 验证广告展示后的关键事件上报。

1.  **操作**: 观看完整广告并点击关闭。
2.  **期望日志**:
    *   `onAdShowed`: 广告展示回调。
    *   `onRewardEarned`: (激励广告) 获得奖励回调。
    *   `RevenueAdManager`: 上报收益数据 (AdRevenueNetwork: [平台名], Value: [金额])。
    *   `onAdDismissed`: 广告关闭回调。

## 5. 常见问题排查

*   **Pangle/TopOn 初始化失败**:
    *   检查 `AdIdHelper` 是否配置了对应平台的 App ID。
    *   检查网络是否能连接到对应广告服务器。
*   **eCPM 为 0**:
    *   可能是测试广告位未配置底价，或平台未返回有效价格信息。
    *   AdMob 测试广告通常 eCPM 较低或为 0，建议在代码中 mock 一个固定值用于测试竞价排序逻辑。
*   **一直只有 AdMob**:
    *   检查 `BiddingConfigManager` 是否加载了正确的配置，确认 Pangle/TopOn 开关是否打开 (`enabled: 1`).

## 6. 测试模式功能 (Debug 专用)

### Mock eCPM 注入
当使用测试广告 ID 时，SDK 通常返回 eCPM = 0，导致无法验证竞价逻辑。

**解决方案**: Debug 版本自动注入 Mock eCPM 值：

| 平台   | Mock eCPM (USD) |
| ------ | --------------- |
| AdMob  | 0.015           |
| Pangle | 0.012           |
| TopOn  | 0.018           |

**期望日志**:
```
[PlatformConfig] [TestMode] 注入 Mock eCPM: ADMOB = 0.015000 USD (真实值: 0.000000)
```

### 自定义 Mock 值
可通过代码动态设置 Mock 值测试不同场景:
```kotlin
// 使 Pangle 胜出
BiddingPlatformController.setMockEcpm(BiddingWinner.PANGLE, 0.050)
```

---

## 7. 反馈模板
如果发现问题，请按以下格式提交 Bug：
*   **问题描述**: (例如：TopOn 广告胜出但无法展示)
*   **复现步骤**:
*   **关键日志片段**: (粘贴 Logcat 内容)
*   **设备信息**:
