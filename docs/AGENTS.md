# Repository Guidelines (Human Reference)

> Shared human-readable reference for the project's dual-runtime setup.
> Codex uses root `AGENTS.md`.
> Antigravity keeps using `.agent/rules/*.md`.
> Both share the same `docs/` system.

---

## Purpose

This document provides human-readable guidelines for:
- New team members joining the project
- Chinese dev colleagues who need full context
- Reference when runtime-specific agent rules need clarification

Runtime model:
- `AGENTS.md`: Codex-native repo rules
- `.codex/skills/*/SKILL.md`: Codex-native project skill library
- `.agent/rules/*.md`: Antigravity-native auto-loaded rules
- `.agent/workflows/*.md`: Antigravity-native workflow library
- `docs/**`: Shared documentation backbone for both

This file is not intended to replace either runtime-specific layer.

---

## Language Rules

| Context | Language | Notes |
|---------|----------|-------|
| Agent communication | **English** | All conversations with agents |
| Documentation prose | **English** | Specs, plans, READMEs |
| Code comments | **Simplified Chinese** | 方便中文开发同事阅读 |
| File headers | **Simplified Chinese** | Summary行使用中文 |
| Inline annotations | **Simplified Chinese** | 代码块内使用中文 |

---

## Doc Sources and Precedence

**Docs > Code > Guessing**

1. Architecture SOT: `docs/specs/Architecture.md`
2. UX SOT: `docs/specs/prism-ui-ux-contract.md`
3. Tracker: `docs/plans/tracker.md`
4. Archived docs (`docs/archived/**`) are historical only

### Spec-Code Alignment

When code diverges from spec:
- **Favor CODE** if it's battle-tested and intentional
- **Favor SPEC** if code is a quick hack that shipped
- Document the decision and update the appropriate source

### Core Flow Rule

For active feature development, this repo uses a stricter chain than ordinary spec-first projects:

1. Core Flow (`docs/core-flow/**`) defines what must happen
2. Spec defines how to build it
3. Code is the delivered result
4. PU Test validates delivered code against Core Flow
5. Fix loops repair lower layers until they match the Core Flow

This means a Core Flow doc may legitimately be ahead of specs and code.
When this happens, treat the lower layers as drift candidates unless the user says the Core Flow itself is obsolete.

### Shared Rule

Do not reorganize the documentation tree just because the active agent runtime changes.
The migration path is:
- Preserve `docs/**` as the shared source of project memory
- Preserve `.agent/**` for Antigravity workflows
- Express Codex-specific operating rules in root `AGENTS.md`

### Workflow Transfer Principle

The `.agent/workflows/**` files are important and often transferable.

- Procedural workflows can usually be reused by Codex as playbooks or SOPs.
- Persona workflows can usually be reused as lenses, checklists, or output formats.
- Antigravity-specific slash-command behavior should remain in `.agent/` unless there is a deliberate Codex migration.
- Reuse the knowledge first; only convert it into a Codex skill when the workflow is both repeatable and worth packaging.

---

## Purification Mindset

> These rules apply to ALL coding work.

### Pre-Coding Audit
1. Read the relevant tracker for architecture status
2. Read the relevant spec for module contracts
3. `grep` to verify assumptions — never guess

### Rewrite > Extract
- **Rewrite**: When code is misaligned or tightly coupled
- **Extract**: Only when code is already aligned and loosely coupled
- **Default to rewrite** — old patterns don't deserve preservation

### Organic Debt Payment
- Fix debt **when it blocks** feature delivery, not proactively
- Ship features first, refactor post-ship

---

## Vibe Coding Guidelines

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

---

## Code Style

All source files must include a unified file header:

```kotlin
// File: <relative path>
// Module: <:moduleName>
// Summary: <简短中文摘要>
// Author: created on <YYYY-MM-DD>
```

Style conventions:
- Kotlin: 4-space indentation, trailing commas, `val`-first
- Packages: `com.smartsales.<layer>`
- Resource IDs: lower snake case

---

## Build Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
./gradlew lint
```

---

## Testing Guidelines

- Unit tests: `<module>/src/test/` with JUnit 4
- UI tests: `<module>/src/androidTest/`
- Name suites after behavior (`DeviceConnectionManagerTest`)

---

## Security

- Never commit `local.properties` (contains API keys)
- Scrub logs so transcripts don't leak into commits
