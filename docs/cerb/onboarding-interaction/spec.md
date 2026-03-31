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

### Permissions Primer Layout

- `PERMISSIONS_PRIMER` remains an explanation-only page and must not request Android permissions directly.
- On compact heights, the primer content may scroll.
- The primary continue CTA stays pinned above the navigation area and remains continuously reachable while the explanatory content scrolls behind it.
- Responsive adaptation should compress decorative spacing and card padding before sacrificing content order or CTA reachability.

## Runtime Law

### Consultation

- user holds to speak on phone mic
- onboarding uses FunASR realtime recognition through the `DeviceSpeechRecognizer` seam for the intro utterance
- one hold-to-speak capture session may remain active for up to `60s`
- transcript becomes the user bubble
- onboarding uses `Executor` + `ModelRegistry.ONBOARDING_CONSULTATION` to build a short coaching reply on the happy path
- after two successful turns, the page reveals completion status and allows advance

### Profile

- user holds to speak on phone mic
- onboarding uses FunASR realtime recognition through the `DeviceSpeechRecognizer` seam for the intro utterance
- one hold-to-speak capture session may remain active for up to `60s`
- transcript becomes the user bubble
- onboarding extraction service uses `Executor` + `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION` to produce strict typed extraction JSON on the happy path
- parsed result becomes:
  - acknowledgement bubble
  - extraction card
- profile data is persisted only when the user taps the CTA

## Processing Watchdog

Onboarding owns a stricter UX watchdog than the main batch ASR / business LLM budgets.

Policy:

- consultation/profile may keep active capture open for up to `60s`, but once capture ends recognition must resolve through an onboarding-local watchdog within about `5s`
- the fast lane uses explicit UI phases: recognizing, building consultation reply, building profile result
- after a usable transcript exists, onboarding owns one user-facing generation watchdog instead of nested timeouts:
  - consultation reply should resolve within about `2.5s`
  - profile extraction should resolve within about `3.5s`
- late or stuck model work must clear processing into calm retry UI instead of synthesizing onboarding content
- recognizer-side `CANCELLED` results that arrive after onboarding has already switched into `recognizing` must be treated as fast-lane failure and must clear through onboarding-local retry handling instead of leaving the footer stuck in processing
- realtime `Failure` / `Cancelled` events that arrive during the active hold must surface immediately, and the later finger-up event becomes a no-op
- timed-out, cancelled, reset, or otherwise stale attempts must not write late transcript / reply / extraction results back into the current UI state
- raw FunASR SDK payloads must be sanitized before they reach onboarding transcript, hint, or error surfaces
- onboarding happy path must not call `AsrService` or OSS upload

## Shared Mic Footer Contract

The consultation and profile pages share one onboarding-local voice footer.

Visual/runtime states:

- `idle`: breathing handshake bars plus sample-prompt hint above the mic button
- `recording`: faster cyan handshake motion, listening status, and live partial transcript replacing the sample-prompt hint slot when available
- `processing`: footer stays mounted, returns to the idle-breathing handshake, and shows the processing label while preserving any already-captured transcript text
- `revealed result`: the footer may disappear only after the next transcript / reply / extraction result state is rendered
- if realtime recognition fails while the user is still pressing, the footer must stop recording immediately, surface calm retry copy, and treat the later finger-up event as a no-op
- if the `60s` capture limit is reached while the user is still pressing, the session must auto-stop, switch immediately into `processing`, and treat the later finger-up event as a no-op

Hint-slot law:

- idle shows the prototype sample prompt
- recording shows the latest live transcript when the realtime lane has emitted one; otherwise the hint slot may remain empty
- processing keeps the latest transcript visible when one has already been captured; otherwise the hint slot may remain empty

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

- the happy path uses LLM-emitted strict JSON
- if model generation fails after a real transcript already exists, onboarding preserves that transcript, clears processing, and asks the user to retry or skip save
- successful extraction still lands as the same typed `OnboardingProfileDraft` contract

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
- onboarding realtime recognition unavailable
- onboarding realtime recognition failure
- onboarding realtime recognition timeout or post-release cancellation
- consultation/profile generation timeout or failure
- save failure

Policy:

- consultation/profile surface calm retry state instead of synthetic content when the fast lane fails
- if a real transcript already exists, preserve that transcript and show retry rather than inventing user or AI content
- post-release recognizer cancellation is part of this failure bucket and must clear the stuck processing state
- realtime failure while the user is still holding must surface immediately so the later release is a no-op
- explicit user/reset/background cancellation must invalidate the pending request and clear back to idle
- consultation: retry until success
- profile: retry or skip save
- skip save advances onboarding without mutating the profile
- failure UI stays calm and non-panic red

## Mic Permission And Session Recovery

Policy:

- onboarding keeps microphone permission at point-of-use rather than requesting it from `PERMISSIONS_PRIMER`
- if the user grants microphone permission from the first consultation/profile press, onboarding returns to idle and asks for a fresh press rather than auto-starting a resumed recording session
- the permission wait itself must not cancel the pending onboarding session
- onboarding may show a calm guidance hint that the microphone is now available and the user should press again
- if the active onboarding listening session is interrupted by gesture cancellation, disposal, or app backgrounding, onboarding must cancel the session and clear the listening state
- this explicit user/reset/dispose cancellation path must invalidate the pending request so no late processing result lands after the user has intentionally left the session
- processing-state footer persistence does not override this cancellation rule; interrupted sessions still clear back to non-listening state

## Fast Lane Boundary

Policy:

- onboarding owns a separate FunASR realtime experience lane for first-run speed, exposed through `DeviceSpeechRecognizer`
- the main `AsrService` batch pipeline remains the source of truth for broader business audio flows
- dedicated onboarding-specific model profiles are the happy-path generation seam for onboarding consultation and profile extraction:
  - `ModelRegistry.ONBOARDING_CONSULTATION`
  - `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION`
- onboarding owns one onboarding-local deadline per fast lane instead of stacked speculative backup branches:
  - post-capture recognition about `5s`
  - consultation generation about `2.5s`
  - profile extraction about `3.5s`
- onboarding keeps only the guards proved necessary for this slice: stale request invalidation, explicit user/dispose cancellation, and bounded deadlines
- realtime auth/token failures must preserve typed diagnosis in logs across token fetch, dialog start, and session failure; the client must not flatten quota/config/network/auth causes into one opaque internal branch before evidence is recorded
- the external `POST /api/ai/dashscope/realtime-token` contract must preserve enough safe evidence for diagnosis, at minimum meaningful HTTP status for 401/403, 429, and 5xx classes
- if realtime or generation fails, onboarding stays in retry UI; when a transcript already exists, keep it visible and do not synthesize a reply or draft

## Ownership Boundary

- onboarding interaction owns only the pre-pairing intro voice lane
- pairing ownership still begins at `HARDWARE_WAKE`
- no onboarding interaction state should leak into pairing domain contracts
