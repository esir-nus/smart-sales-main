---
description: Feature development with Cerb pattern (wave plans, interface/spec split)
---

# Feature Development (Cerb Pattern)

Use this workflow for building new features with incremental wave plans and proper context boundaries.

---

## Step 1: Define Governance Boundary

Create two documents:
1. `docs/cerb/[feature-name]/interface.md` — **Consumer-facing contract** (high-level methods only)
2. `docs/cerb/[feature-name]/spec.md` — **Internal implementation** (algorithms, logic, hidden from consumers)

**Rule**: Feature Agents interact via Interface only. No reading Internal Spec.

---

## Step 2: Create Wave Plan

Use this table template:

```markdown
| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Model | 🔲 PLANNED | Interface + Model + RealImpl + Tests |
| **1.5**| Wiring (VM) | 🔲 PLANNED | ViewModel integration + UI feedback |
| **2** | Secondary Feature | 🔲 PLANNED | Extension interface + Tests |
| **3** | Edge Cases | 🔲 PLANNED | Polish + edge handling |
```

**Wave Philosophy**:
- Each wave = **independently shippable**
- Wave 1 = Hardest core logic (deterministic, pure Kotlin)
- Wave 1.5 = Wire to ViewModel immediately (prevents code rot)
- Later waves = Extensions, probabilistic/AI features

---

## Step 3: Define Ship Criteria Per Wave

For each wave:

```markdown
### 🔬 Wave [N]: [Title]
[One-line goal]
- **Ship Criteria**: [Specific, measurable outcome]
- **Test Cases**:
    - [ ] [Happy path]
    - [ ] [Edge case 1]  
    - [ ] [Edge case 2]
- **Deliverables**: `File1.kt`, `File2.kt`, `Test.kt`
```

---

## Step 4: Domain Model (if applicable)

```kotlin
data class [ModelName](
    val id: String,
    // Core fields with types and Chinese comments
)
```

---

## Step 5: Two-Phase Pipeline (if applicable)

- **Phase 1 (Deterministic)**: Validation, conflict check, linting — fast, no LLM
- **Phase 2 (Probabilistic)**: Context enrichment, LLM calls, habit inference — slow, smart

```
User Input → Phase 1 (Gate) → [CLEAR] → Phase 2 → Response
                           → [CONFLICT] → Resolution UI → Retry
```

---

## Constraints

| Rule | Reason |
|------|--------|
| **Cerb Blackbox** | Consumers use interface, NOT internals |
| **Feature Branch** | Work on `cerb-[feature]` branch |
| **No Legacy Imports** | Rewrite from spec, don't copy legacy |
| **Test-Driven** | Each wave ships with tests |

---

## Execution Loop

```
For each Wave:
1. Update status: 🔲 → 🔧 IN PROGRESS
2. Implement deliverables
3. Run tests: ./gradlew testDebugUnitTest
4. Verify: ./gradlew :app:assembleDebug
5. Update status: 🔧 → 🚢 SHIPPED
6. Next wave
```

---

## Status Legend

| Icon | Meaning |
|------|---------|
| 🔲 | PLANNED |
| 🔧 | IN PROGRESS |
| 🚢 | SHIPPED |
| ❌ | BLOCKED |

---

## Example: Memory Center Waves

| Wave | Focus | Status |
|------|-------|--------|
| **1** | ScheduleBoard (Conflict) | 🚢 SHIPPED |
| **1.5**| Wiring (VM integration) | 🚢 SHIPPED |
| **2** | Entity Resolution | 🔲 PLANNED |
| **3** | Location Conflict | 🔲 PLANNED |
| **4** | Reinforcement Scoring | 🔲 PLANNED |
| **5** | Hot/Cement Persistence | 🔲 PLANNED |
| **6** | User Habit Nudger | 🔲 PLANNED |

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Skip Wave 1.5 (wiring) | Wire immediately after core logic |
| Ship without tests | Each wave has explicit test cases |
| Read blackbox internals | Trust the interface contract |
| Copy legacy code | Rewrite from spec |
