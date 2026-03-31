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

- consultation/profile may keep active capture open for up to `60s`, but once capture ends recognition must resolve or fall back locally within about `1.2s`
- the fast lane uses explicit UI phases: recognizing, building consultation reply, building profile result, deterministic fallback
- after a usable transcript exists, onboarding owns one user-facing generation watchdog instead of nested timeouts:
  - consultation reply should resolve within about `2.5s`
  - profile extraction should resolve within about `3.5s`
- late or stuck model work must terminate into onboarding-local deterministic generation instead of leaving the footer pending
- recognizer-side `CANCELLED` results that arrive after onboarding has already switched into `recognizing` must be treated as fast-lane failure and terminated through the local deterministic fallback path instead of leaving the footer stuck in processing
- timed-out, cancelled, reset, or otherwise stale attempts must not write late transcript / reply / extraction results back into the current UI state
- raw FunASR SDK payloads must be sanitized before they reach onboarding transcript, hint, or error surfaces
- onboarding happy path must not call `AsrService` or OSS upload
- deterministic fallback is backup-only; it must not become the normal visible result when the LLM path is healthy
- debug investigation builds may disable deterministic fallback entirely so runtime failures surface as calm retry/error UI instead of synthetic content

## Shared Mic Footer Contract

The consultation and profile pages share one onboarding-local voice footer.

Visual/runtime states:

- `idle`: breathing handshake bars plus sample-prompt hint above the mic button
- `recording`: faster cyan handshake motion, listening status, and live partial transcript replacing the sample-prompt hint slot when available
- `processing`: footer stays mounted, returns to the idle-breathing handshake, and shows the processing label while preserving any already-captured transcript text
- `revealed result`: the footer may disappear only after the next transcript / reply / extraction result state is rendered
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
- the deterministic fallback path may still build the typed draft locally from the resolved transcript when model generation fails
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
- onboarding realtime recognition unavailable
- onboarding realtime recognition failure
- onboarding realtime recognition timeout
- save failure

Policy:

- consultation/profile should prefer invisible deterministic fallback instead of surfacing a hard failure when the fast lane fails
- post-release recognizer cancellation is part of this failure bucket and should fast-fallback locally rather than surfacing a stuck intermediate state
- if a real transcript already exists, deterministic fallback must reuse that transcript rather than injecting fake user content
- normal network latency alone must not be treated as proof that fallback should win; the fallback lane is for explicit failure or watchdog expiry
- when the debug investigation policy disables deterministic fallback, the same failure classes must clear processing, preserve any real transcript already captured, and surface calm retry/skip-save affordances instead of landing synthetic consultation/profile content
- consultation: retry until success if even the deterministic lane cannot complete
- profile: retry or skip save if even the deterministic lane cannot complete
- skip save advances onboarding without mutating the profile
- failure UI stays calm and non-panic red

## Mic Permission And Session Recovery

Policy:

- onboarding keeps microphone permission at point-of-use rather than requesting it from `PERMISSIONS_PRIMER`
- if the user grants microphone permission from the first consultation/profile press, onboarding returns to idle and asks for a fresh press rather than auto-starting a resumed recording session
- the permission wait itself must not cancel the pending onboarding session
- onboarding may show a calm guidance hint that the microphone is now available and the user should press again
- if the active onboarding listening session is interrupted by gesture cancellation, disposal, or app backgrounding, onboarding must cancel the session and clear the listening state
- this explicit user/reset/dispose cancellation path must invalidate the pending request so no deterministic fallback lands after the user has intentionally left the session
- processing-state footer persistence does not override this cancellation rule; interrupted sessions still clear back to non-listening state

## Fast Lane Boundary

Policy:

- onboarding owns a separate FunASR realtime experience lane for first-run speed, exposed through `DeviceSpeechRecognizer`
- the main `AsrService` batch pipeline remains the source of truth for broader business audio flows
- dedicated onboarding-specific model profiles are the happy-path generation seam for onboarding consultation and profile extraction:
  - `ModelRegistry.ONBOARDING_CONSULTATION`
  - `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION`
- onboarding owns one fast-lane user-facing deadline per generation lane rather than stacking a second service-level timeout underneath
- if the onboarding realtime lane fails, or if LLM generation fails after a real transcript exists, onboarding may use an invisible deterministic fallback with a short artificial dwell to preserve the experience rhythm
- debug investigation builds may flip this lane into truth-exposure mode, which suppresses deterministic fallback and emits failure-state telemetry instead of synthetic content

## Ownership Boundary

- onboarding interaction owns only the pre-pairing intro voice lane
- pairing ownership still begins at `HARDWARE_WAKE`
- no onboarding interaction state should leak into pairing domain contracts
