# E2E Test Tasklist & Tracking (spec.md)

> **State**: GREAT_ASSEMBLY_AUDIT
> **Current Target**: Anti-Illusion Test Overhaul (Reviewing Assembled Layers)

This document is the **dynamic tracker, implementation spec, and North Star roadmap** for the Great Assembly Audit and our End-to-End testing domain. It hooks into automatic updates and tracks the execution of tests against the 6 Core Pillars, guarded by the 'fair judge': the Anti-Illusion Protocol. 

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

### 🚨 The Great Assembly Audit: Anti-Illusion Overhaul Roadmap
> **Context**: The Anti-Illusion protocol is the fair judge of the Great Assembly. We are looking back to rigorously evaluate the layers we have already assembled before adding new features. We cannot test E2E flows on an illusion. All downstream E2E testing relies on the Pipeline routing intents correctly based on physical information gates, not mocked success.

#### Phase 1: Layer 3 Core Pipeline Sweeps (The Epicenter)
- [x] **Target:** `RealUnifiedPipelineTest.kt`
  - [x] **Overhaul W1: Complete Mock Eviction**: Purge `mockito` intercepts for state layers. Inject `:core:test-fakes` (`FakeEntityRepository`, `FakeMemoryRepository`, `FakeContextBuilder` if applicable).
  - [x] **Overhaul W2: Context Branch Coverage**: Write explicit tests for the 3 Context States (No Context/Noise, Partial Context/Trap, Sufficient Context/Execution). Prove the pipeline traps and clarifies intents dynamically.
- [ ] **Target:** `RealInputParserServiceTest.kt` & `EntityDisambiguator`
  - [ ] **Overhaul W3: Strict Interface Integrity**: Ensure tests do not mock the string parsing or bypass Linter gates.
  
> 🧠 **Agent Foresight & Reality Gaps (Phase 1)**: 
> *Here we log predictive limitations where L1/L2 code tests diverge from L3 physical reality.*
> - *Prediction (ASR Latency)*: While Mock Eviction proves the logic routes correctly, we cannot simulate the raw latency of the physical `Tingwu` ASR pipeline. The pipeline might time out in reality if the real ContextBuilder takes too long constructing the prompt.
> - *Gap (Service Lifecycle)*: Unit tests cannot verify if the Android OS will kill the background service executing this pipeline mid-flight during a heavy inference step.
> - *Prediction (Phonetic Brittleness)*: Strict string parsing in `EntityDisambiguator` might pass L1 tests beautifully, but fail in reality when Tingwu ASR outputs phonetically similar errors (e.g., "Chef Cook" vs "Jeff Cook"). 
> - *Gap (Concurrency)*: `FakeMemoryRepository` simulates immediate, linear SSD read/writes. In reality, Room/SQLite database locks under concurrent asynchronous load map to real-world race conditions that a synchronous unit test completely misses.
> - *Prediction (Token Limits)*: L1 tests inject small, clean session histories. In reality, a power user's `EnhancedContext` might breach the LLM's token limit, causing a hard crash that tests never surface.

#### Phase 2: Layer 4 Feature Sweeps (Integration Veracity)
- [ ] **Target:** Scheduler & Notifications
  - [ ] **Overhaul W4: Payload Verification**: Enforce `verify(mock).method(argThat { ... })` payloads. Never assert success without proving the structure sent to the `SchedulerLinter` or `ToolRegistry`.
- [ ] **Target:** `AgentViewModelTest.kt` & `IntentOrchestratorTest.kt`
  - [ ] **Overhaul W5: Red-First State Triggers**: Prove the UI state machine organically falls back to `STATE_IDLE` or throws `ClarificationNeeded` when fed empty data.

> 🧠 **Agent Foresight & Reality Gaps (Phase 2)**:
> - *Prediction (UI Masking)*: Even if UI tests prove `AgentViewModel` emits the `ClarificationNeeded` state perfectly, tricky Compose UI rendering bugs (like the Scrim Drawer anti-pattern) might invisibly mask the amber card behind another layer.
> - *Gap (User Psychology)*: We cannot unit-test if a user will actually understand the "amber breathing" visual cue or what to say next to resolve the trap. UI telemetry is mandatory post-launch.
> - *Prediction (OEM Killers)*: `SchedulerLinter` might perfectly validate the target alarm time, but aggressive OEM battery management (Xiaomi/Oppo/Vivo MIUI/ColorOS) might silently block the `AlarmManager` broadcast from ever waking the app.
> - *Gap (Hardware State)*: `AgentViewModel` might confidently transition to `STATE_LISTENING`, but tests cannot predict if the OS silently revoked the `RECORD_AUDIO` permission in the background, or if the mic hardware is locked by another app.
> - *Gap (Flow Conflation)*: Unit tests using `UnconfinedTestDispatcher` run synchronously. In the real app, rapid state emissions might be silently conflated by Compose observers, causing skipped interface frames.

#### Phase 3: E2E Pillar Resumption
> Only after Phase 1 and 2 pass the Anti-Illusion protocol will we resume the 6 E2E Test Waves below.

---

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
