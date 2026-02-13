# 🚀 CI/CD 工作流集成指南 V2.0

本指南帮助团队成员将自己的 Android 工程接入当前的自动化构建与分发流程。按照步骤操作即可完成改造。

> **替代旧版 `CI_WORKFLOW_GUIDE.md`**

---

## 目录

1. [架构速览](#1-架构速览)
2. [前置准备](#2-前置准备)
3. [Step 1: 复制 scripts 目录](#3-step-1-复制-scripts-目录)
4. [Step 2: 改造根 build.gradle.kts](#4-step-2-改造根-buildgradlekts)
5. [Step 3: 改造 app/build.gradle.kts](#5-step-3-改造-appbuildgradlekts)
6. [Step 4: 填写配置文件](#6-step-4-填写配置文件)
7. [Step 5: 复制 CI Workflow](#7-step-5-复制-ci-workflow)
8. [Step 6: 配置 GitHub Secrets](#8-step-6-配置-github-secrets)
9. [Step 7: 配置镜像同步](#9-step-7-配置镜像同步)
10. [Step 8: 验证](#10-step-8-验证)
11. [日常使用](#11-日常使用)
12. [常见问题](#12-常见问题)

---

## 1. 架构速览

```
开发者运行脚本 → 自动创建 Git Tag → 推送到远程 → CI 自动构建 → 自动分发
```

**两种模式**:

| 模式 | 触发方式 | Tag 格式 | 产物 | 分发目标 |
|------|----------|----------|------|----------|
| **Internal (内测)** | `./scripts/publish_internal.sh` | `T1.0.7.A` | APK | Firebase App Distribution |
| **Official (正式)** | `./scripts/publish_playstore.sh` | `1.0.7` | AAB | 飞书云盘 + 卡片通知 |

**核心原理**: 项目不再使用 `productFlavors`，而是通过两套配置文件 (`internal.gradle` / `official.gradle`) 在 Gradle 加载时动态切换。本地默认用 internal 测试配置，CI 构建正式版时通过参数 `-PremoteOverride=true` 切换到 official。

**本地目录结构要求**:
```
你的工作区/
├── YourProject/       ← 你的业务仓库
└── android-ci/        ← 公共工具仓库 (clone 一次即可)
```

工具仓库地址: `git clone <你的 android-ci 仓库地址>`

---

## 2. 前置准备

开始改造前，请确认以下条件：

- [ ] 项目使用 Kotlin DSL (`build.gradle.kts`)
- [ ] 已有 GitHub 仓库（CI 运行在 GitHub Actions）
- [ ] 手边有以下文件：
  - Internal 签名文件 (`.jks`)
  - Official/Playstore 签名文件 (`.jks`)
  - 两套 `google-services.json` (Internal 和 Official 的 Firebase 配置)
  - Firebase Service Account JSON 密钥 (用于上传 APK 到 Firebase App Distribution)
- [ ] 已 clone 公共工具仓库到本地 `../android-ci/` 目录

---

## 3. Step 1: 复制 scripts 目录

将以下文件复制到你项目的 `scripts/` 目录下：

```bash
mkdir -p scripts
```

### 3.1 必须创建的文件

#### `scripts/internal.gradle` — 内测配置

```groovy
ext {
    // ===================== AdMob (测试 ID) =====================
    admob = [applicationId: "ca-app-pub-3940256099942544~3347511713",
             adUnitIds    : [banner      : "ca-app-pub-3940256099942544/9214589741",
                             interstitial: "ca-app-pub-3940256099942544/1033173712",
                             splash      : "ca-app-pub-3940256099942544/9257395921",
                             native      : "ca-app-pub-3940256099942544/2247696110",
                             full_native : "ca-app-pub-3940256099942544/2247696110",
                             rewarded    : "ca-app-pub-3940256099942544/5224354917",
                             rewarded_interstitial: "ca-app-pub-3940256099942544/5354046379"
             ]
    ]

    // ===================== Pangle (测试 ID) =====================
    // ⚠️ 以下为示例测试 ID，请替换为你在穿山甲平台注册的测试 ID
    // 如果不使用 Pangle，保留空字符串即可
    pangle = [applicationId: "8025677",                  // ← 替换
              adUnitIds    : [splash      : "890000078",
                              banner      : "980099802",
                              interstitial: "980088188",
                              native      : "980088216",
                              full_native : "980088216",
                              rewarded    : "980088192"
              ]
    ]

    // ===================== TopOn (测试 ID) =====================
    // ⚠️ 以下为示例测试 ID，请替换为你在 TopOn 平台注册的测试 ID
    // 如果不使用 TopOn，保留空字符串即可
    topon = [applicationId: "a5aa1f9deda26d",             // ← 替换
             appKey       : "4f7b9ac17decb9babec83aac078742c7",  // ← 替换
             adUnitIds    : [interstitial: "b5baca53984692",
                             rewarded    : "b5b449fb3d89d7",
                             native      : "b5aa1fa2cae775",
                             splash      : "b5f73fe0c5db29",
                             full_native : "b5aa1fa501d9f6",
                             banner      : "b5baca4f74c3d8"
             ]
    ]

    // ===================== 应用基础配置 =====================
    app = [applicationId: "com.your.app.internal",   // ← 替换为你的内测包名
           show_log     : true,
           string_fog   : true,
           shifter_mode : "internal"
    ]

    // ===================== 业务 URL =====================
    url = [privacyUrl: "https://example.com/privacy",   // ← 替换
           teamUrl   : "https://example.com/team",       // ← 替换
           fcmUrl    : "https://your-fcm-server.com",    // ← 替换
           fcmPkg    : "com.your.app.official",           // ← 替换为正式包名
           email     : "your@email.com"                   // ← 替换
    ]

    // ===================== 统计归因 =====================
    analytics = [adjustAppToken        : "your_adjust_token",           // ← 替换
                 thinkingDataAppId     : "your_thinking_data_id",       // ← 替换
                 thinkingDataServerUrl : "https://your-td-server.com",  // ← 替换
                 defaultUserChannel    : "paid"
    ]
}
```

#### `scripts/official.gradle` — 正式配置

> **⚠️ 重要**: 此文件必须包含与 `internal.gradle` **完全相同的所有字段**。Groovy `ext` 赋值是整体覆盖而非合并，缺少任何字段都会导致构建时该值为 `null`。

```groovy
ext {
    // ===================== AdMob (正式 ID) =====================
    admob = [applicationId: "ca-app-pub-xxx",       // ← 替换为正式 AdMob ID
             adUnitIds    : [banner      : "ca-app-pub-xxx/xxx",
                             interstitial: "ca-app-pub-xxx/xxx",
                             splash      : "ca-app-pub-xxx/xxx",
                             native      : "ca-app-pub-xxx/xxx",
                             full_native : "ca-app-pub-xxx/xxx",
                             rewarded    : "ca-app-pub-xxx/xxx",
                             rewarded_interstitial: "ca-app-pub-xxx/xxx"
             ]
    ]

    // ===================== Pangle (正式 ID) =====================
    pangle = [applicationId: "xxx",
              adUnitIds    : [banner      : "",
                              interstitial: "xxx",
                              splash      : "",
                              native      : "",
                              full_native : "",
                              rewarded    : "xxx"
              ]
    ]

    // ===================== TopOn (正式 ID) =====================
    topon = [applicationId: "xxx",
             appKey       : "xxx",
             adUnitIds    : [banner      : "",
                             interstitial: "xxx",
                             splash      : "xxx",
                             native      : "xxx",
                             full_native : "",
                             rewarded    : "xxx"
             ]
    ]

    // ===================== 应用基础配置 =====================
    app = [applicationId: "com.your.app.official",   // ← 替换为正式包名
           show_log     : false,
           string_fog   : true,
           shifter_mode : "official"
    ]

    // ===================== 业务 URL (正式) =====================
    // ⚠️ 必须包含所有字段
    url = [privacyUrl: "https://your-site.com/privacy",
           teamUrl   : "https://your-site.com/team",
           fcmUrl    : "https://your-fcm-server.com",
           fcmPkg    : "com.your.app.official",
           email     : "your@email.com"
    ]

    // ===================== 统计归因 (正式) =====================
    analytics = [adjustAppToken        : "your_prod_adjust_token",
                 thinkingDataAppId     : "your_prod_td_id",
                 thinkingDataServerUrl : "https://your-prod-td-server.com",
                 defaultUserChannel    : "paid"
    ]
}
```

#### `scripts/sign.gradle` — 统一签名加载器

复制后需修改 **Fallback 签名信息**（标记了 `← 替换` 的 4 处）：

```groovy
/**
 * 统一的签名配置脚本
 * 优先从 scripts/sign.properties 读取，CI 会动态生成该文件。
 * 如果不存在则使用 fallback 签名。
 */
ext.setupSigningConfigs = { android ->
    android.signingConfigs {
        internal {
            def internalPropsFile = file("${project.rootDir}/scripts/sign.properties")
            if (internalPropsFile.exists()) {
                def props = new Properties()
                internalPropsFile.withInputStream { props.load(it) }
                storeFile = file("${project.rootDir}/scripts/" + props.getProperty('keystore'))
                storePassword = props.getProperty('keystore.password')
                keyAlias = props.getProperty('keyAlias')
                keyPassword = props.getProperty('keyPassword')
                println("🔑 [Sign] Applied signing config from scripts/sign.properties")
            } else {
                // Fallback: 你的本地默认签名
                storeFile = file("${project.rootDir}/scripts/your-debug.jks")  // ← 替换
                storePassword = "your_password"                                 // ← 替换
                keyAlias = "your_alias"                                         // ← 替换
                keyPassword = "your_password"                                   // ← 替换
                println("⚠️ [Sign] Using Fallback signing config")
            }
        }
    }
    android.buildTypes {
        release { signingConfig = android.signingConfigs.internal }
        debug   { signingConfig = android.signingConfigs.internal }
    }
}
```

#### `scripts/sign.properties` — 本地签名配置

```properties
keystore=your-debug.jks
keystore.password=your_password
keyAlias=your_alias
keyPassword=your_password
```

> **注意**: CI 会动态覆盖此文件。本地版本仅用于开发调试。

### 3.2 复制发布脚本和工具脚本

从参考项目直接复制以下脚本，**无需修改**（它们通过 `app/build.gradle.kts` 读取版本号，路径已参数化）：

```bash
# 从参考项目复制发布脚本
cp <参考项目>/scripts/publish_internal.sh  scripts/
cp <参考项目>/scripts/publish_playstore.sh scripts/
cp <参考项目>/scripts/publish_local.sh     scripts/

# 复制飞书工具脚本 (用于获取群 Chat ID，配置飞书通知时需要)
cp <参考项目>/scripts/get_lark_chat_id.py  scripts/

# 赋予执行权限
chmod +x scripts/publish_*.sh
```

### 3.3 复制 JKS 签名文件

将你的 Internal 签名文件放入 `scripts/`，**文件名必须与 `sign.properties` 中 `keystore=` 的值一致**：

```bash
# 假设你的签名文件叫 my-debug.jks，sign.properties 中应写 keystore=my-debug.jks
cp path/to/your-internal.jks scripts/my-debug.jks
```

### 3.4 .gitignore 检查

确保以下内容在 `.gitignore` 中：

```gitignore
# 签名文件
*.jks
google-services.json
google-services-json-key.json

# 签名配置 (包含密码，CI 会动态生成覆盖)
scripts/sign.properties

# 构建产物
release_notes.txt
build/secrets/
```

---

## 4. Step 2: 改造根 build.gradle.kts

在你项目的**根** `build.gradle.kts` 中，在 `plugins {}` 块**之后**添加 Global Shifter：

```kotlin
// ==========================================
// 🚀 全局三层架构配置加载器 (Global Shifter)
// ==========================================
val remoteOverride = project.hasProperty("remoteOverride")
val shifterFile = if (remoteOverride) "scripts/official.gradle" else "scripts/internal.gradle"

// 加载配置到根项目 ext 中，使所有子模块可见
apply(from = "scripts/internal.gradle")
if (remoteOverride) {
    logger.lifecycle("📡 [Global Shifter] Applying official config patch...")
    apply(from = shifterFile)
}
```

**作用**: 让 `monetize`、`core`、`metrics` 等子模块能通过 `findProperty("admob")` 等方式读取配置。

---

## 5. Step 3: 改造 app/build.gradle.kts

### 5.1 引入签名脚本和 Shifter（在 `plugins {}` 之后）

```kotlin
// 引入统一的签名配置脚本
apply(from = "../scripts/sign.gradle")

// ==========================================
// 🚀 三层解耦配置加载器 (The Shifter)
// ==========================================
val remoteOverride = project.hasProperty("remoteOverride")
val shifterFile = if (remoteOverride) "../scripts/official.gradle" else "../scripts/internal.gradle"

// 1. 加载本地默认配置
apply(from = "../scripts/internal.gradle")

// 2. CI 传入 -PremoteOverride=true 时叠加 official 配置
if (remoteOverride) {
    println("📡 [Shifter] Detected Remote Override Mode. Applying official config...")
    apply(from = shifterFile)
}

// 提取 ext 变量
val admob = extensions.extraProperties["admob"] as Map<*, *>
val admobUnit = admob["adUnitIds"] as Map<*, *>
val appConfig = extensions.extraProperties["app"] as Map<*, *>
val urls = extensions.extraProperties["url"] as Map<*, *>
val analytics = extensions.extraProperties["analytics"] as Map<*, *>

val showLog = appConfig["show_log"] as Boolean
val shifterMode = appConfig["shifter_mode"] ?: "internal"
println("📦 [Shifter] Build Mode: $shifterMode | Package: ${appConfig["applicationId"]}")
```

### 5.2 删除 productFlavors

将原来的 `productFlavors { internal {...}; playstore {...} }` **整段删除**。

### 5.3 修改 defaultConfig

```kotlin
android {
    defaultConfig {
        // 从配置文件读取包名
        applicationId = appConfig["applicationId"] as String
        
        // 动态支持从 CI Tag 注入版本名
        val semanticVersion = project.findProperty("internalVersionName")?.toString()
        if (semanticVersion != null && semanticVersion.isNotEmpty()) {
            println("🏷️ [Shifter] Override VersionName: $semanticVersion")
            versionName = semanticVersion.removePrefix("v")
        }

        // 使用配置文件中的值
        buildConfigField("String", "PRIVACY_POLICY", "\"${urls["privacyUrl"]}\"")
        buildConfigField("String", "FCM_URL", "\"${urls["fcmUrl"]}\"")
        buildConfigField("String", "FCM_PKG", "\"${urls["fcmPkg"]}\"")
        buildConfigField("String", "FEEDBACK_EMAIL", "\"${urls["email"]}\"")
        buildConfigField("String", "ADMOB_APPLICATION_ID", "\"${admob["applicationId"]}\"")
        // ... 其他 buildConfigField 按需添加

        // 仅本地 internal 模式追加版本后缀
        if (shifterMode == "internal" && semanticVersion == null) {
            versionNameSuffix = "-internal"
        }
    }
}
```

### 5.4 配置 Firebase App Distribution（在 `buildTypes.release` 内）

**前提**: 需要先添加 Firebase App Distribution 插件：
- 在 `app/build.gradle.kts` 的 `plugins {}` 中添加:
  ```kotlin
  id("com.google.firebase.appdistribution")
  ```
- 在根 `build.gradle.kts` 的 `buildscript.dependencies` 中添加:
  ```kotlin
  classpath("com.google.firebase:firebase-appdistribution-gradle:<version>")
  ```
  或使用 Version Catalog 管理版本。

```kotlin
buildTypes {
    release {
        // ... 你原有的 minify/proguard 配置 ...

        firebaseAppDistribution {
            appId = System.getenv("FIREBASE_APP_ID")
                ?: System.getenv("INTERNAL_FIREBASE_APP_ID") ?: ""
            serviceCredentialsFile =
                rootProject.file("scripts/google-services-json-key.json").absolutePath
            releaseNotesFile = rootProject.file("release_notes.txt").absolutePath
            groups = "internal-testers"
        }
    }
}
```

### 5.5 调用签名配置（在 `android {}` 块之后）

```kotlin
// 调用统一签名配置脚本设置签名
apply<Any> {
    extensions.extraProperties["setupSigningConfigs"]?.let { setupFn ->
        if (setupFn is groovy.lang.Closure<*>) {
            setupFn.call(android)
        }
    }
}
```

### 5.6 本地验证

```bash
# 验证 internal 模式编译通过
./gradlew assembleRelease

# 验证 official 模式编译通过 (模拟 CI)
./gradlew assembleRelease -PremoteOverride=true
```

两次都应该 BUILD SUCCESSFUL。观察日志中的 `[Shifter]` 输出确认模式切换正确。

---

## 6. Step 4: 填写配置文件

打开 `scripts/internal.gradle` 和 `scripts/official.gradle`，替换所有标记了 `← 替换` 的值为你项目的实际配置。

**校验清单**:
- [ ] `internal.gradle` 中 `app.applicationId` 为内测包名
- [ ] `official.gradle` 中 `app.applicationId` 为正式包名
- [ ] 两个文件的配置块数量和字段名**完全一致**
- [ ] AdMob / Pangle / TopOn 的 ID 已替换（不用的广告平台保留空字符串）
- [ ] URL 和 analytics 已替换

---

## 7. Step 5: 复制 CI Workflow

```bash
mkdir -p .github/workflows
cp <参考项目>/.github/workflows/android_ci.yml .github/workflows/
```

### 7.1 需要修改的地方

打开 `.github/workflows/android_ci.yml`，修改以下内容：

| 行 | 原值 | 替换为 |
|----|------|--------|
| `repository:` | `ReMax-ci/android-ci` | 你的 android-ci 仓库地址 (如 `YourOrg/android-ci`) |

其余内容（Secret 名称、构建逻辑等）**无需修改**，它们是通用的。

---

## 8. Step 6: 配置 GitHub Secrets

进入你的 GitHub 仓库 → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

> **💡 快速生成提示**: 项目中提供了辅助脚本 `scripts/generate_ci_secrets_helper.sh`，可在本地批量生成 Base64 编码值。运行后在 `build/secrets/` 目录查看结果：
> ```bash
> sh scripts/generate_ci_secrets_helper.sh
> ```
> ⚠️ 使用后务必清理: `rm -rf build/secrets/`

### 8.1 Internal 构建 (必配)

按以下步骤逐个添加：

**① 生成签名证书 Base64**
```bash
# 在项目根目录执行，生成 Base64 并复制到剪贴板 (Mac)
base64 -i scripts/pdfreader.jks | tr -d '\n' | pbcopy
```
将剪贴板内容添加为 Secret: **`INTERNAL_KEYSTORE_BASE64`**

**② 生成 google-services.json Base64**
```bash
base64 -i app/google-services.json | tr -d '\n' | pbcopy
```
添加为: **`INTERNAL_GOOGLE_SERVICES_JSON_BASE64`**

**③ 签名密码** — 从 `scripts/sign.properties` 中读取，分别添加：
- **`INTERNAL_STORE_PASSWORD`** → `keystore.password` 的值
- **`INTERNAL_KEY_ALIAS`** → `keyAlias` 的值
- **`INTERNAL_KEY_PASSWORD`** → `keyPassword` 的值

**④ Firebase App ID**
在 Firebase Console → 项目设置 → 常规 → 你的应用 → App ID (格式: `1:xxx:android:xxx`)
添加为: **`INTERNAL_FIREBASE_APP_ID`**

**⑤ Firebase Service Account 凭证**
1. 打开 [Google Cloud Console - Service Accounts](https://console.cloud.google.com/iam-admin/serviceaccounts)
2. 选择 Firebase 项目 → **创建服务账号**
3. 名称: `firebase-ci-uploader`
4. 角色: 搜索选择 **`Firebase App Distribution Admin`**
5. 创建 JSON 密钥 → 浏览器自动下载 `.json` 文件
6. 编码并添加:
```bash
base64 -i <下载的文件>.json | tr -d '\n' | pbcopy
```
添加为: **`INTERNAL_FIREBASE_CREDENTIAL_FILE_CONTENT`**

### 8.2 Official (Playstore) 构建 (如需)

与 Internal 步骤相同，使用正式版的签名文件和 google-services.json：

```bash
# 正式签名证书
base64 -i path/to/release.jks | tr -d '\n' | pbcopy
# → PLAYSTORE_KEYSTORE_BASE64

# 正式 google-services.json
base64 -i path/to/official-google-services.json | tr -d '\n' | pbcopy
# → PLAYSTORE_GOOGLE_SERVICES_JSON_BASE64
```

添加密码 Secrets:
- **`PLAYSTORE_STORE_PASSWORD`**
- **`PLAYSTORE_KEY_ALIAS`**
- **`PLAYSTORE_KEY_PASSWORD`**

### 8.3 飞书通知 (可选)

如需构建结果自动通知到飞书群：

1. 在[飞书开放平台](https://open.feishu.cn)创建应用，获取 App ID 和 App Secret
2. 将应用机器人拉入目标群
3. 获取群 Chat ID：
```bash
LARK_APP_ID=xxx LARK_APP_SECRET=xxx python3 scripts/get_lark_chat_id.py
```
4. (可选) 在飞书云盘创建文件夹，获取 Folder Token

添加 Secrets:
- **`LARK_APP_ID`**
- **`LARK_APP_SECRET`**
- **`LARK_CHAT_ID`**
- **`LARK_FOLDER_TOKEN`** (可选，不配则跳过文件上传)

### 8.4 工具仓库访问 (必配)

CI 需要拉取 `android-ci` 私有仓库：

1. 在 GitHub → Settings → Developer settings → Personal access tokens → 生成 Token
2. 权限勾选 **`repo`**
3. 添加为: **`GH_PAT_FOR_SCRIPTS`**

### 8.5 Secrets 检查清单

| # | Secret Name | 必需 | 已配置 |
|---|-------------|------|--------|
| 1 | `INTERNAL_KEYSTORE_BASE64` | ✅ | ☐ |
| 2 | `INTERNAL_GOOGLE_SERVICES_JSON_BASE64` | ✅ | ☐ |
| 3 | `INTERNAL_STORE_PASSWORD` | ✅ | ☐ |
| 4 | `INTERNAL_KEY_ALIAS` | ✅ | ☐ |
| 5 | `INTERNAL_KEY_PASSWORD` | ✅ | ☐ |
| 6 | `INTERNAL_FIREBASE_APP_ID` | ✅ | ☐ |
| 7 | `INTERNAL_FIREBASE_CREDENTIAL_FILE_CONTENT` | ✅ | ☐ |
| 8 | `GH_PAT_FOR_SCRIPTS` | ✅ | ☐ |
| 9 | `PLAYSTORE_KEYSTORE_BASE64` | 按需 | ☐ |
| 10 | `PLAYSTORE_GOOGLE_SERVICES_JSON_BASE64` | 按需 | ☐ |
| 11 | `PLAYSTORE_STORE_PASSWORD` | 按需 | ☐ |
| 12 | `PLAYSTORE_KEY_ALIAS` | 按需 | ☐ |
| 13 | `PLAYSTORE_KEY_PASSWORD` | 按需 | ☐ |
| 14 | `LARK_APP_ID` | 可选 | ☐ |
| 15 | `LARK_APP_SECRET` | 可选 | ☐ |
| 16 | `LARK_CHAT_ID` | 可选 | ☐ |
| 17 | `LARK_FOLDER_TOKEN` | 可选 | ☐ |

---

## 9. Step 7: 配置镜像同步

如果你使用内网仓库 (Gitea/GitLab)，需配置 Push Mirror 到 GitHub：

1. 进入内网仓库 → **设置** → **Mirror Settings** / **Push Mirrors**
2. 填写:
   - **Repository URL**: `https://github.com/<User>/<Repo>.git`
   - **Username**: GitHub 用户名
   - **Password**: GitHub PAT（**必须勾选 `workflow` 权限**，否则无法同步 `.github/workflows/`）
3. 保存后点击 **同步** 测试

如果直接使用 GitHub 作为主仓库，跳过此步。

---

## 10. Step 8: 验证

### 10.1 本地验证

```bash
# 1. 确认 Internal 模式编译
./gradlew assembleRelease
# 期望日志: 📦 [Shifter] Build Mode: internal | Package: com.your.app.internal

# 2. 确认 Official 模式编译
./gradlew assembleRelease -PremoteOverride=true
# 期望日志: 📡 [Shifter] Detected Remote Override Mode...
# 期望日志: 📦 [Shifter] Build Mode: official | Package: com.your.app.official
```

### 10.2 CI 验证

```bash
# 3. 提交所有改动到 main 分支 (GitHub Actions 需要 main 分支有 workflow 文件)
git add .
git commit -m "build(ci): 接入三层 Shifter CI/CD 架构"
git push origin main

# 4. 首次触发 Internal 构建
./scripts/publish_internal.sh
# 脚本会创建 T{version}.A 并推送

# 5. 在 GitHub Actions 页面查看构建结果
# https://github.com/<User>/<Repo>/actions
```

### 10.3 验证通过的标志

- [ ] GitHub Actions 构建 ✅ 通过
- [ ] Firebase 收到 APK（如果配了 Firebase Secrets）
- [ ] 飞书群收到通知卡片（如果配了飞书 Secrets）

---

## 11. 日常使用

改造完成后，日常发版只需运行一条命令：

### 发布内测版

```bash
./scripts/publish_internal.sh
```

脚本自动完成：读取版本号 → 计算下一个 Tag (如 `T1.0.7.B`) → git tag → git push → CI 自动构建 → Firebase 分发

### 发布正式版

```bash
./scripts/publish_playstore.sh
```

脚本自动完成：读取版本号 → 创建 Tag (如 `1.0.7`) → git push → CI 自动构建 AAB → 飞书群通知 + 云盘下载

### 本地应急构建

```bash
# CI 不可用时的应急方案，本机直接构建并上传 Firebase
./scripts/publish_local.sh
```

### 手动触发 CI

在 GitHub → Actions → `Android Release Build` → **Run workflow** → 选择 mode 和 variant

---

## 12. 常见问题

### 🔴 本地编译报错: 找不到 `version_manager.py`

**原因**: 公共工具仓库未就位

**解决**:
```bash
# 确保工具仓库在项目的同级目录
cd ..
git clone <你的 android-ci 仓库地址> android-ci
```

### 🔴 CI 报错: "Refusing to allow... update workflow"

**原因**: 镜像同步的 GitHub PAT 缺少 `workflow` 权限

**解决**: 重新生成 PAT，勾选 `workflow` 权限

### 🔴 CI 报错: Firebase 上传失败

**原因**: `INTERNAL_FIREBASE_CREDENTIAL_FILE_CONTENT` 未配置或格式错误

**解决**: 重新按 [8.1 ⑤](#81-internal-构建-必配) 步骤生成

### 🔴 Official 构建时 `official.gradle` 字段为 null

**原因**: `official.gradle` 缺少某些配置块（Groovy ext 是整体覆盖，不是合并）

**解决**: 确保 `official.gradle` 包含与 `internal.gradle` **完全相同的所有配置块名称和字段**

### 🟡 Actions 页面看不到 Workflow

**原因**: GitHub 默认只展示 `main` 分支的 Workflow

**解决**: 确保包含 `.github/workflows/android_ci.yml` 的代码已合并到 `main` 分支

### 🟡 飞书群没收到通知

**原因**: 飞书 Secrets 未配置（`LARK_APP_ID` / `LARK_APP_SECRET` / `LARK_CHAT_ID`）

**解决**: 飞书通知为可选功能，不影响构建。如需启用请按 [8.3 节](#83-飞书通知-可选) 配置

### 🟡 Tag 已存在无法推送

**原因**: 同版本号的 Tag 已创建过

**解决**:
- Internal: 脚本会自动递增后缀 (A→B→C...)，一般不会重复
- Playstore: 需要先在 `app/build.gradle.kts` 中升级 `versionName`，再运行脚本
