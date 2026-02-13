#!/usr/bin/env python3
import os
import sys
import requests
import json

# 从环境变量读取飞书凭证 (禁止硬编码)
APP_ID = os.environ.get("LARK_APP_ID", "")
APP_SECRET = os.environ.get("LARK_APP_SECRET", "")

def get_token():
    url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    resp = requests.post(url, json={"app_id": APP_ID, "app_secret": APP_SECRET})
    return resp.json().get("tenant_access_token")

def list_chats(token):
    url = "https://open.feishu.cn/open-apis/im/v1/chats"
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.get(url, headers=headers)
    return resp.json()

if __name__ == "__main__":
    if not APP_ID or not APP_SECRET:
        print("❌ 错误: 请设置环境变量 LARK_APP_ID 和 LARK_APP_SECRET")
        print("   用法: LARK_APP_ID=xxx LARK_APP_SECRET=xxx python3 get_lark_chat_id.py")
        sys.exit(1)
    print("正在获取飞书群组列表...")
    token = get_token()
    if not token:
        print("❌ 获取 Token 失败，请检查凭证")
    else:
        data = list_chats(token)
        if data.get("code") == 0:
            items = data.get("data", {}).get("items", [])
            if not items:
                print("⚠️ 未找到任何群组。请确保机器人已被拉入群组，并且拥有「获取群组信息」权限。")
            else:
                print("-" * 50)
                print(f"{'群组名称':<20} | {'Chat ID'}")
                print("-" * 50)
                for chat in items:
                    name = chat.get("name", "未命名群组")
                    chat_id = chat.get("chat_id")
                    print(f"{name:<20} | {chat_id}")
                print("-" * 50)
        else:
            print(f"❌ 获取失败: {data.get('msg')}")
            print(f"提示: 请确保应用已开启「获取群组信息」权限 (im:chat:readonly)")
