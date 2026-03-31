# Onboarding Flow

> Type: Flow
> Status: Active
> Last Updated: 2026-03-31

## Overview

The production onboarding flow now follows the approved 5-wave prototype family.

This flow has two hosts:

1. `FULL_APP` for cold-start product onboarding
2. `SIM_CONNECTIVITY` for SIM setup reuse inside the connectivity route and SIM first-launch bootstrap after fresh install / reinstall

The same Compose coordinator owns both hosts. Host selection changes the visible sequence, not the pairing runtime owner.

Behavior authority for the pairing runtime remains:

- `docs/cerb/device-pairing/spec.md`
- `docs/cerb/device-pairing/interface.md`
- this flow document for `SIM_CONNECTIVITY` host routing
- `docs/cerb/connectivity-bridge/spec.md` and `interface.md` for shared connectivity runtime behavior

## Host Sequences

### FULL_APP

1. `WELCOME`
2. `PERMISSIONS_PRIMER`
3. `VOICE_HANDSHAKE_CONSULTATION`
4. `VOICE_HANDSHAKE_PROFILE`
5. `HARDWARE_WAKE`
6. `SCAN`
7. `DEVICE_FOUND`
8. `PROVISIONING`
9. `COMPLETE`

### SIM_CONNECTIVITY

1. `WELCOME`
2. `PERMISSIONS_PRIMER`
3. `VOICE_HANDSHAKE_CONSULTATION`
4. `VOICE_HANDSHAKE_PROFILE`
5. `HARDWARE_WAKE`
6. `SCAN`
7. `DEVICE_FOUND`
8. `PROVISIONING`
9. `COMPLETE`

SIM now reuses the same calm intro prefix as the full-app host before entering the pairing-owned setup steps.

## Wave Intent

### Wave 1: Tone Foundation

- `WELCOME` presents the calm premium introduction with `您的 AI 销售教练`
- `PERMISSIONS_PRIMER` explains microphone, Bluetooth, and exact-alarm needs before any native prompt appears

Rule:

- the primer is informational first
- Android permission prompts still happen at point-of-use
- if the first consultation/profile mic press triggers `RECORD_AUDIO`, granting permission should return onboarding to idle with a calm guidance hint and require a fresh press

### Wave 2: Interactive Handshake

- `VOICE_HANDSHAKE_CONSULTATION` is a real two-turn phone-mic consultation, not a fake static placeholder
- `VOICE_HANDSHAKE_PROFILE` is a real phone-mic profile capture step with typed extraction and explicit CTA save
- each hold-to-speak intro capture may remain active for up to `60s`
- interrupted or backgrounded onboarding recording must cancel cleanly and return the mic footer to a non-listening state
- onboarding uses a FunASR realtime fast lane through the existing `DeviceSpeechRecognizer` seam rather than the main batch `AsrService` path
- while listening, the footer hint slot may show live partial transcript instead of the idle sample prompt
- once recording ends, the footer keeps any already-captured transcript visible during processing; if no transcript was captured yet, the hint slot may remain empty until the next result state is revealed
- onboarding uses dedicated onboarding fast profiles for the happy path:
  - `ModelRegistry.ONBOARDING_CONSULTATION` for consultation reply generation
  - `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION` for strict profile JSON extraction
- if realtime recognition fails while the user is still holding, onboarding surfaces retry immediately and the later release becomes a no-op
- if recognition or generation fails after release, onboarding clears to calm retry UI; when a real transcript already exists, it stays visible instead of being replaced with synthetic content
- onboarding owns one visible watchdog per lane rather than nested service + UI timeouts:
  - post-capture recognition target about `5s`
  - consultation target about `2.5s`
  - profile target about `3.5s`
- recognizer-side cancellation that arrives only after release counts as fast-lane failure in this watchdog and must clear to retry UI, while explicit user/reset/background cancellation still clears directly back to idle
- late results from timed-out or reset intro attempts must be ignored instead of mutating the current onboarding state
- raw realtime ASR payloads must be sanitized before reaching onboarding transcript or error UI
- `HARDWARE_WAKE` still teaches the 3-second badge wake ritual

### Wave 3: Operational Pairing

- `SCAN` uses a technical search presentation
- `DEVICE_FOUND` requires explicit manual tap to connect
- `PROVISIONING` combines Wi-Fi entry plus visible pairing/progress states

### Wave 4: Recovery and Handoff

- scan and provisioning failures stay calm and recoverable
- timeout/retry states should use muted amber recovery rather than panic-red treatment
- `COMPLETE` is one shared success wrapper with host-specific action copy

### Wave 5: Host Split Routing

- `FULL_APP` completion enters the main app home and marks onboarding complete
- `SIM_CONNECTIVITY` completion enters the SIM connectivity manager first, preserving accepted `SETUP -> MANAGER` routing
- on fresh SIM install / reinstall, the SIM shell bootstraps directly into the `SIM_CONNECTIVITY` host before ordinary shell use
- after the SIM-only first-launch gate is completed, later SIM connectivity replay still reuses the same host but remains manually entered from the shell
- host split now applies to completion routing and connectivity ownership, not to whether the intro prefix is visible
- forced first-launch `SIM_CONNECTIVITY` now exposes a compact top-right `跳过` action that marks the SIM onboarding gate complete and returns to the normal SIM shell without pairing; system back remains blocked for that forced path

## Locked Invariants

- No generic Android setup wizard treatment
- No mascot energy
- No auto-connect from scan results
- No onboarding-owned SIM shell navigation policy
- No system-back or gesture dismissing forced first-launch SIM onboarding into the shell; the only legal bypass is the explicit SIM top-right `跳过` action
- No light-theme onboarding variant; onboarding stays dark-first on both hosts
- Onboarding backgrounds reuse the shipped SIM aurora floor across all active steps; pages must not ship separate background systems
- No badge-audio or Tingwu dependency inside the intro interaction
- No profile persistence without explicit CTA save
- No account/profile/notification tail after pairing inside the active production flow

## Deferred Out of Scope

The following legacy steps are still not part of the active production onboarding sequence:

- device naming
- account gate
- notification / OEM permission tail

Note:

- the new `VOICE_HANDSHAKE_PROFILE` step is a pre-pairing intro interaction, not a revival of the old post-pairing profile tail
