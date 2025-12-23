# HealthTracker 项目改动说明（血糖统计页）

本次改动目标：将健康统计页适配为血糖统计视图，统一单位显示、修复统计逻辑、并改进图表与历史列表的呈现一致性。

## 主要改动

- 统计页视图（Activity）与视图模型（ViewModel）统一血糖单位：
  - 使用 `BsUnit.getPreferredUnit()` 获取用户首选单位，并将图表与摘要统计统一转换到该单位。
  - 图表使用 `ChartPalette.lineBloodSugar` 配色，并设置 `forceIntegerYAxis = false` 以支持 mmol/L 等小数单位。
- 图例与布局更新：
  - `activity_health_statistics.xml` 中将血压双折线图例替换为血糖单折线图例，颜色引用 `@color/chart_line_bs`，文案引用 `@string/blood_sugar`。
- 历史列表单位统一：
  - 新增 `BloodSugarPreferredHistoryItem`，在统计页历史列表统一按用户首选单位显示，避免同列表出现混合单位。
  - `HealthStatisticsActivity` 中将历史列表映射由 `BloodSugarHistoryItem` 替换为 `BloodSugarPreferredHistoryItem`。
- 文案资源：
  - 在 `strings.xml` 中新增标准化资源 `blood_sugar`，避免因命名不一致导致编译或显示问题。

## 受影响文件

- `app/src/main/java/com/healthtracker/blood/suger/viewmodel/HealthStatisticsViewModel.kt`
- `app/src/main/java/com/healthtracker/blood/suger/ui/act/HealthStatisticsActivity.kt`
- `app/src/main/java/com/healthtracker/blood/suger/ui/history/BloodSugarPreferredHistoryItem.kt`（新增）
- `app/src/main/res/layout/activity_health_statistics.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/health_tips_arrays.xml`（新增：健康Tips的标题与文案数组）

## 编译与验证

1. 执行 `./gradlew :app:assembleDebug` 已通过编译（含 Playstore/Internal 两个变体）。
2. 运行应用后，在“健康统计”页验证：
   - 图表图例为“Blood Sugar”，颜色为橙红（`chart_line_bs`）。
   - 折线图允许小数刻度（mmol/L），数据与摘要数值保持一致。
   - 历史列表中所有记录值统一为用户首选单位（mg/dL 或 mmol/L），不再出现同列表混合单位。
   - 日期范围切换（7天/1月/3月/自定义）能够正确更新图表与摘要。
   - Health tips 文案来自资源数组，标题与描述与设计稿一致。

### 新增：历史列表截断与“全部历史”入口显示规则

- 显示规则（已实现，用户确认）：
  - 统计页历史列表仅展示最近 N 条记录，当前 N = 2（根据所选时间范围过滤后再截断）。
  - 当数据库中“存在任意记录”时，“全部历史（All history）”入口始终显示；
    - 即使当前选择的时间范围内没有任何记录，入口也会显示；
    - 仅当全库完全没有记录时隐藏该入口。
- 技术实现：
  - `HealthStatisticsActivity` 中对历史列表 `records.take(2)` 后再提交到 `HistoryAdapter`；
  - `HealthStatisticsViewModel` 新增 `hasAnyRecord: StateFlow<Boolean>`，通过仓库的全量记录 `Flow` 判断是否存在数据；
  - Activity 观察 `hasAnyRecord`，控制 `tv_all_history` 的可见性。
- 验证要点：
  1. 有全库记录，但当前日期范围内无记录：列表为空；“全部历史”入口显示；可点击进入历史页查看全部（默认一年范围）。
  2. 当前日期范围内有 ≥ 3 条记录：列表仅显示 2 条最近记录；“全部历史”入口显示。
  3. 全库完全无记录：列表为空；“全部历史”入口隐藏；底部“添加记录”按钮正常使用。

## 补充：伸缩式 Banner 在 MainActivity 子页面显示的说明

- 说明：
  - `ad_view_container` 位于 `MainActivity` 根布局底部，属于全局容器，因此 Banner 会在 `MainActivity` 的各个 Tab/Fragment 页面中可见（符合需求）。
- 通用健壮性处理：
  - `loadBanner()` 在成功回调时会二次执行 `condition()` 校验：如果未来需要限定某些场景不展示（例如仅首页、或特定页面），可通过 `condition` 精准控制；
  - `bannerShowComplete.complete(...)` 增加 `isCompleted` 判断，避免重复完成引发崩溃。

## 已知问题与后续优化建议

- 摘要（均值/最小/最大）目前仅显示数值，不含单位标识；如需更明确展示，可在布局中为摘要区域补充单位文本（例如靠右的“mg/dL”或“mmol/L”标签）。
- 图表 Y 轴单位未显式显示；如需在 Marker 或轴标题中显示单位，可在 `HealthLineChartManager` 或 `ChartConfigHelper` 添加自定义格式化器。
- `HistoryRecordActivity` 保持原逻辑（显示记录时的单位），符合“历史查看”场景；统计页统一单位用于“统一分析”场景，两者不同可在帮助文档中说明。
## 补充：自定义 View 渲染 Vector/SVG 的注意事项

- 自定义 View 如果使用 `Canvas.drawBitmap()` 绘制图片，不要用 `BitmapFactory.decodeResource()` 去读取 `drawable/*.xml`（VectorDrawable）。该方式会解码失败导致运行时不显示。
- 推荐做法是使用 `AppCompatResources.getDrawable()` 获取 `Drawable`，再绘制到 `Bitmap`（或直接 `Drawable.draw(canvas)`）。

## 补充：appraise 模块 Gradle 约定与维护说明

- `:appraise` 使用约定插件 `android.library`（见 `build-common`），`compileSdk/minSdk/viewBinding/测试依赖` 等由约定插件统一配置，模块内尽量只保留必要的差异化配置（例如 `namespace`、特殊的 consumer 混淆规则）。
- 依赖版本统一从 `gradle/libs.versions.toml`（Version Catalog）管理，模块内避免硬编码版本号，减少升级成本与版本漂移。

- 背景：为降低法务/版权风险、以及渠道审核对“同素材指纹命中”的风险，对 `appraise/src/main/res/raw` 下的 Lottie 动画资源做差异化处理。
- 约束：不改资源名与调用方式（保持 `R.raw.json_emoji_*` 不变），仅做内容层差异化。
 - 处理方式：
   - 调整 Lottie JSON 内部元信息（例如 `nm` 字段、帧率 `fr` 的极小偏移），允许轻微视觉差异。
   - 对 JSON 内内嵌的 `data:image/png;base64,...` PNG 数据插入 PNG `tEXt` 元数据块，确保图片字节级指纹变化且不影响显示效果。
   - 每个原始文件均生成 `.bak` 备份，并移动到 `appraise/raw_backups/`，避免参与 Android 资源打包。
 - 回滚方式：将 `appraise/raw_backups/*.json.bak` 覆盖回 `appraise/src/main/res/raw/` 下对应的 `.json` 文件即可。
 - 验证方式：执行 `./gradlew :appraise:assembleDebug`，并在相关弹窗/页面触发 Lottie 动画加载确认无崩溃、显示正常。

### 新增：健康Tips资源化与使用（随机化）

- 新增 `health_tips_arrays.xml`，包含以下7类健康指标的字符串数组（每类3条标题与文案）：血压、血糖、胆固醇、心率、BMI、步数、饮水。
- 新增 `HealthTipsProvider`（`app/src/main/java/com/healthtracker/blood/suger/tips/HealthTipsProvider.kt`）：
  - 提供 `HealthMetric` 枚举与 `pickRandom(metric)` 方法；
  - 按指标从对应 `string-array` 随机选取一条建议；与数值等级无关。
- 触发规则（用户确认）：
  - 仅在“页面进入”和“日期范围切换”时随机刷新；
  - 不采用“黏性随机”，同一日期范围再次进入页面会重新随机。
- `HealthStatisticsViewModel` 已接入血糖指标（`HealthMetric.BLOOD_SUGAR`），并在 `dateRangeFlow` 变化时刷新健康建议。
- 这样做的好处：
  - 文案统一管理、易于维护与本地化；
  - 与设计稿标题/文案保持一致；
  - 逻辑可复用到其它指标统计页（血压、胆固醇、心率、BMI、步数、饮水）。

## 变更原因与产品思考

- 统计页的目标是统一分析与对比，统一单位能避免认知混淆（尤其是 mmol/L 与 mg/dL 同时出现时）。
- 图表允许小数可避免 mmol/L 的值被强制取整，提升数据准确性与可读性。
- 历史列表统一单位，有助于用户在同一视图下快速比较数据走势与分布。

---
如需进一步定制（例如在摘要区域显示单位、在图表 Marker 中标注单位），请提出需求，我会在不增加过度复杂性的前提下提供最简洁的改动方案。

---

## 文档维护与发布流程建议（复盘）

- 建议在每次发布后打上 Git tag（例如 `v1.0.3`、`v1.0.4`），以便后续基于 tag 精确生成版本差异与变更记录。
- 若希望 `docs/` 下的变更记录能被工具链读取与自动化处理，建议避免在 `.gitignore` 中全局忽略 `*.md`，或显式放行 `docs/**`。

## 2025-12-22 补充变更记录（DI/构建/工程）

- `BaseMVVMActivity`：通过 `CreationExtras` 创建 `SavedStateHandle` 并注入到 Koin ViewModel，解决以往手动 `SavedStateHandle()` 导致的参数丢失/状态不恢复问题。
- `KoinModules`：将依赖 `SavedStateHandle` 的 ViewModel 调整为 `viewModel { (handle: SavedStateHandle) -> ... }` 形式，确保注入的是“带 extras 的 handle”。
- `HistoryViewModel/HealthStatisticsViewModel`：统一/新增关键参数在 `SavedStateHandle` 中读写（例如 `RECORD_TYPE`、`date_range_preset`），支持旋转与进程重建恢复。
- `build-common`：StringFog 模式由 `base64` 切换为 `bytes`。
- 工程维护：忽略 `app/mapping.txt`（混淆 mapping 产物）；同时在 `.gitignore` 放行 `README.md`，避免被 `*.md` 规则误伤导致工具链无法读取。

### 补充：四大组件包路径/类名混淆（activityGuard）

- 目标：对 `AndroidManifest.xml` 中明文暴露的四大组件（含 `Application`）进行名称与包路径混淆，降低入口组件的可读性。
- 配置位置：`app/build.gradle.kts`。
- 启用策略：仅在执行 `*Release*` 构建任务时启用，避免 Debug 构建引入额外不确定性。
- 范围：仅处理 Manifest 声明的 `Application`、`Activity`、`Service`、`Receiver`、自有 `Provider`（不包含 `androidx.core.content.FileProvider`）。
- 可选：若希望自定义 View 的包路径也不明显，可额外将 `ui.weight` / `ui.widget` 等自定义 View 包加入混淆范围（需要同时确保布局 XML 的类名引用被同步替换）。
- 验收建议：
  - `SplashActivity` 启动正常（Launcher）。
  - 开机广播/包替换广播正常（`SystemBootReceiver`）。
  - 通知动作广播正常（`NotificationActionReceiver`）。
  - 前台服务正常（`HealthService`），FCM 回调正常（`MessageService`）。
- 已知注意事项：若后续升级 AGP 版本导致构建链路变化，需要重点确认插件是否仍能正确更新合并后的 Manifest（否则会出现“类已混淆但 Manifest 未更新”导致无法启动）。
- 重新生成映射：插件默认会增量复用 `app/mapping.txt`，如需生成新的包/类映射，需要先删除 `app/mapping.txt` 再执行 Release 构建。

## 2025-12-22 补充变更记录（资源命名规范）

- 范围：仅对 `app` / `weather` / `earthquake` 三个模块自身定义的字符串资源（`<string>` / `<string-array>` / `<plurals>`）进行统一命名调整。
- 规则：除 `app_name` 外，所有字符串资源名统一添加 `ht_` 前缀；并全量更新工程内引用（XML 的 `@string/@array/@plurals`，以及代码的 `R.string/R.array/R.plurals`）。
- 目的：统一资源命名空间，降低不同模块/功能迭代中出现“同名资源冲突/误引用”的概率。
- 验证：已通过 `./gradlew :app:assembleDebug` 编译验证。

## 2025-12-23 临时下线（天气/地震）

- 目标：暂时移除天气与地震相关功能（包含通知），但保留模块代码，便于后续快速恢复。
- 构建策略：仅将 `:weather` / `:earthquake` 从工程 include 与 `app` 依赖中移除，使其不参与编译与打包。
- App 侧改动：移除 `app` 模块内所有天气/地震入口与引用（主页入口、推送/通知样式、以及地震初始化调度）。
- 验证：已通过 `./gradlew :app:assembleInternalDebug` 编译验证。

### 回滚方式

- 将 `settings.gradle.kts` 中的 `include(":weather")`、`include(":earthquake")` 加回。
- 将 `app/build.gradle.kts` 中的 `api(project(":weather"))`、`api(project(":earthquake"))` 加回。
- 恢复 `app` 模块中与天气/地震相关的入口/通知样式/初始化代码（按本次提交 diff 反向回滚即可）。

## 2025-12-23 启动页迁移（Compose 承载，方案A）

- 启动入口仍为 `SplashActivity`（Manifest 不变），保持原有开屏广告/上报/状态机与跳转逻辑不变；通知权限申请已从启动页移除（计划在首页触发）。
- 将 `ht_activity_splash.xml` 精简为单个 `ComposeView`，在 `SplashActivity` 中通过 `composeView.setContent { ... }` 渲染启动页 UI。
- 当前阶段暂不实现“隐私政策入口”，后续如需恢复可在 Compose UI 中补充点击区域并复用原跳转逻辑。

## 2025-12-23 未使用图片资源清理（drawable/mipmap）

- 范围：仅删除工程内 `src/main/res/drawable*` 下 **零引用** 的图片资源（本次均为 `.xml` drawable），不做重命名/不做兼容 alias。
- 扫描口径：同时扫描代码与 XML 引用（`R.drawable.*` / `R.mipmap.*` / `@drawable/*` / `@mipmap/*`），并额外检查是否存在 `Resources.getIdentifier(..., "drawable"|"mipmap", ...)` 等动态查找（本项目未发现此类用法）。
- 删除清单：
  - `app/src/main/res/drawable/ht_bg_assistant_fg.xml`
  - `app/src/main/res/drawable/ht_bg_blue.xml`
  - `app/src/main/res/drawable/ht_bg_hydrate_drink.xml`
  - `app/src/main/res/drawable/ht_bg_hydrate_reminder_add.xml`
  - `app/src/main/res/drawable/ht_bg_label_selector.xml`
  - `app/src/main/res/drawable/ht_bg_rect_white_16.xml`
  - `app/src/main/res/drawable/ht_ic_ad_close.xml`
  - `app/src/main/res/drawable/ht_ic_feedback_add.xml`
  - `app/src/main/res/drawable/ht_ic_fis_home.xml`
  - `app/src/main/res/drawable/ht_ic_sync_check_normal.xml`
  - `app/src/main/res/drawable/ht_ic_weather.xml`
  - `monetize/src/main/res/drawable/bg_ad_label_enhanced.xml`
  - `monetize/src/main/res/drawable/bg_button_gray_rounded.xml`
  - `monetize/src/main/res/drawable/bg_button_rounded.xml`
  - `monetize/src/main/res/drawable/bg_native_ad_card.xml`
  - `monetize/src/main/res/drawable/ic_ad_collapse.xml`
- 验证：已执行 `./gradlew :app:assembleInternalDebug` 编译通过。
