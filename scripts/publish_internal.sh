#!/bin/bash

# 脚本功能：自动打 Internal Tag (A-Z 递增) 并推送到远程
# 使用方法：./scripts/publish_internal.sh <Version>
# 示例：./scripts/publish_internal.sh 1.0.6

if [ -z "$1" ]; then
  echo "❌ 错误: 请输入目标版本号"
  echo "用法: $0 <Version>"
  echo "示例: $0 1.0.6"
  exit 1
fi

VERSION="$1"
TAG_PREFIX="T${VERSION}"

echo "🔍 正在检查版本 ${VERSION} 的现有 Tag..."

# 获取当前版本所有现有的 Internal Tag (例如 T1.0.6.A, T1.0.6.B)
# sort -V 用于自然版本排序
EXISTING_TAGS=$(git tag -l "${TAG_PREFIX}.*" | sort -V)

if [ -z "$EXISTING_TAGS" ]; then
  # 如果没有，从 A 开始
  NEW_SUFFIX="A"
  echo "ℹ️  未发现现有 Tag，从 A 开始"
else
  echo "📋 发现现有 Tag:"
  echo "$EXISTING_TAGS"
  
  # 获取最后一个 Tag
  LAST_TAG=$(echo "$EXISTING_TAGS" | tail -n 1)
  
  # 提取后缀 (例如 T1.0.6.A -> A)
  # ${LAST_TAG##*.} 获取最后一个 . 之后的内容
  LAST_SUFFIX="${LAST_TAG##*.}"
  
  # 计算下一个后缀 (ASCII 递增)
  # 使用 printf %d 转 ASCII 码，+1 后再转回字符
  LAST_ASCII=$(printf "%d" "'$LAST_SUFFIX")
  NEXT_ASCII=$((LAST_ASCII + 1))
  
  # 检查是否超过 Z (Z=90)
  if [ "$NEXT_ASCII" -gt 90 ]; then
     echo "❌ 错误: 版本后缀已超过 Z，请考虑升级主版本号！"
     exit 1
  fi
  
  # 将 ASCII 码转回字符 (Mac/Linux 通用写法)
  NEW_SUFFIX=$(printf "\\$(printf '%03o' "$NEXT_ASCII")")
fi

NEW_TAG="${TAG_PREFIX}.${NEW_SUFFIX}"

echo "=================================================="
echo "🚀 准备发布 Internal 版本"
echo "=================================================="
echo "🏷️  新 Tag:  $NEW_TAG"
echo "Previous: ${LAST_TAG:-None}"
echo "--------------------------------------------------"

read -p "❓ 确认创建并推送到远程吗? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📦 创建 Tag..."
    git tag "$NEW_TAG"
    
    echo "📤 推送到远程..."
    git push origin "$NEW_TAG"
    
    echo "✅ 完成！CI 构建应即将触发。"
    echo "🔗 查看构建: https://github.com/ReMax-ci/HealthTracker-ci/actions"
else
    echo "🚫以前取消操作"
    exit 0
fi
