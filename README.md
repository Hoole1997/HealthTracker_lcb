# HealthTracker

## 项目概述

HealthTracker 是一个基于 Android 的健康追踪应用，使用现代化的 Android 开发技术栈构建。

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **架构**: MVVM + Repository Pattern
- **依赖注入**: Hilt
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **图片加载**: Glide
- **构建系统**: Gradle with Convention Plugins

## 项目结构

```
HealthTracker/
├── app/                    # 主应用模块
├── framework/              # 框架模块
├── build-common/           # 自定义 Gradle 插件
├── config/                 # 配置文件
└── gradle/                 # Gradle 配置
```

## 构建系统优化

### Convention Plugins

项目使用自定义的 Convention Plugins 来简化模块配置：

- `android.app` - 应用模块配置
- `android.library` - 库模块配置
- `android.compose` - Compose UI 配置
- `android.hilt` - Hilt 依赖注入配置
- `android.room` - Room 数据库配置
- `android.firebase` - Firebase 服务配置
- `android.stringfog` - StringFog 字符串混淆配置

### StringFog 字符串混淆优化

#### 优化内容

1. **多层级配置控制**
   - 环境变量控制（最高优先级）
   - Gradle 属性控制
   - 配置文件控制
   - 构建类型控制（默认）

2. **智能包路径检测**
   - 自动检测主包和框架包
   - 支持自定义包路径配置
   - 智能排除第三方库包

3. **灵活的排除规则**
   - 默认排除 AndroidX、Kotlin 等系统库
   - 支持自定义排除包路径
   - 避免混淆第三方库

4. **自定义密钥支持**
   - 支持固定密钥或随机密钥生成
   - 密钥长度验证
   - 安全建议

5. **构建时验证**
   - 详细的配置验证
   - 错误提示和建议
   - 配置冲突检测

6. **详细日志输出**
   - 构建时显示完整配置信息
   - 便于调试和验证

#### 配置方式

```properties
# config.properties
stable_release=true
stringfog.packages=com.healthtracker.blood.suger,com.healthtracker.framework
stringfog.exclude=androidx.,kotlin.,kotlinx.,com.google.
stringfog.mode=bytes
stringfog.key=myCustomKey123
```

#### 环境变量控制

```bash
# 启用 StringFog
export STRINGFOG_ENABLED=true

# 禁用 StringFog
export STRINGFOG_ENABLED=false
```

## 构建指南

### 开发环境构建

```bash
./gradlew assembleDebug
```

### 生产环境构建

```bash
./gradlew assembleRelease
```

### 安装到设备

```bash
./gradlew installDebug
```

## 配置说明

### 应用配置

主要配置文件位于 `app/assets/config.properties`：

- `stable_release` - 控制是否为稳定发布版本
- `stringfog.*` - StringFog 相关配置

### 签名配置

签名配置位于 `config/` 目录：
- `pdfreader.jks` - 签名文件
- `sign.properties` - 签名属性
- `sign.gradle` - 签名配置脚本

## 性能优化

项目集成了多个性能优化工具：

- **WebView 预加载优化**
- **SP 阻塞主线程处理** (spwaitkiller)
- **隐藏 API 绕过** (hiddenapibypass)
- **应用进程生命周期管理**
- **StringFog 字符串混淆** (仅 release 版本)

## 开发规范

### 包命名

- 主包: `com.healthtracker.blood.suger`
- 框架包: `com.healthtracker.framework`

### 代码约定

- 使用 Kotlin 作为主要开发语言
- 遵循 Android 官方开发指南
- 使用 Material3 设计规范
- 支持 Edge-to-Edge 显示

## 最近更新

### StringFog 插件优化 (2024-12-19)

- ✅ 重构 StringFog 配置逻辑，提高灵活性和可维护性
- ✅ 添加多层级配置控制（环境变量、Gradle属性、配置文件）
- ✅ 实现智能包路径检测和排除规则
- ✅ 优化密钥生成和加密模式配置
- ✅ 添加构建时配置验证和错误处理
- ✅ 创建详细的配置文档和使用指南

### 主要改进

1. **配置灵活性**: 支持多种配置方式，优先级明确
2. **智能检测**: 自动检测包路径，减少手动配置
3. **错误处理**: 完善的错误提示和验证机制
4. **文档完善**: 提供详细的配置指南和示例
5. **向后兼容**: 保持与现有配置的兼容性

## 许可证

本项目采用私有许可证，仅供内部使用。

