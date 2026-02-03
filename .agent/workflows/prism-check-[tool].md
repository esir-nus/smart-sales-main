---
description: Strict spec-code alignment check. Registry (Physics) -> Contract (Flow) -> Ship.
---

# Prism Verification & Ship Gate

**"No Lazy Works"** — Verify code against the 3-Spec System before shipping.

---

## 🚦 The 3-Stage Pipeline

To ship ANY feature in Prism, you must pass these three gates in order:

```mermaid
graph LR
    A[Start] --> B{Gate 1: Registry}
    B -- Fail --> F[Fix Aesthetics/Physics]
    B -- Pass --> C{Gate 2: Contract}
    C -- Fail --> G[Fix Logic/Flow]
    C -- Pass --> D{Gate 3: Architecture}
    D -- Fail --> H[Fix Architecture]
    D -- Pass --> E[SHIP]
```

---

## Gate 1: The Assembly Check (Registry)

**Source of Truth**: [`docs/specs/ui_element_registry.md`](file:///home/cslh-frank/main_app/docs/specs/ui_element_registry.md)

Verify the "Lego Assembly" (Physics, Layers, IDs).

### Verification Steps
1.  **Z-Index / Layers**
    -   Check: Does the code use `PrismElevation` constants?
    -   Anti-Pattern: Hardcoded integers `zIndex(10f)` or implicit stacking.
    -   Command: `grep -r "zIndex" feature/path`
2.  **Component Constants**
    -   Check: Do UI element IDs match the Registry? (e.g., `[☰]`, `[📶]`)
    -   Check: Are dimensions/colors using Registry tokens?
3.  **States**
    -   Check: Does the Composable handle ALL states listed in the Registry?
    -   Example: If Registry says `idle`, `pressed`, `disabled` — code must handle all 3.

> 🔴 **STOP**: If visual physics/assembly fails, do not proceed to logic checks.

---

## Gate 2: The Flow Check (Contract)

**Source of Truth**: [`docs/specs/prism-ui-ux-contract.md`](file:///home/cslh-frank/main_app/docs/specs/prism-ui-ux-contract.md)

Verify the "User Journey" (Behavior, Logic, Intention).

### Verification Steps
1.  **Literal Step Trace**
    -   Pick a flow from the Contract (e.g., "User opens Scheduler").
    -   Trace: Does the code execute the EXACT steps in order?
    -   Discrepancy: "Contract says expand at 50px, Code expands at 100px" → **FAIL**.
2.  **Navigation Logic**
    -   Check: Do standard patterns (Back, Close, Confirm) follow the Contract?
3.  **Microcopy Validation**
    -   Check: Is the text identical to the Contract?
    -   Command: `grep -r "Exact Phrase" feature/path`

> 🔴 **STOP**: If logic/flow diverges from Contract, FIX THE CODE.

---

## Gate 3: Architecture Check (Prism Principles)

**Source of Truth**: [`docs/specs/Prism-V1.md`](file:///home/cslh-frank/main_app/docs/specs/Prism-V1.md) §7 (Testability Contract)

> **Philosophy**: Alignment over arbitrary thresholds. Correct architecture — not cargo cult metrics.
> **"Thin" = Logic-free delegation, NOT small LOC count.**

### 3.1 Box Compliance (REQUIRED)

Every service with I/O or state MUST have:

| Check | Command | Pass | Fail |
|-------|---------|------|------|
| **Interface exists** | `grep "interface [Name]"` | ✅ | ❌ BLOCKER |
| **Fake exists** | `grep "class Fake[Name]"` | ✅ | ❌ BLOCKER |
| **Hilt binding** | `grep "@Binds.*[Name]"` | ✅ | ❌ BLOCKER |
| **Has tests** | `find "*Test.kt"` | ✅ | ⚠️ Log debt |

### 3.2 Domain Purity

```bash
# Zero Android imports in domain
grep -r "import android" app-prism/src/main/java/**/domain/
# Result MUST be EMPTY
```

### 3.3 Dependency Direction

| Rule | Violation | Severity |
|------|-----------|----------|
| Box imports Box | ❌ | BLOCKER |
| Android imports in domain | ❌ | BLOCKER |
| Orchestrator imports Box | ✅ | Expected |
| Business logic inline | ⚠️ | SMELL |

### 3.4 Fake I/O Pattern

- **Check**: Does async/external behavior go through **Interface → Fake Implementation**?
- **Anti-Pattern**: `delay()`, mock data inside UI code (Composables, Screens)
- **Command**: `grep -rn "delay(" app-prism/src/main/java/**/ui/`
- **Result MUST be EMPTY** — Delays belong in `platform/fake/` or `domain/fake/`

### 3.5 Naming Hygiene

| Component | Pattern | Good | Bad |
|-----------|---------|------|-----|
| Interface | `[Concern]` | `ResultProcessor` | `IResultProcessor` |
| Fake | `Fake[Concern]` | `FakeResultProcessor` | `MockResultProcessor` |
| Real Impl | `Real[Concern]` | `RealResultProcessor` | `ResultProcessorImpl` |
| Orchestrator | `[Domain]Runner` | `TingwuRunner` | `TingwuManager` |

**Naming Blockers**:
- No `*Manager` classes (use `*Coordinator` or `*Runner`)
- No `*Helper` classes (use `*Service` or `*Processor`)
- Fakes prefixed with `Fake`, not `Mock` or `Stub`

> 🔴 **STOP**: If Box Compliance fails, refactor before shipping.

---

## 🚢 Ship Inspection

If all 3 gates pass:

1.  **Run Frank Grading** (Optional but recommended)
    -   Command: `@[/frank-grading]`
    -   Assess: "Is this minimal? Is it clean?"

2.  **Commit Message**
    -   Format: `feat(module): [Description] (Aligned with Registry §X, Contract §Y)`

---

## Troubleshooting "Lazy Works"

**Q: "The Contract is slightly wrong/outdated. Can I just ship the 'better' code?"**
**A: NO.**
1.  Update the Contract FIRST.
2.  Get approval.
3.  THEN match the code to the new Contract.
**Never ship code that disagrees with the Spec.**

**Q: "I hardcoded the Z-index because it was faster."**
**A: REJECTED.**
-   Refactor to use `PrismElevation` immediately.

**Q: "I used `delay()` in the UI because Fake I/O is more work."**
**A: REJECTED. This is a "Lazy Work".**
1.  Create an interface (e.g., `ConnectivityService`).
2.  Create a `FakeConnectivityService` with the `delay()` inside.
3.  Inject via Hilt.
4.  **Never embed simulation logic inside Composables.**
-   Rationale: Real code swapping requires 1 binding change, not a UI rewrite.
