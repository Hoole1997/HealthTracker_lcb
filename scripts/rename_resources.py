#!/usr/bin/env python3
"""
资源重命名脚本
为 app 模块的资源文件添加 ht_ 前缀，并更新所有引用
"""

import os
import re
import sys
from pathlib import Path

# 配置
PREFIX = "ht_"
APP_DIR = Path("app")
RES_DIR = APP_DIR / "src/main/res"
JAVA_DIR = APP_DIR / "src/main/java"

# 需要排除的资源（系统引用）
EXCLUDE_PATTERNS = [
    r"^ic_launcher.*",  # 启动器图标
    r"^ic_notification.*",  # 通知图标
    r"^splash.*",  # 启动页
]

# 需要处理的资源类型
RESOURCE_TYPES = ["drawable", "layout", "mipmap", "menu", "anim", "animator", "color", "raw", "xml"]

# 不需要重命名文件的资源类型（只重命名内部定义）
VALUE_TYPES = ["values"]

def should_exclude(name, filename):
    """检查资源名是否应该排除"""
    # 排除隐藏文件
    if filename.startswith('.'):
        return True
    for pattern in EXCLUDE_PATTERNS:
        if re.match(pattern, name):
            return True
    return False

def get_resource_files():
    """获取所有需要重命名的资源文件"""
    files = []
    for res_type in RESOURCE_TYPES:
        for res_dir in RES_DIR.glob(f"{res_type}*"):
            if res_dir.is_dir():
                for f in res_dir.iterdir():
                    if f.is_file() and not f.name.startswith(PREFIX):
                        name = f.stem
                        if not should_exclude(name, f.name):
                            files.append(f)
    return files

def rename_resource_files(files, dry_run=False):
    """重命名资源文件"""
    renamed = {}
    for f in files:
        old_name = f.stem
        new_name = PREFIX + old_name
        new_path = f.parent / (new_name + f.suffix)
        
        if not dry_run:
            f.rename(new_path)
        
        renamed[old_name] = new_name
        print(f"  {f.name} -> {new_path.name}")
    
    return renamed

def snake_to_pascal(name):
    """将 snake_case 转换为 PascalCase"""
    return ''.join(word.capitalize() for word in name.split('_'))

def update_kotlin_java_files(renamed, dry_run=False):
    """更新 Kotlin/Java 文件中的资源引用"""
    count = 0
    for ext in ["*.kt", "*.java"]:
        for f in JAVA_DIR.rglob(ext):
            content = f.read_text(encoding='utf-8')
            new_content = content
            
            for old_name, new_name in renamed.items():
                # R.drawable.xxx, R.layout.xxx, etc.
                for res_type in RESOURCE_TYPES:
                    pattern = rf'\bR\.{res_type}\.{re.escape(old_name)}\b'
                    replacement = f'R.{res_type}.{new_name}'
                    new_content = re.sub(pattern, replacement, new_content)
                
                # ViewBinding 类名: ActivityMainBinding -> HtActivityMainBinding
                # 布局文件名 activity_main -> ht_activity_main
                # ViewBinding 类名 ActivityMainBinding -> HtActivityMainBinding
                old_binding_name = snake_to_pascal(old_name) + "Binding"
                new_binding_name = snake_to_pascal(new_name) + "Binding"
                
                # 更新 import 语句和类引用
                pattern = rf'\b{re.escape(old_binding_name)}\b'
                new_content = re.sub(pattern, new_binding_name, new_content)
            
            if new_content != content:
                count += 1
                if not dry_run:
                    f.write_text(new_content, encoding='utf-8')
                print(f"  Updated: {f}")
    
    return count

def update_xml_files(renamed, dry_run=False):
    """更新 XML 文件中的资源引用"""
    count = 0
    for f in RES_DIR.rglob("*.xml"):
        content = f.read_text(encoding='utf-8')
        new_content = content
        
        for old_name, new_name in renamed.items():
            # @drawable/xxx, @layout/xxx, etc.
            for res_type in RESOURCE_TYPES:
                pattern = rf'@{res_type}/{re.escape(old_name)}\b'
                replacement = f'@{res_type}/{new_name}'
                new_content = re.sub(pattern, replacement, new_content)
            
            # @+id/xxx (不重命名 ID，只重命名资源引用)
            # tools:listitem="@layout/xxx"
            # android:src="@drawable/xxx"
        
        if new_content != content:
            count += 1
            if not dry_run:
                f.write_text(new_content, encoding='utf-8')
            print(f"  Updated: {f}")
    
    return count

def update_manifest(renamed, dry_run=False):
    """更新 AndroidManifest.xml 中的资源引用"""
    manifest = APP_DIR / "src/main/AndroidManifest.xml"
    if not manifest.exists():
        return 0
    
    content = manifest.read_text(encoding='utf-8')
    new_content = content
    
    for old_name, new_name in renamed.items():
        for res_type in RESOURCE_TYPES:
            pattern = rf'@{res_type}/{re.escape(old_name)}\b'
            replacement = f'@{res_type}/{new_name}'
            new_content = re.sub(pattern, replacement, new_content)
    
    if new_content != content:
        if not dry_run:
            manifest.write_text(new_content, encoding='utf-8')
        print(f"  Updated: {manifest}")
        return 1
    
    return 0

def main():
    dry_run = "--dry-run" in sys.argv
    
    if dry_run:
        print("=== DRY RUN MODE (不会实际修改文件) ===\n")
    
    print("=== 步骤 1: 收集资源文件 ===")
    files = get_resource_files()
    print(f"找到 {len(files)} 个需要重命名的资源文件\n")
    
    print("=== 步骤 2: 重命名资源文件 ===")
    renamed = rename_resource_files(files, dry_run)
    print(f"重命名了 {len(renamed)} 个文件\n")
    
    print("=== 步骤 3: 更新 Kotlin/Java 文件 ===")
    kt_count = update_kotlin_java_files(renamed, dry_run)
    print(f"更新了 {kt_count} 个 Kotlin/Java 文件\n")
    
    print("=== 步骤 4: 更新 XML 文件 ===")
    xml_count = update_xml_files(renamed, dry_run)
    print(f"更新了 {xml_count} 个 XML 文件\n")
    
    print("=== 步骤 5: 更新 AndroidManifest.xml ===")
    manifest_count = update_manifest(renamed, dry_run)
    print(f"更新了 {manifest_count} 个 Manifest 文件\n")
    
    print("=== 完成 ===")
    print(f"总计: 重命名 {len(renamed)} 个资源, 更新 {kt_count + xml_count + manifest_count} 个文件")
    
    if dry_run:
        print("\n这是 DRY RUN 模式，没有实际修改文件。")
        print("移除 --dry-run 参数以执行实际修改。")

if __name__ == "__main__":
    main()
