#!/bin/bash

# 脚本功能：在本地构建发布 Internal 包
# 流程：
# 1. 自动查找上一个 Tag
# 2. 生成 Changelog (release_notes.txt)
# 3. 调用 Gradle 分发任务

# 检查 Python 环境
if ! command -v python3 &> /dev/null; then
    echo "❌ 错误: 未找到 python3"
    exit 1
fi

# 1. 计算下一个版本号
GRADLE_FILE="app/build.gradle.kts"
VERSION=$(python3 scripts/version_manager.py get_version "$GRADLE_FILE")
if [ -z "$VERSION" ]; then
    echo "❌ 无法读取版本号"
    exit 1
fi

TAG_PREFIX="T${VERSION}"
ALL_TAGS=$(git tag -l "${TAG_PREFIX}.*")
NEXT_SUFFIX=$(python3 scripts/version_manager.py next_suffix "$VERSION" "$ALL_TAGS")
NEXT_TAG="${TAG_PREFIX}.${NEXT_SUFFIX}"

echo "ℹ️  当前基准: $VERSION"
echo "🔮 预测下个 Tag: $NEXT_TAG"

# 2. 查找最近的 Tag 作为 Changelog 起点
# 逻辑：找到最近的一个 Matching Tag，如果没有则用 HEAD~1
LAST_TAG=$(git describe --tags --match "${TAG_PREFIX}.*" --abbrev=0 2>/dev/null)
if [ -z "$LAST_TAG" ]; then
    # 尝试找任意 Tag
    LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)
fi

start_ref="$LAST_TAG"
if [ -z "$start_ref" ]; then
    start_ref=$(git rev-list --max-parents=0 HEAD)
    echo "⚠️  未找到历史 Tag，将生成全量日志"
else
    echo "📍 对比基准: $start_ref"
fi

echo "📝 生成 Changelog ($start_ref -> HEAD)..."
# 第三个参数用于在 Changelog 标题中显示预测的版本号 (而不是 "HEAD")
python3 scripts/generate_changelog.py "$start_ref" "HEAD" "$NEXT_TAG"

if [ ! -f "release_notes.txt" ]; then
    echo "❌ Changelog 生成失败。"
    exit 1
fi

echo "✅ Changelog 已生成: release_notes.txt"
cat release_notes.txt
echo "--------------------------------------------------"

echo "🚀 开始构建并分发 (Version: ${NEXT_TAG})..."
# 显式执行 assemble 以确保 APK 存在，并注入动态版本号
./gradlew clean assembleInternalRelease appDistributionUploadInternalRelease \
  -PinternalVersionName="${NEXT_TAG}"

echo "✅ 本地分发完成！"
