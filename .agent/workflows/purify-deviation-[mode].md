---
description: Enter Purify Deviation Mode - enforce architectural alignment and debt repayment
---

# Purify Deviation Mode

A **strict compliance mode** to stop architectural drift and repay technical debt.

---

## When to Enter

- Touching any module marked as `[DEVIATION]` in `RealizeTheArchi.md`
- Refactoring "God Files" (e.g., HomeViewModel)
- Implementing features that touch core architecture (Orchestrator V1)
- Whenever you feel the urge to "just copy paste this legacy function"

---

## Mode Behavior

### 1. Mandatory Pre-Flight Check

**Before writing a single line of code**, you MUST:

1. **Read the Map**:
   - [`docs/Orchestrator-V1.md`](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md) (The Law)
   - [`docs/RealizeTheArchi.md`](file:///home/cslh-frank/main_app/docs/plans/RealizeTheArchi.md) (The Current State)

2. **Scan for Deviations**:
   ```bash
   # Check if target files are marked as deviations
   grep -r "DEVIATION" .
   ```

3. **Declare Intent**:
   > 🛡️ **Purify Mode: ON**
   > Target: [Module/File]
   > Current Status: [Compliant / DEVIATION / Unknown]
   > Purify Goal: [Rewrite X / Remove Y]

### 2. The Golden Rule: Rewrite > Extract

**NEVER surgically extract legacy code.**

| ❌ Surgical Extraction | ✅ Fresh Rewrite |
|------------------------|------------------|
| Copy-paste old function to new file | Read V1 Spec for intent |
| Fix compile errors | Write fresh signature matching spec |
| Bring along hidden dependencies | Implement logic using *current* architecture |
| Preserves unknown side-effects | Verifies behavior from first principles |
| "It works like the old one" | "It works as designed" |

**Why?** Extraction preserves the "how" (often wrong). Rewrite captures the "what" (intent).

### 3. Execution Protocol

1. **Isolate**: Identify the responsibility to move.
2. **Audit**: Read the legacy code to understand *what* it tries to do. ignore *how* it does it.
3. **Spec**: Read `Orchestrator-V1.md` to see how it *should* work.
4. **Implement**: Write new code in the correct domain layer (pure Kotlin, no Android).
5. **Switch**: Point the UI/Consumer to the new implementation.
6. **Kill**: Delete the old code. do not comment it out.

### 4. Exit Criteria

Only exit when:
- [ ] `RealizeTheArchi.md` is updated (Deviation removed or updated)
- [ ] No new legacy dependencies created
- [ ] Old implementation is DELETED (not deprecated)
- [ ] Build passes
- [ ] **Purify Score** generated

---

## Purify Score (Required Summary)

**Produce this summary when exiting.**

```markdown
### 🛡️ Purify Deviation Report

| Item | Result |
|------|--------|
| Target | [Name] |
| Strategy | [Rewrite / Delete] |
| Lines Deleted | -XX |
| Deviations Fixed | [List] |
| RTA Status | [Updated/Synced] |

**Verdict**: [PURIFIED / CONTAINED]
```

---

## Anti-Patterns (The "Broken Windows")

- **"I'll fix the architecture later"** → You won't. Fix it now or mark it `[DEVIATION]`.
- **"Just moving generic logic"** → If it's generic, it belongs in `core/`.
- **"Keeping the old one just in case"** → Delete it. Git has history.
- **Copying `ViewModel` logic to `Domain`** → ViewModel logic is usually UI state management, not Domain logic. Don't pollute Domain.

---

## Critical Resources

- **Source of Truth**: [`docs/plans/RealizeTheArchi.md`](file:///home/cslh-frank/main_app/docs/plans/RealizeTheArchi.md)
- **The Spec**: [`docs/specs/Orchestrator-V1.md`](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md)
