---
description: Audit Lattice architecture compliance with STRICT purity metrics
---

# Lattice Architecture Review (STRICT MODE)

> **Mindset**: Building for 3-year maintainability, not 3-day shipping.
> **Standard**: Pure Lattice compliance — no pragmatic shortcuts.

---

## 0. Philosophy

This workflow enforces **PURE LATTICE ARCHITECTURE**:
- No "ship it, fix later" — every deviation is a blocker
- No "workable" — must be **correct by spec**
- Orchestrators MUST be thin (logic-free wiring only)
- Every box MUST have Interface + Fake + Tests

**The bar is 100/100. Anything less requires remediation before proceeding.**

---

## 1. Invocation

```
/17-lattice-review [target_path]
```

---

## 2. Metrics (STRICT)

### 2.1 Box Compliance (ALL REQUIRED)

| Check | How | Pass | Fail = BLOCKER |
|-------|-----|------|----------------|
| Interface exists | `grep "interface [Name]"` | ✅ | ❌ STOP |
| Fake exists | `grep "class Fake[Name]"` | ✅ | ❌ STOP |
| DTOs co-located | `data class` in interface file | ✅ | ❌ STOP |
| Returns `Result<T>` | `grep "Result<"` | ✅ | ❌ STOP |
| Hilt binding | `grep "@Binds.*[Name]"` | ✅ | ❌ STOP |
| Has tests | `find "*Test.kt"` | ✅ | ❌ STOP |

**Missing ANY check = ❌ NON-COMPLIANT. Must fix before proceeding.**

### 2.2 Orchestrator Thin-ness (STRICT THRESHOLDS)

| Metric | PASS | FAIL |
|--------|------|------|
| LOC | <200 | ≥200 = ❌ FAT |
| Conditionals | <5 | ≥5 = ❌ FAT |
| Direct API calls | 0 | >0 = ❌ VIOLATION |
| Business logic | 0 lines | >0 = ❌ NOT ORCHESTRATOR |

**Orchestrators MUST be logic-free wiring. Any business logic = extraction required.**

### 2.3 Dependency Direction (ZERO TOLERANCE)

| Rule | Violation | Action |
|------|-----------|--------|
| Box imports Box | ❌ CRITICAL | MUST FIX |
| Android imports in domain | ❌ CRITICAL | MUST FIX |
| Orchestrator imports Box | ✅ Expected | — |
| Business logic in Orchestrator | ❌ CRITICAL | EXTRACT TO BOX |

---

## 3. Scoring (STRICT)

| Category | Weight | Required |
|----------|--------|----------|
| Box Compliance (6 checks) | 40% | 40/40 |
| Orchestrator Thin-ness | 20% | 20/20 |
| Dependency Direction | 20% | 20/20 |
| Test Coverage | 20% | 20/20 |

**Target: 100/100. Anything less = action items before proceeding.**

---

## 4. Verdicts (STRICT)

| Score | Verdict | Action |
|-------|---------|--------|
| 100 | ✅ COMPLIANT | Proceed |
| 90-99 | ⚠️ MINOR GAPS | Fix before next feature |
| <90 | ❌ NON-COMPLIANT | **STOP. Fix now.** |

**"Ship with debt logged" is NOT acceptable. Fix first.**

---

## 5. TingwuRunner Special Rule

`TingwuRunner` currently violates Thin Orchestrator Principle:
- 1158 LOC (should be <200)
- 50 conditionals (should be <5)
- Contains business logic (should be 0)

**Remediation path**:
1. Extract business logic to boxes
2. Make TingwuRunner a true thin orchestrator
3. Target: <200 LOC, <5 conditionals, 0 business logic

---

## 6. Definition: What IS an Orchestrator?

**Per Lattice Spec §1.2:**

```kotlin
// CORRECT: Thin Orchestrator (~50-200 LOC)
class TranscriptionOrchestrator @Inject constructor(
    private val preparer: AudioPreparerService,
    private val submission: SubmissionService,
    private val polling: PollingService,
    private val processor: ProcessorService,
    private val publisher: PublisherService
) {
    suspend fun transcribe(audio: AudioInput): Result<Transcript> {
        val prepared = preparer.prepare(audio)
        val submitted = submission.submit(prepared)
        val completed = polling.pollUntilComplete(submitted)
        val processed = processor.process(completed)
        return publisher.publish(processed)
    }
}
```

**Characteristics**:
- No if/when/else (or <5 for error handling)
- No business logic
- Only wires boxes together
- Passes DTOs between boxes

**NOT an orchestrator if**:
- Contains parsing/transformation logic
- Has complex conditionals
- Directly calls APIs
- Maintains non-trivial state

---

## 7. Checklist Before Closing Any PR

- [ ] All boxes have Interface + Fake + Tests
- [ ] Orchestrator <200 LOC, <5 conditionals
- [ ] Zero Box-to-Box imports
- [ ] Zero Android imports in domain
- [ ] Score = 100/100
- [ ] No "fix later" comments
