# Smart Sales Prism Tracker

> **Purpose**: Central project history and active epic tracking.
> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.
> **Campaign Lifecycle**: Every major initiative (rewrite, refactor, UI polish, large fix) is an "Epic" or "Campaign". Every Campaign MUST be initialized using the `/campaign-planner` workflow to enforce the following checklist sequence:
> 1. **Docs** (Ensure Specs exist) 🔄 2. **Interface Map** (Ensure Layer/Contract boundaries align) 🔄 3. **Plan** (Dev Planner) 🔄 4. **Execute** (Implementation) 🔄 5. **Test** (E2E/L2 Verification). 
> **Master Guide Alignment**: The Master Guide acts as the overarching strategy doc for a campaign. Agents MUST NEVER auto-update the Master Guide without strict explicit human review (like a Review Conference) to prevent architectural hallucination drift. Instead, run `/04-doc-sync` at the *end* of a campaign.
> **Last Updated**: 2026-03-14

---

## ✅ COMPLETED EPIC: The Data-Oriented OS ("Project Mono")
> **Context**: Transitioning the architecture from "Behavioral Contracts" (Essay Questions) to "Data Contracts" (Multiple Choice). The SSD (Memory) becomes the absolute center of the universe. All LLM Prompts, Linters, and Interfaces must be driven directly by Kotlin Data Classes. 
> **Migration Law**: Strict Cerb Compliance. Each module is migrated incrementally via the Campaign Lifecycle.

### 🌊 Wave 1: The Contract Foundation (The One Currency)
> Establish the strict Data Classes that will act as the single currency for the entire OS.
- [x] **Docs**: Create `docs/cerb/core-contracts/spec.md` defining `UnifiedMutation` and domain mutations.
- [x] **Docs**: Update `docs/cerb/interface-map.md` to reflect the new Data Contract boundary between LLM and CRM.
- [x] **Plan**: Run `/feature-dev-planner` to establish execution steps for the mechanical test.
- [x] **Execute**: Build the `BrainBodyAlignmentTest.kt` to mechanically enforce the contract. Use Kotlin Reflection / `kotlinx.serialization` to dynamically extract the schema from the Kotlin `data class`.
- [x] **Test**: L1 Unit tests assert that the `PromptCompiler` output explicitly contains all JSON keys defined by the Kotlin domain `data class`, proving the Prompt cannot drift from the Database reality.

### 🌊 Wave 2: The EntityWriter Linter Upgrade (The Bouncer)
> Refactor the Linters to act as pure Type Checkers (The Teller/Bouncer), eliminating regex and LLM-translation guessing.
- [x] **Docs**: Update `docs/cerb/entity-writer/spec.md` and `interface.md`.
- [x] **Plan**: Run `/feature-dev-planner` for the Linter rework.
- [x] **Execute**: Refactor `EntityWriter` inputs and upgrade the Linter to use a strict 1-line JSON deserialization against the "One Currency" Kotlin class.
- [x] **Test**: Execute L2 World State Seeder Flow ensuring hallucinated fields (counterfeit currency) trigger a safe `SerializationException` without ghosting.

### 🌊 Wave 3: The Scheduler Migration (The First Refactor)
> Migrate a single vertical slice to the new Mono architecture.
- [x] **Docs**: Rewrite `docs/cerb/scheduler/spec.md` removing `CRM_TASK` hard fork logic.
- [x] **Interface Map**: Update `docs/cerb/interface-map.md` with Open-Loop Architecture.
- [x] **Plan**: Run `/feature-dev-planner`.
- [x] **Execute**: Replace `SchedulerLinter` with strict Data Contract and implement PendingProposalStore.
- [x] **Test**: Full L1/L2 scheduling flow succeeds natively under the Mono pipeline.

### 🌊 Wave 4: Analyst Harmonization (The Integration)
> Roll out the Mono contract to the Analyst/Consultant pipeline.
- [x] **Docs**: Update `docs/cerb/unified-pipeline/spec.md`.
- [x] **Interface Map**: Update `docs/cerb/interface-map.md`.
- [x] **Plan**: Run `/feature-dev-planner`.
- [x] **Execute**: Migrate the Analyst/Consultant pipeline to conform to multiple-choice selection.
- [x] **Test**: Verify Analyst Mode pipeline processes JSON multiple-choice cleanly without prompt drift.

### 🌊 Wave 4.5: RL Harmonization (The Integration)
> Roll out the Mono contract to the RL Subsystem.
- [x] **Docs**: Update `docs/cerb/rl-module/spec.md`.
- [x] **Interface Map**: Update `docs/cerb/interface-map.md`.
- [x] **Plan**: Run `/feature-dev-planner`.
- [x] **Execute**: Migrate the RL Subsystem to conform to multiple-choice selection.
- [x] **Test**: Verify RL module processes JSON multiple-choice cleanly without prompt drift.

---

### 🌊 Wave 5: The Dual-Loop CQRS Engine (Sync vs Async)
> Implement the Dual-Loop UJM architecture defined in `project-mono-master-guide.md` (Section 6), decoupling fast Entity ID lookup (Sync) from SSD Mutation (Async).

#### T1: The Sync Loop (Fast Query)
- [x] **Docs**: Update `docs/cerb/lightning-router/spec.md` with the "Entity Candidate Gatekeeper" protocol.
- [x] **Interface Map**: Update `docs/cerb/interface-map.md` for fast alias cache boundary.
- [x] **Plan**: Run `/feature-dev-planner`.
- [x] **Execute**: Wire the Lightning Router to strictly intercept intents lacking names. Build the clarification loop yield state (`UiState.AwaitingClarification`).
- [x] **Execute**: Connect the Lightning Router to the `Alias Lib` (L1 Cache) to instantly resolve `EntityID` before pushing the state into the SSD Graph fetch.
- [x] **Execute**: Build the Disambiguation yield state for multiple/missing `Alias Lib` returns.
- [x] **Test**: Verify sub-second Lightning Router responses via test simulations and adb logcat output.

#### T2: The Async Loop (Background Mutations)
- [x] **Docs**: Update `docs/cerb/entity-writer/spec.md` for asynchronous mutation decoupling.
- [x] **Interface Map**: Update `docs/cerb/interface-map.md`.
- [x] **Plan**: Run `/feature-dev-planner`.
- [x] **Execute**: Rip the heavy `decodeFromString` EntityWriter SSD mutation out of the main conversational execution path.
- [x] **Execute**: Schedule SSD writes in an isolated Coroutines background scope (or WorkManager) and stream the completion event back into the RAM Context Table.
- [x] **Test**: Verify background SSD writes via Logcat without stalling main conversational response text generation.

#### T3: The RL Harmonization (Background Learnings)
- [x] **Docs**: Update `docs/cerb/rl-module/spec.md` denoting the "Secondary Currency" contract for background Habit Extraction.
- [x] **Interface Map**: Update `docs/cerb/interface-map.md` to define the secondary contract boundary.
- [x] **Plan**: Run `/feature-dev-planner`.
- [x] **Execute**: Decouple the `RL Module` habit extraction into a background listener that passively ingests the chat transcript without blocking the next user turn.
- [x] **Test**: Write a mechanical schema verification script for the RL module's JSON output (Secondary Currency) to guarantee adherence to the Kotlin `data class`.

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

### 🌊 Wave 14: Dual-Path Scheduler Architecture
> Objective: Formalize the "Town and Highway" protocol for the DEV Audio Hook by decoupling optimistic UI Task Creation (Path A) from heavyweight async CRM Entity Disambiguation (Path B).
> 🧭 **North Star**: [Wave 14 Master Guide](../specs/wave14-dual-path-master-guide.md)

- [x] ✅ **T0: Campaign North Star**
  - [x] **Docs**: Create `docs/specs/wave14-dual-path-master-guide.md` to establish architectural principles and Intricacies.

- [x] ✅ **T1: The God Spec Splice (Context Isolation)**
  - [x] **Plan**: Shard `docs/cerb/scheduler/` into `scheduler-domain`, `scheduler-linter`, and `scheduler-drawer`.
  - [x] **Execute**: Run `/cerb-spec-template` and `/cerb-ui-template` to scaffold the 3 new directories. Migrate the 32KB content appropriately. Append to `interface-map.md`.
  - [x] **Test**: Run `/cerb-check` on all three new specs. Delete the old `scheduler/` doc directory.

- [x] ✅ **T2: Shard 1 Execution (Domain `unifiedID` Infrastructure)**
  - [x] **Specs**: Ensure `scheduler-domain/interface.md` is strictly pure.
  - [x] **Execute**: Update ASR transcription layer (or core IntentOrchestrator generation) to mint a random `unifiedID`. 
  - [x] **Execute**: Update `ScheduledTask` entity if necessary to accept `unifiedID`.
  - [x] **Test**: Unit tests verify `unifiedID` propagates into the pipeline `Input` object.

- [x] ✅ **T3: Shard 2 Execution (Linter Path A & B Dual-Routing)**
  - [x] **Specs**: Adhere to `scheduler-linter/interface.md`.
  - [x] **Execute (Path A)**: Implement lightweight fast-track parser to instantly generate a `ScheduledTask` via DB insertion with the `unifiedID`.
  - [x] **Execute (Path B)**: Route the slow background CRM Disambiguation through the `UnifiedPipeline` targeting the exact same `unifiedID`.
  - [x] **Test**: L2 simulated pipeline test tracking both forks resolving correctly.

- [ ] 🔲 **T4: Shard 3 Execution (UI Presentation Teardown)**
  - [x] **Specs**: Ensure `scheduler-drawer/contract.md` defines pure state execution and handles Dual-Path rendering mapped from individual `TimelineItem` states.
  - [ ] **Execute**: Hoist `PhoneAudioRecorder` out of the visual card layer up to ViewModel/Screen.
  - [ ] **Execute**: Dismantle 700-line `SchedulerCards.kt`. Move atomic components (`TaskCardHeader`, `TaskCardDetails`, etc.) into `components/` package.
  - [ ] **Test**: Mechanical `grep` proves no raw Android dependencies inside the nested UI component layer.

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
- [project-mono-master-guide.md](../specs/project-mono-master-guide.md) — The Data-Oriented OS Migration Guide (Read before any Mono tasks)
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — (Deprecated) The Single Source of Truth for UI/UX is now exclusively the `docs/cerb/[feature]/spec.md` files.
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping

---

## Changelog

The changelog has been moved to a standalone file to prevent content explosion. 

👉 **[View Changelog](changelog.md)**
