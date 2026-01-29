#!/usr/bin/env python3
import subprocess
import sys
import re
import os

def get_git_commits(from_tag, to_tag):
    """
    Get all commits between two tags using git log.
    """
    cmd = ["git", "log", "--pretty=format:%s%n%b%n---COMMIT_DELIMITER---", f"{from_tag}..{to_tag}"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error getting git log: {result.stderr}", file=sys.stderr)
        return []
    # Split by delimiter and filter empty
    raw_commits = result.stdout.split("---COMMIT_DELIMITER---")
    return [c.strip() for c in raw_commits if c.strip()]

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
            
        # Split subject and body
        parts = line.split('\n', 1)
        subject = parts[0].strip()
        body = parts[1].strip() if len(parts) > 1 else ""

        match = re.match(regex, subject)
        if match:
            c_type, c_scope, c_desc = match.groups()
            
            # Map type to category
            category = categories.get(c_type.lower())
            
            if category:
                # Format: "description (scope)"
                scope_str = f"**[{c_scope}]** " if c_scope else ""
                
                # Construct main bullet
                formatted_msg = f"{scope_str}{c_desc}"
                
                # Append body as sub-list if exists
                if body:
                    # Indent body lines
                    body_lines = body.split('\n')
                    for bl in body_lines:
                        bl = bl.strip()
                        if bl.startswith('- ') or bl.startswith('* '):
                            formatted_msg += f"\n  • {bl[2:]}"
                        elif bl:
                            formatted_msg += f"\n  • {bl}"
                            
                grouped[category].append(formatted_msg)
            else:
                others.append(subject)
                if body:
                     others.append(f"  {body}")
        else:
            others.append(subject)
            if body:
                 others.append(f"  {body}")

    return grouped, others

def generate_text(result, version, omitted_count=0):
    """
    Generate Plain Text changelog.
    """
    grouped, others = result
    from datetime import datetime
    current_date = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    output = []
    output.append(f"📦 Internal Release {version}")
    output.append(f"📅 {current_date}")
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
            output.append(f"【 {category} 】")
            for msg in msgs:
                clean_msg = msg.replace("**", "")
                output.append(f"➤ {clean_msg}")
            output.append("")
            
    # Append Others if any
    if others:
        has_content = True
        output.append("【 🧩 Other Changes 】")
        for msg in others:
            clean_msg = msg.replace("**", "")
            output.append(f"➤ {clean_msg}")
        output.append("")
            
    if not has_content:
        output.append("No significant changes found in this release.")
    
    # Append omission warning if needed
    if omitted_count > 0:
        if has_content:
            output.append("...")
        output.append(f"⚠️ ... and {omitted_count} more commits omitted.")
        
    return "\n".join(output)

def main():
    if len(sys.argv) < 3:
        pass # Handle in start

    prev_tag = sys.argv[1]
    curr_tag = sys.argv[2]
    
    # Optional 3rd arg for display name (e.g. if curr_tag is HEAD but we want to show T1.0.7.A)
    display_version = sys.argv[3] if len(sys.argv) > 3 else curr_tag
    
    print(f"Generating changelog from {prev_tag} to {curr_tag} (Title: {display_version})...")
    
    commits = get_git_commits(prev_tag, curr_tag)
    
    # Limit to latest 20 commits
    MAX_COMMITS = 20
    total_commits = len(commits)
    omitted_count = 0
    
    if total_commits > MAX_COMMITS:
        omitted_count = total_commits - MAX_COMMITS
        commits = commits[:MAX_COMMITS]
        print(f"⚠️  Limit applied: Showing 20 of {total_commits} commits.")

    grouped = parse_commits(commits)
    changelog = generate_text(grouped, display_version, omitted_count)
    
    # Output to file
    output_file = "release_notes.txt"
    with open(output_file, "w") as f:
        f.write(changelog)
        
    print(f"✅ Changelog generated in {output_file}")

if __name__ == "__main__":
    main()
