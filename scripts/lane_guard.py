#!/usr/bin/env python3

import argparse
import fnmatch
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Optional, Sequence, Tuple


RESERVED_STATUSES = {"active", "paused", "review"}
ALLOWED_STATUSES = {"active", "paused", "review", "accepted", "deferred"}
DEFAULT_REGISTRY = "ops/lane-registry.json"
LEASE_RELATIVE_PATH = "smart-sales/current-lane.json"
IGNORED_DIR_MARKERS = ("/__pycache__/", ".pytest_cache/", "/.mypy_cache/")
IGNORED_SUFFIXES = (".pyc",)


class LaneGuardError(Exception):
    pass


@dataclass
class ValidationMessage:
    level: str
    text: str


@dataclass
class LaneState:
    lane: dict
    staged_paths: List[str]
    dirty_paths: List[str]
    generated_message: Optional[str]


@dataclass
class LaneContext:
    repo_root: Path
    git_dir: Path
    worktree_root: Path
    branch: str
    registry_path: Path

    @property
    def lease_path(self) -> Path:
        return self.git_dir / LEASE_RELATIVE_PATH

    def is_integration_tree(self, registry: dict) -> bool:
        integration_path = registry.get("integration_tree", {}).get("path")
        if integration_path:
            return self.worktree_root.resolve() == Path(integration_path).resolve()
        return self.worktree_root.resolve() == self.repo_root.resolve()


def run_git(args: Sequence[str], cwd: Optional[Path] = None, check: bool = True) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=str(cwd) if cwd else None,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        check=False,
    )
    if check and result.returncode != 0:
        raise LaneGuardError(result.stderr.strip() or f"git {' '.join(args)} failed")
    return result.stdout.strip()


def get_context(registry_arg: Optional[str] = None) -> LaneContext:
    repo_root = Path(run_git(["rev-parse", "--show-toplevel"]))
    git_dir = Path(run_git(["rev-parse", "--absolute-git-dir"]))
    worktree_root = Path(os.getcwd()).resolve()
    branch = run_git(["symbolic-ref", "--quiet", "--short", "HEAD"], check=False) or "DETACHED"
    registry_path = repo_root / (registry_arg or DEFAULT_REGISTRY)
    return LaneContext(repo_root=repo_root, git_dir=git_dir, worktree_root=worktree_root, branch=branch, registry_path=registry_path)


def read_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise LaneGuardError(f"Missing JSON file: {path}") from exc
    except json.JSONDecodeError as exc:
        raise LaneGuardError(f"Invalid JSON in {path}: {exc}") from exc


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def normalize_path(value: str) -> str:
    return value.replace("\\", "/").strip("/")


def should_ignore_path(path: str) -> bool:
    normalized = normalize_path(path)
    if any(marker in f"/{normalized}/" for marker in IGNORED_DIR_MARKERS):
        return True
    return normalized.endswith(IGNORED_SUFFIXES)


def matches(patterns: Iterable[str], candidate: str) -> bool:
    candidate = normalize_path(candidate)
    for raw_pattern in patterns:
        pattern = normalize_path(raw_pattern)
        if fnmatch.fnmatch(candidate, pattern):
            return True
        if pattern.endswith("/**"):
            prefix = pattern[:-3]
            if candidate == prefix or candidate.startswith(prefix + "/"):
                return True
    return False


def lane_map(registry: dict) -> dict:
    return {lane["lane_id"]: lane for lane in registry.get("lanes", [])}


def load_lease(context: LaneContext) -> Optional[dict]:
    if not context.lease_path.exists():
        return None
    return read_json(context.lease_path)


def save_lease(context: LaneContext, lane: dict) -> None:
    payload = {
        "lane_id": lane["lane_id"],
        "branch": lane.get("branch"),
        "recommended_worktree": lane.get("recommended_worktree"),
        "repo_root": str(context.repo_root),
        "attached_from": str(context.worktree_root),
    }
    write_json(context.lease_path, payload)


def clear_lease(context: LaneContext) -> None:
    if context.lease_path.exists():
        context.lease_path.unlink()


def get_changed_paths(mode: str, cwd: Path) -> List[str]:
    outputs: List[str] = []
    if mode in {"dirty", "all"}:
        outputs.append(run_git(["status", "--porcelain"], cwd=cwd))
    if mode == "staged":
        outputs.append(run_git(["diff", "--cached", "--name-only"], cwd=cwd))
    if mode not in {"dirty", "staged", "all"}:
        raise LaneGuardError(f"Unsupported mode: {mode}")

    files: List[str] = []
    if mode == "staged":
        files = [normalize_path(line) for line in outputs[0].splitlines() if line.strip()]
    else:
        for line in outputs[0].splitlines():
            if not line:
                continue
            path = line[3:] if not line.startswith("?? ") else line[3:]
            if " -> " in path:
                path = path.split(" -> ", 1)[1]
            files.append(normalize_path(path))
    deduped = sorted(set(path for path in files if path and not should_ignore_path(path)))
    return deduped


def get_diff_paths(base_ref: str, head_ref: str, cwd: Path) -> List[str]:
    merge_base = run_git(["merge-base", base_ref, head_ref], cwd=cwd)
    output = run_git(["diff", "--name-only", merge_base, head_ref], cwd=cwd)
    return sorted(set(normalize_path(line) for line in output.splitlines() if line.strip()))


def path_owners(registry: dict, path: str, include_non_reserved: bool = True) -> List[dict]:
    owners = []
    for lane in registry.get("lanes", []):
        if not include_non_reserved and lane.get("status") not in RESERVED_STATUSES:
            continue
        all_patterns = list(lane.get("owned_paths", [])) + list(lane.get("allowed_shared_paths", []))
        if matches(all_patterns, path):
            owners.append(lane)
    return owners


def validate_lane_runtime(context: LaneContext, registry: dict, require_staged: bool = False) -> LaneState:
    messages = validate_registry_payload(registry, repo_root=context.repo_root)
    lease = load_lease(context)
    lane = None
    if context.is_integration_tree(registry):
        messages.append(ValidationMessage("error", "integration tree cannot run lane-local commit/push commands"))
    if not lease:
        messages.append(ValidationMessage("error", "no lane lease is attached to this worktree"))
    else:
        lane = lane_map(registry).get(lease.get("lane_id"))
        if not lane:
            messages.append(ValidationMessage("error", f"lease references unknown lane {lease.get('lane_id')}"))
        else:
            if lane.get("branch") and lane["branch"] != context.branch:
                messages.append(
                    ValidationMessage(
                        "error",
                        f"lease lane {lane['lane_id']} expects branch {lane['branch']} but current branch is {context.branch}",
                    )
                )
            if lane.get("status") not in {"active", "review"}:
                messages.append(
                    ValidationMessage(
                        "error",
                        f"lane {lane['lane_id']} must be active or review before commit/push, current status is {lane.get('status')}",
                    )
                )

    dirty_paths = get_changed_paths("dirty", cwd=context.worktree_root)
    staged_paths = get_changed_paths("staged", cwd=context.worktree_root)
    admin_paths = registry.get("integration_tree", {}).get("admin_paths", [])
    messages.extend(validate_paths_for_lane(registry, lane, dirty_paths, admin_paths, integration_tree=False))
    messages.extend(validate_paths_for_lane(registry, lane, staged_paths, admin_paths, integration_tree=False))

    if require_staged and not staged_paths:
        messages.append(ValidationMessage("error", "no staged files found for lane commit"))

    require_clean(messages)
    assert lane is not None
    generated_message = generate_commit_message(lane, staged_paths) if staged_paths else None
    return LaneState(
        lane=lane,
        staged_paths=staged_paths,
        dirty_paths=dirty_paths,
        generated_message=generated_message,
    )


def validate_registry_payload(registry: dict, repo_root: Optional[Path] = None) -> List[ValidationMessage]:
    messages: List[ValidationMessage] = []
    if registry.get("protocol_version") != 1:
        messages.append(ValidationMessage("error", "lane registry must declare protocol_version 1"))

    lanes = registry.get("lanes", [])
    if not isinstance(lanes, list) or not lanes:
        messages.append(ValidationMessage("error", "lane registry must contain a non-empty lanes array"))
        return messages

    seen_ids = set()
    reserved_paths: List[Tuple[str, str]] = []
    for lane in lanes:
        lane_id = lane.get("lane_id")
        if not lane_id:
            messages.append(ValidationMessage("error", "lane entry is missing lane_id"))
            continue
        if lane_id in seen_ids:
            messages.append(ValidationMessage("error", f"duplicate lane_id: {lane_id}"))
        seen_ids.add(lane_id)

        status = lane.get("status")
        if status not in ALLOWED_STATUSES:
            messages.append(ValidationMessage("error", f"lane {lane_id} has unsupported status {status!r}"))

        owned_paths = lane.get("owned_paths", [])
        if status in RESERVED_STATUSES and not owned_paths:
            messages.append(ValidationMessage("error", f"lane {lane_id} is {status} but owned_paths is empty"))

        handoff_path = lane.get("handoff_path")
        if status == "paused" and not handoff_path:
            messages.append(ValidationMessage("error", f"lane {lane_id} is paused but has no handoff_path"))

        if repo_root and handoff_path and not (repo_root / handoff_path).exists():
            messages.append(ValidationMessage("error", f"lane {lane_id} references missing handoff {handoff_path}"))

        branch = lane.get("branch")
        if branch is not None and not isinstance(branch, str):
            messages.append(ValidationMessage("error", f"lane {lane_id} branch must be a string or null"))

        if status in RESERVED_STATUSES:
            for pattern in owned_paths:
                reserved_paths.append((lane_id, pattern))

    for index, (left_lane, left_pattern) in enumerate(reserved_paths):
        for right_lane, right_pattern in reserved_paths[index + 1 :]:
            if left_lane == right_lane:
                continue
            if left_pattern == right_pattern:
                messages.append(
                    ValidationMessage(
                        "error",
                        f"reserved path collision: {left_pattern} is owned by both {left_lane} and {right_lane}",
                    )
                )
    return messages


def validate_paths_for_lane(registry: dict, lane: dict, paths: Sequence[str], admin_paths: Sequence[str], integration_tree: bool) -> List[ValidationMessage]:
    messages: List[ValidationMessage] = []
    lane_id = lane.get("lane_id") if lane else None

    for path in paths:
        if integration_tree:
            if not matches(admin_paths, path):
                messages.append(
                    ValidationMessage(
                        "error",
                        f"integration tree cannot carry feature edits: {path}",
                    )
                )
            continue

        if lane is None:
            messages.append(ValidationMessage("error", f"no lane lease is attached, but worktree contains {path}"))
            continue

        owned_patterns = list(lane.get("owned_paths", [])) + list(lane.get("allowed_shared_paths", []))
        if not matches(owned_patterns, path):
            owners = [owner["lane_id"] for owner in path_owners(registry, path, include_non_reserved=False) if owner["lane_id"] != lane_id]
            if owners:
                messages.append(
                    ValidationMessage(
                        "error",
                        f"path {path} belongs to another active lane: {', '.join(sorted(owners))}",
                    )
                )
            else:
                messages.append(
                    ValidationMessage(
                        "error",
                        f"path {path} is outside lane {lane_id} owned_paths/allowed_shared_paths",
                    )
                )
    return messages


def human_lane_title(lane: dict) -> str:
    return re.sub(r"\s+", " ", lane.get("title", lane["lane_id"]).strip()).lower()


def is_control_plane_path(path: str) -> bool:
    control_plane_prefixes = (
        ".agent/",
        "agent/",
        ".claude/",
        "claude/",
        ".github/",
        "github/",
        ".githooks/",
        "githooks/",
        "ops/",
        "scripts/",
        "docs/",
        "handoffs/",
    )
    return path in {"CLAUDE.md", ".claude", ".agent", ".github", ".githooks", "ops", "scripts"} or path.startswith(control_plane_prefixes)


def generate_commit_message(lane: dict, staged_paths: Sequence[str], explicit_message: Optional[str] = None) -> str:
    if explicit_message:
        return explicit_message.strip()
    if not staged_paths:
        raise LaneGuardError("cannot generate a commit message from an empty staged set")

    lane_title = human_lane_title(lane)
    all_docs = all(path.startswith("docs/") or path.startswith("handoffs/") for path in staged_paths)
    all_control_plane = all(is_control_plane_path(path) for path in staged_paths)

    if all_control_plane and lane["lane_id"] == "DTQ-06":
        summary = "update lane harness governance"
    elif all_docs:
        summary = f"sync {lane_title} docs"
    else:
        summary = f"sync {lane_title} changes"
    return f"{lane['lane_id']}: {summary}"


def find_attached_lane(context: LaneContext, registry: dict) -> dict:
    lease = load_lease(context)
    if not lease:
        raise LaneGuardError("no lane lease is attached to this worktree")
    lane = lane_map(registry).get(lease.get("lane_id"))
    if not lane:
        raise LaneGuardError(f"lease references unknown lane {lease.get('lane_id')}")
    if lane.get("branch") and lane["branch"] != context.branch:
        raise LaneGuardError(
            f"lease lane {lane['lane_id']} expects branch {lane['branch']} but current branch is {context.branch}"
        )
    return lane


def collect_lane_state(
    context: LaneContext,
    registry: dict,
    explicit_message: Optional[str] = None,
    require_staged: bool = True,
) -> LaneState:
    messages = validate_registry_payload(registry, repo_root=context.repo_root)
    lane = find_attached_lane(context, registry)
    if context.is_integration_tree(registry):
        messages.append(ValidationMessage("error", "integration tree cannot run lane commit/push commands"))
    if lane.get("status") not in {"active", "review"}:
        messages.append(
            ValidationMessage(
                "error",
                f"lane {lane['lane_id']} must be active or review before commit/push, current status is {lane.get('status')}",
            )
        )

    admin_paths = registry.get("integration_tree", {}).get("admin_paths", [])
    dirty_paths = get_changed_paths("dirty", cwd=context.worktree_root)
    staged_paths = get_changed_paths("staged", cwd=context.worktree_root)
    messages.extend(validate_paths_for_lane(registry, lane, dirty_paths, admin_paths, integration_tree=False))
    messages.extend(validate_paths_for_lane(registry, lane, staged_paths, admin_paths, integration_tree=False))
    if require_staged and not staged_paths:
        messages.append(ValidationMessage("error", "no staged files found for this lane"))
    require_clean(messages)
    generated_message = None
    if staged_paths or explicit_message:
        generated_message = generate_commit_message(lane, staged_paths, explicit_message)

    return LaneState(
        lane=lane,
        staged_paths=staged_paths,
        dirty_paths=dirty_paths,
        generated_message=generated_message,
    )


def lane_state_payload(context: LaneContext, registry: dict, state: LaneState) -> dict:
    upstream = run_git(["rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}"], cwd=context.worktree_root, check=False)
    return {
        "worktree_path": str(context.worktree_root),
        "branch": context.branch,
        "lane_id": state.lane["lane_id"],
        "lane_title": state.lane.get("title"),
        "lane_status": state.lane.get("status"),
        "integration_tree": context.is_integration_tree(registry),
        "staged_files": state.staged_paths,
        "dirty_files": state.dirty_paths,
        "can_commit": True,
        "can_push": True,
        "generated_message": state.generated_message,
        "upstream": upstream or None,
    }


def print_messages(messages: Sequence[ValidationMessage]) -> None:
    for message in messages:
        prefix = "ERROR" if message.level == "error" else "WARN"
        print(f"[{prefix}] {message.text}")


def require_clean(messages: Sequence[ValidationMessage]) -> None:
    print_messages(messages)
    if any(message.level == "error" for message in messages):
        raise LaneGuardError("lane guard validation failed")


def command_validate_registry(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    messages = validate_registry_payload(registry, repo_root=context.repo_root)
    require_clean(messages)
    print(f"lane registry ok: {context.registry_path}")
    return 0


def command_validate_worktree(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    messages = validate_registry_payload(registry, repo_root=context.repo_root)
    lease = load_lease(context)
    lane = None
    if lease:
        lane = lane_map(registry).get(lease.get("lane_id"))
        if not lane:
            messages.append(ValidationMessage("error", f"lease references unknown lane {lease.get('lane_id')}"))
        elif lane.get("branch") and lane["branch"] != context.branch:
            messages.append(
                ValidationMessage(
                    "error",
                    f"lease lane {lane['lane_id']} expects branch {lane['branch']} but current branch is {context.branch}",
                )
            )

    paths = get_changed_paths(args.mode, cwd=context.worktree_root)
    admin_paths = registry.get("integration_tree", {}).get("admin_paths", [])
    messages.extend(validate_paths_for_lane(registry, lane, paths, admin_paths, context.is_integration_tree(registry)))
    require_clean(messages)
    lane_label = lane["lane_id"] if lane else "integration-tree"
    print(f"lane worktree ok: {lane_label} ({args.mode}, {len(paths)} paths checked)")
    return 0


def command_status(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    state = collect_lane_state(context, registry, explicit_message=args.message, require_staged=False)
    payload = lane_state_payload(context, registry, state)
    if args.json:
        print(json.dumps(payload, indent=2, ensure_ascii=True))
        return 0

    print(f"worktree: {payload['worktree_path']}")
    print(f"branch: {payload['branch']}")
    print(f"lane: {payload['lane_id']} ({payload['lane_title']})")
    print(f"status: {payload['lane_status']}")
    print(f"staged: {len(payload['staged_files'])} file(s)")
    print(f"dirty: {len(payload['dirty_files'])} path(s)")
    print(f"message: {payload['generated_message'] or '(no staged files)'}")
    push_target = payload["upstream"] or f"origin/{payload['branch']}"
    print(f"push target: {push_target}")
    return 0


def command_report_collisions(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    if args.path:
        paths = [normalize_path(item) for item in args.path]
    else:
        paths = get_changed_paths("dirty", cwd=context.worktree_root)
    collisions = []
    for path in paths:
        owners = [lane["lane_id"] for lane in path_owners(registry, path, include_non_reserved=False)]
        if len(owners) > 1:
            collisions.append((path, owners))
    if not collisions:
        print("no dirty-tree collisions found")
        return 0
    for path, owners in collisions:
        print(f"collision: {path} -> {', '.join(owners)}")
    return 1


def command_attach(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    lane = lane_map(registry).get(args.lane_id)
    if not lane:
        raise LaneGuardError(f"unknown lane: {args.lane_id}")
    if context.is_integration_tree(registry):
        raise LaneGuardError("refusing to attach a feature lane inside the integration tree")
    save_lease(context, lane)
    print(f"attached lane {args.lane_id} to {context.worktree_root}")
    return 0


def command_pause(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    lanes = lane_map(registry)
    lane = lanes.get(args.lane_id)
    if not lane:
        raise LaneGuardError(f"unknown lane: {args.lane_id}")
    handoff_path = normalize_path(args.handoff_path)
    if lane.get("handoff_path") and lane["handoff_path"] != handoff_path:
        raise LaneGuardError(f"lane {args.lane_id} already points to handoff {lane['handoff_path']}")
    if not (context.repo_root / handoff_path).exists():
        raise LaneGuardError(f"handoff file does not exist: {handoff_path}")
    lane["status"] = "paused"
    lane["handoff_path"] = handoff_path
    write_json(context.registry_path, registry)
    print(f"paused lane {args.lane_id} with handoff {handoff_path}")
    return 0


def command_resume(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    lane = lane_map(registry).get(args.lane_id)
    if not lane:
        raise LaneGuardError(f"unknown lane: {args.lane_id}")
    if lane.get("status") == "paused":
        lane["status"] = "active"
        write_json(context.registry_path, registry)
    save_lease(context, lane)
    print(f"resumed lane {args.lane_id}")
    return 0


def command_integrate(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    lane = lane_map(registry).get(args.lane_id)
    if not lane:
        raise LaneGuardError(f"unknown lane: {args.lane_id}")
    if lane.get("status") not in {"active", "review"}:
        raise LaneGuardError(f"lane {args.lane_id} must be active or review before integrate")
    paths = get_changed_paths("dirty", cwd=context.worktree_root)
    messages = validate_registry_payload(registry, repo_root=context.repo_root)
    admin_paths = registry.get("integration_tree", {}).get("admin_paths", [])
    messages.extend(validate_paths_for_lane(registry, lane, paths, admin_paths, integration_tree=False))
    require_clean(messages)
    lane["status"] = "review"
    write_json(context.registry_path, registry)
    print(f"lane {args.lane_id} marked review-ready")
    return 0


def command_ci(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    messages = validate_registry_payload(registry, repo_root=context.repo_root)
    branch = args.branch or os.environ.get("GITHUB_HEAD_REF") or context.branch
    paths = get_diff_paths(args.base_ref, args.head_ref, cwd=context.repo_root)
    admin_paths = registry.get("integration_tree", {}).get("admin_paths", [])
    lanes = lane_map(registry)
    lane = next((item for item in lanes.values() if item.get("branch") == branch), None)

    if lane:
        messages.extend(validate_paths_for_lane(registry, lane, paths, admin_paths, integration_tree=False))
    else:
        for path in paths:
            if not matches(admin_paths, path):
                messages.append(
                    ValidationMessage(
                        "error",
                        f"branch {branch} is not registered to a lane, but changes feature path {path}",
                    )
                )
    require_clean(messages)
    print(f"ci lane validation ok for branch {branch} ({len(paths)} changed paths)")
    return 0


def push_target(context: LaneContext) -> Tuple[List[str], str]:
    upstream = run_git(["rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}"], cwd=context.worktree_root, check=False)
    if upstream:
        return ["push"], upstream
    return ["push", "-u", "origin", context.branch], f"origin/{context.branch}"


def command_commit(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    state = collect_lane_state(context, registry, explicit_message=args.message, require_staged=True)
    print(f"[lane-ship] lane {state.lane['lane_id']} on {context.branch}")
    print(f"[lane-ship] staged {len(state.staged_paths)} file(s)")
    print(f"[lane-ship] commit message: {state.generated_message}")
    run_git(["commit", "-m", state.generated_message], cwd=context.worktree_root)
    print(f"lane commit created on {context.branch}")
    return 0


def command_push(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    state = collect_lane_state(context, registry, explicit_message=args.message, require_staged=False)
    del state
    git_args, target = push_target(context)
    print(f"[lane-ship] lane push target: {target}")
    run_git(git_args, cwd=context.worktree_root)
    print(f"lane pushed to {target}")
    return 0


def command_ship(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    state = collect_lane_state(context, registry, explicit_message=args.message, require_staged=True)
    git_args, target = push_target(context)
    print(f"[lane-ship] worktree: {context.worktree_root}")
    print(f"[lane-ship] branch: {context.branch}")
    print(f"[lane-ship] lane: {state.lane['lane_id']} ({state.lane.get('title')})")
    print(f"[lane-ship] staged: {len(state.staged_paths)} file(s)")
    print(f"[lane-ship] commit message: {state.generated_message}")
    print(f"[lane-ship] push target: {target}")
    run_git(["commit", "-m", state.generated_message], cwd=context.worktree_root)
    run_git(git_args, cwd=context.worktree_root)
    print(f"lane ship completed for {state.lane['lane_id']}")
    return 0


def command_create(args: argparse.Namespace) -> int:
    context = get_context(args.registry)
    registry = read_json(context.registry_path)
    lanes = registry.setdefault("lanes", [])
    if any(lane.get("lane_id") == args.lane_id for lane in lanes):
        raise LaneGuardError(f"lane already exists: {args.lane_id}")
    branch = args.branch or f"lane/{args.lane_id}/{args.slug}"
    worktree = args.worktree or str((context.repo_root.parent / f"lane-worktrees/{args.lane_id}-{args.slug}").resolve())
    handoff = args.handoff or f"handoffs/{args.lane_id.lower()}-{args.slug}.md"
    lane = {
        "lane_id": args.lane_id,
        "title": args.title or args.slug.replace("-", " "),
        "status": "active",
        "work_class": args.work_class,
        "branch": branch,
        "recommended_worktree": worktree,
        "owned_paths": [normalize_path(item) for item in args.paths],
        "allowed_shared_paths": [normalize_path(item) for item in args.allow],
        "handoff_path": handoff,
        "tracker_refs": ["docs/plans/dirty-tree-quarantine.md", "docs/plans/tracker.md"],
        "alignment": "Both pending",
    }
    lanes.append(lane)
    write_json(context.registry_path, registry)
    subprocess.run(["git", "worktree", "add", "-b", branch, worktree], cwd=str(context.repo_root), check=True)
    target_context = LaneContext(
        repo_root=context.repo_root,
        git_dir=Path(run_git(["rev-parse", "--absolute-git-dir"], cwd=Path(worktree))),
        worktree_root=Path(worktree),
        branch=branch,
        registry_path=context.registry_path,
    )
    save_lease(target_context, lane)
    print(f"created lane {args.lane_id} on branch {branch}")
    print(f"worktree: {worktree}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Smart Sales lane harness validator")
    parser.add_argument("--registry", default=None, help="Path to the lane registry relative to repo root")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("validate-registry")

    validate_worktree = subparsers.add_parser("validate-worktree")
    validate_worktree.add_argument("--mode", choices=["dirty", "staged", "all"], default="dirty")

    status = subparsers.add_parser("status")
    status.add_argument("--json", action="store_true")
    status.add_argument("--message", default=None)

    report = subparsers.add_parser("report-collisions")
    report.add_argument("path", nargs="*")

    attach = subparsers.add_parser("attach")
    attach.add_argument("lane_id")

    pause = subparsers.add_parser("pause")
    pause.add_argument("lane_id")
    pause.add_argument("handoff_path")

    resume = subparsers.add_parser("resume")
    resume.add_argument("lane_id")

    integrate = subparsers.add_parser("integrate")
    integrate.add_argument("lane_id")

    ci = subparsers.add_parser("validate-ci")
    ci.add_argument("--base-ref", required=True)
    ci.add_argument("--head-ref", default="HEAD")
    ci.add_argument("--branch", default=None)

    commit = subparsers.add_parser("commit")
    commit.add_argument("--message", default=None)

    push = subparsers.add_parser("push")
    push.add_argument("--message", default=None)

    ship = subparsers.add_parser("ship")
    ship.add_argument("--message", default=None)

    create = subparsers.add_parser("create")
    create.add_argument("lane_id")
    create.add_argument("slug")
    create.add_argument("--title", default=None)
    create.add_argument("--work-class", default="shared-contract")
    create.add_argument("--branch", default=None)
    create.add_argument("--worktree", default=None)
    create.add_argument("--handoff", default=None)
    create.add_argument("--paths", nargs="+", required=True)
    create.add_argument("--allow", nargs="*", default=[])

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    command_map = {
        "validate-registry": command_validate_registry,
        "validate-worktree": command_validate_worktree,
        "status": command_status,
        "report-collisions": command_report_collisions,
        "attach": command_attach,
        "pause": command_pause,
        "resume": command_resume,
        "integrate": command_integrate,
        "validate-ci": command_ci,
        "commit": command_commit,
        "push": command_push,
        "ship": command_ship,
        "create": command_create,
    }
    try:
        return command_map[args.command](args)
    except LaneGuardError as exc:
        print(f"lane guard error: {exc}", file=sys.stderr)
        return 1
    except subprocess.CalledProcessError as exc:
        print(f"lane guard command failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
