#!/usr/bin/env bash
# Harmony sprint-gate: blocks Edit/Write/NotebookEdit under platforms/harmony/
# unless docs/plans/sprint-tracker.md contains an Active or Blocked sprint entry.
# Rationale: docs/specs/platform-governance.md (Sprint-Gated Platforms).

set -euo pipefail

read_input() {
    if [ -n "${CLAUDE_TOOL_INPUT:-}" ]; then
        printf '%s' "$CLAUDE_TOOL_INPUT"
    elif [ ! -t 0 ]; then
        cat
    else
        printf ''
    fi
}

extract_path() {
    local input="$1"
    if [ -z "$input" ]; then
        return
    fi
    if command -v jq >/dev/null 2>&1; then
        printf '%s' "$input" | jq -r '.tool_input.file_path // .tool_input.notebook_path // .file_path // .notebook_path // empty' 2>/dev/null
        return
    fi
    local path
    path=$(printf '%s' "$input" | grep -oE '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n1 | sed -E 's/.*"file_path"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/')
    if [ -z "$path" ]; then
        path=$(printf '%s' "$input" | grep -oE '"notebook_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n1 | sed -E 's/.*"notebook_path"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/')
    fi
    printf '%s' "$path"
}

locate_tracker() {
    if [ -f "docs/plans/sprint-tracker.md" ]; then
        printf 'docs/plans/sprint-tracker.md'
        return
    fi
    local root
    root=$(git rev-parse --show-toplevel 2>/dev/null || printf '')
    if [ -n "$root" ] && [ -f "$root/docs/plans/sprint-tracker.md" ]; then
        printf '%s/docs/plans/sprint-tracker.md' "$root"
        return
    fi
    printf ''
}

INPUT=$(read_input)
FILE_PATH=$(extract_path "$INPUT")

# No file_path -> not a file-mutating tool call we gate; allow.
if [ -z "$FILE_PATH" ]; then
    exit 0
fi

case "$FILE_PATH" in
    *"platforms/harmony/"*) ;;
    *) exit 0 ;;
esac

TRACKER=$(locate_tracker)
if [ -z "$TRACKER" ]; then
    echo "Harmony edit blocked: docs/plans/sprint-tracker.md not found. Cannot verify sprint contract." >&2
    exit 2
fi

if grep -qE '^- Status: (Active|Blocked)\b' "$TRACKER"; then
    exit 0
fi

cat >&2 <<'MSG'
Harmony edit blocked: no Active or Blocked sprint in docs/plans/sprint-tracker.md.

Run /sprint to open a sprint contract (HS-NNN) covering this work before
editing platforms/harmony/**. Enforcement: .claude/hooks/harmony-sprint-gate.sh
per docs/specs/platform-governance.md (Sprint-Gated Platforms).
MSG
exit 2
