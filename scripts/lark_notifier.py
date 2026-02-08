#!/usr/bin/env python3
"""
飞书通知脚本 - 用于 CI/CD 构建结果通知

功能:
    1. 获取飞书 Access Token
    2. 上传文件到飞书云盘
    3. 发送卡片消息到群（含下载链接）

环境变量:
    LARK_APP_ID      - 飞书应用 ID (必需)
    LARK_APP_SECRET  - 飞书应用 Secret (必需)
    LARK_CHAT_ID     - 目标群 Chat ID (必需)

用法:
    python3 scripts/lark_notifier.py \\
        --version "1.0.7" \\
        --status "success" \\
        --duration "8m 32s" \\
        --file "/path/to/app-release.aab" \\
        --log-url "https://..." \\
        --commit "abc1234"
"""
import argparse
import json
import os
import sys
from datetime import datetime
from pathlib import Path

import requests


def get_env_or_exit(name: str) -> str:
    """获取环境变量，不存在则退出"""
    value = os.environ.get(name)
    if not value:
        print(f"❌ 错误: 环境变量 {name} 未设置", file=sys.stderr)
        sys.exit(1)
    return value


def get_tenant_access_token(app_id: str, app_secret: str) -> str:
    """获取 tenant_access_token"""
    url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    resp = requests.post(url, json={"app_id": app_id, "app_secret": app_secret}, timeout=10)
    data = resp.json()
    if data.get("code") != 0:
        print(f"❌ 获取 Token 失败: {data.get('msg')}", file=sys.stderr)
        sys.exit(1)
    return data["tenant_access_token"]


def upload_file_to_lark(token: str, file_path: str) -> str:
    """
    上传文件到飞书，返回 file_key
    
    使用 im/v1/files 接口上传文件
    """
    url = "https://open.feishu.cn/open-apis/im/v1/files"
    headers = {"Authorization": f"Bearer {token}"}
    
    file_name = Path(file_path).name
    file_size = os.path.getsize(file_path)
    
    print(f"   上传文件: {file_name} ({file_size / 1024 / 1024:.2f} MB)")
    
    with open(file_path, "rb") as f:
        files = {
            "file": (file_name, f, "application/octet-stream"),
            "file_type": (None, "stream"),
            "file_name": (None, file_name),
        }
        resp = requests.post(url, headers=headers, files=files, timeout=300)
    
    data = resp.json()
    if data.get("code") != 0:
        print(f"   ❌ 上传失败: {data.get('msg')}", file=sys.stderr)
        return ""
    
    file_key = data.get("data", {}).get("file_key", "")
    print(f"   ✅ 上传成功: {file_key}")
    return file_key


def send_file_message(token: str, chat_id: str, file_key: str) -> dict:
    """发送文件消息到群"""
    url = "https://open.feishu.cn/open-apis/im/v1/messages"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    params = {"receive_id_type": "chat_id"}
    
    data = {
        "receive_id": chat_id,
        "msg_type": "file",
        "content": json.dumps({"file_key": file_key})
    }
    
    resp = requests.post(url, headers=headers, params=params, json=data, timeout=10)
    return resp.json()


def send_card_message(
    token: str,
    chat_id: str,
    version: str,
    status: str,
    duration: str,
    log_url: str,
    commit: str,
    file_key: str = "",
) -> dict:
    """发送卡片消息"""
    url = "https://open.feishu.cn/open-apis/im/v1/messages"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    params = {"receive_id_type": "chat_id"}
    
    # 根据状态设置颜色
    is_success = status.lower() in ("success", "成功", "true", "1")
    template = "green" if is_success else "red"
    status_emoji = "✅" if is_success else "❌"
    status_text = "构建成功" if is_success else "构建失败"
    title = "📦 构建通知"
    
    # 构建时间
    build_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    # 卡片消息内容
    elements = [
        {
            "tag": "div",
            "fields": [
                {"is_short": True, "text": {"tag": "lark_md", "content": f"**版本**\n{version}"}},
                {"is_short": True, "text": {"tag": "lark_md", "content": f"**状态**\n{status_emoji} {status_text}"}},
                {"is_short": True, "text": {"tag": "lark_md", "content": f"**耗时**\n{duration}"}},
                {"is_short": True, "text": {"tag": "lark_md", "content": "**触发者**\nGitHub CI"}}
            ]
        },
        {"tag": "hr"},
    ]
    
    # 按钮区域
    actions = []
    
    # 如果有文件，添加说明文字
    if file_key:
        elements.append({
            "tag": "div",
            "text": {"tag": "lark_md", "content": "📎 **AAB 文件已上传**，请点击下方文件下载"}
        })
        elements.append({"tag": "hr"})
    
    # 查看日志按钮
    actions.append({
        "tag": "button",
        "text": {"tag": "plain_text", "content": "📋 查看日志"},
        "type": "default",
        "url": log_url
    })
    
    elements.append({
        "tag": "action",
        "actions": actions
    })
    
    # 底部注释
    elements.append({
        "tag": "note",
        "elements": [
            {"tag": "plain_text", "content": f"构建时间: {build_time} | Commit: {commit[:7] if len(commit) > 7 else commit}"}
        ]
    })
    
    card = {
        "config": {"wide_screen_mode": True},
        "header": {
            "title": {"tag": "plain_text", "content": title},
            "template": template
        },
        "elements": elements
    }
    
    data = {
        "receive_id": chat_id,
        "msg_type": "interactive",
        "content": json.dumps(card)
    }
    
    resp = requests.post(url, headers=headers, params=params, json=data, timeout=10)
    return resp.json()


def main():
    parser = argparse.ArgumentParser(description="发送飞书构建通知")
    parser.add_argument("--version", required=True, help="版本号")
    parser.add_argument("--status", required=True, help="构建状态 (success/failure)")
    parser.add_argument("--duration", default="N/A", help="构建耗时")
    parser.add_argument("--file", default="", help="要上传的文件路径 (AAB/APK)")
    parser.add_argument("--log-url", default="#", help="日志链接")
    parser.add_argument("--commit", default="unknown", help="Commit SHA")
    
    args = parser.parse_args()
    
    # 获取凭证
    app_id = get_env_or_exit("LARK_APP_ID")
    app_secret = get_env_or_exit("LARK_APP_SECRET")
    chat_id = get_env_or_exit("LARK_CHAT_ID")
    
    print("1. 获取 Access Token...")
    token = get_tenant_access_token(app_id, app_secret)
    print(f"   ✅ Token: {token[:20]}...")
    
    # 上传文件（如果提供）
    file_key = ""
    if args.file and os.path.exists(args.file):
        print("\n2. 上传文件到飞书...")
        file_key = upload_file_to_lark(token, args.file)
    elif args.file:
        print(f"\n⚠️ 警告: 文件不存在 {args.file}")
    
    # 发送卡片消息
    print("\n3. 发送卡片消息...")
    result = send_card_message(
        token=token,
        chat_id=chat_id,
        version=args.version,
        status=args.status,
        duration=args.duration,
        log_url=args.log_url,
        commit=args.commit,
        file_key=file_key,
    )
    
    if result.get("code") == 0:
        print("   ✅ 卡片消息发送成功!")
    else:
        print(f"   ❌ 发送失败: {result.get('msg')}", file=sys.stderr)
        print(f"   错误码: {result.get('code')}", file=sys.stderr)
        sys.exit(1)
    
    # 发送文件消息（如果有文件）
    if file_key:
        print("\n4. 发送文件消息...")
        file_result = send_file_message(token, chat_id, file_key)
        if file_result.get("code") == 0:
            print("   ✅ 文件消息发送成功!")
        else:
            print(f"   ⚠️ 文件消息发送失败: {file_result.get('msg')}")


if __name__ == "__main__":
    main()
