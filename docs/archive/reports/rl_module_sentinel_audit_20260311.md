# Sentinel Audit: RL Module & Habit System (Drift & Ghosting)

**Date**: 2026-03-11
**Workflows Applied**: `@[/cerb-check-[tool]]`, `@[/01-senior-reviewr-[persona]]`, `@[/06-audit-[tool]]`
**Target**: `:domain:habit` and `:core:pipeline` (RL & HabitListener)

---

## 1. 🛡️ Cerb Doc Check (Context-Aware Linter)

**Classification**: Feature (`docs/cerb/rl-module/spec.md`)
**Result**: ⚠️ **NEEDS WORK**

### Universal Rules
- [x] Link Purity: Fixed crossing spec logic. `rl-module/spec.md` now points strictly to `interface.md` contracts.
- [x] Wave Planning: Present in spec.
- [x] Self-Containment: Handled inline.

### Domain Rules (Feature / OS Model)
- [x] OS Layer Declared: `RAM Application`
- [ ] **FAILED**: Boundary Guard. The `interface.md` for `rl-module` does not contain a "You Should NOT" section establishing misuse rules as required by the Domain A Gauntlet.

---

## 2. 🔍 Evidence-Based Code Audit (Drift & Ghosting)

I ran a literal spec alignment audit comparing `docs/cerb/rl-module/spec.md` and `docs/cerb/user-habit/spec.md` against actual Kotlin implementations (`RealReinforcementLearner.kt`, `UserHabit.kt`, `RlModels.kt`).

### The Literal Comparison Table

| Spec Rule (VERBATIM) | Code Implementation (VERBATIM) | Match? |
|----------------------|--------------------------------|--------|
| `val lastObservedAt: Instant` | `val lastObservedAt: Long` | ❌ **Drift** (Type mismatch) |
| `val userHabits: List<Habit>` | `val userHabits: List<UserHabit>` | ❌ **Drift** (Naming mismatch) |
| `source = USER_POSITIVE` | `ObservationSource.valueOf(...)` | ✅ YES |
| `Write-Through (Atomic): 1. Update RAM: Immediately update the in-memory HabitContext in SessionWorkingSet. 2. Persist to SSD` | `observations.forEach { obs -> habitRepository.observe(...) }` | ❌ **Ghost/Drift (Major)** |

### Major Architectural Finding (Ghosting)
The Spec strictly mandates a **Write-Through** pattern: the `ReinforcementLearner` MUST immediately update the `SessionWorkingSet` RAM so that follow-up turns in the same session instantly "remember" the new preference without requiring a fresh SSD pull. 
**Reality**: `RealReinforcementLearner.processObservations` exclusively writes to the SSD (Room Database). It does not hold a reference to `SessionWorkingSet` nor does it pipe the observation back into RAM. If the user corrects a habit on Turn 1, Turn 2 will NOT know about it because the RAM is stale.

---

## 3. 摸 01-Senior-Reviewer Persona verdict

### 🔴 Hard No (Stop Doing This)
Missing the RAM Write-Through is a fatal architectural drift. In an agentic system, if the user explicitly says "Actually, call me Alex", and the agent writes it to SSD but leaves the RAM unchanged, the very next turn the agent will call them the wrong name again, making the agent look stupid despite "learning" the fact. **If your OS model says RAM Application, you must hydrate RAM.**

### 🟡 Yellow Flags (Reconsider)
- **Time Typing**: The spec explicitly demands `java.time.Instant`, but `UserHabit.kt` uses `Long`. While `Long` is pragmatically faster for Room DB SQLite mapping, spec/code drift causes future AI agents to generate hallucinated mapping logic (e.g., trying to call `.toEpochMilli()` on a `Long`). EITHER update the Spec to `Long`, or update the Code to `Instant`.

### 🟢 Good Calls
- **Out-of-band Processing**: The `HabitListener` correctly implements true fire-and-forget background processing (`coroutineScope.launch` with `appScope`). It successfully parses the LLM JSON without holding the primary UnifiedPipeline hostage.
- **Fail-Safe JSON parsing**: The `try/catch` wrapper over the JSON manipulation with safe array scanning is extremely solid "Vibe Coding." If the LLM hallucinates markdown ticks, it strips them cleanly.

---

## 🛠️ Required Fixes (Next Steps)

1. **Architecture Repair**: Inject `SessionWorkingSet` (or a RAM-updating callback interface) into `RealReinforcementLearner` so that `processObservations` actually fulfills the Step 1 RAM Write-Through mandate.
2. **Spec vs Code Alignment**: Choose a source of truth for the Time representation (`Instant` vs `Long`) and the domain naming (`Habit` vs `UserHabit`) and sync both the Cerb Spec and the Kotlin code.
3. **Doc Hygiene**: Add the "You Should NOT" section to `nl-module/interface.md`.
