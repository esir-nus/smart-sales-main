---
description: Enter UX Refactoring Mode - principles for M0 legacy cleanup
---

# UX Refactoring Mode

A **mindset**, not an automation. Enter this mode when rewriting messy legacy flows.

---

## When to Enter

- Rewriting legacy UI flows toward clean UX spec
- M0 stabilization where existing code is reference only
- Extracting logic from god files (HomeViewModel, etc.)

---

## Mode Behavior

### Required Reading (Mode Entry)

Before refactoring, **read these in order**:

| Priority | Document | Purpose |
|----------|----------|---------|
| 1 | [`docs/Orchestrator-V1.md`](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md) | System architecture spec (SoT) |
| 2 | [`docs/RealizeTheArchi.md`](file:///home/cslh-frank/main_app/docs/plans/RealizeTheArchi.md) | Current module state & target structure |
| 3 | [`docs/ux-contract.md`](file:///home/cslh-frank/main_app/docs/specs/ux-contract.md) | Data contracts (what system presents) |
| 4 | [`docs/ux-experience.md`](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md) | UX states & microcopy (how it's presented) |

**SoT Hierarchy**: V1 Spec > RealizeTheArchi > ux-contract > ux-experience > code

> ⚠️ **Do NOT start refactoring without reading the relevant sections.**

### Entry Acknowledgment

When entering this mode, **acknowledge mode + reading status**:
> 🔧 **UX Refactoring Mode: ON** — [Flow Name]
> Required docs: ✅ Read / ⏳ Reading

### Persistence
- Mode stays **ON** until user explicitly says "exit mode" or similar
- Completing a refactoring task does **NOT** auto-exit the mode
- User may continue to the next flow or task within the same mode

### Exit
Only exit when user explicitly requests:
- "exit mode"
- "end refactoring mode"
- "done with UX refactoring"

---

## Core Principles

### 1. Legacy = Reference Only

> Read legacy code to understand **WHAT** it does, not HOW.

- Never patch legacy — rewrite from spec
- Treat old code as "behavior documentation"
- Delete old code after tests prove new code works

### 2. One Responsibility Per Pass

> When touching god files, extract **ONE** thing at a time.

✅ Good: "Delete duplicate `parseRenameCandidate`"
❌ Bad: "Clean up all of HomeViewModel"

### 3. Docs > Code

> When code contradicts docs, **trust docs**.

Hierarchy: `ux-experience.md` > `api-contracts.md` > legacy code

If unsure, ask — don't infer from broken patterns.

### 4. No Bug Chasing

> If you find a bug during refactoring: **log it, don't fix it**.

- Add: `// TODO(bug): [description]`
- Complete original refactor task
- File bug separately afterward

This prevents: "Quick fix" → side quest → never finish.

### 5. Evidence Before Delete

> Never delete code without verification.

```bash
# 1. Confirm no callers
grep -r "functionName" feature/chat

# 2. Confirm compiles
./gradlew :app:assembleDebug

# 3. Confirm tests pass
./gradlew :feature:chat:testDebugUnitTest
```

### 6. Strangler Fig: Mark → Implement → Delete

> If legacy can't be deleted immediately, **don't block**.

1. Mark legacy: `@Deprecated("Use X instead")` or `// TODO(migrate): description`
2. Implement new code alongside
3. Migrate callers iteratively
4. Delete legacy only when grep shows 0 callers

This allows shipping clean code without waiting for full migration.

### 7. Evidence-Based Debugging

> Never hypothesize crash causes. **Get the evidence first.**

When user reports a bug or crash:

**Step 1: Get actual error from device**
```bash
# Get crash stack trace
adb logcat -d -s AndroidRuntime:E | tail -50

# Get app logs
adb logcat -d | grep -i smartsales | tail -100
```

**Step 2: Read crash location in code**
```bash
# From stack trace, find the exact file:line
view_file /path/to/CrashLocation.kt
```

**Step 3: Only then propose a fix**

| ❌ Don't | ✅ Do |
|----------|------|
| "I suspect the crash is in X" | "Logcat shows crash at `OkHttpClient.java:82`" |
| "Let me wrap this in try-catch" | "Stack trace shows NPE on null response, need pre-check" |
| "This should fix it" | "Evidence: [logcat output], Fix: [specific change]" |

**This prevents:**
- Fixing the wrong thing
- Breaking other things while guessing
- Wasted cycles on hypothesis → test → fail loops

---

## Workflow (Manual)

```
1. Define objective (what flow?)
2. Review/polish UX spec in ux-experience.md
3. Audit legacy code → document WHAT it does
4. Write fresh code from spec
5. Delete legacy code
6. Verify build + tests
```

Use these workflows as needed:
- `/8-ux-specialist` — Polish UX spec
- `/6-audit` — Evidence-based code analysis
- `/1-senior-review` — Sanity check decisions
- `/3-plan-review` — Verify assumptions before execute

---

## Exit Criteria

Leave this mode when:
- [ ] UX spec updated in `ux-experience.md`
- [ ] Legacy code deleted (not patched)
- [ ] Build passes
- [ ] Tests pass
- [ ] No "TODO(bug)" comments introduced
- [ ] **Metrics summary produced** (see template below)

---

## Refactoring Metrics Summary (Required)

**Produce this summary when exiting UX Refactoring Mode.**

```markdown
### [Flow Name] Refactoring — Metrics Summary

| Metric | Value |
|--------|-------|
| Lines removed | -XX lines |
| Duplicate DTOs/types deleted | X |
| Domain functions added | X |
| Docs updated | X files |

### Files Modified
| File | Change |
|------|--------|
| `Foo.kt` | +function, -duplicate |
| `Bar.kt` | Simplified X |

### Exit Criteria
- [x] Build passes
- [x] Tests pass
- [x] No TODOs introduced
```

**Why this matters:**
- Proves refactoring shipped value, not just "cleaned things up"
- Enables comparison across sessions
- Gives stakeholders a TL;DR

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| "Let me fix this bug first" | Log bug, continue refactor |
| "I'll just patch this one line" | Rewrite entire function from spec |
| "The code does X so spec must be wrong" | Trust spec, ask if unsure |
| "Let me refactor all of HomeViewModel" | One responsibility per PR |
| "I remember this function does Y" | grep to verify callers |
| "I suspect the crash is in X" | `adb logcat` → read stack trace → verify |
| "This try-catch should fix it" | Evidence first → targeted fix |
| "The user says it crashes, let me guess where" | Get crash log, read code at crash location |

---

## Context

- **App audience**: Chinese (microcopy in Chinese)
- **M0 milestone**: Stabilization phase
- **Reference docs**:
  - [`docs/ux-experience.md`](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md)
  - [`docs/ux-contract.md`](file:///home/cslh-frank/main_app/docs/specs/ux-contract.md)
  - [`docs/api-contracts.md`](file:///home/cslh-frank/main_app/docs/specs/api-contracts.md)
