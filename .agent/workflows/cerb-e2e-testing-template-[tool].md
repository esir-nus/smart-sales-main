---
description: Create a standardized Cerb Testing Script specification (Protocol + Coverage + Scenarios)
last_reviewed: 2026-04-13
---

# Cerb Test Script Scaffolder

> **Purpose**: Create a standardized specification for an automated Test Suite or Script.
> **Output**: `test-spec.md` detailing the coverage zones and Anti-Illusion criteria.
> **Next Step**: Once spec is created, use `/feature-dev-planner` to write the tests.

---

## Step 1: Define Test Boundaries

Create `docs/cerb-e2e-test/specs/[test-name]/boundaries.md`.

**Rule**: Define exactly what is being tested and what must be Faked (Mock Eviction).

```markdown
# [Test Name] Boundaries

> **Target**: `[TargetFile]Test.kt`
> **Level**: [L1 Unit | L2 Simulated | L3 Real Device]

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: [e.g. `RealUnifiedPipeline`]
- **Required Fakes**: [e.g. `FakeEntityRepository`]
- **Forbidden Mocks**: Do not use `mockito` for [X, Y, Z].
```

---

## Step 2: Define Scenarios and Context Branches

Create `docs/cerb-e2e-test/specs/[test-name]/spec.md`.

### 2.1 OS Layer Declaration

> **OS Layer**: Testing Environment

### 2.2 Coverages & Scenarios

Define the exact execution paths the test must prove.

```markdown
## Execution Scenarios (Mandatory)

> **Rule**: Every test must prove both Cold Start (Zero Context) and Warm Start (Chaos Context) to prevent Testing Illusions.

### 1. Cold Start (Zero Seed)
- **State**: `WorldStateSeeder` is EMPTY.
- **Inject**: [Input]
- **Expect**: [Proof that LLM doesn't hallucinate / gracefully asks for clarification]

### 2. Warm Start (Chaos Seed)
- **State**: `WorldStateSeeder` injects dense, messy B2B context (aliases, overlapping data).
- **Inject**: [Ambiguous/Complex Input]
- **Expect**: [Proof that semantic routing/disambiguation works correctly]

### 3. The Linter Verification
- **Test**: Send hallucinated data from upstream.
- **Expect**: Test proves data is REJECTED by `verify()` mismatch.
```

### 2.3 Wave Plan

```markdown
## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Mock Eviction Structure | 🔲 PLANNED | Setup tear-down, fakes injected |
| **2** | Scenario Writing | 🔲 PLANNED | All Context Branches written |
```

---

## Step 3: Register in E2E Tasklist

Update `docs/cerb-e2e-test/tasklist_log.md`:
1. Add the test target to the appropriate Roadmap Phase.
2. Formally declare it as a testing constraint.

---

## Checklist

- [ ] `boundaries.md` created (Fakes vs Mocks)
- [ ] `spec.md` created (Scenarios, Anti-Illusion alignment)
- [ ] Registered in `tasklist_log.md`
