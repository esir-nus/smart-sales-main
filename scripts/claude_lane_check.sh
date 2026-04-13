#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(git rev-parse --show-toplevel)"
cd "$ROOT_DIR"

REGISTRY="ops/lane-registry.json"

if [ ! -f "$REGISTRY" ]; then
  exit 0
fi

INTEGRATION_PATH="$(python3 -c "import json; print(json.loads(open('$REGISTRY').read()).get('integration_tree',{}).get('path',''))")"

CURRENT_PATH="$(pwd -P)"

if [ "$CURRENT_PATH" = "$INTEGRATION_PATH" ]; then
  exit 0
fi

python3 scripts/lane_guard.py validate-worktree --mode dirty
