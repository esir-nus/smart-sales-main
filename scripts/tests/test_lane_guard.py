#!/usr/bin/env python3

import argparse
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.lane_guard import (
    ALLOWED_EVIDENCE_CLASSES,
    LaneContext,
    LaneGuardError,
    ValidationMessage,
    command_ci,
    command_integrate,
    command_pause,
    command_resume,
    matches,
    path_owners,
    read_json,
    validate_paths_for_lane,
    validate_registry_payload,
    write_json,
)


class LaneGuardTests(unittest.TestCase):
    def test_matches_supports_recursive_glob(self) -> None:
        self.assertTrue(matches(["docs/platforms/**"], "docs/platforms/harmony/README.md"))
        self.assertTrue(matches(["app-core/src/main/java/**"], "app-core/src/main/java/com/example/Main.kt"))
        self.assertFalse(matches(["docs/platforms/**"], "docs/specs/platform-governance.md"))

    def test_registry_rejects_duplicate_reserved_pattern(self) -> None:
        registry = {
            "protocol_version": 1,
            "lanes": [
                {"lane_id": "A", "status": "active", "owned_paths": ["docs/specs/**"], "allowed_shared_paths": [], "handoff_path": None},
                {"lane_id": "B", "status": "active", "owned_paths": ["docs/specs/**"], "allowed_shared_paths": [], "handoff_path": None},
            ],
        }
        messages = validate_registry_payload(registry)
        self.assertTrue(any("reserved path collision" in message.text for message in messages))

    def test_paused_lane_requires_existing_handoff(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry = {
                "protocol_version": 1,
                "lanes": [
                    {
                        "lane_id": "DTQ-04",
                        "status": "paused",
                        "owned_paths": ["app-core/src/main/java/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": "handoffs/missing.md",
                    }
                ],
            }
            messages = validate_registry_payload(registry, repo_root=repo_root)
            self.assertTrue(any("references missing handoff" in message.text for message in messages))

    def test_lane_validation_rejects_foreign_owned_path(self) -> None:
        registry = {
            "lanes": [
                {
                    "lane_id": "DTQ-01",
                    "status": "active",
                    "owned_paths": ["app-core/src/main/java/com/example/onboarding/**"],
                    "allowed_shared_paths": [],
                },
                {
                    "lane_id": "DTQ-03",
                    "status": "active",
                    "owned_paths": ["app-core/src/main/java/com/example/connectivity/**"],
                    "allowed_shared_paths": [],
                },
            ]
        }
        lane = registry["lanes"][0]
        messages = validate_paths_for_lane(
            registry,
            lane,
            ["app-core/src/main/java/com/example/connectivity/Bridge.kt"],
            admin_paths=[],
            integration_tree=False,
        )
        self.assertTrue(any("belongs to another active lane" in message.text for message in messages))

    def test_explicit_integration_tree_path_is_honored(self) -> None:
        from scripts.lane_guard import LaneContext
        context = LaneContext(
            repo_root=Path('/repo/worktree-a'),
            git_dir=Path('/repo/worktree-a/.git'),
            worktree_root=Path('/repo/worktree-b'),
            branch='lane/DTQ-06/governance-harness',
            registry_path=Path('/repo/worktree-b/ops/lane-registry.json'),
        )
        self.assertTrue(context.is_integration_tree({'integration_tree': {'path': '/repo/worktree-b'}}))
        self.assertFalse(context.is_integration_tree({'integration_tree': {'path': '/repo/worktree-a'}}))

    def test_integration_tree_rejects_feature_edit(self) -> None:
        registry = {
            "lanes": [],
        }
        messages = validate_paths_for_lane(
            registry,
            lane=None,
            paths=["app-core/src/main/java/com/example/MainActivity.kt"],
            admin_paths=["ops/lane-registry.json"],
            integration_tree=True,
        )
        self.assertEqual([message.level for message in messages], ["error"])
        self.assertIn("integration tree cannot carry feature edits", messages[0].text)

    def test_registry_warns_missing_evidence_class(self) -> None:
        registry = {
            "protocol_version": 1,
            "lanes": [
                {"lane_id": "A", "status": "active", "owned_paths": ["docs/**"], "allowed_shared_paths": [], "handoff_path": None},
            ],
        }
        messages = validate_registry_payload(registry)
        self.assertTrue(any("no evidence_class" in m.text and m.level == "warn" for m in messages))

    def test_registry_rejects_invalid_evidence_class(self) -> None:
        registry = {
            "protocol_version": 1,
            "lanes": [
                {"lane_id": "A", "status": "active", "owned_paths": ["docs/**"], "allowed_shared_paths": [], "handoff_path": None, "evidence_class": "bogus"},
            ],
        }
        messages = validate_registry_payload(registry)
        self.assertTrue(any("invalid evidence_class" in m.text and m.level == "error" for m in messages))

    def test_registry_accepts_valid_evidence_class(self) -> None:
        registry = {
            "protocol_version": 1,
            "lanes": [
                {"lane_id": "A", "status": "active", "owned_paths": ["docs/**"], "allowed_shared_paths": [], "handoff_path": None, "evidence_class": "ui-visible"},
            ],
        }
        messages = validate_registry_payload(registry)
        self.assertFalse(any("evidence_class" in m.text for m in messages))

    def test_deferred_lane_may_omit_evidence_class(self) -> None:
        registry = {
            "protocol_version": 1,
            "lanes": [
                {"lane_id": "A", "status": "deferred", "owned_paths": [], "allowed_shared_paths": [], "handoff_path": None},
            ],
        }
        messages = validate_registry_payload(registry)
        self.assertFalse(any("evidence_class" in m.text for m in messages))

    def test_pause_sets_status_and_handoff(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            handoff_path = repo_root / "handoffs" / "test.md"
            handoff_path.parent.mkdir(parents=True)
            handoff_path.write_text("handoff content")
            registry = {
                "protocol_version": 1,
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "active",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": None,
                        "evidence_class": "contract-test",
                    }
                ],
            }
            write_json(registry_path, registry)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git",
                worktree_root=repo_root,
                branch="lane/T-01/test",
                registry_path=registry_path,
            )
            args = argparse.Namespace(registry=None, lane_id="T-01", handoff_path="handoffs/test.md")
            with patch("scripts.lane_guard.get_context", return_value=context):
                command_pause(args)
            updated = read_json(registry_path)
            lane = updated["lanes"][0]
            self.assertEqual(lane["status"], "paused")
            self.assertEqual(lane["handoff_path"], "handoffs/test.md")

    def test_pause_rejects_missing_handoff_file(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            registry = {
                "protocol_version": 1,
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "active",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": None,
                    }
                ],
            }
            write_json(registry_path, registry)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git",
                worktree_root=repo_root,
                branch="lane/T-01/test",
                registry_path=registry_path,
            )
            args = argparse.Namespace(registry=None, lane_id="T-01", handoff_path="handoffs/missing.md")
            with patch("scripts.lane_guard.get_context", return_value=context):
                with self.assertRaises(LaneGuardError) as cm:
                    command_pause(args)
                self.assertIn("does not exist", str(cm.exception))

    def test_resume_sets_active_and_saves_lease(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            registry = {
                "protocol_version": 1,
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "paused",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": "handoffs/test.md",
                        "evidence_class": "contract-test",
                    }
                ],
            }
            write_json(registry_path, registry)
            git_dir = repo_root / ".git"
            git_dir.mkdir(exist_ok=True)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=git_dir,
                worktree_root=repo_root / "worktree",
                branch="lane/T-01/test",
                registry_path=registry_path,
            )
            args = argparse.Namespace(registry=None, lane_id="T-01")
            with patch("scripts.lane_guard.get_context", return_value=context):
                command_resume(args)
            updated = read_json(registry_path)
            self.assertEqual(updated["lanes"][0]["status"], "active")
            self.assertTrue(context.lease_path.exists())

    def test_integrate_rejects_non_active_lane(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            registry = {
                "protocol_version": 1,
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "deferred",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": None,
                    }
                ],
            }
            write_json(registry_path, registry)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git",
                worktree_root=repo_root,
                branch="lane/T-01/test",
                registry_path=registry_path,
            )
            args = argparse.Namespace(registry=None, lane_id="T-01")
            with patch("scripts.lane_guard.get_context", return_value=context):
                with self.assertRaises(LaneGuardError) as cm:
                    command_integrate(args)
                self.assertIn("must be active or review", str(cm.exception))

    def test_integrate_moves_to_review(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            registry = {
                "protocol_version": 1,
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "active",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": None,
                        "evidence_class": "contract-test",
                    }
                ],
            }
            write_json(registry_path, registry)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git",
                worktree_root=repo_root,
                branch="lane/T-01/test",
                registry_path=registry_path,
            )
            args = argparse.Namespace(registry=None, lane_id="T-01")
            with patch("scripts.lane_guard.get_context", return_value=context), \
                 patch("scripts.lane_guard.get_changed_paths", return_value=[]):
                command_integrate(args)
            updated = read_json(registry_path)
            self.assertEqual(updated["lanes"][0]["status"], "review")

    def test_ci_rejects_unregistered_branch(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            registry = {
                "protocol_version": 1,
                "integration_tree": {"admin_paths": ["ops/**"]},
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "active",
                        "branch": "lane/T-01/test",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": None,
                        "evidence_class": "contract-test",
                    }
                ],
            }
            write_json(registry_path, registry)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git",
                worktree_root=repo_root,
                branch="unknown-branch",
                registry_path=registry_path,
            )
            args = argparse.Namespace(
                registry=None,
                base_ref="abc123",
                head_ref="def456",
                branch="unknown-branch",
            )
            with patch("scripts.lane_guard.get_context", return_value=context), \
                 patch("scripts.lane_guard.get_diff_paths", return_value=["src/Main.kt"]):
                with self.assertRaises(LaneGuardError):
                    command_ci(args)

    def test_ci_accepts_registered_branch_with_owned_paths(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            registry_path = repo_root / "ops" / "lane-registry.json"
            registry = {
                "protocol_version": 1,
                "integration_tree": {"admin_paths": ["ops/**"]},
                "lanes": [
                    {
                        "lane_id": "T-01",
                        "status": "active",
                        "branch": "lane/T-01/test",
                        "owned_paths": ["src/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": None,
                        "evidence_class": "contract-test",
                    }
                ],
            }
            write_json(registry_path, registry)
            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git",
                worktree_root=repo_root,
                branch="lane/T-01/test",
                registry_path=registry_path,
            )
            args = argparse.Namespace(
                registry=None,
                base_ref="abc123",
                head_ref="def456",
                branch="lane/T-01/test",
            )
            with patch("scripts.lane_guard.get_context", return_value=context), \
                 patch("scripts.lane_guard.get_diff_paths", return_value=["src/Main.kt"]):
                result = command_ci(args)
            self.assertEqual(result, 0)


if __name__ == "__main__":
    unittest.main()
