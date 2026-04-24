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
| 02 | batlevel-nullable-ui | done | Flipped `ConnectivityViewModel.batteryLevel` to `StateFlow<Int?>` seeded `null`, propagated nullable through Modal / Drawer / Island coordinator, and rendered `--%` / hidden ambient battery until first `Bat#` push | [02-batlevel-nullable-ui.md](sprints/02-batlevel-nullable-ui.md) |
| 03 | wav-suffix-parser-fix | done | Normalized suffix-present and suffix-absent `log#` / `rec#` payloads to canonical `log_<ts>.wav` / `rec_<ts>.wav`, added focused filename tests, and closed the spec fallout note | [03-wav-suffix-parser-fix.md](sprints/03-wav-suffix-parser-fix.md) |
| 04 | ver-query-handler | done | Implemented `Ver#get` -> `Ver#...` through parser -> bridge -> `ConnectivityViewModel.firmwareVersion`, with auto-query-on-connect and a UserCenter refresh row; verified with focused `:app-core:testDebugUnitTest` coverage plus `:app:assembleDebug` | [04-ver-query-handler.md](sprints/04-ver-query-handler.md) |
| 05 | command-end-emitter | authored | Retire legacy commandend#1 emitter and wire Command#end through both pipeline terminal states per esp32-protocol.md §11 | [05-command-end-emitter.md](sprints/05-command-end-emitter.md) |

## Genesis

- **2026-04-24** — Firmware team shipped three new BLE items (volume / Bat# / Ver:). Spec delta landed ad-hoc in `docs/specs/esp32-protocol.md` §§8-10 and `docs/cerb/interface-map.md:147` before this project existed; the edit was a single-step doc update that did not warrant a sprint contract per the feedback rule "contracts for multi-step work with iteration value; ad-hoc ops run outside the contract model". This project exists to hold all subsequent firmware drops and the follow-up wiring work surfaced by that drop.
- **2026-04-24 (second drop, same day)** — Firmware team restated the full BLE protocol surface and added four deltas that the earlier drop did not carry:
  1. §10 Firmware Version — trigger finalized as app-initiated query/reply (`Ver#get` → `Ver#<project>.<major>.<minor>.<feature>`); delimiter changed from `:` to `#` for family consistency. Reflected in `docs/specs/esp32-protocol.md` §10.
  2. §11 Task Completion Signal (new) — app sends `Command#end` after a short-recording or long-recording transcription-analysis completes. No badge reply. Reflected in `docs/specs/esp32-protocol.md` §11.
  3. §12 SD Card Space Query (new) — app-initiated pull `SD#space` → `SD#space#<size>` (e.g., `SD#space#27.23GB`). Shares `SD#` prefix with the existing WiFi-status fragment; disambiguation by the `space` token. Reflected in `docs/specs/esp32-protocol.md` §12.
  4. §§6-7 Recording notifications — firmware now includes the `.wav` suffix in the payload (`log#<ts>.wav` / `rec#<ts>.wav`) and labels long-recording as `rec#` and short-recording as `log#`, which is the opposite axis from the current app-side routing (log# → scheduler, rec# → drawer). Reflected in `docs/specs/esp32-protocol.md` §§6-7 with an explicit Semantic Reconciliation note. Spec was updated docs-only; app-side parser behavior around the `.wav` suffix and the routing semantic are explicitly NOT touched by this doc edit — both are pending sprints (see Inputs Pending).
  
  Decision not to bundle all four into a single sprint: item 1 (Ver#) and item 3 (SD#space) are independent pull-protocol handlers; item 2 (Command#end) needs to plug into two existing pipelines; item 4 is a two-part investigation (parser bug from the `.wav` suffix, plus a semantic question for the firmware team about the log/rec labeling). Each becomes its own sprint entry below. Spec delta itself was a single docs-only commit per the same ad-hoc-ops rule cited above.

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
- **Sprint 04 (`ver-query-handler`):** Query/reply path now uses `DeviceConnectionManager.requestFirmwareVersion()` -> `ConnectivityBridge.firmwareVersionNotifications()` -> `ConnectivityViewModel.firmwareVersion: StateFlow<String?>`. The app auto-queries once per fresh connect, UserCenter exposes a manual refresh icon, and disconnect clears the value back to `null`.
- **Future sprint — `Command#end` emitter:** App-to-badge signal sent after the downstream pipeline triggered by a `log#` or `rec#` drop finishes. Authoring needs to decide (a) emit per pipeline (two sites: `RealBadgeAudioPipeline` scheduler completion + `SimBadgeAudioAutoDownloader` completion) vs one common sink; (b) whether terminal-error outcomes also emit; (c) whether the emission is best-effort fire-and-forget (like `volume#`) or uses the same round-trip pattern as `tim#get`. Fire-and-forget is the strong default per `docs/specs/esp32-protocol.md` §11.
- **Future sprint — `SD#space` handler:** App-initiated pull for badge SD card free space (`SD#space` → `SD#space#<size>`). Authoring needs to decide (a) parse strategy — the size string is pre-formatted human-readable (`27.23GB`) with no guaranteed unit, so the app must parse defensively or display raw; (b) call site — UserCenter diagnostics panel is the natural home, no reactive-UI use case justifies a listener; (c) disambiguation from the legacy §1 `SD#<SSID>` WiFi-fragment notification (the `space` token in the second segment is the discriminator). Watch the `RateLimitedBleGateway` floor — this is a user-triggered query, not a periodic poll.
- **Future sprint — log#/rec# semantic reconciliation (firmware-team coordination):** Firmware team's 2026-04-24 note labels `rec#` as long-recording and `log#` as short-recording, which is the opposite axis from the app's current pipeline routing (log# → scheduler/transcribe, rec# → drawer/audio-only). Spec delta is recorded in `docs/specs/esp32-protocol.md` §§6-7 Semantic Reconciliation note. This is NOT an app-side swap sprint — authoring requires a firmware-team clarification round first (are 长/短 physical button-press durations independent of app intent, or a genuine semantic flip?), then a decision on whether any app wiring changes at all. Do not spin this sprint up before the clarification exchange.
- **Future sprint — Harmony-native battery port:** Mirror sprints 01 + 02 on `platform/harmony` once both ship on Android. Needs Harmony-side `AppState` surface for `batteryLevel: Int?` and hero-level island equivalent; authored separately under this project tracker when the Harmony connectivity seam is ready to consume `Bat#` pushes.

## Lessons Pointer

- None yet. At first sprint close, propose one lesson candidate: "firmware drops arrive as docs-only deltas first; app-side wiring follows in a later sprint. Do not bundle the spec edit and the wiring into the same contract unless both happen in the same session."
- At sprint 02 authoring (2026-04-24), noted a second candidate lesson for eventual close: "firmware-drop follow-up sprints naturally split into two passes — data-plumbing pass (wire the seam, keep existing non-null type for binary compat) then semantics pass (widen type once all consumers are mapped). The two-pass split is cheaper than one fat contract because the consumer audit is done with the seam already in place." Surface at sprint 02 close.
