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
### ✅ Wave 16: Scheduler Decoupling (The Archival Purge)
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

### 🌊 Wave 17: Scheduler Fast-Track (Path A Execution)
> Objective: Implement the System III Dual-Path Architecture's Optimistic UI Execution (Path A), including a dedicated mutation module with lexical matching and the Small Attention Flow for conflict resolution. - 🧭 **North Star**: [docs/specs/scheduler-path-a-execution-prd.md](../specs/scheduler-path-a-execution-prd.md)
> ⚠️ **Campaign Constraints**:
> 1. **Legacy Reference Only**: Treat legacy code as a learning source, not the Source of Truth.
> 2. **UI Isolation**: Do not change current UI unless absolutely necessary (strict FE/BE separation).
- [x] ✅ **T0: Campaign North Star**
  - [x] **Docs**: Created `docs/specs/scheduler-path-a-execution-prd.md` to establish architectural principles and Intricacies.
- [x] ✅ **T1: Data Contracts & Linter Pipeline (One Currency Schema)**
  - [x] **Docs**: Created `docs/cerb/scheduler-linter/spec.md` and `interface.md`.
  - [x] **Interface Map**: Updated `docs/cerb/interface-map.md`.
  - [x] **Plan**: `/feature-dev-planner` — approved.
  - [x] **Execute**: Implemented FastTrack Parser to emit deterministic DTOs (`CreateTasksParams`, `RescheduleTaskParams`, `CreateInspirationParams`).
  - [x] **Test**: L1 (10/10 green), Mechanical (4/4 clean), Full build passes.
- [x] ✅ **T2: Dedicated Mutation Module (Atomic Operations & Conflict Evaluation)**
  - [x] **Docs**: Created `docs/cerb/scheduler-domain/spec.md` and `interface.md`.
  - [x] **Interface Map**: Read/update `docs/cerb/interface-map.md`.
  - [x] **Plan**: `/feature-dev-planner`
  - [x] **Execute**: Implement Lexical Fuzzy Matcher, ScheduleBoard conflict check, and atomic Delete -> Insert `@Transaction` updates.
  - [x] **Test**: L1/L2 Verification passes against the spec.
- [ ] 🔲 **T3: The Small Attention Flow (Presentation Layer)**
  - [ ] **Docs**: Create/update `docs/cerb-ui/scheduler-drawer/spec.md` and `contract.md`.
  - [ ] **Interface Map**: Verify `IAgentViewModel` isolation (No backend logic).
  - [ ] **Plan**: Run `/08-ux-specialist` for interaction design.
  - [ ] **Execute**: Implement UI feedback mechanisms (Caution Banner, Red Flag Cards, Inspiration Note Cards).
  - [ ] **Test**: Mechanical Check: `grep` to prove no concrete ViewModel imports.
