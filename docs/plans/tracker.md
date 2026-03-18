# Smart Sales Prism Tracker

> **Purpose**: Central project history and active epic tracking.
> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.
> **Campaign Lifecycle**: Every major initiative (rewrite, refactor, UI polish, large fix) is an "Epic" or "Campaign". Every Campaign MUST be initialized using the `/campaign-planner` workflow to enforce the following checklist sequence:
> 1. **Docs** (Ensure Specs exist) 🔄 2. **Interface Map** (Ensure Layer/Contract boundaries align) 🔄 3. **Plan** (Dev Planner) 🔄 4. **Execute** (Implementation) 🔄 5. **Test** (E2E/L2 Verification). 
> **Master Guide Alignment**: The Master Guide acts as the overarching strategy doc for a campaign. Agents MUST NEVER auto-update the Master Guide without strict explicit human review (like a Review Conference) to prevent architectural hallucination drift. Instead, run `/04-doc-sync` at the *end* of a campaign.
> **Last Updated**: 2026-03-18

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
> System-wide E2E Device Tests for: Lightning Fast-Track, Dual-Engine Bridge, Strict Interface Integrity, Adaptive Habit Loop, Efficiency Overload, Transparent Mind. This is the capstone requirement before declaring the foundational architecture stable. Closed on 2026-03-13; tracker synced on 2026-03-18.
- [x] ✅ **T1: Lightning Fast-Track E2E**
- [x] ✅ **T2: Dual-Engine E2E**
- [x] ✅ **T3: E2E Error & Constraint Testing**
  - **Spec**: [`docs/cerb-e2e-test/specs/wave7-final-audit/spec.md`](../cerb-e2e-test/specs/wave7-final-audit/spec.md)
  - **Boundaries**: [`docs/cerb-e2e-test/specs/wave7-final-audit/boundaries.md`](../cerb-e2e-test/specs/wave7-final-audit/boundaries.md)
  - [TER: L3 Wave 7 Final Audit](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260313-wave7-final-audit.md)
  - [TER: L3 GPS Valves (World State)](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260316-gps-valves.md)

### 🌊 Wave 20: Architecture Constitution Audit (5 System Core Flows)
> Objective: Audit the current codebase against the five architecture-derived system core flows and convert architectural drift into explicit lower-layer fix tasks.
> 🧭 **Constitution**: [`docs/specs/Architecture.md`](../specs/Architecture.md)
> 🧭 **System Core Flows**:
> - [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md)
> - [`docs/core-flow/system-typed-mutation-flow.md`](../core-flow/system-typed-mutation-flow.md)
> - [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)
> - [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)
> - [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)
> **Audit Law**: For each lane, follow `Architecture -> System Core Flow -> Code Reality -> Valve / Telemetry Check -> Drift Report -> Spec/Fix Task`.
> **Execution Method**: This wave is evidence-first. Read code, trace ownership, inspect telemetry, and make only the smallest doc or code edits required to expose reality. Do not “fix by assumption”.

- [x] ✅ **T0: Audit Spine / Evidence Map**
  - [x] **Architecture**: Locked the constitutional clauses each audit must reuse (`One Currency`, runtime layers, RAM ownership, minor loops, typed mutation boundary, central writer rule, UI/domain decoupling, valve protocol).
  - [x] **Code Reality**: Identified the active entrypoints and owners for the current architecture path (`IntentOrchestrator`, `RealLightningRouter`, `RealContextBuilder`, `RealUnifiedPipeline`, `RealEntityWriter`, `RealHabitListener`, `ToolRegistry` / `RealToolRegistry`).
  - [x] **Telemetry**: Confirmed the live valve / GPS tracker and the implementation anchor in `PipelineValve.kt`, including current plugin telemetry gaps already marked pending.
  - [x] **Output**: Created the Wave 20 T0 evidence map: [`docs/reports/20260317-wave20-t0-architecture-evidence-map.md`](../reports/20260317-wave20-t0-architecture-evidence-map.md)

- [x] ✅ **T1: Query Lane Audit**
  - [x] **Flow**: Audited current sync/query behavior against `system-query-assembly-flow.md`.
  - [x] **Code Reality**: Inspected the phase-0 gateway, router, alias/disambiguation path, SSD fetch, RAM assembly, and LLM handoff.
  - [x] **Telemetry**: Verified the current query-lane valve anchors for `INPUT_RECEIVED`, `ROUTER_DECISION`, `ALIAS_RESOLUTION`, `SSD_GRAPH_FETCHED`, `LIVING_RAM_ASSEMBLED`, and `LLM_BRAIN_EMISSION`.
  - [x] **Drift Focus**: Confirmed drift around anti-guessing loop resume, duplicate grounding, and the missing delivered minimal/partial RAM branch.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t1-query-lane-audit.md`](../reports/20260317-wave20-t1-query-lane-audit.md)

- [x] ✅ **T2: Typed Mutation Lane Audit**
  - [x] **Flow**: Audited current mutation behavior against `system-typed-mutation-flow.md`.
  - [x] **Code Reality**: Inspected structured emission, typed decode, confirmation/auto-commit seams, central writer routing, and RAM write-through.
  - [x] **Telemetry**: Verified `LLM_BRAIN_EMISSION`, `LINTER_DECODED`, `DB_WRITE_EXECUTED`, and the current UI-visible state emission anchors.
  - [x] **Drift Focus**: Confirmed strict typed decode for `UnifiedMutation`, but also confirmed drift around scheduler/tool dispatch, open-loop proposal execution, and plugin-caused write handoff.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t2-mutation-lane-audit.md`](../reports/20260317-wave20-t2-mutation-lane-audit.md)

- [x] ✅ **T3: Reinforcement Write-Through Audit**
  - [x] **Flow**: Audited current RL behavior against `system-reinforcement-write-through-flow.md`.
  - [x] **Code Reality**: Inspected the background listener, observation extraction, habit repository writes, contextual-vs-global habit routing, and RAM refresh path.
  - [x] **Telemetry**: Confirmed the lane is background and write-through, but also confirmed RL-specific valve coverage remains weak.
  - [x] **Drift Focus**: Verified latest-input trigger behavior, typed extraction, and quiet no-op behavior; confirmed remaining drift around learning-packet breadth and observability.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t3-reinforcement-write-through-audit.md`](../reports/20260317-wave20-t3-reinforcement-write-through-audit.md)

- [x] ✅ **T4: Session Memory Extension Audit**
  - [x] **Flow**: Audited current session-memory behavior against `system-session-memory-flow.md`.
  - [x] **Code Reality**: Inspected how recent turns are retained, admitted into RAM, replaced/evicted, and reused across clarification or follow-up threads.
  - [x] **Telemetry**: Confirmed `SESSION_MEMORY_UPDATED` is not yet delivered and that session-memory observability is materially behind the core flow.
  - [x] **Drift Focus**: Verified Kernel ownership and bounded history inside `RealContextBuilder`, but confirmed live runtime admission and clarification-resume wiring remain behind.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t4-session-memory-extension-audit.md`](../reports/20260317-wave20-t4-session-memory-extension-audit.md)

- [x] ✅ **T5: Plugin Gateway / Capability SDK Audit**
  - [x] **Flow**: Audited current plugin behavior against `system-plugin-gateway-flow.md`.
  - [x] **Code Reality**: Inspected plugin dispatch, internal routing, capability/API surfaces, external call ownership, and result handoff paths.
  - [x] **Telemetry**: Confirmed dispatch telemetry exists, but capability-call and external-call telemetry are not yet delivered consistently.
  - [x] **Drift Focus**: Confirmed the current lane is a thin registry with async UI plugins, not yet a real capability SDK, and confirmed plugin-caused write handoff into the mutation lane is still absent.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md`](../reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md)

- [x] ✅ **T6: Cross-Lane Composition Audit**
  - [x] **Flow**: Audited the interchange points between the five system lanes rather than each lane in isolation.
  - [x] **Code Reality**: Inspected `query -> mutation`, `query -> session memory`, `RL -> session memory`, and `plugin -> mutation` handoff seams.
  - [x] **Telemetry**: Confirmed transfer-point observability remains behind, especially for session-memory admission and plugin/mutation handoff.
  - [x] **Drift Focus**: Confirmed the main composition risk is bypassed ownership at lane transfers, not missing lanes.
  - [x] **Output**: Created the final architecture-composition report with derived next-wave tasks: [`docs/reports/20260317-wave20-t6-cross-lane-composition-audit.md`](../reports/20260317-wave20-t6-cross-lane-composition-audit.md)

### 🌊 Wave 21: Architecture Composition Fix Wave
> Objective: Turn the Wave 20 audit outputs into a focused repair wave ordered by architectural leverage, starting with the weakest transfer seams instead of isolated lane cleanups.
> 🧭 **Entry Reports**:
> - [`docs/reports/20260317-wave20-t1-query-lane-audit.md`](../reports/20260317-wave20-t1-query-lane-audit.md)
> - [`docs/reports/20260317-wave20-t2-mutation-lane-audit.md`](../reports/20260317-wave20-t2-mutation-lane-audit.md)
> - [`docs/reports/20260317-wave20-t3-reinforcement-write-through-audit.md`](../reports/20260317-wave20-t3-reinforcement-write-through-audit.md)
> - [`docs/reports/20260317-wave20-t4-session-memory-extension-audit.md`](../reports/20260317-wave20-t4-session-memory-extension-audit.md)
> - [`docs/reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md`](../reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md)
> - [`docs/reports/20260317-wave20-t6-cross-lane-composition-audit.md`](../reports/20260317-wave20-t6-cross-lane-composition-audit.md)
> **Fix Law**: `Architecture -> System Core Flow -> Cross-Lane Ownership -> Spec / Tracker Update -> Code -> Valve Coverage -> PU`
> **Execution Order**: Repair the highest-leverage transfer seams first. Do not start plugin write re-entry implementation before the mutation lane, session-admission seam, and runtime plugin gateway foundation are clarified.

- [x] ✅ **T1: Query -> Session Memory Runtime Wiring**
  - [x] **Flow**: Made the session-memory lane real in live runtime rather than leaving it as a Kernel-only capability. New sessions now bind the history session ID back into the Kernel before turn admission continues.
  - [x] **Spec**: Updated the session-context docs so the runtime admission contract is explicit for user turns, assistant turns, clarification/disambiguation repair, and anti-guessing resume.
  - [x] **Code (Phase A)**: Routed `AgentViewModel` send/reply paths through `ContextBuilder.recordUserMessage()` / `recordAssistantMessage()` and made clarification follow-up reuse the bounded Kernel thread instead of UI-only history.
  - [x] **Telemetry**: Added `SESSION_MEMORY_UPDATED` and tagged real admission / replacement points in `RealContextBuilder`.
  - [x] **Validation (Current Evidence)**: Added live-runtime-style and clarification-resume coverage in `AgentViewModelTest`, then ran:
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest`
    - `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.real.RealContextBuilderTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.domain.session.SessionAntiIllusionIntegrationTest`
  - [x] **Acceptance Follow-Up**: Resolved the runtime threading blocker by keeping Kernel ownership but moving live user/assistant turn admission onto `Dispatchers.IO` before returning to UI state updates.
  - [x] **Post-Fix Verification**: Re-ran the same four targeted checks after the threading fix:
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest`
    - `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.real.RealContextBuilderTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.domain.session.SessionAntiIllusionIntegrationTest`
  - [ ] **Deferred Design Note**: clarification/disambiguation prompts currently persist as flattened text, so session reload does not restore structured candidate options. Decide later whether this belongs in T1 or a follow-up wave.

- [x] ✅ **T2: Query -> Mutation Normalization**
  - [x] **Flow**: Normalized the `query -> mutation` seam without prematurely solving the plugin SDK wave. The concrete target is now delivered as:
    profile/entity writes stay in the typed mutation lane,
    scheduler task execution becomes a typed task-command seam,
    generic plugin/workflow dispatch stays deferred to `T3` / `T4`.
  - [x] **Scope Guard**: Did **not** attempt a repo-wide plugin rewrite. `T2` only repaired the current typed-handoff split inside `UnifiedPipeline` and `IntentOrchestrator`.
  - [x] **Phase A: Spec / Ownership Decision**
    - [x] Reconciled `docs/core-flow/system-typed-mutation-flow.md` with `docs/cerb/unified-pipeline/spec.md` and `docs/cerb/unified-pipeline/interface.md`.
    - [x] Declared the branch rules explicitly:
      `profileMutations` -> typed mutation / central writer,
      scheduler create/delete/reschedule -> typed task command owned by scheduler execution,
      workflow recommendation / future plugin capability calls -> plugin lane, not mutation lane.
    - [x] Synced `docs/cerb/interface-map.md` because the ownership edge changed materially.
  - [x] **Phase B: Typed Result Normalization**
    - [x] Replaced stringly scheduler `ToolDispatch(Map<String, String>)` emissions from `RealUnifiedPipeline` with typed scheduler/task proposal results.
    - [x] Kept `ToolRecommendation` for non-executing workflow suggestions.
    - [x] Kept generic plugin execution out of this phase.
  - [x] **Phase C: Proposal / Commit Normalization**
    - [x] Replaced the old raw `PipelineResult` cache split with an explicit pending execution contract in `IntentOrchestrator`.
    - [x] Preserved the user-visible `"确认执行"` behavior while removing opaque scheduler execution behind generic `ToolDispatch`.
    - [x] Preserved the voice auto-commit path and made its typed handoff explicit.
  - [x] **Telemetry**
    - [x] Added clearer transfer visibility for:
      proposal cached,
      commit requested / auto-commit accepted,
      typed task command emitted,
      typed task command handed into its owning executor.
    - [x] Reused existing mutation-lane valves and added new ones only at real ownership-transfer points.
  - [x] **Validation**
    - [x] Added end-to-end coverage for cached proposal -> confirm -> `EntityWriter` commit in `IntentOrchestratorTest`.
    - [x] Preserved and revalidated voice auto-commit behavior for the profile-mutation branch through the focused orchestrator and UI-side slices.
    - [x] Added scheduler-task branch coverage proving the path is typed and no longer depends on generic `ToolDispatch(Map<...>)`.
  - [x] **Verification**
    - [x] `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - [x] `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest --tests com.smartsales.prism.data.real.RealUnifiedPipelineTest --tests com.smartsales.prism.data.real.L2GatewayGauntletTest --tests com.smartsales.prism.data.real.L2EfficiencyOverloadTest --tests com.smartsales.prism.data.real.L2DualEngineBridgeTest`
  - [x] **Done When**
    - [x] `UnifiedPipeline` no longer emits generic `ToolDispatch` for scheduler create/delete/reschedule intents.
    - [x] `IntentOrchestrator` no longer relies on an opaque cached `PipelineResult` object as the only proposal execution contract.
    - [x] The ownership split between mutation lane, scheduler task command lane, and plugin lane is documented and tested.

- [ ] 🔲 **T3: Runtime Plugin Gateway / Capability SDK Foundation**
  - [ ] **Flow**: Upgrade the plugin runtime from thin registry + stub gateway toward the first real capability gateway.
  - [ ] **Spec**: Define the first narrow capability APIs from real use cases, not a generic “query anything” surface.
  - [ ] **Code**: Replace empty runtime gateway lambdas with real gateway-backed history/progress access where appropriate.
  - [ ] **Telemetry**: Deliver `PLUGIN_CAPABILITY_CALL` and consistent plugin valve coverage.
  - [ ] **Validation**: Add a production-style permission/gateway execution test.

- [ ] 🔲 **T4: Plugin -> Mutation Re-Entry Contract**
  - [ ] **Flow**: Define the first real plugin-caused write handoff back into the typed mutation / central writer lane.
  - [ ] **Spec**: Introduce a bounded plugin result contract for write-causing plugins.
  - [ ] **Code**: Implement one concrete re-entry path only after the runtime plugin gateway foundation is in place.
  - [ ] **Telemetry**: Add explicit transfer visibility for plugin result -> mutation handoff.
  - [ ] **Validation**: Add one plugin result -> typed mutation integration test.

- [ ] 🔲 **T5: RL / Session Composition Hardening**
  - [ ] **Flow**: Keep the existing `RL -> session memory` seam, but improve the quality and traceability of the learning packet after session-admission wiring is fixed.
  - [ ] **Spec**: Decide whether RL’s “active RAM context” remains broad or is narrowed to explicit carried fields.
  - [ ] **Code**: Tighten RL observability and, if needed, expand the contextual packet intentionally rather than by accident.
  - [ ] **Telemetry**: Add RL-specific valves for listener trigger, payload decode, and RAM refresh.
  - [ ] **Validation**: Add a fragmented-turn RL test using real recent session context.

- [ ] 🔲 **T6: Residual Post-Composition Query Cleanup**
  - [ ] **Flow**: After transfer seams are fixed, return to the Wave 20 query-lane residuals that are not purely composition issues.
  - [ ] **Spec**: Re-evaluate duplicate grounding, alias-first purity, and minimal / partial RAM fallback against the repaired composition model.
  - [ ] **Code**: Remove or narrow duplicate grounding paths that remain after `T1` and `T2`.
  - [ ] **Validation**: Add one focused query audit/fix report confirming what remains after composition repairs.

### 🌊 Wave 22: Test Surface Hardening
> Objective: Convert the current testing surface from "documented and runnable" into "mechanically trustworthy", starting with the weakest anti-illusion seams exposed by the 2026-03-18 audit.
> 🧭 **Entry Docs**:
> - [`docs/cerb/test-infrastructure/spec.md`](../cerb/test-infrastructure/spec.md)
> - [`docs/cerb/test-infrastructure/interface.md`](../cerb/test-infrastructure/interface.md)
> - [`docs/cerb-e2e-test/testing-protocol.md`](../cerb-e2e-test/testing-protocol.md)
> - [`docs/cerb-e2e-test/tasklist_log.md`](../cerb-e2e-test/tasklist_log.md)
> **Fix Law**: `Test SOT -> Canonical Entry -> Real Assertions -> Mock Eviction -> Acceptance Evidence`
> **Execution Order**: First make the infra tasks assert something real, then evict avoidable Mockito from L2-style tests, then close the governance and acceptance loop.

- [x] ✅ **T1: Infra Module Assertion Reality**
  - [x] **Flow**: `scripts/run-tests.sh infra` now validates real test behavior instead of only empty task wiring.
  - [x] **Spec**: Locked one concrete invariant per infra module before implementation:
    `:core:test` proves dispatcher/test-scope control,
    `:core:test-fakes-domain` proves one state-backed fake mutation/read-through invariant,
    `:core:test-fakes-platform` proves one platform fake collaboration seam without delegating the behavior to Mockito.
  - [x] **Code**: Added the smallest tests that prove those invariants mechanically, not placeholder smoke coverage.
  - [x] **Validation**: `:core:test:test`, `:core:test-fakes-domain:test`, and `:core:test-fakes-platform:testDebugUnitTest` all execute real assertions tied to those declared invariants and no longer finish as `NO-SOURCE`.

- [x] ✅ **T2: L2 Shared-Fake Migration**
  - [x] **Flow**: Removed avoidable "Testing Illusion" patterns specifically from L2 / simulated-E2E tests where repo-owned collaborators already had shared fakes.
  - [x] **Spec**: Classified remaining Mockito usage into two buckets:
    acceptable leaf seam = Android `Context` in local JVM `L2CrossOffLifecycleTest`;
    out-of-scope lower-level mocks = non-L2 tests such as `RealUnifiedPipelineTest`, `RealHabitListenerTest`, `RealAudioRepositoryBreakItTest`, and `RealOssUploaderTest`;
    anti-illusion drift inside L2 verification paths = repo-owned collaborator mocks in `L2CrossOffLifecycleTest` (now removed).
  - [x] **Code**: Rewrote the highest-signal L2 offender (`L2CrossOffLifecycleTest`) toward shared fake wiring by constructing real/fake scheduler and pipeline collaborators instead of stubbing them with Mockito.
  - [x] **Boundary**: Kept the task scoped to L2 migration; did not broaden it into a repo-wide Mockito purge.
  - [x] **Validation**: The converted L2 test still passes and proves the same behavior through state-backed fakes rather than stubbed control flow.

- [x] ✅ **T3: Canonical Runner Coverage Expansion**
  - [x] **Flow**: `scripts/run-tests.sh` now acts as a single truthful automated entrypoint rather than a thin wrapper around partial institutional memory.
  - [x] **Spec**: Defined the first-class automated slices as `all`, `infra`, `pipeline`, `scheduler`, and `l2`; kept `app` as a convenience alias rather than a first-class verification contract.
  - [x] **Code**: Extended the runner with a stable `l2` mode and explicit `all` semantics without adding vanity aliases.
  - [x] **Docs**: Synced README/spec/interface references so contributors see one command path and one slice contract.
  - [x] **Validation**: `scripts/run-tests.sh infra`, `l2`, `pipeline`, `scheduler`, and `all` all execute successfully against real documented slices, and `all` now explicitly declares itself as the curated repo-default slice rather than an exhaustive aggregate.

- [x] ✅ **T4: E2E Governance Closure**
  - [x] **Flow**: Prevent tracker/changelog/ledger drift from reappearing after future test waves ship.
  - [x] **Spec**: Clarified that `tracker.md` owns active testing-wave status, `tasklist_log.md` owns the operational evidence mirror, and `changelog.md` is historical-only.
  - [x] **Docs**: Tightened the testing protocol and ledger notes so a shipped wave cannot stay open in the tracker without being treated as unresolved drift.
  - [x] **Ship Gate**: Made tracker + ledger sync an explicit closeout requirement for future testing waves, not a best-effort follow-up.
  - [x] **Validation**: Performed a Wave 7 dry-run sync on 2026-03-18, preserving the 2026-03-13 shipment date while aligning tracker language, ledger notes, and changelog ownership without contradiction.

- [x] ✅ **T5: Acceptance Closeout**
  - [x] **Flow**: Proved the repaired testing surface works end-to-end for both infra and core automated entrypoints.
  - [x] **Validation**: Re-ran every first-class runner slice declared in `T3` during closeout: `scripts/run-tests.sh infra`, `l2`, `pipeline`, `scheduler`, and `all`, all with successful results.
  - [x] **Evidence**: Recorded the acceptance verdict in [`docs/reports/tests/20260318-wave22-t5-test-surface-hardening-acceptance.md`](../reports/tests/20260318-wave22-t5-test-surface-hardening-acceptance.md).




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
> 📐 **Foundation Contract**: [docs/cerb/scheduler-path-a-spine/spec.md](../cerb/scheduler-path-a-spine/spec.md)
> 📐 **Active T1 Contract**: [docs/cerb/scheduler-path-a-uni-a/spec.md](../cerb/scheduler-path-a-uni-a/spec.md)
> **Delivery Law**: For each universe or safety branch, follow `Flow -> Spec -> Code -> PU Test -> Fix Loop` before advancing.
> **Execution Order**: Build the shared Path A spine first, then deliver `Uni-A`, then the safety/guardrail universes, then the reschedule branches.
- [x] ✅ **T0: Shared Path A Spine**
  - [x] **Flow**: Locked the common Path A skeleton from the Core Flow (`ASR_CAPTURED -> GUID_ALLOCATED -> INTENT_CLASSIFIED -> DB_WRITE_EXECUTED/UI_RENDERED or FAST_FAIL_RETURNED`) with `IntentOrchestrator` as the single shared spine owner.
  - [x] **Spec**: Created the owning T0 Cerb shard in `docs/cerb/scheduler-path-a-spine/`, defining the shared spine contract and replacing stale implementation-contract assumptions.
  - [x] **Code**: Wired the minimum shared execution path used by all Path A universes by routing badge audio transcript scheduling through `IntentOrchestrator` and surfacing `PipelineResult.PathACommitted` as the early completion checkpoint.
  - [x] **PU Test**: Added baseline Path A assertions in targeted orchestrator and badge-audio tests so every later universe inherits one shared entry seam.
  - [x] **Fix Loop**: Repaired the ownership drift between implementation docs and the delivered single-spine behavior before universe-specific work continues.
- [x] ✅ **T1: Uni-A Specific Creation**
  - [x] **Flow**: Implemented [Uni-A](../core-flow/scheduler-fast-track-flow.md) as the first happy-path slice.
  - [x] **Spec**: Built from [docs/cerb/scheduler-path-a-uni-a/spec.md](../cerb/scheduler-path-a-uni-a/spec.md) and [docs/cerb/scheduler-path-a-uni-a/interface.md](../cerb/scheduler-path-a-uni-a/interface.md), defining lightweight semantic extraction, exact-task DTO/output shape, no-conflict persist rules, and normal timeline render expectations.
  - [x] **Plan**: Executed from [docs/plans/wave19-t1-uni-a-plan.md](wave19-t1-uni-a-plan.md), locking the narrow-contract rewrite points, ownership map, and PU gates before code.
  - [x] **Code**: Delivered exact schedulable creation through `narrow :domain extraction contract -> prompt/linter mechanical alignment -> scheduler validation -> deterministic persist`, not Kotlin heuristic parsing.
  - [x] **PU Test**: Verified exact semantic extraction yields one non-vague, non-conflict task with visible timeline render, and non-exact input does not masquerade as `Uni-A`.
  - [x] **Fix Loop**: Repaired prompt/linter seam drift and `unifiedId` persistence drift before closing T1.
  - [ ] **L3 Runtime Repair**: On-device validation exposed that real exact schedulable utterances are still bypassing `Uni-A` and surfacing false success copy without a persisted schedule card.
  - [ ] **Flow**: Repair the live Path A entry law so schedulable voice input can enter `Uni-A` even when the upstream router labels it `DEEP_ANALYSIS` or `SIMPLE_QA`; `Uni-A` runtime entry must be governed by schedulable semantics, not by an over-narrow router enum gate.
  - [ ] **Spec**: Update the `Uni-A` shard and, if needed, the shared Path A spine shard so runtime entry and exit are explicit for: `entered Uni-A`, `NotExact exit`, `conflict reject`, `fell through to Path B`, and `no user-visible success without persistence proof`.
  - [ ] **Code**: Replace the current `CRM_TASK / BADGE_DELEGATION`-only gate in `IntentOrchestrator` with a bounded live runtime gate that reaches `Uni-A` for real exact voice scheduling input, while still preventing heuristic overreach.
  - [ ] **Success Proof Law**: Do not emit scheduler success copy such as `搞定` unless there is write proof:
    `PathACommitted` / `PATH_A_DB_WRITTEN` for Path A, or real scheduler persistence proof for any later fallback lane.
  - [ ] **Telemetry**: Add or tighten the missing runtime evidence so on-device logs can distinguish `Uni-A entered`, `Uni-A exact persisted`, `Uni-A exited NotExact`, `Uni-A rejected by conflict`, `Path B fallback invoked`, and `success copy emitted`.
  - [ ] **Validation**: Re-run L3 on-device with TER evidence after the repair. Required outcomes:
    exact create reaches `PATH_A_PARSED -> PATH_A_DB_WRITTEN -> UI_STATE_EMITTED`,
    conflicting exact input does not create a normal card,
    and no success toast/copy appears without persistence proof.
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
