---
description: Audit Lattice architecture compliance with evidence-based metrics
---

# Lattice Architecture Review

Generic compliance audit for any Lattice layer, module, or box. User provides target path.

---

## 1. Invocation

```
/17-lattice-review [target_path]
```

**Examples**:
```bash
/17-lattice-review data/ai-core/src/main/.../tingwu
/17-lattice-review feature/chat/src/main/.../coordinator
/17-lattice-review core/domain/src/main/...
```

The agent will:
1. Discover scope (Layer, Module, or Box) from path
2. Apply appropriate metrics
3. Generate compliance report

---

## 2. Scope Detection

| Path Pattern | Scope | Audit Type |
|--------------|-------|------------|
| `feature/*/`, `data/*/` | Layer | Aggregate all modules |
| `.../tingwu/`, `.../chat/` | Module | Audit all boxes |
| Single file or interface | Box | Single component check |

---

## 3. Metrics

### 3.1 Box Compliance (Per Component)

| Check | How | Pass |
|-------|-----|------|
| Interface exists | `grep "interface [Name]"` | ✅ |
| Fake exists | `grep "class Fake[Name]"` | ✅ |
| DTOs co-located | `data class` in interface file | ✅ |
| Returns `Result<T>` | `grep "Result<"` | ✅ |
| Hilt binding | `grep "@Binds.*[Name]"` | ✅ |
| Has tests | `find "*Test.kt"` | ✅ |

### 3.2 Orchestrator Thin-ness

| Metric | Command | Threshold |
|--------|---------|-----------|
| Conditionals | `grep -c "if \|when "` | <5 THIN, 5-15 MEDIUM, >15 FAT |
| Direct API calls | `grep -c "\.api\."` | 0 expected |
| LOC | `wc -l` | <200 THIN, <300 MEDIUM, >300 FAT |

### 3.3 Dependency Direction

| Rule | Violation |
|------|-----------|
| Box imports Box | ❌ CRITICAL |
| Android imports in domain | ❌ Layer leak |
| Orchestrator imports Box | ✅ Expected |

---

## 4. Scoring

| Category | Weight |
|----------|--------|
| Box Compliance (6 checks) | 40% |
| Orchestrator Thin-ness | 20% |
| Dependency Direction | 20% |
| Test Coverage | 20% |

**Aggregate**: For multi-module scope, average module scores.

---

## 5. Output Format

```markdown
# Lattice Compliance Report

**Target**: [path provided]  
**Scope**: Layer / Module / Box  
**Score**: X/100  
**Verdict**: ✅ COMPLIANT / ⚠️ NEEDS WORK / ❌ NON-COMPLIANT

---

## Inventory

| Component | Interface | Fake | DTOs | Result | Hilt | Tests |
|-----------|-----------|------|------|--------|------|-------|
| [Name] | ✅/❌ | ✅/❌ | ✅/❌ | ✅/❌ | ✅/❌ | N |

## Orchestrator Audit

| Name | LOC | Conditionals | API Calls | Verdict |
|------|-----|--------------|-----------|---------|
| [Name] | N | N | N | ✅/⚠️/❌ |

## Violations

| Severity | Issue | Location |
|----------|-------|----------|
| 🔴 | [desc] | [file:line] |
| 🟡 | [desc] | [file:line] |

## Scoring Breakdown

| Category | Score |
|----------|-------|
| Box Compliance | X/40 |
| Orchestrator | X/20 |
| Dependencies | X/20 |
| Tests | X/20 |
| **TOTAL** | **X/100** |

## Action Items

### 🔴 Must Fix
1. [item]

### 🟡 Should Fix
1. [item]
```

---

## 6. Verdicts

| Score | Verdict | Ship? |
|-------|---------|-------|
| 90-100 | ✅ COMPLIANT | Yes |
| 75-89 | ⚠️ NEEDS WORK | With debt logged |
| <75 | ❌ NON-COMPLIANT | No |
