---
description: Gateway workflow for feature development - enforces anti-drift protocol and 3-level testing
---

# Feature Dev Planner

> **Gateway Workflow**: Run `/feature-dev-planner [feature area]` before starting any feature work.

---

## Usage

```
/feature-dev-planner memory center
/feature-dev-planner scheduler
/feature-dev-planner analyst mode
```

---

## Phase 0: Spec Check (Mandatory Gate)

Before ANY planning, read the relevant spec:

1. **Locate spec**: `docs/cerb/[feature]/spec.md`
2. **Read COMPLETELY** — no skimming
3. **Quote** the exact section being implemented

### Spec Alignment Checklist

```markdown
### Spec Quote
> [Paste exact spec text here]

### My Understanding
[Explain what you will build]

### Mismatch?
- [ ] Spec is clear, I understand it
- [ ] Spec is unclear, need clarification from USER
- [ ] Spec doesn't cover this, need spec update FIRST
```

**If mismatch → STOP. Do not proceed.**

---

## Phase 1: Contamination Cleanup

Before implementing ANYTHING:

1. **Check Fakes** for hardcoded test data → DELETE
2. **Check for TODO stubs** → DELETE
3. **Check for skeleton implementations** → DELETE

```bash
# Contamination audit
grep -rn "TODO" app-prism/src/main/java/com/smartsales/prism/data/fakes/
grep -rn "Phase 2" app-prism/src/main/java/
```

**Clean slate = correct implementation.**

---

## Phase 2: First Principles Check

### LLM vs Kotlin Decision Tree

| Question | If YES → | If NO → |
|----------|----------|---------|
| Does this require understanding language/context? | LLM | Kotlin |
| Is this pure math/logic? | Kotlin | LLM |
| Does this need disambiguation/inference? | LLM | Kotlin |

**Examples:**
- "Is 3pm free?" → **Kotlin** (interval math)
- "Who is 张总?" → **LLM** (context understanding)
- "Parse 下周三" → **LLM** (NLP)
- "Check for overlap" → **Kotlin** (time math)

### Anti-Pattern Check

| Anti-Pattern | Detection | Fix |
|--------------|-----------|-----|
| Premature abstraction | Interface with 1 impl | Just use the class |
| Kotlin for LLM work | Parsing/disambiguation | Let LLM do it |
| Over-engineering | "This might be useful later" | YAGNI — delete it |

---

## Phase 3: Implementation Plan

Only after Phases 0-2 pass:

```markdown
### Feature: [Name]
### Spec Reference: [File:Line#]

### Deliverables
| File | Purpose | Status |
|------|---------|--------|
| ... | ... | 🔲 |

### Anti-Drift Audit
- [ ] Spec section quoted
- [ ] No skeleton data in Fakes
- [ ] LLM vs Kotlin justified
- [ ] L2 scenario planned
```

---

## Phase 4: Three-Level Testing

| Level | What | When |
|-------|------|------|
| **L1** | `./gradlew testDebugUnitTest` | Every change |
| **L2** | Debug buttons in UI (isolated scenarios) | Before integration |
| **L3** | Full on-device with real UX | After L2 pass |

### L2 Scenario Runner Pattern

```kotlin
// In ViewModel (DEBUG only)
fun debugRunScenario(scenario: String) {
    when (scenario) {
        "CLEAN" -> clearAllTestData()
        "SCENARIO_A" -> simulateWithPreset(PRESET_A)
        "SCENARIO_B" -> simulateWithPreset(PRESET_B)
    }
}
```

**L2 MUST pass before L3.** L3 can have false positives.

---

## Enforcement

**Do NOT proceed to implementation if:**

- [ ] ❌ Lessons-learned.md NOT read
- [ ] Spec not read completely
- [ ] Fakes contain hardcoded test data
- [ ] LLM vs Kotlin decision not justified
- [ ] No L2 scenario planned

---

## Phase -1: READ LESSONS LEARNED (FIRST!)

**BEFORE touching any code, read the lessons file:**

```bash
# MANDATORY — do this FIRST
cat .agent/rules/lessons-learned.md
```

| Check | What to Look For |
|-------|------------------|
| **Similar symptom** | Has this bug pattern been seen before? |
| **Same file/component** | Is there a known gotcha for this area? |
| **UI issue** | Check Compose patterns (scrim, swipe, click) |
| **Data flow issue** | Check sealed class, parser, pipeline lessons |

**If a lesson matches → apply the documented fix IMMEDIATELY. Do NOT reinvent.**

---

## Phase 5: LOG FIX (After User Confirms)

**After USER says "problem fixed" (not just BUILD SUCCESSFUL):**

1. Open `.agent/rules/lessons-learned.md`
2. Add entry using the template:

```markdown
### [SHORT TITLE] — [DATE]

**Symptom**: What the user reported  
**Root Cause**: The actual problem  
**Wrong Approach**: What didn't work  
**Correct Fix**: What worked  
**File(s)**: Where the fix was applied  
**Status**: ✅ CONFIRMED [DATE]
```

3. Add ABOVE the `<!-- Add new lessons above this line -->` marker

**⚠️ Do NOT log until user confirms. BUILD SUCCESS ≠ FIXED.**

---

## Quick Reference

| Phase | Gate |
|-------|------|
| **-1** | ✅ Lessons read |
| 0 | Spec quoted |
| 1 | Fakes clean |
| 2 | First principles checked |
| 3 | Plan approved |
| 4 | L1 → L2 → L3 pass |
| **5** | ✅ Lesson logged (if bug fix) |

---

## Related Rules

- `.agent/rules/anti-drift-protocol.md` — Full anti-drift rules
- `.agent/rules/lessons-learned.md` — **READ FIRST, UPDATE AFTER FIX**
