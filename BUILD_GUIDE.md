# 项目构建配置指南

## 🏗️ 构建架构概述

本项目采用了基于 **Convention Plugins** 的现代化 Gradle 构建架构，通过自定义插件实现了构建配置的标准化和模块化。

### 核心特点

- ✅ **配置集中化**：通过自定义插件统一管理构建配置
- ✅ **版本统一管理**：使用 Version Catalog 管理所有依赖版本
- ✅ **代码复用最大化**：消除模块间的重复配置
- ✅ **易于扩展**：新增模块只需几行配置

## 📁 项目结构

```
HealthTracker/
├── build-common/           # 🔧 构建配置插件模块
│   ├── README.md          # 插件使用说明
│   └── src/               # 插件源代码
├── app/                   # 📱 主应用模块
├── framework/             # 📚 框架库模块
├── gradle/
│   └── libs.versions.toml # 📦 版本目录
├── build.gradle.kts       # 根项目配置
└── settings.gradle.kts    # 项目设置
```

## 🚀 快速使用指南

### 1. 创建新的应用模块

```kotlin
// new-app/build.gradle.kts
plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.android.hilt.convention)
}

android {
    namespace = "com.example.newapp"

    defaultConfig {
        applicationId = "com.example.newapp"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":framework"))
    // 其他依赖...
}
```

### 2. 创建新的库模块

```kotlin
// new-library/build.gradle.kts
plugins {
    alias(libs.plugins.android.library.convention)
}

android {
    namespace = "com.example.library"
}

dependencies {
    // 库依赖...
}
```

### 3. 创建纯 Kotlin 模块

```kotlin
// domain/build.gradle.kts
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    // 其他依赖...
}
```

## 📦 依赖管理

### Version Catalog 使用

所有依赖版本都在 `gradle/libs.versions.toml` 中集中管理：

```toml
[versions]
kotlin = "2.1.0"
compose-bom = "2025.09.00"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }

[plugins]
android-app = { id = "android.app", version = "unspecified" }
```

### 在模块中使用依赖

```kotlin
dependencies {
    // 使用 Version Catalog 中定义的依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // 使用 BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)

    // 项目间依赖
    implementation(project(":framework"))
}
```

## 🔧 常用构建命令

```bash
# 清理项目
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行所有测试
./gradlew test

# 检查代码
./gradlew lint

# 安装到设备
./gradlew installDebug

# 查看依赖树
./gradlew app:dependencies

# 更新依赖版本
./gradlew dependencyUpdates
```

## 🎯 最佳实践

### 1. 模块化设计

- **app**: 主应用入口，包含 UI 和应用特定逻辑
- **framework**: 通用框架和工具类
- **feature**: 功能模块（按需创建）
- **domain**: 业务逻辑（纯 Kotlin）
- **data**: 数据层实现

### 2. 插件组合策略

#### 标准应用模块
```kotlin
plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.android.hilt.convention)
    alias(libs.plugins.android.room.convention)
}
```

#### UI 库模块
```kotlin
plugins {
    alias(libs.plugins.android.library.convention)
    alias(libs.plugins.android.compose.convention)
}
```

#### 数据库模块
```kotlin
plugins {
    alias(libs.plugins.android.library.convention)
    alias(libs.plugins.android.room.convention)
    alias(libs.plugins.android.hilt.convention)
}
```

### 3. 配置覆盖

虽然插件提供了默认配置，但仍可以按需覆盖：

```kotlin
android {
    // 覆盖默认的 minSdk
    defaultConfig {
        minSdk = 26
    }

    // 添加自定义构建类型
    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
}
```

## 🐛 常见问题

### Q: 如何添加新的自定义插件？

1. 在 `build-common/src/main/kotlin/convention/plugins/` 创建插件类
2. 在 `build-common/build.gradle.kts` 注册插件
3. 在 `libs.versions.toml` 添加插件定义
4. 重新同步项目

### Q: 如何更新 SDK 版本？

编辑 `gradle/libs.versions.toml`:
```toml
[versions]
compileSdk = "35"  # 修改这里
minSdk = "24"       # 修改这里
targetSdk = "35"    # 修改这里
```

### Q: 构建失败怎么办？

```bash
# 1. 清理缓存
./gradlew clean

# 2. 刷新依赖
./gradlew --refresh-dependencies

# 3. 停止 Gradle 守护进程
./gradlew --stop

# 4. 清理 .gradle 缓存（最后手段）
rm -rf ~/.gradle/caches/
```

### Q: 如何查看插件实际应用的配置？

```bash
# 查看应用的插件
./gradlew :app:buildEnvironment

# 查看实际配置
./gradlew :app:properties
```

## 📊 性能优化建议

### 1. 并行构建
```properties
# gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
```

### 2. 增加内存
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
```

### 3. 配置缓存
```properties
# gradle.properties
org.gradle.configuration-cache=true
```

## 🔄 迁移指南

### 从传统配置迁移到 Convention Plugins

#### Before (传统方式):
```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    // ... 很多依赖
}
```

#### After (Convention Plugins):
```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
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
    // 大部分依赖由插件自动添加
    // 只需添加模块特定的依赖
}
```

## 🔗 相关资源

- [build-common 插件详细文档](./build-common/README.md)
- [Android 官方文档](https://developer.android.com/studio/build)
- [Gradle 文档](https://docs.gradle.org/current/userguide/userguide.html)
- [Kotlin DSL 指南](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

## 📝 更新日志

### v1.0.0 (2024-09-13)
- 初始化 Convention Plugins 架构
- 实现 6 个核心插件
- 完成项目模块化重构
- 添加完整文档

---

💡 **提示**: 遇到问题请先查看本文档和 [build-common/README.md](./build-common/README.md)，如果还有疑问，请提交 Issue。