#!/usr/bin/env python3
"""
修复 ViewBinding 引用
将旧的 ViewBinding 类名更新为带 ht_ 前缀的新类名
"""

import os
import re
from pathlib import Path

PREFIX = "ht_"
APP_DIR = Path("app")
JAVA_DIR = APP_DIR / "src/main/java"
RES_DIR = APP_DIR / "src/main/res"

def snake_to_pascal(name):
    """将 snake_case 转换为 PascalCase"""
    return ''.join(word.capitalize() for word in name.split('_'))

def get_layout_files():
    """获取所有以 ht_ 开头的布局文件"""
    layouts = []
    for layout_dir in RES_DIR.glob("layout*"):
        if layout_dir.is_dir():
            for f in layout_dir.iterdir():
                if f.is_file() and f.name.startswith(PREFIX) and f.suffix == '.xml':
                    layouts.append(f)
    return layouts

def main():
    print("=== 修复 ViewBinding 引用 ===\n")
    
    # 获取所有带前缀的布局文件
    layouts = get_layout_files()
    print(f"找到 {len(layouts)} 个带 ht_ 前缀的布局文件\n")
    
    # 构建映射: 旧 binding 名 -> 新 binding 名
    binding_map = {}
    for f in layouts:
        new_name = f.stem  # ht_activity_main
        old_name = new_name[len(PREFIX):]  # activity_main
        
        old_binding = snake_to_pascal(old_name) + "Binding"
        new_binding = snake_to_pascal(new_name) + "Binding"
        
        binding_map[old_binding] = new_binding
    
    print(f"生成 {len(binding_map)} 个 ViewBinding 映射\n")
    
    # 更新 Kotlin/Java 文件
    count = 0
    for ext in ["*.kt", "*.java"]:
        for f in JAVA_DIR.rglob(ext):
            content = f.read_text(encoding='utf-8')
            new_content = content
            
            for old_binding, new_binding in binding_map.items():
                # 更新 import 语句和类引用
                pattern = rf'\b{re.escape(old_binding)}\b'
                new_content = re.sub(pattern, new_binding, new_content)
            
            if new_content != content:
                count += 1
                f.write_text(new_content, encoding='utf-8')
                print(f"  Updated: {f}")
    
    print(f"\n更新了 {count} 个文件")

if __name__ == "__main__":
    main()
