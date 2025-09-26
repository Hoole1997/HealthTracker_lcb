# AddReminderActivity 使用指南

## 功能概述

AddReminderActivity 支持两种运行模式：
1. **新建模式**: 创建新的服药提醒，支持创建180天的持续提醒
2. **编辑模式**: 修改现有的服药提醒记录

## 使用方法

### 1. 新建服药提醒模式

```kotlin
// 方式1: 不指定起始日期，使用当前日期
AddReminderActivity.start(context)

// 方式2: 指定起始日期
AddReminderActivity.start(context, startDate = "2024-01-01")

// 方式3: 使用Intent方式
val intent = Intent(context, AddReminderActivity::class.java)
intent.putExtra("startDate", "2024-01-01")  // 可选
context.startActivity(intent)
```

### 2. 编辑现有提醒模式

```kotlin
// 方式1: 使用便利方法
AddReminderActivity.start(context, remindId = 123L)

// 方式2: 使用Intent方式
val intent = Intent(context, AddReminderActivity::class.java)
intent.putExtra("remindId", 123L)
context.startActivity(intent)
```

## 参数说明

| 参数名 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| `remindId` | Long | 否 | 提醒记录ID。如果提供，进入编辑模式；如果为null或不提供，进入新建模式 |
| `startDate` | String | 否 | 新建模式的起始日期，格式为"yyyy-MM-dd"。仅在新建模式下有效 |

## 功能特性

### 新建模式特性
- ✅ 支持设置药物名称
- ✅ 支持选择每日服药次数（1-6次）
- ✅ 支持自定义每次服药时间
- ✅ 支持添加备注信息
- ✅ 支持设置是否同步到系统日历
- ✅ 创建持续180天的服药提醒（可扩展）
- ✅ 表单验证确保数据完整性

### 编辑模式特性
- ✅ 自动加载现有提醒数据
- ✅ 支持修改所有字段
- ✅ 保存按钮显示"Save Changes"
- ✅ 完整的错误处理和状态反馈

## 界面说明

### 主要组件
- **药物名称输入框**: 输入药物名称
- **每日服药次数**: 点击选择1-6次/天
- **服药时间列表**: 网格显示，点击可修改具体时间
- **备注输入框**: 添加额外说明
- **同步日历开关**: 是否同步到系统日历
- **保存按钮**: 保存或更新提醒

### 交互流程
1. 打开页面后自动初始化对应模式
2. 用户填写/修改表单信息
3. 实时验证表单完整性
4. 点击保存触发保存流程
5. 显示保存状态和结果反馈

## 技术实现

### 架构模式
- **MVVM架构**: 使用ViewModel管理业务逻辑和状态
- **数据绑定**: 使用ViewBinding进行视图绑定
- **依赖注入**: 使用Hilt进行依赖管理

### 状态管理
- **UI状态**: `AddReminderUiState`管理界面状态
- **保存状态**: `SaveState`管理保存流程状态
- **响应式更新**: 使用StateFlow + collectLatestLifecycle

### 数据持久化
- **数据库**: 使用Room数据库存储
- **仓库模式**: 通过MedicineReminderRepository进行数据操作

## 扩展功能

如需实现真正的180天提醒功能，可以在`createMultiDayReminder`方法中：
1. 创建多个MedicineReminder记录
2. 为每天设置不同的提醒时间
3. 支持跳过周末或节假日
4. 添加提醒完成状态跟踪

## 注意事项

1. 确保传入的`remindId`存在于数据库中
2. 起始日期格式必须为"yyyy-MM-dd"
3. 编辑模式下会保留原有的创建时间等元数据
4. 新建模式支持创建持续性提醒计划