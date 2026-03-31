# Debugging SOP

> **Purpose**: A rigorous, repeatable process for debugging issues based on user-provided evidence and literal runtime proof.
> **Status**: Active SOP
> **Last Updated**: 2026-03-31
> **Homepage**: [`docs/README.md`](../README.md)
> **Control Plane**: [`docs/plans/doc-tracker.md`](../plans/doc-tracker.md)

---

## Evidence Hierarchy

Use this evidence order during debugging:

1. literal runtime proof (`adb logcat`, system output, test output, compiler output)
2. code/spec inspection
3. screenshots, screen recordings, STR, and user reports

Rule:

- for Android runtime bugs, `adb logcat` is mandatory
- screenshots and recollection are supporting evidence only
- do not claim root cause, fix confidence, or non-repro without relevant logcat evidence unless adb is genuinely unavailable
- if adb is unavailable, state that limitation explicitly and lower confidence instead of guessing
- if current logs are too weak, add targeted tags/logging and rerun the repro

## Exception Boundary

- compile/build failures: compiler output is primary
- pure unit-test failures: test output is primary
- Android runtime/UI/device/lifecycle/service/BLE/network/notification/alarm/integration bugs: `adb logcat` is mandatory
- if a test failure is only the surface symptom of an Android runtime issue, capture `adb logcat` for the runtime repro as well

## Small Feature Reality Rule

- if the feature is a small bounded interaction slice, start from the smallest state machine that satisfies the approved flow
- do not add speculative fallback branches, build-type splits, or hidden success masking unless the spec or reproduced runtime evidence requires them
- for Android runtime bugs, use logcat to prove whether failure happened during hold, release, processing, or result delivery before editing code

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
6. Decide whether this is a compiler/test-only failure or an Android runtime bug

If it is an Android runtime bug:

7. identify the relevant log tags before entering diagnosis
8. prepare an `adb logcat` capture plan for the repro

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
3. For Android runtime bugs, clear/capture logcat with the relevant tags before reproducing
4. Reproduce the issue while collecting `adb logcat`
5. Quote the literal logcat evidence that proves the failing branch or missing event
6. Compare screenshot to spec (Literal Alignment)

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
- `logcat` output: [...]  // mandatory for Android runtime bugs
- Spec says: [...] vs Code does: [...]
```

### Bisection (Optional for regression bugs)
```bash
git log --oneline -20 -- [affected file]
```
"Worked in [commit A], broken after [commit B]"

**Checkpoint**: Root cause stated with evidence.

---

## Phase 4: PROPOSE (The Pre-Fix Report)
**[ABSOLUTE ZERO VIOLATION RULE]**

**Goal**: Produce an evidence-based, spec-based report before writing any code. MUST assess gaps and risks proactively.

### The Mandatory Report Format
Before touching code, you **MUST** present the following report to the user:

```markdown
### Pre-Fix Report

**1. Evidence-Based Root Cause**:
- [Literal logcat/grep proof]
- If this is a runtime bug, include the exact `adb logcat` lines or state explicitly why adb was unavailable

**2. Spec Alignment Gate (from 01-senior-reviewr)**:
- [What the spec says] vs [What the code currently does]
- **Literal Audit**: Output of `/06-audit`. If ANY ❌ exists, BLOCK the fix until spec is resolved.
- Is this a code bug or a spec gap?

**3. Proposed Fix**:
- [Exact file and logic changes]

**4. Proactive Risk Assessment & Readiness (Crucial)**:
- **Readiness Score**: (Verified/Total Assumptions × 60) + Evidence(0-20) + Risk(0-20). Minimum score of 90% required to proceed.
- **Potential Gaps**: What edge cases might this fix miss?
- **Blast Radius**: What other modules/features could this break?
- **L1/L2 Testability**: Can this actually be proven in unit tests?
```

**Checkpoint**: Report generated. Wait for user approval before writing code.

---

## Phase 5: FIX (Red-Green)

**Goal**: Fix with minimal change + regression test.

### Rules
| ❌ Don't | ✅ Do |
|----------|------|
| Refactor during bugfix | Fix only the bug |
| "While I'm here" changes | Separate PR for improvements |
| Fix without test | Write failing test FIRST |
| Fix without Report | Provide Pre-Fix Report first |

### Red-Green Process
1. **RED**: Write test that reproduces the bug (fails)
2. **GREEN**: Apply minimal fix (test passes)
3. **VERIFY**: Existing tests still pass

**Checkpoint**: Fix applied, new test written.

---

## Phase 6: VERIFY

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

## Phase 7: CLOSE

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
| Guess from screenshot alone | Capture `adb logcat` for runtime bugs first |
| Treat logcat as optional for runtime issues | Make logcat part of the first diagnostic pass |
| Guess without evidence | Run `/06-audit`, capture logcat, quote literal proof |
| Fix + refactor together | Separate commits |
| Log lessons on build success | Only on "problem fixed" confirmation |
| Change multiple layers at once | One layer per hypothesis |
| Skip STR | Reject intake, request STR |
