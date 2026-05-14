#!/bin/bash

# Android 项目打包脚本
# 支持分渠道打包，自动下载 JDK 和 Android SDK，无需 Android Studio 环境

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_ROOT=$(cd "$(dirname "$0")" && pwd)
cd "$PROJECT_ROOT"

# 本地环境目录
LOCAL_ENV_DIR="$PROJECT_ROOT/.build_env"
LOCAL_JDK_DIR="$LOCAL_ENV_DIR/jdk"
LOCAL_SDK_DIR="$LOCAL_ENV_DIR/android-sdk"

# 输出目录
OUTPUT_DIR="$PROJECT_ROOT/outputs"

# 系统检测
OS_TYPE=""
ARCH_TYPE=""

detect_system() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS_TYPE="linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS_TYPE="macos"
    elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
        OS_TYPE="windows"
    else
        OS_TYPE="unknown"
    fi
    
    case $(uname -m) in
        x86_64|amd64)
            ARCH_TYPE="x64"
            ;;
        arm64|aarch64)
            ARCH_TYPE="arm64"
            ;;
        *)
            ARCH_TYPE="x64"  # 默认
            ;;
    esac
    
    log_info "检测到系统: $OS_TYPE-$ARCH_TYPE"
}

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_download() {
    echo -e "${PURPLE}[DOWNLOAD]${NC} $1"
}

log_setup() {
    echo -e "${CYAN}[SETUP]${NC} $1"
}

# 获取 sdkmanager 命令（兼容 Windows Git Bash）
get_sdkmanager_cmd() {
    local sdkmanager_cmd="sdkmanager"
    if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ -n "$MSYSTEM" ]]; then
        sdkmanager_cmd="sdkmanager.bat"
    fi
    echo "$sdkmanager_cmd"
}

# 从项目中读取 compileSdk（优先读取 gradle/libs.versions.toml）
detect_project_compile_sdk() {
    local versions_file="$PROJECT_ROOT/gradle/libs.versions.toml"
    local compile_sdk=""

    if [ -f "$versions_file" ]; then
        compile_sdk=$(awk -F'"' '/^[[:space:]]*compileSdk[[:space:]]*=/{print $2; exit}' "$versions_file")
    fi

    if [ -z "$compile_sdk" ]; then
        compile_sdk="35"
        log_warning "未能自动识别 compileSdk，回退到默认值: $compile_sdk"
    fi

    echo "$compile_sdk"
}

# 从 Gradle 日志中提取缺失的 SDK 组件（如 platforms;android-36 / build-tools;34.0.0）
extract_missing_sdk_components() {
    local log_file=$1
    awk '
        /Failed to install the following SDK components:/ {capture=1; next}
        capture && NF==0 {capture=0}
        capture {
            gsub(/^[[:space:]]+/, "", $0)
            if ($1 ~ /^(platforms;android-[0-9]+|build-tools;[0-9.]+)$/) {
                print $1
            }
        }
    ' "$log_file" | sort -u | tr '\n' ' '
}

# 执行 Gradle 任务：失败后若检测到缺失 SDK 组件，自动安装并重试一次
run_gradle_task() {
    local gradle_task="$1"
    shift

    local extra_args=("$@")
    local log_file="$LOCAL_ENV_DIR/gradle_${gradle_task}_$(date +%s).log"
    mkdir -p "$LOCAL_ENV_DIR"

    set +e
    "$GRADLEW_CMD" "$gradle_task" "${extra_args[@]}" 2>&1 | tee "$log_file"
    local gradle_exit=${PIPESTATUS[0]}
    set -e

    if [ "$gradle_exit" -eq 0 ]; then
        return 0
    fi

    local missing_components
    missing_components=$(extract_missing_sdk_components "$log_file")

    if [ -z "$missing_components" ]; then
        return "$gradle_exit"
    fi

    local sdkmanager_cmd
    sdkmanager_cmd=$(get_sdkmanager_cmd)

    if ! command -v "$sdkmanager_cmd" &> /dev/null; then
        log_error "检测到缺失 SDK 组件，但未找到 sdkmanager，无法自动修复"
        return "$gradle_exit"
    fi

    log_warning "检测到缺失 SDK 组件: $missing_components"
    log_setup "自动安装缺失组件后重试: $gradle_task"
    "$sdkmanager_cmd" $missing_components --channel=0

    "$GRADLEW_CMD" "$gradle_task" "${extra_args[@]}"
}

# 下载进度显示
download_with_progress() {
    local url=$1
    local output=$2
    local description=$3
    
    log_download "正在下载 $description..."
    log_download "URL: $url"
    
    if command -v curl &> /dev/null; then
        curl -L --progress-bar -o "$output" "$url"
    elif command -v wget &> /dev/null; then
        wget --progress=bar:force -O "$output" "$url"
    else
        log_error "未找到 curl 或 wget，无法下载文件"
        exit 1
    fi
}

# 下载并安装 JDK
download_and_setup_jdk() {
    log_setup "开始下载和配置 JDK..."
    
    # 创建本地环境目录
    mkdir -p "$LOCAL_ENV_DIR"
    
    # 确定 JDK 下载链接
    local jdk_url=""
    local jdk_filename=""
    
    case "$OS_TYPE" in
        "macos")
            if [ "$ARCH_TYPE" = "arm64" ]; then
                jdk_url="https://download.oracle.com/java/17/archive/jdk-17.0.12_macos-aarch64_bin.tar.gz"
                jdk_filename="openjdk-17-macos-aarch64.tar.gz"
            else
                jdk_url="https://download.oracle.com/java/17/archive/jdk-17.0.12_macos-x64_bin.tar.gz"
                jdk_filename="openjdk-17-macos-x64.tar.gz"
            fi
            ;;
        "linux")
            if [ "$ARCH_TYPE" = "arm64" ]; then
                jdk_url="https://download.oracle.com/java/17/archive/jdk-17.0.12_linux-aarch64_bin.tar.gz"
                jdk_filename="openjdk-17-linux-aarch64.tar.gz"
            else
                jdk_url="https://download.oracle.com/java/17/archive/jdk-17.0.12_linux-x64_bin.tar.gz"
                jdk_filename="openjdk-17-linux-x64.tar.gz"
            fi
            ;;
        "windows")
            jdk_url="https://download.oracle.com/java/17/archive/jdk-17.0.12_windows-x64_bin.zip"
            jdk_filename="openjdk-17-windows-x64.zip"
            ;;
        *)
            log_error "不支持的操作系统: $OS_TYPE"
            exit 1
            ;;
    esac
    
    local jdk_archive="$LOCAL_ENV_DIR/$jdk_filename"
    
    # 下载 JDK
    if [ ! -f "$jdk_archive" ]; then
        download_with_progress "$jdk_url" "$jdk_archive" "JDK 17"
    else
        log_info "JDK 安装包已存在，跳过下载"
    fi
    
    # 解压 JDK
    if [ ! -d "$LOCAL_JDK_DIR" ]; then
        log_setup "解压 JDK..."
        mkdir -p "$LOCAL_JDK_DIR"
        
        case "$jdk_filename" in
            *.tar.gz)
                tar -xzf "$jdk_archive" -C "$LOCAL_JDK_DIR" --strip-components=1
                ;;
            *.zip)
                if command -v unzip &> /dev/null; then
                    unzip -q "$jdk_archive" -d "$LOCAL_ENV_DIR/temp_jdk"
                    mv "$LOCAL_ENV_DIR/temp_jdk"/*/* "$LOCAL_JDK_DIR/"
                    rm -rf "$LOCAL_ENV_DIR/temp_jdk"
                else
                    log_error "未找到 unzip 命令，无法解压 ZIP 文件"
                    exit 1
                fi
                ;;
        esac
        
        log_success "JDK 解压完成"
    else
        log_info "JDK 已解压，跳过解压步骤"
    fi
    
    # 设置 JAVA_HOME 和 PATH
    export JAVA_HOME="$LOCAL_JDK_DIR"
    export PATH="$JAVA_HOME/bin:$PATH"
    
    log_success "JDK 配置完成"
    log_info "JAVA_HOME: $JAVA_HOME"
}

# 下载并安装 Android SDK
download_and_setup_android_sdk() {
    log_setup "开始下载和配置 Android SDK..."
    
    # 创建本地环境目录
    mkdir -p "$LOCAL_ENV_DIR"
    
    # 确定 Android SDK Command Line Tools 下载链接
    local sdk_url=""
    local sdk_filename=""
    
    case "$OS_TYPE" in
        "macos")
            sdk_url="https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip"
            sdk_filename="commandlinetools-mac-latest.zip"
            ;;
        "linux")
            sdk_url="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
            sdk_filename="commandlinetools-linux-latest.zip"
            ;;
        "windows")
            sdk_url="https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
            sdk_filename="commandlinetools-win-latest.zip"
            ;;
        *)
            log_error "不支持的操作系统: $OS_TYPE"
            exit 1
            ;;
    esac
    
    local sdk_archive="$LOCAL_ENV_DIR/$sdk_filename"
    
    # 下载 Android SDK Command Line Tools
    if [ ! -f "$sdk_archive" ]; then
        download_with_progress "$sdk_url" "$sdk_archive" "Android SDK Command Line Tools"
    else
        log_info "Android SDK 安装包已存在，跳过下载"
    fi
    
    # 解压 Android SDK
    if [ ! -d "$LOCAL_SDK_DIR" ]; then
        log_setup "解压 Android SDK..."
        mkdir -p "$LOCAL_SDK_DIR"
        
        if command -v unzip &> /dev/null; then
            unzip -q "$sdk_archive" -d "$LOCAL_SDK_DIR"
            # 移动到正确的目录结构
            mv "$LOCAL_SDK_DIR/cmdline-tools" "$LOCAL_SDK_DIR/cmdline-tools-temp"
            mkdir -p "$LOCAL_SDK_DIR/cmdline-tools/latest"
            mv "$LOCAL_SDK_DIR/cmdline-tools-temp"/* "$LOCAL_SDK_DIR/cmdline-tools/latest/"
            rm -rf "$LOCAL_SDK_DIR/cmdline-tools-temp"
        else
            log_error "未找到 unzip 命令，无法解压 Android SDK"
            exit 1
        fi
        
        log_success "Android SDK 解压完成"
    else
        log_info "Android SDK 已解压，跳过解压步骤"
    fi
    
    # 设置 Android SDK 环境变量
    export ANDROID_HOME="$LOCAL_SDK_DIR"
    export ANDROID_SDK_ROOT="$LOCAL_SDK_DIR"
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
    
    # 安装必要的 SDK 组件
    log_setup "安装必要的 Android SDK 组件..."
    
    # 根据操作系统选择正确的 sdkmanager 命令
    local sdkmanager_cmd
    sdkmanager_cmd=$(get_sdkmanager_cmd)
    
    # 接受许可证
    log_setup "接受 Android SDK 许可证..."
    if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ -n "$MSYSTEM" ]]; then
        # Windows Git Bash 环境，使用 echo 来自动确认
        echo -e "y\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny" | "$sdkmanager_cmd" --licenses >/dev/null 2>&1 || true
    else
        # Unix/Linux/macOS 环境
        yes | "$sdkmanager_cmd" --licenses >/dev/null 2>&1 || true
    fi
    log_success "许可证接受完成"
    
    # 按项目版本安装基本组件
    local compile_sdk
    compile_sdk=$(detect_project_compile_sdk)
    log_info "检测到项目 compileSdk: $compile_sdk"

    # 说明：
    # - platforms;android-$compile_sdk：匹配项目编译平台
    # - build-tools;34.0.0 与 35.0.0：兼容 AGP 在不同任务（如 lint）中的实际依赖
    "$sdkmanager_cmd" \
        "platform-tools" \
        "platforms;android-${compile_sdk}" \
        "build-tools;34.0.0" \
        "build-tools;35.0.0" \
        --channel=0
    
    log_success "Android SDK 配置完成"
    log_info "ANDROID_HOME: $ANDROID_HOME"
}

# 显示帮助信息
show_help() {
    echo "Android 项目打包脚本"
    echo ""
    echo "用法:"
    echo "  $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help                显示帮助信息"
    echo "  -c, --channel CHANNEL     指定渠道 (official|internal|all|none)，默认: none"
    echo "  -b, --build-type TYPE     指定构建类型 (debug|release|all)，默认: release"
    echo "  -o, --output DIR          指定输出目录，默认: ./outputs"
    echo "  --clean                   构建前清理项目"
    echo "  --bundle                  同时生成 AAB 包"
    echo "  --aab-only                仅构建 AAB 包（不构建 APK）"
    echo "  --no-lint                 跳过 Lint 检查"
    echo ""
    echo "渠道说明:"
    echo "  official   - 正式版本"
    echo "  playstore  - official 的兼容别名"
    echo "  internal   - 内部测试版本"
    echo "  all        - 所有渠道"
    echo "  none       - Gradle 聚合任务（构建 assembleRelease / assembleDebug）"
    echo ""
    echo "示例:"
    echo "  $0                        # 构建不带渠道变体的 release 版本"
    echo "  $0 -c none -b debug       # 构建不带渠道变体的 debug 版本"
    echo "  $0 -c official -b release    # 只构建正式渠道的 release 版本"
    echo "  $0 -c internal -b debug      # 只构建内部测试渠道的 debug 版本"
    echo "  $0 --clean --bundle       # 清理后构建，并生成 AAB 包"
    echo "  $0 -c official --aab-only    # 只构建正式渠道的 AAB 包"
    echo "  $0 -b release --aab-only  # 构建所有渠道的 release AAB 包"
}

# 确保 SDK 许可证已接受
ensure_sdk_licenses_accepted() {
    log_setup "检查并接受 Android SDK 许可证..."

    # 根据操作系统选择正确的 sdkmanager 命令
    local sdkmanager_cmd
    sdkmanager_cmd=$(get_sdkmanager_cmd)

    # 检查 sdkmanager 是否可用
    if ! command -v "$sdkmanager_cmd" &> /dev/null; then
        log_warning "未找到 sdkmanager，跳过许可证检查"
        return
    fi

    # 接受许可证
    if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ -n "$MSYSTEM" ]]; then
        # Windows Git Bash 环境，使用 echo 来自动确认
        echo -e "y\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny\ny" | "$sdkmanager_cmd" --licenses >/dev/null 2>&1 || true
    else
        # Unix/Linux/macOS 环境
        yes | "$sdkmanager_cmd" --licenses >/dev/null 2>&1 || true
    fi

    log_success "SDK 许可证检查完成"
}

# 智能环境检查和设置
setup_environment() {
    log_info "检查和设置构建环境..."

    # 检测系统信息
    detect_system

    # 检查是否需要下载 JDK
    local java_available=false
    local java_version_ok=false

    if command -v java &> /dev/null; then
        java_available=true
        # 检查 Java 版本
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$JAVA_VERSION" -ge 11 ]; then
            java_version_ok=true
            log_success "找到合适的 Java 版本: $JAVA_VERSION"
        else
            log_warning "Java 版本过低: $JAVA_VERSION (需要 11+)"
        fi
    else
        log_warning "未找到系统 Java"
    fi

    # 如果没有合适的 JDK，下载本地 JDK
    if [ "$java_available" = false ] || [ "$java_version_ok" = false ]; then
        log_info "需要设置本地 JDK 环境"
        if [ ! -d "$LOCAL_JDK_DIR" ]; then
            download_and_setup_jdk
        else
            log_info "本地 JDK 已存在，使用本地 JDK"
            export JAVA_HOME="$LOCAL_JDK_DIR"
            export PATH="$JAVA_HOME/bin:$PATH"
            log_info "JAVA_HOME: $JAVA_HOME"
        fi
    fi

    # 检查是否需要设置 Android SDK
    local android_sdk_available=false

    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
        android_sdk_available=true
        log_success "找到系统 Android SDK: $ANDROID_HOME"
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
        android_sdk_available=true
        export ANDROID_HOME="$ANDROID_SDK_ROOT"
        log_success "找到系统 Android SDK: $ANDROID_SDK_ROOT"
    else
        log_warning "未找到系统 Android SDK"
    fi

    # 如果没有 Android SDK，下载本地 SDK
    if [ "$android_sdk_available" = false ]; then
        log_info "需要设置本地 Android SDK 环境"
        if [ ! -d "$LOCAL_SDK_DIR" ]; then
            download_and_setup_android_sdk
        else
            log_info "本地 Android SDK 已存在，使用本地 SDK"
            export ANDROID_HOME="$LOCAL_SDK_DIR"
            export ANDROID_SDK_ROOT="$LOCAL_SDK_DIR"
            export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
            log_info "ANDROID_HOME: $ANDROID_HOME"

            # 确保许可证已接受
            ensure_sdk_licenses_accepted
        fi
    else
        # 即使使用系统 SDK，也要确保许可证已接受
        ensure_sdk_licenses_accepted
    fi

    # 检查 gradlew 并设置跨平台命令
    GRADLEW_CMD="./gradlew"
    if [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ -n "$MSYSTEM" ]]; then
        # Windows Git Bash/MSYS2 环境，优先使用 .bat 版本
        if [ -f "./gradlew.bat" ]; then
            GRADLEW_CMD="./gradlew.bat"
        elif [ -f "./gradlew" ]; then
            GRADLEW_CMD="./gradlew"
        else
            log_error "未找到 gradlew 或 gradlew.bat 文件"
            exit 1
        fi
    else
        # Unix/Linux/macOS 环境
        if [ ! -f "./gradlew" ]; then
            log_error "未找到 gradlew 文件"
            exit 1
        fi
    fi

#    # 检查签名文件
#    log_info "检查签名文件..."
#
#    # 检查 Play 市场渠道签名文件
#    if [ ! -f "./app/src/playstore/videorecovery.keystore" ]; then
#        log_error "未找到 Play 市场渠道签名文件: ./app/src/playstore/videorecovery.keystore"
#        exit 1
#    fi
#
#    if [ ! -f "./app/src/playstore/google-services.json" ]; then
#        log_error "未找到 Play 市场渠道配置文件: ./app/src/playstore/google-services.json"
#        exit 1
#    fi
#
#    # 检查内部测试渠道签名文件
#    if [ ! -f "./app/src/internal/internal-release-key.jks" ]; then
#        log_error "未找到内部测试渠道签名文件: ./app/src/internal/internal-release-key.jks"
#        exit 1
#    fi
#
#    if [ ! -f "./app/src/internal/google-services.json" ]; then
#        log_error "未找到内部测试渠道配置文件: ./app/src/internal/google-services.json"
#        exit 1
#    fi
#
#    log_success "所有渠道签名文件检查通过"

    # 验证最终环境
    log_info "验证构建环境..."

    if ! command -v java &> /dev/null; then
        log_error "Java 仍然不可用，环境设置失败"
        exit 1
    fi

    FINAL_JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$FINAL_JAVA_VERSION" -lt 11 ]; then
        log_error "Java 版本仍然不符合要求: $FINAL_JAVA_VERSION"
        exit 1
    fi

    if [ -z "$ANDROID_HOME" ]; then
        log_error "ANDROID_HOME 未设置，环境设置失败"
        exit 1
    fi

    log_success "环境设置完成"
    log_info "Java版本: $FINAL_JAVA_VERSION"
    log_info "JAVA_HOME: $JAVA_HOME"
    log_info "ANDROID_HOME: $ANDROID_HOME"
    log_info "Gradle命令: $GRADLEW_CMD"
}

# 旧的环境检查函数，保留作为备用
check_environment_simple() {
    log_info "检查构建环境..."

    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "未找到 Java，请安装 JDK 11 或更高版本"
        exit 1
    fi

    # 检查 Java 版本
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        log_error "Java 版本过低，需要 JDK 11 或更高版本"
        exit 1
    fi

    # 检查 gradlew
    if [ ! -f "./gradlew" ]; then
        log_error "未找到 gradlew 文件"
        exit 1
    fi

    # 检查 Android SDK (可选，因为可能在 gradle.properties 中配置了)
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log_warning "未设置 ANDROID_HOME 或 ANDROID_SDK_ROOT 环境变量"
        log_warning "如果构建失败，请设置 Android SDK 路径"
    fi

    # 检查签名文件
    log_info "检查签名文件..."
    
    # 检查正式渠道配置文件
    if [ ! -f "./app/src/official/google-services.json" ]; then
        log_error "未找到正式渠道配置文件: ./app/src/official/google-services.json"
        exit 1
    fi

    if [ ! -f "./app/src/internal/google-services.json" ]; then
        log_error "未找到内部测试渠道配置文件: ./app/src/internal/google-services.json"
        exit 1
    fi
    
    log_success "所有渠道签名文件检查通过"
}

# 清理项目
clean_project() {
    if [ "$CLEAN_BUILD" = true ]; then
        log_info "清理项目..."
        run_gradle_task "clean"
        log_success "项目清理完成"
    fi
}

# 创建输出目录
create_output_dir() {
    if [ ! -d "$OUTPUT_DIR" ]; then
        mkdir -p "$OUTPUT_DIR"
        log_info "创建输出目录: $OUTPUT_DIR"
    fi
}

# 构建 APK
build_apk() {
    local channel=$1
    local build_type=$2
    # 构建类型首字母大写（Release/Debug）
    local build_first_char=$(echo "${build_type:0:1}" | tr '[:lower:]' '[:upper:]')
    local build_rest_chars="${build_type:1}"
    local build_type_cap="${build_first_char}${build_rest_chars}"

    local task_variant=""
    local output_variant_filter=""
    if [ "$channel" = "none" ] || [ -z "$channel" ]; then
        # 不带渠道变体：assembleRelease / assembleDebug
        task_variant="$build_type_cap"
        output_variant_filter="$build_type"
        log_info "构建 $build_type APK（不带渠道变体）..."
    else
        # 带渠道变体：assembleOfficialRelease / assembleInternalDebug
        local channel_first_char=$(echo "${channel:0:1}" | tr '[:lower:]' '[:upper:]')
        local channel_rest_chars="${channel:1}"
        local channel_cap="${channel_first_char}${channel_rest_chars}"
        task_variant="${channel_cap}${build_type_cap}"
        output_variant_filter="${channel}${build_type_cap}"
        log_info "构建 ${channel}-${build_type} APK..."
    fi

    # 在R8混淆前激进清理内存
    log_info "激进清理内存为R8混淆做准备..."
    $GRADLEW_CMD --stop > /dev/null 2>&1 || true

    # 清理所有可能占用内存的中间文件
    log_info "清理中间文件..."
    rm -rf ./app/build/tmp/ > /dev/null 2>&1 || true
    rm -rf ./app/build/intermediates/dex* > /dev/null 2>&1 || true
    rm -rf ./app/build/intermediates/transforms/ > /dev/null 2>&1 || true
    rm -rf ./app/build/intermediates/javac/ > /dev/null 2>&1 || true
    rm -rf ./app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/ > /dev/null 2>&1 || true
    rm -rf ./.gradle/caches/transforms-* > /dev/null 2>&1 || true

    # 等待内存释放
    log_info "等待内存释放..."
    sleep 5

    # 跳过可能导致OOM的Lint任务，但保持R8混淆
    log_info "开始构建（已跳过Lint任务，优化内存使用）..."
    local lint_skip_args=""
    if [ "$NO_LINT" = true ]; then
        lint_skip_args="-x lint -x lintVitalRelease -x lintVitalDebug"
    fi
    
    run_gradle_task "assemble${task_variant}" \
        $lint_skip_args \
        --max-workers=1 \
        --no-parallel \
        --no-daemon

    # 查找生成的 APK 文件（优先使用变体过滤）
    APK_PATH=$(find ./app/build/outputs/apk -name "*.apk" -path "*$output_variant_filter*" 2>/dev/null | head -1)
    
    # 如果没找到，尝试使用渠道和构建类型查找（无渠道模式仅按构建类型）
    if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
        if [ "$channel" = "none" ] || [ -z "$channel" ]; then
            APK_PATH=$(find ./app/build/outputs/apk -name "*.apk" -path "*$build_type*" 2>/dev/null | head -1)
        else
            APK_PATH=$(find ./app/build/outputs/apk -name "*.apk" -path "*$channel*" -path "*$build_type*" 2>/dev/null | head -1)
        fi
    fi

    if [ -f "$APK_PATH" ]; then
        # 复制到输出目录
        APK_NAME=$(basename "$APK_PATH")
        cp "$APK_PATH" "$OUTPUT_DIR/"
        log_success "APK 构建完成: $APK_NAME"

        # 显示文件信息
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log_info "文件大小: $APK_SIZE"
        log_info "文件路径: $OUTPUT_DIR/$APK_NAME"
    else
        log_error "未找到生成的 APK 文件"
        return 1
    fi
}

# 构建 AAB
build_bundle() {
    local channel=$1
    local build_type=$2
    local build_first_char=$(echo "${build_type:0:1}" | tr '[:lower:]' '[:upper:]')
    local build_rest_chars="${build_type:1}"
    local build_type_cap="${build_first_char}${build_rest_chars}"

    local task_variant=""
    local output_variant_filter=""
    if [ "$channel" = "none" ] || [ -z "$channel" ]; then
        # 不带渠道变体：bundleRelease / bundleDebug
        task_variant="$build_type_cap"
        output_variant_filter="$build_type"
        log_info "构建 $build_type AAB（不带渠道变体）..."
    else
        # 带渠道变体：bundleOfficialRelease / bundleInternalDebug
        local channel_first_char=$(echo "${channel:0:1}" | tr '[:lower:]' '[:upper:]')
        local channel_rest_chars="${channel:1}"
        local channel_cap="${channel_first_char}${channel_rest_chars}"
        task_variant="${channel_cap}${build_type_cap}"
        output_variant_filter="${channel}${build_type_cap}"
        log_info "构建 ${channel}-${build_type} AAB..."
    fi

    run_gradle_task "bundle${task_variant}"

    # 查找生成的 AAB 文件（优先使用变体过滤）
    AAB_PATH=$(find ./app/build/outputs/bundle -name "*.aab" -path "*$output_variant_filter*" 2>/dev/null | head -1)
    
    # 如果没找到，尝试使用渠道和构建类型查找（无渠道模式仅按构建类型）
    if [ -z "$AAB_PATH" ] || [ ! -f "$AAB_PATH" ]; then
        if [ "$channel" = "none" ] || [ -z "$channel" ]; then
            AAB_PATH=$(find ./app/build/outputs/bundle -name "*.aab" -path "*$build_type*" 2>/dev/null | head -1)
        else
            AAB_PATH=$(find ./app/build/outputs/bundle -name "*.aab" -path "*$channel*" -path "*$build_type*" 2>/dev/null | head -1)
        fi
    fi

    if [ -f "$AAB_PATH" ]; then
        # 复制到输出目录
        AAB_NAME=$(basename "$AAB_PATH")
        cp "$AAB_PATH" "$OUTPUT_DIR/"
        log_success "AAB 构建完成: $AAB_NAME"

        # 显示文件信息
        AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
        log_info "文件大小: $AAB_SIZE"
        log_info "文件路径: $OUTPUT_DIR/$AAB_NAME"
    else
        log_error "未找到生成的 AAB 文件"
        return 1
    fi
}

# 构建指定变体
build_variant() {
    local channel=$1
    local build_type=$2

    if [ "$channel" = "none" ] || [ -z "$channel" ]; then
        log_info "==== 构建 $build_type（不带渠道变体） ===="
    else
        log_info "==== 构建 $channel-$build_type 变体 ===="
    fi

    # 根据 AAB_ONLY 决定构建内容
    if [ "$AAB_ONLY" = true ]; then
        # 仅构建 AAB
        build_bundle "$channel" "$build_type"
    else
        # 构建 APK
        build_apk "$channel" "$build_type"

        # 如果需要，构建 AAB
        if [ "$BUILD_BUNDLE" = true ]; then
            build_bundle "$channel" "$build_type"
        fi
    fi

    echo ""
}

# 主构建函数
main_build() {
    local channels=()
    local build_types=()

    # 确定要构建的渠道
    if [ "$CHANNEL" = "all" ]; then
        channels=("official" "internal")
    elif [ "$CHANNEL" = "none" ]; then
        channels=("none")
    else
        channels=("$CHANNEL")
    fi

    # 确定要构建的类型
    if [ "$BUILD_TYPE" = "all" ]; then
        build_types=("debug" "release")
    else
        build_types=("$BUILD_TYPE")
    fi

    log_info "开始构建..."
    log_info "渠道: ${channels[*]}"
    log_info "构建类型: ${build_types[*]}"
    echo ""

    # 遍历所有组合进行构建
    for channel in "${channels[@]}"; do
        for build_type in "${build_types[@]}"; do
            build_variant "$channel" "$build_type"
        done
    done
}

# 显示构建结果
show_results() {
    log_success "==== 构建完成 ===="
    log_info "输出文件列表:"

    if [ -d "$OUTPUT_DIR" ]; then
        ls -la "$OUTPUT_DIR"/*.apk "$OUTPUT_DIR"/*.aab 2>/dev/null | while read -r line; do
            log_info "$line"
        done
    fi

    log_info "输出目录: $OUTPUT_DIR"
}

# 默认参数
CHANNEL="none"
BUILD_TYPE="release"
OUTPUT_DIR="$PROJECT_ROOT/outputs"
CLEAN_BUILD=false
BUILD_BUNDLE=false
AAB_ONLY=false
NO_LINT=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -c|--channel)
            CHANNEL="$2"
            if [ "$CHANNEL" = "playstore" ]; then
                CHANNEL="official"
                log_warning "channel=playstore 已映射为 official"
            fi
            if [[ ! "$CHANNEL" =~ ^(official|internal|all|none)$ ]]; then
                log_error "无效的渠道: $CHANNEL"
                log_error "支持的渠道: official, internal, all, none"
                exit 1
            fi
            shift 2
            ;;
        -b|--build-type)
            BUILD_TYPE="$2"
            if [[ ! "$BUILD_TYPE" =~ ^(debug|release|all)$ ]]; then
                log_error "无效的构建类型: $BUILD_TYPE"
                log_error "支持的构建类型: debug, release, all"
                exit 1
            fi
            shift 2
            ;;
        -o|--output)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        --clean)
            CLEAN_BUILD=true
            shift
            ;;
        --bundle)
            BUILD_BUNDLE=true
            shift
            ;;
        --aab-only)
            AAB_ONLY=true
            shift
            ;;
        --no-lint)
            NO_LINT=true
            shift
            ;;
        *)
            log_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

# 主流程
main() {
    log_info "Android 项目打包脚本启动"
    echo ""

    setup_environment
    create_output_dir
    clean_project
    main_build
    show_results

    log_success "所有构建任务完成！"
}

# 捕获 Ctrl+C
trap 'log_warning "构建被用户中断"; exit 130' INT

# 执行主函数
main
