# 广告频控支持情况分析报告

> **版本**：v1.0  
> **日期**：2026-01-16  
> **范围**：monetize 广告模块（展示/竞价链路）

## 1. 结论摘要

- **应用级频控已落地**：按广告类型限制「每日展示次数 / 每日点击次数 / 展示间隔」，并支持全局广告开关；展示前通过拦截器链统一校验，数据按天复位并持久化。
- **平台级频控仅完成“配置 + 统计/日志”**：已有 `platform_frequency_enabled` 与平台配置结构、以及 `PlatformFrequencyManager` 的统计能力，但当前未接入竞价过滤/展示拦截链路。
- **覆盖不均衡**：开屏/插屏/原生/Banner/全屏原生已接入拦截器链；激励与插页激励未接入；激励竞价场景下插屏路径跳过冷却时间与展示计数更新。

## 2. 应用级频控支持

### 2.1 配置与持久化

- 配置模型：`daily_display_cap`（每日展示上限）、`daily_interaction_cap`（每日点击上限）、`cooldown_seconds`（展示冷却时间）。
- 配置来源：`monetize/src/main/assets/ad_config.json`，并可被 Remote Config 参数 `adConfigJson` 覆盖；区分 free/premium 渠道。
- 运行时：`AdConfig` 使用 `SpUtils` 存储每日计数与上次展示时间，按日期自动复位。

### 2.2 拦截器链

拦截器链顺序：
1. **GlobalAdSwitchInterceptor**：全局广告开关（本地 DataStore）。
2. **ShowCountLimitInterceptor**：每日展示上限。
3. **ShowIntervalLimitInterceptor**：展示间隔（支持系统时间异常的兜底重置）。
4. **ClickLimitInterceptor**：每日点击上限。

### 2.3 已接入的广告类型

- **已接入**：开屏（AppOpen）、插屏（Interstitial）、原生（Native）、Banner、全屏原生（FullNative）。
- **未接入**：激励（Rewarded）、插页激励（RewardedInterstitial）。
- **特殊路径**：`InterstitialAds.displayAdForRewardBidding` 走简化拦截器链（无冷却时间），且不更新展示计数。

### 2.4 配额映射说明

- Banner 与 FullNative 当前复用 `embed_ad`（原生）的配额配置；若需要独立配额，需要扩展配置模型。
- 原生广告有单独的“自动刷新间隔”配置（非展示频控，但会影响曝光节奏）。

## 3. 平台级频控支持

### 3.1 配置结构

- 竞价配置中包含：`platform_frequency_enabled` 与 `platform_frequency`（平台 → 广告类型 → 配额）。
- 默认通过 `biddingConfigJson`（Remote Config）读取，assets `bidding_config_default.json` 为兜底。

### 3.2 统计能力

`PlatformFrequencyManager` 提供以下能力：
- 平台级每日展示/点击计数。
- 最小展示间隔判断。
- 过期 Key 清理（按 7 天）。
- 汇总状态输出（调试面板）。

### 3.3 当前落地情况

- 代码中未看到 `PlatformFrequencyManager.initialize` 的调用点。
- `canParticipate / recordShow / recordClick` 未接入竞价选择或展示回调，仅在日志/调试面板使用。
- 竞价日志中仅展示每日展示计数，未体现点击上限或间隔。

## 4. 配置来源与渠道策略

| 类型 | 入口 | 默认来源 | 远程覆盖 | 渠道区分 |
| --- | --- | --- | --- | --- |
| 应用级频控 | `AdConfigManager` | `ad_config.json` | `adConfigJson` | free/premium |
| 平台级频控 | `BiddingConfigManager` | `bidding_config_default.json` | `biddingConfigJson` | free/premium |

## 5. 缺口与风险

1. **平台级频控未接入竞价/展示链路**：目前仅统计/日志，不会影响广告选择与展示。
2. **激励/插页激励未做应用级频控**：可能造成频次过高或与运营策略不一致。
3. **激励竞价插屏路径绕过冷却**：displayAdForRewardBidding 只做展示/点击上限校验，且不更新展示计数。
4. **Banner/全屏原生缺少独立配额**：复用原生配额可能与运营预期不一致。

## 6. 建议（优先级）

### P0（若计划启用平台级频控）
- 在模块初始化中调用 `PlatformFrequencyManager.initialize`。
- 在竞价参与者筛选中调用 `canParticipate`，并在展示/点击回调中调用 `recordShow/recordClick`。
- 在日志中补充点击上限与展示间隔信息，方便排查。

### P1（应用级覆盖完善）
- 为 `Rewarded` / `RewardedInterstitial` 增加专用配置与拦截器链；或明确“激励免频控”的业务约束。

### P2（配置结构优化）
- 若运营需要区分 Banner/FullNative 配额，扩展 `ad_config.json` 与 `AdConfigData` 结构，避免复用原生配额造成误配。

---

如需我补充“平台级频控接入方案”的具体改动清单，请直接告诉我目标策略（是否要启用、哪些广告类型生效、是否区分渠道）。
