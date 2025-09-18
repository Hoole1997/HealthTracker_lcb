# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HealthTracker 是一个基于 Android 的健康追踪应用，采用模块化架构，使用 Kotlin 和 Jetpack Compose 构建。

## 架构

项目采用模块化架构，并使用 **Convention Plugins** 进行构建配置管理：

### 模块结构
- `app/` - 主应用模块，包含应用特定的代码
- `framework/` - 框架模块，包含通用的基础类和工具
- `build-common/` - 构建配置插件模块，包含自定义 Gradle 插件

### 构建系统
项目使用自定义的 Convention Plugins 来管理构建配置：
- `android.app` - Android 应用基础配置
- `android.library` - Android 库基础配置
- `android.compose` - Jetpack Compose 配置
- `android.hilt` - Hilt 依赖注入配置
- `android.room` - Room 数据库配置
- `android.firebase` - Firebase 服务配置

详细的构建配置说明请参考：
- [构建配置指南](./BUILD_GUIDE.md)
- [插件使用说明](./build-common/README.md)

### 核心技术栈
- Kotlin + Android Gradle Plugin 8.13.0
- Jetpack Compose (BOM 2025.09.00)
- Hilt 依赖注入
- Room 数据库
- Firebase 服务 (Crashlytics, Analytics, Performance)
- MVVM 架构模式

## 基础架构类

项目使用自定义的基础类：

### Activity 基础类
- `BaseMVVMActivity<VM, VB>` - 结合了 ViewModel 和 ViewBinding 的基础 Activity
  - 自动处理 ViewModel 注入
  - 统一的 ViewBinding 管理
  - Fragment 管理辅助方法

### Fragment 基础类
- `BaseMVVMFragment<VM, VB>` - Fragment 的 MVVM 基础类
  - ViewModel 和 ViewBinding 自动注入
  - 生命周期状态跟踪
  - 内存泄漏防护

### ViewModel 基础类
- `BaseViewModel` - ViewModel 基础类

## 开发命令

### 构建命令
```bash
# 构建 debug 版本
./gradlew assembleDebug

# 构建 release 版本
./gradlew assembleRelease

# 清理项目
./gradlew clean
```

### 安装命令
```bash
# 安装 debug 版本到设备
./gradlew installDebug

# 卸载应用
./gradlew uninstallDebug
```

## 项目配置

### 构建配置
项目使用 Convention Plugins 简化模块配置：

#### 新建应用模块示例：
```kotlin
plugins {
    alias(libs.plugins.android.app)
    alias(libs.plugins.android.compose.convention)
    alias(libs.plugins.android.hilt.convention)
}

android {
    namespace = "com.healthtracker.module"
    defaultConfig {
        applicationId = "com.healthtracker.module"
        versionCode = 1
        versionName = "1.0"
    }
}
```

#### 新建库模块示例：
```kotlin
plugins {
    alias(libs.plugins.android.library.convention)
    alias(libs.plugins.android.compose.convention)
}

android {
    namespace = "com.healthtracker.library"
}
```

### 重要配置文件
- `gradle/libs.versions.toml` - 版本目录，管理所有依赖版本
- `build-common/` - 自定义 Gradle 插件
- `app/assets/config.properties` - 应用配置文件，控制发布状态
- `app/proguard-rules.pro` - 混淆规则

### SDK 配置
- 最小 SDK: 24
- 目标 SDK: 35
- 编译 SDK: 35
- Java 版本: 11

### 特殊功能
- StringFog 字符串混淆 (仅在 release 版本启用)
- Firebase 服务集成
- 多进程支持 (主进程检查逻辑在 App.kt)

## 代码约定

### 包命名
- 主包: `com.healthtracker.blood.suger`
- 框架包: `com.healthtracker.framework`

### UI 层级
- Compose UI 使用 Material3 设计
- ViewBinding 与传统 View 系统并用
- 支持 Edge-to-Edge 显示

## 数据管理

- 使用 Room 数据库，schema 文件位于 `app/schemas/`
- 依赖注入使用 Hilt，模块定义在 `app/src/main/java/com/healthtracker/blood/suger/di/`
- SharedPreferences 使用 MMKV 替代

## 性能优化

项目集成了多个性能优化工具：
- WebView 预加载优化
- SP 阻塞主线程处理 (spwaitkiller)
- 隐藏 API 绕过 (hiddenapibypass)
- 应用进程生命周期管理

## Firebase 集成

- 使用 Firebase BOM 33.10.0
- 集成 Crashlytics 崩溃报告
- Analytics 数据分析
- Performance 性能监控
- Remote Config 远程配置