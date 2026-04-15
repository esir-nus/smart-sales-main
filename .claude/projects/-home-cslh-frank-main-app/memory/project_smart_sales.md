---
name: Project Smart Sales
description: Current project state including platform posture, active trackers, documentation spine, and handoff registry
type: project
---

Smart Sales (main_app) is a voice-first sales operating app.

**Why:** This context helps orient any session quickly without re-exploring the repo.

**How to apply:** Use this as a starting point for session orientation; verify against current git state and tracker files before acting on specifics.

Current state (as of 2026-04-11):
- Branch: `harmony/tingwu-container`
- Platform posture: Android beta-maintenance, Harmony forward-development
- HarmonyOS root: `platforms/harmony/tingwu-container/` (transient Tingwu-only app, reduced capability)
- Harmony capability: audio import, Tingwu processing, artifact viewing, operator-only scheduler Path A mini lab
- Harmony disabled: scheduler UI, Ask-AI/chat, badge hardware, onboarding scheduler handoff

Active trackers:
- Main campaign: `docs/plans/tracker.md`
- Harmony program: `docs/plans/harmony-tracker.md`
- Structure cleanup: `docs/plans/god-tracker.md`
- UI campaign: `docs/plans/ui-tracker.md`
- Bug tracking: `docs/plans/bug-tracker.md`
- Dirty-tree quarantine: `docs/plans/dirty-tree-quarantine.md`
- Lane harness SOP: `docs/sops/lane-worktree-governance.md`
- Lane registry: `ops/lane-registry.json`
- Changelog: `docs/plans/changelog.md`

Documentation spine:
- `docs/core-flow/` — behavioral north-star flows
- `docs/cerb/` — self-contained module specs
- `docs/cerb-ui/` — UI module specs
- `docs/specs/` — architecture and feature contracts
- `docs/sops/` — standard operating procedures
- `docs/plans/` — living trackers
- `docs/platforms/` — platform-specific overlays (android, harmony)

Lane harness posture:
- Repo root is now the integration tree and should not carry normal feature edits
- Active feature work should normally run in dedicated lane worktrees
- Lane ownership is machine-checked in `ops/lane-registry.json` and human-tracked in `docs/plans/dirty-tree-quarantine.md`

Active handoffs (see `handoffs/README.md`):
- `handoffs/audio_drawer_compose_handoff.md`
- `handoffs/notification_compose_handoff.md`
- `handoffs/oem_permission_diagnosis.md`
- `handoffs/schedule_quick_start_compose_handoff.md`
