# Onboarding Flow

> Type: Flow
> Status: Active
> Last Updated: 2026-04-03
> UX Surface Authority Above This Doc: [`docs/core-flow/base-runtime-ux-surface-governance-flow.md`](../../core-flow/base-runtime-ux-surface-governance-flow.md) (`UX.ONBOARDING.*`)

## Overview

The production onboarding flow now follows one canonical sequence across cold-start product onboarding, connectivity setup replay, and forced first-launch setup entry.

The same Compose coordinator owns all production entry points. Remaining host toggles are preview/capture debt only; they are no longer allowed to change production routing or completion behavior.
Production completion always returns through the same `MainActivity -> RuntimeShell` handoff path; legacy wrapper hosts are not lawful onboarding completion owners.

Behavior authority for the pairing runtime remains:

- `docs/cerb/device-pairing/spec.md`
- `docs/cerb/device-pairing/interface.md`
- `docs/cerb/connectivity-bridge/spec.md` and `interface.md` for shared connectivity runtime behavior

## Production Sequence

1. `WELCOME`
2. `PERMISSIONS_PRIMER`
3. `VOICE_HANDSHAKE_CONSULTATION`
4. `VOICE_HANDSHAKE_PROFILE`
5. `HARDWARE_WAKE`
6. `SCAN`
7. `DEVICE_FOUND`
8. `PROVISIONING`
9. `SCHEDULER_QUICK_START`
10. `COMPLETE`

All production entry points now reuse the same calm intro prefix, pairing lane, quick-start sandbox, and home-first completion.

## Wave Intent

### Wave 1: Tone Foundation

- `WELCOME` presents the calm premium introduction with `您的 AI 销售教练`
- `PERMISSIONS_PRIMER` explains microphone, Bluetooth, and exact-alarm needs before any native prompt appears

Rule:

- the primer is informational first
- Android permission prompts still happen at point-of-use
- if the first consultation/profile mic tap triggers `RECORD_AUDIO`, granting permission should return onboarding to idle with a calm guidance hint and require a fresh tap

### Wave 2: Interactive Handshake

- `VOICE_HANDSHAKE_CONSULTATION` is a real two-turn phone-mic consultation, not a fake static placeholder
- `VOICE_HANDSHAKE_PROFILE` is a real phone-mic profile capture step with typed extraction and explicit CTA save
- onboarding routes `VOICE_HANDSHAKE_PROFILE` directly into `HARDWARE_WAKE`, then inserts `SCHEDULER_QUICK_START` after successful `PROVISIONING` and before `COMPLETE`
- each onboarding tap-started capture may remain active for up to `60s`
- interrupted or backgrounded onboarding recording must cancel cleanly and return the mic footer to a non-listening state
- onboarding uses a FunASR realtime fast lane through the existing `DeviceSpeechRecognizer` seam rather than the main batch `AsrService` path
- during active capture, onboarding requests the shared realtime onboarding profile with `max_sentence_silence = 6000` so short thinking pauses stay in the same utterance more reliably before the stop tap
- while listening, the footer hint slot may show live partial transcript instead of the idle sample prompt
- once recording ends, the footer keeps any already-captured transcript visible during processing; if no transcript was captured yet, the hint slot may remain empty until the next result state is revealed
- onboarding uses dedicated onboarding fast profiles for the happy path:
  - `ModelRegistry.ONBOARDING_CONSULTATION` for consultation reply generation
  - `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION` for strict profile JSON extraction
- if realtime recognition fails while the user is still listening, onboarding surfaces retry immediately and returns the footer to tap-retry state
- if recognition or generation fails after release, onboarding clears to calm retry UI; when a real transcript already exists, it stays visible instead of being replaced with synthetic content
- onboarding owns one visible watchdog per lane rather than nested service + UI timeouts:
  - post-capture recognition target about `5s`
  - consultation target about `2.5s`
  - profile target about `3.5s`
- recognizer-side cancellation that arrives only after the stop tap counts as fast-lane failure in this watchdog and must clear to retry UI, while explicit user/reset/background cancellation still clears directly back to idle
- late results from timed-out or reset intro attempts must be ignored instead of mutating the current onboarding state
- raw realtime ASR payloads must be sanitized before reaching onboarding transcript or error UI
- onboarding voice capture now uses one two-phase tap contract across consultation, profile, and quick start: first tap starts listening, second tap ends capture and submits the real phone-mic utterance through `DeviceSpeechRecognizer`
- quick-start create parsing now reuses the same shared Path A create order as the main scheduler: deterministic relative/exact cue helpers first where lawful, then `Uni-M`, then `Uni-A`, then `Uni-B`; onboarding still omits `Uni-C`, passes `displayedDateIso = null`, and keeps a bounded onboarding-local `Uni-M` sub-budget so timeout/exception falls through to the later shared lanes instead of stalling quick start
- because quick start can now traverse multiple shared scheduler hops before safe-fail, the outer onboarding quick-start watchdog must allow roughly `10s` before surfacing the generic `当前日程整理暂时没有返回，请再试一次。` retry copy
- this capture-local pause widening is separate from the `5s` post-stop recognition watchdog and the quick-start `10s` apply watchdog
- chained same-day exact phrases with one lawful day anchor and later explicit clocks, such as `明天早上7点叫我起床，9点要带合同去见老板`, should stage as multiple exact items rather than surfacing a terminal `Uni-B` vague-style rejection
- quick-start items stay staged locally until the final `COMPLETE` CTA succeeds; reset, exit, or failed finalization must not persist those staged items
- after the first successful staged exact item, onboarding must surface the shared `ReminderReliabilityAdvisor` prompt at point-of-use when reminder reliability guidance exists; if app notifications are disabled, the first branch is the app-notification-settings guidance shown by that advisor, while exact-alarm / OEM follow-up stays under the same shared advisor instead of a quick-start-only dialog
- onboarding may open the advisor-selected settings action only from that shared reminder guidance prompt; it must not invent a custom quick-start-only permission dialog
- after the first successful staged exact item, onboarding may also request `READ_CALENDAR` / `WRITE_CALENDAR` at point-of-use so the final committed exact items can be mirrored into the system calendar; this permission is owned by quick start, not by a later global `MainActivity` prompt
- the quick-start calendar-permission success copy is transient guidance, not persistent success-state body text; it must clear before the completed preview state settles
- `HARDWARE_WAKE` still teaches the 3-second badge wake ritual
- onboarding no longer exposes a global top-right `跳过`; the only production shortcut into `SCHEDULER_QUICK_START` is the local `跳过，直接体验日程` action on `HARDWARE_WAKE` and on the Wi-Fi entry / Wi-Fi recovery surfaces

### Wave 3: Operational Pairing

- `SCAN` uses a technical search presentation
- `DEVICE_FOUND` requires explicit manual tap to connect
- `PROVISIONING` combines Wi-Fi entry plus visible pairing/progress states
- successful `PROVISIONING` now hands into `SCHEDULER_QUICK_START` instead of leaving onboarding immediately
- the local skip shortcut is visible on `HARDWARE_WAKE` and on the recoverable Wi-Fi entry / Wi-Fi failure surfaces only
- the local skip shortcut is not visible on `SCAN`, `DEVICE_FOUND`, or provisioning progress/success

### Wave 4: Recovery and Handoff

- scan and provisioning failures stay calm and recoverable
- timeout/retry states should use muted amber recovery rather than panic-red treatment
- `COMPLETE` is one shared success wrapper with one home-first action copy
- onboarding keeps the `COMPLETE` screen after quick start; it does not jump directly from provisioning into home

### Wave 5: Unified Home Routing

- completion always enters the main app home and marks onboarding complete
- before completion exits into home, it first commits any staged quick-start items into the scheduler-owned fast-track mutation path; failure blocks completion and surfaces calm retry copy
- after successful completion enters home, the shell auto-opens the real scheduler drawer once through the shell-owned drawer animation as the organic teaching handoff for the just-created quick-start items
- on fresh install / reinstall, forced first-launch setup still boots directly into onboarding before ordinary shell use
- later connectivity setup replay still reuses the same onboarding lane, but successful completion returns to home rather than opening connectivity manager
- forced first-launch setup keeps system-back blocked until the user finishes onboarding; the only intentional shortcut is the local pairing-step jump into `SCHEDULER_QUICK_START`

## Locked Invariants

- No generic Android setup wizard treatment
- No mascot energy
- No auto-connect from scan results
- No onboarding-owned SIM shell navigation policy
- No global onboarding `跳过` action
- No system-back or gesture dismissing forced first-launch onboarding into the shell
- No light-theme onboarding variant; onboarding stays dark-first on both hosts
- Onboarding backgrounds reuse the shipped SIM aurora floor across all active steps; pages must not ship separate background systems
- No badge-audio or Tingwu dependency inside the intro interaction
- No profile persistence without explicit CTA save
- No quick-start persistence before the final completion acknowledgement
- No onboarding-owned shadow scheduler runtime; quick-start persistence must reuse scheduler-owned fast-track mutation seams
- No direct onboarding callback that forces the scheduler drawer open; the post-completion reveal must flow through a one-shot shell handoff gate
- No account/profile/notification tail after pairing inside the active production flow

## Deferred Out of Scope

The following legacy steps are still not part of the active production onboarding sequence:

- device naming
- account gate
- notification / OEM permission tail

Note:

- the new `VOICE_HANDSHAKE_PROFILE` step is a pre-pairing intro interaction, not a revival of the old post-pairing profile tail
