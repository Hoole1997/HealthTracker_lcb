#!/usr/bin/env python3
"""
飞书通知脚本 - 用于 CI/CD 构建结果通知

功能:
    1. 获取飞书 Access Token
    2. 分片上传大文件到飞书云盘 (支持 > 20MB)
    3. 创建分享链接
    4. 发送卡片消息到群（含下载链接）

环境变量:
    LARK_APP_ID      - 飞书应用 ID (必需)
    LARK_APP_SECRET  - 飞书应用 Secret (必需)
    LARK_CHAT_ID     - 目标群 Chat ID (必需)
    LARK_FOLDER_TOKEN - 云盘文件夹 Token (可选，默认上传到根目录)

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
    """
    获取飞书云盘「我的空间」根目录的 folder token
    
    返回: folder_token 或空字符串
    """
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
    """
    列出文件夹下的所有子文件夹
    
    返回: [{"name": "xxx", "token": "xxx"}, ...]
    """
    url = "https://open.feishu.cn/open-apis/drive/v1/files"
    headers = {"Authorization": f"Bearer {token}"}
    params = {
        "folder_token": folder_token,
        "page_size": 100
    }
    
    resp = requests.get(url, headers=headers, params=params, timeout=10)
    data = resp.json()
    
    if data.get("code") != 0:
        return []
    
    files = data.get("data", {}).get("files", [])
    # 只返回文件夹
    return [{"name": f["name"], "token": f["token"]} 
            for f in files if f.get("type") == "folder"]


def create_folder(token: str, parent_token: str, folder_name: str) -> str:
    """
    在指定文件夹下创建子文件夹
    
    返回: 新文件夹的 token 或空字符串
    """
    url = "https://open.feishu.cn/open-apis/drive/v1/files/create_folder"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    data = {
        "name": folder_name,
        "folder_token": parent_token
    }
    
    resp = requests.post(url, headers=headers, json=data, timeout=10)
    result = resp.json()
    
    if result.get("code") != 0:
        print(f"   ❌ 创建文件夹 '{folder_name}' 失败: {result.get('msg')}")
        return ""
    
    new_token = result.get("data", {}).get("token", "")
    print(f"   ✅ 已创建文件夹: {folder_name}")
    return new_token


def get_or_create_folder(token: str, parent_token: str, folder_name: str) -> str:
    """
    获取或创建指定名称的文件夹
    
    返回: 文件夹的 token
    """
    # 先查找是否已存在
    children = list_folder_children(token, parent_token)
    for child in children:
        if child["name"] == folder_name:
            print(f"   📁 使用已有文件夹: {folder_name}")
            return child["token"]
    
    # 不存在则创建
    return create_folder(token, parent_token, folder_name)


def upload_file_multipart(token: str, file_path: str, folder_token: str = "") -> tuple[str, str]:
    """
    分片上传大文件到飞书云盘
    
    返回: (file_token, file_name)
    """
    headers = {"Authorization": f"Bearer {token}"}
    file_name = Path(file_path).name
    file_size = os.path.getsize(file_path)
    block_num = math.ceil(file_size / CHUNK_SIZE)
    
    print(f"   文件: {file_name} ({file_size / 1024 / 1024:.2f} MB)")
    print(f"   分片数: {block_num} (每片 {CHUNK_SIZE // 1024 // 1024} MB)")
    
    # 如果没有指定文件夹，获取根目录 token
    if not folder_token:
        folder_token = get_root_folder_token(token)
        if not folder_token:
            print("   ❌ 无法获取根目录 token，跳过上传")
            return "", ""
    
    # Step 1: 准备上传 (获取 upload_id)
    prepare_url = "https://open.feishu.cn/open-apis/drive/v1/files/upload_prepare"
    prepare_data = {
        "file_name": file_name,
        "parent_type": "explorer",  # 上传到云盘
        "parent_node": folder_token,  # 必须是有效的 folder token
        "size": file_size
    }
    
    resp = requests.post(prepare_url, headers=headers, json=prepare_data, timeout=30)
    data = resp.json()
    
    if data.get("code") != 0:
        print(f"   ❌ 准备上传失败: {data.get('msg')}")
        return "", ""
    
    upload_id = data["data"]["upload_id"]
    print(f"   ✅ Upload ID: {upload_id[:20]}...")
    
    # Step 2: 分片上传
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
            result = resp.json()
            
            if result.get("code") != 0:
                print(f"   ❌ 分片 {seq + 1}/{block_num} 上传失败: {result.get('msg')}")
                return "", ""
            
            progress = (seq + 1) / block_num * 100
            print(f"   📤 上传进度: {progress:.1f}% ({seq + 1}/{block_num})")
    
    # Step 3: 完成上传
    finish_url = "https://open.feishu.cn/open-apis/drive/v1/files/upload_finish"
    finish_data = {
        "upload_id": upload_id,
        "block_num": block_num
    }
    
    resp = requests.post(finish_url, headers=headers, json=finish_data, timeout=30)
    data = resp.json()
    
    if data.get("code") != 0:
        print(f"   ❌ 完成上传失败: {data.get('msg')}")
        return "", ""
    
    file_token = data["data"]["file_token"]
    print(f"   ✅ 上传完成! File Token: {file_token}")
    
    return file_token, file_name


def create_share_link(token: str, file_token: str) -> str:
    """
    创建文件分享链接
    
    返回: 分享链接 URL
    """
    # 设置文件权限为"链接分享"
    permission_url = f"https://open.feishu.cn/open-apis/drive/v1/permissions/{file_token}/public"
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    params = {"type": "file"}
    
    # 设置权限: 组织内可阅读
    permission_data = {
        "external_access_entity": "open",  # 任何人可访问
        "security_entity": "anyone_can_view",  # 任何人可查看
        "comment_entity": "anyone_can_view",
        "share_entity": "anyone",
        "link_share_entity": "anyone_readable"  # 链接可读
    }
    
    resp = requests.patch(permission_url, headers=headers, params=params, json=permission_data, timeout=10)
    data = resp.json()
    
    if data.get("code") != 0:
        print(f"   ⚠️ 设置分享权限失败: {data.get('msg')}")
        # 继续尝试，可能权限已存在
    
    # 获取分享链接
    # 飞书云盘文件的标准下载链接格式
    share_url = f"https://open.feishu.cn/open-apis/drive/v1/files/{file_token}/download"
    
    # 实际用户下载需要通过飞书客户端，这里返回一个查看链接
    view_url = f"https://fvkbzjdob1.feishu.cn/file/{file_token}"
    
    print(f"   ✅ 分享链接已创建")
    return view_url


def send_card_message(
    token: str,
    chat_id: str,
    version: str,
    status: str,
    duration: str,
    log_url: str,
    commit: str,
    download_url: str = "",
    file_name: str = "",
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
    
    # 如果有下载链接，添加下载按钮
    if download_url:
        actions.append({
            "tag": "button",
            "text": {"tag": "plain_text", "content": f"📥 下载 {file_name or 'AAB'}"},
            "type": "primary",
            "url": download_url
        })
    
    # 查看日志按钮 (仅在失败或需要调试时显示，用户要求成功时不显示)
    if not is_success:
        actions.append({
            "tag": "button",
            "text": {"tag": "plain_text", "content": "📋 查看日志"},
            "type": "default",
            "url": log_url
        })
    
    if actions:
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
    parser.add_argument("--project", default="HealthTracker", help="项目名称（用于创建目录）")
    parser.add_argument("--log-url", default="#", help="日志链接")
    parser.add_argument("--commit", default="unknown", help="Commit SHA")
    
    args = parser.parse_args()
    
    # 获取凭证
    app_id = get_env_or_exit("LARK_APP_ID")
    app_secret = get_env_or_exit("LARK_APP_SECRET")
    chat_id = get_env_or_exit("LARK_CHAT_ID")
    folder_token = get_env_optional("LARK_FOLDER_TOKEN", "")
    
    print("1. 获取 Access Token...")
    token = get_tenant_access_token(app_id, app_secret)
    print(f"   ✅ Token: {token[:20]}...")
    
    # 上传文件（如果提供）
    download_url = ""
    file_name = ""
    
    if args.file and os.path.exists(args.file):
        print("\n2. 准备上传目录...")
        
        # 获取或使用指定的根目录
        root_token = folder_token
        if not root_token:
            root_token = get_root_folder_token(token)
            if not root_token:
                print("   ⚠️ 无法获取根目录，跳过文件上传")
            else:
                # 创建 项目/版本号 目录结构
                project_folder = get_or_create_folder(token, root_token, args.project)
                if project_folder:
                    version_folder = get_or_create_folder(token, project_folder, args.version)
                    if version_folder:
                        folder_token = version_folder
        
        if folder_token:
            print("\n3. 分片上传文件到飞书云盘...")
            file_token, file_name = upload_file_multipart(token, args.file, folder_token)
            
            if file_token:
                print("\n4. 创建分享链接...")
                download_url = create_share_link(token, file_token)
    elif args.file:
        print(f"\n⚠️ 警告: 文件不存在 {args.file}")
    
    # 发送卡片消息
    step_num = 5 if download_url else 2
    print(f"\n{step_num}. 发送卡片消息...")
    result = send_card_message(
        token=token,
        chat_id=chat_id,
        version=args.version,
        status=args.status,
        duration=args.duration,
        log_url=args.log_url,
        commit=args.commit,
        download_url=download_url,
        file_name=file_name,
    )
    
    if result.get("code") == 0:
        print("   ✅ 卡片消息发送成功!")
    else:
        print(f"   ❌ 发送失败: {result.get('msg')}", file=sys.stderr)
        print(f"   错误码: {result.get('code')}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
