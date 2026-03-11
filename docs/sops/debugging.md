# Debugging SOP

> **Purpose**: A rigorous, repeatable process for debugging issues based on user-provided evidence (screenshots, descriptions, STR).

---

## When to Use

- User reports a bug with visual evidence
- UI doesn't match spec
- Feature works in one environment but not another
- Regression after code change

---

## Phase 1: INTAKE (Evidence Gathering)

**Goal**: Collect complete evidence before touching code.

### Required from User (MANDATORY)

| Field | Required | Description |
|-------|----------|-------------|
| **Screenshot(s)** | ✅ | Visual evidence of the issue |
| **Expected vs Actual** | ✅ | "Expected: X. Actual: Y." |
| **Steps to Reproduce** | ✅ | Numbered steps. No STR = No debug. |
| **Environment** | ✅ | Device, OS version, App version |

> ⚠️ **REJECT** incomplete intakes. "I can't reproduce without STR."

### Severity Classification

| Level | Criteria | Gate Policy | MTTR Target |
|-------|----------|-------------|-------------|
| **P0** | Crash, data loss, security | Skip gates, hotfix NOW | < 1 hour |
| **P1** | Major feature broken | Expedited gates | < 4 hours |
| **P2** | Degraded experience | Normal gates | < 1 day |
| **P3** | Minor UX polish | Normal gates | Can defer |

### Agent Actions
1. Save screenshot to artifacts
2. Confirm STR is complete (reject if missing)
3. Classify severity: `P[0-3]`
4. Summarize: `"P[X]: Expected: [A]. Actual: [B]."`
5. Identify **affected module** from spec index

**Checkpoint**: Can summarize bug in one sentence with severity.

---

## Phase 2: DIAGNOSE (Trace the Bug)

**Goal**: Find WHERE in the code reality diverges from spec.

### Diagnostic Layers

| Layer | What to Check | Tools |
|-------|---------------|-------|
| **Brain** | LLM prompt, response format | View `DashscopeExecutor` |
| **Guard** | Linter parsing, validation | View `*Linter.kt` |
| **Face** | UI rendering, state binding | View `*Screen.kt`, `*Card.kt` |
| **Mapper** | Domain → UI transformation | View `*Drawer.kt`, `*ViewModel.kt` |

### Agent Actions
1. Run `/06-audit` on affected files
2. Trace data flow: `User Input → Pipeline → UI`
3. Add logcat if needed: `adb logcat -s [Tag]:D`
4. Compare screenshot to spec (Literal Alignment)

### Key Question
> "At which layer does reality diverge from spec?"

**Checkpoint**: Layer identified, evidence collected.

---

## Phase 3: HYPOTHESIZE (Root Cause)

**Goal**: State the root cause with evidence.

### Template
```markdown
**Symptom**: [What user sees in screenshot]
**Severity**: P[X]
**Layer**: [Brain/Guard/Face/Mapper]
**Root Cause**: [Technical reason it happens]
**Evidence**: 
- `grep` output: [...]
- `logcat` output: [...]
- Spec says: [...] vs Code does: [...]
```

### Bisection (Optional for regression bugs)
```bash
git log --oneline -20 -- [affected file]
```
"Worked in [commit A], broken after [commit B]"

**Checkpoint**: Root cause stated with evidence.

---

## Phase 4: FIX (Red-Green)

**Goal**: Fix with minimal change + regression test.

### Rules
| ❌ Don't | ✅ Do |
|----------|------|
| Refactor during bugfix | Fix only the bug |
| "While I'm here" changes | Separate PR for improvements |
| Fix without test | Write failing test FIRST |

### Red-Green Process
1. **RED**: Write test that reproduces the bug (fails)
2. **GREEN**: Apply minimal fix (test passes)
3. **VERIFY**: Existing tests still pass

**Checkpoint**: Fix applied, new test written.

---

## Phase 5: VERIFY

**Goal**: Confirm bug is fixed E2E.

### Required Checks

| Check | Command/Action |
|-------|----------------|
| Build | `./gradlew :app:assembleDebug` |
| Tests | `./gradlew testDebugUnitTest` |
| New Test | Verify new test is included and passes |
| Manual E2E | Reproduce STR → confirm fixed |
| Screenshot | Capture fixed state |

**Checkpoint**: All checks pass, build green.

---

## Phase 6: CLOSE

**Goal**: Document and prevent regression.

### Actions
1. **Update `.agent/rules/lessons-learned.md`** — ONLY after user confirms "fixed"
2. **Create pattern file** if reusable (e.g., `compose-scrim-drawer-pattern.md`)
3. **Update tracker** if feature status changed

### Lessons Entry Format
```markdown
### [SHORT TITLE] — [DATE]

**Symptom**: What the user reported  
**Root Cause**: The actual problem  
**Wrong Approach**: What didn't work  
**Correct Fix**: What worked  
**File(s)**: Where the fix was applied
```

---

## Quick Reference

### Common Logcat Tags
```bash
adb logcat -s SchedulerVM:D IntentOrchestrator:D DashscopeExecutor:D
```

### Diagnostic Workflows
| Scenario | Workflow |
|----------|----------|
| Need code trace | `/06-audit` |
| UI mismatch | Compare screenshot → spec ASCII → code |
| LLM output wrong | Check `DashscopeExecutor` prompt |
| Data not reaching UI | Check mapper in Drawer/ViewModel |

### Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Guess without evidence | Run `/06-audit`, add logcat |
| Fix + refactor together | Separate commits |
| Log lessons on build success | Only on "problem fixed" confirmation |
| Change multiple layers at once | One layer per hypothesis |
| Skip STR | Reject intake, request STR |
