#!/bin/bash
# =============================================================================
# Play Store 发布脚本
# 用法: ./scripts/publish_playstore.sh
# 
# 功能:
#   1. 从 build.gradle.kts 读取当前版本号
#   2. 检查是否已存在同版本 Tag
#   3. 创建并推送 Tag，触发 GitHub Actions 构建
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${YELLOW}📦 Play Store 发布脚本${NC}"
echo "=================================="

# 1. 读取版本号
echo -e "\n${YELLOW}[1/4]${NC} 读取版本号..."
BUILD_GRADLE="$PROJECT_ROOT/app/build.gradle.kts"

if [ ! -f "$BUILD_GRADLE" ]; then
    echo -e "${RED}❌ 错误: 找不到 $BUILD_GRADLE${NC}"
    exit 1
fi

# 使用 Python 提取版本号 (更可靠)
if [ -f "$PROJECT_ROOT/scripts/version_manager.py" ]; then
    VERSION=$(python3 "$PROJECT_ROOT/scripts/version_manager.py" get_version "$BUILD_GRADLE")
else
    # 备用方案: 使用 grep
    VERSION=$(grep -E 'versionName\s*=' "$BUILD_GRADLE" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
fi

if [ -z "$VERSION" ]; then
    echo -e "${RED}❌ 错误: 无法读取版本号${NC}"
    exit 1
fi

echo -e "   版本号: ${GREEN}$VERSION${NC}"

# 2. 检查 Tag 是否已存在
echo -e "\n${YELLOW}[2/4]${NC} 检查 Tag 是否存在..."
git fetch --tags --quiet

if git rev-parse "$VERSION" >/dev/null 2>&1; then
    echo -e "${RED}❌ 错误: Tag '$VERSION' 已存在${NC}"
    echo -e "   请先在 build.gradle.kts 中升级 versionName"
    exit 1
fi

echo -e "   ${GREEN}✅ Tag '$VERSION' 可用${NC}"

# 3. 确认发布
echo -e "\n${YELLOW}[3/4]${NC} 确认发布..."
echo -e "   即将创建 Tag: ${GREEN}$VERSION${NC}"
echo -e "   这将触发 GitHub Actions 构建 Play Store AAB"
echo ""
read -p "确认发布? (y/N): " CONFIRM

if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}已取消${NC}"
    exit 0
fi

# 4. 创建并推送 Tag
echo -e "\n${YELLOW}[4/4]${NC} 创建并推送 Tag..."
git tag -a "$VERSION" -m "Play Store Release $VERSION"
git push origin "$VERSION"

echo -e "\n${GREEN}🎉 发布成功!${NC}"
echo "=================================="
echo -e "Tag:    ${GREEN}$VERSION${NC}"
echo -e "状态:   ${GREEN}已推送到远程仓库${NC}"
echo -e "下一步: GitHub Actions 将自动开始构建"
echo ""
echo -e "查看构建进度: https://github.com/$(git remote get-url origin | sed -E 's/.*github.com[:/](.+)(\.git)?/\1/' | sed 's/\.git$//')/actions"
