#!/bin/bash
# Figma to Compose Helper Wrapper
# This script calls the global figma_helper.py from Windsurf Skills directory

SKILL_SCRIPT="$HOME/.windsurf/skills/figma-to-compose/figma_helper.py"

if [ ! -f "$SKILL_SCRIPT" ]; then
    echo "❌ Error: figma_helper.py not found at $SKILL_SCRIPT"
    echo "   Please ensure the figma-to-compose skill is installed."
    exit 1
fi

python3 "$SKILL_SCRIPT" "$@"
