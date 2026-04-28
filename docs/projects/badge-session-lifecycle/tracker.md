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
| 03 | audio-drawer-live-observation | done | Audio drawer inventory/progress flows now stay eagerly observed in both badge audio ViewModels so repository updates remain current while drawers are closed. | [sprints/03-audio-drawer-live-observation.md](sprints/03-audio-drawer-live-observation.md) |
| 04 | connectivity-isolation-repair-dataflow | done | Runtime Wi-Fi isolation repair now enters registered-badge credential repair, with focused JVM, full unit, APK install, screenshot, and logcat evidence committed in `4d273192b`. | [sprints/04-connectivity-isolation-repair-dataflow.md](sprints/04-connectivity-isolation-repair-dataflow.md) |
| 05 | connectivity-dataflow-monitoring-fixloop | cancelled | Superseded by Sprint 06 before operation; local Sprint 05 audit material is stale after `b7340f878` and is not current truth without re-audit. | — |
| 06 | badge-wifi-recovery-state-machine | done | Superseded Sprint 05's connectivity north-star/spec/runtime recovery path; BLE disconnected remains transport recovery, solid-IP HTTP media failure triggers bounded single-flight saved-credential replay, and multi-badge sync/download work is scoped to the active badge. | [sprints/06-badge-wifi-recovery-state-machine.md](sprints/06-badge-wifi-recovery-state-machine.md) |
| 07 | connectivity-dataflow-simplification | done | Removed stale phone-Wi-Fi-derived and HTTP-unreachable reconnect error branches with green focused tests/build/install and constrained one-badge L3 reconnect evidence; multi-badge and hard manual-collaboration paths were explicitly skipped. | [sprints/07-connectivity-dataflow-simplification.md](sprints/07-connectivity-dataflow-simplification.md) |
| 08 | connectivity-backend-lifecycle-cleanup | blocked | Backend lifecycle cleanup met local structure/test/build gates and reduced target files below 650 LOC, but adb install and filtered logcat L3 evidence could not be captured because no device/emulator was attached. Unresolved connectivity-contract and evidence follow-up absorbed by bake-transformation project (tier-1 connectivity-bridge cluster). | [sprints/08-connectivity-backend-lifecycle-cleanup.md](sprints/08-connectivity-backend-lifecycle-cleanup.md) |
| 09 | audio-card-hold-state | done | Audio cards now render a calm HOLD state during badge-switch resume windows instead of showing active download animation while transport is not ready. | [sprints/09-audio-card-hold-state.md](sprints/09-audio-card-hold-state.md) |
| 10 | download-reconnect-dependency | blocked | Same-badge disconnect now cancels manual and active `rec#` HTTP download jobs, preserves interrupted entries for HOLD, and targeted-resumes only those filenames after the same badge returns to `Ready`; hardware L3 and full unit-test proof remain blocked in this environment. | [sprints/10-download-reconnect-dependency.md](sprints/10-download-reconnect-dependency.md) |

## Cross-Sprint Decisions

- Sprint 01 is docs only. Sprints 02 and 03 are blocked on Sprint 01 closing because they implement against the design doc.
- Dependency direction: audio repository may observe `DeviceRegistryManager`, but the registry must not import audio types.
- UI surfaces must use `SharingStarted.Eagerly` for flows that carry real-time badge state (audio files, download progress). `WhileSubscribed` is acceptable only for flows that are purely derived from user actions and can tolerate a snapshot.
- All sprint contracts in this project are authored as starting hypotheses, not definitive prescriptions. The operator must evaluate each fix in context before executing.
- Hardware/runtime lifecycle regressions discovered during a later sprint should become their own dataflow test/fix sprint when they cross feature boundaries. Do not hide connectivity repair work inside an audio-only sprint.
- Phone SSID is diagnostic only for this project. Badge Wi-Fi recovery branches from BLE state, badge-reported IP/SSID, active HTTP media readiness, and remembered user-confirmed credentials.
- Sprint 06 absorbed Sprint 05's connectivity north-star, spec sync, and runtime recovery implementation direction, but it did not close Sprint 03's UI observation debt; Sprint 03 closed separately afterward.
- The untracked Sprint 05 contract/evidence should be treated as pre-Sprint-06 stale audit material unless a later operator re-audits it against `b7340f878` or newer code.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.

## Ongoing Justification

Sprint 08 continues this project past the six-sprint guideline because Sprint 06 closed the recovery model and Sprint 07 removed stale same-surface branches, but the backend reconnect, registered-badge repair, and HTTP media-readiness corridor still has lifecycle-sensitive support-code debt. Keeping this work in the same project preserves the evidence chain for registered-badge recovery, active-device ownership, and active-download guards without expanding into audio sync, BLE GATT internals, setup ViewModels, or UI redesign.
