import json
import os
import re
import sys
import urllib.request
import urllib.error
from pathlib import Path

# Config
CONFIG_FILE = ".figma/config.properties"

def load_config():
    config = {}
    if not os.path.exists(CONFIG_FILE):
        print(f"Error: {CONFIG_FILE} not found")
        sys.exit(1)
    
    with open(CONFIG_FILE, 'r') as f:
        for line in f:
            if '=' in line and not line.strip().startswith('#'):
                key, val = line.strip().split('=', 1)
                config[key.strip()] = val.strip()
    return config

def make_request(url, token):
    headers = {"X-Figma-Token": token}
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode())
    except Exception as e:
        print(f"API Error: {e}", file=sys.stderr)
        sys.exit(1)

def print_node(node, depth=0):
    indent = "  " * depth
    name = node.get("name", "unnamed")
    type = node.get("type")
    id = node.get("id")
    
    # Extract text characters if available
    characters = node.get("characters", "").replace('\n', '\\n')
    
    info = f"{indent}- [{type}] {name}"
    if characters:
        info += f" : \"{characters}\""
    
    print(info)
    
    if "children" in node:
        for child in node["children"]:
            print_node(child, depth + 1)

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 inspect_figma_node.py <FIGMA_URL>")
        sys.exit(1)
        
    url = sys.argv[1]
    config = load_config()
    token = config.get("figma.token")
    
    if not token:
        # Fallback check
        token = config.get("figma_token")
        
    if not token:
        print("Error: No figma.token in config")
        sys.exit(1)

    # Parse URL
    file_key_match = re.search(r"figma\.com/(?:file|design)/([^/?]+)", url)
    node_id_match = re.search(r"node-id=([^&]+)", url)
    
    if not file_key_match or not node_id_match:
        print("Error: Invalid URL. Must contain file key and node-id.")
        sys.exit(1)
        
    file_key = file_key_match.group(1)
    node_id = node_id_match.group(1).replace("-", ":") # API uses colon
    
    print(f"Fetching Node: {node_id} from File: {file_key}...")
    
    api_url = f"https://api.figma.com/v1/files/{file_key}/nodes?ids={node_id}"
    data = make_request(api_url, token)
    
    nodes = data.get("nodes", {})
    root = nodes.get(node_id, {}).get("document")
    
    if root:
        print_node(root)
    else:
        print("Node not found in response.")

if __name__ == "__main__":
    main()
