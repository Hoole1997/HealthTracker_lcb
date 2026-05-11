# CI/CD 配置入口说明

当前项目使用 Android product flavors 区分渠道配置，不再使用旧的 `scripts/internal.gradle` / `scripts/official.gradle` Shifter 配置。

## 实际配置文件

- Internal: `app/src/internal/config.gradle.kts`
- Official: `app/src/official/config.gradle.kts`
- 签名脚本: `scripts/sign.gradle`

根 `build.gradle.kts` 会根据 Gradle 任务名选择配置：

```kotlin
val selectedChannel = when {
    hasOfficialTask && !hasInternalTask -> "official"
    else -> "internal"
}

apply(from = file("app/src/$selectedChannel/config.gradle.kts"))
```

app 模块通过 `rootProject.extra` 读取 `admob`、`gam`、`pangle`、`topon`、`app`、`url`、`analytics` 等配置。

## 常用命令

```bash
./gradlew assembleInternalDebug
./gradlew assembleInternalRelease
./gradlew assembleOfficialDebug
./gradlew assembleOfficialRelease
./gradlew bundleOfficialRelease
```

不要再使用 `-PremoteOverride=true` 切换配置；如需正式包，请直接执行 `Official` 变体任务。

## 配置检查

- `app/src/internal/config.gradle.kts` 中 `app.applicationId` 应为内测包名
- `app/src/official/config.gradle.kts` 中 `app.applicationId` 应为正式包名
- 两个配置文件应保持相同配置块和字段结构
- 不使用的广告平台 ID 保留空字符串
- `string_fog` 是否开启以这两个 flavor 配置文件为准
