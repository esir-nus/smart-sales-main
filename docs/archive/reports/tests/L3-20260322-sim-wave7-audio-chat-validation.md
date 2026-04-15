# L3 On-Device Test Record: SIM Wave 7 Audio Chat Validation

**Date**: 2026-03-22
**Tester**: Codex
**Target Build**: `:app-core:installDebug`

---

## 1. Test Context & Entry State
* **Objective**: Validate the post-T6.3 positive-path SIM audio/chat route on device: browse-mode artifact open, `Ask AI` continuation, grounded chat entry, and chat-side audio reselect surfacing already-transcribed reuse without a visible rerun state.
* **Testing Medium**: L3 Device Validation on physical phone via `adb`
* **Initial Device State**: Fresh `:app-core:installDebug` build installed on the connected device. Calendar permission was granted. Because raw `adb` taps against live transcription startup were flaky on this MIUI build, this run seeded two SIM-namespaced persisted transcribed cards directly into SIM app storage before launch, then validated the already-transcribed browse/reuse route on device.

## 2. Source of Truth

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/plans/sim-tracker.md`

## 3. Execution Plan

1. Relaunch `SimMainActivity`
2. Open browse-mode Audio Drawer from chat
3. Confirm persisted transcribed inventory is visible
4. Open one transcribed card and verify informational artifact sections render
5. Tap `Ask AI` and confirm grounded-chat entry
6. Reopen the drawer from chat via attach and verify the chat-side reselect surface exposes an already-transcribed reuse action without a pending/progress rerun state

## 4. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **Browse drawer entry** | Chat should reopen the SIM browse drawer rather than a phone file picker. | Stable swipe-up from SIM chat opened a drawer titled `录音文件`. | ✅ |
| **Transcribed inventory** | Already-transcribed cards should be visible as reusable persisted items. | `SIM_Wave2_Seed.mp3` and `SIM_Wave2_Pending_A.mp3` both surfaced with status `已转写`. | ✅ |
| **Informational artifact surface** | Opening a transcribed card should render source-led artifacts, not a fake placeholder. | Expanding `SIM_Wave2_Seed.mp3` rendered `转写`, `摘要`, `重点`, `章节`, `说话人`, `结果链接`, plus `Ask AI`. The transcript body and summary text were visible on device. | ✅ |
| **Persisted-artifact telemetry** | Opening a persisted transcribed card should emit the SIM artifact-opened route summary. | Logcat recorded `SIM audio persisted artifact opened` with `audioId=sim_wave2_seed title=SIM_Wave2_Seed.mp3`. | ✅ |
| **Ask AI continuation** | `Ask AI` should open grounded chat bound to the selected audio. | After tapping `Ask AI`, chat title became `SIM_Wave2_Seed.mp3`, the banner said `已进入《SIM_Wave2_Seed.mp3》的讨论模式`, and logcat recorded `SIM audio grounded chat opened from artifact`. | ✅ |
| **Chat-side reselect surface** | Attach from grounded chat should reopen the audio drawer in chat-reselect mode. | Attach opened a drawer titled `选择要讨论的录音`. | ✅ |
| **Already-transcribed reuse surface** | Chat-side reselect should expose already-transcribed reuse without progress/rerun affordances. | The current audio showed disabled `当前讨论音频`; `SIM_Wave2_Pending_A.mp3` showed status `已转写` and CTA `用于当前聊天`; no `转写中`, progress bar, or pending-state CTA was visible for these transcribed reuse candidates. | ✅ |

## 5. Deviation & Resolution Log

* **Observation**: Direct raw-`adb` taps against live transcription start and the final secondary reselect action were flaky on this MIUI launcher build and could occasionally spill to the launcher instead of staying inside SIM.
  - **Impact**: This report uses seeded SIM-owned persisted artifacts for the positive-path browse/chat reuse route and does not claim a fresh live Tingwu run.
  - **Acceptance Rule**: The accepted evidence in this report is limited to the already-transcribed informational/reuse path defined by the core flow.

## 6. Final Verdict

**✅ ACCEPTED for the Wave 7 audio/chat positive-path acceptance slice.**

This run proves, on device, that the current post-T6.3 SIM build can:

- surface already-transcribed SIM-owned audio in browse mode
- open a source-led informational artifact card
- continue into grounded chat through `Ask AI`
- reopen the drawer from chat for audio reselection
- present already-transcribed chat-side reuse without exposing a visible rerun/progress branch

This report does **not** replace earlier negative-branch validation or claim a fresh live transcription run. It closes the focused T7.1 positive-path audio/chat gap under the current runtime.
