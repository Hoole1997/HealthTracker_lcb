# 广告位动态开关接入核查

核查基准：`Health Tracker Launcher 广告位.md`，并扫描项目全部 Kotlin/Java 广告调用。

## 控制入口

- 业务广告统一通过 `BusinessAdExt.kt`，每次请求读取 `AdSlotSwitchController.isEnabled(PositionName)`，不缓存结果、不修改大小写。
- 当前 Remax core 1.0.15 的远程键为 `ad_slot_switch`；初始化、解密、paid/organic 分组由 SDK 负责，应用不重复实现。SDK 的 CoreModuleProvider 已调用控制器初始化。
- 缺失/关闭的开关不调用广告 SDK；读取开关异常按关闭处理。
- View 公共工具、闹钟 Compose 原生容器、地震模块桥接均使用统一入口。
- 删除业务启动时无 PositionName 的 `PreloadController.preloadAll`，避免绕过广告位检查请求广告。SDK 初始化保留，开启的广告位按需加载。

## 关闭后的下一步

| 类型 | 关闭后的行为 |
|---|---|
| 插屏 | 回调一次 false；所有现有调用方继续跳转、保存完成、finish 或卸载流程，不等待广告。 |
| Banner | 清空并隐藏容器，结果回调 false；首页不再创建 bannerShowComplete 进行二次等待。 |
| 原生 | 清空并隐藏容器，结果回调 false；退出弹窗/药物列表通过现有回调关闭骨架屏。地震桥接返回 false，Compose 容器高度为零。 |
| 激励 | 直接展示专家建议，页面初始化跳过解锁确认和倒计时；广告开启时，成功、失败或关闭后的 call 回调均直接执行解锁，不按布尔结果拦截下一步。 |

业务回调在广告异常处理外执行，避免回调自身异常导致重复导航。页面生命周期取消时继续传播取消，不执行失效页面的业务回调。

## 按 StreetHtml 精简调用

参考 `/Users/apple/Documents/StreetHtml/app/src/main/java/com/example/lcb/app/utils/BusinessAdExt.kt` 的调用方式：页面启动一次生命周期协程，检查开关，直接调用 SDK，返回业务结果。

- 移除 `AppAdGateway`、`AdSlotGate` 多状态封装、空的 `NativeAdAutoRefreshManager`，删除未使用的全屏原生兼容入口和自动刷新/点击参数。
- 插屏统一使用 `loadInterstitial`；激励统一使用 `loadReward`，不再由多层别名转发。
- 原生的 View、Compose、地震入口共用一个挂起函数，不在已有协程内另启广告协程。
- 加载提示保留原有 Lottie 动画及资源就绪后的 `onReady()` 回调方式。
- SDK 内部超时或取消、但页面协程仍有效时，按广告失败完成回调。页面确实销毁时传播取消，不再导航。
- 全屏请求结束、异常或页面销毁时清理 SDK Loading，避免遮罩遗留。
- Banner 不再把结果回调转为 Deferred 二次等待。既有权限、广告同意及首页引导时序保留。
- 保留原 PositionName、广告关闭后的业务放行、Launcher 展示前通知；未引入 StreetHtml 的频次策略，未更换广告 SDK 版本。

## 覆盖清单

文档 62 个广告位全部有实际代码入口，另发现 2 个卸载插屏，共 64 个被引用的广告位。
两个卸载插屏原先硬编码关闭，本次改为对应线上键控制；文档默认配置未包含它们，因此缺省仍关闭，配置为 1 后才启用。
`NA_New_Guide_Full` 仅有常量，无调用入口，本次不新增广告展示场景。

| PositionName | 调用或路由文件 |
|---|---|
| `IV_BloodSugar_back` | `BaseInterActivity.kt` |
| `IV_BloodPressure_back` | `BaseInterActivity.kt` |
| `IV_Cholesterol_back` | `BaseInterActivity.kt` |
| `IV_HeartRate_back` | `BaseInterActivity.kt` |
| `IV_BMI_back` | `BaseInterActivity.kt` |
| `IV_Water_back` | `BaseInterActivity.kt` |
| `IV_Walk_back` | `BaseInterActivity.kt` |
| `IV_Step_Setting_back` | `StepSettingAct.kt` |
| `IV_BloodSugar_Save` | `HealthRecordAct.kt` |
| `IV_BloodPressure_Save` | `HealthRecordAct.kt` |
| `IV_Cholesterol_Save` | `HealthRecordAct.kt` |
| `IV_HeartRate_Save` | `HealthRecordAct.kt` |
| `IV_BMI_Save` | `HealthRecordAct.kt` |
| `IV_Step_Goal_Save` | `StepSettingAct.kt` |
| `IV_AddMeds_back` | `AddReminderAct.kt` |
| `IV_AddMeds_Save` | `AddReminderAct.kt` |
| `IV_Profile_back` | `ProfileScreen.kt` |
| `IV_InsightsDetails_back` | `InsightsDetailAct.kt` |
| `IV_BloodSugarTrack_back` | `BaseInterActivity.kt` |
| `IV_BloodPressureTrack_back` | `BaseInterActivity.kt` |
| `IV_CholesterolTrack_back` | `BaseInterActivity.kt` |
| `IV_HeartRateTrack_back` | `BaseInterActivity.kt` |
| `IV_BMITrack_back` | `BaseInterActivity.kt` |
| `IV_WaterTrack_back` | `BaseInterActivity.kt` |
| `IV_WalkTrack_back` | `BaseInterActivity.kt` |
| `IV_BloodSugarTrack_Enter` | `TrackerTabFragment.kt` |
| `IV_BloodPressureTrack_Enter` | `TrackerTabFragment.kt` |
| `IV_CholesterolTrack_Enter` | `TrackerTabFragment.kt` |
| `IV_HeartRateTrack_Enter` | `TrackerTabFragment.kt` |
| `IV_BMITrack_Enter` | `TrackerTabFragment.kt` |
| `IV_WaterTrack_Enter` | `TrackerTabFragment.kt` |
| `IV_WalkTrack_Enter` | `TrackerTabFragment.kt` |
| `RV_BloodSugar_Note` | `BaseInterActivity.kt` |
| `RV_BloodPressure_Note` | `BaseInterActivity.kt` |
| `RV_Cholesterol_Note` | `BaseInterActivity.kt` |
| `RV_HeartRate_Note` | `BaseInterActivity.kt` |
| `RV_BMI_Note` | `BaseInterActivity.kt` |
| `NA_Home_exit_dialog` | `ExitDialog.kt` |
| `NA_Main_Tracker_middle` | `TrackerTabFragment.kt` |
| `NA_Hydrate_Complete_bottom` | `HydrateCompleteScreen.kt` |
| `NA_Meds_reminder_list` | `MedsReminderAdapter.kt` |
| `NA_Alarm_Manager_bottom` | `AlarmManageScreen.kt` |
| `NA_Insights_detail_bottom` | `InsightsDetailAct.kt` |
| `NA_Settings_profile_bottom` | `ProfileScreen.kt` |
| `NA_Settings_language_bottom` | `LanguageAct.kt` |
| `NA_NewRecord_BloodSugar_bottom` | `HealthRecordAct.kt` |
| `NA_NewRecord_BloodPressure_bottom` | `HealthRecordAct.kt` |
| `NA_NewRecord_Cholesterol_bottom` | `HealthRecordAct.kt` |
| `NA_NewRecord_HeartRate_bottom` | `HealthRecordAct.kt` |
| `NA_HeartRateMeasure_bottom` | `HeartRateMeasureScreen.kt` |
| `NA_NewRecord_BMI_bottom` | `HealthRecordAct.kt` |
| `NA_Detail_BloodSugar_bottom` | `HealthDetailAct.kt` |
| `NA_Detail_BloodPressure_bottom` | `HealthDetailAct.kt` |
| `NA_Detail_Cholesterol_bottom` | `HealthDetailAct.kt` |
| `NA_Detail_HeartRate_bottom` | `HealthDetailAct.kt` |
| `NA_Detail_BMI_bottom` | `HealthDetailAct.kt` |
| `NA_Detail_confirm_dialog` | `ConfirmDialog.kt` |
| `NA_Uninstall1_bottom` | `UninstallResenActivity.kt` |
| `NA_Uninstall2_bottom` | `UninstallConfirmActivity.kt` |
| `IV_Uninstall1` | `AdConfigManager.kt`, `UninstallResenActivity.kt` |
| `IV_Uninstall2` | `AdConfigManager.kt`, `UninstallConfirmActivity.kt` |
| `NA_Earthquake_bottom` | `AppInitializer.kt` |
| `BA_Home_bottom` | `MainAct.kt` |
| `NA_Alarm_Config_Dialog` | `AlarmEditDialog.kt` |

## 生效时机与范围

线上更新后在下一次广告请求读取最新值；异步返回时再次检查，已关闭的内嵌广告会移除。专家建议页面恢复时也会检查并解锁。
控制器只暴露查询接口，本次不轮询撤回已经展示的广告；已进入 SDK 内部的全屏请求不能通过现有 API 中途取消。Launcher 二进制依赖内部的独立广告逻辑不属于本仓库可修改的业务入口。

## 验证

- `:app:assembleLocalDebug`：通过（JDK 17）。
- `:app:testLocalDebugUnitTest`：通过。广告请求的 9 项测试覆盖全部广告位关闭、逐次动态读取、配置异常、加载期间关闭、激励解锁、SDK 超时/取消与页面销毁。
- 静态扫描：文档 62 个 PositionName 无遗漏；全部直接 `AdShowExt.show*` 调用仅位于 `BusinessAdExt.kt`，没有业务级无广告位预加载调用。
- 本次未修改线上配置，未执行 Google 渠道构建。
