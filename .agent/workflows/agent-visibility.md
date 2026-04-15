---
description: Audit agent thinking visibility - Thinking Box, Toast, Notifications, Pop-outs
last_reviewed: 2026-04-13
---

# Agent Visibility Audit

A **UX Specialist persona-driven audit** for evaluating how intelligently the app exposes agent activity to users.

> **Philosophy**: The user should *feel* the agent is smart. This workflow measures that feeling.

---

## When to Use

| Scenario | Use This? |
|----------|-----------|
| "App feels dumb/unresponsive" | ✅ Yes |
| "User doesn't know what's happening" | ✅ Yes |
| New feature touching agent visibility | ✅ Yes |
| Code-level debugging | ❌ Use `/06-audit` |

---

## Inputs

- **Screenshots** (optional): Attach images of the current UI state
- **User complaint** (optional): "Nothing happens when I tap X"
- **Feature context**: What flow are we auditing?

---

## Visibility Channels

| Channel | Purpose | Key Question |
|---------|---------|--------------|
| **Thinking Box** | Show agent cognition | Is the trace visible and meaningful? |
| **Pseudo-Thinking** | Simulate processing | Does it feel authentic, not fake? |
| **Toast** | Transient feedback | Is timing right (2-5s)? |
| **In-App Popout** | Blocking dialog | Is it used sparingly? |
| **Banner** | Persistent status | Does it clear when resolved? |
| **System Notification** | Background alert | Is priority appropriate? |
| **Agent Activity Banner** | Inline thinking | Does it shimmer? Collapse correctly? |

---

## Agent Intelligence Score

Rate each dimension. Multiply by weight. Sum for total.

| Dimension | Weight | Question | Score (0-100) |
|-----------|--------|----------|---------------|
| **Responsiveness** | 25% | Does the app react instantly to input? | __ |
| **Transparency** | 25% | Can user see what the agent is thinking? | __ |
| **Anticipation** | 20% | Does the agent predict user needs? | __ |
| **Recovery** | 15% | Does the agent handle errors gracefully? | __ |
| **Personality** | 15% | Does the agent have a consistent 'voice'? | __ |

**Total Score**: `(R×0.25) + (T×0.25) + (A×0.20) + (Rc×0.15) + (P×0.15)`

| Score Range | Verdict |
|-------------|---------|
| 80-100 | ✅ Excellent — Agent feels intelligent |
| 60-79 | 🟡 Acceptable — Room for improvement |
| 40-59 | 🟠 Weak — User likely confused |
| 0-39 | 🔴 Poor — Feels broken |

---

## Spec vs Reality Audit

Compare what `prism-ui-ux-contract.md` says vs what user actually sees.

| Spec Says | Reality | Status |
|-----------|---------|--------|
| *Copy from spec* | *Describe what you see* | ✅ / ❌ |

**Example**:
| Spec Says | Reality | Status |
|-----------|---------|--------|
| Trace auto-collapses after 3 lines | Collapses after 2 lines | ❌ Gap |
| Toast shows for 3s | Shows for 2s | ❌ Gap |
| Shimmer on Thinking Box header | Missing | ❌ Gap |

---

## Output Template

```markdown
## 📊 Agent Intelligence Score: __/100

| Dimension | Score | Notes |
|-----------|-------|-------|
| Responsiveness | __ | |
| Transparency | __ | |
| Anticipation | __ | |
| Recovery | __ | |
| Personality | __ | |

## 🔍 Areas to Improve
1. [Priority 1]
2. [Priority 2]
3. [Priority 3]

## 📋 Spec vs Reality
| Spec Says | Reality | Status |
|-----------|---------|--------|
| ... | ... | ❌ |

## 🔧 Actionable Steps
1. [UX-level fix, not code]
2. [UX-level fix, not code]
```

---

## Escalation

| Condition | Action |
|-----------|--------|
| Score < 60 | Run `/cerb-check` to sync spec and code |
| Spec gaps found | Update `prism-ui-ux-contract.md` |
| Code doesn't match spec | Run `/06-audit` for code fix |

---

## Examples

**User**: "The app feels frozen when I ask a question"

**Audit Result**:
- **Responsiveness**: 40 (No immediate feedback)
- **Transparency**: 50 (Thinking Box appears late)
- **Total**: 58/100 🟠 Weak

**Fix**: Add immediate ticker on input, show Thinking Box within 100ms.
