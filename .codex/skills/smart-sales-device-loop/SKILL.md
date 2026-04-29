---
name: smart-sales-device-loop
description: Enforced device-loop evidence workflow for the Smart Sales repository. Use when Codex must debug, optimize, or verify Android/Harmony runtime behavior, UI/device/lifecycle/BLE/connectivity/notification/alarm/networking issues, scheduler drawer scenarios, adb logcat plus uiautomator evidence, platform-runtime/L2.5 synthetic ingress, physical L3 boundary claims, or any optimization claim that must stay in the loop until explicit exit criteria are met. Prefer debug buttons, debug panels, seeded actions, or other deterministic controls as the L2.5 route when they enter the same production boundary; keep physical L3 mandatory for authentic hardware, BLE scanner, GATT, firmware, power-state, and real-device emission claims.
---

# Smart Sales Device Loop

## Source Of Truth

Read these before operating the loop:

1. `docs/specs/device-loop-protocol.md`
2. Relevant sprint contract under `docs/projects/<project>/sprints/`
3. Relevant Core Flow doc under `docs/core-flow/` when one exists
4. `docs/sops/debugging.md` when the loop is part of a bug diagnosis

The protocol is mandatory for runtime claims. Unit tests, screenshots, and agent narration are supporting evidence only.

L2 is useful while building or repairing incomplete business logic. Once an implementation path exists and is only awaiting runtime confidence, L2 fakes are regression support only; they must not close installed UI, lifecycle, BLE/connectivity, notification, alarm, networking, background-work, or device-integration claims. Prefer L2.5 for app-side installed-debug-APK closure when deterministic ingress can enter the production boundary, and physical L3 for authentic hardware/source truth.

## Core Rule

Run one exact scenario per loop. Do not mix hypotheses, gestures, or user journeys in the same capture window.

If a device/emulator is unavailable, mark the runtime gate blocked. Do not call the behavior verified.

If the first loop reveals a failure, write the Pre-Fix Report from `docs/sops/debugging.md` before behavior edits. If logs are insufficient, add the smallest targeted diagnostic logging and rerun the same loop before changing behavior.

Do not exit the loop because the implementation "looks right". Exit only when the sprint contract or active debugging task has explicit criteria and the latest loop meets them with saved artifacts. If criteria are absent or vague, define concrete criteria before claiming completion.

## Evidence Class Selection

Declare the evidence class before the loop starts:

- `platform-runtime/L2.5`: default for deterministic installed-debug-APK app-side dataflow verification when a debug control or fixture ingress exists.
- physical `L3`: required for authentic hardware, BLE scanner, GATT, firmware, power-state, real-device emission, or any claim that the physical upstream source itself behaved correctly.

Use L2.5 only when all of these hold:

- the injected input enters the same internal boundary as the real signal
- the scenario uses fixed fixtures or proves the required pre-state
- logcat includes `[L2.5][BEGIN]`, `[L2.5][ASSERT]`, and `[L2.5][END] result=PASS`
- UI XML, telemetry, or saved debug-state evidence proves debug gating when the control's availability is part of the claim

L2.5 can close app-side dataflow uncertainty, but it is not physical evidence. Do not report L2.5 as proof of BLE discovery, GATT exchange, firmware emission, badge power state, or real-device emission.

## Physical L3 Manual Collaboration

For physical L3, do not leave human or hardware choreography implicit. Before the loop starts, declare each manual collaboration item in the working notes or sprint ledger:

- human action owner
- exact physical device or badge identity
- action to perform
- timing relative to `adb logcat -c` or `hdc shell hilog -r`
- expected app telemetry, UI state, or hardware observation
- pass/fail/block condition

If the L3 branch requires a person to power a badge on or off, move it in range, start advertising, tap a card, pair a device, disconnect networking, or create a dual-device state, that action must be an explicit test item. If the item is not performed or cannot be confirmed, mark the L3 branch blocked rather than inferred.

## Exit Criteria

Before running the loop, write down the exit criteria in the working notes or sprint ledger:

- declared evidence class: `platform-runtime/L2.5` or physical `L3`
- physical L3 manual collaboration items when hardware or human action is needed
- exact scenario or debug action sequence
- required positive telemetry
- required UI state in `uiautomator` XML
- required negative logs/UI states
- required L1/L2 regression tests after any code or diagnostic-log change
- artifact paths to be saved

Keep iterating until one of these is true:

- **Pass**: latest artifacts satisfy every positive, UI, and negative criterion.
- **Blocked**: device/emulator, install, permissions, account state, deterministic ingress, or hardware dependency prevents the declared evidence class; record the blocker and lower confidence.
- **Stop**: sprint stop criteria or iteration bound is hit; record the failed criterion and next required decision.

## Debug Controls First

Prefer in-app debug buttons, debug panels, seeded scenarios, launch extras, fixture import actions, or other deterministic controls when they exercise the same production path as the user action or upstream signal. These controls are not merely convenience helpers: when they faithfully enter the production boundary, they are the preferred `platform-runtime/L2.5` route for app-side dataflow verification.

Use debug controls to replace high-friction input such as repeated typing, voice recording, BLE badge actions, or long setup sequences only when the control preserves the route under test. The evidence must show the simulated source explicitly, for example `source=scheduler_debug_button`.

Do not use a debug control if it bypasses the layer being tested. If a debug shortcut changes the behavioral path, treat that evidence as invalid and move the control closer to the real ingress before continuing.

## Android Loop

Use this shape unless the sprint contract declares a narrower command set:

1. Build the exact debug artifact.

   ```bash
   ./gradlew :app-core:assembleDebug
   ```

2. Install the exact APK.

   ```bash
   adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk
   ```

3. Clear app data unless the sprint explicitly requires preserved state.

   ```bash
   adb shell pm clear com.smartsales.prism
   ```

4. Establish the known entry state: launch app, complete or bypass onboarding if required by the sprint, enter the target screen/drawer, and confirm the start UI with `uiautomator dump`.

   Permission dialogs, OEM guidance, and app-level sheets are part of entry-state setup. If a dialog covers scenario taps, record that as a failed loop observation, dismiss it deliberately, and rerun or continue with the contamination noted in the ledger.

5. Clear logcat immediately before the scenario.

   ```bash
   adb logcat -c
   ```

6. Run one declared scenario according to the evidence class. For `platform-runtime/L2.5`, trigger the deterministic debug ingress that enters the production boundary. For physical `L3`, perform only the declared real gesture, hardware action, BLE scanner/GATT path, firmware emission, or device-state transition required by the claim, and record whether each manual collaboration item was performed.

7. Save filtered logcat, UI XML, and optional screenshot under:

   ```text
   docs/projects/<project>/evidence/<sprint>/
   ```

   Recommended naming:

   ```text
   run-01-baseline-logcat.txt
   run-01-baseline-ui.xml
   run-01-baseline.png
   run-02-after-fix-logcat.txt
   run-02-after-fix-ui.xml
   run-02-after-fix.png
   ```

8. Evaluate:

   - declared evidence class was actually exercised
   - physical L3 manual collaboration items were performed or explicitly blocked
   - expected telemetry is present
   - L2.5 telemetry includes `[L2.5][BEGIN]`, `[L2.5][ASSERT]`, and `[L2.5][END] result=PASS` when the declared class is `platform-runtime/L2.5`
   - expected UI state is present in UI XML
   - forbidden logs and UI states are absent

9. Append a concise sprint-ledger verdict with commands, artifact paths, pass/fail, and any contamination such as permission dialogs.

10. If any exit criterion fails, continue the loop: diagnose from the measured artifact, apply only the minimal diagnostic or behavior change, run focused L1/L2 regression checks, reinstall or cold relaunch as appropriate, and rerun the same scenario with the declared evidence class.

## Scheduler Time-Anchor Retitle Loop

Use these exact scheduler drawer debug inputs in order:

1. `明天早上八点我要赶飞机`
2. `明早八点应该是要去开会`
3. `不对，明早八点应该是去机场接人`

Required positive evidence:

- `source=scheduler_debug_button`
- `SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY`
- `Task Rescheduled (Room)`
- final UI XML contains `去机场接人` and `08:00`

Required negative evidence:

- no `SimBadgeFollowUpChat`
- no `SIM badge scheduler follow-up action completed`
- no `action=retitle`
- no chat follow-up action panel in UI XML

Recommended checks:

```bash
rg -n "source=scheduler_debug_button|SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY|Task Rescheduled \\(Room\\)" \
  docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/*logcat*.txt
rg -n "SimBadgeFollowUpChat|SIM badge scheduler follow-up action completed|action=retitle" \
  docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/*logcat*.txt
rg -n "去机场接人|08:00|follow-up|retitle" \
  docs/projects/scheduler-major-update/evidence/01-time-anchor-retitle/*ui*.xml
```

The second command must return no matches.

## Optimization Rule

Do not optimize from intuition. Start each optimization iteration from a measured loop observation: wrong routing, slow response, bad UI state, missing card update, duplicate task, noisy logs, or insufficient telemetry.

Every optimization iteration closes with:

- focused L1/L2 regression tests for the touched area
- reinstall or cold relaunch as appropriate
- fresh `platform-runtime/L2.5` or physical `L3` evidence from the same scenario according to the declared evidence class
- sprint ledger update with artifact paths

If the optimization is about speed or friction, the loop must include the measurement or UI-state observation that proves the improvement, not just a subjective claim.

## Harmony Variant

For Harmony claims, keep the same loop shape and replace Android commands with the lane's declared `hdc`/DevEco commands. `hdc shell hilog` is the minimum runtime log proof.
