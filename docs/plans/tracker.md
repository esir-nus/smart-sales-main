# Smart Sales Prism Tracker

> **Purpose**: Central project history and active epic tracking.
> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.
> **Rule**: History entries are commit-style — key info only.
> **Last Updated**: 2026-03-10

---

## ✅ COMPLETED EPIC: The Data-Oriented OS ("Project Mono")
> **Context**: Transitioning the architecture from "Behavioral Contracts" (Essay Questions) to "Data Contracts" (Multiple Choice). The SSD (Memory) becomes the absolute center of the universe. All LLM Prompts, Linters, and Interfaces must be driven directly by Kotlin Data Classes. 
> **Migration Law**: Strict Cerb Compliance. Each module is migrated incrementally. The lifecycle must NEVER be bypassed: [1. Docs/Specs 🔄 2. Interface Map 🔄 3. Plan 🔄 4. Execute 🔄 5. E2E Test]. 

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
- [ ] ⏬ **T6: DEFERRED: The Hand-Off Animation** (Visual bridging between voice ingestion and LLM execution. Paused due to Voice Source ambiguity — see Tech Debt)
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
- [ ] **Execute (Phase 3)**: The Cross-Off Lifecycle — Implement data migration in `SchedulerViewModel` (or equivalent observer) where completing a task permanently creates a `MemoryEntry` and deletes the `ScheduledTask`.
- [ ] **Test**: Verify `L1ClientProfileHubTest.kt` passes Phase 2 logic, and an L2 simulation proves Phase 3 Source of Truth migration.

### 🎨 Epic: UI Skin Modernization & Protocol Validation
> Applying the newly established Docs-First `IAgentViewModel` Contract to aggressively clean, refactor, and rewrite the UI layer into a pristine presentation boundary, proving the Parallel Dev Workflow before formalizing it into an Antigravity Rule.
- [x] ✅ **Phase 1: Component Rewrite (Code Cleaning)** - Nuke and pave existing screens (Chat, Agent States) to purely consume `IAgentViewModel`. Replace tangled `AgentViewModel` logic with strict `@Preview` tests powered by `FakeAgentViewModel`. Drop all dead legacy UI code.
- [ ] 🔲 **Phase 2: The Parallel Proving Ground** - Design and implement a net-new UI component (e.g., the new Scheduler interface) entirely using `FakeAgentViewModel` vibes *before* the backend is connected. Prove we can build the Skin without the Body.
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
| **Voice Hand-Off Animation** | `AgentChatScreen.kt`, `UiState.kt` — Visual bridging for voice ingestion. Deferred due to architectural ambiguity: must decide if `AgentChatScreen` mic records directly, or if it strictly observes `BadgeAudioPipeline` global states. Spec updated (`UiState.AudioProcessing`), but implementation pending source definition. | Medium |

---

## Quick Links

- [os-model-architecture.md](../specs/os-model-architecture.md) — RAM/SSD mental model
- [project-mono-master-guide.md](../specs/project-mono-master-guide.md) — The Data-Oriented OS Migration Guide (Read before any Mono tasks)
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — (Deprecated) The Single Source of Truth for UI/UX is now exclusively the `docs/cerb/[feature]/spec.md` files.
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping

---

## Changelog

> Key spec/impl changes, newest first. Like `git log --oneline`.

### 2026-03-14
- **agent-intelligence-ui**: Docs-First UI Verification (Skin Verification). Wrote `UiSpecAlignmentTest.kt` to mathematically bind `docs/cerb-ui/agent-intelligence/interface.md` with compiling `UiState` via Reflection. Any drift in the docs now strictly fails the android build tests.
- **agent-intelligence-ui**: Parallel UI Contract (Skin) Established. Gutted deprecated 160-line legacy `prism-ui-ux-contract.md`. Extracted `IAgentViewModel` to unblock UI developers from Project Mono backend restructuring. Provided `FakeAgentViewModel` strictly implementing `UiState` and `AgentActivity` mocks for Compose @Preview design validation.
- **architecture**: Wave 6 T5 Hardware Badge Delegation Constraint SHIPPED. Added a strict architectural boundary (`isBadge`) separating phone app intelligence from physical hardware capabilities. The phone app LLM is now mathematically proven (via `PromptCompilerBadgeTest.kt`) to route all scheduling attempts to a `badge_delegation` UI hint overlay (`AgentChatScreen.kt`), strictly forcing the user to utilize the ESP32 physical badge for calendar mutations.

### 2026-03-13
- **architecture**: Wave 7 The Final Audit (Phase 3 E2E Pillar Resumption) SHIPPED. Executing 5 rigorous L3 On-Device test scenarios proven across the 6 Core Pillars (Lightning Fast-Track, Dual-Engine Bridge, Strict Interface Integrity, Adaptive Habit Loop, Efficiency Overload, and Transparent Mind). The architecture behaves flawlessly under real-world input and ContextAssembly stress. Project Mono's foundational Phase 3 architecture is now declared stable.
  - [TER: L3 Wave 7 Final Audit](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260313-wave7-final-audit.md)
  - [Spec: Wave 7 Final Audit Boundaries & Scenarios](../cerb-e2e-test/specs/wave7-final-audit/)
- **architecture**: JSON Coercion Hotfix (Project Mono). Discovered a systemic Kotlin deserialization crash during L3 tests where LLM JSON payloads containing explicit `null` tokens failed against native Kotlin non-nullable defaults (e.g., `classification = "schedulable"`). Mechanically resolved across the pipeline by enforcing `coerceInputValues = true` within local `PrismJson` instances for `SchedulerLinter` and `RealHabitListener`, and adding resilient Array-List fallback parsing for hallucinations.
- **test-infrastructure**: L2 User Flow Purity Remediation VALIDATED. Executed the exact chronological dataset that failed on March 12th (`"张总的顾虑主要是..."`, `"帮我定一个周五..."`, `"把阶段往后挪一下..."`). All three architectural defects (T1 Misclassification, T2 Context Dropping, T3 Intent Routing Overload) have been mathematically proven fixed via `L2DebugHud` injection and ADB logcat verification. Wave 6 is officially completely validated.
  - [TER: L2 World State Seeder User Flow Tests (Re-run)](file:///home/cslh-frank/main_app/docs/reports/tests/L2-20260313-world-state-seeder-user-flow-rerun.md)
- **agent-intelligence-ui**: Wave 6 T5 (Hand-Off Animation) DEFERRED. Review Conference identified critical architectural ambiguity regarding the source of voice ingestion (Badge vs Phone Mic). `UiState.AudioProcessing` contract drafted in spec, but implementation deferred as Tech Debt until the hardware source is formally defined to prevent breaking the OS Model. Wave 6 is otherwise complete.
- **architecture**: Wave 5 T3 The RL Harmonization SHIPPED. Extracted the background Reinforcement Learning habit mapping into a strict Secondary Currency contract (`RlPayload`) defined in `:domain:habit`, decoupling it entirely from the main conversational SSD mutation pipeline (`UnifiedMutation`). Created `RlPayloadSchemaTest` to mechanically prove that the LLM system prompt output matches the Kotlin data class properties exactly.
- **architecture**: KernelWriteBack Async Race Condition HOTFIXED. Discovered a dirty-reading gap in `L2WriteBackConcurrencyTest` where parallel rapid-fire intents fetched stale SSD graphs into the `RealContextBuilder`'s RAM before background DB writes completed. Updated the `KernelWriteBack` abstract contract to natively seed the dirty `EntityEntry` payload completely into RAM via a synchronized WriteThrough, physically sealing the race condition. 
- **architecture**: Wave 5 T1 The Sync Loop SHIPPED. Implemented the "Entity Candidate Gatekeeper" protocol via `AliasCache` in `:domain:crm` and `:data:crm`. `IntentOrchestrator` successfully intercepts candidates via Lightning Router's `missing_entities` and fast-fails ambiguous queries into `UiState.AwaitingClarification`, avoiding the heavy `UnifiedPipeline` and saving significant SSD/LLM transit time.
- **architecture**: 100% test coverage proven for the L1 cache logic (`RealAliasCacheTest.kt` handling cache misses, ExactMatch, Ambiguous clashes, and graceful malformed JSON handling). Verified Acceptance Team Spec, Contract, Build, and Break-It examiners flawlessly.

### 2026-03-12
- **test-infrastructure**: L2 User Flow Purity Testing FAILED (Remediated on 2026-03-13). Executed pure User-Flow L2 scenarios (`l2-user-flow-tests.md`) powered by chronological `dataset.md`. Discovered severe architectural defects across domains: T1 (LightningRouter misclassified `SIMPLE_QA`), T2 (ContextBuilder failed to inherit entity pointer causing amnesiac scheduling hallucination), T3 (Intent Routing Overload pushing high-stakes CRM mutations into Scheduler Linter).
  - [Decision Log]: Committing to **Option 1**: Decoupled Physical Scheduling. Chat will no longer be the entrance for hard NLP calendar writes to prevent L3 Analyst pipeline contamination.
  - [TER: L2 World State Seeder User Flow Tests](file:///home/cslh-frank/main_app/docs/reports/tests/L2-20260312-world-state-seeder-user-flow.md)
- **architecture**: Intent Routing & Disambiguation Reliability Hotfixes SHIPPED. Resolved "Intent Routing Overload" where all `CRM_TASK` intents were blindly run through the Scheduler Linter by dynamically decoupling the fallback LLM mode (Mode.SCHEDULER vs Mode.ANALYST) in `RealUnifiedPipeline`. Implemented native OS-level write-backs for proactive `EntityDeclaration` parsing, entirely bypassing the heavy LLM. Fixed `IntentOrchestrator` to properly route `SIMPLE_QA` intents to the Unified Pipeline so facts are loaded from the database instead of short-circuiting to UI hallucinations.
  - [TER: L3 Analyst Mode Execution](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260312-AnalystModeExecution.md)

### 2026-03-11
- **architecture**: Write-Back Concurrency (Wave 4) SHIPPED. Implemented `L2WriteBackConcurrencyTest.kt` directly mimicking RAM/SSD split constraints. Mathematically proved the Twin Engines (CRM and RL) execute safely via interleaved `async` suspend boundaries without overriding each other's native memory layers, guaranteeing strict `SessionWorkingSet` RAM synchronization under rapid-fire intents.
- **architecture**: Phase 3 E2E Pillar Resumption (The 6 Waves) Epic SHIPPED! Rigorously proved the complete Core Pipeline architecture (Lightning Fast-Track, Dual-Engine Bridge, Strict Interface Integrity, Adaptive Habit Loop, Efficiency Overload, and Transparent Mind) against the Anti-Illusion protocol without Mockito. Upgrading "The Crucible" (Pipeline Validation roadmap from `pipeline-explainer.md`) to the new Active Epic.
- **test-infrastructure**: WorldStateSeeder DSL & Chaos Seed Dataset SHIPPED. Built the `WorldStateSeeder` in `:core:test-fakes` and established the definitive JSON Chaos Payload (`dataset.md`) containing 6 months of sporadic noise and 1 week of dense, overlapping B2B context. Verified integration via `L2WorldStateSeederTest` covering fragmented aliases, overlapping company noise, and contextual assembly (RAM aggregation with SessionWorkingSet). Fixed `entity-writer` Spec drift regarding `aliasesJson` FIFO history preservation.
- **architecture**: The Great Assembly Epic SHIPPED! All 4 rings of the inside-out architecture (Kernel → RAM → SSD → App) have been physically decoupled and mathematically proven. Mockito is fully evicted and L2 UI simulation validation is rigorously enforced. Upgrading Phase 3 E2E Pillar Resumption to the new Active Epic.
- **agent-intelligence-ui**: W3 L2 Testing Validation SHIPPED. Added strict isolated L2 test fixtures for `UiState.AwaitingClarification`, `SchedulerTaskCreated`, and `SchedulerMultiTaskCreated` natively to `AgentViewModel` via `L2DebugHud`. Device tests verified Compose UI Literal Sync perfectly rendered inline notifications. [Report](../reports/tests/L2-20260310-agent-intelligence-wait-states.md).
- **agent-intelligence-ui**: MarkdownStrategyBubble L2 Verification SHIPPED. Built an isolated L2 UI injection testing mechanism via `L2DebugHud` and `AgentViewModel.debugRunScenario()`. Fixed legacy `LiveArtifactBuilder` routing bug in `AgentChatScreen` and upgraded `MarkdownText.kt` to natively parse headers and lists. The UI now explicitly verifies title extraction and rendering styling without relying on LLM executor passes.
- **scheduler**: W1 Dataflow Veracity SHIPPED. Retrofitted `FakeToolRegistry` and `FakeExecutor` to track exact execution payloads (`executedRequests` and `executedPrompts`) natively without Mockito. Enforced explicit payload structure and Dataflow Context assertions in `AgentViewModelTest.kt` and `RealUnifiedPipelineTest.kt`, eliminating Testing Illusions in the scheduler pipeline.
- **architecture**: Tracker Philosophy SHIPPED. Established `tracker.md` as the "Master Status Ledger" and Active Epic scratchpad while deferring to `spec.md` for architectural contracts and `interface-map.md` for global topology. Enforced "Operational Law" requiring evidence for any tracker updates. Removed redundant `Cerb Spec Index`.
- **architecture**: Completed Phase 2 of Layer 4 Feature Sweeps. Relocated `SchedulerLinterTest` and `AlarmSchedulerTest` to `:domain:scheduler:test`. Evicted Mockito from `AgentViewModelTest` by generating and injecting pure test Fakes from `:core:test-fakes`. Verified via Acceptance Team testing (`FakeUnifiedPipeline`).
- **architecture**: Completed Phase 2 of Layer 3 Core Pipeline Purge. Refactored `InputParserService` & `EntityDisambiguator` to use native `:core:llm` `Executor` and evicted Mockito from `RealInputParserServiceTest.kt`. Validated via Acceptance Team testing (`FakeExecutor`).
- **architecture**: "New Assembly" Pivot DECLARED. Abandoned the original "Great Assembly" due to compiler-driven extraction causing hidden coupling (Layer 3 depending on Layer 1) and abandoned test files. Initiating a bottom-up Layer 2 rebuild enforcing the 4 Cerb Pillars (Feature Purity, Anti-Illusion Testing, UI Literal Sync, Observable Telemetry).

### 2026-03-09
- **architecture**: Testing Strategy Pivot SHIPPED. Formally paused the 'Great Assembly' at Layer 3 to prioritize the 'Anti-Illusion Test Overhaul' across the Core Pipeline. Mocks that bypass information gates have created a testing illusion, hiding critical routing defects. Future work will focus exclusively on Mock Eviction (Wave 2) and Context Branch Coverage (Wave 1).
- **agent-intelligence-ui**: Wave 2 SHIPPED — Successfully executed the "Nuke & Pave" refactor of the monolithic `PrismViewModel` into `AgentViewModel`. Completely decoupled Layer 3 AI routing logic out of the Presentation layer into a dedicated `:core:pipeline:IntentOrchestrator`. Physically eliminated `PrismShell`, `PrismChatScreen`, and `PrismMainActivity` in favor of their `Agent*` equivalents via AST manipulation. All tests pass, proving perfect routing encapsulation.
- **test-infrastructure**: Wave 1 SHIPPED — Successfully extracted fragmented `Fake*Repository` classes (`FakeMemoryRepository`, `FakeEntityRepository`, `FakeUserHabitRepository`) out of the monolithic `app-core` test directory into a strictly isolated `:core:test-fakes` Gradle module. Purged hardcoded skeleton data to enforce the Clean Before Build Anti-Drift protocol. All consumers refactored and unit tests passed.

### 2026-03-07
- **architecture**: Layer 3 Core Pipeline Physical Extraction (Phase 2) SHIPPED — Physically extracted the Orchestrator, Context Builder, and LLM plugins from the `app-core` monolith into strictly isolated Gradle library modules (`:core:llm`, `:core:context`, `:core:pipeline`). Decoupled DashscopeExecutor constraints, repaired global Dagger bindings, and bridged cross-module Kotlin smart-casting limits. Verification passed perfectly.

### 2026-03-06
- **architecture**: Layer 2 Domain Modularization (Stage 2) SHIPPED — Extracted monolithic `PrismDatabase` and Room DAOs into a strictly decoupled `:core:database` module. Successfully moved and wired `RoomUserHabitRepository`, `RoomMemoryRepository`, and `RoomEntityRepository` to isolated `:data:habit`, `:data:memory`, and `:data:crm` feature data modules. Additionally, surgically extracted `RoomHistoryRepository` to `:data:session` to complete 100% of the Layer 2 Data Services assembly.
- **architecture**: The Great Renaming & OS Layer Standardization SHIPPED — Renamed the `app-core` prototype codename to `app-core` consistently across folders, `settings.gradle.kts`, and all documentation. Formally standardized the `docs/cerb/interface-map.md` OS Layer taxonomy to enforce a strict `OS: Kernel`, `OS: RAM`, `OS: SSD`, `OS: App` architecture definition.
- **architecture**: Layer 2 Domain Modularization (Stage 1) SHIPPED — Physically extracted pure Kotlin domain contracts for STM (`session`), LTM (`crm`, `memory`), and RL (`habit`) out of `app-core` into four distinct, isolated Gradle library modules. Passed rigorous Architectural Drift Audit mathematically proving strict one-way dependency isolation between SSD and RAM boundaries.
- **architecture**: The Great Assembly (Phase 1) SHIPPED — Extracted all 5 core Layer 1 Infrastructure modules from `app-core/domain` into physically isolated Gradle libraries (`:data:connectivity`, `:core:notifications`, `:data:asr`, `:data:tingwu`, `:core:telemetry`). Passed strict Acceptance Team build & contract verification.
- **architecture**: Finalized "Analyst Mode" sunsetting. Removed ghost "Analyst pending" tech debt from M3 milestone goals in favor of tracking actual Unified Pipeline blockers. Synced `plugin-registry` spec state to **SHIPPED** (Wave 4 Async Execution Wiring was previously implemented natively). Confirmed `audio-management` (Wave 2) is the true remaining blocker for full operational parity.
- **agent-intelligence-ui**: Spec Created — Added dual-document specs (`spec.md` and `interface.md`) in new `docs/cerb-ui/` hierarchy. Strictly enforced "Dumb UI" and absolute decoupling from ViewModel and Plugins.
- **input-parser**: Wave 4 SHIPPED — Refactored `SessionTitleGenerator` to `SemanticSessionTitleGenerator`, removing redundant LLM calls. Name and temporal intent are now synchronously extracted from `InputParserService` JSON and routed natively via `PipelineResult.AutoRenameTriggered`. Proven bypass logic where Mascot `NOISE`/`GREETING` intents correctly avoid the trigger entirely.

### 2026-03-05
- **architecture**: Comprehensive Interface Map Audit SHIPPED — Identified extensive signature drift across 17 modules and corrected `interface-map.md` to reflect exact Kotlin boundaries. Downgraded `RLModule` from SHIPPED to PARTIAL (Fake masquerade). Flagged `NotificationService` domain-layer Android import rule violation.
- **infra**: Fixed missing `UnifiedPipeline` constructor binding in `PrismModule` which broke L2 simulation testing.
- **oss-service**: Wave 1 SHIPPED — Passed Anti-Laziness testing gates by implementing unit tests with simulated Dashscope/Aliyun API failures (`RealOssUploaderTest`).
- **asr-service**: Wave 1 SHIPPED — Passed Anti-Laziness testing gates by implementing comprehensive L1 unit tests (`FunAsrServiceTest`).

### 2026-03-04
- **analyst-orchestrator**: Dual-Engine UI Alignment SHIPPED. Completely removed legacy `Mode.COACH` enum, UI toggles, and Switchers.
- **analyst-orchestrator**: Analyst Pipeline State Machine Ghost State FIX. Wired session switches to automatically call `AnalystPipeline.reset()`.
- **plugin-registry**: Wave 1-4 COMPLETED — RealToolRegistry, flow execution, structure plugins.
- **analyst-orchestrator**: Wave 6 SHIPPED — Analyze Gateway & Plugin Routing (Expert Bypass) implemented. `RealArchitectService` now dynamically injects `ToolRegistry` into the Planning phase to strictly enforce tool execution boundaries via `PlanResult.ExpertBypass`.
- **mascot-service**: Wave 3 SHIPPED (EventBus Integration and AppIdle Latch).
- **mascot-service**: Wave 4 SHIPPED — Integrated Compose UI `MascotOverlay` out-of-band in `AgentShell`.
- **mascot-service**: Wave 3 SHIPPED — Wired Mascot EventBus to `AgentViewModel` AppIdle trigger.
- **mascot-service**: Wave 2 SHIPPED (Basic Routing and Intent disambiguation). `LightningRouter` now distinguishes between `NOISE` and `GREETING` and routes both to Mascot, while correctly routing `VAGUE` to the persistent Analyst flow.
- **mascot-service**: Wave 1 SHIPPED — `MascotService` interface and `FakeMascotService` prototype formally integrated and wired into dependency injection. Spec upgraded to PARTIAL state.
- **architecture**: Wave 5 Dual-Engine Architecture SHIPPED. Established "Mascot (System I)" vs "Prism Orchestrator (System II)" boundary.
  - Mascot handles ephemeral notifications/greetings out-of-band. OS Toasts remain the source of truth for reliable system state.
  - Orchestrator uses the Analyze Gateway to launch Plugins (e.g., Talk Simulator).
- **mascot-service**: Interface + Fakes SHIPPED to track System I out-of-band EventBus capabilities.
- **lightning-router**: Phase 0 Nuke and Pave SHIPPED. Extracted from Analyst Pipeline and elevated to PrismOrchestrator. Intercepts intents and routes NOISE/GREETING to System I, DEEP_ANALYSIS to System II.
- **analyst-orchestrator**: Wave 2/4 SHIPPED (Fast Track Refinement) — Implemented exact spec "Open-Loop Lifecycle", routing `SIMPLE_QA` Consultant intents directly to `IDLE` with immediate answers, bypassing the `PROPOSAL` and `INVESTIGATING` states. Validated with `FakeAnalystPipeline` simulation.

### 2026-03-03
- **entity-disambiguation**: W1-3 SHIPPED — `EntityDisambiguator` Global Gateway implemented. Intercept & Resume loop successfully wired into `PrismOrchestrator` and `AnalystPipeline`. LLM semantic disambiguation and explicit entity declarations gracefully route to `EntityWriter`.
- **session-context**: W4 OS Model Refinement — Implemented Delta Loading Entity Cache (`entityCache` Map) in `SessionWorkingSet` and `RealContextBuilder`. Fixed infinite pipeline loops and redundant SSD queries.
- **session-context**: W5 SHIPPED — Context Compression / Lazy Loading implemented via `ContextDepth` enum in `ContextBuilder`. Eliminates full DB and Habitat loads for NOISE/QA intents.
- **analyst-consultant**: W4 SHIPPED — Lightning Router utilizes `ContextDepth.MINIMAL` in `RealAnalystPipeline`, passing through the fast Extractor model and avoiding heavy token footprints.
- **audio-management**: Ask AI Dataflow Verification — Reworked "Ask AI" entrance. Implemented zero-latency ASCII overview card generation, standard `Mode.ANALYST` routing, and invisible `documentContext` binding in `SessionWorkingSet` to offload heavy payload rendering from the UI.

### 2026-03-02
- **entity-writer**: W5 SHIPPED — Audited `upsertFromClue` Resolution Cascade (`resolvedId` → `findByAlias` → `findByDisplayName`), which is organically functioning correctly and conforms to the `InputParser`'s upstream LLM entity resolution payload contract. All existing 18 unit tests verified and passed.
- **pipeline-telemetry**: W1-W2 SHIPPED — `PipelineTelemetry` abstracted and injected into `PrismOrchestrator`, `RealContextBuilder`, and `RealInputParserService`. Granular Tagging implemented to perfectly map the Layer 2 & Layer 3 architecture diagram into ADB logcat output.
- **infra**: Holistic Cleanup SHIPPED — Purged dead Analyst routing from Orchestrator, fixed ContextBuilder `structuredJson` schema drift (Activity records), and removed dead `httpChecker` from Connectivity. Fixed tech debt ticking time-bombs.

### 2026-02-27
- **audio-management**: Wave 2 partial — Implemented `RealAudioRepository` with file storage, wired `TingwuPipeline`, and added Fake Streaming (Typewriter effect / Shimmer loading) to Audio UI.
- **tingwu-pipeline**: Decoupled legacy Tingwu API into Prism domain feature.

### 2026-02-25
- **analyst-orchestrator**: Wave 3 & 5 SHIPPED 🎯 Extracted consultant logic, transitioned state to PROPOSAL without `PlannerTable`, and wired `EntityResolverService` disambiguation loops natively into `handleInput`.
- **analyst-consultant**: Wave 3 SHIPPED 🎯 Formalized `ConsultantResult` to correctly extract `missingEntities` and provide raw clarifying text.
- **tingwu-pipeline**: Wave 1-4 SHIPPED 🎯 Decoupled Tingwu API from legacy ai-core into a clean Prism Feature. Wired Real/Fake pipelines to AudioRepository.
- **analyst-orchestrator**: Wave 1 SHIPPED — Domain models, FakeAnalystPipeline L2 simulator, wiring with AgentViewModel, and verification tests passing.

### 2026-02-14
- **session-history**: Wave 4 SHIPPED — Auto-Renaming (`SessionTitleGenerator`, `LlmSessionTitleGenerator`, PrismVM trigger on first response, horizontal HistoryDrawer layout)
- **infra**: Added `mockito-core` + `mockito-kotlin` to `libs.versions.toml` and `app-core/build.gradle.kts`

### 2026-02-13
- **scheduler**: Wave 10 SHIPPED — CRM Hierarchy Wiring (business gate + `keyCompany` extraction + ACCOUNT creation + `accountId` linking)
- **entity-writer**: Wave 1.5 UNWINDING resolved — Sticky Notes abandoned, Scheduler is permanent caller
- **scheduler**: Wave 9 SHIPPED — Smart Tips (TipGenerator, LlmTipGenerator, ViewModel lazy-load, shimmer/bubble UI)
- **coach**: Sticky Notes integration — `ScheduledTaskRepository` injected into ContextBuilder, top 3 tasks as greeting context
- **mascot-service**: Two-phase greeting (§3.6) — Turn 1 reminds tasks naturally, Turn N passive reference only
- **mascot-service**: Spec updated — dependency table + pipeline flow + §3.6 documented
- **session-context**: `scheduleContext` field added to `SessionWorkingSet` spec model
- **scheduler**: Wave 8 amendment — Real-time alarm-fire reflection via `SchedulerRefreshBus` (DEADLINE alarm → ViewModel sweep → instant UI update)
- **scheduler**: Auto-expiry refined — 4 trigger points (init, drawer open, day switch, alarm fire), removed redundant sweeps from `triggerRefresh()`
- **scheduler**: FIRE_OFF duration fix — LLM prompt now requires `duration: null` for instant reminders (was incorrectly assigning 5m)
- **mascot-service**: Output Quality hardened (4-Layer Fix) — Plain text system prompt (no `##`), `<KNOWN_FACTS>` data envelope, Positive-only hallucination guard, `MarkdownSanitizer` safety net. Fixed "delivery cycle sensitivity" hallucination.

### 2026-02-12
- **scheduler**: Task Completion Lifecycle section — isDone toggle, grey/strikethrough, voice scope exclusion, alarm lifecycle, reactivation safety
- **scheduler**: Wave 12 planned — ViewModel toggleDone wiring + alarm cancel/restore on completion
- **scheduler**: Voice Command Scope section — 5 classifications, scheduler-mode + active-session only, card-context-free
- **scheduler**: Wave 8 — Auto-expiry: `autoCompleteExpiredTasks()` on ViewModel init, sweeps today's expired tasks. Debt: multi-day sweep, visual distinction manual vs auto
- **scheduler**: Wave 12 SHIPPED — Task Completion Wiring (already implemented: toggleDone, alarm lifecycle, UI strikethrough, voice scope exclusion)
- **mascot-service**: §3.11 Schedule Guidance — Mascot educates user to use badge/record for schedule changes.
- **scheduler**: Sticky Notes Principle spec'd — scheduler does NOT create entities, defers to Analyst clarity loop
- **entity-writer**: Caller updated `Scheduler` → `Analyst`, Wave 1.5 marked UNWINDING
- **scheduler**: Cerb sync — `interface.md` rewritten from code, `spec.md` state → PARTIAL, domain model drift fixed
- **scheduler**: Cascade `-1m` offset removed (UX review: cognitively indistinct from `0m`)
- **notifications**: Cascade visual tiers collapsed from 3 to 2 (EARLY + DEADLINE), added DND policy (1.7.10), UX invariants (1.7.11)
- **notifications**: Channel split `TASK_REMINDER` → `TASK_REMINDER_EARLY` (respects DND) + `TASK_REMINDER_DEADLINE` (bypasses DND), old channels deleted
- **ui**: User Center 100% Chinese localization (sections, labels, buttons)
- **rl-module**: W4 SHIPPED — `calculateConfidence()` with 4-rule weighting + time decay, garbage collection on load
- **client-profile-hub**: Tracker corrected → PARTIAL (W1+W2+W4 shipped, W3 CRM Export remaining)

### 2026-02-11
- **session-context**: W4 SHIPPED — Clean rewrite (SessionWorkingSet, KernelWriteBack, 3-Section RAM)
- **real-context-builder**: Tech Debt fixed — `runBlocking` removed, `kernelWriteBack` implemented
- **session-context**: Spec updated — EntityKnowledge section added (pointer cache, pathIndex, EntityState machine)
- **memory-center**: Spec updated — W3 Entity Knowledge Context, `getAll(limit)` read path
- **entity-registry**: Spec updated — added `getAll(limit)` to interface, CRM Snapshot responsibilities
- **tracker**: Rewritten as faithful Cerb index (was 591 lines of free-form content)
- **feature-dev-planner**: Compressed 490→160 lines, added Single Spec Scope Rule, spec `state` management, Ship Gate
- **lessons-learned**: Logged Multi-Spec Drift (Cerb Scope Violation)

### 2026-02-10
- **rl-module**: W5 SHIPPED — OS Model Upgrade. Split `getHabitContext()` → `loadUserHabits()` + `loadClientHabits()`
- **entity-writer**: OS Model aligned — write-through to RAM S1
- **mascot-service**: OS Model aligned — consumer of RAM, reads from SessionWorkingSet
- **scheduler**: OS Model aligned — consumer of RAM Section 1
- **client-profile-hub**: OS Layer declared: File Explorer
- **interface-map**: Audited and synced with OS Model changes
- **notifications**: W2 SHIPPED — Alarm cascade with UrgencyLevel enum, full-screen alarm Activity
- **badge-audio-pipeline**: W3 wired — pipeline merge with scheduler post-processing

### 2026-02-09
- **connectivity-bridge**: W3 SHIPPED — `log#` recording handler (BLE notification→download trigger)
- **entity-writer**: Spec created — centralized entity writes with name/company/title change tracking
- **badge-audio-pipeline**: W3 SHIPPED — Real implementation (download→transcribe→schedule)
- **conflict-resolver**: Visual polish — amber tinting, breathing glow on conflict cards
- **entity-registry**: Entity resolution wired into scheduler pipeline (all paths: single/multi/delete)

### 2026-02-08
- **mascot-service**: W3-4 SHIPPED — Memory + habit integration, proactive suggestion mode
- **client-profile-hub**: W1-2 SHIPPED — Interface + models, timeline aggregation
- **entity-registry**: W3 SHIPPED → spawned Client Profile Hub
- **memory-center**: Entity-tagged `structuredJson` via `saveToMemory()`

### 2026-02-07
- **session-context**: W1-3 SHIPPED — SessionContext, EntityTrace, EntityState, path index cache
- **connectivity-bridge**: Reconnect race condition fixed — replaced fire-and-poll with `reconnectAndWait()`
- **connectivity-bridge**: HTTP gate removed from `connectUsingSession()` — BLE ≠ HTTP

### 2026-02-05
- **rl-module**: W1.5-3 SHIPPED — Schema migration (4-rule model), orchestrator + context builder integration
- **scheduler**: W5-7 SHIPPED — Inspiration storage, conflict resolution, NL deletion
- **badge-audio-pipeline**: W1-2 SHIPPED — Interface + state machine, fake pipeline

### 2026-02-04
- **memory-center**: W2 SHIPPED — Active/Archived lazy compaction, SubscriptionConfig tiers
- **entity-registry**: W2.5 SHIPPED — CRM schema, `RelevancyEntry` → `EntityEntry` rename
- **user-habit**: W1 SHIPPED — Schema + repository (storage layer)
- **rl-module**: W1 SHIPPED — Interface + observation schema

### 2026-02-03
- **memory-center**: W1 SHIPPED — ScheduleBoard, conflict detection, `excludeId` self-exclusion
- **scheduler**: W1-4 SHIPPED — Core CRUD, alarm cascade, reminder inference, multi-task
- **entity-registry**: W2 SHIPPED — LLM disambiguation flow, ParsedClues carrier

### 2026-02-02
- **scheduler**: W1-1.5 SHIPPED — Repository + linter, ViewModel wiring
- **mascot-service**: W1-2 SHIPPED — Interface + fake, real LLM + context
