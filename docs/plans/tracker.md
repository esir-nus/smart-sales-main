# Smart Sales Prism Tracker

> **Purpose**: Cerb spec index + changelog. Faithfully tracks spec states and key history.
> **Rule**: Rows are derived FROM specs. History entries are commit-style — key info only.
> **Last Updated**: 2026-02-12

---

## Cerb Spec Index

### Pipeline & Modes

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [coach](../cerb/coach/spec.md) | SHIPPED | — |
| [scheduler](../cerb/scheduler/spec.md) | PARTIAL | W10: Sticky Notes Boundary |
| [conflict-resolver](../cerb/conflict-resolver/spec.md) | SHIPPED | — |
| [badge-audio-pipeline](../cerb/badge-audio-pipeline/spec.md) | SHIPPED | W4: Error Recovery |
| [notifications](../cerb/notifications/spec.md) | SHIPPED | — |

### Data & Memory

| Cerb Shard | State | Next Wave |
|------------|-------|-----------|
| [session-context](../cerb/session-context/spec.md) | SHIPPED | W5: Context Compression |
| [memory-center](../cerb/memory-center/spec.md) | SHIPPED | — |
| [entity-registry](../cerb/entity-registry/spec.md) | SHIPPED | — |
| [entity-writer](../cerb/entity-writer/spec.md) | PARTIAL | W1.5 UNWINDING (Sticky Notes) |
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
| [audio-management](../cerb/audio-management/spec.md) | SPEC_ONLY | W2: Room Persistence |

---

## Changelog

> Key spec/impl changes, newest first. Like `git log --oneline`.

### 2026-02-12

- **scheduler**: Task Completion Lifecycle section — isDone toggle, grey/strikethrough, voice scope exclusion, alarm lifecycle, reactivation safety
- **scheduler**: Wave 12 planned — ViewModel toggleDone wiring + alarm cancel/restore on completion
- **scheduler**: Voice Command Scope section — 5 classifications, scheduler-mode + active-session only, card-context-free
- **scheduler**: Wave 11 planned — Global Reschedule (fuzzy match + create-and-delete, no card required)
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
| **Sticky Notes Boundary** | `PrismOrchestrator.createScheduledTask()` calls `entityWriter.upsertFromClue()` — creates phantom entities from unconfirmed intentions. **NOT an isolated code removal.** Requires: (1) Remove `upsertFromClue` from scheduler pipeline, (2) Coach/Analyst ContextBuilder reads scheduler tasks, (3) Coach clarity loop asks user for entity confirmation ("你说要见王老板，见了吗？"). Must be tested **end-to-end with Coach mode**, not in isolation. See [scheduler spec §Sticky Notes](../cerb/scheduler/spec.md), [entity-writer spec §Note](../cerb/entity-writer/spec.md). | **High** |


---

## Quick Links

- [os-model-architecture.md](../specs/os-model-architecture.md) — RAM/SSD mental model
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — UX SOT
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping
