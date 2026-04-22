#!/usr/bin/env bash
# Harmony push preflight: blocks git push to platform/harmony or harmony/*
# unless the worktree, lane diff, sprint contract, and trunk freshness all match governance.

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

extract_command() {
    local input="$1"
    if [ -z "$input" ]; then
        return
    fi
    if command -v jq >/dev/null 2>&1; then
        printf '%s' "$input" | jq -r '.tool_input.command // .command // empty' 2>/dev/null
        return
    fi
    printf '%s' "$input" | grep -oE '"command"[[:space:]]*:[[:space:]]*"([^"\\]|\\.)*"' | head -n1 | sed -E 's/.*"command"[[:space:]]*:[[:space:]]*"(([^"\\]|\\.)*)".*/\1/' | sed 's/\\"/"/g'
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

locate_interface_map() {
    if [ -f "docs/cerb/interface-map.md" ]; then
        printf 'docs/cerb/interface-map.md'
        return
    fi
    local root
    root=$(git rev-parse --show-toplevel 2>/dev/null || printf '')
    if [ -n "$root" ] && [ -f "$root/docs/cerb/interface-map.md" ]; then
        printf '%s/docs/cerb/interface-map.md' "$root"
        return
    fi
    printf ''
}

block() {
    echo "$1" >&2
    exit 2
}

scope_pattern_match() {
    local file="$1"
    local pattern="$2"
    case "$file" in
        $pattern) return 0 ;;
    esac
    if [[ "$pattern" != *"*"* ]]; then
        case "$file" in
            "$pattern"/*) return 0 ;;
        esac
    fi
    return 1
}

is_allowed_lane_path() {
    local file="$1"
    case "$file" in
        platforms/harmony/*) return 0 ;;
        docs/platforms/harmony/*) return 0 ;;
        docs/plans/harmony-tracker.md|docs/plans/sprint-tracker.md) return 0 ;;
        docs/core-flow/*|docs/cerb/*|docs/cerb-ui/*|docs/specs/*) return 0 ;;
        .claude/settings.json|.claude/hooks/harmony-sprint-gate.sh|.claude/hooks/harmony-push-preflight.sh) return 0 ;;
        .codex/skills/ship-harmony/SKILL.md) return 0 ;;
    esac
    return 1
}

INPUT=$(read_input)
COMMAND=$(extract_command "$INPUT")

if [ -z "$COMMAND" ]; then
    exit 0
fi

if ! git rev-parse --show-toplevel >/dev/null 2>&1; then
    exit 0
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf '')

mapfile -t PUSH_CONTEXT < <(python3 - "$COMMAND" "$CURRENT_BRANCH" <<'PY'
import shlex
import subprocess
import sys

command = sys.argv[1]
current_branch = sys.argv[2]

def git(*args):
    return subprocess.run(
        ["git", *args],
        check=False,
        capture_output=True,
        text=True,
    ).stdout.strip()

try:
    tokens = shlex.split(command)
except ValueError:
    print("0")
    print("")
    print("")
    print("")
    raise SystemExit

if len(tokens) < 2 or tokens[0] != "git" or tokens[1] != "push":
    print("0")
    print("")
    print("")
    print("")
    raise SystemExit

escape_reason = ""
filtered = []
i = 2
while i < len(tokens):
    token = tokens[i]
    if token == "--force-parallel":
        if i + 1 < len(tokens):
            escape_reason = tokens[i + 1]
        i += 2
        continue
    filtered.append(token)
    i += 1

remotes = set(filter(None, git("remote").splitlines()))
positionals = [token for token in filtered if not token.startswith("-")]
upstream = git("rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
upstream_remote = ""
upstream_branch = ""
if upstream and "/" in upstream:
    upstream_remote, upstream_branch = upstream.split("/", 1)

remote = ""
refspec = ""
if not positionals:
    remote = upstream_remote
    refspec = upstream_branch or current_branch
elif len(positionals) == 1:
    candidate = positionals[0]
    if candidate in remotes:
        remote = candidate
        refspec = upstream_branch if upstream_remote == candidate and upstream_branch else current_branch
    else:
        remote = upstream_remote or "origin"
        refspec = candidate
else:
    remote = positionals[0]
    refspec = positionals[1]

target = refspec
if ":" in refspec:
    left, right = refspec.split(":", 1)
    target = right or left
if target in ("", "HEAD"):
    target = current_branch
if target.startswith("refs/heads/"):
    target = target[len("refs/heads/"):]

is_target = int(target == "platform/harmony" or target.startswith("harmony/"))
print(is_target)
print(remote)
print(target)
print(escape_reason)
PY
)

IS_TARGET_PUSH=${PUSH_CONTEXT[0]:-0}
TARGET_REMOTE=${PUSH_CONTEXT[1]:-}
TARGET_BRANCH=${PUSH_CONTEXT[2]:-}
ESCAPE_REASON=${PUSH_CONTEXT[3]:-}

if [ "$IS_TARGET_PUSH" != "1" ]; then
    exit 0
fi

TRACKER=$(locate_tracker)
if [ -z "$TRACKER" ]; then
    block "Harmony push blocked: docs/plans/sprint-tracker.md not found. Cannot verify sprint contract."
fi

INTERFACE_MAP=$(locate_interface_map)
if [ -z "$INTERFACE_MAP" ]; then
    block "Harmony push blocked: docs/cerb/interface-map.md not found. Cannot verify lane-pure shared-contract exceptions."
fi

WORKTREE_ROOT=$(git rev-parse --show-toplevel)
COMMON_DIR=$(git rev-parse --git-common-dir)
MAIN_ROOT=$(cd "$(dirname "$COMMON_DIR")" && pwd)

mapfile -t WORKTREE_META < <(python3 - "$WORKTREE_ROOT" <<'PY'
import subprocess
import sys

worktree_root = sys.argv[1]
out = subprocess.run(
    ["git", "worktree", "list", "--porcelain"],
    check=False,
    capture_output=True,
    text=True,
).stdout.splitlines()

current_path = None
current_branch = ""
for line in out + [""]:
    if line.startswith("worktree "):
        if current_path == worktree_root:
            print(current_branch)
            raise SystemExit
        current_path = line.split(" ", 1)[1]
        current_branch = ""
    elif line.startswith("branch "):
        ref = line.split(" ", 1)[1]
        if ref.startswith("refs/heads/"):
            current_branch = ref[len("refs/heads/"):]
        else:
            current_branch = ref
    elif line == "":
        if current_path == worktree_root:
            print(current_branch)
            raise SystemExit
        current_path = None
        current_branch = ""
print("")
PY
)

ATTACHED_BRANCH=${WORKTREE_META[0]:-}

if [ "$WORKTREE_ROOT" = "$MAIN_ROOT" ] || [ -z "$ATTACHED_BRANCH" ] || [[ ! "$ATTACHED_BRANCH" =~ ^(platform/harmony|harmony/.+)$ ]]; then
    cat >&2 <<'MSG'
Harmony push blocked: current cwd is not a dedicated Harmony linked worktree on platform/harmony or harmony/*.

Create one first:
git worktree add -b harmony/<name> .worktrees/<name> origin/platform/harmony
cp local.properties .worktrees/<name>/local.properties
MSG
    exit 2
fi

DIRTY_STATUS=$(git status --porcelain)
if [ -n "$DIRTY_STATUS" ]; then
    cat >&2 <<'MSG'
Harmony push blocked: worktree is dirty.

Remediation: stash the residue, commit it to a docs lane, or /abandon it before pushing Harmony.
MSG
    exit 2
fi

if [ -n "$ESCAPE_REASON" ]; then
    echo "Harmony push preflight: bypassing checks 3-6 via --force-parallel: $ESCAPE_REASON" >&2
    exit 0
fi

mapfile -t DIFF_FILES < <(git diff --name-only origin/platform/harmony...HEAD)

INVALID_LANE_FILES=()
for file in "${DIFF_FILES[@]}"; do
    [ -z "$file" ] && continue
    if ! is_allowed_lane_path "$file"; then
        INVALID_LANE_FILES+=("$file")
    fi
done

if [ ${#INVALID_LANE_FILES[@]} -gt 0 ]; then
    {
        echo "Harmony push blocked: diff vs origin/platform/harmony is not lane-pure."
        echo
        echo "Out-of-lane paths:"
        printf ' - %s\n' "${INVALID_LANE_FILES[@]}"
        echo
        echo "Remediation: split the branch. Keep Harmony platform files here; move Android/shared residue to a docs lane or Android branch."
    } >&2
    exit 2
fi

mapfile -t SPRINT_CONTEXT < <(python3 - "$TRACKER" "$CURRENT_BRANCH" <<'PY'
import pathlib
import re
import sys

tracker_path = pathlib.Path(sys.argv[1])
branch_name = sys.argv[2]
text = tracker_path.read_text(encoding="utf-8")
entries = re.split(r'(?m)^### ', text)

for raw in entries:
    raw = raw.strip()
    if not raw:
        continue
    entry = raw if raw.startswith("### ") else f"### {raw}"
    branch_match = re.search(r'^- Branch: `([^`]+)`', entry, re.M)
    status_match = re.search(r'^- Status: (Active|Blocked)\b', entry, re.M)
    if not branch_match or branch_match.group(1) != branch_name or not status_match:
        continue

    print("FOUND")
    print(status_match.group(1))
    scope_match = re.search(r'^\*\*Scope\*\*:\n(.*?)(?=\n\*\*|\n### |\Z)', entry, re.S | re.M)
    if scope_match:
        for line in scope_match.group(1).splitlines():
            bullet = re.match(r'^\s*-\s+(.*)$', line)
            if bullet:
                print(bullet.group(1).strip())
    raise SystemExit

print("MISSING")
PY
)

if [ "${SPRINT_CONTEXT[0]:-MISSING}" != "FOUND" ]; then
    cat >&2 <<'MSG'
Harmony push blocked: no Active or Blocked sprint contract matches the current branch in docs/plans/sprint-tracker.md.

Remediation: run /sprint to open or activate an HS-NNN contract for this branch before pushing.
MSG
    exit 2
fi

SCOPE_PATTERNS=()
if [ ${#SPRINT_CONTEXT[@]} -gt 2 ]; then
    for raw_pattern in "${SPRINT_CONTEXT[@]:2}"; do
        pattern=${raw_pattern#\`}
        pattern=${pattern%\`}
        pattern=${pattern%% \(*}
        pattern=${pattern%%$'\r'}
        pattern=${pattern## }
        pattern=${pattern%% }
        if [ -n "$pattern" ]; then
            SCOPE_PATTERNS+=("$pattern")
        fi
    done
fi

OUT_OF_SCOPE_FILES=()
for file in "${DIFF_FILES[@]}"; do
    [ -z "$file" ] && continue
    matched=0
    for pattern in "${SCOPE_PATTERNS[@]}"; do
        if scope_pattern_match "$file" "$pattern"; then
            matched=1
            break
        fi
    done
    if [ "$matched" -ne 1 ]; then
        OUT_OF_SCOPE_FILES+=("$file")
    fi
done

if [ ${#OUT_OF_SCOPE_FILES[@]} -gt 0 ]; then
    {
        echo "Harmony push blocked: diff vs origin/platform/harmony exceeds the matching sprint Scope bullets."
        echo
        echo "Out-of-scope paths:"
        printf ' - %s\n' "${OUT_OF_SCOPE_FILES[@]}"
        echo
        echo "Remediation: update the sprint Scope bullets first in the same commit, or drop the out-of-scope files."
    } >&2
    exit 2
fi

BEHIND_COUNT=$(git rev-list --count HEAD..origin/platform/harmony)
if [ "$BEHIND_COUNT" != "0" ]; then
    echo "Harmony push blocked: branch is $BEHIND_COUNT commit(s) behind origin/platform/harmony. Remediation: git rebase origin/platform/harmony before push (never merge)." >&2
    exit 2
fi
