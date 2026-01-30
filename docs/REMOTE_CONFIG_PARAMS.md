# 在线参数 (Remote Config) 详尽说明文档

本文档为 HealthTracker 项目中所有 Firebase Remote Config 在线参数的完整技术手册，包含字段定义、数据结构及业务逻辑说明。

---

## 1. 广告全局配置 (`adConfigJson`)

本参数控制应用内所有广告的展示、频率和策略。

- **键名**: `adConfigJson`
- **类型**: String (JSON)
- **结构**:
  - `free_user` / `premium_user`: (对象) 区分自然量与付费用户的配置。
    - `launch_ad` / `fullpage_ad` / `embed_ad`: (对象) 分别对应开屏、插页、内嵌原生广告。
      - `daily_display_cap`: (Int) 每日最大展示上限。
      - `daily_interaction_cap`: (Int) 每日最大点击上限。
      - `cooldown_seconds`: (Int) 两次展示之间的最小冷却时间（秒）。
    - `immersive_ad_after_fullpage`: (Int) 插页广告结束后展示的全屏原生广告数量。
    - `fallback_fullpage_on_launch_fail`: (Int) 开屏加载失败时是否回退到插页广告 (0/1)。
    - `enable_bottom_ad_on_locale`: (Int) 语言选择页底部是否显示原生广告 (0/1)。
    - `splash_time_out`: (Int) 开屏广告最大等待超时（秒）。
    - `long_leave_app` / `long_leave_time`: (Int) 用户离开应用多久后，重新进入时触发开屏（秒）。
    - `Guide_Full_Native`: (Int) 引导页是否展示全屏原生广告 (0/1)。
    - `Guide_Page`: (Int) 是否启用新手引导流程 (0/1)。
    - `auto_play_reward`: (Int) 是否自动播放激励视频 (0/1)。
    - `splash_bidding_enabled`: (Int) 是否启用开屏竞价模式 (0/1)。
    - `reward_bidding_enabled`: (Int) 是否启用激励视频竞价 (0/1)。
    - `reward_bidding_time_out`: (Int) 激励竞价超时时长（秒）。
    - `NA_Uninstall1` / `NA_Uninstall2`: (Int) 卸载挽留页1/2是否展示原生广告 (0/1)。
    - `IV_Uninstall1` / `IV_Uninstall2`: (Int) 卸载挽留页1/2是否展示插页广告 (0/1)。
    - `native_ad_refresh_interval`: (Int) 原生广告自动刷新间隔（秒），默认 30s。

---

## 2. 推送策略配置 (`pushConfigJson`)

控制应用的远程推送和保活推送策略。

- **键名**: `pushConfigJson`
- **类型**: String (JSON)
- **结构**:
  - `paid_channel` / `organic_channel`: (对象) 渠道差异化策略。
    - `total_push_count`: (Int) 每日每用户推送总次数限制。
    - `unlock_push_interval`: (Int) 解锁屏幕触发推送的最小间隔（分钟）。
    - `background_push_interval`: (Int) 应用进入后台触发推送的最小间隔（分钟）。
    - `hover_duration_strategy_switch`: (Int) 悬浮时长策略开关 (0/1)。
    - `hover_duration_loop_count`: (Int) 悬浮时长循环次数。
    - `new_user_cooldown`: (Int) 新用户注册后的冷却期（分钟，期间不上报/不推送）。
    - `do_not_disturb_start`: (String) 免打扰开始时间 (如 "02:00")。
    - `do_not_disturb_end`: (String) 免打扰结束时间 (如 "08:00")。
    - `notification_enabled`: (Int) 全局通知开关 (0/1)。
    - `keepalive_polling_interval_minutes`: (Int) 系统保活轮询间隔（分钟）。

---

## 3. 推送内容池 (`push_array`)

支持多语言动态下发的推送文案库。

- **键名**: `push_array`
- **类型**: String (JSON Array)
- **结构**: 数组项包含：
  - `id`: (String) 消息唯一 ID。
  - `iconType`: (Int) 图标类型索引 (1-12)。
  - `actionType`: (Int) 点击跳转行为索引 (1-12)。
  - `localizations`: (Map) 键为语言代码 (如 "en", "ja", "ko")。
    - `title`: (String) 推送标题。
    - `content`: (String) 推送描述。
    - `buttonText`: (String) 按钮文字。

---

## 4. 用户分组 (`Grouping`)

- **键名**: `Grouping`
- **类型**: String
- **用途**: 设置用户所在的 A/B 测试分组。上报时会作为 `Grouping_{value}` 格式的事件名。

---

## 5. 收益与展示上报阈值

这些参数用于控制当用户累积达到特定条件时，触发高价值事件上报。

### 收益上报 (`adtarget_total_revenue`)
- **JSON 字段**: `name` (事件名), `enabled`, `revenue` (累积美分阈值), `ad_types` (适用列表), `reset_after_trigger` (是否重置计数)。

### 展示次数上报 (`AD_Count2`)
- **JSON 字段**: `name`, `enabled`, `ecpm` (累积 eCPM 阈值), `ipu` (展示次数阈值), `ad_types`, `reset_after_trigger`。

### 填充次数上报 (`adfill_target_fpu`)
- **JSON 字段**: `name`, `enabled`, `fpu` (填充次数阈值), `ad_types`, `reset_after_trigger`。

### Adjust/Firebase 上报权重 (`rev_adj`, `rev_fir`)
- **说明**: 控制不同 SDK 上报收益的分配比例。
- **结构**: `[{"name": "...", "rate": 70}, ...]` (rate 总和应为 100)。

---

## 6. 其他独立开关与参数

- **`isRewardBiddingEnabled`**: (Boolean) 全局激励竞价开关。
- **`splashBiddingEnabled`**: (Boolean) 全局开屏竞价开关。
- **`adConfigJsonFromRemote`**: (String) 内部使用的远程配置备份键。
