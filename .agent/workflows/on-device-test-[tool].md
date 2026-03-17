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
**Telemetry (GPS) Route**: [Expected VALVE_PROTOCOL checkpoints (e.g. INPUT_RECEIVED -> PATH_A_PARSED -> UI_STATE_EMITTED)]
**Expected Logs**: [Implementation-specific Log.d tags that MUST appear]
**Negative Check**: [What should NOT happen]
```

Rules:
- **Telemetry traces are mandatory.** You MUST define the expected `PipelineValve` (GPS) route based on the architectural data path.
- Expected logs use the exact `Log.d` tag from code for implementation-specific details.
- Include at least one negative test (error path, missing data)

---

## Phase 2: Start Monitoring (Agent)

// turbo-all

```bash
# Clear old logs
adb logcat -c

# Start monitoring with GPS Telemetry ALWAYS ON + relevant tags
adb logcat -s VALVE_PROTOCOL:I TAG1:D TAG2:D | head -200
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
| **Telemetry (GPS)** | `[Expected route (e.g. INPUT -> DB -> UI)]` | `[Actual checkpoints hit]` | ✅/❌ |
| **Log Evidence** | `[Expected internal Log.d]` | `[Actual log snippet]` | ✅/❌ |
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

1. **Telemetry is the ONLY mathematical proof of data survival.** A random `Log.d` deep in a repository doesn't prove the data surfaced to the UI. You must track the full `VALVE_PROTOCOL` GPS route (Layer 0 to Layer 4) to eliminate OS ghosting.
2. **Logs are evidence, not user reports.** User says "it worked" but logs show error → FAIL.
3. **Negative tests are mandatory.** Always test the "no data" / "missing entity" path.
4. **Cache tests require two passes.** First expand = generation. Second expand = cache hit.
