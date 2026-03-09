# E2E Test Tasklist & Tracking (spec.md)

> **State**: SPEC_ONLY
> **Current Target**: Wave 1: The Lightning Fast-Track

This document is the **dynamic tracker and implementation spec** for our End-to-End testing domain. It hooks into automatic updates and tracks the execution of tests against the 6 Core Pillars. 

*For the static, hard-defined rules and mental models, see `testing-protocol.md`.*

---

## The E2E Implementation Waves

| Wave | Pillar Focus | Status | Notes |
|------|--------------|--------|-------|
| 1 | The Lightning Fast-Track | ✅ SHIPPED | Routing & State Management bypassing heavy analysis |
| 2 | The Dual-Engine Bridge | 🔲 | Context RAM dynamically syncing with SSD DB writes |
| 3 | Strict Interface Integrity | 🔲 | Linters catching poisoned/hallucinated data at boundary |
| 4 | The Adaptive Habit Loop | 🔲 | RL Module writes outlasting app restarts |
| 5 | The Efficiency Overload | 🔲 | Alias queries bypassing the Agent entirely |
| 6 | The Transparent Mind | 🔲 | Real-time state emissions to the UI Thinking Box |

---

## Task Blackboard (The Crucibles)

### Wave 1: The Lightning Fast-Track 
- [x] **L2 Sim**: Inject `GREETING` intent. Verify `LightningRouter` short-circuits to `MascotService` without triggering `Executor`.
- [x] **L3 Manual**: Speak "Hello" into badge. Verify instant auditory response with zero delay in UI.
- [x] **Acceptance Team**: Check passed.

### Wave 2: The Dual-Engine Bridge
- [ ] **L2 Sim**: Run a two-turn simulation adjusting CRM record value mid-stream without deadlocking.
- [ ] **L3 Manual**: Natural multi-sentence interruption verifying CRM delta.
- [ ] **Acceptance Team**: Check passed.

### Wave 3: Strict Interface Integrity
- [ ] **L2 Sim**: Inject poisoned/hallucinated `TargetId`. Verify Interface Linter aborts SSD write.
- [ ] **L3 Manual**: Speak confusing, contradictory prompt; verify graceful clarification.
- [ ] **Acceptance Team**: Check passed.

### Wave 4: The Adaptive Habit Loop
- [ ] **L2 Sim**: Reject proposal 3 times -> verify `HabitScore` shifts on SSD.
- [ ] **L3 Manual**: Explicitly retrain the Agent over repeated days.
- [ ] **Acceptance Team**: Check passed.

### Wave 5: The Efficiency Overload
- [ ] **L2 Sim**: Assert `Alias` routing natively returns pre-compiled DB result, bypassing LLM.
- [ ] **L3 Manual**: Tap shortcut; verify sub-second loading vs generic query latency.
- [ ] **Acceptance Team**: Check passed.

### Wave 6: The Transparent Mind
- [ ] **L2 Sim**: EventBus emits exact state array matches (`STATE_RETRIEVING`, `STATE_EXECUTING`).
- [ ] **L3 Manual**: Ask complex question; verify UI Thinking Box exactly matches backend.
- [ ] **Acceptance Team**: Check passed.

---

## Changelog
*Hooks to automatic updates as waves are shipped. Do not place static rules here.*

- **2026-03-09**: Initialized cerb-e2e-test domain. Separated static testing protocols (`testing-protocol.md`) from this dynamic task tracking (`spec.md`). Defined the 6 initial E2E Waves.
