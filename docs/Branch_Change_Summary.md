# feature_1.0.0 分支变更总结（防关联 & 代码安全）

> 本文聚焦当前分支（`feature_1.0.0`）中与“防止关联（静态代码扫描 & 用户行为特征）”和“代码安全”相关的改动，便于评审/回归/发布前验收。

## 1. 基线与范围（对比口径）

- **对比基线**：提交 `f2b0d624`（`perf(monetize): 优化 AdmobNextGenReflectionUtil 性能...`）
- **当前 HEAD**：`a8a8f89`
- **提交数量**：`38` commits（`f2b0d624..HEAD`）
- **变更文件数**：`742` files（`f2b0d624..HEAD`）
- **主要变更集中目录**（按改动文件数量统计）：
  - `app/`（约 685 files）
  - `framework/`（约 18 files）
  - `weather/`（约 8 files）
  - `monetize/`（约 7 files）
  - `build-common/`（约 7 files）
  - `earthquake/`（约 4 files）
  - `appraise/`、`core/`、`scripts/`、`gradle/` 等

> 备注：本文重点总结“防关联 + 安全”的核心链路；大量 UI/资源/模块工程化改动不逐条展开。

## 2. 变更范围总览（按目标归类）

### 2.1 防关联：静态代码扫描维度（APK 静态特征）

核心策略：**“构建期差异化 + 运行时符号可读性降低 + 明文特征减少”**。

- **四大组件/包路径混淆（activityGuard）**：降低 Manifest/入口组件的可读性与可关联性。
- **字符串混淆（StringFog）**：降低明文关键字/域名/路径/埋点 key 等静态检索命中。
- **混淆字典随机化**：每次 Release 构建生成不同字典，降低同源构建间的静态相似度。
- **垃圾代码生成**：增加控制流/符号噪声，提升静态分析成本。
- **Proguard/R8 规则强化**：移除 Source/Line 信息，压缩可读性；同时补齐三方与反射保活规则。
- **Room 标识混淆（Entity/Dao/Table/Column）**：降低数据库 schema 的可读性与特征提取能力。

### 2.2 防关联：用户行为特征维度（埋点/交互/页面结构）

核心策略：**“入口统一 + 行为稳定 + 埋点与 Activity 类名解耦”**。

- **HealthType 体系统一**：通过 `HealthTypeProvider` 解耦“当前健康类型”与 Activity 类名/包名。
- **合并多个 Record/Detail Activity**：统一入口 `HealthRecordActivity`、`HealthDetailActivity`，降低“页面栈结构 + Activity 名称集合”带来的可识别特征。
- **交互回调抑制与生命周期安全**：`RulerView`、`ExpertAdviceView` 等对回调频率/生命周期做约束，避免异常行为特征（例如短时间大量回调、倒计时在后台继续跑等）。

### 2.3 代码安全与健壮性

核心策略：**“对外暴露面最小化 + 生命周期/资源管理可靠 + 构建产物敏感信息管理”**。

- **Manifest exported 与权限声明校准**：控制组件对外暴露面；降低被外部显式调用/劫持的风险。
- **ComposeView 生命周期策略**：避免 Compose/AndroidView 混用场景的泄漏与异常。
- **构建产物与敏感文件管理**：例如混淆 mapping 产物的仓库策略调整。

## 3. 关键防关联措施（静态代码扫描）详述

### 3.1 四大组件/包路径混淆：activityGuard

- **目标**：对 Manifest 中明文暴露的组件（`Application/Activity/Service/Receiver/Provider`）进行类名/包名混淆，降低入口点可读性。
- **配置位置**：`app/build.gradle.kts`
- **启用策略**：仅在执行 `*Release*` 相关 Gradle 任务时启用（避免 Debug/日常开发构建引入不确定性）。
- **关键配置点**：
  - `whiteClassList`：第三方与必须保留的类名白名单（如 Google/Firebase/Koin 等）
  - `otherClassList` / `changePackageList`：自有包名范围（`com.daily.health.manager.*` 下的多业务包）
  - `classNameCharPool` / `dirNameCharPool`：字符池限制，保证混淆结果更“短且离散”

**风险与规避**
- **风险**：如后续引入反射/Router/Hilt/DataBinding 等强依赖类名的机制，可能导致运行时崩溃。
- **规避**：
  - 白名单持续维护
  - Release 构建与回归用例覆盖：启动、通知、前台服务、广播、Provider 等

### 3.2 字符串混淆：StringFog（XOR + bytes）

- **目标**：降低明文字符串特征（关键字/URL/埋点 key/参数名等）被静态扫描命中。
- **配置位置**：`build-common/src/main/kotlin/convention/plugins/AndroidStringFogConventionPlugin.kt`
- **实现策略**：XOR 加密 + **bytes 模式**（从 base64 切换到 bytes），兼顾体积与可分析性。
- **启用策略**：受构建属性控制（按需开启，便于本地/CI 切换）。

### 3.3 混淆字典：构建期随机化生成

- **目标**：让每次 Release 产物的符号集合不同，降低“同源多包静态比对”相关性。
- **脚本位置**：`app/generate-dictionary.gradle.kts`
- **字典特征**：
  - 单/双/三字符组合
  - 方法名伪装、下划线/数字混合等

### 3.4 垃圾代码：构建期生成

- **目标**：引入大量无业务意义的类/方法与控制流，提升静态分析与特征提取成本。
- **脚本位置**：`app/generate-junk-code.gradle.kts`
- **集成点**：通过构建任务挂载到生成流程，并在混淆规则中保留生成代码（避免被完全裁剪）。

### 3.5 Proguard/R8 混淆规则强化

- **配置位置**：`app/proguard-obfuscation.pro`
- **核心点**：
  - 移除 `SourceFile/LineNumberTable` 等调试信息（降低逆向可读性）
  - 保活关键框架（序列化/DI/Room/ViewBinding/WorkManager 等）
  - 配合垃圾代码/自定义字典，形成“混淆闭环”

### 3.6 Room 数据库标识混淆（闭环）

- **目标**：降低数据库 schema 的可读性（表名/列名/实体/DAO 名称），避免通过 DB 结构直接推断业务。
- **策略**：
  - 底层 `@Entity/@Dao` 改为短/无意义命名（例如 `LocalEntityXX` / `LocalDaoXX`）
  - `tableName` 改为 `t01..t11`，列名改为 `c01..`
  - Kotlin 侧通过 `typealias` 保持原业务类型名不改（减少改动面）
- **注意**：`typealias` 对 Java 不可见，因此需确保没有 Java 调用点依赖这些类型。

### 3.7 字符串资源命名规范化（`ht_` 前缀）

- **范围**：仅对 `app` / `weather` / `earthquake` 三个模块**自身定义**的字符串资源（`<string>` / `<string-array>` / `<plurals>`）做调整（`app_name` 例外）。
- **目的**：
  - 统一命名空间，降低同名资源冲突/误引用
  - 对静态扫描而言，减少“常见/可预测资源名”带来的特征命中
- **实现**：全量重命名 + 全量替换引用（XML + 代码）

> 备注：资源名属于静态特征的一部分。若对“字面可读性”有更强诉求，可进一步对部分资源名进行更激进的去语义化（但会显著降低可维护性）。

## 4. 关键防关联措施（用户行为特征）详述

### 4.1 埋点与 Activity 类名解耦：HealthTypeProvider

- **新增接口**：`app/src/main/java/com/daily/health/manager/ui/tracker/HealthTypeProvider.kt`
- **应用点**：
  - `BaseInterActivity`：统一 back 埋点逻辑，避免依赖“具体 Activity 类名集合”
  - `ExpertAdviceView`：优先从宿主提供 `HealthType`，降低对页面结构的硬编码

**收益**
- 页面合并/重构后，埋点维度可稳定在 `HealthType`，避免因类名变化导致埋点维度漂移。
- 对外“行为特征”更一致，降低通过页面栈/类名反推业务结构的可能性。

### 4.2 合并 Record 页面：HealthRecordActivity（统一入口）

- **入口统一**：5 个 RecordActivity 合并为 `HealthRecordActivity`
- **实现策略**：Compose 宿主 + `AndroidView` inflate 复用原 XML，降低迁移风险
- **配套调整**：
  - 统一各入口跳转（首页/历史/详情编辑等）
  - 清理 Manifest 中旧 Activity 声明与源码

**与防关联的关系**
- Activity 数量减少 + 入口统一，降低“页面结构集合”暴露出的静态/动态特征。

### 4.3 合并 Detail 页面：HealthDetailActivity（统一入口）

- **入口统一**：5 个 DetailActivity 合并为 `HealthDetailActivity`
- **兼容点**：
  - 返回插屏与 back 埋点保持一致
  - `ExpertAdviceView` 通过 `HealthTypeProvider` 获取类型

### 4.4 交互稳定与回调频率控制

- **`RulerView`**：对回调触发进行抑制，避免滑动过程产生大量离散事件；并对速度/动画做更安全的边界控制。
- **血糖单位切换同步修复**：通过 `combine` 合并 `currentUnit` 与 `currentValue` 的收集，保证“先配置刻度参数，再定位刻度”是原子行为，避免 UI 与值不同步导致异常交互特征。
- **`ExpertAdviceView`**：倒计时遮罩改为生命周期可暂停/恢复，避免后台继续跑造成状态错乱（同时也降低异常行为序列）。

## 5. 代码安全与工程安全点

### 5.1 Manifest 安全性

- **导出控制**：组件 `exported` 显式声明，降低默认行为差异导致的暴露风险。
- **启动模式统一**：多 Activity 统一 `singleTask`（结合业务需求约束任务栈行为），降低多实例带来的边界问题。
- **权限声明**：对前台服务、通知、开机广播等能力做显式声明，避免因系统版本差异导致不可控行为。

### 5.2 生命周期与资源管理

- **ComposeView 的销毁策略**：在 Compose/AndroidView 混合承载的 Activity 中，显式指定 `ViewCompositionStrategy`，避免泄漏与不可预期的重组。
- **倒计时/回调解绑**：自定义 View 在 `onDetachedFromWindow` / 生命周期回调中做资源释放与暂停处理，降低后台异常。

### 5.3 构建产物敏感信息管理

- **混淆 mapping 产物策略**：将 `app/mapping.txt` 纳入忽略/产物管理（避免误提交导致逆向成本降低）。

## 6. 影响评估与验证建议

### 6.1 编译验证

- Debug 回归建议：
  - `./gradlew :app:assembleInternalDebug`
- Release 防关联链路验证（建议在 CI 或本地专门环境执行）：
  - `./gradlew :app:assembleRelease`
  - 核验 activityGuard/StringFog/字典/垃圾代码任务均按预期开启

### 6.2 运行时回归清单（抽样即可）

- **启动与核心链路**：启动页 -> 首页 -> 统计/历史 -> 详情 -> 编辑/新增 -> 保存
- **合并页覆盖**：BS/BP/BMI/HR/CHO 五种类型分别走一遍“新增 + 编辑”
- **返回行为**：返回插屏展示、back 埋点健康类型正确
- **专家建议**：遮罩/倒计时/解锁弹窗/返回后恢复正常

### 6.3 静态分析抗性核验（发布前建议做一次）

- **Manifest**：检查组件类名/包名已被改写（Release 包）
- **strings**：关键明文是否仍可直接 grep 命中（StringFog 开启时）
- **mapping**：mapping 产物是否按预期生成且不落入仓库
- **Room**：数据库 schema（表名/列名）是否按 `tXX/cXX` 生效

## 7. 相关文档索引

- `docs/APK混淆差异化方案.md`：静态差异化总体策略
- `docs/Record页面合并与Compose迁移_方案A.md`：Record 页面合并与低风险迁移方案

## 8. 工作区未提交改动（未纳入上述“已合入变更”统计）

当前工作区存在未提交文件变更（`git status`）：

- `app/src/main/java/com/daily/health/manager/ui/act/HydrateActivity.kt`
- `app/src/main/java/com/daily/health/manager/ui/adapter/HydrateAdapter.kt`
- `app/src/main/java/com/daily/health/manager/ui/widget/WaveLoadingView.java`（新增/改动）
- 以及若干 `res/drawable` / `res/mipmap` / `res/raw` / `attrs.xml` / 布局文件改动

如需将这部分也纳入“分支变更总结”，建议先补齐 commit（或至少明确其目的与预期风险），再更新本文对应章节。
