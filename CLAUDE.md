# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HealthTracker 是一个基于 Android 的健康追踪应用，采用模块化架构，使用 Kotlin 和 Jetpack Compose 构建。

## Architecture

项目采用模块化架构，并使用 **Convention Plugins** 进行构建配置管理：

### Module Structure
- `app/` - 主应用模块，包含应用特定的代码
- `framework/` - 框架模块，包含通用的基础类和工具
- `monetize/` - 广告变现模块，封装 AdMob 广告相关功能
- `core/` - 核心模块，包含远程配置、数据上报等基础设施
- `build-common/` - 构建配置插件模块，包含自定义 Gradle 插件

### Build System
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

### Core Tech Stack
- Kotlin + Android Gradle Plugin 8.13.0
- Jetpack Compose (BOM 2025.09.00)
- Hilt 依赖注入
- Room 数据库
- Firebase 服务 (Crashlytics, Analytics, Performance)
- MVVM 架构模式

## Base Classes (Framework Module)

项目使用自定义的基础类，位于 `framework` 模块：

### Activity 基础类
- `BaseMVVMActivity<VM, VB>` - 结合了 ViewModel 和 ViewBinding 的基础 Activity
  - 自动处理 ViewModel 注入
  - 统一的 ViewBinding 管理
  - Fragment 管理辅助方法
  - Edge-to-Edge 显示支持
  - 系统栏管理 (状态栏、导航栏)

### Fragment 基础类
- `BaseMVVMFragment<VM, VB>` - Fragment 的 MVVM 基础类
  - ViewModel 和 ViewBinding 自动注入
  - 生命周期状态跟踪
  - 内存泄漏防护
- `BaseDialogFragment` - 对话框基础类
- `BaseVbDialogFragment` - 使用 ViewBinding 的对话框基础类
- `BaseBottomSheetDialogFragment` - 底部弹窗基础类

### ViewModel 基础类
- `BaseViewModel` - ViewModel 基础类

### Architecture Pattern
项目严格采用 MVVM 架构：
- **Model**: Room 数据库实体 + Repository 模式
- **View**: Activity/Fragment + ViewBinding/Compose
- **ViewModel**: 继承自 BaseViewModel，使用 Hilt 注入
- **数据流**: StateFlow/LiveData 用于状态管理

## Development Commands

### Build Commands
```bash
# 构建 debug 版本
./gradlew assembleDebug

# 构建 release 版本
./gradlew assembleRelease

# 清理项目
./gradlew clean

# 构建特定 variant
./gradlew assembleInternalDebug
./gradlew assemblePlaystoreRelease
```

### Installation Commands
```bash
# 安装 debug 版本到设备
./gradlew installDebug

# 安装特定 variant
./gradlew installInternalDebug

# 卸载应用
./gradlew uninstallDebug
```

### Testing Commands
```bash
# 运行所有单元测试
./gradlew test

# 运行特定 variant 的单元测试
./gradlew testDebugUnitTest

# 运行特定测试类
./gradlew test --tests "com.healthtracker.blood.suger.viewmodel.AlarmViewModelTest"

# 运行设备测试
./gradlew connectedDebugAndroidTest
```

### Code Quality Commands
```bash
# 运行 lint 检查
./gradlew lint

# 运行 lint 并修复可修复的问题
./gradlew lintFix

# 运行所有检查 (包括 lint 和测试)
./gradlew check
```

## Project Configuration

### Build Configuration
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

### Important Configuration Files
- `gradle/libs.versions.toml` - 版本目录，管理所有依赖版本
- `build-common/` - 自定义 Gradle 插件
- `app/src/internal/config.gradle` - 内部测试版本配置 (包含测试 AdMob ID)
- `app/src/playstore/config.gradle` - 正式发布版本配置
- `app/proguard-rules.pro` - 混淆规则
- `scripts/sign.gradle` - 签名配置脚本

### SDK Configuration
- 最小 SDK: 24
- 目标 SDK: 35
- 编译 SDK: 35
- Java 版本: 11

### Build Variants
项目支持多个构建变种：
- **Internal** - 内部测试版本 (`com.healthtracker.blood.suger.internal`)
- **Playstore** - 正式发布版本 (`com.healthtracker.blood.suger`)

构建系统会根据任务名称自动选择配置文件：
- 包含 "Playstore" 的任务使用 `app/src/playstore/config.gradle`
- 包含 "Internal" 的任务使用 `app/src/internal/config.gradle`
- 默认使用内部测试配置

### Special Features
- StringFog 字符串混淆 (仅在 release 版本启用)
- Firebase 服务集成
- 多进程支持 (主进程检查逻辑在 App.kt)
- AdMob 广告集成 (测试 ID 在内部版本，实际 ID 在正式版本)

## Code Conventions

### Package Naming
- 主包: `com.healthtracker.blood.suger`
- 框架包: `com.healthtracker.framework`
- 广告模块包: `net.corekit.monetize`
- 核心模块包: `net.corekit.core`

### UI Architecture
- Compose UI 使用 Material3 设计
- ViewBinding 与传统 View 系统并用
- 支持 Edge-to-Edge 显示

### Data Layer Architecture
**Database**:
- 使用 Room 数据库，schema 文件位于 `app/schemas/`
- 主要实体类:
  - `BloodSugarRecord` - 血糖记录
  - `BloodPressureRecord` - 血压记录
  - `BmiRecord` - BMI 记录
  - `HeartRateRecord` - 心率记录
  - `CholesterolRecord` - 胆固醇记录
  - `MedicineReminder` - 药物提醒
  - `AlarmRecord` - 闹钟记录
  - `HealthTag` - 健康标签

**Repository Pattern**:
- `BaseRepository<T>` - 通用 Repository 基类
- `BaseTagRepository<T>` - 带标签支持的 Repository 基类
- 所有数据操作通过 Repository 层进行，不直接在 ViewModel 中访问 DAO

**Dependency Injection**:
- 使用 Hilt，模块定义在 `app/src/main/java/com/healthtracker/blood/suger/di/`
- 主要 Hilt 模块:
  - `AppModule` - 应用级别依赖
  - `DatabaseModule` - 数据库相关依赖
  - `RepositoryModule` - Repository 依赖

### Preferences
- SharedPreferences 使用 MMKV 替代
- 远程配置通过 `ConfigRemoteManager` 管理 (core 模块)

## Performance Optimizations

项目集成了多个性能优化工具：
- WebView 预加载优化 (`WebViewZygote`)
- SP 阻塞主线程处理 (spwaitkiller)
- 隐藏 API 绕过 (hiddenapibypass)
- 应用进程生命周期管理 (`AppLifecycleManager`)

## Firebase Integration

- 使用 Firebase BOM 33.10.0
- 集成 Crashlytics 崩溃报告
- Analytics 数据分析
- Performance 性能监控
- Remote Config 远程配置

## Monetization Module

`monetize` 模块封装了广告相关功能：
- **广告类型支持**:
  - `LaunchAds` - 启动广告
  - `InterstitialAds` - 插屏广告
  - `BannerAds` - 横幅广告
  - `NativeAds` - 原生广告
  - `FullNativeAds` - 全屏原生广告
  - `RewardedAds` - 激励视频广告

- **广告管理**:
  - `AdsManager` - 广告管理器，统一管理所有广告类型
  - `AdConfigManager` - 广告配置管理
  - `PreloadController` - 广告预加载控制

- **UI 组件**:
  - `BannerAdView` - 横幅广告视图
  - `NativeAdView` - 原生广告视图
  - `FullScreenNativeAdView` - 全屏原生广告视图

## Core Module

`core` 模块提供基础设施功能：
- **远程配置**: `ConfigRemoteManager` - 统一管理远程配置，根据构建状态调整请求间隔
- **数据上报**: `RevenueAdReporter` - 广告收入上报
- **用户管理**: `ChannelUserController` - 渠道用户管理
- **权限扩展**: `NotificationPermissionExt` - 通知权限处理

## Important Development Notes

### Build Requirements
- 项目使用 Gradle 配置缓存 (`org.gradle.configuration-cache=true`)
- JVM 堆内存设置为 8GB (`org.gradle.jvmargs=-Xmx8g`)
- 需要 Kotlin 2.1.0+ 和 AGP 8.13.0+

### Debug and Monitoring Tools
项目集成了完整的调试和监控工具链：
- **内存泄漏**: LeakCanary (仅 Debug 版本)
- **ANR 监控**: ANR-WatchDog
- **主线程阻塞**: BlockCanary (Debug/Release 不同实现)
- **SP 优化**: MMKV 替代 SharedPreferences
- **隐藏 API**: hiddenapibypass 绕过系统限制

### Version Management
- 所有依赖版本统一在 `gradle/libs.versions.toml` 管理
- 使用版本目录 (Version Catalog) 避免版本冲突
- 支持 Compose BOM 统一管理 Compose 组件版本

### Multi-Process Support
- 应用支持多进程架构
- `App.kt` 中的 `isMainProcess()` 方法用于检查是否为主进程
- 只在主进程中进行完整的应用初始化，避免重复初始化导致的问题

### Alarm and Reminder System
- 使用 `AlarmScheduler` 管理系统闹钟
- `PermissionManager` 处理通知权限
- 支持药物提醒和自定义闹钟功能
- 使用 `AlarmRepeatHelper` 处理重复闹钟逻辑

### Custom Views
项目包含多个自定义 View 组件：
- `WeeklyDateSelector` - 周日期选择器（支持导航限制）
- `DateTimePicker` - 日期时间选择器
- `RulerView` - 刻度尺视图
- `GenericLevelBar` - 通用等级条
- `GenericStatusView` - 通用状态视图
- `BloodSugarRangeView` - 血糖范围视图

### StringFog Configuration
字符串混淆配置支持多层级控制：
1. 环境变量控制 (`STRINGFOG_ENABLED`)
2. Gradle 属性控制
3. 配置文件控制 (`config.properties`)
4. 构建类型控制（默认仅 release 启用）

配置示例：
```properties
# config.properties
stable_release=true
stringfog.packages=com.healthtracker.blood.suger,com.healthtracker.framework
stringfog.exclude=androidx.,kotlin.,kotlinx.,com.google.
stringfog.mode=bytes
stringfog.key=myCustomKey123
```
