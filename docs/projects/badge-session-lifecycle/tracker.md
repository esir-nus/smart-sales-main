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
| 02 | audio-download-device-scope | done | Audio repository now cancels queued and active badge downloads when the active device MAC changes, keeping old-device work from using the new connection. | [sprints/02-audio-download-device-scope.md](sprints/02-audio-download-device-scope.md) |
| 03 | audio-drawer-live-observation | authored | — | [sprints/03-audio-drawer-live-observation.md](sprints/03-audio-drawer-live-observation.md) |
| 04 | connectivity-isolation-repair-dataflow | in_progress | Runtime dataflow test/fix for hotspot/network isolation recovery; registered badges must enter Wi-Fi credential repair, not full re-pairing. | [sprints/04-connectivity-isolation-repair-dataflow.md](sprints/04-connectivity-isolation-repair-dataflow.md) |
| 05 | connectivity-dataflow-monitoring-fixloop | authored | Connectivity north-star reset, spec sync, static gap map, and adb/logcat dataflow matrix before bounded implementation follow-ups. | [sprints/05-connectivity-dataflow-monitoring-fixloop.md](sprints/05-connectivity-dataflow-monitoring-fixloop.md) |
| 06 | badge-wifi-recovery-state-machine | done | BLE disconnected remains a transport-reconnect branch; solid-IP HTTP media failure triggers bounded single-flight saved-credential replay, and multi-badge sync/download work is scoped to the active badge. | [sprints/06-badge-wifi-recovery-state-machine.md](sprints/06-badge-wifi-recovery-state-machine.md) |

## Cross-Sprint Decisions

- Sprint 01 is docs only. Sprints 02 and 03 are blocked on Sprint 01 closing because they implement against the design doc.
- Dependency direction: audio repository may observe `DeviceRegistryManager`, but the registry must not import audio types.
- UI surfaces must use `SharingStarted.Eagerly` for flows that carry real-time badge state (audio files, download progress). `WhileSubscribed` is acceptable only for flows that are purely derived from user actions and can tolerate a snapshot.
- All sprint contracts in this project are authored as starting hypotheses, not definitive prescriptions. The operator must evaluate each fix in context before executing.
- Hardware/runtime lifecycle regressions discovered during a later sprint should become their own dataflow test/fix sprint when they cross feature boundaries. Do not hide connectivity repair work inside an audio-only sprint.
- Phone SSID is diagnostic only for this project. Badge Wi-Fi recovery branches from BLE state, badge-reported IP/SSID, active HTTP media readiness, and remembered user-confirmed credentials.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.
