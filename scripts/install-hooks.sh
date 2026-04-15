#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

git config core.hooksPath .githooks

echo "Configured core.hooksPath -> .githooks"
echo "Local hooks now run the lane harness and dashboard generator checks."
