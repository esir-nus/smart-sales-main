---
description: Audit a doc for Cerb (context boundary) compliance
---

# Cerb Doc Check

Verify that a Cerb doc follows context boundary principles.

---

## When to Use

- **Mandatory**: After completing Wave 1 of `/feature-cerb-pattern`
- **Optional**: When updating existing Cerb docs (run on modified files only)
- **Migration**: Auditing legacy docs for Cerb compliance

---

## Prerequisites

This workflow validates docs created via `/feature-cerb-pattern`.

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

### Verdict
**[PASS / NEEDS WORK / FAIL]**

### Fixes Required
1. Inline content from Prism-V1.md §5.2 (lines 42-60)
2. Add "You Should NOT" section to interface.md
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
