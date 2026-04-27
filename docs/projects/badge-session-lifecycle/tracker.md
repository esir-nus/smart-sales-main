# Project: badge-session-lifecycle

## Objective

Establish a correct, holistically-designed lifecycle for multi-device badge session management and audio sync. The audio and connectivity features in this app were built incrementally — each sprint added a capability assuming a single active device at a time, without considering what should happen when the active device changes. The result is architectural debt: in-flight work from one device leaks into another device's session, and UI surfaces can't observe live updates because the observation chain breaks when surfaces close.

This project approaches the problem from the top: Sprint 01 defines the correct lifecycle as a design document, and subsequent sprints implement against it. The sprint contracts in this project describe the nature of each problem and the reasoning behind the fix — not just a prescription of what to change.

Baseline: commit `620d3136f` (multi-device cards-first modal, BLE detection).

## Status

open

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | lifecycle-design | done | Authored the badge session lifecycle core flow covering single-device states, multi-device switch teardown, audio sync binding, and UI observation rules. | [sprints/01-lifecycle-design.md](sprints/01-lifecycle-design.md) |
| 02 | audio-download-device-scope | authored | — | [sprints/02-audio-download-device-scope.md](sprints/02-audio-download-device-scope.md) |
| 03 | audio-drawer-live-observation | authored | — | [sprints/03-audio-drawer-live-observation.md](sprints/03-audio-drawer-live-observation.md) |

## Cross-Sprint Decisions

- Sprint 01 is docs only. Sprints 02 and 03 are blocked on Sprint 01 closing because they implement against the design doc.
- Dependency direction: audio repository may observe `DeviceRegistryManager`, but the registry must not import audio types.
- UI surfaces must use `SharingStarted.Eagerly` for flows that carry real-time badge state (audio files, download progress). `WhileSubscribed` is acceptable only for flows that are purely derived from user actions and can tolerate a snapshot.
- All sprint contracts in this project are authored as starting hypotheses, not definitive prescriptions. The operator must evaluate each fix in context before executing.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.
