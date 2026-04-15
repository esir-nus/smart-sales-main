# L3 On-Device Test Record: Onboarding Quick-Start Session Recovery

**Date**: 2026-04-03
**Tester**: Agent + user
**Target Build**: `:app-core:assembleDebug`

---

## 1. Test Context & Entry State

* **Objective**: Validate the production onboarding `SCHEDULER_QUICK_START` mic path on physical device after the recognizer-timeout recovery fix, against `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, `docs/specs/flows/OnboardingFlow.md`, and `docs/cerb/onboarding-interaction/spec.md`.
* **Testing Medium**: L3 physical-device test with `adb logcat` monitoring on connected device `2410DPN6CC`.
* **Initial Device State**: SIM connectivity onboarding path active on-device. Earlier in-session logs showed a profile-lane recognizer timeout at `19:48:04`, which was the concrete precondition for the previously reported “mic button looks dead” regression.

## 2. Execution Plan

* **Trigger Action**:
  * reproduce the earlier stuck-session condition by reaching onboarding profile recognition timeout
  * retest the quick-start mic button after the timeout
  * run one successful quick-start create utterance and subsequent update/reschedule utterances
* **Input Payload**:
  * create batch: `明天早上7点叫我起床，9点要带合同去见老板`
  * later quick-start follow-up utterances that exercised update/create fallback paths on staged items

## 3. Expected vs Actual Results

### T1: Recognizer Timeout Must Not Leave Quick-Start Mic Dead

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | After an onboarding recognizer timeout, the quick-start mic must still react to a fresh hold instead of behaving like a dead button | User reported the button had previously looked dead; after the patch and reinstall, fresh presses generated new realtime start logs and moved the lane forward again | ✅ |
| **Telemetry (GPS)** | Ideally the sandbox would emit dedicated `VALVE_PROTOCOL` checkpoints for quick-start voice ingress and state emission | No dedicated `VALVE_PROTOCOL` checkpoints were emitted for the onboarding quick-start sandbox path; proof relied on feature-local logs instead | ❌ |
| **Log Evidence** | A fresh press after the prior timeout must restart realtime listening rather than being blocked by a stale listening session | At `19:50:45.951` the device emitted `DeviceSpeechRecognizer start_listening mode=FUN_ASR_REALTIME`, followed by `start_realtime_session`, `FunAsrRealtime start_requested`, and `realtime_event type=listening_started` | ✅ |
| **Negative Check** | The lane must not remain permanently blocked by stale `isListening()` state after the earlier timeout | After the earlier profile timeout at `19:48:04`, later quick-start attempts reached fresh start logs repeatedly; the mic was not stuck behind the old session anymore | ✅ |

### T2: Quick-Start Live Parsing and Staged Mutation Paths

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | A valid quick-start utterance should stage scheduler items instead of falling into dead/no-op behavior | Logs showed quick-start success with staged item count growth and later update success against staged items; user also reported the mic path now seemed to work | ✅ |
| **Telemetry (GPS)** | The quick-start sandbox still lacks dedicated valve checkpoints, but the route should remain observable through feature-local routing logs | No `VALVE_PROTOCOL` proof exists for this sandbox route; local routing logs still showed create and reschedule ownership decisions with concrete durations | ❌ |
| **Create Path Evidence** | Chained explicit-clock utterance should enter quick-start processing and succeed through the shared Path A create family | At `19:49:13.560` the lane entered `processing_start ... lane=quick_start`; at `19:49:13.887` quick start logged `quick_start_apply_begin`; at `19:49:13.896` it logged `route_decision intent=CREATE shape=BATCH owner=UNI_M` and `create_route stage=DETERMINISTIC_CHAINED_DAY_CLOCK`; at `19:49:13.897` onboarding logged `quick_start_apply_success ... items=2 touchedExact=true mutation=create` | ✅ |
| **Fallback Evidence** | Bounded onboarding-local `Uni-M` timeout must fall through to later shared lanes instead of stalling the quick-start request | At `19:49:34.029` quick start began a new request; at `19:49:37.665` it logged `create_route stage=UNI_B uniM=TIMEOUT`; at `19:49:37.666` onboarding logged `quick_start_apply_success ... mutation=create` after about `3.6s`, proving the fallback remained live | ✅ |
| **Reschedule / Update Evidence** | Staged-item follow-up utterances should reach the sandbox reschedule path and update staged items instead of forcing persisted-task flows | At `19:50:48.583` and again at `19:50:55.491`, quick start logged `route_decision intent=RESCHEDULE shape=TARGETED_UPDATE owner=GLOBAL_RESCHEDULE`; both runs ended in `quick_start_apply_success ... mutation=update` | ✅ |
| **Negative Check** | Quick-start should not regress back into the dead-button state after intermediate recognizer failures | Later retries included several realtime start failures (`240080`, `240007`) surfaced as `recording_failed`, but the lane remained retryable and later recovered into successful quick-start processing at `19:50:48` and `19:50:55` | ✅ |

## 4. Deviation & Resolution Log

* **Attempt 1 Failure**: physical-device repro initially presented as “the mic button does nothing” on the quick-start page after onboarding profile timeout.
  * **Root Cause**: the onboarding ViewModel surfaced recognizer timeout/exception failure UI without cancelling the underlying `DeviceSpeechRecognizer` session, leaving `speechRecognizer.isListening()` true and causing later quick-start `startQuickStartRecording()` calls to return early.
  * **Resolution**: `OnboardingInteractionViewModel` now calls `speechRecognizer.cancelListening()` on recognizer timeout and recognizer exception before surfacing failure state. Focused unit coverage was added and passed before reinstalling the device build.

## 5. Final Verdict

**Accepted for the stuck-session recovery slice; outer quick-start watchdog proof remains incomplete.**

This device run gives concrete L3 evidence that:

* a prior onboarding recognizer timeout no longer leaves the quick-start mic permanently dead
* quick-start can restart realtime listening after the timeout condition
* the shared Path A quick-start create path works on device for chained explicit-clock input
* onboarding-local `Uni-M` timeout still falls through to later shared lanes instead of stalling the request
* staged-item reschedule/update routing works through the onboarding sandbox on device

Residual gaps remain:

* the onboarding quick-start sandbox still does not emit dedicated `VALVE_PROTOCOL` checkpoints, so telemetry proof is weaker than the repo's preferred GPS standard for this path
* this run did not force the full ~`10s` outer quick-start watchdog branch, so the exact timeout surface `当前日程整理暂时没有返回，请再试一次。` remains unverified at L3
