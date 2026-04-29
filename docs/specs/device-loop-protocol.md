# Deterministic Device-Loop Protocol

> **Purpose**: Reusable runtime evidence loop for Android and Harmony device claims.
> **Status**: Active spec
> **Last Updated**: 2026-04-29
> **Repository Guide**: [`docs/AGENTS.md`](../AGENTS.md)
> **Related SOP**: [`docs/sops/debugging.md`](../sops/debugging.md)

---

## Rule

Runtime, UI, scheduler, lifecycle, background-work, BLE/connectivity,
notification, alarm, networking, and device-integration claims require fresh
device-loop evidence.

Screenshots, screen recordings, agent narration, and unit tests can support a
runtime claim, but they do not replace the loop. Compile success is not runtime
proof.

If a device or emulator is unavailable, record the runtime gate as blocked. Do
not downgrade the requirement to "verified".

## L2 Role

L2 is a deterministic business-logic and dataflow development scaffold. Use it
while a behavior path is being designed or repaired, when the business rules are
not yet complete enough for installed runtime evidence. L2 may define a
parallel universe, seed fixed state, inject fixed input, and assert DTO,
repository, domain, or UI-state outputs with fakes.

After the implementation path exists and the remaining question is runtime
confidence, L2 becomes regression support only. It must not close an awaiting-
testing state for installed UI, lifecycle, background work, BLE/connectivity,
notification, alarm, networking, or device-integration claims. At that point,
use L2.5 for app-side installed-debug-APK dataflow closure when deterministic
ingress exists, and use physical L3 for authentic hardware or upstream-source
truth.

## Evidence Folder

Save artifacts under the active sprint:

```text
docs/projects/<project>/evidence/<sprint>/
```

Use stable run names so iterations can be compared:

```text
run-01-baseline-logcat.txt
run-01-baseline-ui.xml
run-01-baseline.png
run-02-after-fix-logcat.txt
run-02-after-fix-ui.xml
run-02-after-fix.png
```

If an existing sprint already declares narrower names, keep the sprint's names
and preserve the same baseline/after-fix distinction in the ledger.

## L2.5 Synthetic Ingress

L2.5 is the deterministic device-installed synthetic ingress class. It is
allowed when a debug control enters the same app boundary as the real upstream
signal, uses fixed fixtures, emits scenario IDs and assertion telemetry, and is
captured with the Android loop on an installed debug APK.

Rules:

- the debug control must be debug-build gated
- the scenario must seed a fixed pre-state or prove the required pre-state
- the injected input must enter the same internal boundary as the real signal
- logcat must include `[L2.5][BEGIN]`, `[L2.5][ASSERT]`, and `[L2.5][END]`
- `[L2.5][END]` must state `result=PASS` before the branch can be counted
- UI XML or other state evidence must prove the control was available only in
  debug mode when UI gating is part of the claim
- L2.5 may close app-side dataflow uncertainty, but it must not be reported as
  authentic physical hardware evidence

L2.5 is intentionally close to L3 dataflow fidelity, but it is not L3. For BLE
or firmware claims, physical scanner, GATT, firmware emission, and power-state
evidence remain L3-only.

## Physical L3 Manual Collaboration

Physical L3 tests that depend on real-world hardware state or human action must
declare the manual collaboration items before the capture window starts.

Each manual collaboration item must name:

- the human action owner
- the exact physical device or badge identity
- the action to perform
- the timing relative to `adb logcat -c` / `hdc shell hilog -r`
- the expected app telemetry, UI state, or hardware observation
- the pass/fail/block condition

Do not leave hardware choreography implicit. If a human must power a badge on or
off, move it in range, start advertising, tap a card, pair a device, disconnect
networking, or create a dual-device state, write that item in the sprint ledger
or working notes before running the loop. If the item is not performed or cannot
be confirmed, the L3 branch is blocked, not passed.

## Android Loop

Run one exact scenario per loop. Do not mix multiple hypotheses in the same
capture window.

1. Build the exact debug artifact for the lane.

   ```bash
   ./gradlew :app-core:assembleDebug
   ```

2. Install the exact APK that was built.

   ```bash
   adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk
   ```

3. Clear app data unless the sprint requires a preserved account, pairing, or
   badge state. If state is preserved, write the reason in the sprint ledger.

   ```bash
   adb shell pm clear com.smartsales.prism
   ```

4. Set the known entry state: launch the app, enter the required drawer/screen,
   seed required debug data, and confirm the visible start state.

   ```bash
   adb shell am start -n com.smartsales.prism/.MainActivity
   ```

   If onboarding gates, Android permission dialogs, OEM permission guidance, or
   app-level permission sheets are expected in a fresh install, handle them as
   part of this entry-state setup and write the exact action in the sprint
   ledger. Do not let a permission dialog cover later scenario taps without
   recording it as a failed loop observation.

5. Clear logcat immediately before the scenario.

   ```bash
   adb logcat -c
   ```

6. Run exactly one declared user scenario. For physical L3 that needs human or
   hardware collaboration, execute only the declared manual collaboration items
   for that scenario and record whether each item was performed.

7. Capture filtered logcat.

   ```bash
   adb logcat -d -v time -s VALVE_PROTOCOL:I SimSchedulerViewModel:D SimSchedulerIngress:D SimBadgeFollowUpChat:D '*:S' \
     > docs/projects/<project>/evidence/<sprint>/run-01-baseline-logcat.txt
   ```

8. Capture UI state through `uiautomator dump`; screenshot is optional
   supporting evidence.

   ```bash
   adb shell uiautomator dump /sdcard/window.xml
   adb pull /sdcard/window.xml docs/projects/<project>/evidence/<sprint>/run-01-baseline-ui.xml
   adb exec-out screencap -p > docs/projects/<project>/evidence/<sprint>/run-01-baseline.png
   ```

9. Evaluate the loop against three evidence checks:

   - expected telemetry is present
   - expected UI state is present in the UI dump
   - negative logs and forbidden UI states are absent

10. Record a short verdict in the sprint ledger with artifact paths.

## Failure Loop

If the baseline loop shows a failure:

1. Write the Pre-Fix Report required by [`docs/sops/debugging.md`](../sops/debugging.md).
2. If logs are insufficient, add the smallest targeted diagnostic log and rerun
   the same loop before changing behavior.
3. Apply the minimal fix only after the failure branch is proven.
4. Run focused L1/L2 regression tests for the touched area. These tests support
   the fix but do not replace L2.5 or L3 runtime evidence after implementation
   exists.
5. Rebuild, reinstall or cold relaunch as appropriate, and repeat the same L3
   scenario.
6. Save after-fix artifacts with the next run number.
7. Update the sprint ledger with pass/fail and artifact paths.

Do not optimize from intuition. Every optimization iteration must start from a
measured device-loop observation: wrong routing, slow response, bad UI state,
missing card update, duplicate task, or noisy/insufficient telemetry.

## Scheduler Time-Anchor Retitle Loop

For the scheduler drawer debug-button loop, use these exact inputs in order:

1. `明天早上八点我要赶飞机`
2. `明早八点应该是要去开会`
3. `不对，明早八点应该是去机场接人`

Required positive evidence:

- `source=scheduler_debug_button`
- `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY`
- `Task Rescheduled (Room)`
- final UI dump contains `去机场接人` and `08:00`

Required negative evidence:

- no `SimBadgeFollowUpChat`
- no `SIM badge scheduler follow-up action completed`
- no `action=retitle`
- no chat follow-up action panel in the UI dump

Recommended grep checks:

```bash
rg -n "source=scheduler_debug_button|SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY|Task Rescheduled \\(Room\\)" \
  docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/*logcat*.txt
rg -n "SimBadgeFollowUpChat|SIM badge scheduler follow-up action completed|action=retitle" \
  docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/*logcat*.txt
rg -n "去机场接人|08:00|follow-up|retitle" \
  docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/*ui*.xml
```

The second command must return no matches. The UI grep must prove the final
scheduler state and no chat follow-up action panel.

## Harmony Note

For Harmony device claims, use the same loop shape but replace Android install,
launch, log, and UI-capture commands with the lane's declared `hdc`/DevEco
commands. `hdc shell hilog` is the runtime log minimum bar.

## See Also

- [`docs/specs/harness-manifesto.md`](harness-manifesto.md) — evidence-class contract.
- [`docs/specs/project-structure.md`](project-structure.md) — sprint evidence layout.
- [`docs/specs/sprint-contract.md`](sprint-contract.md) — ledger and closeout evidence rules.
