# Project: firmware-protocol-intake

## Objective

Receive firmware protocol updates from the hardware team and reflect them in the app. The flow is always firmware-first, software-after: hardware team ships a new or revised BLE/HTTP protocol item, this project records the spec delta, then wires the app-side handler (connectivity module by default; may touch adjacent modules when a drop reaches further). The canonical protocol SOT is `docs/specs/esp32-protocol.md`; app-side module ownership is tracked through `docs/cerb/interface-map.md`.

This is a **persistent intake project**. It stays open as a long-lived funnel for firmware drops. Each drop (or a small coherent cluster of drops) becomes one sprint. The six-sprint guideline in `docs/specs/project-structure.md` does not apply here in the usual decompose-or-close sense — see Ongoing Justification.

## Status

open — authored 2026-04-24

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | bat-listener-wiring | done | Wired `Bat#<0..100>` through parser -> bridge -> `ConnectivityViewModel.batteryLevel`, added focused tests, and verified with scoped `testDebugUnitTest` plus `:app:assembleDebug` | [01-bat-listener-wiring.md](sprints/01-bat-listener-wiring.md) |
| 02 | batlevel-nullable-ui | authored 2026-04-24 (blocked-by sprint 01) | Flip `ConnectivityViewModel.batteryLevel` to `StateFlow<Int?>` seeded `null`; propagate nullable through Modal / Drawer / Island coordinator; render `--%` placeholder until first `Bat#` push arrives | [02-batlevel-nullable-ui.md](sprints/02-batlevel-nullable-ui.md) |

## Genesis

- **2026-04-24** — Firmware team shipped three new BLE items (volume / Bat# / Ver:). Spec delta landed ad-hoc in `docs/specs/esp32-protocol.md` §§8-10 and `docs/cerb/interface-map.md:147` before this project existed; the edit was a single-step doc update that did not warrant a sprint contract per the feedback rule "contracts for multi-step work with iteration value; ad-hoc ops run outside the contract model". This project exists to hold all subsequent firmware drops and the follow-up wiring work surfaced by that drop.

## Cross-Sprint Decisions

- **Intake direction:** firmware-first, app-after. No sprint in this project drives a firmware change; the hardware team is the source.
- **Canonical protocol SOT:** `docs/specs/esp32-protocol.md`. Every sprint in this project either extends that spec, wires app-side to an already-extended spec, or both.
- **Sprint granularity:** one sprint per drop (or per coherent cluster of drops that share a listener pattern). If a drop only touches docs and has no iteration value, record it as a Genesis-style note under a dated heading here and skip the sprint contract.
- **Operator default:** Codex. User may override per sprint at handoff.
- **Trunk:** `develop` for app-side wiring (connectivity module lives on the Android side). Harmony-native ports of the same protocol changes are a separate sprint under `platform/harmony` if/when needed, authored against this same project tracker.

## Ongoing Justification

This project is intentionally persistent. Per `docs/specs/project-structure.md` size discipline, projects running past sprint 6 must declare why. The justification here is scope: the hardware team's firmware drops are an open-ended upstream stream, not a bounded objective. Closing this project would force each drop to spawn a new project folder, which is bureaucratic overhead without information value. Re-evaluation at sprint 6 will consider whether the stream has slowed enough to close and migrate to ad-hoc docs-only updates, or whether the project is genuinely load-bearing.

## Inputs Pending for Later Sprints

- **Sprint 01 (`bat-listener-wiring`):** Listener pattern to mirror is the `log#` / `rec#` flow in `ConnectivityBridge.recordingNotifications()` / `audioRecordingNotifications()`. Replace the provisional `ConnectivityViewModel.batteryLevel` source flagged in `docs/cerb/interface-map.md:147` and `docs/cerb-ui/dynamic-island/spec.md:100`. Firmware push cadence is periodic but not yet specified; sprint authoring should clarify with hardware team or treat any receipt as authoritative.
- **Sprint 02 (`batlevel-nullable-ui`):** UI follow-up pass once sprint 01 has shipped the bridge-backed `batteryLevel`. Flips the type to `StateFlow<Int?>` and propagates through `ConnectivityModal`, `HistoryDrawer`, `SimShellDynamicIslandCoordinator`, `SimHomeHeroShell` so "no `Bat#` push received yet" is distinguishable from a real low reading. Out of scope: Harmony-native port, new low-battery visual state, charging indicator.
- **Future sprint — `Ver:` handler:** Blocked on firmware team finalizing the trigger (on-connect vs on-boot vs query-reply). When that arrives, authoring needs to decide where the version string is stored (likely `ConnectivityViewModel` or a new `BadgeFirmwareInfo` seam) and whether it surfaces in UserCenter or stays internal.
- **Future sprint — Harmony-native battery port:** Mirror sprints 01 + 02 on `platform/harmony` once both ship on Android. Needs Harmony-side `AppState` surface for `batteryLevel: Int?` and hero-level island equivalent; authored separately under this project tracker when the Harmony connectivity seam is ready to consume `Bat#` pushes.

## Lessons Pointer

- None yet. At first sprint close, propose one lesson candidate: "firmware drops arrive as docs-only deltas first; app-side wiring follows in a later sprint. Do not bundle the spec edit and the wiring into the same contract unless both happen in the same session."
- At sprint 02 authoring (2026-04-24), noted a second candidate lesson for eventual close: "firmware-drop follow-up sprints naturally split into two passes — data-plumbing pass (wire the seam, keep existing non-null type for binary compat) then semantics pass (widen type once all consumers are mapped). The two-pass split is cheaper than one fat contract because the consumer audit is done with the seam already in place." Surface at sprint 02 close.
