#!/usr/bin/env python3

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.lane_guard import LaneContext, collect_lane_state, generate_commit_message, matches, validate_paths_for_lane, validate_registry_payload


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

    def test_commit_message_prefers_lane_harness_governance(self) -> None:
        lane = {"lane_id": "DTQ-06", "title": "Governance, trackers, and repo guardrails"}
        message = generate_commit_message(
            lane,
            ["ops/lane-registry.json", "scripts/lane_guard.py", "docs/sops/lane-worktree-governance.md"],
        )
        self.assertEqual(message, "DTQ-06: update lane harness governance")

    def test_commit_message_scopes_docs_for_feature_lane(self) -> None:
        lane = {"lane_id": "DTQ-01", "title": "Onboarding and quick-start"}
        message = generate_commit_message(
            lane,
            ["docs/cerb/onboarding-interaction/spec.md", "handoffs/schedule_quick_start_compose_handoff.md"],
        )
        self.assertEqual(message, "DTQ-01: sync onboarding and quick-start docs")

    def test_status_state_allows_empty_staged_set(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            handoff = repo_root / "handoffs/dtq-06-governance-harness.md"
            handoff.parent.mkdir(parents=True, exist_ok=True)
            handoff.write_text("stub\n", encoding="utf-8")

            context = LaneContext(
                repo_root=repo_root,
                git_dir=repo_root / ".git/worktrees/lane-dtq-06",
                worktree_root=repo_root / "lane-worktrees/DTQ-06-governance-harness",
                branch="lane/DTQ-06/governance-harness",
                registry_path=repo_root / "ops/lane-registry.json",
            )
            registry = {
                "protocol_version": 1,
                "integration_tree": {"path": str(repo_root), "admin_paths": ["ops/**", "scripts/**", "docs/**"]},
                "lanes": [
                    {
                        "lane_id": "DTQ-06",
                        "title": "Governance, trackers, and repo guardrails",
                        "status": "active",
                        "branch": "lane/DTQ-06/governance-harness",
                        "owned_paths": ["ops/**", "scripts/**", "docs/**"],
                        "allowed_shared_paths": [],
                        "handoff_path": "handoffs/dtq-06-governance-harness.md",
                    }
                ],
            }

            with patch("scripts.lane_guard.load_lease", return_value={"lane_id": "DTQ-06"}), patch(
                "scripts.lane_guard.get_changed_paths",
                side_effect=[["scripts/lane_guard.py"], []],
            ):
                state = collect_lane_state(context, registry, require_staged=False)

        self.assertEqual(state.dirty_paths, ["scripts/lane_guard.py"])
        self.assertEqual(state.staged_paths, [])
        self.assertIsNone(state.generated_message)


if __name__ == "__main__":
    unittest.main()
