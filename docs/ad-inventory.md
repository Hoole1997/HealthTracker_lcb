# 广告位清单

更新时间：2026-09-14

本清单基于当前 `lcb4` 工作区静态分析，分为两类：

- **业务广告位（position）**：从当前可见业务页面能够走到广告请求的场景标识，共 62 个；本清单不统计开屏页面。
- **平台广告单元（ad unit ID）**：AdMob、GAM、Pangle、TopOn 的实际请求 ID，由渠道配置注入 `BuildConfig`。

业务广告位与平台广告单元不是一一对应关系。页面先指定业务 position 和广告类型，SDK 再按竞价配置从可用平台及相应类型的 ad unit ID 中选择。

## 数量概览

| 类型前缀 | 类型 | 可触发数 | 触发范围 |
| --- | --- | ---: | --- |
| `IV` | 插屏 | 32 | 页面进入、保存完成或卡片点击 |
| `RV` | 激励 | 5 | 详情页专家建议区域使用 |
| `NA` | 原生 | 24 | 页面、列表和弹窗容器 |
| `BA` | Banner | 1 | 首页主容器使用 |
| **合计** |  | **62** |  |

“可达”表示项目代码存在运行路径，不表示每次进入都一定成功展示；SDK 初始化、频控、竞价、网络、用户状态和广告库存仍会影响最终结果。

## 插屏广告

### 新记录、功能页进入

`BaseInterActivity.onCreate()` 会立即调用 `showInter(getBackAdPosition())`。因此下表中虽然 position 名称包含 `back`，当前真实触发时机是**进入 Activity**，`handleBackPress()` 本身只记录事件并结束页面。

| 常量 | position 值 | 实际页面/触发 | 状态 |
| --- | --- | --- | --- |
| `IV_BLOOD_SUGAR_BACK` | `IV_BloodSugar_back` | 血糖新记录页进入；也是无法识别页面类型时的默认值 | 使用中 |
| `IV_BLOOD_PRESSURE_BACK` | `IV_BloodPressure_back` | 血压新记录页进入 | 使用中 |
| `IV_CHOLESTEROL_BACK` | `IV_Cholesterol_back` | 胆固醇新记录页进入 | 使用中 |
| `IV_HEART_RATE_BACK` | `IV_HeartRate_back` | 心率新记录页进入 | 使用中 |
| `IV_BMI_BACK` | `IV_BMI_back` | BMI 新记录页进入 | 使用中 |
| `IV_WATER_BACK` | `IV_Water_back` | 饮水记录页进入 | 使用中 |
| `IV_WALK_BACK` | `IV_Walk_back` | 步数页进入 | 使用中 |
| `IV_STEP_SETTING_BACK` | `IV_Step_Setting_back` | 步数目标设置页进入 | 使用中 |
| `IV_ADD_MEDS_BACK` | `IV_AddMeds_back` | 添加/编辑药物页进入 | 使用中 |
| `IV_PROFILE_BACK` | `IV_Profile_back` | 个人资料页进入，包括引导模式 | 使用中 |
| `IV_INSIGHTS_DETAILS_BACK` | `IV_InsightsDetails_back` | NEWS 详情页进入 | 使用中 |

需要注意：`HydrateSettingAct` 和 `HydrateCompleteScreen` 也继承 `BaseInterActivity`，但未覆盖广告位；两者进入时会使用默认的 `IV_BloodSugar_back`。`HydrateCompleteScreen` 同时还加载底部原生广告。

### 保存后插屏

| 常量 | position 值 | 触发位置 | 状态 |
| --- | --- | --- | --- |
| `IV_BLOOD_SUGAR_SAVE` | `IV_BloodSugar_Save` | 血糖记录保存成功弹窗完成后 | 使用中 |
| `IV_BLOOD_PRESSURE_SAVE` | `IV_BloodPressure_Save` | 血压记录保存成功弹窗完成后 | 使用中 |
| `IV_CHOLESTEROL_SAVE` | `IV_Cholesterol_Save` | 胆固醇记录保存成功弹窗完成后 | 使用中 |
| `IV_HEART_RATE_SAVE` | `IV_HeartRate_Save` | 心率记录保存成功弹窗完成后 | 使用中 |
| `IV_BMI_SAVE` | `IV_BMI_Save` | BMI 记录保存成功弹窗完成后 | 使用中 |
| `IV_STEP_GOAL_SAVE` | `IV_Step_Goal_Save` | 保存步数目标后 | 使用中 |
| `IV_ADD_MEDS_SAVE` | `IV_AddMeds_Save` | 药物保存成功后，展示完成再关闭页面 | 使用中 |

### 记录/报表页插屏

`*_TRACK_ENTER` 在主页“记录”Tab 中，仅当对应卡片已有图表数据时触发，然后进入统计页。没有数据时直接进入新记录页，由新记录页自己的 `*_BACK` position 处理。

| 常量 | position 值 | 触发位置 | 状态 |
| --- | --- | --- | --- |
| `IV_BLOOD_SUGAR_TRACK_ENTER` | `IV_BloodSugarTrack_Enter` | 有数据时点击血糖卡片 | 使用中 |
| `IV_BLOOD_PRESSURE_TRACK_ENTER` | `IV_BloodPressureTrack_Enter` | 有数据时点击血压卡片 | 使用中 |
| `IV_CHOLESTEROL_TRACK_ENTER` | `IV_CholesterolTrack_Enter` | 有数据时点击胆固醇卡片 | 使用中 |
| `IV_HEART_RATE_TRACK_ENTER` | `IV_HeartRateTrack_Enter` | 有数据时点击心率卡片 | 使用中 |
| `IV_BMI_TRACK_ENTER` | `IV_BMITrack_Enter` | 有数据时点击 BMI 卡片 | 使用中 |
| `IV_WATER_TRACK_ENTER` | `IV_WaterTrack_Enter` | 有数据时点击饮水卡片 | 使用中 |
| `IV_WALK_TRACK_ENTER` | `IV_WalkTrack_Enter` | 有数据时点击步数卡片 | 使用中 |

`*_TRACK_BACK` 同样由 `BaseInterActivity.onCreate()` 调用，实际在统计页和健康详情页进入时请求，并非返回时请求。

| 常量 | position 值 | 实际页面/触发 | 状态 |
| --- | --- | --- | --- |
| `IV_BLOOD_SUGAR_TRACK_BACK` | `IV_BloodSugarTrack_back` | 血糖统计页、血糖详情页进入 | 使用中 |
| `IV_BLOOD_PRESSURE_TRACK_BACK` | `IV_BloodPressureTrack_back` | 血压统计页、血压详情页进入 | 使用中 |
| `IV_CHOLESTEROL_TRACK_BACK` | `IV_CholesterolTrack_back` | 胆固醇统计页、胆固醇详情页进入 | 使用中 |
| `IV_HEART_RATE_TRACK_BACK` | `IV_HeartRateTrack_back` | 心率统计页、心率详情页进入 | 使用中 |
| `IV_BMI_TRACK_BACK` | `IV_BMITrack_back` | BMI 统计页、BMI 详情页进入 | 使用中 |
| `IV_WATER_TRACK_BACK` | `IV_WaterTrack_back` | 饮水统计页进入 | 使用中 |
| `IV_WALK_TRACK_BACK` | `IV_WalkTrack_back` | 步数统计页进入 | 使用中 |

## 激励广告

5 个广告位都由健康详情页的 `ExpertAdviceView` 触发：用户点击获取建议，或 2 秒倒计时结束且 `autoPlayReward() = true` 时调用激励广告；完成后解除建议内容遮罩。

| 常量 | position 值 | 健康类型 | 状态 |
| --- | --- | --- | --- |
| `RV_BLOOD_SUGAR_NOTE` | `RV_BloodSugar_Note` | 血糖详情 | 使用中；也是未知类型的默认值 |
| `RV_BLOOD_PRESSURE_NOTE` | `RV_BloodPressure_Note` | 血压详情 | 使用中 |
| `RV_CHOLESTEROL_NOTE` | `RV_Cholesterol_Note` | 胆固醇详情 | 使用中 |
| `RV_HEART_RATE_NOTE` | `RV_HeartRate_Note` | 心率详情 | 使用中 |
| `RV_BMI_NOTE` | `RV_BMI_Note` | BMI 详情 | 使用中 |

## 原生广告

| 常量 | position 值 | 页面/容器 | 样式 | 状态 |
| --- | --- | --- | --- | --- |
| `NA_HOME_EXIT_DIALOG` | `NA_Home_exit_dialog` | 首页退出确认弹窗 | `CARD_3` | 弹窗出现时加载 |
| `NA_MAIN_TRACKER_MIDDLE` | `NA_Main_Tracker_middle` | 记录 Tab 中部 | `CARD_8` | Fragment 恢复且容器可见后首次加载 |
| `NA_HYDRATE_COMPLETE_BOTTOM` | `NA_Hydrate_Complete_bottom` | 饮水完成页底部 | `CARD_7` | 页面进入时加载 |
| `NA_MEDS_REMINDER_LIST` | `NA_Meds_reminder_list` | 药物提醒列表第 2 项 | `CARD_8` | 有至少 2 项且切换到药物 Tab 后加载 |
| `NA_ALARM_MANAGER_BOTTOM` | `NA_Alarm_Manager_bottom` | 闹钟管理页底部 | `CARD_7` | 页面进入时加载 |
| `NA_ALARM_CONFIG_DIALOG` | `NA_Alarm_Config_Dialog` | 闹钟编辑 Compose 弹窗底部 | `CARD_7` | 弹窗组合时加载 |
| `NA_INSIGHTS_DETAIL_BOTTOM` | `NA_Insights_detail_bottom` | NEWS 详情底部 | `STANDARD` | 页面进入时加载 |
| `NA_SETTINGS_PROFILE_BOTTOM` | `NA_Settings_profile_bottom` | 个人资料页底部 | `CARD_7` | 页面进入时加载 |
| `NA_SETTINGS_LANGUAGE_BOTTOM` | `NA_Settings_language_bottom` | 语言选择页底部 | `CARD_7` | 当前开关为 `true` |
| `NA_NEW_RECORD_BLOOD_SUGAR_BOTTOM` | `NA_NewRecord_BloodSugar_bottom` | 血糖新记录页底部 | `STANDARD` | 使用中 |
| `NA_NEW_RECORD_BLOOD_PRESSURE_BOTTOM` | `NA_NewRecord_BloodPressure_bottom` | 血压新记录页底部 | `STANDARD` | 使用中 |
| `NA_NEW_RECORD_CHOLESTEROL_BOTTOM` | `NA_NewRecord_Cholesterol_bottom` | 胆固醇新记录页底部 | `STANDARD` | 使用中 |
| `NA_NEW_RECORD_HEART_RATE_BOTTOM` | `NA_NewRecord_HeartRate_bottom` | 心率新记录页底部 | `STANDARD` | 使用中 |
| `NA_HEART_RATE_MEASURE_BOTTOM` | `NA_HeartRateMeasure_bottom` | 心率测量页底部 | `STANDARD` | 使用中 |
| `NA_NEW_RECORD_BMI_BOTTOM` | `NA_NewRecord_BMI_bottom` | BMI 新记录页底部 | `STANDARD` | 使用中 |
| `NA_DETAIL_BLOOD_SUGAR_BOTTOM` | `NA_Detail_BloodSugar_bottom` | 血糖详情页底部 | `CARD_5` | 使用中 |
| `NA_DETAIL_BLOOD_PRESSURE_BOTTOM` | `NA_Detail_BloodPressure_bottom` | 血压详情页底部 | `CARD_5` | 使用中 |
| `NA_DETAIL_CHOLESTEROL_BOTTOM` | `NA_Detail_Cholesterol_bottom` | 胆固醇详情页底部 | `CARD_5` | 使用中 |
| `NA_DETAIL_HEART_RATE_BOTTOM` | `NA_Detail_HeartRate_bottom` | 心率详情页底部 | `CARD_5` | 使用中 |
| `NA_DETAIL_BMI_BOTTOM` | `NA_Detail_BMI_bottom` | BMI 详情页底部 | `CARD_5` | 使用中 |
| `NA_DETAIL_CONFIRM_DIALOG` | `NA_Detail_confirm_dialog` | 详情删除等确认弹窗 | `CARD_5` | 仅 `ConfirmDialog.isShowNative = true` 时加载 |
| `NA_UNINSTALL_1_BOTTOM` | `NA_Uninstall1_bottom` | 第一层卸载原因页底部 | `CARD_7` | 当前开关为 `true` |
| `NA_UNINSTALL_2_BOTTOM` | `NA_Uninstall2_bottom` | 第二层卸载确认页底部 | `CARD_7` | 当前开关为 `true` |
| `NA_EARTHQUAKE_BOTTOM` | `NA_Earthquake_bottom` | 地震详情模块底部 | SDK `LARGE` | 通过 `EarthquakeAdBridge` 注入，进入模块时加载 |
## Banner 广告

| 常量 | position 值 | 触发位置 | 状态 |
| --- | --- | --- | --- |
| `BA_HOME_BOTTOM` | `BA_Home_bottom` | `MainAct` 底部；权限流程和首页引导完成后加载 | 使用中 |

## Local 渠道平台广告单元

Local 渠道全部为官方/平台测试 ID，配置文件为 `app/src/local/config.gradle`。

| 平台 | 应用 ID | Banner | 插屏 | 原生 | 激励 |
| --- | --- | --- | --- | --- | --- |
| AdMob | `ca-app-pub-3940256099942544~3347511713` | `ca-app-pub-3940256099942544/9214589741` | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-3940256099942544/2247696110` | `ca-app-pub-3940256099942544/5224354917` |
| GAM | 无应用 ID | `/21775744923/example/adaptive-banner` | `/21775744923/example/interstitial` | `/21775744923/example/native` | `/21775744923/example/rewarded` |
| Pangle | `8025677` | `980099802` | `980088188` | `980088216` | `980088192` |
| TopOn | `h1h7t76tddrt25` | `n1h7eoo4ijcp8h` | `n1h7t76tg76hja` | `n1h7t76th560qm` | `n1h7t76tevmst6` |

TopOn 另有 App Key `a5892462779e07420fcf84e9e6b341ece`。这里只列出当前可触发广告类型对应的 ID。

## Google 渠道平台广告单元

`app/src/google/config.gradle` 中 AdMob、GAM、Pangle、TopOn 的应用 ID、App Key 和所有 ad unit ID **当前全部为空字符串**。Google 渠道 SDK 依赖虽然存在，但项目没有可供实际请求的平台广告位参数。本清单仅做静态检查，未执行 Google 渠道构建。

## 初始化、预加载与竞价

- `AppInitializer.initializeRemaxAds()` 通过 `AppOpenBiddingInitializer` 配置 AdMob、GAM、Pangle、TopOn 及各自原生广告渲染器，随后执行 `PreloadController.preloadAll()`。
- 初始化配置设置了 `externallyInitialized = true`，表示底层广告 SDK 的初始化由外部渠道 SDK 负责；这是运行时能否加载广告的前置条件。
- `app/src/main/assets/bidding_config_default.json` 提供本地默认竞价参数；项目源码没有显式读取文件名，推测由广告库按资产约定加载，实际行为应以 SDK 实现为准。
- 默认竞价配置只列出 AdMob、Pangle、TopOn，没有 GAM 平台块；GAM 仍被传入 `BillConfig`，其是否参与竞价取决于 SDK 内部逻辑或远端覆盖。
- Free/Premium 两组默认配置相同：平台优先级 AdMob 1、Pangle 2、TopOn 3，并启用 two-layer bidding。
- 激励场景由 SDK 在可用平台中竞价，10 秒超时，AdMob rewarded 兜底。

## 默认平台频控

| 广告类型 | 每日最大展示 | 每日最大点击 | 最小展示间隔 |
| --- | ---: | ---: | ---: |
| 插屏 | 30 | 8 | 60 秒 |
| 激励 | 20 | 5 | 30 秒 |
| 原生 | 100 | 20 | 10 秒 |
| Banner | 200 | 30 | 5 秒 |

上表在默认 JSON 中对 AdMob、Pangle、TopOn 相同。频控是否被远端配置或 SDK 内部状态覆盖，需要结合运行日志确认。

## 主要代码入口

- 业务 position 定义：`app/src/main/java/net/corekit/monetize/ads/AdPosition.kt`
- 页面广告封装：`app/src/main/java/com/daily/health/manager/utils/AppCompatExt.kt`
- 通用 Activity 插屏/激励逻辑：`app/src/main/java/com/daily/health/manager/ad/BaseInterActivity.kt`
- 广告初始化和平台 ID 注入：`app/src/main/java/com/daily/health/manager/AppInitializer.kt`
- Local 平台 ID：`app/src/local/config.gradle`
- Google 平台 ID：`app/src/google/config.gradle`
- 默认竞价及频控：`app/src/main/assets/bidding_config_default.json`
- 原生渲染器：`app/src/main/java/com/daily/health/manager/ad/renderer/RemaxAdRenderers.kt`

## 当前需要关注的现状

1. `*_BACK` 广告位在 `BaseInterActivity.onCreate()` 中展示，命名/注释与真实触发时机不一致。
2. `HydrateSettingAct`、`HydrateCompleteScreen` 错用默认的 `IV_BloodSugar_back`，没有专属 position。
3. Google 渠道所有平台广告参数为空，无法按当前配置实际请求广告。
4. `NativeAdAutoRefreshManager` 的三个方法都是空实现；`loadNativeWithManager()` 成功时也始终回传 `null`，所以 `enableAutoRefresh` 参数当前不产生刷新效果。
