# Onboarding Interaction Spec

> Status: Active
> Owner: Onboarding Interaction

## Purpose

This shard owns the pre-pairing interactive handshake inside onboarding.

It covers:

- phone-mic two-phase tap capture for onboarding intro steps
- ASR transcription for those steps
- consultation reply generation
- typed profile extraction generation
- optional profile persistence on explicit CTA
- scheduler quick-start sandbox staging after pairing or approved pairing-step skip
- final quick-start scheduler commit on successful completion
- onboarding-local failure, retry, and skip behavior before pairing starts

It does not own:

- BLE scanning or provisioning
- badge-audio ingestion
- Tingwu long-form processing
- live scheduler drawer/runtime behavior outside the onboarding sandbox
- SIM shell routing after onboarding completion

## Step Contract

The old single `VOICE_HANDSHAKE` is replaced with:

1. `VOICE_HANDSHAKE_CONSULTATION`
2. `VOICE_HANDSHAKE_PROFILE`
3. `SCHEDULER_QUICK_START`

All production entry paths use the same sequence through `VOICE_HANDSHAKE_PROFILE`.
Onboarding routes directly into `HARDWARE_WAKE`, keeps pairing ownership through successful `PROVISIONING`, then inserts `SCHEDULER_QUICK_START` before `COMPLETE`.

Pairing-owned steps still begin at `HARDWARE_WAKE`.

### Permissions Primer Layout

- `PERMISSIONS_PRIMER` remains an explanation-only page and must not request Android permissions directly.
- On compact heights, the primer content may scroll.
- The primary continue CTA stays pinned above the navigation area and remains continuously reachable while the explanatory content scrolls behind it.
- Responsive adaptation should compress decorative spacing and card padding before sacrificing content order or CTA reachability.

## Runtime Law

### Consultation

- user taps once to start phone-mic capture, then taps again to end capture and submit
- onboarding uses FunASR realtime recognition through the `DeviceSpeechRecognizer` seam for the intro utterance
- one active tap-started capture session may remain active for up to `60s`
- transcript becomes the user bubble
- onboarding uses `Executor` + `ModelRegistry.ONBOARDING_CONSULTATION` to build a short coaching reply on the happy path
- after two successful turns, the page reveals completion status and allows advance

### Profile

- user taps once to start phone-mic capture, then taps again to end capture and submit
- onboarding uses FunASR realtime recognition through the `DeviceSpeechRecognizer` seam for the intro utterance
- one active tap-started capture session may remain active for up to `60s`
- transcript becomes the user bubble
- onboarding extraction service uses `Executor` + `ModelRegistry.ONBOARDING_PROFILE_EXTRACTION` to produce strict typed extraction JSON on the happy path
- parsed result becomes:
  - acknowledgement bubble
  - extraction card
- profile data is persisted only when the user taps the CTA

### Scheduler Quick Start

- the page reuses the onboarding frame, background family, and shared mic footer shell; like consultation/profile, it now uses a two-phase tap mic interaction: first tap starts capture, second tap ends capture and submits the utterance through `DeviceSpeechRecognizer`
- quick-start transcript handling must reuse the current scheduler Path A extraction family rather than inventing a second onboarding parser:
  - deterministic relative-time and explicit day-clock helpers may fast-path obvious create/update phrases when they already cover the intent cleanly
  - chained same-day exact phrases with one lawful day anchor plus later explicit clocks, such as `明天早上7点叫我起床，9点要带合同去见老板`, must be recovered as staged exact creates before falling into `Uni-M` / `Uni-A` / `Uni-B`
  - onboarding and the main scheduler now share one Path A create interpreter order: deterministic helpers, `Uni-M`, `Uni-A`, then `Uni-B`
  - onboarding still excludes `Uni-C`, passes `displayedDateIso = null`, and keeps one onboarding-local sandbox instead of mutating persisted tasks during staging
  - onboarding keeps first-run responsiveness by giving the shared `Uni-M` hop a bounded local sub-budget; timeout or exception must fall through to `Uni-A` / `Uni-B` rather than failing the whole quick-start attempt
  - because the shared route can still consume several extractor hops in one attempt, the outer onboarding watchdog must budget about `10s` before collapsing into the generic no-return retry copy
  - staged-item reschedule reuses `RealGlobalRescheduleExtractionService` plus an onboarding-local sandbox target resolver so the page can update staged items before final persistence
- the page keeps one onboarding-local sandbox only; it must not read or mutate persisted scheduler tasks before final completion
- each staged item receives or preserves a stable onboarding-session id and stays isolated inside onboarding-local sandbox state until final completion
- once staged items exist, quick start must auto-position its scrollable content so the completion note remains visible above the persistent guidance / CTA / mic footer stack
- this quick-start auto-positioning must run both on the first transition from empty preview to staged preview and on later staged-preview updates while items remain present
- the completion note renders the success icon plus the title `体验已就绪`; the earlier explanatory subtitle is removed because the page already shows the continue/edit affordances elsewhere
- after the first successful staged exact item, onboarding must reuse the shared `ReminderReliabilityAdvisor` prompt when reminder reliability guidance exists instead of auto-opening exact-alarm settings
- when that advisor reports app notifications disabled, quick start must surface the app-notification-settings branch first; exact-alarm, battery, and OEM follow-up stay under the same shared advisor contract rather than a quick-start-local dialog policy
- onboarding may open the advisor-selected settings action only from that shared prompt and only while the prompt gate still says the user should be guided there
- after the first successful staged exact item, onboarding may request `READ_CALENDAR` / `WRITE_CALENDAR` at point-of-use so the final committed exact items can later be mirrored into system calendar events
- calendar-permission feedback inside quick start is transient page guidance only; it must clear on the next mic interaction, on the next successful staged result render, or after a short local timeout rather than persisting into the completed preview state
- onboarding must not ship a custom quick-start-only reminder dialog, a custom calendar education dialog, or a parallel scheduler preview runtime for this page
- after successful provisioning, onboarding shows this page before the existing success wrapper instead of leaving onboarding
- local pairing-step skip from `HARDWARE_WAKE` and from the Wi-Fi entry / Wi-Fi recovery surfaces may also enter this page directly; onboarding must not expose a global top-right skip for that shortcut
- the final `COMPLETE` CTA commits the sandbox through scheduler-owned `FastTrackMutationEngine` seams:
  - exact items use `CreateTasks`
  - vague items use `CreateVagueTask`
- commit success returns created scheduler task ids for any exact/vague writes that actually persisted; when calendar permission was granted, onboarding may mirror the committed exact task ids into the system calendar after commit succeeds
- successful commit or `Noop` clears the sandbox, marks a one-shot shell handoff gate, and lets home auto-open the real scheduler drawer once; reset/exit also clears the sandbox; commit failure blocks completion and keeps the user on the onboarding success page with retry copy
- if a later staged-item write fails, onboarding must roll back earlier writes from the same completion attempt so the quick-start commit stays all-or-nothing at the user-visible level
- the former scripted three-round demo (`起床闹钟` / `带合同见老板` / `接王经理` -> `赶飞机` -> delayed update) is now only a capture/prototype reference, not runtime law

## Processing Watchdog

Onboarding owns a stricter UX watchdog than the main batch ASR / business LLM budgets.

Policy:

- consultation/profile may keep active capture open for up to `60s`, but once capture ends recognition must resolve through an onboarding-local watchdog within about `5s`
- during active capture, onboarding requests the shared realtime recognizer's onboarding profile with `max_sentence_silence = 6000` so short thinking pauses do not collapse the utterance too aggressively before the second tap
- the fast lane uses explicit UI phases: recognizing, building consultation reply, building profile result
- after a usable transcript exists, onboarding owns one user-facing generation watchdog instead of nested timeouts:
  - consultation reply should resolve within about `2.5s`
  - profile extraction should resolve within about `3.5s`
- late or stuck model work must clear processing into calm retry UI instead of synthesizing onboarding content
- recognizer-side `CANCELLED` results that arrive after onboarding has already switched into `recognizing` must be treated as fast-lane failure and must clear through onboarding-local retry handling instead of leaving the footer stuck in processing
- realtime `Failure` / `Cancelled` events that arrive during the active listening session must surface immediately and clear back to tap-retry UI without waiting for another stop tap
- timed-out, cancelled, reset, or otherwise stale attempts must not write late transcript / reply / extraction results back into the current UI state
- raw FunASR SDK payloads must be sanitized before they reach onboarding transcript, hint, or error surfaces
- onboarding happy path must not call `AsrService` or OSS upload

## Shared Mic Footer Contract

The consultation, profile, and quick-start pages share one onboarding-local voice footer.

Visual/runtime states:

- `idle`: breathing handshake bars plus sample-prompt hint above the mic button
- `recording`: faster cyan handshake motion, listening status, and live partial transcript replacing the sample-prompt hint slot when available
- `processing`: footer stays mounted, returns to the idle-breathing handshake, and shows the processing label while preserving any already-captured transcript text
- `revealed result`: the footer may disappear only after the next transcript / reply / extraction result state is rendered
- if realtime recognition fails while the user is still listening, the footer must stop recording immediately, surface calm retry copy, and return to tap-retry state without waiting for another stop tap
- if the `60s` capture limit is reached while the user is still listening, the session must auto-stop, switch immediately into `processing`, and require no extra stop tap

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
- realtime failure while the user is still listening must surface immediately so the UI can return to tap-retry state
- explicit user/reset/background cancellation must invalidate the pending request and clear back to idle
- consultation: retry until success
- profile: retry or skip save
- skip save advances onboarding without mutating the profile
- failure UI stays calm and non-panic red

## Mic Permission And Session Recovery

Policy:

- onboarding keeps microphone permission at point-of-use rather than requesting it from `PERMISSIONS_PRIMER`
- if the user grants microphone permission from the first consultation/profile tap, onboarding returns to idle and asks for a fresh tap rather than auto-starting a resumed recording session
- the permission wait itself must not cancel the pending onboarding session
- onboarding may show a calm guidance hint that the microphone is now available and the user should tap again
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
- the capture-local pause widening above is distinct from both the `5s` post-capture recognition watchdog and the quick-start outer apply watchdog of about `10s`
- onboarding keeps only the guards proved necessary for this slice: stale request invalidation, explicit user/dispose cancellation, and bounded deadlines
- the shared realtime lane currently authenticates through direct `DASHSCOPE_API_KEY` SDK init rather than a custom backend token endpoint
- realtime auth failures must preserve typed diagnosis in logs across direct-key preflight, SDK start, and session failure; the client must not flatten config/auth rejection into one opaque internal branch before evidence is recorded
- if realtime or generation fails, onboarding stays in retry UI; when a transcript already exists, keep it visible and do not synthesize a reply or draft

## Ownership Boundary

- onboarding interaction owns only the pre-pairing intro voice lane
- onboarding interaction also owns the scheduler quick-start sandbox, but it may persist tasks only through the scheduler-owned fast-track mutation seam at final completion and may only request the post-completion scheduler reveal through the shell-owned one-shot handoff gate
- pairing ownership still begins at `HARDWARE_WAKE`
- no onboarding interaction state should leak into pairing domain contracts
