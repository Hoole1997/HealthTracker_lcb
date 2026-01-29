#!/usr/bin/env python3
import sys
import re
import os

def extract_version_from_gradle(gradle_file_path):
    """
    Extracts versionName from build.gradle.kts
    """
    if not os.path.exists(gradle_file_path):
        return None
        
    with open(gradle_file_path, 'r') as f:
        content = f.read()
        # Look for versionName = "1.0.6"
        # Match 'versionName' followed by optional spaces, '=', optional spaces, quote, (capture), quote
        match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(1)
    return None

def suffix_to_int(suffix):
    """
    Converts a suffix like 'A' or 'A_Z' to an integer.
    Treats as Bijective Base-26 (A=1, ..., Z=26, A_A=27, ...)
    Separator '_' is ignored for value calculation, but position matters.
    Actually, let's treat 'A_A' as 'AA' for calculation.
    """
    clean_suffix = suffix.replace('_', '')
    num = 0
    for char in clean_suffix:
        if 'A' <= char <= 'Z':
            num = num * 26 + (ord(char) - ord('A') + 1)
        else:
            return 0 # Invalid
    return num

def int_to_suffix(num):
    """
    Converts an integer back to suffix format (A, Z, A_A, ...).
    """
    if num <= 0:
        return "A"
        
    res = []
    while num > 0:
        num -= 1
        rem = num % 26
        res.append(chr(rem + ord('A')))
        num //= 26
        
    # Reverse to get big-endian
    res = res[::-1]
    
    # Join with underscores
    return "_".join(res)

def get_next_version(tags, version_prefix):
    """
    Given a list of tags and the current version prefix (e.g., '1.0.6'),
    Finds the max suffix and increments it.
    Input tags examples: ['T1.0.6.A', 'T1.0.6.B', 'v1.0.0']
    """
    # Filter tags that match T<version>.<SUFFIX>
    # Regex: ^T1\.0\.6\.([A-Z_]+)$
    # Escape dots in version
    safe_version = re.escape(version_prefix)
    pattern = re.compile(f"^T{safe_version}\.([A-Z_]+)$")
    
    max_val = 0
    
    for tag in tags:
        match = pattern.match(tag)
        if match:
            suffix = match.group(1)
            val = suffix_to_int(suffix)
            if val > max_val:
                max_val = val
                
    # Next value
    next_val = max_val + 1
    return int_to_suffix(next_val)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: version_manager.py <command> [args...]")
        sys.exit(1)
        
    cmd = sys.argv[1]
    
    if cmd == "get_version":
        # python3 version_manager.py get_version app/build.gradle.kts
        if len(sys.argv) < 3:
            print("Error: missing gradle file path")
            sys.exit(1)
        v = extract_version_from_gradle(sys.argv[2])
        if v:
            print(v)
        else:
            sys.exit(1)
            
    elif cmd == "next_suffix":
        # python3 version_manager.py next_suffix 1.0.6 "T1.0.6.A T1.0.6.B"
        if len(sys.argv) < 4:
            print("Error: missing version or tags")
            sys.exit(1)
            
        ver = sys.argv[2]
        tags_str = sys.argv[3]
        tags = tags_str.split()
        
        print(get_next_version(tags, ver))
