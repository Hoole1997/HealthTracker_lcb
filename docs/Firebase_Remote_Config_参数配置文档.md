# Firebase Remote Config 在线参数配置文档

本文档列出了 HealthTracker 应用中使用的所有 Firebase Remote Config 在线参数，供测试人员和运营人员配置生产环境使用。

---

## 目录

1. [adConfigJson - 广告配置](#1-adconfigjson---广告配置)
2. [adfill_target_fpu - 广告填充目标配置](#2-adfill_target_fpu---广告填充目标配置)
3. [AD_Count2 - 广告展示次数配置](#3-ad_count2---广告展示次数配置)
4. [adtarget_total_revenue - 广告收益目标配置](#4-adtarget_total_revenue---广告收益目标配置)
5. [rev_adj - Adjust收益上报配置](#5-rev_adj---adjust收益上报配置)
6. [rev_fir - Firebase收益上报配置](#6-rev_fir---firebase收益上报配置)
7. [Grouping - 用户分组配置](#7-grouping---用户分组配置)
8. [pushConfigJson - 推送策略配置](#8-pushconfigjson---推送策略配置)
9. [push_array - 推送消息内容配置](#9-push_array---推送消息内容配置)
10. [push_close_action - 推送关闭按钮配置](#10-push_close_action---推送关闭按钮配置)

---

## 1. adConfigJson - 广告配置

**参数名**: `adConfigJson`

**类型**: String (JSON格式)

**说明**: 控制应用中所有广告类型的展示频次、冷却时间及各种广告策略开关。配置分为 `free_user`（自然量用户）和 `premium_user`（付费渠道用户）两个渠道。

### JSON 结构说明

| 字段路径 | 类型 | 说明 | 默认值 |
|---------|------|------|--------|
| `free_user` / `premium_user` | Object | 用户渠道配置，分别对应自然量和付费渠道用户 | - |
| `└─ launch_ad` | Object | 开屏广告配置 | - |
| `  └─ daily_display_cap` | Int | 每日最大展示次数 | 10 |
| `  └─ daily_interaction_cap` | Int | 每日最大点击次数 | 3 |
| `  └─ cooldown_seconds` | Int | 展示冷却时间（秒） | 60 |
| `└─ fullpage_ad` | Object | 插屏广告配置 | - |
| `  └─ daily_display_cap` | Int | 每日最大展示次数 | 10 |
| `  └─ daily_interaction_cap` | Int | 每日最大点击次数 | 3 |
| `  └─ cooldown_seconds` | Int | 展示冷却时间（秒） | 120 |
| `└─ embed_ad` | Object | 原生广告配置 | - |
| `  └─ daily_display_cap` | Int | 每日最大展示次数 | 10 |
| `  └─ daily_interaction_cap` | Int | 每日最大点击次数 | 3 |
| `  └─ cooldown_seconds` | Int | 展示冷却时间（秒） | 30 |
| `└─ immersive_ad_after_fullpage` | Int | 插屏广告关闭后展示全屏原生广告的次数（0=不展示） | 0 |
| `└─ fallback_fullpage_on_launch_fail` | Int | 开屏广告加载失败后是否展示插屏广告（1=是，0=否） | 0 |
| `└─ enable_bottom_ad_on_locale` | Int | 语言选择页面是否展示底部原生广告（1=是，0=否） | 0 |
| `└─ splash_time_out` | Int | 开屏页超时时间（秒） | 10 |
| `└─ Guide_Full_Native` | Int | 引导页结束后是否展示全屏原生广告（1=是，0=否） | 1 |
| `└─ Guide_Page` | Int | 是否展示新手引导页（1=是，0=否） | 0 |
| `└─ auto_play_reward` | Int | 是否自动播放激励广告（1=是，0=否） | 1 |
| `└─ reward_bidding_time_out` | Int | 激励广告竞价超时时间（秒） | 5 |
| `└─ reward_bidding_enabled` | Int | 是否启用激励广告竞价（1=是，0=否） | 1 |
| `└─ splash_bidding_enabled` | Int | 是否启用开屏广告竞价（1=是，0=否） | 1 |
| `└─ long_leave_app` | Int | 长时间离开应用的判定时间（秒），用于返回时展示插屏 | 20 |
| `└─ NA_Uninstall1` | Int | 卸载挽留页1是否展示原生广告（1=是，0=否） | 1 |
| `└─ IV_Uninstall1` | Int | 卸载挽留页1是否展示插屏广告（1=是，0=否） | 0 |
| `└─ NA_Uninstall2` | Int | 卸载挽留页2是否展示原生广告（1=是，0=否） | 1 |
| `└─ IV_Uninstall2` | Int | 卸载挽留页2是否展示插屏广告（1=是，0=否） | 0 |
| `└─ native_ad_refresh_interval` | Int | 原生广告自动刷新间隔（秒），最小值10秒 | 30 |

### 完整JSON示例

```json
{
  "free_user": {
    "launch_ad": {
      "daily_display_cap": 10,
      "daily_interaction_cap": 3,
      "cooldown_seconds": 60
    },
    "fullpage_ad": {
      "daily_display_cap": 10,
      "daily_interaction_cap": 3,
      "cooldown_seconds": 120
    },
    "embed_ad": {
      "daily_display_cap": 10,
      "daily_interaction_cap": 3,
      "cooldown_seconds": 30
    },
    "immersive_ad_after_fullpage": 0,
    "fallback_fullpage_on_launch_fail": 0,
    "enable_bottom_ad_on_locale": 0,
    "splash_time_out": 10,
    "Guide_Full_Native": 1,
    "Guide_Page": 0,
    "auto_play_reward": 1,
    "reward_bidding_time_out": 5,
    "reward_bidding_enabled": 1,
    "splash_bidding_enabled": 1,
    "long_leave_app": 20,
    "NA_Uninstall1": 1,
    "IV_Uninstall1": 0,
    "NA_Uninstall2": 1,
    "IV_Uninstall2": 0,
    "native_ad_refresh_interval": 30
  },
  "premium_user": {
    "launch_ad": {
      "daily_display_cap": 20,
      "daily_interaction_cap": 10,
      "cooldown_seconds": 0
    },
    "fullpage_ad": {
      "daily_display_cap": 20,
      "daily_interaction_cap": 10,
      "cooldown_seconds": 30
    },
    "embed_ad": {
      "daily_display_cap": 20,
      "daily_interaction_cap": 10,
      "cooldown_seconds": 0
    },
    "immersive_ad_after_fullpage": 3,
    "fallback_fullpage_on_launch_fail": 1,
    "enable_bottom_ad_on_locale": 1,
    "splash_time_out": 12,
    "Guide_Full_Native": 1,
    "Guide_Page": 0,
    "auto_play_reward": 1,
    "reward_bidding_time_out": 5,
    "reward_bidding_enabled": 1,
    "splash_bidding_enabled": 1,
    "long_leave_app": 20,
    "NA_Uninstall1": 1,
    "IV_Uninstall1": 0,
    "NA_Uninstall2": 1,
    "IV_Uninstall2": 0,
    "native_ad_refresh_interval": 30
  }
}
```

---

## 2. adfill_target_fpu - 广告填充目标配置

**参数名**: `adfill_target_fpu`

**类型**: String (JSON数组格式)

**说明**: 控制广告填充次数（Fill Per User）达到阈值时触发的事件上报。可配置多个目标事件。

### JSON 结构说明

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| `name` | String | 上报的事件名称 | 是 |
| `enabled` | Boolean | 是否启用该配置 | 是 |
| `fpu` | Int | 填充次数阈值，达到后触发上报（0=不限制，每次填充都上报） | 是 |
| `reset_after_trigger` | Boolean | 触发后是否重置累积计数 | 是 |
| `ad_types` | Array[String] | 适用的广告类型列表 | 否 |

### 广告类型说明

| 类型代码 | 说明 |
|---------|------|
| `SP` | 开屏广告 (Splash) |
| `IV` | 插屏广告 (Interstitial) |
| `NA` | 原生广告 (Native) |
| `RV` | 激励视频广告 (Rewarded Video) |
| `BA` | 横幅广告 (Banner) |

### JSON示例

```json
[
  {
    "name": "ad_fill_5",
    "enabled": true,
    "fpu": 5,
    "reset_after_trigger": false,
    "ad_types": ["SP", "IV", "NA", "RV", "BA"]
  },
  {
    "name": "ad_fill_10",
    "enabled": true,
    "fpu": 10,
    "reset_after_trigger": true,
    "ad_types": ["SP", "IV"]
  }
]
```

---

## 3. AD_Count2 - 广告展示次数配置

**参数名**: `AD_Count2`

**类型**: String (JSON数组格式)

**说明**: 控制广告展示次数（Impression Per User）和累积eCPM达到阈值时触发的事件上报。

### JSON 结构说明

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| `name` | String | 上报的事件名称 | 是 |
| `enabled` | Boolean | 是否启用该配置 | 是 |
| `ecpm` | Long | 累积eCPM阈值（微美元，需达到此值才触发） | 是 |
| `ipu` | Int | 展示次数阈值（0=不限制） | 是 |
| `reset_after_trigger` | Boolean | 触发后是否重置累积计数和eCPM | 是 |
| `ad_types` | Array[String] | 适用的广告类型列表（支持 FullNa=全屏原生） | 否 |

### 广告类型说明

| 类型代码 | 说明 |
|---------|------|
| `SP` | 开屏广告 |
| `IV` | 插屏广告 |
| `NA` | 原生广告 |
| `RV` | 激励视频广告 |
| `BA` | 横幅广告 |
| `FullNa` | 全屏原生广告 |

### JSON示例

```json
[
  {
    "name": "ad_imp_3",
    "enabled": true,
    "ecpm": 0,
    "ipu": 3,
    "reset_after_trigger": false,
    "ad_types": ["SP", "RV"]
  },
  {
    "name": "high_value_user",
    "enabled": true,
    "ecpm": 100000,
    "ipu": 5,
    "reset_after_trigger": true,
    "ad_types": ["SP", "IV", "NA", "RV", "BA", "FullNa"]
  }
]
```

---

## 4. adtarget_total_revenue - 广告收益目标配置

**参数名**: `adtarget_total_revenue`

**类型**: String (JSON数组格式)

**说明**: 控制累积广告收益（Revenue Per User）达到阈值时触发的事件上报。

### JSON 结构说明

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| `name` | String | 上报的事件名称 | 是 |
| `enabled` | Boolean | 是否启用该配置 | 是 |
| `revenue` | Long | 累积收益阈值（微美元） | 是 |
| `reset_after_trigger` | Boolean | 触发后是否重置累积收益 | 是 |
| `ad_types` | Array[String] | 适用的广告类型列表 | 否 |

### JSON示例

```json
[
  {
    "name": "revenue_1_dollar",
    "enabled": true,
    "revenue": 1000000,
    "reset_after_trigger": false,
    "ad_types": ["SP", "IV", "NA", "RV", "BA"]
  },
  {
    "name": "revenue_5_dollar",
    "enabled": true,
    "revenue": 5000000,
    "reset_after_trigger": true,
    "ad_types": ["SP", "IV", "NA", "RV", "BA"]
  }
]
```

---

## 5. rev_adj - Adjust收益上报配置

**参数名**: `rev_adj`

**类型**: String (JSON数组格式)

**说明**: 控制向 Adjust 平台上报广告收益时使用的来源名称及其概率分布。用于收益归因混淆。

### JSON 结构说明

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| `name` | String | Adjust 广告收益来源名称 | 是 |
| `rate` | Int | 选中概率百分比（0-100），所有配置的 rate 之和应为 100 | 是 |

### JSON示例

```json
[
  {
    "name": "applovin_max_sdk",
    "rate": 70
  },
  {
    "name": "ironsource_sdk",
    "rate": 30
  }
]
```

---

## 6. rev_fir - Firebase收益上报配置

**参数名**: `rev_fir`

**类型**: String (JSON数组格式)

**说明**: 控制向 Firebase Analytics 上报广告收益时使用的事件名称及其概率分布。

### JSON 结构说明

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| `name` | String | Firebase Analytics 事件名称 | 是 |
| `rate` | Int | 选中概率百分比（0-100），所有配置的 rate 之和应为 100 | 是 |

### JSON示例

```json
[
  {
    "name": "ad_impression",
    "rate": 80
  },
  {
    "name": "ad_other",
    "rate": 20
  }
]
```

---

## 7. Grouping - 用户分组配置

**参数名**: `Grouping`

**类型**: String

**说明**: 用于 A/B 测试的用户分组标识。配置后，应用启动时会上报 `Grouping_{value}` 事件（每个分组值只上报一次）。

### 使用说明

- 配置值为空或不配置时，不进行分组上报
- 每个用户对于同一分组值只会上报一次
- 上报事件名格式：`Grouping_{配置值}`

### 示例值

```
A
```

或

```
test_group_1
```

---

## 8. pushConfigJson - 推送策略配置

**参数名**: `pushConfigJson`

**类型**: String (JSON格式)

**说明**: 控制应用推送通知的策略配置，包括推送频率、时间限制、免打扰时段等。配置分为 `paid_channel`（付费渠道）和 `organic_channel`（自然量渠道）。

### JSON 结构说明

| 字段路径 | 类型 | 说明 | 默认值 |
|---------|------|------|--------|
| `paid_channel` / `organic_channel` | Object | 渠道推送配置 | - |
| `└─ total_push_count` | Int | 每日推送总次数限制 | paid: 999, organic: 3 |
| `└─ unlock_push_interval` | Int | 解锁推送间隔（分钟） | 10 |
| `└─ background_push_interval` | Int | 后台推送间隔（分钟） | 10 |
| `└─ hover_duration_strategy_switch` | Int | 悬浮时长策略开关（0=关闭，1=开启） | paid: 1, organic: 0 |
| `└─ hover_duration_loop_count` | Int | 悬浮时长循环次数 | paid: 9, organic: 0 |
| `└─ new_user_cooldown` | Int | 新用户冷却期（分钟） | paid: 0, organic: 24 |
| `└─ do_not_disturb_start` | String | 免打扰开始时间（格式: "HH:mm"） | "02:00" |
| `└─ do_not_disturb_end` | String | 免打扰结束时间（格式: "HH:mm"） | paid: "07:00", organic: "08:00" |
| `└─ notification_enabled` | Int | 通知开关（0=关闭，1=开启） | 1 |
| `└─ keepalive_polling_interval_minutes` | Int | 保活轮询间隔（分钟） | 15 |

### 完整JSON示例

```json
{
  "paid_channel": {
    "total_push_count": 999,
    "unlock_push_interval": 10,
    "background_push_interval": 10,
    "hover_duration_strategy_switch": 1,
    "hover_duration_loop_count": 9,
    "new_user_cooldown": 0,
    "do_not_disturb_start": "02:00",
    "do_not_disturb_end": "07:00",
    "notification_enabled": 1,
    "keepalive_polling_interval_minutes": 15
  },
  "organic_channel": {
    "total_push_count": 3,
    "unlock_push_interval": 10,
    "background_push_interval": 10,
    "hover_duration_strategy_switch": 0,
    "hover_duration_loop_count": 0,
    "new_user_cooldown": 24,
    "do_not_disturb_start": "02:00",
    "do_not_disturb_end": "08:00",
    "notification_enabled": 1,
    "keepalive_polling_interval_minutes": 15
  }
}
```

---

## 9. push_array - 推送消息内容配置

**参数名**: `push_array`

**类型**: String (JSON数组格式)

**说明**: 配置推送消息的内容模板，支持多语言。每条消息包含标题、内容、按钮文字以及图标和跳转类型。

### JSON 结构说明

| 字段 | 类型 | 说明 | 必填 |
|------|------|------|------|
| `id` | String | 消息唯一标识（如 "push_001"） | 是 |
| `iconType` | Int | 图标类型（1-12，见下表） | 是 |
| `actionType` | Int | 点击行为类型（1-12，见下表） | 是 |
| `localizations` | Object | 多语言内容映射表 | 是 |
| `└─ {语言代码}` | Object | 语言内容（如 "en", "ja", "ko", "es" 等） | 至少一个 |
| `  └─ title` | String | 推送标题 | 是 |
| `  └─ content` | String | 推送内容描述 | 是 |
| `  └─ buttonText` | String | 按钮文字 | 是* |

> *注：`iconType=12`（助手来电）时 buttonText 可为空

### 图标类型 (iconType) 说明

| 值 | 说明 |
|----|------|
| 1 | 通用健康图标 |
| 2 | 血糖图标 |
| 3 | 血压图标 |
| 4 | 胆固醇图标 |
| 5 | BMI/体重图标 |
| 6 | 心率图标 |
| 7 | 健康报告图标 |
| 8 | 用药提醒图标 |
| 9 | 饮水提醒图标 |
| 10 | 计步图标 |
| 11 | 天气通知图标（特殊：内容由业务动态填充） |
| 12 | 助手来电图标（特殊：buttonText 可为空） |

### 点击行为类型 (actionType) 说明

| 值 | 跳转页面 |
|----|---------|
| 1 | 主页 |
| 2 | 血糖记录页 |
| 3 | 血压记录页 |
| 4 | 胆固醇记录页 |
| 5 | BMI记录页 |
| 6 | 心率记录页 |
| 7 | 健康报告页 |
| 8 | 用药提醒页 |
| 9 | 饮水记录页 |
| 10 | 计步页 |
| 11 | 天气页 |
| 12 | 助手来电页 |

### 完整JSON示例

```json
[
  {
    "id": "push_001",
    "iconType": 1,
    "actionType": 1,
    "localizations": {
      "en": {
        "title": "💖 Your Daily Health Check-in!",
        "content": "Tracking your blood sugar, blood pressure, and weight is the first step to wellness. Have you measured today?",
        "buttonText": "RECOVER NOW"
      },
      "ja": {
        "title": "💖 デイリー健康チェック！",
        "content": "血糖値、血圧、体重の記録は健康への第一歩です。今日は測定しましたか？",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "💖 데일리 건강 체크인!",
        "content": "혈당, 혈압, 체중 추적은 건강 관리의 첫걸음입니다. 오늘 측정하셨나요?",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_002",
    "iconType": 1,
    "actionType": 1,
    "localizations": {
      "en": {
        "title": "📝 Time to Log Your Health Data!",
        "content": "Take a minute to record your blood sugar/pressure. Tracking trends is the first step to better health.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "📝 健康データを記録しましょう！",
        "content": "血糖値や血圧を記録する時間です。傾向を追跡することは健康改善への第一歩です。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "📝 건강 데이터 기록할 시간!",
        "content": "혈당/혈압을 기록할 시간입니다. 추적을 통해 더 나은 건강 관리의 첫걸음을 내딛으세요.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_003",
    "iconType": 2,
    "actionType": 2,
    "localizations": {
      "en": {
        "title": "🩸 Time to Check Your Blood Sugar!",
        "content": "Log your levels for the day and guard your health at every step.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "🩸 血糖値をチェックする時間です！",
        "content": "今日の数値を記録して、健康を守りましょう。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "🩸 혈당 체크할 시간!",
        "content": "오늘의 수치를 기록하고 건강을 지키세요.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_004",
    "iconType": 3,
    "actionType": 3,
    "localizations": {
      "en": {
        "title": "💓 Time for Your Blood Pressure Check!",
        "content": "Take a minute to log your BP and keep your heart health in check.",
        "buttonText": "Measure Now"
      },
      "ja": {
        "title": "💓 血圧チェックの時間です！",
        "content": "血圧を記録して、心臓の健康を維持しましょう。",
        "buttonText": "今すぐ測定"
      },
      "ko": {
        "title": "💓 혈압 체크할 시간!",
        "content": "혈압을 기록하고 심장 건강을 관리하세요.",
        "buttonText": "지금 측정하기"
      }
    }
  },
  {
    "id": "push_005",
    "iconType": 2,
    "actionType": 2,
    "localizations": {
      "en": {
        "title": "🩸 Is Your Sugar in Check?",
        "content": "Your body is talking. One quick tap to log your glucose and stay in the green zone.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "🩸 血糖値は正常ですか？",
        "content": "体がサインを送っています。グルコースを記録して正常範囲を維持しましょう。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "🩸 혈당이 정상인가요?",
        "content": "몸이 신호를 보내고 있습니다. 혈당을 기록하고 정상 범위를 유지하세요.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_006",
    "iconType": 4,
    "actionType": 4,
    "localizations": {
      "en": {
        "title": "🧪 Don't Forget Your Cholesterol!",
        "content": "Regularly knowing your levels is key to cardiovascular health.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "🧪 コレステロールを忘れずに！",
        "content": "定期的に数値を把握することが心血管の健康への鍵です。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "🧪 콜레스테롱 잊지 마세요!",
        "content": "정기적으로 수치를 확인하는 것이 심혈관 건강의 핵심입니다.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_007",
    "iconType": 4,
    "actionType": 4,
    "localizations": {
      "en": {
        "title": "❤️ One Minute for a Healthier Heart?",
        "content": "Shed light on your cholesterol and build a healthier future, now.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "❤️ 1分で健康な心臓に？",
        "content": "コレステロールを把握して、より健康な未来を築きましょう。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "❤️ 1분이 더 건강한 심장을 만듭니다?",
        "content": "콜레스테롱을 확인하고 더 건강한 미래를 만들어 보세요.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_008",
    "iconType": 5,
    "actionType": 5,
    "localizations": {
      "en": {
        "title": "⚖️ It's Weekly Weigh-In Time!",
        "content": "Step on the scale, log your BMI, and manage your long-term health.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "⚖️ 週一度の体重測定の時間です！",
        "content": "体重計に乗り、BMIを記録して、長期的な健康を管理しましょう。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "⚖️ 주간 체중 측정 시간!",
        "content": "체중계에 올라서고 BMI를 기록하여 장기적인 건강을 관리하세요.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_009",
    "iconType": 6,
    "actionType": 6,
    "localizations": {
      "en": {
        "title": "❤️ Heart Rate Record",
        "content": "Log your resting heart rate to understand your heart's healthy rhythm.",
        "buttonText": "Log Now"
      },
      "ja": {
        "title": "❤️ 心拍数記録",
        "content": "安静時心拍数を記録して、心臓の健康なリズムを理解しましょう。",
        "buttonText": "今すぐ記録"
      },
      "ko": {
        "title": "❤️ 심박수 기록",
        "content": "안정 시 심박수를 기록하여 심장의 건강한 리듬을 이해하세요.",
        "buttonText": "지금 기록하기"
      }
    }
  },
  {
    "id": "push_010",
    "iconType": 7,
    "actionType": 7,
    "localizations": {
      "en": {
        "title": "📊 Your Health Report is Updated!",
        "content": "Blood sugar, blood pressure, heart rate... All your key metrics at a glance. Check out your health trends.",
        "buttonText": "View Full Report"
      },
      "ja": {
        "title": "📊 健康レポートが更新されました！",
        "content": "血糖値、血圧、心拍数...主要な指標が一目でわかります。健康の傾向を確認しましょう。",
        "buttonText": "レポートを見る"
      },
      "ko": {
        "title": "📊 건강 리포트가 업데이트되었습니다!",
        "content": "혈당, 혈압, 심박수... 모든 주요 지표를 한눈에 확인하세요. 건강 추이를 확인해보세요.",
        "buttonText": "전체 리포트 보기"
      }
    }
  },
  {
    "id": "push_015",
    "iconType": 9,
    "actionType": 9,
    "localizations": {
      "en": {
        "title": "💧 Need a quick energy boost?",
        "content": "Dehydration can cause fatigue. A glass of water can help!",
        "buttonText": "Drink Now"
      },
      "ja": {
        "title": "💧 すぐにエネルギー補給が必要ですか？",
        "content": "脱水症状は疲労の原因になります。コップ一杯の水が助けになります！",
        "buttonText": "今すぐ飲む"
      },
      "ko": {
        "title": "💧 빠른 에너지 충전이 필요하신가요?",
        "content": "탈수는 피로를 유발할 수 있습니다. 물 한 잔이 도움이 됩니다!",
        "buttonText": "지금 마시기"
      }
    }
  },
  {
    "id": "push_016",
    "iconType": 9,
    "actionType": 9,
    "localizations": {
      "en": {
        "title": "💧 Quick Mission: Hydrate in 60 sec!",
        "content": "Tap to log your water and keep your streak alive.",
        "buttonText": "Drink Now"
      },
      "ja": {
        "title": "💧 クイックミッション：60秒で水分補給！",
        "content": "タップして水分を記録し、継続記録を維持しましょう。",
        "buttonText": "今すぐ飲む"
      },
      "ko": {
        "title": "💧 퀵 미션: 60초 내로 수분 보충!",
        "content": "탭하여 물 섭취를 기록하고 연속 기록을 유지하세요.",
        "buttonText": "지금 마시기"
      }
    }
  },
  {
    "id": "push_017",
    "iconType": 10,
    "actionType": 10,
    "localizations": {
      "en": {
        "title": "👟 Guess what your steps built this week!",
        "content": "You won't believe the progress you've made. Tap to see your report!",
        "buttonText": "View Full Report"
      },
      "ja": {
        "title": "👟 今週の歩数で何が達成できたかご存知ですか？",
        "content": "あなたが成し遂げた進歩に驚くでしょう。レポートを確認してみましょう！",
        "buttonText": "レポートを見る"
      },
      "ko": {
        "title": "👟 이번 주 걸음수로 무엇을 이뤘는지 아시나요?",
        "content": "여러분이 이룬 진전을 믿기 어려우실 거예요. 리포트를 확인해보세요!",
        "buttonText": "전체 리포트 보기"
      }
    }
  },
  {
    "id": "push_021",
    "iconType": 11,
    "actionType": 11,
    "localizations": {
      "en": {
        "title": "",
        "content": "",
        "buttonText": ""
      },
      "ja": {
        "title": "",
        "content": "",
        "buttonText": ""
      },
      "ko": {
        "title": "",
        "content": "",
        "buttonText": ""
      }
    }
  },
  {
    "id": "push_022",
    "iconType": 12,
    "actionType": 12,
    "localizations": {
      "en": {
        "title": "Your Health Assistant",
        "content": "I'm here to remind you to add your [type] record!",
        "buttonText": ""
      },
      "ja": {
        "title": "健康サポートのお知らせ",
        "content": "[type]の記録を追加する時間です！",
        "buttonText": ""
      },
      "ko": {
        "title": "건강 기록 알림",
        "content": "[type] 기록을 추가할 시간이에요!",
        "buttonText": ""
      }
    }
  }
]
```

> **注意**: `push_021` (天气通知) 的 title、content、buttonText 均为空字符串，实际内容由应用业务逻辑根据天气数据动态填充。

### 多语言支持说明

- 可在配置中添加任意语言代码（如 "en", "ja", "ko", "es", "fr", "de" 等）
- 无需修改应用代码即可支持新语言
- 应用会自动根据用户语言设置选择对应内容
- 如果目标语言不存在，自动降级到英语（en）
- 如果英语也不存在，使用配置中第一个可用的语言

---

## 10. push_close_action - 推送关闭按钮配置

**参数名**: `push_close_action`

**类型**: String

**说明**: 控制推送通知是否显示关闭按钮。

### 值说明

| 值 | 说明 |
|----|------|
| `"0"` | 隐藏关闭按钮（用户无法手动关闭推送通知） |
| `"1"` 或其他 | 显示关闭按钮（默认行为） |

### 示例值

```
1
```

---

## 配置注意事项

1. **JSON格式校验**: 配置前请使用 JSON 校验工具确保格式正确
2. **渠道区分**: `adConfigJson` 中的 `free_user` 和 `premium_user` 分别对应自然量和付费渠道用户；`pushConfigJson` 中的 `paid_channel` 和 `organic_channel` 同理
3. **阈值单位**: 收益相关的阈值通常使用微美元（1美元 = 1,000,000微美元）
4. **概率配置**: `rev_adj` 和 `rev_fir` 中的 rate 值之和应为 100
5. **配置生效**: 配置更新后，用户下次启动应用时会拉取新配置
6. **推送多语言**: `push_array` 支持动态添加新语言，无需更新应用代码

---

## 更新日志

| 日期 | 版本 | 更新内容 |
|------|------|---------|
| 2026-01-05 | v1.1 | 新增推送相关配置：pushConfigJson、push_array、push_close_action |
| 2026-01-05 | v1.0 | 初始版本，包含所有在线参数说明 |
