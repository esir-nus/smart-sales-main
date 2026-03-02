# Smart Sales Prism Tracker

> **Purpose**: Cerb spec index + changelog. Faithfully tracks spec states and key history.
> **Rule**: Rows are derived FROM specs. History entries are commit-style — key info only.
> **Last Updated**: 2026-02-14

---

## Cerb Spec Index

### Pipeline & Modes

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [input-parser](../cerb/input-parser/spec.md) | PARTIAL | W4: Auto-Renaming |
| [pipeline-telemetry](../cerb/pipeline-telemetry/spec.md) | SHIPPED | — |
| [coach](../cerb/coach/spec.md) | SHIPPED | — |
| [scheduler](../cerb/scheduler/spec.md) | PARTIAL | W8: Pipeline Unification |
| [conflict-resolver](../cerb/conflict-resolver/spec.md) | SHIPPED | — |
| [badge-audio-pipeline](../cerb/badge-audio-pipeline/spec.md) | SHIPPED | W4: Error Recovery |
| [analyst-orchestrator](../cerb/analyst-orchestrator/spec.md) | SHIPPED | 🎯 |
| [analyst-consultant](../cerb/analyst-consultant/spec.md) | SHIPPED | 🎯 |
| [analyst-architect](../cerb/analyst-architect/spec.md) | SHIPPED | 🎯 |

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

---

## Changelog

> Key spec/impl changes, newest first. Like `git log --oneline`.

### 2026-03-02

- **entity-writer**: W5 SHIPPED — Audited `upsertFromClue` Resolution Cascade (`resolvedId` → `findByAlias` → `findByDisplayName`), which is organically functioning correctly and conforms to the `InputParser`'s upstream LLM entity resolution payload contract. All existing 18 unit tests verified and passed.

- **pipeline-telemetry**: W1-W2 SHIPPED — `PipelineTelemetry` abstracted and injected into `PrismOrchestrator`, `RealContextBuilder`, `RealInputParserService`, and `RealCoachPipeline`. Granular Tagging implemented to perfectly map the Layer 2 & Layer 3 architecture diagram into ADB logcat output.

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
- **infra**: Added `mockito-core` + `mockito-kotlin` to `libs.versions.toml` and `app-prism/build.gradle.kts`

### 2026-02-13

- **scheduler**: Wave 10 SHIPPED — CRM Hierarchy Wiring (business gate + `keyCompany` extraction + ACCOUNT creation + `accountId` linking)
- **entity-writer**: Wave 1.5 UNWINDING resolved — Sticky Notes abandoned, Scheduler is permanent caller
- **scheduler**: Wave 9 SHIPPED — Smart Tips (TipGenerator, LlmTipGenerator, ViewModel lazy-load, shimmer/bubble UI)
- **coach**: Sticky Notes integration — `ScheduledTaskRepository` injected into ContextBuilder, top 3 tasks as greeting context
- **coach**: Two-phase greeting (§3.6) — Turn 1 reminds tasks naturally, Turn N passive reference only
- **coach**: Spec updated — dependency table + pipeline flow + §3.6 documented
- **session-context**: `scheduleContext` field added to `SessionWorkingSet` spec model
- **scheduler**: Wave 8 amendment — Real-time alarm-fire reflection via `SchedulerRefreshBus` (DEADLINE alarm → ViewModel sweep → instant UI update)
- **scheduler**: Auto-expiry refined — 4 trigger points (init, drawer open, day switch, alarm fire), removed redundant sweeps from `triggerRefresh()`
- **scheduler**: FIRE_OFF duration fix — LLM prompt now requires `duration: null` for instant reminders (was incorrectly assigning 5m)
- **coach**: Output Quality hardened (4-Layer Fix) — Plain text system prompt (no `##`), `<KNOWN_FACTS>` data envelope, Positive-only hallucination guard, `MarkdownSanitizer` safety net. Fixed "delivery cycle sensitivity" hallucination.

### 2026-02-12

- **scheduler**: Task Completion Lifecycle section — isDone toggle, grey/strikethrough, voice scope exclusion, alarm lifecycle, reactivation safety
- **scheduler**: Wave 12 planned — ViewModel toggleDone wiring + alarm cancel/restore on completion
- **scheduler**: Voice Command Scope section — 5 classifications, scheduler-mode + active-session only, card-context-free
- **scheduler**: Wave 8 — Auto-expiry: `autoCompleteExpiredTasks()` on ViewModel init, sweeps today's expired tasks. Debt: multi-day sweep, visual distinction manual vs auto
- **scheduler**: Wave 12 SHIPPED — Task Completion Wiring (already implemented: toggleDone, alarm lifecycle, UI strikethrough, voice scope exclusion)
- **scheduler**: Wave 11 SHIPPED — Global Reschedule via voice (fuzzy match + create-and-delete). Fixed: LLM misclassifying 延迟 as deletion, infinite reschedule loop via explicit schedulable guard
- **coach**: §3.11 Schedule Guidance — educates user to use badge/record for schedule changes, no cross-mode mutation
- **scheduler**: Sticky Notes Principle spec'd — scheduler does NOT create entities, defers to Coach/Analyst clarity loop
- **entity-writer**: Caller updated `Scheduler/Coach` → `Coach/Analyst`, Wave 1.5 marked UNWINDING
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
- **coach**: Spec updated — entity knowledge injection from SessionWorkingSet Section 1
- **tracker**: Rewritten as faithful Cerb index (was 591 lines of free-form content)
- **feature-dev-planner**: Compressed 490→160 lines, added Single Spec Scope Rule, spec `state` management, Ship Gate
- **lessons-learned**: Logged Multi-Spec Drift (Cerb Scope Violation)

### 2026-02-10

- **rl-module**: W5 SHIPPED — OS Model Upgrade. Split `getHabitContext()` → `loadUserHabits()` + `loadClientHabits()`
- **entity-writer**: OS Model aligned — write-through to RAM S1
- **coach**: OS Model aligned — consumer of RAM, reads from SessionWorkingSet
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

- **coach**: W3-4 SHIPPED — Memory + habit integration, analyst suggestion mode
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
- **coach**: W1-2 SHIPPED — Interface + fake, real LLM + context

---

## Product Milestones

| Milestone | Status |
|-----------|--------|
| **M0: Prism Spec** | ✅ |
| **M1: Core Pipeline** | ✅ |
| **M2: Memory Integration** | ✅ |
| **M3: All Modes** | 🚧 (Analyst pending) |
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
| Dead `httpChecker` | `DefaultDeviceConnectionManager.kt` | Low |

| `structuredJson` schema inconsistency | `RealContextBuilder.recordActivity()` — uses `{"entityId":"..."}` instead of `{"relatedEntityIds":["..."]}` convention. Works today (LIKE query catches both), but any code parsing `relatedEntityIds` from activity records gets `emptyList()`. One-line fix: add `relatedEntityIds` field to `recordActivity()` output. | Low |
| ~~Sticky Notes Boundary~~ | ~~`PrismOrchestrator.createScheduledTask()` calls `entityWriter.upsertFromClue()`~~ | ~~**High**~~ | ✅ **Resolved** — Sticky Notes abandoned. Scheduler creates PERSON + ACCOUNT entities for business-relevant contacts (Wave 10 SHIPPED). |
| **Confidence-Based Reminder Interceptor** | Replace deterministic round-1 wrap-up with LLM confidence-based interception. Agent decides when to surface schedule context: (1) User greets/noise → inject, (2) User discusses agenda → inject, (3) User wraps up work → suggest completion. Requires classifier or LLM self-assessment of conversation intent. Current workaround: smarter prompting that lets LLM decide naturally. | Medium |


---

## Quick Links

- [os-model-architecture.md](../specs/os-model-architecture.md) — RAM/SSD mental model
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — UX SOT
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping
