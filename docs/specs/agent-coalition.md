# Agent Coalition Contract

This document records the operating agreement between Claude, Codex, and the operator.
It is not a tracker and should not accumulate per-task entries. For audit history, see
`docs/plans/tracker.md` and execution briefs. For operating protocol, see
`docs/specs/harness-manifesto.md`.

## Agents and Domains

**Claude** -- brain layer. Plans, reasons, documents, brainstorms. Owns all documentation
authorship and synchronization. Executes UI work directly when needed. Produces sprint
contracts and `/codex-handoff` blocks for Codex. Never executes git operations directly.

**Codex** -- execution layer. Implements features from sprint contracts. Owns all git
operations (stage, commit, push, PR, merge). Performs peer review before shipping.
Handles structural doc-sync work when a campaign wave requires it. Returns execution
reports to Claude for incorporation into trackers and docs.

**Operator** -- decision authority. Approves plans, arbitrates disputes between agents,
supplies real-device evidence (screenshots, adb logcat, on-device test results) that
neither agent can produce autonomously.

## Task Routing

| Task type | Primary agent | Secondary / handoff |
|-----------|---------------|---------------------|
| Feature planning | Claude | Claude produces sprint contract, hands to Codex |
| Brainstorming / reasoning | Claude | -- |
| Documentation authorship | Claude | -- |
| UI work (planning + execution) | Claude | Claude executes, then `/codex-handoff` for git |
| Feature implementation | Codex | Sprint contract from Claude |
| Structural doc sync (campaign) | Codex | Codex → Claude execution report |
| Code review | Codex | -- |
| git operations (commit/push/PR/merge) | Codex | Always, including after Claude UI work |
| Execution report | Codex | Codex → Claude for doc incorporation |
| Real-device evidence | Operator | -- |
| Plan approval / arbitration | Operator | -- |

## Handoff Protocol

**Claude to Codex**: sprint contract or `/codex-handoff` block.

Minimum contents:
- Branch name and base branch
- Scope summary (1-2 sentences)
- Changed files grouped by module
- Suggested action (e.g., commit + push + PR to `platform/harmony`)
- Context notes (CI expectations, doc-sync state, anything Codex needs)

**Codex to Claude**: execution report.

Minimum contents:
- What was done (commits, PR URL if created)
- CI result
- Any doc drift or outstanding issues found during review

Claude incorporates the report into `docs/plans/tracker.md` and relevant execution briefs.

## Logging vs This Document

Trackers (`docs/plans/tracker.md`, execution briefs, `harmony-tracker.md`, etc.) record
*what happened*. This document records *who does what*. Do not merge the two purposes.

## Non-Goals

- This is not a tracker. Do not append per-task status here.
- This is not a substitute for `docs/specs/harness-manifesto.md` (operating protocol).
- This is not a substitute for per-skill documentation in `.claude/commands/` or `.codex/skills/`.
- This does not record branch protection rules or CI configuration; see `docs/specs/platform-governance.md`.
