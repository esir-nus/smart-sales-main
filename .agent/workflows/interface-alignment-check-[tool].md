---
description: Verify that interface.md files across features are compatible before integration
---

# Interface Alignment Check

> **Purpose**: Simulate feature integration at the documentation layer. Catch incompatibilities before code is written.

---

## When to Use

| Scenario | Run This? |
|----------|-----------|
| Feature A will call Feature B in next wave | ✅ Required |
| Two features share a domain model | ✅ Required |
| Major interface refactor | ✅ Required |
| Single feature, no dependencies | ❌ Skip |
| Already integrated and working | ❌ Skip |

---

## Inputs

```
/interface-alignment-check [FeatureA] [FeatureB] "[integration point]"
```

**Example**:
```
/interface-alignment-check scheduler user-habit "Scheduler calls UserHabitRepository.observe()"
```

---

## Step 1: Read Both Interfaces

Read `docs/cerb/[feature]/interface.md` for both features.

Focus on:
1. Method signatures
2. Input/Output types
3. "You Should NOT" sections
4. Error handling expectations

---

## Step 2: Integration Point Analysis

### 2.1 Method Signature Match

| Aspect | Feature A Expects | Feature B Exposes | Match? |
|--------|-------------------|-------------------|--------|
| Method name | `observe()` | `observe()` | ✅/❌ |
| Parameters | `key: String, value: String` | `key: String, value: String, entityId: String?` | ⚠️ Default OK |
| Return type | `Unit` | `Unit` | ✅/❌ |
| Suspend | Yes | Yes | ✅/❌ |

### 2.2 Type Compatibility

| Type | Definition in A | Definition in B | Compatible? |
|------|-----------------|-----------------|-------------|
| `UserHabit` | Not defined (uses B's) | Full data class | ✅ Consumer uses provider's type |
| `String` | Primitive | Primitive | ✅ |

### 2.3 Null Safety

| A Passes | B Accepts | Safe? |
|----------|-----------|-------|
| `null` for entityId | `entityId: String?` | ✅ |
| Non-null entityId | `entityId: String?` | ✅ |

---

## Step 3: Behavioral Compatibility

### 3.1 Edge Case Alignment

| Edge Case | A Assumes | B Handles | Aligned? |
|-----------|-----------|-----------|----------|
| Empty result | Returns `emptyList()` | Returns `emptyList()` | ✅ |
| Not found | Returns `null` | Returns `null` | ✅ |
| Concurrent access | Not specified | Thread-safe | ⚠️ Document assumption |

### 3.2 "You Should NOT" Cross-Check

Check if A violates any of B's anti-patterns:

| B Says "Don't..." | A Does... | Violation? |
|-------------------|-----------|------------|
| "Don't cache long-term" | Caches per-session only | ✅ OK |
| "Check confidence > 0.7" | Uses raw value | ⚠️ Potential issue |

---

## Step 4: Dependency Direction

```
┌─────────────────────────────────────────────┐
│ Does A depend on B, or B depend on A?       │
│ (Unidirectional is healthy)                 │
└─────────────────────────────────────────────┘
```

| Check | Result |
|-------|--------|
| A imports B's types | ✅ Expected (A is consumer) |
| B imports A's types | ❌ Circular dependency |
| Both import shared domain | ⚠️ Extract to core |

---

## Output Format

```markdown
# Interface Alignment: [Feature A] ↔ [Feature B]

## Integration Point
`[A calls B.method()]`

## Verdict: [COMPATIBLE / INCOMPATIBLE / NEEDS WORK]

### Method Signature
| Aspect | A Expects | B Exposes | Match |
|--------|-----------|-----------|-------|
| ... | ... | ... | ✅/❌ |

### Type Compatibility
| Type | Compatible | Notes |
|------|------------|-------|
| ... | ✅/❌ | ... |

### Behavioral Alignment
| Edge Case | Aligned | Notes |
|-----------|---------|-------|
| ... | ✅/❌ | ... |

### Anti-Pattern Check
| B's Constraint | A Compliant | Notes |
|----------------|-------------|-------|
| ... | ✅/❌ | ... |

### Dependency Direction
[Diagram or statement]

---

## Required Fixes (if any)
1. [Specific change needed in A or B interface]
2. ...

## Recommendation
[PROCEED / BLOCK / SPEC UPDATE NEEDED]
```

---

## Industrial Practices Applied

| Practice | Source | How Applied |
|----------|--------|-------------|
| **Consumer-Driven Contracts** | Pact, Spring Cloud Contract | A defines what it needs, B verifies it provides |
| **API Compatibility Matrix** | Google API Guidelines | Type + signature + behavior check |
| **Liskov Substitution** | SOLID | B's contract must satisfy A's expectations |
| **Dependency Inversion** | Clean Architecture | Check direction is correct |
| **Defensive Documentation** | Microsoft API Guidelines | "You Should NOT" as machine-checkable constraints |

---

## Example: Scheduler ↔ UserHabit

```markdown
# Interface Alignment: Scheduler ↔ UserHabit

## Integration Point
`SchedulerViewModel calls UserHabitRepository.observe("preferred_meeting_time", "morning")`

## Verdict: ✅ COMPATIBLE

### Method Signature
| Aspect | Scheduler Expects | UserHabit Exposes | Match |
|--------|-------------------|-------------------|-------|
| Method | `observe()` | `observe()` | ✅ |
| Params | `key, value` | `key, value, entityId?` | ✅ (default null) |
| Return | `Unit` | `Unit` | ✅ |
| Suspend | Yes | Yes | ✅ |

### Behavioral Alignment
| Edge Case | Aligned | Notes |
|-----------|---------|-------|
| First observation | ✅ | Creates with confidence 0.5 |
| Repeated observation | ✅ | Increments count |

### Recommendation
✅ PROCEED — Interfaces are compatible for Wave 2 integration.
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|-------|
| Run on every PR | Run only before integration waves |
| Check internal implementation | Check interface.md only |
| Invent missing behavior | Flag as spec gap |
| Assume compatibility | Verify with evidence |
