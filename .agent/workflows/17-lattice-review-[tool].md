---
description: Audit Lattice architecture compliance with alignment-first metrics
---

# Lattice Architecture Review

> **Mindset**: Alignment over arbitrary thresholds.
> **Standard**: Correct architecture — not cargo cult metrics.

---

## 0. Philosophy

This workflow enforces **LATTICE ARCHITECTURE ALIGNMENT**:
- **Alignment > LOC** — A 600 LOC orchestrator that delegates correctly IS compliant
- **Coupling matters** — Can you swap dependencies for tests?
- **Interfaces + Fakes** — The testability contract is non-negotiable
- **No Box-to-Box imports** — Dependency direction is critical

**LOC and conditionals are SMELLS, not FAILURES.**

---

## 1. Invocation

```
/17-lattice-review [target_path]
```

---

## 2. Metrics (Alignment-First)

### 2.1 Box Compliance (REQUIRED)

| Check | How | Pass | Fail |
|-------|-----|------|------|
| Interface exists | `grep "interface [Name]"` | ✅ | ❌ BLOCKER |
| Fake exists | `grep "class Fake[Name]"` | ✅ | ❌ BLOCKER |
| Hilt binding | `grep "@Binds.*[Name]"` | ✅ | ❌ BLOCKER |
| Has tests | `find "*Test.kt"` | ✅ | ⚠️ GAP |

**Missing Interface, Fake, or Hilt = ❌ BLOCKER**
**Missing tests = ⚠️ GAP (not blocker)**

### 2.2 Orchestrator Alignment (NOT LOC)

| Question | Pass | Fail |
|----------|------|------|
| Delegates to boxes? | ✅ | ❌ |
| Can swap dependencies for tests? | ✅ | ❌ |
| No inline business logic? | ✅ | ⚠️ SMELL |
| No direct API/network calls? | ✅ | ❌ |

**Key insight**: A 600 LOC orchestrator that delegates correctly IS a thin orchestrator.
**"Thin" means logic-free, not small.**

### 2.3 Dependency Direction (Critical)

| Rule | Violation | Severity |
|------|-----------|----------|
| Box imports Box | ❌ | BLOCKER |
| Android imports in domain | ❌ | BLOCKER |
| Orchestrator imports Box | ✅ | Expected |
| Business logic inline | ⚠️ | SMELL |

---

## 3. Scoring (Alignment-Based)

| Category | Weight | Criteria |
|----------|--------|----------|
| Box Compliance | 40% | Interface + Fake + Hilt for all |
| Architecture Alignment | 30% | Correct delegation pattern |
| Dependency Direction | 20% | No Box-to-Box imports |
| Test Coverage | 10% | Key paths tested |

---

## 4. Verdicts

| Score | Verdict | Action |
|-------|---------|--------|
| 90-100 | ✅ COMPLIANT | Ship |
| 80-89 | ⚠️ MINOR GAPS | Log debt, ship |
| <80 | ❌ GAPS | Fix blockers before ship |

---

## 5. What Makes an Orchestrator "Thin"

**Thin = Logic-free delegation, NOT small LOC count.**

A correctly structured orchestrator:
- ✅ Injects all dependencies via constructor
- ✅ Delegates work to boxes
- ✅ Passes DTOs between boxes
- ✅ Can be fully tested with Fakes
- ✅ Has no inline parsing/transformation

An orchestrator can be 600+ LOC and still be thin if:
- Every function delegates to an injected box
- No business logic is inline
- All dependencies are swappable

---

## 6. Anti-Patterns (Avoid Cargo Cult)

| ❌ Cargo Cult | ✅ Correct Thinking |
|---------------|---------------------|
| "Must be <200 LOC" | "Must delegate correctly" |
| "Must have <5 conditionals" | "Conditionals should be error handling only" |
| "Fat file = bad" | "Coupled file = bad" |
| "Extract until small" | "Extract until aligned" |

## 7. Naming Hygiene

**Consistent naming makes architecture self-documenting.**

### Required Patterns

| Component | Pattern | Good | Bad |
|-----------|---------|------|-----|
| Interface | `[Concern]` | `ResultProcessor` | `IResultProcessor`, `ResultProcessorInterface` |
| Fake | `Fake[Concern]` | `FakeResultProcessor` | `MockResultProcessor`, `StubProcessor` |
| Real Impl | `Real[Concern]` | `RealResultProcessor` | `ResultProcessorImpl` |
| Orchestrator | `[Domain]Runner` or `[Domain]Coordinator` | `TingwuRunner` | `TingwuManager`, `TingwuHelper` |
| Service Box | `[Verb][Noun]Service` or `[Noun]Processor` | `SubmissionService` | `Submitter`, `SubmitHelper` |
| Integration Box | `[Concern]Integration` | `EnhancerIntegration` | `EnhancerWrapper` |

### Naming Audit Checklist

- [ ] No `*Manager` classes (use `*Coordinator` or `*Runner`)
- [ ] No `*Helper` classes (use `*Service` or `*Processor`)
- [ ] No `*Util` classes in domain (use `*Service`)
- [ ] Fakes prefixed with `Fake`, not `Mock` or `Stub`
- [ ] Real implementations prefixed with `Real`
- [ ] Interfaces have no prefix (no `I*`)

---

## 8. Checklist Before Closing PR

- [ ] All boxes have Interface + Fake + Hilt binding
- [ ] Orchestrator delegates to boxes (check injection)
- [ ] Zero Box-to-Box imports
- [ ] Zero Android imports in domain
- [ ] Key paths have tests
- [ ] No inline business logic in orchestrator
