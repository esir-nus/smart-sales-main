---
description: Run manual physical device tests (L2/L3) with adb log monitoring and agent-evaluated pass/fail
---

# On-Device Test Runner (L2/L3)

Agent-assisted manual testing. Agent monitors adb logs while user tests on device.

## Flow

```
Agent: Lists test items + expected logs → starts adb logcat
User:  Tests on device → returns "done" or describes what happened
Agent: Reads adb logs → evaluates PASS/FAIL per test item
```

---

## Phase 1: Test Plan (Agent)

Generate a test checklist in this format:

```markdown
### T1: [Test Name]
**Action**: [Exact steps for user]
**Expected UI**: [What user should see]
**Expected Logs**: [adb log lines that MUST appear]
**Negative Check**: [What should NOT happen]
```

Rules:
- Each test item must have at least ONE expected log pattern
- Expected logs use the exact `Log.d` tag from code (grep first)
- Include at least one negative test (error path, missing data)

---

## Phase 2: Start Monitoring (Agent)

// turbo-all

```bash
# Clear old logs
adb logcat -c

# Start monitoring with relevant tags
adb logcat -s TAG1:D TAG2:D TAG3:D | head -200
```

Run this as a background command. Store the command ID.

---

## Phase 3: User Tests

Tell the user:
1. The exact test checklist (from Phase 1)
2. "Test on device, then come back and say 'done' or describe any issues"

Use `notify_user` with `BlockedOnUser = true`.

---

## Phase 4: Evaluate & Document (Agent)

When user returns:

1. Read adb logcat output from the background command
2. For each test item, evaluate Pass/Fail based on explicit log evidence.
3. Generate a formal Test Execution Record (TER) using the following template:

```markdown
# L2/L3 On-Device Test Record: [Component / Feature Name]

**Date**: [YYYY-MM-DD]
**Tester**: [Agent/User]
**Target Build**: `[Module]:assembleDebug`

---

## 1. Test Context & Entry State
* **Objective**: [Brief description]
* **Testing Medium**: [e.g., L2 Debug HUD Injection (Mock) OR L3 Physical Device Test (Real LLM)]
* **Initial Device State**: [e.g., Fresh app launch, Agent timeline empty].

## 2. Execution Plan
* **Trigger Action**: [e.g., Tapped "Test XYZ" in HUD].
* **Input Payload**: [Brief description of injected data].

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | [Expected visual] | [Actual visual from user] | ✅/❌ |
| **Log Evidence** | `[Expected log]` | `[Actual log snippet]` | ✅/❌ |
| **Negative Check**| [What shouldn't happen] | [What actually didn't happen] | ✅/❌ |

---

## 4. Deviation & Resolution Log
*(Only filled if initial passes failed)*
* **Attempt 1 Failure**: [Description]
  - **Root Cause**: [Why it failed]
  - **Resolution**: [How it was fixed]

## 5. Final Verdict
**[✅ SHIPPED / ❌ FAILED]**. 
```

4. **Save the Record**: Write the populated template to a new file in `docs/reports/tests/L[2|3]-[YYYYMMDD]-[FeatureName].md`.
5. **Update Tracker**: Add a link to the new test report in `docs/plans/tracker.md` under the relevant Epic or Tech Debt item.

---

## Key Principles

1. **Logs are evidence, not user reports.** User says "it worked" but logs show error → FAIL.
2. **Negative tests are mandatory.** Always test the "no data" / "missing entity" path.
3. **Cache tests require two passes.** First expand = generation. Second expand = cache hit.
