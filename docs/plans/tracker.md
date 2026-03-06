# Smart Sales Prism Tracker

> **Purpose**: Cerb spec index + changelog. Faithfully tracks spec states and key history.
> **Rule**: Rows are derived FROM specs. History entries are commit-style — key info only.
> **Last Updated**: 2026-02-14

---

## Cerb Spec Index

### Pipeline & Modes

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [model-routing](../cerb/model-routing/spec.md) | SHIPPED | — |
| [input-parser](../cerb/input-parser/spec.md) | SHIPPED | — |
| [entity-disambiguation](../cerb/entity-disambiguation/spec.md) | SHIPPED | — |
| [pipeline-telemetry](../cerb/pipeline-telemetry/spec.md) | SHIPPED | — |
| [scheduler](../cerb/scheduler/spec.md) | PARTIAL | W8: Pipeline Unification |
| [conflict-resolver](../cerb/conflict-resolver/spec.md) | SHIPPED | — |
| [badge-audio-pipeline](../cerb/badge-audio-pipeline/spec.md) | SHIPPED | W4: Error Recovery |
| [lightning-router](../cerb/lightning-router/spec.md) | SHIPPED | — |
| [mascot-service](../cerb/mascot-service/spec.md) | SHIPPED | Wave 4: UI Integration | System I Overlay |
| [plugin-registry](../cerb/plugin-registry/spec.md) | SHIPPED | — |

### Data & Memory

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [session-context](../cerb/session-context/spec.md) | SHIPPED | W5: Context Compression |
| [session-history](../cerb/session-history/spec.md) | SHIPPED | — |
| [memory-center](../cerb/memory-center/spec.md) | SHIPPED | — |
| [entity-registry](../cerb/entity-registry/spec.md) | SHIPPED | — |
| [entity-writer](../cerb/entity-writer/spec.md) | SHIPPED | — |
| [user-habit](../cerb/user-habit/spec.md) | SHIPPED | — |
| [rl-module](../cerb/rl-module/spec.md) | SHIPPED | — |
| [client-profile-hub](../cerb/client-profile-hub/spec.md) | PARTIAL | W3: CRM Export |

### Connectivity & Hardware

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [connectivity-bridge](../cerb/connectivity-bridge/spec.md) | SHIPPED | W4: Battery (pending HW) |
| [device-pairing](../cerb/device-pairing/spec.md) | PARTIAL | W2: Robustness |
| [asr-service](../cerb/asr-service/spec.md) | SHIPPED | W3: Retry |
| [oss-service](../cerb/oss-service/spec.md) | SHIPPED | W2: Resilience |
| [audio-management](../cerb/audio-management/spec.md) | ACTIVE | W2: Real Repository Wiring |
| [tingwu-pipeline](../cerb/tingwu-pipeline/spec.md) | SHIPPED | — |

### Plugins & Tools

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|

### UI & Presentation

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [agent-intelligence](../cerb-ui/agent-intelligence/spec.md) | SPEC_ONLY | W1: Basic Components |

---

## Changelog

> Key spec/impl changes, newest first. Like `git log --oneline`.

### 2026-03-06

- **architecture**: Layer 2 Domain Modularization (Stage 2) SHIPPED — Extracted monolithic `PrismDatabase` and Room DAOs into a strictly decoupled `:core:database` module. Successfully moved and wired `RoomUserHabitRepository`, `RoomMemoryRepository`, and `RoomEntityRepository` to isolated `:data:habit`, `:data:memory`, and `:data:crm` feature data modules.
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
- **mascot-service**: Wave 4 SHIPPED — Integrated Compose UI `MascotOverlay` out-of-band in `PrismShell`.
- **mascot-service**: Wave 3 SHIPPED — Wired Mascot EventBus to `PrismViewModel` AppIdle trigger.
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
- **analyst-orchestrator**: Wave 1 SHIPPED — Domain models, FakeAnalystPipeline L2 simulator, wiring with PrismViewModel, and verification tests passing.

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

---

## Product Milestones

| Milestone | Status |
|-----------|--------|
| **M0: Prism Spec** | ✅ |
| **M1: Core Pipeline** | ✅ |
| **M2: Memory Integration** | ✅ |
| **M3: All Modes** | 🚧 (Blocked by audio-management W2) |
| **M4: Polish** | 🔲 |

---

## Tech Debt (Deferred for Beta)

| Item | Location | Priority |
|------|----------|----------|
| `delay()` in UI | `ResponseBubble.kt`, `ConnectivityModal.kt`, `OnboardingScreen.kt` | Low |
| FTS4 Search | `MemoryDao.kt` — LIKE for Chinese | Medium |
| Remaining Fakes | `FakeHistoryRepository`, `FakeAudioRepository` — not Room-backed | Low |
| TOCTOU in observe() | `RoomUserHabitRepository.kt` | Low |
| Room error handling | `Room*Repository` — no try-catch on writes | Low |
| ~~Dead `httpChecker`~~ | ~~`DefaultDeviceConnectionManager.kt`~~ | ~~Low~~ | ✅ **Resolved** — Deleted interface and removed Dagger bindings. |
| ~~`structuredJson` schema inconsistency~~ | ~~`RealContextBuilder.recordActivity()`~~ | ~~Low~~ | ✅ **Resolved** — Updated schema to include both `relatedEntityIds` array and scalar `entityId`. |
| ~~Sticky Notes Boundary~~ | ~~`PrismOrchestrator.createScheduledTask()` calls `entityWriter.upsertFromClue()`~~ | ~~**High**~~ | ✅ **Resolved** — Sticky Notes abandoned. Scheduler creates PERSON + ACCOUNT entities for business-relevant contacts (Wave 10 SHIPPED). |
| **Confidence-Based Reminder Interceptor** | Replace deterministic round-1 wrap-up with LLM confidence-based interception. Agent decides when to surface schedule context: (1) User greets/noise → inject, (2) User discusses agenda → inject, (3) User wraps up work → suggest completion. Requires classifier or LLM self-assessment of conversation intent. Current workaround: smarter prompting that lets LLM decide naturally. | Medium |
| **L2 UI Verification** | `MarkdownStrategyBubble.kt` — Verify the title extraction and rendering styling (T2 test from `l2_test_plan.md`) after major component refactoring is complete. | High |
| **NotificationService Layer Violation** | `NotificationService.kt` in `domain` imports `android.app.PendingIntent` | High |
---

## Quick Links

- [os-model-architecture.md](../specs/os-model-architecture.md) — RAM/SSD mental model
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — UX SOT
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping

---

## The Great Assembly (Core-to-Periphery Architecture Audit)

> **Phase Context**: The era of rapid, parallel "Lego block" development is complete. We are now in the rigorous assembly phase. We evaluate each module strictly from Core (Layer 1) to Periphery (Layer 5). Good code is integrated; rotten/drifted code is subjected to "Rewrite Over Extract" to perfectly match the Spec and `interface-map.md`. No new concepts allowed.

### Layer 1: Infrastructure Services (The Power Plant & Water)
*Self-contained libraries with no upstream dependencies. Must be perfect.*
- [x] `ConnectivityBridge` (extracted to `:data:connectivity`)
- [x] `NotificationService` (extracted to `:core:notifications`)
- [x] `OSS` (extracted to `:data:oss`)
- [x] `ASR` (extracted to `:data:asr`)
- [x] `TingwuPipeline` (extracted to `:data:tingwu`)
- [x] `PipelineTelemetry` (extracted to `:core:telemetry`)

### Layer 2: Data Services (The Foundation)
*Stores and queries domain data. SSD and RAM storage boundaries.*
- [x] `EntityWriter` (extracted to `:data:crm`)
- [x] `EntityRegistry` (extracted to `:data:crm`)
- [x] `MemoryCenter` (extracted to `:data:memory`)
- [x] `UserHabit` (extracted to `:data:habit`)
- [ ] `SessionHistory`
- [x] `SessionContext`

### Layer 3: Core Pipeline (The Roads & Intersections)
*Orchestrates the LLM processing and routes intents.*
- [ ] `ContextBuilder`
- [ ] `InputParser`
- [ ] `EntityDisambiguator`
- [ ] `LightningRouter`
- [ ] `ModelRegistry` & `Executor`
- [ ] `PluginRegistry`
- [ ] `UnifiedPipeline`

### Layer 4 & 5: Features & Intelligence (The Hospitals & Schools)
*User-facing integrations that consume Layer 2/3.*
- [ ] `MascotService`
- [ ] `Scheduler` & `ScheduleBoard`
- [ ] `BadgeAudioPipeline`
- [ ] `ConflictResolver`
- [ ] `DevicePairing`
- [ ] `SessionTitleGenerator`
- [ ] `ClientProfileHub` (Layer 5)
- [ ] `RLModule` (Layer 5)
