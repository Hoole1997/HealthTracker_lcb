#!/usr/bin/env python3
import subprocess
import sys
import re
import os

def get_git_commits(from_tag, to_tag):
    """
    Get all commits between two tags using git log.
    """
    cmd = ["git", "log", "--pretty=format:%s @%cn", f"{from_tag}..{to_tag}"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error getting git log: {result.stderr}", file=sys.stderr)
        return []
    return result.stdout.strip().split('\n')

def parse_commits(commits):
    """
    Parse conventional commits into groups.
    """
    # Categories mapping
    categories = {
        "feat": "🚀 Features",
        "fix": "🐛 Bug Fixes",
        "perf": "⚡ Performance",
        "refactor": "♻️ Refactor",
        "style": "🎨 Style",
        "docs": "📝 Documentation",
        "test": "✅ Tests",
        "chore": "🔧 Chore",
        "build": "🏗️ Build"
    }
    
    grouped = {key: [] for key in categories.values()}
    others = []

    # Regex for conventional commits: type(scope): description
    # e.g., "feat(auth): add login" or "fix: crash on start"
    regex = r"^(\w+)(?:\(([^)]+)\))?:\s*(.+)$"

    for line in commits:
        line = line.strip()
        if not line:
            continue
            
        match = re.match(regex, line)
        if match:
            c_type, c_scope, c_desc = match.groups()
            
            # Map type to category
            category = categories.get(c_type.lower())
            
            if category:
                # Format: "description (scope) @author"
                scope_str = f"**[{c_scope}]** " if c_scope else ""
                formatted_msg = f"{scope_str}{c_desc}"
                grouped[category].append(formatted_msg)
            else:
                # Type recognized but not in main map, add to Others
                others.append(line)
        else:
            # Non-conventional commits - add to Others
            others.append(line)

    return grouped, others

def generate_markdown(result, version):
    """
    Generate Markdown changelog.
    """
    grouped, others = result
    
    output = []
    output.append(f"# 📦 Internal Release {version}")
    output.append("")
    
    # Order of display
    priority = [
        "🚀 Features",
        "🐛 Bug Fixes",
        "⚡ Performance",
        "♻️ Refactor",
        "🏗️ Build",
        "📝 Documentation",
        "✅ Tests",
        "🔧 Chore",
        "🎨 Style",
    ]
    
    has_content = False
    
    for category in priority:
        msgs = grouped.get(category, [])
        if msgs:
            has_content = True
            output.append(f"### {category}")
            for msg in msgs:
                output.append(f"- {msg}")
            output.append("")
            
    # Append Others if any
    if others:
        has_content = True
        output.append("### 🧩 Other Changes")
        for msg in others:
            output.append(f"- {msg}")
        output.append("")
            
    if not has_content:
        output.append("No significant changes found in this release.")
        
    return "\n".join(output)

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 generate_changelog.py <prev_tag> <curr_tag>")
        sys.exit(1)

    prev_tag = sys.argv[1]
    curr_tag = sys.argv[2]
    
    print(f"Generating changelog from {prev_tag} to {curr_tag}...")
    
    commits = get_git_commits(prev_tag, curr_tag)
    grouped = parse_commits(commits)
    changelog = generate_markdown(grouped, curr_tag)
    
    # Output to file
    with open("release_notes.txt", "w") as f:
        f.write(changelog)
        
    print("✅ Changelog generated in release_notes.txt")
    print("-" * 20)
    print(changelog)

if __name__ == "__main__":
    main()
