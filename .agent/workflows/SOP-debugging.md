---
description: Systematic debugging process with phase gates, severity classification, and evidence-based diagnosis
trigger: auto-invoke on ANY user request containing "bug", "debug", "fix", or "issue"
---

# SOP: Debugging

> **Entry point** for debugging issues following the Debugging SOP.
> **MANDATORY TRIGGER**: All agents must invoke and follow this workflow for any debugging task. Bypassing this workflow is a critical violation.

---

## ⛔ PHASE GATE PRINCIPLE (Mandatory)

> **No phase transition without explicit user approval.**

| Transitiion | Gate |
|-------------|------|
| Entry → INTAKE | Auto (start) |
| INTAKE → DIAGNOSE | 🔒 Approval required |
| DIAGNOSE → HYPOTHESIZE | 🔒 Approval required |
| HYPOTHESIZE → PROPOSE (Report) | 🔒 Approval required |
| PROPOSE → FIX | ❌ ABSOLUTE ZERO VIOLATION: Wait for user approval on Report |
| FIX → VERIFY | 🔒 Approval required |
| VERIFY → CLOSE | 🔒 Approval required |

**Exception**: P0 incidents may skip gates with user acknowledgment.

---

## 📚 Lessons-Learned Check (Mandatory)

> **At Entry and during DIAGNOSE**, check `.agent/rules/lessons-learned.md` for similar past bugs.

If a lesson **matches** the current symptom:
1. **Tell the user**: "📚 Lesson matched: [TITLE] — applying known fix pattern"
2. **Apply** the documented fix pattern
3. **Skip** unnecessary diagnosis if pattern matches exactly

**Format**: `📚 Lesson matched: [Title] — [one-line summary]`

---

## Entry

```markdown
🔧 **Debugging: [Issue Summary]**
📚 Checking lessons-learned...
Reading: docs/sops/debugging.md
```

**Read the SOP**: [`docs/sops/debugging.md`](file:///home/cslh-frank/main_app/docs/sops/debugging.md)

Follow the 7-phase process:
1. **INTAKE** — Screenshot + STR + Environment + Severity → 🔒 Gate
2. **DIAGNOSE** — Trace through Brain/Guard/Face/Mapper → 🔒 Gate
3. **HYPOTHESIZE** — Root cause with evidence → 🔒 Gate
4. **PROPOSE** — Generate mandatory Pre-Fix Report (Evidence, Spec Alignment Gate, Risk, Readiness Score) → ❌ ZERO VIOLATION GATE: Wait for Approval
5. **FIX** — Red-Green (failing test first) → 🔒 Gate
6. **VERIFY** — Build + Tests + E2E → 🔒 Gate
7. **CLOSE** — Update lessons-learned (only after user confirms)

---

## Intake Checklist (MANDATORY)

Before proceeding, confirm user has provided:

- [ ] Screenshot(s) of the issue
- [ ] Expected vs Actual behavior
- [ ] Steps to Reproduce (STR) — **REQUIRED**
- [ ] Environment (Device, OS, App version)

> ⚠️ **REJECT** if STR missing: "I need steps to reproduce before I can debug."

---

## Severity Quick Reference

| Level | Criteria | Response |
|-------|----------|----------|
| **P0** | Crash, data loss | Hotfix NOW, skip gates |
| **P1** | Major broken | Expedited |
| **P2** | Degraded | Normal gates |
| **P3** | Minor polish | Can defer |

---

## Verification Gate (Must Pass Before Close)

```bash
# Gate 1: Build
./gradlew :app:assembleDebug

# Gate 2: Tests
./gradlew testDebugUnitTest
```

- [ ] Build passes
- [ ] Tests pass (including new regression test)
- [ ] Manual E2E confirms fix
- [ ] Screenshot of fixed state captured

---

## Helper Workflows

| Workflow | When to Use |
|----------|-------------|
| `/06-audit` | Evidence-based code trace |
| `/01-senior-reviewr` | Sanity check hypothesis |
| `/cerb-check` | If fix touches UI contract |
