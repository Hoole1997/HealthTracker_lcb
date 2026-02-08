#!/usr/bin/env python3
"""
飞书通知脚本 - 用于 CI/CD 构建结果通知

功能:
    1. 获取飞书 Access Token
    2. 上传文件到飞书云盘 (可选)
    3. 发送卡片消息到群

环境变量:
    LARK_APP_ID      - 飞书应用 ID (必需)
    LARK_APP_SECRET  - 飞书应用 Secret (必需)
    LARK_CHAT_ID     - 目标群 Chat ID (必需)

用法:
    python3 scripts/lark_notifier.py \\
        --version "1.0.7" \\
        --status "success" \\
        --duration "8m 32s" \\
        --changelog "- feat: xxx\n- fix: yyy" \\
        --download-url "https://..." \\
        --log-url "https://..." \\
        --commit "abc1234" \\
        --trigger "GitHub Actions"
"""
import argparse
import json
import os
import sys
from datetime import datetime

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


def send_card_message(
    token: str,
    chat_id: str,
    version: str,
    status: str,
    duration: str,
    changelog: str,
    download_url: str,
    log_url: str,
    commit: str,
    trigger: str,
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
    title = "📦 Play Store 构建通知"
    
    # 构建时间
    build_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    # 截断 changelog 避免过长
    if len(changelog) > 500:
        changelog = changelog[:497] + "..."
    
    # 卡片消息内容
    card = {
        "config": {"wide_screen_mode": True},
        "header": {
            "title": {"tag": "plain_text", "content": title},
            "template": template
        },
        "elements": [
            {
                "tag": "div",
                "fields": [
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**版本**\n{version}"}},
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**状态**\n{status_emoji} {status_text}"}},
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**耗时**\n{duration}"}},
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**触发者**\n{trigger}"}}
                ]
            },
            {"tag": "hr"},
            {
                "tag": "div",
                "text": {"tag": "lark_md", "content": f"**更新日志**\n{changelog}"}
            },
            {"tag": "hr"},
            {
                "tag": "action",
                "actions": [
                    {
                        "tag": "button",
                        "text": {"tag": "plain_text", "content": "📥 下载 AAB"},
                        "type": "primary",
                        "url": download_url
                    },
                    {
                        "tag": "button",
                        "text": {"tag": "plain_text", "content": "📋 查看日志"},
                        "type": "default",
                        "url": log_url
                    }
                ]
            },
            {
                "tag": "note",
                "elements": [
                    {"tag": "plain_text", "content": f"构建时间: {build_time} | Commit: {commit[:7] if len(commit) > 7 else commit}"}
                ]
            }
        ]
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
    parser.add_argument("--changelog", default="无更新日志", help="更新日志")
    parser.add_argument("--download-url", default="#", help="下载链接")
    parser.add_argument("--log-url", default="#", help="日志链接")
    parser.add_argument("--commit", default="unknown", help="Commit SHA")
    parser.add_argument("--trigger", default="GitHub Actions", help="触发者")
    
    args = parser.parse_args()
    
    # 获取凭证
    app_id = get_env_or_exit("LARK_APP_ID")
    app_secret = get_env_or_exit("LARK_APP_SECRET")
    chat_id = get_env_or_exit("LARK_CHAT_ID")
    
    print("1. 获取 Access Token...")
    token = get_tenant_access_token(app_id, app_secret)
    print(f"   ✅ Token: {token[:20]}...")
    
    print("\n2. 发送卡片消息...")
    result = send_card_message(
        token=token,
        chat_id=chat_id,
        version=args.version,
        status=args.status,
        duration=args.duration,
        changelog=args.changelog,
        download_url=args.download_url,
        log_url=args.log_url,
        commit=args.commit,
        trigger=args.trigger,
    )
    
    if result.get("code") == 0:
        print("   ✅ 消息发送成功!")
    else:
        print(f"   ❌ 发送失败: {result.get('msg')}", file=sys.stderr)
        print(f"   错误码: {result.get('code')}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
