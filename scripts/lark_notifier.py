#!/usr/bin/env python3
"""
飞书通知脚本 - 用于 CI/CD 构建结果通知

功能:
    1. 获取飞书 Access Token
    2. 分片上传大文件到飞书云盘 (支持 > 20MB)
    3. 创建分享链接
    4. 发送卡片消息到群（含下载链接、查看文件夹链接）

环境变量:
    LARK_APP_ID      - 飞书应用 ID (必需)
    LARK_APP_SECRET  - 飞书应用 Secret (必需)
    LARK_CHAT_ID     - 目标群 Chat ID (必需)
    LARK_FOLDER_TOKEN - 云盘文件夹 Token (可选，默认上传到根目录)
"""
import argparse
import json
import math
import os
import sys
from datetime import datetime
from pathlib import Path

import requests


# 分片大小: 4MB
CHUNK_SIZE = 4 * 1024 * 1024


def get_env_or_exit(name: str) -> str:
    """获取环境变量，不存在则退出"""
    value = os.environ.get(name)
    if not value:
        print(f"❌ 错误: 环境变量 {name} 未设置", file=sys.stderr)
        sys.exit(1)
    return value


def get_env_optional(name: str, default: str = "") -> str:
    """获取可选环境变量"""
    return os.environ.get(name, default)


def get_tenant_access_token(app_id: str, app_secret: str) -> str:
    """获取 tenant_access_token"""
    url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
    resp = requests.post(url, json={"app_id": app_id, "app_secret": app_secret}, timeout=10)
    data = resp.json()
    if data.get("code") != 0:
        print(f"❌ 获取 Token 失败: {data.get('msg')}", file=sys.stderr)
        sys.exit(1)
    return data["tenant_access_token"]


def get_root_folder_token(token: str) -> str:
    """获取飞书云盘「我的空间」根目录的 folder token"""
    url = "https://open.feishu.cn/open-apis/drive/explorer/v2/root_folder/meta"
    headers = {"Authorization": f"Bearer {token}"}
    resp = requests.get(url, headers=headers, timeout=10)
    data = resp.json()
    if data.get("code") != 0:
        print(f"   ⚠️ 获取根目录失败: {data.get('msg')}")
        return ""
    folder_token = data.get("data", {}).get("token", "")
    print(f"   ✅ 根目录 Token: {folder_token[:10]}...")
    return folder_token


def list_folder_children(token: str, folder_token: str) -> list[dict]:
    """列出文件夹下的所有子文件夹"""
    url = "https://open.feishu.cn/open-apis/drive/v1/files"
    headers = {"Authorization": f"Bearer {token}"}
    params = {"folder_token": folder_token, "page_size": 100}
    resp = requests.get(url, headers=headers, params=params, timeout=10)
    data = resp.json()
    if data.get("code") != 0:
        print(f"   ⚠️ 列出文件夹内容失败: {data.get('msg')} (code: {data.get('code')})")
        return []
    files = data.get("data", {}).get("files", [])
    return [{"name": f["name"], "token": f["token"]} for f in files if f.get("type") == "folder"]


def create_folder(token: str, parent_token: str, folder_name: str) -> str:
    """在指定文件夹下创建子文件夹"""
    url = "https://open.feishu.cn/open-apis/drive/v1/files/create_folder"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    data = {"name": folder_name, "folder_token": parent_token}
    resp = requests.post(url, headers=headers, json=data, timeout=10)
    result = resp.json()
    if result.get("code") != 0:
        print(f"   ❌ 创建文件夹 '{folder_name}' 失败: {result.get('msg')}")
        return ""
    new_token = result.get("data", {}).get("token", "")
    print(f"   ✅ 已创建文件夹: {folder_name}")
    return new_token


def get_or_create_folder(token: str, parent_token: str, folder_name: str) -> str:
    """获取或创建指定名称的文件夹"""
    children = list_folder_children(token, parent_token)
    for child in children:
        if child["name"] == folder_name:
            print(f"   📁 使用已有文件夹: {folder_name}")
            return child["token"]
    return create_folder(token, parent_token, folder_name)


def upload_file_multipart(token: str, file_path: str, folder_token: str = "") -> tuple[str, str]:
    """分片上传大文件到飞书云盘"""
    headers = {"Authorization": f"Bearer {token}"}
    file_name = Path(file_path).name
    file_size = os.path.getsize(file_path)
    block_num = math.ceil(file_size / CHUNK_SIZE)
    
    if not folder_token:
        folder_token = get_root_folder_token(token)
        if not folder_token:
            print("   ❌ 无法获取根目录 token，跳过上传")
            return "", ""
    
    prepare_url = "https://open.feishu.cn/open-apis/drive/v1/files/upload_prepare"
    prepare_data = {
        "file_name": file_name,
        "parent_type": "explorer",
        "parent_node": folder_token,
        "size": file_size
    }
    
    resp = requests.post(prepare_url, headers=headers, json=prepare_data, timeout=30)
    data = resp.json()
    if data.get("code") != 0:
        print(f"   ❌ 准备上传失败: {data.get('msg')}")
        return "", ""
    
    upload_id = data["data"]["upload_id"]
    part_url = "https://open.feishu.cn/open-apis/drive/v1/files/upload_part"
    
    with open(file_path, "rb") as f:
        for seq in range(block_num):
            chunk = f.read(CHUNK_SIZE)
            files = {
                "upload_id": (None, upload_id),
                "seq": (None, str(seq)),
                "size": (None, str(len(chunk))),
                "file": (file_name, chunk, "application/octet-stream")
            }
            resp = requests.post(part_url, headers=headers, files=files, timeout=120)
            if resp.json().get("code") != 0:
                return "", ""
            print(f"   📤 上传进度: {(seq + 1) / block_num * 100:.1f}%")
    
    finish_url = "https://open.feishu.cn/open-apis/drive/v1/files/upload_finish"
    resp = requests.post(finish_url, headers=headers, json={"upload_id": upload_id, "block_num": block_num}, timeout=30)
    data = resp.json()
    if data.get("code") != 0:
        return "", ""
    return data["data"]["file_token"], file_name


def create_share_link(token: str, file_token: str, is_folder: bool = False) -> str:
    """创建并开放分享权限"""
    res_type = "folder" if is_folder else "file"
    permission_url = f"https://open.feishu.cn/open-apis/drive/v1/permissions/{file_token}/public"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    permission_data = {
        "external_access_entity": "open",
        "security_entity": "anyone_can_view",
        "link_share_entity": "anyone_readable"
    }
    requests.patch(permission_url, headers=headers, params={"type": res_type}, json=permission_data, timeout=10)
    
    if is_folder:
        return f"https://fvkbzjdob1.feishu.cn/drive/folder/{file_token}"
    return f"https://fvkbzjdob1.feishu.cn/file/{file_token}"


def send_card_message(token, chat_id, version, status, duration, log_url, commit, **kwargs) -> dict:
    """发送卡片消息"""
    url = "https://open.feishu.cn/open-apis/im/v1/messages"
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    is_success = status.lower() in ("success", "成功", "true", "1")
    
    elements = [
        {
            "tag": "div",
            "fields": [
                {"is_short": True, "text": {"tag": "lark_md", "content": f"**版本**\n{version}"}},
                {"is_short": True, "text": {"tag": "lark_md", "content": f"**状态**\n{'✅' if is_success else '❌'} {status}"}},
                {"is_short": True, "text": {"tag": "lark_md", "content": f"**耗时**\n{duration}"}},
                {"is_short": True, "text": {"tag": "lark_md", "content": "**触发者**\nGitHub CI"}}
            ]
        },
        {"tag": "hr"},
    ]
    
    actions = []
    if kwargs.get("download_url"):
        actions.append({"tag": "button", "text": {"tag": "plain_text", "content": f"📥 下载 {kwargs.get('file_name', 'AAB')}"}, "type": "primary", "url": kwargs.get("download_url")})
    
    if kwargs.get("folder_url"):
        actions.append({"tag": "button", "text": {"tag": "plain_text", "content": "📂 进入云盘目录"}, "type": "default", "url": kwargs.get("folder_url")})
    
    if not is_success:
        actions.append({"tag": "button", "text": {"tag": "plain_text", "content": "📋 查看日志"}, "type": "default", "url": log_url})
    
    if actions:
        elements.append({"tag": "action", "actions": actions})
    
    elements.append({"tag": "note", "elements": [{"tag": "plain_text", "content": f"构建时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} | Commit: {commit[:7]}"}]})
    
    card = {
        "config": {"wide_screen_mode": True},
        "header": {"title": {"tag": "plain_text", "content": "📦 构建通知"}, "template": "green" if is_success else "red"},
        "elements": elements
    }
    resp = requests.post(url, headers=headers, params={"receive_id_type": "chat_id"}, json={"receive_id": chat_id, "msg_type": "interactive", "content": json.dumps(card)}, timeout=10)
    return resp.json()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--status", required=True)
    parser.add_argument("--duration", default="N/A")
    parser.add_argument("--file", default="")
    parser.add_argument("--project", default="HealthTracker")
    parser.add_argument("--log-url", default="#")
    parser.add_argument("--commit", default="unknown")
    args = parser.parse_args()
    
    app_id = get_env_or_exit("LARK_APP_ID")
    app_secret = get_env_or_exit("LARK_APP_SECRET")
    chat_id = get_env_or_exit("LARK_CHAT_ID")
    folder_token = get_env_optional("LARK_FOLDER_TOKEN", "")
    
    token = get_tenant_access_token(app_id, app_secret)
    download_url = ""
    folder_url = ""
    file_name = ""
    
    if args.file and os.path.exists(args.file):
        root_token = folder_token or get_root_folder_token(token)
        if root_token:
            target_folder = root_token
            project_folder = get_or_create_folder(token, root_token, args.project)
            if project_folder:
                target_folder = project_folder
                version_folder = get_or_create_folder(token, project_folder, args.version)
                if version_folder:
                    target_folder = version_folder
            folder_token = target_folder
            
            file_token, file_name = upload_file_multipart(token, args.file, folder_token)
            if file_token:
                download_url = create_share_link(token, file_token)
                # 为文件夹也开启分享权限，确保外部可进入
                folder_url = create_share_link(token, folder_token, is_folder=True)

    send_card_message(token, chat_id, args.version, args.status, args.duration, args.log_url, args.commit, download_url=download_url, file_name=file_name, folder_url=folder_url)


if __name__ == "__main__":
    main()
