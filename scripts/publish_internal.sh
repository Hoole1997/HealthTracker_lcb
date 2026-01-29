#!/bin/bash

# 脚本功能：自动打 Internal Tag (A-Z...A_A 递增) 并推送到远程
# 使用方法：./scripts/publish_internal.sh [Optional: Version]
# 如果不传参数，自动从 app/build.gradle.kts 读取

GRADLE_FILE="app/build.gradle.kts"
VERSION=""

# 1. 确定版本号
if [ -n "$1" ]; then
  VERSION="$1"
  echo "ℹ️  使用用户输入版本: $VERSION"
else
  if [ -f "scripts/version_manager.py" ]; then
     VERSION=$(python3 scripts/version_manager.py get_version "$GRADLE_FILE")
  fi
  
  if [ -z "$VERSION" ]; then
     echo "❌ 无法从 $GRADLE_FILE 自动提取版本号，请手动指定。"
     echo "用法: $0 <Version>"
     exit 1
  else
     echo "ℹ️  自动检测到版本: $VERSION"
  fi
fi

TAG_PREFIX="T${VERSION}"

# 2. 获取所有 Tags
echo "🔍 正在检查版本 ${VERSION} 的现有 Tag..."
ALL_TAGS=$(git tag -l "${TAG_PREFIX}.*")

# 3. 计算下一个后缀
# 调用 Python 脚本处理复杂的 Z -> A_A 逻辑
NEXT_SUFFIX=$(python3 scripts/version_manager.py next_suffix "$VERSION" "$ALL_TAGS")

NEW_TAG="${TAG_PREFIX}.${NEXT_SUFFIX}"

echo "=================================================="
echo "🚀 准备发布 Internal 版本"
echo "=================================================="
echo "🏷️  新 Tag:  $NEW_TAG"
echo "--------------------------------------------------"

read -p "❓ 确认创建并推送到远程吗? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📦 创建 Tag..."
    git tag "$NEW_TAG"
    
    echo "📤 推送到远程..."
    git push origin "$NEW_TAG"
    
    echo "✅ 完成！Release 构建已触发。"
else
    echo "🚫 操作已取消"
    exit 0
fi
