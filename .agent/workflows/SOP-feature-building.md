---
description: Feature development SOP entrance — read SOP, build feature, pass shipping gate
---

# SOP: Feature Building

> **Entry point** for building new features following the Feature Development SOP.

---

## ⛔ PHASE GATE PRINCIPLE (Mandatory)

> **No phase transition without explicit user approval.**

Before moving from one phase to the next, you MUST:
1. **Summarize** what was done in current phase
2. **Request approval** via `notify_user` with `BlockedOnUser: true`
3. **WAIT** for user to say "approved", "proceed", "next", or equivalent

| Transition | Gate |
|------------|------|
| Entry → UNDERSTAND | Auto (start) |
| UNDERSTAND → VALIDATE | 🔒 Approval required |
| VALIDATE → REGISTRY AUDIT | 🔒 Approval required |
| REGISTRY AUDIT → IMPLEMENT | 🔒 Approval required |
| IMPLEMENT → VERIFY | 🔒 Approval required |
| VERIFY → SHIP | 🔒 Approval required |

**Why**: Prevents runaway execution. Complex features need human judgment at phase boundaries.

---

## 📚 Lessons-Learned Check (Mandatory)

> **At Entry**, check `.agent/rules/lessons-learned.md` for patterns related to this feature area.

If a lesson **matches** the current work:
1. **Tell the user**: "📚 Lesson applied: [TITLE] — avoiding known pitfall"
2. **Proactively apply** the documented pattern
3. **Avoid** repeating past mistakes

**Format**: `📚 Lesson applied: [Title] — [one-line summary]`

---

## Entry

```markdown
🔨 **Feature Building: [Feature Name]**
📚 Checking lessons-learned...
Reading: docs/sops/feature-development.md
```

**Read the SOP**: [`docs/sops/feature-development.md`](file:///home/cslh-frank/main_app/docs/sops/feature-development.md)

Follow the 5-phase process:
1. **UNDERSTAND** — Read GLOSSARY → INDEX → Module → Flow specs → 🔒 Gate
2. **VALIDATE** — Map E2E flow, identify Trinity layers → 🔒 Gate
3. **REGISTRY AUDIT** — Check `ui_element_registry.md` for component definitions → 🔒 Gate
4. **IMPLEMENT** — Interface → Fake → Impl → Hilt → 🔒 Gate
5. **VERIFY** — Run shipping gate (below) → 🔒 Gate
6. **SHIP** — Update tracker, done

---

## Shipping Gate (Must Pass Before Ship)

Run these checks in order. **ALL must pass.**

### Gate 1-3: Prism Verification
```
/prism-check
```
- Gate 1 (Registry): Visual physics ✅
- Gate 2 (Contract): Spec alignment ✅
- Gate 3 (Architecture): Box Compliance ✅

### Gate 4: Build
```bash
./gradlew :app:assembleDebug
```

### Gate 5: Tests
```bash
./gradlew testDebugUnitTest
```

### Gate 6: E2E Flow (REQUIRED)

> ⚠️ **BLOCKER**: Feature MUST work end-to-end before shipping.

Manual test the complete user journey:
- [ ] User can trigger the feature
- [ ] Expected behavior occurs
- [ ] No regressions in related flows

**If E2E fails → FIX before proceeding. No exceptions.**

### Gate 7: Docs
Update `docs/plans/tracker.md` with feature status.

---

## Ship Checklist

```markdown
- [ ] /prism-check passes all 3 gates
- [ ] Build passes
- [ ] Tests pass
- [ ] E2E works
- [ ] tracker.md updated
```

**All checks pass → Feature shipped. Workflow ends.**

---

## Helper Workflows

| Workflow | When to Use |
|----------|-------------|
| `/prism-check` | Pre-ship verification (all 3 gates) |
| `/agent-visibility` | If feature touches agent activity |
| `/01-senior-reviewr` | Sanity check design decisions |
| `/06-audit` | Evidence-based code analysis |
