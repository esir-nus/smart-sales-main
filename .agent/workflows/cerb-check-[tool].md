---
description: Audit a doc for Cerb (context boundary) + OS Model compliance
---

# Cerb Doc Check

Verify that a Cerb doc follows context boundary principles and OS Model architecture.

---

## When to Use

- **Mandatory**: After completing Wave 1 of `/cerb-spec-template`
- **Optional**: When updating existing Cerb docs (run on modified files only)
- **Migration**: Auditing legacy docs for Cerb compliance

---

## Prerequisites

This workflow validates docs created via `/cerb-spec-template`.

Expected structure:
```
docs/cerb/[feature]/
├── interface.md  → Consumer contract
└── spec.md       → Internal implementation
```

---

## Audit Checklist

### 1. Link Analysis

| Check | Pass | Fail |
|-------|------|------|
| **Spec → Interface** | ✅ Terminal link | — |
| **Spec → Glossary** | ✅ Quick lookup | — |
| **Spec → Code file** | ✅ Implementation ref | — |
| **Spec → Another Spec** | ⚠️ Avoid | ❌ Chain risk |
| **"See X for details" → external doc** | — | ❌ Context break |

**Run:** `grep -n "See\|see\|参见" [doc.md]` to find cross-references.

### 2. Self-Containment

| Question | Expected |
|----------|----------|
| Can agent complete task reading ONLY this doc? | ✅ Yes |
| Does doc require reading another spec to understand? | ❌ No |
| Are all domain models defined inline? | ✅ Yes |
| Are edge cases inline, not linked? | ✅ Yes |
| Does `spec.md` contain a Wave Plan table? | ✅ Yes |

### 3. Interface Clarity

| Check | For `interface.md` |
|-------|-------------------|
| Method signatures listed? | ✅ Required |
| Input/Output types defined? | ✅ Required |
| "You Should NOT" section present? | ✅ Required |
| Links to implementation internals? | ❌ Forbidden |

### 4. OS Model Compliance

> **Reference**: `docs/specs/os-model-architecture.md`

#### Layer Declaration

Every spec MUST declare its OS layer role:

| Layer | Role | Expected in Spec |
|-------|------|-------------------|
| **RAM (Application)** | Reads/writes through SessionWorkingSet | `interface.md` methods accept/return WorkingSet data |
| **SSD (Storage)** | Permanent storage, source of truth | No session dependency, no RAM references |
| **Kernel** | Manages RAM lifecycle | Owns SessionWorkingSet, loads from SSD |
| **File Explorer** | Reads SSD directly (dashboards) | No session dependency, reads repos directly |

**Check**: `grep -n "OS Layer\|RAM\|SSD\|Kernel\|Working Set" docs/cerb/[feature]/spec.md`

#### Interaction Rules Audit

| Rule | Check | Fail Signal |
|------|-------|---------|
| Apps work on RAM | Does spec reference direct repo access? | `import.*Repository` in domain code |
| Write-Through | Does spec mention flush/sync/batch? | Any deferred persistence |
| Kernel owns lifecycle | Does spec load its own data from SSD? | `getById()` calls outside ContextBuilder |
| SSD = Source of Truth | Does spec claim RAM is authoritative? | RAM treated as permanent |

---

## Output Format

```markdown
## Cerb Audit: [doc name]

### Link Analysis
| Link | Target | Type | Verdict |
|------|--------|------|---------|
| L42 | Prism-V1.md §5 | Spec→Spec | ❌ Chain |
| L78 | glossary.md | Spec→Glossary | ✅ Terminal |

### Self-Containment: [PASS/FAIL]
- [x] Domain models inline
- [ ] Missing: Edge cases linked to external doc

### Interface Clarity: [PASS/FAIL] (if interface.md)
- [x] Method signatures
- [x] I/O types
- [ ] Missing: "You Should NOT" section

### OS Model Compliance: [PASS/FAIL]
- [ ] OS Layer declared (RAM App / SSD / Kernel / File Explorer)
- [x] Interaction rules respected
- [ ] Missing: No direct repo access pattern documented

### Verdict
**[PASS / NEEDS WORK / FAIL]**

### Fixes Required
1. Inline content from Prism-V1.md §5.2 (lines 42-60)
2. Add "You Should NOT" section to interface.md
3. Declare OS Layer role in spec header
```

---

## Quick Commands

```bash
# Find all cross-references in a doc
grep -n "See\|see\|参见\|§\|\.md" docs/cerb/memory-center/spec.md

# Count links to other specs
grep -c "docs/specs/" docs/cerb/memory-center/spec.md

# Verify no Prism-V1.md references
grep -n "Prism-V1" docs/cerb/memory-center/spec.md
```

---

## Anthropic Alignment

This workflow enforces:
1. **Simplicity** — No unnecessary cross-references
2. **Transparency** — Clear interface contracts
3. **Well-crafted interfaces** — Explicit "You Should NOT" boundaries
