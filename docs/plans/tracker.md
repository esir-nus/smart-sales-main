# Smart Sales Prism Tracker

> **Purpose**: Central project history and active epic tracking.
> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.
> **Campaign Lifecycle**: Every major initiative (rewrite, refactor, UI polish, large fix) is an "Epic" or "Campaign". Every Campaign MUST be initialized using the `/campaign-planner` workflow to enforce the following checklist sequence:
> 1. **Docs** (Ensure Specs exist) 🔄 2. **Interface Map** (Ensure Layer/Contract boundaries align) 🔄 3. **Plan** (Dev Planner) 🔄 4. **Execute** (Implementation) 🔄 5. **Test** (E2E/L2 Verification). 
> **Master Guide Alignment**: The Master Guide acts as the overarching strategy doc for a campaign. Agents MUST NEVER auto-update the Master Guide without strict explicit human review (like a Review Conference) to prevent architectural hallucination drift. Instead, run `/04-doc-sync` at the *end* of a campaign.
> **Last Updated**: 2026-03-17

---


## 🚧 ACTIVE EPIC: The Crucible (Pipeline Validation)
> **Context**: Paused to implement the Data-Oriented OS foundational architecture, which mathematically resolved foundational User-Flow Purity Remediation bugs. Resumed to prove the architecture and fix remaining UI racing issues.

### 🌊 Wave 6: User-Flow Purity Remediation (Mono Upgrade Polish)
> Prove the L3 UI handles human-centric workflows, decoupled intents, and context memory flawlessly (based on L2 User Flow Test Failures from 2026-03-12).

- [x] ✅ **T1: Disambiguation Fast-Fail Routing** (Entity resolution before LLM lock-in) - `/00-review-conference` completed.
- [x] ✅ **T2: The Async Loop** (Unbinding System II execution from Voice completion)
- [x] ✅ **T3: Secondary Currency RL Harmonization** (`HabitContext` -> `EnhancedContext` injection)
- [x] ✅ **T4: Mascot Presentation Collection** (Migrate from single-frame shimmer to sustained lifecycle collection)
- [x] ✅ **T5: Hardware Badge Delegation Constraint** (Strictly separating phone app (`badge_delegation`) vs physical badge (`crm_task`) scheduling capabilities)
- [x] 🛑 **T6: CANCELLED: The Hand-Off Animation** (Visual bridging between voice ingestion and LLM execution. Cancelled due to Voice Source ambiguity — accepted as minor UX debt to preserve OS Model boundaries)
- [x] ✅ **T7: Parallel UI Skin Contract** (Extracted IAgentViewModel from AgentViewModel to decouple Layer 4 UI from Layer 3 Pipeline allowing Fake ViewModel vibe coding)
- [x] ✅ **T8: Mechanical UI Contract Verification** (Implement UiSpecAlignmentTest.kt to mechanically enforce Docs-First Protocol between interface.md and UiState)

### 🌊 Wave 7: The Final Audit (Phase 3 E2E Pillar Resumption)
> System-wide E2E Device Tests for: Lightning Fast-Track, Dual-Engine Bridge, Strict Interface Integrity, Adaptive Habit Loop, Efficiency Overload, Transparent Mind. This is the capstone requirement before declaring the foundational architecture stable.
- [ ] 🔲 **T1: Lightning Fast-Track E2E**
- [ ] 🔲 **T2: Dual-Engine E2E**
- [ ] 🔲 **T3: E2E Error & Constraint Testing**
  - **Spec**: [`docs/cerb-e2e-test/specs/wave7-final-audit/spec.md`](../cerb-e2e-test/specs/wave7-final-audit/spec.md)
  - **Boundaries**: [`docs/cerb-e2e-test/specs/wave7-final-audit/boundaries.md`](../cerb-e2e-test/specs/wave7-final-audit/boundaries.md)
  - [TER: L3 Wave 7 Final Audit](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260313-wave7-final-audit.md)
  - [TER: L3 GPS Valves (World State)](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260316-gps-valves.md)




---

## Tech Debt (Deferred for Beta)

| Item | Location | Priority |
|------|----------|----------|
| `delay()` in UI | `ResponseBubble.kt`, `ConnectivityModal.kt`, `OnboardingScreen.kt` | Low |
| FTS4 Search | `MemoryDao.kt` — LIKE for Chinese | Medium |
| Remaining Fakes | `FakeHistoryRepository`, `FakeAudioRepository` — not Room-backed | Low |
| TOCTOU in observe() | `RoomUserHabitRepository.kt` | Low |
| Room error handling | `Room*Repository` — no try-catch on writes | Low |
| **Confidence-Based Reminder Interceptor** | Replace deterministic round-1 wrap-up with LLM confidence-based interception. Agent decides when to surface schedule context. Requires classifier or LLM self-assessment of conversation intent. Current workaround: smarter prompting that lets LLM decide naturally. | Medium |
| **Voice Hand-Off Animation** | `AgentIntelligenceScreen.kt`, `UiState.kt` — Visual bridging for voice ingestion. Deferred due to architectural ambiguity: must decide if `AgentIntelligenceScreen` mic records directly, or if it strictly observes `BadgeAudioPipeline` global states. Spec updated (`UiState.AudioProcessing`), but implementation pending source definition. | Medium |

---

## Quick Links

- [Architecture.md](../specs/Architecture.md) — RAM/SSD mental model
- [Architecture.md](../specs/Architecture.md) — The Data-Oriented OS Migration Guide (Read before any Mono tasks)
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — (Deprecated) The Single Source of Truth for UI/UX is now exclusively the `docs/cerb/[feature]/spec.md` files.
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping

---

## Changelog

The changelog has been moved to a standalone file to prevent content explosion. 

👉 **[View Changelog](changelog.md)**

### 🌊 Wave 19: Scheduler Fast-Track Core-Flow Completion (Universe-Driven Delivery)
> Objective: Use the Path A Core Flow as the behavioral north star and complete implementation universe-by-universe instead of as one vague backend rewrite.
> 🧭 **North Star**: [docs/core-flow/scheduler-fast-track-flow.md](../core-flow/scheduler-fast-track-flow.md)
> 📐 **Implementation Contract**: [docs/cerb/scheduler-path-a-spine/spec.md](../cerb/scheduler-path-a-spine/spec.md)
> **Delivery Law**: For each universe or safety branch, follow `Flow -> Spec -> Code -> PU Test -> Fix Loop` before advancing.
> **Execution Order**: Build the shared Path A spine first, then deliver `Uni-A`, then the safety/guardrail universes, then the reschedule branches.
- [x] ✅ **T0: Shared Path A Spine**
  - [x] **Flow**: Locked the common Path A skeleton from the Core Flow (`ASR_CAPTURED -> GUID_ALLOCATED -> INTENT_CLASSIFIED -> DB_WRITE_EXECUTED/UI_RENDERED or FAST_FAIL_RETURNED`) with `IntentOrchestrator` as the single shared spine owner.
  - [x] **Spec**: Created the owning T0 Cerb shard in `docs/cerb/scheduler-path-a-spine/`, defining the shared spine contract and replacing stale implementation-contract assumptions.
  - [x] **Code**: Wired the minimum shared execution path used by all Path A universes by routing badge audio transcript scheduling through `IntentOrchestrator` and surfacing `PipelineResult.PathACommitted` as the early completion checkpoint.
  - [x] **PU Test**: Added baseline Path A assertions in targeted orchestrator and badge-audio tests so every later universe inherits one shared entry seam.
  - [x] **Fix Loop**: Repaired the ownership drift between implementation docs and the delivered single-spine behavior before universe-specific work continues.
- [ ] 🔲 **T1: Uni-A Specific Creation**
  - [ ] **Flow**: Implement [Uni-A](../core-flow/scheduler-fast-track-flow.md) as the first happy-path slice.
  - [ ] **Spec**: Align exact-create wording, exact-time resolution, and success-state rendering.
  - [ ] **Code**: Deliver exact schedulable creation with no-conflict persistence and visible timeline render.
  - [ ] **PU Test**: Add one-universe validation for exact create with no conflict.
  - [ ] **Fix Loop**: Repair any drift against the Core Flow before advancing.
- [ ] 🔲 **T2: Uni-B Vague Creation**
  - [ ] **Flow**: Implement vague / needs-time handling without fabricated time.
  - [ ] **Spec**: Align vague-task representation and out-of-slot render behavior.
  - [ ] **Code**: Deliver explicit vague persistence plus red-flagged / awaiting-time UI treatment.
  - [ ] **PU Test**: Add one-universe validation for vague create with conflict bypass.
  - [ ] **Fix Loop**: Repair any lower-layer mismatch before advancing.
- [ ] 🔲 **T3: Uni-C Inspiration**
  - [ ] **Flow**: Implement inspiration routing as a non-schedulable, non-task branch.
  - [ ] **Spec**: Align inspiration-only persistence and display contract.
  - [ ] **Code**: Deliver inspiration write path isolated from the task table.
  - [ ] **PU Test**: Add one-universe validation for timeless intent -> inspiration output.
  - [ ] **Fix Loop**: Repair any task-table bleed or UI drift before advancing.
- [ ] 🔲 **T4: Uni-D Conflict-Visible Create**
  - [ ] **Flow**: Implement conflict-visible creation without rejecting user intent.
  - [ ] **Spec**: Align conflict semantics and caution-state rendering contract.
  - [ ] **Code**: Deliver exact create with persisted conflict-visible state and caution treatment.
  - [ ] **PU Test**: Add one-universe validation for overlap detection with successful creation.
  - [ ] **Fix Loop**: Repair any reject-on-conflict drift before advancing.
- [ ] 🔲 **T5: Branch-S0 Null / Garbled Fast-Fail**
  - [ ] **Flow**: Implement explicit non-mutation fast-fail for empty or unusable input.
  - [ ] **Spec**: Align fast-fail wording, traceability, and non-write behavior.
  - [ ] **Code**: Deliver no-op feedback path with no task or inspiration write.
  - [ ] **PU Test**: Add one-branch validation for null / garbled input.
  - [ ] **Fix Loop**: Repair any silent-drop or mutation drift before advancing.
- [ ] 🔲 **T6: Branch-S1 Reschedule Happy Path**
  - [ ] **Flow**: Implement replacement-style reschedule with lineage preservation and latest-revision result.
  - [ ] **Spec**: Align session-memory usage, replacement semantics, and follow-up parsing contract.
  - [ ] **Code**: Deliver create-new -> retire-old handling for successful reschedule follow-up.
  - [ ] **PU Test**: Add one-branch validation for successful contextual replacement.
  - [ ] **Fix Loop**: Repair any surgical-edit drift before advancing.
- [ ] 🔲 **T7: Branch-S2 Reschedule No-Match**
  - [ ] **Flow**: Implement explicit safe failure when no target can be resolved.
  - [ ] **Spec**: Align target-missing branch and non-mutation feedback contract.
  - [ ] **Code**: Deliver no-match fast-fail with zero mutation.
  - [ ] **PU Test**: Add one-branch validation for target missing.
  - [ ] **Fix Loop**: Repair any accidental mutation before advancing.
- [ ] 🔲 **T8: Branch-S3 Reschedule Ambiguous Match**
  - [ ] **Flow**: Implement explicit safe failure when multiple targets match.
  - [ ] **Spec**: Align target-ambiguous branch and manual-resolution feedback contract.
  - [ ] **Code**: Deliver ambiguity fast-fail with zero mutation.
  - [ ] **PU Test**: Add one-branch validation for ambiguous reschedule targeting.
  - [ ] **Fix Loop**: Repair any wrong-target mutation drift before advancing.
