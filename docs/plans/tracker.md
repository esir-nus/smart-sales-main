# Smart Sales Prism Tracker

> **Purpose**: Central project history and active epic tracking.
> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.
> **Campaign Lifecycle**: Every major initiative (rewrite, refactor, UI polish, large fix) is an "Epic" or "Campaign". Every Campaign MUST be initialized using the `/campaign-planner` workflow to enforce the following checklist sequence:
> 1. **Docs** (Ensure Specs exist) 🔄 2. **Interface Map** (Ensure Layer/Contract boundaries align) 🔄 3. **Plan** (Dev Planner) 🔄 4. **Execute** (Implementation) 🔄 5. **Test** (E2E/L2 Verification). 
> **Master Guide Alignment**: The Master Guide acts as the overarching strategy doc for a campaign. Agents MUST NEVER auto-update the Master Guide without strict explicit human review (like a Review Conference) to prevent architectural hallucination drift. Instead, run `/04-doc-sync` at the *end* of a campaign.
> **Last Updated**: 2026-03-14

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

### 🌊 Wave 8: Actionable/Factual Unification (Raw Domain Surfacing)
> Reconstruct the CRM intelligence dashboard (Layer 5: ClientProfileHub) to surface raw `TimelineItemModel.Task` (Actionable) and raw `MemoryEntry` (Factual) via reactive Kotlin flows, completely separating the Future from the Past without relying on UI-layer translation mappings or database dual-writes. Unification through a true Source of Truth.
- [ ] 🔲 **T1: Discard the `UnifiedActivity` entity mapping abstraction.**
- [x] **Plan**: Establish the [Actionable vs Factual Unification Blueprint](../reports/review_conferences/20260314_actionable_factual_plan.md) and prove the Badge hardware scheduling constraint cannot be bypassed.
- [x] **Conference**: Validate the Data Class Unification math with the Senior Engineer.
- [x] **Docs**: Synchronized the `client-profile-hub/spec.md`, `entity-writer/spec.md`, and `entity-registry/spec.md` Data Models to reflect `ProfileActivityState` and remove the legacy `UnifiedActivity` mapping struct (`/04-doc-sync` complete).
- [x] **Execute**:
   1. ✅ **Phase 1: Domain Contract Purge**
      - Clean slate isolation for `:domain:scheduler` and `:domain:core`
      - Verified JVM compliance via Linter.
   2. ✅ **Phase 2: The Reactive Engine**
      - Implementation of `RealClientProfileHub`
      - DAO up-conversions to `observeByEntityId` (`LIKE` operator for `MemoryDao`)
      - Reactive streaming `Flow.combine` for aggregation without RAM caching.
- [x] ✅ **Execute (Phase 3)**: The Cross-Off Lifecycle — Implement data migration in `SchedulerViewModel` (or equivalent observer) where completing a task permanently creates a `MemoryEntry` and deletes the `ScheduledTask`.
- [ ] 🔲 **Test**: Verify `L1ClientProfileHubTest.kt` passes Phase 2 logic, and an L2 simulation proves Phase 3 Source of Truth migration.

### 🌊 Wave 14: Scheduler Architecture Refinement (Monolithic)
> Objective: Enhance the monolithic Scheduler pipeline to handle Voice Hooks deterministically.

- [x] ✅ **T0: Campaign North Star**
  - [x] **Docs**: Enhance `docs/cerb/scheduler/spec.md` to establish architectural principles and Intricacies.

- [x] ✅ **T2: Domain `unifiedID` Infrastructure**
  - [x] **Execute**: Update ASR transcription layer (or core IntentOrchestrator generation) to mint a random `unifiedID`. 
  - [x] **Execute**: Update `ScheduledTask` entity if necessary to accept `unifiedID`.
  - [x] **Test**: Unit tests verify `unifiedID` propagates into the pipeline `Input` object.

- [ ] 🔲 **T3: Semantic Path A via Monolithic Pipeline**
  - [ ] **Execute**: Run the monolithic `SchedulerLinter` + LLM Extractor purely to immediately insert an optimistic task with an accurate semantic ISO time.

- [ ] 🔲 **T4: Presentation Teardown**
  - [x] **Specs**: Ensure `scheduler/contract.md` defines pure state execution.
  - [x] **Execute**: Hoist `PhoneAudioRecorder` out of the visual card layer up to ViewModel/Screen.
  - [x] **Execute**: Dismantle 700-line `SchedulerCards.kt/SchedulerTaskCard.kt`. Move atomic components into `components/` package.
  - [x] **Test**: Mechanical `grep` proves no raw Android dependencies inside the nested UI component layer.

### 🌊 Wave 15: Dual-Path Asynchronous Scheduling (Plugin Demotion)
> Objective: Formalize the "Town and Highway" protocol for the DEV Audio Hook by decoupling optimistic UI Task Creation (Path A) from heavyweight async CRM Entity Disambiguation (Path B).
> Note: This architecture has been demoted to a future plugin-like capability. The primary scheduler will remain monolithic.

### 🎨 Epic: UI Skin Modernization & Protocol Validation
> Applying the newly established Docs-First `IAgentViewModel` Contract to aggressively clean, refactor, and rewrite the UI layer into a pristine presentation boundary, proving the Parallel Dev Workflow before formalizing it into an Antigravity Rule.
- [x] ✅ **Phase 1: Component Rewrite (Code Cleaning)** - Nuke and pave existing screens (Chat, Agent States) to purely consume `IAgentViewModel`. Replace tangled `AgentViewModel` logic with strict `@Preview` tests powered by `FakeAgentViewModel`. Drop all dead legacy UI code.
- [x] ✅ **Phase 2: The Parallel Proving Ground (Scheduler)** - Rewrite `SchedulerViewModel` to purge pipeline/conflict logic, creating a pure `ISchedulerViewModel` boundary. Implement `FakeSchedulerViewModel` and prove we can build the new UI Skin purely in Compose previews.
  - [TER: L3 Scheduler DEV Audio Hook Validation (BugFix 4.4)](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260314-Scheduler-AudioHook.md)
- [ ] 🔲 **Phase 3: Formalize UI Antigravity Protocol** - After proving success in Phase 1 and 2, create the official SOP rule (`.agent/rules/ui-development-protocol.md`) demanding strict `@Preview` logic isolation, Fake ViewModels, and `interface.md` synchronization for all subsequent UI features.



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

- [os-model-architecture.md](../specs/os-model-architecture.md) — RAM/SSD mental model
- [Architecture.md](../specs/Architecture.md) — The Data-Oriented OS Migration Guide (Read before any Mono tasks)
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — (Deprecated) The Single Source of Truth for UI/UX is now exclusively the `docs/cerb/[feature]/spec.md` files.
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping

---

## Changelog

The changelog has been moved to a standalone file to prevent content explosion. 

👉 **[View Changelog](changelog.md)**
### 🌊 Wave 16: Scheduler Decoupling (The Archival Purge)
> Objective: Formalize the Scheduler as a standalone plugin/domain by severing all hardcoded dependencies from the Core OS Pipeline (`IntentOrchestrator`, `RealUnifiedPipeline`, `PromptCompiler`).
> 🧭 **North Star**: [docs/specs/wave16-scheduler-decoupling-guide.md]()
- [x] ✅ **T0: Campaign North Star**
  - [x] **Docs**: Create `docs/specs/wave16-scheduler-decoupling-guide.md` defining the Plugin interface boundary.
- [x] ✅ **T1: Interface Extraction**
  - [x] **Docs**: Update `docs/cerb/interface-map.md` to reflect the Pipeline -> Plugin boundary.
  - [x] **Plan**: Run `/feature-dev-planner` to map out the generic `ToolDispatcher` interface.
  - [x] **Execute**: Wire the existing `ToolRegistry` / `PluginGateway` into `:core:pipeline`.
  - [x] **Test**: L1 tests verify the new interface contract without Scheduler imports.
- [x] ✅ **T2: Pipeline Purge**
  - [x] **Plan**: Run `/feature-dev-planner` to replace Scheduler imports with the generic interface.
  - [x] **Execute**: Nuke all `com.smartsales.prism.domain.scheduler.*` imports from `IntentOrchestrator`, `RealUnifiedPipeline`, and `PromptCompiler`. 
  - [x] **Execute**: Rewrite the Reschedule/Lint logic to be domain-agnostic (mapped direct to `ToolDispatch`).
  - [x] **Test**: Mechanical Check: `grep` proves zero Scheduler imports exist in `:core:pipeline`.
- [ ] 🔲 **T3: Scheduler Plugin Wiring**
  - [ ] **Execute**: Implement the generic Plugin interface within the `:domain:scheduler` module.
  - [ ] **Execute**: Wire the Scheduler Plugin into the App-level DI graph (so the Pipeline can use it at runtime without compile-time knowledge).
  - [ ] **Test**: L3 Scheduler audio hook validation passes (proving the plugin still works when decoupled).
