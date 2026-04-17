#!/bin/bash

# 脚本功能：帮助生成 GitHub Actions 需要的 Secrets 值
# 使用方法：在项目根目录运行 sh scripts/generate_ci_secrets_helper.sh

# 创建输出目录
OUTPUT_DIR="build/secrets"
mkdir -p "$OUTPUT_DIR"

echo "=================================================="
echo "      GitHub Actions Secrets 生成助手"
echo "=================================================="
echo "🔒 Secrets 将保存到目录: $OUTPUT_DIR"
echo "⚠️  注意: 请勿将这些文件提交到版本控制系统！"

generate_secrets() {
    local FLAVOR=$1
    local PROP_FILE="app/src/${FLAVOR}/sign.properties"
    
    echo ""
    echo "Processing ${FLAVOR}..."

    if [ ! -f "$PROP_FILE" ]; then
        echo "⚠️  未找到 ${PROP_FILE}，跳过 ${FLAVOR}。"
        return
    fi

    # 读取属性
    local STORE_FILE_PATH=$(grep "keystore=" "$PROP_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
    local STORE_PASSWORD=$(grep "keystore.password=" "$PROP_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
    local KEY_ALIAS=$(grep "keyAlias=" "$PROP_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
    local KEY_PASSWORD=$(grep "keyPassword=" "$PROP_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
    
    # 清理路径
    STORE_FILE_PATH=${STORE_FILE_PATH//..\/config\//}
    
    local KEYSTORE_FULL_PATH="app/src/${FLAVOR}/${STORE_FILE_PATH}"

    if [ ! -f "$KEYSTORE_FULL_PATH" ]; then
         echo "⚠️  未找到 Keystore 文件: ${KEYSTORE_FULL_PATH}，尝试在当前目录查找..."
         KEYSTORE_FULL_PATH="app/src/${FLAVOR}/${STORE_FILE_PATH}"
         if [ ! -f "$KEYSTORE_FULL_PATH" ]; then
             echo "❌ 无法找到 Keystore 文件，请检查 sign.properties 配置。"
             return
         fi
    fi

    # 转大写
    local UPPER_FLAVOR=$(echo "$FLAVOR" | tr '[:lower:]' '[:upper:]')

    # 生成 Base64 文件
    local B64_FILE="${OUTPUT_DIR}/${FLAVOR}_keystore_base64.txt"
    base64 -i "$KEYSTORE_FULL_PATH" | tr -d '\n' > "$B64_FILE"
    
    echo "✅ [${UPPER_FLAVOR}] Keystore Base64 已保存到: $B64_FILE"
    echo "   (你可以使用 'cat $B64_FILE | pbcopy' 复制到剪贴板)"

    # 处理 google-services.json
    local GOOGLE_JSON_PATH="app/src/${FLAVOR}/google-services.json"
    if [ -f "$GOOGLE_JSON_PATH" ]; then
        local JSON_B64_FILE="${OUTPUT_DIR}/${FLAVOR}_google_services_json_base64.txt"
        base64 -i "$GOOGLE_JSON_PATH" | tr -d '\n' > "$JSON_B64_FILE"
        echo "✅ [${UPPER_FLAVOR}] Google Services JSON Base64 已保存到: $JSON_B64_FILE"
        echo "   (你可以使用 'cat $JSON_B64_FILE | pbcopy' 复制到剪贴板)"
        echo "   Secret Name: ${UPPER_FLAVOR}_GOOGLE_SERVICES_JSON_BASE64"
    else
        echo "⚠️  [${UPPER_FLAVOR}] 未找到 google-services.json，跳过。"
    fi

    # 处理 Firebase 凭证 (google-services-json-key.json)
    # 查找顺序: app/src/{flavor}/ -> 根目录
    local CRED_FILE="app/src/${FLAVOR}/google-services-json-key.json"
    if [ ! -f "$CRED_FILE" ]; then
        CRED_FILE="google-services-json-key.json"
    fi

    if [ -f "$CRED_FILE" ]; then
        local CRED_B64_FILE="${OUTPUT_DIR}/${FLAVOR}_firebase_credential_base64.txt"
        base64 -i "$CRED_FILE" | tr -d '\n' > "$CRED_B64_FILE"
        echo "✅ [${UPPER_FLAVOR}] Firebase Credential Base64 已保存到: $CRED_B64_FILE"
        echo "   (你可以使用 'cat $CRED_B64_FILE | pbcopy' 复制到剪贴板)"
        echo "   Secret Name: ${UPPER_FLAVOR}_FIREBASE_CREDENTIAL_FILE_CONTENT"
    else
        echo "⚠️  [${UPPER_FLAVOR}] 未找到 google-services-json-key.json (在 src/${FLAVOR}/ 或 根目录)，跳过 Firebase 凭证生成。"
    fi

    echo "   Password: $STORE_PASSWORD"
    echo "   Alias:    $KEY_ALIAS"
    echo "   Key Pass: $KEY_PASSWORD"
}

generate_secrets "internal"
generate_secrets "official"

echo ""
echo "=================================================="
echo "完成后请记得清理生成的文件: rm -rf $OUTPUT_DIR"
echo "=================================================="
