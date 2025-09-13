# Android Convention Plugins

## 📋 概述

这是一套通用的 Android Gradle 构建插件集合，旨在简化和标准化 Android 项目的构建配置。通过使用这些插件，可以显著减少模块间的重复配置代码，提高项目的可维护性。

## 🎯 设计理念

- **简洁性**：使用极简的插件命名，易于理解和记忆
- **通用性**：不包含任何产品特定信息，可应用于任何 Android 项目
- **模块化**：每个插件负责特定的功能配置，可按需组合使用
- **标准化**：遵循 Android 开发最佳实践，确保配置的一致性

## 📦 插件列表

| 插件 ID | 功能说明 | 适用场景 |
|---------|---------|---------|
| `android.app` | Android 应用基础配置 | 主应用模块 |
| `android.library` | Android 库基础配置 | 功能库模块 |
| `android.compose` | Jetpack Compose 配置 | 使用 Compose UI 的模块 |
| `android.hilt` | Hilt 依赖注入配置 | 需要依赖注入的模块 |
| `android.room` | Room 数据库配置 | 使用数据库的模块 |
| `android.firebase` | Firebase 服务配置 | 集成 Firebase 的模块 |

## 🚀 快速开始

### 1. 集成插件

在项目根目录的 `settings.gradle.kts` 中添加：

```kotlin
pluginManagement {
    includeBuild("build-common")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

### 2. 配置版本目录

在 `gradle/libs.versions.toml` 中添加插件定义：

```toml
[plugins]
android-app = { id = "android.app", version = "unspecified" }
android-library-convention = { id = "android.library", version = "unspecified" }
android-compose-convention = { id = "android.compose", version = "unspecified" }
android-hilt-convention = { id = "android.hilt", version = "unspecified" }
android-room-convention = { id = "android.room", version = "unspecified" }
android-firebase-convention = { id = "android.firebase", version = "unspecified" }
```

### 3. 在模块中使用

#### 应用模块 (app/build.gradle.kts)

```kotlin
plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.android.hilt.convention)
    alias(libs.plugins.android.room.convention)
    alias(libs.plugins.android.firebase.convention)
}

android {
    namespace = "com.example.app"

    defaultConfig {
        applicationId = "com.example.app"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    // 添加模块特定依赖
}
```

#### 库模块 (library/build.gradle.kts)

```kotlin
plugins {
    alias(libs.plugins.android.library.convention)
    alias(libs.plugins.android.compose.convention)
}

android {
    namespace = "com.example.library"
}

dependencies {
    // 添加模块特定依赖
}
```

## 📝 插件详细说明

### android.app

**功能**：
- 配置 Android 应用的基础设置
- 设置编译 SDK、最小 SDK、目标 SDK
- 配置 Java/Kotlin 编译选项
- 启用 ViewBinding 和 BuildConfig
- 配置 MultiDex
- 添加测试依赖

**自动配置**：
```kotlin
compileSdk = 35
minSdk = 24
targetSdk = 35
JavaVersion = 11
JvmTarget = JVM_11
multiDexEnabled = true
viewBinding = true
buildConfig = true
```

### android.library

**功能**：
- 配置 Android 库的基础设置
- 设置编译选项和版本
- 启用 ViewBinding
- 配置 ProGuard 规则

**自动配置**：
```kotlin
compileSdk = 35
minSdk = 24
JavaVersion = 11
JvmTarget = JVM_11
viewBinding = true
consumerProguardFiles = "consumer-rules.pro"
```

### android.compose

**功能**：
- 启用 Compose 编译器
- 添加 Compose BOM 依赖
- 添加 Compose UI 核心库
- 添加 Material3 组件
- 配置 Compose 预览工具

**自动添加的依赖**：
- androidx.compose:compose-bom
- androidx.compose.ui:ui
- androidx.compose.ui:ui-graphics
- androidx.compose.ui:ui-tooling-preview
- androidx.compose.material3:material3
- androidx.activity:activity-compose
- androidx.lifecycle:lifecycle-viewmodel-compose

### android.hilt

**功能**：
- 应用 Hilt 插件
- 配置 KSP 处理器
- 添加 Hilt 依赖

**自动添加的依赖**：
- com.google.dagger:hilt-android
- com.google.dagger:hilt-compiler (KSP)

### android.room

**功能**：
- 配置 Room 数据库
- 设置 Schema 导出目录
- 添加 Room 依赖

**自动配置**：
```kotlin
room.schemaDirectory = "$projectDir/schemas"
```

**自动添加的依赖**：
- androidx.room:room-runtime
- androidx.room:room-ktx
- androidx.room:room-compiler (KSP)

### android.firebase

**功能**：
- 应用 Google Services 插件
- 应用 Firebase Crashlytics 插件
- 添加 Firebase BOM
- 配置 Firebase 核心服务

**自动添加的依赖**：
- com.google.firebase:firebase-bom (BOM)
- com.google.firebase:firebase-config
- com.google.firebase:firebase-analytics-ktx
- com.google.firebase:firebase-crashlytics-ktx
- com.google.firebase:firebase-perf-ktx

## 🔧 自定义配置

虽然插件提供了标准配置，但你仍然可以在模块的 `build.gradle.kts` 中覆盖或扩展配置：

```kotlin
android {
    // 覆盖默认配置
    defaultConfig {
        minSdk = 26  // 覆盖默认的 minSdk
    }

    // 添加额外配置
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## 📂 项目结构

```
build-common/
├── build.gradle.kts                     # 插件模块配置
├── settings.gradle.kts                  # 插件模块设置
└── src/main/kotlin/
    └── convention/plugins/
        ├── AndroidKotlinAndroid.kt      # 通用配置工具
        ├── AndroidAppConventionPlugin.kt    # 应用插件
        ├── AndroidLibraryConventionPlugin.kt # 库插件
        ├── AndroidComposeConventionPlugin.kt # Compose 插件
        ├── AndroidHiltConventionPlugin.kt    # Hilt 插件
        ├── AndroidRoomConventionPlugin.kt    # Room 插件
        └── AndroidFirebaseConventionPlugin.kt # Firebase 插件
```

## 🛠️ 维护指南

### 添加新插件

1. 在 `build-common/src/main/kotlin/convention/plugins/` 创建新的插件类
2. 在 `build-common/build.gradle.kts` 的 `gradlePlugin` 块中注册
3. 在 `gradle/libs.versions.toml` 中添加插件定义
4. 更新本文档

### 修改现有配置

1. 找到对应的插件类文件
2. 修改配置逻辑
3. 运行 `./gradlew clean build` 测试
4. 更新受影响的模块配置

## ⚠️ 注意事项

1. **插件顺序**：某些插件有依赖关系，如 `android.compose` 需要在 `android.app` 或 `android.library` 之后应用
2. **版本同步**：SDK 版本通过 `libs.versions.toml` 统一管理，修改时需同步更新
3. **清理缓存**：修改插件后建议执行 `./gradlew clean` 清理缓存
4. **兼容性**：插件基于 AGP 8.13.0 和 Kotlin 2.1.0 开发，使用时注意版本兼容

## 📚 参考资源

- [Android Gradle Plugin 文档](https://developer.android.com/studio/build)
- [Kotlin DSL 指南](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Version Catalog](https://docs.gradle.org/current/userguide/platforms.html)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进这些插件。在提交 PR 前，请确保：

1. 代码遵循现有的编码风格
2. 所有测试通过
3. 更新相关文档