---
description: Audit spec-code alignment with evidence, then sync in correct direction
---

# Prism Alignment Check

Verify alignment between Prism specs and codebase through evidence-based auditing.

**Philosophy:** Neither spec nor code is always right. This workflow finds gaps, then decides which source should win based on evidence.

---

## When to Use

- Before major features (pre-flight alignment check)
- After implementation (post-mortem sync)
- Periodic hygiene (quarterly spec review)
- When behavior seems "off" (bug vs spec-mismatch)

---

## Step 1: Define Audit Scope

Pick ONE of these scopes per run:

| Scope | Example | Files to Check |
|-------|---------|----------------|
| **Component** | "Disambiguation Picker" | Specific UI + related Prism section |
| **Flow** | "Voice Intent → Scheduler" | End-to-end pipeline |
| **Module** | "Memory System" | All classes in `data/ai-core/memory/` |
| **Section** | "Prism §5.4" | Everything referencing that section |

**Write down your scope before proceeding.**

---

## Step 2: Evidence Gathering (Spec → Code)

For each key claim in the spec, verify with grep/view_file:

```markdown
### Claim: [Statement from spec]
**Spec Location**: Prism-V1.md §X.Y, line N
**Verification**: `grep -rn "KeyTerm" app/src/`
**Result**: VERIFIED | MISSING | DIFFERENT
**Evidence**: [command output or file:line reference]
```

**Minimum 5 claims per audit.**

### Example Claims to Verify

| Spec Section | What to Check |
|--------------|---------------|
| §5.4 Entity Disambiguation | Is `AliasMapping` schema in Room entity? |
| §4.7 Rethink | Does `decisionLogJson` exist? Is it populated? |
| §2.2 Context Builder | Is `EnhancedContext` data class present? |
| §6 Recovery Queue | Is `LocalRecoveryQueue` Room table present? |
| UX §3.16 | Does picker UI call scoring algorithm? |

---

## Step 3: Evidence Gathering (Code → Spec)

Find behaviors in code NOT documented in spec:

```bash
# Find public APIs/data classes that might be undocumented
grep -rn "data class.*Entry\|data class.*State\|sealed class" app/src/ --include="*.kt"

# Find feature flags or modes
grep -rn "enum class.*Mode\|object.*Feature" app/src/ --include="*.kt"
```

For each finding:
```markdown
### Code Pattern: [Class/Function name]
**Location**: file:line
**Documented?**: YES (spec §X) | NO | PARTIAL
**Notes**: [What it does]
```

---

## Step 4: Gap Analysis

Categorize all gaps into:

| Gap Type | Description | Count |
|----------|-------------|-------|
| **SPEC_MISSING** | Code exists, spec doesn't mention it | |
| **CODE_MISSING** | Spec defines it, code doesn't have it | |
| **MISMATCH** | Both exist, but behavior differs | |
| **STALE** | Spec references deleted code | |

---

## Step 5: Decide Resolution Direction

For EACH gap, apply this decision tree:

```
┌─────────────────────────────────────────────────────────────┐
│ Is the CODE behavior correct and intentional?              │
│ (Was it a deliberate improvement? Does it work better?)    │
└──────────────────────────┬──────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
             YES                        NO
              │                         │
              ▼                         ▼
    ┌─────────────────┐      ┌─────────────────┐
    │  🟢 SPEC WINS   │      │  🔵 CODE WINS   │
    │  (Fix the code) │      │  (Update spec)  │
    └─────────────────┘      └─────────────────┘
```

### Decision Criteria

| Favor CODE (update spec) when... | Favor SPEC (fix code) when... |
|----------------------------------|-------------------------------|
| Code is battle-tested in production | Spec is recently reviewed/approved |
| Code handles edge case spec missed | Code is a quick hack that shipped |
| Spec was written before implementation revealed constraints | Code contradicts architectural intent |
| Multiple devs rely on current behavior | Code introduces tech debt or coupling |

**Default bias: Favor CODE unless spec was explicitly designed to prevent the code pattern.**

---

## Step 6: Execute Resolution

### If SPEC updates (Code Wins):

1. Open the relevant spec file
2. Update to match reality
3. Add changelog entry:
   ```markdown
   | V1.X | DATE | §X.Y updated to match implementation: [brief description] |
   ```
4. Verify cross-references still valid

### If CODE updates (Spec Wins):

1. Create implementation task
2. Reference spec section in commit: `fix: align with Prism §X.Y`
3. After fix, re-run Step 2 verification

---

## Step 7: Update Validation Matrix

If this is a UX flow, update `prism-ui-ux-contract.md §4 Validation Matrix`:

| Flow ID | Prism Section | Component/Mechanic | Status |
|---------|---------------|--------------------|--------|
| 3.X | §Y.Z | [Description] | ✅ |

---

## Output Template

```markdown
# Prism Alignment Audit: [Scope]
**Date**: YYYY-MM-DD
**Auditor**: [Agent/Human]

## Scope
[What was audited]

## Findings

| # | Gap Type | Location | Resolution | Status |
|---|----------|----------|------------|--------|
| 1 | MISMATCH | §5.4 vs DisambiguationPicker.kt | SPEC WINS | TODO |
| 2 | SPEC_MISSING | SmartAlarm logic in AlarmScheduler.kt | CODE WINS | Done |
| ... | | | | |

## Changes Made

### Spec Updates
- Prism-V1.md: [description]

### Code Updates
- [file]: [description]

## Next Audit
Recommended scope: [suggestion based on findings]
```

---

## Quick Audit Checklist

- [ ] Scope defined (component/flow/module/section)
- [ ] 5+ spec claims verified with grep/view_file
- [ ] Code patterns checked for undocumented behavior
- [ ] Each gap categorized (SPEC_MISSING, CODE_MISSING, MISMATCH, STALE)
- [ ] Resolution direction decided for each gap
- [ ] Changes executed
- [ ] Validation matrix updated (if applicable)
- [ ] Changelog entry added to spec (if updated)
