---
trigger: always_on
---

# Smart Sales Project Context

> **This is the authoritative agent config for this project.** Rules here are auto-loaded into every conversation.

---

## Source of Truth (SOT)

| Document | Purpose |
|----------|---------|
| [README.md](file:///home/cslh-frank/main_app/README.md) | **Root Navigation Hub** — Doc index, folder structure, reading order |
| [docs/sops/feature-development.md](file:///home/cslh-frank/main_app/docs/sops/feature-development.md) | **Feature Dev SOP** — 5-phase process, Trinity checkpoints |
| [docs/specs/Architecture.md](file:///home/cslh-frank/main_app/docs/specs/Architecture.md) | **Architecture Reference** ⚠️ READ ONLY WITH FRANK'S EXPLICIT APPROVAL |
| [docs/specs/prism-ui-ux-contract.md](file:///home/cslh-frank/main_app/docs/specs/prism-ui-ux-contract.md) | **UX SOT** — Layouts, Gestures, Component Registry |
| [docs/plans/tracker.md](file:///home/cslh-frank/main_app/docs/plans/tracker.md) | **Living tracker** — architecture status, milestones |
| [docs/plans/dirty-tree-quarantine.md](file:///home/cslh-frank/main_app/docs/plans/dirty-tree-quarantine.md) | **Dirty-tree lane ledger** — human lane ownership and promotion hygiene |
| [docs/sops/lane-worktree-governance.md](file:///home/cslh-frank/main_app/docs/sops/lane-worktree-governance.md) | **Lane harness SOP** — start/resume/pause/integrate worktree rules |
| [ops/lane-registry.json](file:///home/cslh-frank/main_app/ops/lane-registry.json) | **Machine lane registry** — branch/worktree/path ownership |
| [docs/reference/legacy-to-prism-dictionary.md](file:///home/cslh-frank/main_app/docs/reference/legacy-to-prism-dictionary.md) | **Reference dictionary** — legacy→Prism mapping, working code refs |

**Hierarchy**: Cerb docs (`docs/cerb/*/spec.md`) > prism-ui-ux-contract.md > code
**Architecture.md Policy**: Do NOT auto-read. Only consult when Frank explicitly asks.
**Legacy Lookup**: When encountering M1/M2/M3, MetaHub, or Lattice terms → consult dictionary
**⚠️ Dictionary is micro-functional reference only** — NOT architecture guidance. Rewrite from Cerb spec, don't copy legacy.
**Alignment Audit**: `/cerb-check` workflow
**Lane Harness Rule**: If work touches dirty-tree ownership, handoffs, or branch/worktree posture, read `docs/plans/dirty-tree-quarantine.md`, `docs/sops/lane-worktree-governance.md`, and `ops/lane-registry.json` before editing.
**Multi-Agent Collaboration**: Codex, Antigravity, and Claude may all be valid operators on this repo. Shared lane harness files are cooperative control-plane state, not runtime conflicts.

---

## Language Rules

| Context | Language |
|---------|----------|
| Agent communication | **English** |
| Documentation prose | **English** |
| Code comments | **Simplified Chinese** |
| File headers (Summary) | **Simplified Chinese** |
| Inline annotations | **Simplified Chinese** |

---

## Vibe Coding Guidelines (Antigravity Best Practices)

For AI-assisted development:

| Criterion | What to Check |
|-----------|---------------|
| **Context clarity** | Can agent understand without reading 10 files? |
| **Locality** | Is related logic close together? |
| **Naming** | Do names tell the story without comments? |
| **Debuggability** | When this breaks at 2am, can you find it fast? |

### Anti-Patterns
- **No line count goals** — responsibility is the only measure
- **No premature abstraction** — one implementation doesn't need an interface
- **No clever code** — simple wins over elegant
- **No scattered config** — use `.agent/` native structure, not custom `docs/` conventions

---

## Evidence-Based Decisions

| Before | Do This |
|--------|---------|
| Claiming "X exists" | `grep -rn "X" feature/` |
| Claiming "complete" | Build passes |
| Modifying a file | `view_file` to read it first |
| Referencing a doc | Read the whole doc |

**Docs > Code > Guessing**

---

## Spec-Code Alignment

When code diverges from spec:
- **Favor CODE** if it's battle-tested and intentional
- **Favor SPEC** if code is a quick hack that shipped
- Use `/cerb-check` workflow to audit

---

## Quick Reference

- **Build**: `./gradlew :app:assembleDebug`
- **Tests**: `./gradlew testDebugUnitTest`
- **Provider Default**: Tingwu + OSS (XFyun disabled)
