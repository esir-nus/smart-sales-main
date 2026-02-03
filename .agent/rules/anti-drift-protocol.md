---
description: Anti-drift rules for feature development - MUST follow before implementing
trigger: always_on
---

# Anti-Drift Protocol

> **Root Cause**: Agent builds "smart" solutions that drift from spec, creating useless code.

---

## Rule 1: Spec-First Development

**BEFORE writing any code, verify:**

1. Does spec define this feature? → If NO, **update spec first**
2. Does spec match your understanding? → If NO, **clarify with user**
3. Can LLM do this simpler? → If YES, **don't build Kotlin abstraction**

### Spec Alignment Checklist

```markdown
- [ ] Read the relevant spec section COMPLETELY
- [ ] Quote the exact spec text you're implementing
- [ ] If spec is silent, ASK USER before inventing
```

---

## Rule 2: Clean Before Build

**Skeleton code is contamination.** Before implementing real logic:

1. **Delete** test data hardcoded in Fakes
2. **Delete** placeholder implementations
3. **Delete** "Phase 2 TODO" stubs

**Why?** Skeleton data causes false positive tests. Real implementation should start clean.

---

## Rule 3: Three-Level Testing

| Level | What | When | Purpose |
|-------|------|------|---------|
| **L1: Unit Test** | `./gradlew test` | Every change | Logic correctness |
| **L2: Simulated On-Device** | Debug buttons with preset inputs | Before integration | Isolated feature validation |
| **L3: Full On-Device** | Real app with real UX | After L2 pass | Integration validation |

### L2 Pattern (Scenario Runners)

```kotlin
// In ViewModel (DEBUG only)
fun debugRunScenario(scenario: String) {
    when (scenario) {
        "SCENARIO_A" -> simulateWithInput(PRESET_INPUT_A)
        "SCENARIO_B" -> simulateWithInput(PRESET_INPUT_B)
    }
}
```

**UI**: Add debug buttons in DEBUG builds only.

**L2 MUST pass before L3.** L3 can have false positives from other features.

---

## Rule 4: First Principles Check

Before implementing, ask:

1. **What problem does this solve?** (Not "what does the task say")
2. **Is there a simpler way?** (LLM vs Kotlin, single call vs abstraction)
3. **What would break if I'm wrong?** (Scope of blast radius)

### Anti-Patterns to Catch

| Pattern | Symptom | Fix |
|---------|---------|-----|
| **Premature abstraction** | Interface with 1 implementation | Just use the class |
| **Kotlin for LLM work** | Parsing/disambiguation in Kotlin | Let LLM do it |
| **False positive tests** | Tests pass but feature doesn't work | Use L2 simulation |
| **Spec drift** | Implementation doesn't match spec | Re-read spec |

---

## Enforcement

**At every implementation plan, verify:**

```markdown
### Anti-Drift Audit
- [ ] Spec section quoted
- [ ] No skeleton data in Fakes
- [ ] L2 scenario planned
- [ ] First principles validated
```

**If any box is unchecked, plan is NOT READY.**
