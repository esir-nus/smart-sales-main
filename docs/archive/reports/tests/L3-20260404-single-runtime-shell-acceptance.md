# L3 On-Device Test Record: Single Runtime Shell Acceptance

**Date**: 2026-04-04
**Tester**: Agent
**Target Build**: `:app-core:installDebug`

---

## 1. Test Context & Entry State

* **Objective**: Validate the approved single-runtime shell cleanup on physical device against `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, `docs/specs/base-runtime-unification.md`, `docs/specs/flows/OnboardingFlow.md`, and `docs/cerb-ui/dynamic-island/spec.md`.
* **Testing Medium**: L3 physical-device pass with `adb logcat`, `adb dumpsys activity`, and `uiautomator dump` on connected device `2410DPN6CC` (`fc8ede3e`).
* **Installed Build**: fresh `:app-core:installDebug` deploy to package `com.smartsales.prism`.
* **Initial Device State**: onboarding gate was already complete on-device, so the expected cold-launch branch was direct entry into the unified production shell rather than forced onboarding.

## 2. Execution Plan

* clear `adb logcat`
* install current debug build
* force-stop and cold-launch `com.smartsales.prism/.MainActivity`
* confirm the resumed production activity on-device
* validate the top-level unified shell surface
* validate scheduler entry/exit from the dynamic-island lane
* validate audio drawer open -> select -> chat handoff -> audio reselect from chat
* inspect whether any run rendered overlapping scheduler/audio drawers

## 3. Expected vs Actual Results

### T1: Single Production Root Must Be `MainActivity`

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Cold launch root** | Production cold launch should enter only `com.smartsales.prism/.MainActivity` | `adb logcat` captured `START u0 ... cmp=com.smartsales.prism/.MainActivity` at `16:33:57.980` and `Displayed com.smartsales.prism/.MainActivity ... +2s784ms` at `16:34:00.761` | ✅ |
| **Top resumed activity** | The active resumed production activity should be `MainActivity`, not split-era roots | `adb shell dumpsys activity activities` showed `topResumedActivity=ActivityRecord{... com.smartsales.prism/.MainActivity ...}` and `ResumedActivity: ActivityRecord{... com.smartsales.prism/.MainActivity ...}` | ✅ |
| **Negative check** | No live `AgentMainActivity` / `SimMainActivity` ownership should appear in the active task | The live activity dump only exposed `com.smartsales.prism/.MainActivity` as the resumed top activity during the acceptance run | ✅ |

### T2: Completed-Gate Cold Launch Must Land in the Unified Home Shell

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Onboarding gate branch** | With onboarding already completed, cold launch should bypass onboarding and mount the shell home/chat surface | The first `uiautomator dump` after cold launch showed home-shell elements instead of onboarding: greeting `你好, Frank`, subtitle `我是您的销售助手`, a dynamic-island task summary, and the bottom composer family | ✅ |
| **Shell continuity** | Shared shell chrome should remain present around the home surface | The same dump showed the persistent top row with menu, dynamic island, and new-session affordance plus the bottom composer family | ✅ |
| **Negative check** | Cold launch must not silently route into the old split production shell model | No alternate production activity was observed; the visible surface stayed inside the `MainActivity` task and matched the shared shell contract | ✅ |

### T3: Scheduler Entry and Exit Must Stay in the Same Unified Shell Lane

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Visible-lane scheduler entry** | The visible scheduler lane in the dynamic island should open the scheduler drawer | After tapping the dynamic island, the next dump showed scheduler content in-place: month header `2026年 4月`, weekday strip, timeline items such as `叫我起床`, `去拿快递`, and `去机场接人` | ✅ |
| **Downward drag path** | Downward drag on the island should also be scheduler-only and lawful | A separate island swipe also opened the scheduler drawer and exposed the scheduler calendar/timeline surface | ✅ |
| **Close path** | Closing the scheduler should return to the shell home/chat frame, not another runtime | Tapping the bottom scheduler handle returned to the home shell, with the greeting and composer family visible again | ✅ |

### T4: Audio Drawer Must Open, Bind Chat, and Reselect Through the Drawer

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Audio drawer open** | Tapping the attach entry should open the shell audio drawer instead of leaving the app | The drawer opened with title `选择要讨论的录音`, helper copy `点击录音卡片切换当前聊天`, and the `SIM_Wave2_Seed.mp3` card | ✅ |
| **Select to chat** | Choosing audio should bind discussion context and return to the current shell chat session | Tapping `SIM_Wave2_Seed.mp3` returned to chat and rendered a long AI response in the same shell, while the bottom composer now showed the normal drafting hint `输入消息...` | ✅ |
| **Audio reselect from chat** | Attach-from-chat must reopen the audio drawer instead of defaulting to Android file management | Tapping attach again reopened the audio drawer. The selected card was shown with `当前讨论中` and the current card was disabled, proving reselect returned to the drawer route rather than launching a file picker | ✅ |
| **Negative check** | Audio re-entry must not escape to Android file management for this chat flow | No external picker or package switch appeared during the open/select/reselect sequence | ✅ |

### T5: One-Drawer-at-a-Time Invariant

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Rendered state** | Scheduler and audio should not render as overlapping major drawers | Scheduler-open dumps showed only scheduler drawer content; audio-open dumps showed only audio drawer content; chat/home dumps showed neither major drawer | ✅ |
| **Conflict branch proof** | Ideally the run should force a second-drawer trigger while the first drawer is already active | This pass did not complete a clean direct conflict-trigger repro between already-open scheduler and already-open audio; evidence is limited to observed non-overlapping rendered states across separate drawer transitions | ⚠️ |

### T6: Forced Onboarding Gate and Post-Onboarding Handoff

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Forced first-launch branch** | When onboarding gates are forced incomplete, cold launch should surface onboarding instead of the normal home shell | After writing the onboarding gate prefs to `completed=false` and relaunching `MainActivity`, `uiautomator dump` showed the onboarding welcome copy `SmartSales`, `您的 AI 销售教练`, and CTA `开启旅程` over the runtime | ✅ |
| **Back-block invariant** | Forced first-launch onboarding must not be dismissed by a simple back press | After `KEYCODE_BACK`, the onboarding welcome UI remained visible in the follow-up dump rather than dropping into home | ✅ |
| **Post-onboarding scheduler handoff** | Successful onboarding completion should arm a one-shot scheduler auto-open when returning home | A later organic onboarding rerun kept `MainActivity` as the top resumed activity, left `base_runtime_onboarding_gate.xml` at `completed=true`, consumed `base_runtime_onboarding_handoff_gate.xml` back to `false`, and the immediate `uiautomator dump` showed the scheduler drawer open with `2026年 4月` and timeline content instead of the plain home shell | ✅ |

### T7: Connectivity Takeover and Routing from the Visible Island Lane

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Setup escalation path** | A no-session reconnect path should enter the connectivity needs-setup branch instead of silently staying on scheduler | After restoring the shell and clearing `ble_session_store.xml`, the audio browse drawer showed blocked connectivity copy `Badge 未连接`; opening the connectivity modal and tapping `重试连接` produced `ConnectivityService ... current state: NeedsSetup` at `17:06:04.836`, and the next dump showed `设备未配网` with CTA `开始配网` | ✅ |
| **Visible-lane takeover** | Once the runtime reaches needs-setup, the dynamic island should switch from scheduler to the connectivity lane | After closing the modal, the next home dump showed the island text `Badge 需要配网` in the shared center slot instead of the scheduler summary | ✅ |
| **Visible-lane tap routing** | Tapping the connectivity-visible island must reopen connectivity entry instead of the scheduler drawer | Tapping the `Badge 需要配网` island reopened the needs-setup connectivity modal with `设备未配网` and `开始配网` | ✅ |
| **Downward drag routing** | Downward drag from the connectivity-visible island must remain scheduler-only | After closing the modal and swiping downward from the `Badge 需要配网` island area, the next dump showed the scheduler drawer (`2026年 4月` and task timeline items) rather than another connectivity surface | ✅ |

## 4. Deviations & Limits

* **Connectivity branch required state forcing**: the connectivity takeover branch was reproduced on device by clearing `ble_session_store.xml` and using the real audio-drawer -> connectivity -> reconnect route to drive `NeedsSetup`.
* **Drawer conflict branch only partially covered**: the one-drawer-at-a-time invariant is supported by observed rendered states, but the strict “open second drawer while first is active” conflict branch was not fully reproduced during this pass.

## 5. Final Verdict

**Conditionally accepted for the single-runtime shared-surface cleanup slice.**

Concrete L3 evidence now shows that:

* the live production root is the single `MainActivity` path
* a completed-gate cold launch lands in the unified shell home surface rather than split-era production hosts
* scheduler entry/exit stays inside that same shell lane
* audio open/select/reselect stays drawer-based and returns through the shell instead of escaping to Android file management
* forced first-launch onboarding still blocks back dismissal, and organic onboarding completion now proves the one-shot scheduler auto-open handoff in the live runtime
* the connectivity needs-setup branch can take over the dynamic island, visible-lane tap reopens connectivity entry, and downward drag remains scheduler-only
* no overlapping scheduler/audio drawer render was observed during the exercised transitions

Residual L3 gaps remain:

* the explicit scheduler-vs-audio second-drawer conflict branch still needs a cleaner direct repro if that invariant becomes release-critical for this slice
