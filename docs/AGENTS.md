# Repository Guidelines (Human Reference)

> ⚠️ **Note for Agents**: This file is for **human onboarding only**. Agent rules are in `.agent/rules/smart-sales.md` which is auto-loaded. Do not treat this file as authoritative config.

---

## Purpose

This document provides human-readable guidelines for:
- New team members joining the project
- Chinese dev colleagues who need full context
- Reference when `.agent/` rules need clarification

**For agents**: Read `.agent/rules/*.md` instead — those are auto-loaded.

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
