# lcb4 UI 代码整理

本次以 `281ad007` 为基线，仅调整内部 UI 类、方法名和源码包路径，保留方法体、参数、返回值、回调时机及资源引用。新代码按展示功能放在 `com.daily.health.manager.presentation` 下，不添加随机命名、冗余逻辑或兼容转发层。

## 目录和类名

| 原位置 / 类名 | 新位置 / 类名 |
| --- | --- |
| `face.dashboard` | `presentation.home` |
| `DashboardSurface` | `HomeOverviewContent` |
| `DashboardModels.kt` | `HomeOverviewModels.kt` |
| `DashboardGuideOverlay.kt` | `HomeGuideOverlay.kt` |
| `HomeHeroUi` / `HomeFeatureCardUi` | `HeartSummaryUi` / `MetricTileUi` |
| `face.history` | `presentation.history` |
| `HistoryRecordItem` / `HistoryAdapter` | `HealthHistoryRow` / `HealthHistoryAdapter` |
| `BloodPressureHistoryItem` | `BloodPressureHistoryRow` |
| `BloodSugarHistoryItem` | `BloodSugarHistoryRow` |
| `BmiHistoryItem` | `BmiHistoryRow` |
| `CholesterolHistoryItem` | `CholesterolHistoryRow` |
| `HeartRateHistoryItem` | `HeartRateHistoryRow` |
| `face.adapter.ChoosePhotoRCVAdapter` | `presentation.feedback.FeedbackPhotoAdapter` |
| `face.adapter.HealthTagAdapter` | `presentation.tags.HealthTagSelectionAdapter` |
| `face.adapter.InsightsArticleAdapter` | `presentation.news.NewsArticleAdapter` |
| `face.adapter.ReminderTimeAdapter` | `presentation.medication.MedicationTimeAdapter` |
| `face.adapter.TargetRangeAdapter` | `presentation.glucose.GlucoseTargetRangeAdapter` |
| `RangeItemDiffCallback` | `GlucoseRangeDiffCallback` |

## 内部方法命名

- 历史记录展示颜色：`getLeveColorRes` → `getLevelColorRes`。
- 标签删除模式：`switchDelectMode` → `setDeletionMode`，私有字段 `isDelectMode` → `deletionModeEnabled`。
- 给药时间列表：`updateTimes` → `submitReminderTimes`。
- 反馈图片加载：`showPhoto` → `loadPhotoPreview`。
- 反馈图片回调：`ChoosePhotoRCVListener` → `PhotoActionListener`，两个方法分别为 `onAddPhotoRequested`、`onPhotoRemovalRequested`。

## 保留的兼容性约束

- `applicationId`、namespace、Manifest 组件名、Activity/Fragment、Worker、Receiver、Service、Provider 不变，避免影响系统恢复、任务调度和 SDK 注册。
- `App`、`AppDelegate`、SDK 回调、广告初始化与加载顺序不变。
- Room 实体/DAO/schema、数据库名称、偏好键、Intent extra 键、通知 action 和渠道标识不变。
- 历史记录 `RecordType` 枚举成员及顺序不变；已有 Intent 和偏好存储传递的是 ordinal，不是本次调整的类名。
- 远程配置、JSON/序列化模型、资源名称、XML 自定义 View 路径不变。
- 心率采集分析、健康等级计算、单位换算、图表绘制、饮水和药物闹钟调度不变。
- Activity、ViewModel 等调用方仅同步静态类型和方法引用，不改业务流程。

## 验证范围

2026-08-31 验证结果：

- 35 个源码文件（含 17 个迁移文件和 18 个调用方）与基线逐项核对：除显式命名/包路径替换和注释外，源码 token 一致；字符串和字符常量保持一致。
- 历史记录 `RecordType` 的 5 个成员及 ordinal 顺序保持一致。
- `:app:assembleLocalDebug`、`:app:testLocalDebugUnitTest` 通过，现有 7 项单元测试无失败。
- `:app:assembleLocalRelease` 通过，包括 Lint 和 R8；混淆映射已包含迁移后的类，未保留原已迁移的顶层类。
- Release 首次离线尝试因缺少 Lint 缓存失败，联网补齐依赖后通过；没有跳过检查。SDK 的 R8 stack-map 警告仍存在，本次未修改 SDK。
- 构建自动生成的混淆字典已恢复，不纳入此次重构。没有执行 Google 渠道构建。

重命名本身不能证明运行时完全无回归；后续仍应在设备上回归首页测量入口、历史记录查看/删除、反馈图片选择/移除、标签选择/删除和药物时间编辑。
