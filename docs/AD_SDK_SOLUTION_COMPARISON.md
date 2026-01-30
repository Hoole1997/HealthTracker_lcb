# 广告SDK封装方案对比与关联风险评估

> **文档版本**: v1.0  
> **更新日期**: 2026-01-30  
> **适用范围**: 团队广告SDK架构决策参考

---

## 📋 目录

1. [背景与问题](#背景与问题)
2. [方案对比](#方案对比)
3. [关联风险评估](#关联风险评估)
4. [风险规避方案](#风险规避方案)
5. [推荐方案](#推荐方案)
6. [实施路线](#实施路线)

---

## 背景与问题

### 当前现状

团队内各产品**独立封装广告库**，存在以下问题：

| 问题类型 | 具体表现 |
|---------|---------|
| **重复开发成本** | 相同功能（竞价、频控、缓存等）多次实现 |
| **质量参差不齐** | 开发者水平差异、AI提示词不同导致实现质量不一 |
| **测试成本高** | 每套代码独立测试，测试人员学习曲线陡峭 |
| **配置混乱** | 无统一规范，配置人员学习成本高 |
| **Bug修复慢** | 同一问题需多处修复，易遗漏 |
| **知识难沉淀** | 优化经验分散，无法系统性积累 |

### 期望目标

- 团队只维护**一个统一广告聚合SDK**
- 统一配置规则和策略逻辑
- 专人负责维护，不同产品只接入这一个SDK
- 配置由专人统一管理，减少配置错误

### 核心担忧

- Google Play账号关联风险

---

## 方案对比

### 方案A：各产品独立封装

```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  产品A团队   │  │  产品B团队   │  │  产品C团队   │
│  独立封装    │  │  独立封装    │  │  独立封装    │
│  广告SDK    │  │  广告SDK    │  │  广告SDK    │
└─────────────┘  └─────────────┘  └─────────────┘
       ↓               ↓               ↓
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   APK A     │  │   APK B     │  │   APK C     │
│  代码差异化  │  │  代码差异化  │  │  代码差异化  │
└─────────────┘  └─────────────┘  └─────────────┘
```

#### 优点

| 优势 | 说明 |
|------|------|
| ✅ 代码差异化 | 每产品广告代码独立实现，代码指纹天然不同 |
| ✅ 隔离性强 | 单产品问题不影响其他产品 |
| ✅ 灵活性高 | 可针对特定产品做定制优化 |
| ✅ 关联风险低 | 代码层面无直接关联证据 |

#### 缺点

| 劣势 | 说明 | 影响程度 |
|------|------|---------|
| ❌ 重复开发成本 | 相同功能多次实现 | 🔴 高 |
| ❌ 质量参差 | 实现水平不一致 | 🔴 高 |
| ❌ 测试成本高 | 每套代码独立测试 | 🔴 高 |
| ❌ 配置混乱 | 无统一规范 | 🟡 中 |
| ❌ Bug修复慢 | 多处修复易遗漏 | 🔴 高 |
| ❌ 知识难沉淀 | 优化经验分散 | 🟡 中 |

#### 适用场景

- 账号高度敏感（有被封历史）
- 团队分布极度分散，协作困难
- 各产品广告策略差异巨大

---

### 方案B：团队统一SDK

```
                ┌─────────────────┐
                │   统一广告SDK    │
                │   专人维护      │
                └────────┬────────┘
                         │
        ┌────────────────┼────────────────┐
        ↓                ↓                ↓
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   产品A     │  │   产品B     │  │   产品C     │
│   接入SDK   │  │   接入SDK   │  │   接入SDK   │
└─────────────┘  └─────────────┘  └─────────────┘
        ↓                ↓                ↓
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   APK A     │  │   APK B     │  │   APK C     │
│  差异化构建  │  │  差异化构建  │  │  差异化构建  │
└─────────────┘  └─────────────┘  └─────────────┘
```

#### 优点

| 优势 | 说明 | 收益程度 |
|------|------|---------|
| ✅ 开发效率高 | 一次开发，多产品复用 | 🟢 高 |
| ✅ 质量可控 | 专人维护，代码质量有保障 | 🟢 高 |
| ✅ 配置标准化 | 统一规则，降低学习成本 | 🟢 高 |
| ✅ 快速迭代 | 新功能/Bug修复一处搞定 | 🟢 高 |
| ✅ 经验沉淀 | 竞价策略、频控逻辑持续优化 | 🟢 高 |
| ✅ 降低出错 | 配置人员只需学一套规则 | 🟢 高 |

#### 缺点

| 劣势 | 说明 | 可解决性 |
|------|------|---------|
| ⚠️ 代码关联风险 | 主要担忧点 | ✅ 可通过差异化构建解决 |
| ⚠️ 单点故障 | SDK有bug影响所有产品 | ✅ 完善测试+灰度发布 |
| ⚠️ 版本协调 | 多产品升级需协调 | ✅ 版本管理策略 |
| ⚠️ 灵活性降低 | 特殊需求需SDK改动 | ✅ 预留扩展点 |

#### 适用场景

- 团队协作良好
- 希望降低重复开发成本
- 追求配置标准化和质量统一

---

### 方案对比总结

| 维度 | 方案A：独立封装 | 方案B：统一SDK |
|------|----------------|---------------|
| 开发成本 | ❌ 高（重复N次） | ✅ 低（一次开发） |
| 维护成本 | ❌ 高（N套代码） | ✅ 低（一套代码） |
| 测试成本 | ❌ 高（N套测试） | ✅ 低（统一测试） |
| 配置学习成本 | ❌ 高（规则不统一） | ✅ 低（统一规则） |
| Bug修复效率 | ❌ 低（逐个修复） | ✅ 高（一处修复） |
| 代码相似度 | ✅ 低 | ⚠️ 需差异化处理 |
| 关联风险 | ✅ 低 | ⚠️ 可控（见下文） |
| 团队协作要求 | ✅ 低 | ⚠️ 需要协调 |

---

## 关联风险评估

### Google Play关联检测维度

| 检测维度 | 权重 | 与统一SDK关系 | 风险等级 |
|---------|------|--------------|---------|
| **开发者信息** | 🔴 高 | ❌ 无关（SDK不含开发者信息） | ✅ 无风险 |
| **签名证书** | 🔴 高 | ❌ 无关（签名在APK打包时进行） | ✅ 无风险 |
| **后端域名** | 🟡 中 | ❌ 无关（配置走Firebase/构建注入） | ✅ 无风险 |
| **代码指纹** | 🟡 中 | ⚠️ 相关（需差异化处理） | ⚠️ 可控 |
| **SDK指纹** | 🟢 低 | ❌ 无关（第三方SDK全球共用） | ✅ 无风险 |
| **广告位ID** | 🟢 低 | ❌ 无关（每产品独立ID） | ✅ 无风险 |

### 关键结论

#### 统一SDK本身不是关联的主要因素

1. **第三方SDK普遍性**
   - AdMob、Pangle、TopOn等SDK全球数百万App使用
   - Google不会因使用相同第三方SDK判定关联

2. **真正的关联信号**
   - 相同开发者账户信息
   - 相同签名证书
   - 相同后端服务器域名
   - 相同用户隐私政策链接
   - 相同客服邮箱
   - 设备指纹采集代码

3. **你们的现状优势**
   - SDK无开发者信息 → ✅ 无风险
   - 各产品独立签名 → ✅ 无风险
   - 无后端域名（Firebase + 构建注入） → ✅ 无风险
   - 已有混淆+垃圾代码机制 → ⚠️ 可进一步优化

### 代码指纹相似度分析

#### 混淆的作用与局限

| 场景 | R8/ProGuard效果 |
|------|----------------|
| 类名/方法名 | ✅ 有效，变成 `a.b.c()` |
| 方法体逻辑结构 | ❌ 不变，字节码指令序列相同 |
| 控制流图 | ❌ 不变，if/for/while结构保留 |
| 字符串常量 | ⚠️ 需StringFog额外处理 |

```
// 示例：混淆后两个App的相同方法
// App A
public final void a(Context p0) {
    invoke-virtual p0.getPackageName()
    const-string "ad_config"
}

// App B（相同逻辑，不同混淆名）
public final void b(Context p0) {
    invoke-virtual p0.getPackageName()  // 相同指令
    const-string "ad_config"            // 相同字符串
}
```

→ **结论**：混淆改名有帮助，但需配合其他手段打破字节码相似性。

#### SDK代码在APK中的占比

| 组成部分 | 大小估算 | 占APK比例 |
|---------|---------|----------|
| 自研广告封装代码 | ~300-400KB DEX | **3-5%** |
| 第三方广告SDK | 2-4MB | 30-50% |
| 业务代码 + 其他SDK | 3-5MB | 40-60% |

→ **结论**：自研封装代码占比很小，即使完全相同，对整体相似度影响有限。

---

## 风险规避方案

### 总体策略

```
┌─────────────────────────────────────────────────────────────┐
│                 统一SDK + 差异化构建策略                      │
├─────────────────────────────────────────────────────────────┤
│  Layer 1: 源码层 → 统一维护，版本控制                        │
│  Layer 2: 构建层 → 产品专属混淆字典、垃圾代码注入             │
│  Layer 3: 产物层 → 差异化AAR/DEX                            │
│  Layer 4: 集成层 → 各产品独立签名、独立配置                   │
└─────────────────────────────────────────────────────────────┘
```

### 具体措施

#### 1. 混淆字典差异化

每个产品使用**独立的混淆字典**，确保混淆后类名/方法名不同。

```kotlin
// build.gradle.kts
val productId: String by project  // 如 "app1", "app2"

android {
    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-dictionary-${productId}.txt"  // 产品专属字典
            )
        }
    }
}
```

**字典生成脚本**：
```kotlin
// generate-dictionary.gradle.kts
fun generateDictionary(productId: String, outputFile: File) {
    val seed = productId.hashCode().toLong()
    val random = Random(seed)
    val chars = ('a'..'z') + ('A'..'Z')
    
    val words = (1..5000).map {
        val length = random.nextInt(3, 8)
        (1..length).map { chars[random.nextInt(chars.size)] }.joinToString("")
    }.distinct()
    
    outputFile.writeText(words.joinToString("\n"))
}
```

#### 2. 垃圾代码注入（增强版）

在SDK代码中注入**产品专属的无用但合法代码**，打破字节码相似性。

```kotlin
// 垃圾代码生成策略
object JunkCodeGenerator {
    
    fun generateForProduct(productId: String): List<JunkClass> {
        val seed = productId.hashCode().toLong()
        val random = Random(seed)
        
        val classCount = random.nextInt(30, 50)  // 30-50个垃圾类
        
        return (1..classCount).map { i ->
            JunkClass(
                name = "Internal${productId.take(2).uppercase()}$i",
                methods = generateMethods(random, productId, i),
                fields = generateFields(random)
            )
        }
    }
    
    private fun generateMethods(random: Random, productId: String, classIndex: Int): List<JunkMethod> {
        val methodCount = random.nextInt(5, 15)
        
        return (1..methodCount).map { j ->
            val bodyType = random.nextInt(5)
            JunkMethod(
                name = "process${productId}${classIndex}_$j",
                body = when (bodyType) {
                    0 -> generateIfElseBody(random)      // if-else结构
                    1 -> generateLoopBody(random)        // 循环结构
                    2 -> generateSwitchBody(random)      // switch结构
                    3 -> generateTryCatchBody(random)    // try-catch结构
                    else -> generateMixedBody(random)    // 混合结构
                }
            )
        }
    }
    
    private fun generateIfElseBody(random: Random): String {
        val conditions = random.nextInt(2, 5)
        return buildString {
            appendLine("int result = 0;")
            repeat(conditions) { i ->
                val op = if (random.nextBoolean()) ">" else "<"
                val value = random.nextInt(100)
                appendLine("if (result $op $value) { result += ${i + 1}; }")
            }
            appendLine("return result;")
        }
    }
    
    // ... 其他body生成方法
}
```

#### 3. 资源前缀差异化

```kotlin
// build.gradle.kts
android {
    resourcePrefix = "ad_${productId}_"
}
```

#### 4. 构建时类名前缀注入

使用ASM在编译时为SDK类添加产品专属前缀。

```kotlin
// 使用Transform API或AGP新版bytecode manipulation
abstract class ClassPrefixTransform : AsmClassVisitorFactory<ClassPrefixParameters> {
    
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val productPrefix = parameters.get().productPrefix.get()
        return ClassPrefixVisitor(productPrefix, nextClassVisitor)
    }
}

class ClassPrefixVisitor(
    private val prefix: String,
    cv: ClassVisitor
) : ClassVisitor(ASM9, cv) {
    
    override fun visit(
        version: Int, access: Int, name: String,
        signature: String?, superName: String?, interfaces: Array<String>?
    ) {
        // 只处理SDK包名下的类
        val newName = if (name.startsWith("net/corekit/monetize")) {
            name.replace("monetize", "monetize/$prefix")
        } else {
            name
        }
        super.visit(version, access, newName, signature, superName, interfaces)
    }
}
```

#### 5. 字符串混淆差异化

```kotlin
// StringFog配置：每产品使用不同key
stringFog {
    enable = true
    fogClassName = "com.${productId}.StringFog"
    key = generateKeyForProduct(productId)  // 基于productId生成
    excludes = listOf("androidx.**", "com.google.**")
}
```

#### 6. 差异化AAR构建流程

```
┌─────────────────────────────────────────────────────────────┐
│                    完整构建流程                              │
├─────────────────────────────────────────────────────────────┤
│  1. 统一SDK源码（Git仓库）                                   │
│           ↓                                                  │
│  2. CI触发（参数：PRODUCT_ID）                               │
│           ↓                                                  │
│  3. 生成产品专属混淆字典                                     │
│           ↓                                                  │
│  4. 注入产品专属垃圾代码                                     │
│           ↓                                                  │
│  5. 编译 + 混淆                                              │
│           ↓                                                  │
│  6. 字符串混淆（产品专属key）                                 │
│           ↓                                                  │
│  7. 输出差异化AAR → Maven仓库                                │
│           ↓                                                  │
│  8. 各产品依赖自己的AAR版本                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 推荐方案

### 综合评估

| 评估维度 | 方案A得分 | 方案B得分 |
|---------|----------|----------|
| 开发效率 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 维护成本 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 代码质量 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 关联风险 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 团队协作 | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **总分** | **12/25** | **23/25** |

### 最终建议

**推荐采用方案B：统一SDK + 差异化构建**

理由：
1. 关联风险**可控**（代码占比小 + 差异化构建）
2. 开发/维护/测试效率**显著提升**
3. 已有混淆+垃圾代码基础设施，改造成本低

### 风险等级评定

| 场景 | 风险等级 | 建议 |
|------|---------|------|
| 账号完全独立 | 🟢 低 | 直接采用方案B |
| 同公司多账号 | 🟡 中 | 方案B + 完整差异化措施 |
| 有被封历史 | 🔴 高 | 谨慎评估，可考虑方案A |

---

## 实施路线

### Phase 1：基础设施准备（1-2周）

- [ ] 设计统一SDK架构（参考现有`ad_sdk_refactor_plan.md`）
- [ ] 搭建SDK专属Git仓库
- [ ] 配置CI/CD流水线

### Phase 2：差异化构建能力（2-3周）

- [ ] 实现产品专属混淆字典生成
- [ ] 增强垃圾代码注入（多种控制流结构）
- [ ] 实现字符串混淆差异化
- [ ] 验证DEX指纹差异性

### Phase 3：SDK统一重构（4-6周）

- [ ] 抽取各产品广告封装的共性逻辑
- [ ] 实现统一配置体系（短别名JSON）
- [ ] 完善错误处理与降级策略
- [ ] 编写统一接入文档

### Phase 4：产品迁移（按产品逐个）

- [ ] 产品A迁移 + 回归测试
- [ ] 产品B迁移 + 回归测试
- [ ] ...

### Phase 5：持续优化

- [ ] 监控关联风险指标
- [ ] 定期更新垃圾代码模板
- [ ] 收集反馈，迭代SDK

---

## 附录

### A. 相关文档

- [广告聚合SDK重构方案](./monetize/docs/ad_sdk_refactor_plan.md)
- [竞价配置指南](./BIDDING_CONFIG_GUIDE.md)
- [广告测试指南](./AD_TESTING_GUIDE.md)

### B. 参考资料

- Google Play开发者政策中心
- ProGuard/R8混淆最佳实践
- ASM字节码操作指南

---

> **文档维护者**: 广告SDK团队  
> **审核状态**: 待审核
