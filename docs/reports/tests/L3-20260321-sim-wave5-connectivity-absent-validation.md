# L3 On-Device Test Record: SIM Wave 5 Connectivity-Absent Validation

**Date**: 2026-03-21
**Tester**: User + agent
**Target Build**: `:app-core:installDebug`

---

## 1. Test Context & Entry State

* **Objective**: Close the remaining `T5.4` device-validation branch for the SIM connectivity-absent contract.
* **Testing Medium**: L3 physical device test on the installed debug build.
* **Initial Device State**: SIM launched through `com.smartsales.prism/.SimMainActivity` with connectivity disabled by airplane mode, and at least one already-persisted transcribed SIM audio item available for artifact-open / `Ask AI` reuse.

This run validates the disconnected branch only. It does not replace the separate real-badge routing proof already recorded in `docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`.

## 2. Execution Plan

* Open SIM while connectivity is absent.
* Check scheduler reachability/usability from the normal SIM shell.
* Open an already-persisted transcribed audio artifact while disconnected.
* Trigger grounded `Ask AI` from that artifact while disconnected.
* Return to audio browse mode and trigger manual `sync from badge` while still disconnected.
* Reopen the browse drawer again to confirm disconnected browse-open behavior stays usable.

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **T1 Scheduler While Disconnected** | Scheduler remains reachable and meaningful without forcing connectivity setup/manager. | User reported the scheduler path remained usable in airplane mode and was not blocked by Wi-Fi/connectivity absence. | ✅ |
| **T2 Persisted Artifact Open While Disconnected** | Already-persisted SIM audio artifact still opens while disconnected. | User reported the persisted transcribed artifact opened as expected while offline. | ✅ |
| **T3 Grounded `Ask AI` While Disconnected** | `Ask AI` opens the grounded SIM chat continuation surface rather than redirecting into connectivity management. | User reported grounded chat behavior was unaffected by the offline state and opened as expected from the artifact. | ✅ |
| **T4 Manual `sync from badge` Failure** | Manual sync fails explicitly, non-blockingly, and without clearing existing inventory or hijacking shell routing. | With airplane mode enabled, manual sync surfaced an explicit `oss_unknown null` error message. User reported the failure did not break the rest of the SIM flow and the remaining expectations stayed green. | ✅ |
| **T4b Browse-Open Auto-Sync Safety** | Browse-open stays usable while disconnected; lack of readiness should not hijack routing or block the drawer. | User reported the disconnected browse-open path remained usable and was not affected by Wi-Fi disconnection. | ✅ |

## 4. Supporting Evidence

* Primary evidence for this focused rerun is user-observed on-device behavior in airplane mode.
* No dedicated logcat / telemetry excerpt was captured in this note.
* Companion code-level evidence for the offline seams already existed before this rerun in the same Wave 5 session:
  * `SimShellHandoffTest` for persisted-artifact and grounded-chat telemetry
  * `SimAudioDebugScenarioTest` for disconnected sync-failure telemetry helper
  * `SimAudioDrawerViewModelTest` for manual-sync and browse-open auto-sync gating behavior

## 5. Deviation & Resolution Log

* **Offline failure literal is still rough**: the disconnected manual sync path surfaced `oss_unknown null`.
  * **Impact**: This is acceptable for `T5.4` because the branch failed explicitly, non-destructively, and did not turn connectivity into a hidden prerequisite.
  * **Follow-up**: Treat the literal as UI/UX debt. The offline sync failure message should eventually be mapped to a more human-readable SIM error surface.

## 6. Final Verdict

**✅ ACCEPTED (focused Wave 5 T5.4 disconnected L3 slice)**.

On **2026-03-21**, the remaining connectivity-absent validation branch is now green on device:

* scheduler remains usable while disconnected
* already-persisted audio artifacts remain usable while disconnected
* grounded `Ask AI` remains usable while disconnected
* manual `sync from badge` fails explicitly and non-destructively while disconnected

This closes the remaining Wave 5 proof that connectivity remains a support module rather than a hidden prerequisite for the two main SIM lanes. The remaining Wave 5 carry debt is presentation quality, not behavioral acceptance: the connectivity manager is still visually too heavy, and the offline sync error literal should later become more user-readable.
