---
description: Run L2 manual tests on device with adb log monitoring and agent-evaluated pass/fail
---

# L2 On-Device Test Runner

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

## Phase 4: Evaluate (Agent)

When user returns:

1. Read adb logcat output from the background command
2. For each test item, check:
   - ✅ PASS: Expected log lines found
   - ❌ FAIL: Expected log lines missing or error logs present
   - ⚠️ PARTIAL: Some expected logs found, some missing
3. Produce evaluation report:

```markdown
## L2 Test Report

| # | Test | Log Evidence | Result |
|---|------|-------------|--------|
| T1 | [name] | `💡 Tips loaded: 3 tips` | ✅ PASS |
| T2 | [name] | Missing `📝 Prompt built` | ❌ FAIL |

### Evidence
[Paste relevant log lines]

### Verdict
X/Y tests passed. [Ship / Debug]
```

---

## Key Principles

1. **Logs are evidence, not user reports.** User says "it worked" but logs show error → FAIL.
2. **Negative tests are mandatory.** Always test the "no data" / "missing entity" path.
3. **Cache tests require two passes.** First expand = generation. Second expand = cache hit.
