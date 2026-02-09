#!/usr/bin/env python3
"""
飞书卡片消息测试脚本
用于验证飞书机器人配置是否正确，发送模拟的构建通知卡片。

用法:
    python3 scripts/test_lark_card.py

环境变量 (可选，默认使用测试凭证):
    LARK_APP_ID      - 飞书应用 ID
    LARK_APP_SECRET  - 飞书应用 Secret
    LARK_CHAT_ID     - 目标群 Chat ID
"""
import os
import requests
import json

# 凭证配置 (优先使用环境变量)
APP_ID = os.environ.get("LARK_APP_ID", "cli_a903fbac57badcd6")
APP_SECRET = os.environ.get("LARK_APP_SECRET", "22msGNoggdqOFL4f1HSpabpXOVfsAirb")
CHAT_ID = os.environ.get("LARK_CHAT_ID", "oc_b7ac3097e762189bd9517ce3f13e245c")


def get_token():
    """获取 tenant_access_token"""
    url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    resp = requests.post(url, json={"app_id": APP_ID, "app_secret": APP_SECRET})
    return resp.json().get("tenant_access_token")


def send_card_message(token, version="1.0.7", status="success", duration="8m 32s"):
    """发送卡片消息"""
    url = "https://open.feishu.cn/open-apis/im/v1/messages"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    params = {"receive_id_type": "chat_id"}
    
    # 根据状态设置颜色
    template = "green" if status == "success" else "red"
    status_text = "✅ 构建成功" if status == "success" else "❌ 构建失败"
    
    # 卡片消息内容
    card = {
        "config": {"wide_screen_mode": True},
        "header": {
            "title": {"tag": "plain_text", "content": "📦 Play Store 构建通知"},
            "template": template
        },
        "elements": [
            {
                "tag": "div",
                "fields": [
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**版本**\n{version}"}},
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**状态**\n{status_text}"}},
                    {"is_short": True, "text": {"tag": "lark_md", "content": f"**耗时**\n{duration}"}},
                    {"is_short": True, "text": {"tag": "lark_md", "content": "**触发者**\n测试用户"}}
                ]
            },
            {"tag": "hr"},
            {
                "tag": "div",
                "text": {
                    "tag": "lark_md",
                    "content": "**更新日志**\n- feat: 新增用户资料页面\n- fix: 修复启动崩溃问题\n- perf: 优化图片加载速度"
                }
            },
            {"tag": "hr"},
            {
                "tag": "action",
                "actions": [
                    {
                        "tag": "button",
                        "text": {"tag": "plain_text", "content": "📥 下载 AAB"},
                        "type": "primary",
                        "url": "https://github.com/example/repo/releases/download/1.0.7/app-release.aab"
                    },
                    {
                        "tag": "button",
                        "text": {"tag": "plain_text", "content": "📋 查看日志"},
                        "type": "default",
                        "url": "https://github.com/example/repo/actions/runs/123456789"
                    }
                ]
            },
            {
                "tag": "note",
                "elements": [
                    {"tag": "plain_text", "content": f"构建时间: 2026-02-09 01:00:00 | Commit: abc1234"}
                ]
            }
        ]
    }
    
    data = {
        "receive_id": CHAT_ID,
        "msg_type": "interactive",
        "content": json.dumps(card)
    }
    
    resp = requests.post(url, headers=headers, params=params, json=data)
    return resp.json()


if __name__ == "__main__":
    print("=" * 50)
    print("飞书卡片消息测试")
    print("=" * 50)
    
    print("\n1. 获取 Access Token...")
    token = get_token()
    if not token:
        print("❌ 获取 Token 失败，请检查 APP_ID 和 APP_SECRET")
        exit(1)
    print(f"   ✅ Token: {token[:20]}...")
    
    print("\n2. 发送卡片消息...")
    result = send_card_message(token)
    print(json.dumps(result, indent=2, ensure_ascii=False))
    
    if result.get("code") == 0:
        print("\n🎉 卡片消息发送成功！请查看飞书群。")
    else:
        print(f"\n❌ 发送失败: {result.get('msg')}")
        print(f"   错误码: {result.get('code')}")
