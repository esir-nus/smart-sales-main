# Onboarding Interaction Spec

> Status: Active
> Owner: Onboarding Interaction

## Purpose

This shard owns the pre-pairing interactive handshake inside onboarding.

It covers:

- phone-mic hold-to-speak capture for onboarding intro steps
- ASR transcription for those steps
- consultation reply generation
- typed profile extraction generation
- optional profile persistence on explicit CTA
- onboarding-local failure, retry, and skip behavior before pairing starts

It does not own:

- BLE scanning or provisioning
- badge-audio ingestion
- Tingwu long-form processing
- SIM shell routing after onboarding completion

## Step Contract

The old single `VOICE_HANDSHAKE` is replaced with:

1. `VOICE_HANDSHAKE_CONSULTATION`
2. `VOICE_HANDSHAKE_PROFILE`

Both hosts use the same sequence.

Pairing-owned steps still begin at `HARDWARE_WAKE`.

## Runtime Law

### Consultation

- user holds to speak on phone mic
- onboarding uses device speech recognition for the short utterance
- transcript becomes the user bubble
- onboarding builds a short deterministic coaching reply locally
- after two successful turns, the page reveals completion status and allows advance

### Profile

- user holds to speak on phone mic
- onboarding uses device speech recognition for the short utterance
- transcript becomes the user bubble
- onboarding extraction service builds a deterministic local draft
- parsed result becomes:
  - acknowledgement bubble
  - extraction card
- profile data is persisted only when the user taps the CTA

## Processing Watchdog

Onboarding owns a stricter UX watchdog than the main batch ASR / business LLM budgets.

Policy:

- consultation/profile device recognition must resolve or fall back locally within about `1.2s`
- the fast lane uses explicit UI phases: recognizing, building consultation reply, building profile result, deterministic fallback
- timed-out, cancelled, reset, or otherwise stale attempts must not write late transcript / reply / extraction results back into the current UI state
- onboarding happy path must not call `AsrService`, OSS upload, or business LLM services

## Shared Mic Footer Contract

The consultation and profile pages share one onboarding-local voice footer.

Visual/runtime states:

- `idle`: breathing handshake bars plus sample-prompt hint above the mic button
- `recording`: faster cyan handshake motion and listening status
- `processing`: footer stays mounted, returns to the idle-breathing handshake, and shows the processing label
- `revealed result`: the footer may disappear only after the next transcript / reply / extraction result state is rendered

Layout law:

- use six bars with fixed `6.dp` width and `6.dp` gap
- allow bar growth up to `40.dp`
- keep the mic button persistent through recording release and processing

Animation law:

- idle motion starts after a short `600ms` delay and uses a slower breathing cycle
- recording switches immediately on press-down to a faster high-intensity cycle
- implementation should prefer one shared infinite transition with per-bar phase math rather than six independent animators

## Typed Extraction Law

The extraction path must not parse freeform prose heuristically.

Required structured fields:

- `displayName`
- `role`
- `industry`
- `experienceYears`
- `communicationPlatform`
- `acknowledgement`

If JSON is malformed, required fields are missing, or all extracted values are blank, treat the attempt as extraction failure.

Note:

- the active implementation may build the typed draft through deterministic local parsing rather than LLM-emitted JSON
- the draft must still land as the same typed `OnboardingProfileDraft` contract

## Profile Save Law

Profile persistence must write into the existing `UserProfileRepository` model.

Save behavior:

- overwrite `displayName`, `role`, `industry`, `experienceYears`, and `communicationPlatform`
- preserve untouched fields such as `preferredLanguage`, `subscriptionTier`, and `id`
- update `updatedAt`

### Experience Level

The repository model still requires `experienceLevel`.

Policy:

- derive `experienceLevel` deterministically from `experienceYears` when parseable
- if parse fails, preserve the existing `experienceLevel`
- do not ask the model to hallucinate a level

## Failure Policy

Supported failure classes:

- microphone permission denied
- recording too short
- device speech recognition unavailable
- device speech recognition failure
- device speech recognition timeout
- save failure

Policy:

- consultation/profile should prefer invisible deterministic fallback instead of surfacing a hard failure when the fast lane fails
- consultation: retry until success if even the deterministic lane cannot complete
- profile: retry or skip save if even the deterministic lane cannot complete
- skip save advances onboarding without mutating the profile
- failure UI stays calm and non-panic red

## Mic Permission And Session Recovery

Policy:

- onboarding keeps microphone permission at point-of-use rather than requesting it from `PERMISSIONS_PRIMER`
- if the user grants microphone permission from the first consultation/profile press, onboarding should auto-start that interrupted recording session immediately
- the permission-resumed session may switch to tap-to-send because the original hold gesture is interrupted by the Android permission dialog
- if the active onboarding listening session is interrupted by gesture cancellation, disposal, or app backgrounding, onboarding must cancel the session and clear the listening state
- processing-state footer persistence does not override this cancellation rule; interrupted sessions still clear back to non-listening state

## Fast Lane Boundary

Policy:

- onboarding owns a separate device-STT experience lane for first-run speed
- the main `AsrService` batch pipeline remains the source of truth for broader business audio flows
- the main business executor/model registry path remains untouched by this onboarding slice
- if the device-STT lane fails, onboarding may use an invisible deterministic fallback with a short artificial dwell to preserve the experience rhythm

## Ownership Boundary

- onboarding interaction owns only the pre-pairing intro voice lane
- pairing ownership still begins at `HARDWARE_WAKE`
- no onboarding interaction state should leak into pairing domain contracts
