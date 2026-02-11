---
trigger: always_on
description: Docs-first development — no code without docs, no code change without doc sync
---

# Docs-First Protocol

> **Root Cause**: Code ships without spec/tracker updates → docs drift → agents build from stale context → bugs.

---

## The Rule

```
NO CODE WITHOUT DOCS.
NO CODE CHANGE WITHOUT DOC SYNC.
```

---

## Decision Tree

```
Starting work on feature/bug/change...
│
├─ Does docs/cerb/[feature]/spec.md exist?
│   ├─ NO → CREATE spec.md + interface.md first (/cerb-spec-template)
│   └─ YES → Read it COMPLETELY
│       │
│       ├─ Does spec cover this change?
│       │   ├─ NO → UPDATE spec first, then implement
│       │   └─ YES → Implement (spec is SOT)
│       │
│       └─ After implementation:
│           ├─ Did domain models change? → UPDATE spec model section
│           ├─ Did UI states change? → UPDATE spec states table
│           ├─ Did wave status change? → UPDATE spec wave table
│           └─ Did tech debt emerge? → UPDATE tracker.md
```

---

## Three Gates

### Gate 1: BEFORE Code (Pre-Flight)

| Check | Action if FAIL |
|-------|----------------|
| Cerb doc exists? | CREATE via `/cerb-spec-template` |
| Spec covers this behavior? | UPDATE spec, then implement |
| Tracker reflects current wave? | UPDATE tracker, then implement |

### Gate 2: DURING Code (Mid-Flight)

| If You... | Then Also... |
|-----------|-------------|
| Add a domain model variant | Add it to spec's Domain Models |
| Add a UI state | Add it to spec's States table |
| Change an interface signature | Update interface.md |

### Gate 3: AFTER Code (Post-Flight)

| Check | Action |
|-------|--------|
| Wave status changed? | Update spec wave table (🔲 → ✅ SHIPPED) |
| New tech debt identified? | Append to tracker.md Tech Debt section |
| Implementation status changed? | Update spec implementation table |
| Ship criteria met? | Check boxes in spec |

---

## Enforcement

**Add to every implementation plan:**

```markdown
### Doc Sync Checklist
- [ ] Spec read before planning
- [ ] Spec covers the behavior (or updated to cover it)
- [ ] Domain models in spec match code
- [ ] UI states in spec match code
- [ ] Wave status updated after shipping
- [ ] Tracker debt updated if applicable
```

**After `/04-doc-sync`, verify with `/cerb-check`.**

---

## Quick Commands

```bash
# Verify spec exists for feature
ls docs/cerb/[feature]/spec.md

# Check spec model matches code
grep "sealed class\|data class\|data object" app-prism/src/main/java/.../domain/[feature]/

# Find stale wave statuses
grep -n "🔲\|🔧" docs/cerb/*/spec.md
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Ship code, update docs "later" | Update docs in same session |
| Add states to code without spec | Update spec table first |
| Mark wave shipped without updating spec | Update wave table + ship criteria |
| Invent behavior not in spec | Flag as spec gap, ask USER |
| Skip doc update for "small" changes | Every state/model change syncs |
