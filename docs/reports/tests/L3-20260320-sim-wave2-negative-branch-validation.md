# L3 On-Device Test Record: SIM Wave 2 Negative-Branch Validation

**Date**: 2026-03-20
**Tester**: Agent + user
**Target Build**: `:app-core:installDebug`

---

## 1. Test Context & Entry State

* **Objective**: Close the remaining Wave 2 negative-branch acceptance debt against `docs/core-flow/sim-audio-artifact-chat-flow.md` and `docs/cerb/sim-audio-chat/spec.md`.
* **Testing Medium**: L3 physical device test on the attached Android device.
* **Initial Device State**: Latest debug APK installed with `:app-core:installDebug`. SIM launched through `com.smartsales.prism/.SimMainActivity`.
* **Important UI Note**: The Wave 2 debug aid is intentionally **not** a global SIM debug HUD button. In code, the SIM shell keeps `showDebugButton = false`, and the scenario panel is exposed only inside `SimAudioDrawer` browse mode when `BuildConfig.DEBUG` is true.

## 2. Execution Plan

* **Trigger Surface**: Open SIM audio drawer in browse mode, scroll to the drawer-local `调试验证场景` panel, and exercise the three seeded validation cards.
* **Scenario Buttons**:
  * `Seed Failure Scenario`
  * `Seed Missing Sections Scenario`
  * `Seed Fallback Scenario`

## 3. Expected vs Actual Results

### T1: Explicit Transcription Failure

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | `SIM_Debug_Failure.mp3` must surface explicit failure / retry-ready behavior, not a fake completed artifact | User reported green on device after starting transcription from the seeded failure card; no fake artifact completion was observed | ✅ |
| **Contract Alignment** | Matches Core Flow Branch-S1: failure remains explicit and SIM does not fabricate transcribed output | Observed branch behavior matches the requested failure contract | ✅ |
| **Code/Fixture Basis** | Seeded card must route through the dedicated failure fixture rather than the global HUD | Verified in code: browse drawer-only scenario panel seeds `sim_debug_failure`, and repository throws the dedicated debug failure message for that id | ✅ |

### T2: Missing Optional Sections

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | `SIM_Debug_Missing_Sections.mp3` should render transcript/summary only and leave absent optional sections absent | User reported green on device: transcript/summary rendered, and no chapters/highlights/speaker sections were invented | ✅ |
| **Contract Alignment** | Matches Core Flow Branch-S2 and artifact-surface rule: absent sections must stay absent rather than locally invented | Observed behavior matches the no-invention requirement | ✅ |
| **Code/Fixture Basis** | Seeded artifacts must intentionally omit optional sections | Verified in code: the missing-sections fixture writes artifacts with transcript + summary only, while chapters, diarization, speakers, and provider-adjacent sections remain absent | ✅ |

### T3: Polisher Fallback / Provider-Led Raw Output

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | `SIM_Debug_Fallback.mp3` should render provider-led raw/lightly formatted output instead of blanking the card | User reported green on device: the fallback card rendered content rather than going blank | ✅ |
| **Contract Alignment** | Matches Core Flow Branch-S3: fallback keeps provider-led output visible and does not pretend a polished result exists | Observed behavior matches the fallback rule | ✅ |
| **Code/Fixture Basis** | Seeded artifacts must preserve raw/provider-adjacent content | Verified in code: the fallback fixture stores `[Provider Raw Transcript]` content plus raw provider-adjacent JSON instead of a polished summary structure | ✅ |

## 4. Supporting Evidence

* `:app-core:compileDebugKotlin` passed in the same closeout session.
* `SimAudioDebugScenarioTest` passed in the same closeout session.
* `UiSpecAlignmentTest` passed in the same closeout session after retrying with `--no-build-cache` due to a Gradle/Hilt cache-pack issue unrelated to source correctness.
* `:app-core:installDebug` completed successfully on the attached device before this L3 run.
* `adb` launch evidence confirmed SIM started via `com.smartsales.prism/.SimMainActivity`.

## 5. Deviation & Resolution Log

* **Expected confusion surfaced**: The user first looked for a top-level debug HUD button on the SIM home screen.
  * **Resolution**: This is not a product bug. The implementation intentionally keeps the Wave 2 debug aid local to the audio drawer browse mode and outside the global `L2DebugHud`.

* **Telemetry gap**: This focused negative-branch run does not currently emit dedicated SIM audio validation telemetry/log markers for each seeded branch.
  * **Impact**: L3 proof here is primarily UI-observed plus code/fixture-backed, rather than log-led.
  * **Resolution**: Accept for current Wave 2 closeout scope; do not claim dedicated telemetry evidence that does not exist.

## 6. Final Verdict

**✅ ACCEPTED (focused Wave 2 negative-branch L3 slice)**.

This device run closes the three previously hard-to-trigger Wave 2 branches:

* explicit transcription failure is green
* missing optional artifact sections remain absent instead of being invented
* polisher fallback / provider-led raw rendering is green

Residual note:

* this run was a focused debug-assisted negative-branch pass, not a fresh cold-start rerun of the entire Wave 2 lane
