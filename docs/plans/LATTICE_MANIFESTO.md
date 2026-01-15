# Lattice Architecture (V2) Manifesto

**Branch**: `Lattice`  
**Objective**: Pure Lattice architecture — built for 3-year maintainability.

---

## 0. The Mindset

> **We are NOT shipping in 3 days. We are building for 3 years.**

This means:
- **No pragmatic shortcuts** — "ship it, fix later" is not acceptable
- **No "workable" code** — must be **correct by spec**
- **100/100 compliance** — anything less is a blocker
- **Architecture purity over velocity** — do it right the first time

---

## Core Principles

### 1. Box Isolation
Every component is a **Box** with:
- ✅ Interface (contract)
- ✅ Fake (testing)
- ✅ DTOs co-located
- ✅ `Result<T>` returns
- ✅ Hilt binding
- ✅ Unit tests

**No exceptions. No "we'll add the Fake later."**

### 2. Thin Orchestrator
Orchestrators are **logic-free wiring**:
- <200 LOC
- <5 conditionals
- 0 business logic
- Only passes DTOs between boxes

**If it has business logic, it's NOT an orchestrator — extract to a Box.**

### 3. Zero Coupling Violations
- ❌ Box imports Box = CRITICAL violation
- ❌ Android imports in domain = CRITICAL violation
- ❌ Business logic in Orchestrator = Extraction required

### 4. Evidence-Based Decisions
- Every refactor backed by audit (grep, find, view_file)
- No guessing, no assumptions
- `/17-lattice-review` before closing any work

---

## Quality Gate

**Before ANY work is considered "done":**

- [ ] `/17-lattice-review` score = 100/100
- [ ] All boxes have Interface + Fake + Tests
- [ ] Orchestrator <200 LOC, <5 conditionals
- [ ] Zero "fix later" comments
- [ ] Zero architectural violations

---

## Current State: TingwuRunner

`TingwuRunner` violates Thin Orchestrator Principle:

| Metric | Current | Target |
|--------|---------|--------|
| LOC | 1158 | <200 |
| Conditionals | 50 | <5 |
| Business logic | YES | NO |

**Remediation**: Extract business logic to boxes until TingwuRunner is a pure thin orchestrator.

---

## Rules of Engagement

1. **No Big Bang**: Migration happens box-by-box
2. **Evidence-Based**: Every refactor backed by verification
3. **Vibe Compatible**: Code must remain AI-readable (high locality, clear naming)
4. **100% Compliance**: No shipping with debt logged

---

## Reference Documents

| Document | Purpose |
|----------|---------|
| [`Orchestrator-Lattice.md`](../specs/Orchestrator-Lattice.md) | Full spec |
| [`lattice-interfaces.md`](../specs/lattice-interfaces.md) | Interface registry |
| `/17-lattice-review` | Compliance audit workflow |
