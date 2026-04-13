---
name: Harness scaffolding evaluation 2026-04-13
description: Comprehensive evaluation of repo-wide harness — grades, gaps, and prioritized recommendations for agent instruction layers, lane harness, CI, and governance
type: project
---

Comprehensive harness evaluation completed 2026-04-13. Frank agreed with all findings.

**Key grades:** Lane harness A, CLAUDE.md/AGENTS.md A, Codex skills A, Governance SOPs A, Antigravity rules B+ (context bloat), Frank observation D (stalled), cross-runtime configs D (placeholder-only), lane_guard tests B-.

**Why:** Frank wants to understand structural health and prioritize harness improvements.

**How to apply:** When touching harness files, reference these findings:
- 13 always-on Antigravity rules load ~600 lines; narrow rules (compose-scrim, data-oriented-os) should become conditional
- Frank observation system (frank_thinking_journal, frank_evidence_log, frank_principles) is stalled — 1 entry each, unchanged since 2025-01-25
- DTQ-01 through DTQ-04 have `branch: null` / `recommended_worktree: null` — harness can't enforce lane boundaries for them
- Cross-runtime configs (.gemini/.kiro/.roo/.windsurf) only contain ui-ux-pro-max — no project rules
- lane_guard.py mutation commands (create, pause, resume, integrate) have no tests
- No Claude Code hooks for lane validation exist yet
- Workflow library (30+ files, ~5500 lines) has no pruning/retirement lifecycle
