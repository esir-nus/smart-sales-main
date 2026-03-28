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
- onboarding records local WAV audio
- `AsrService` transcribes the audio
- transcript becomes the user bubble
- onboarding consultation service generates a short natural-language coaching reply
- after two successful turns, the page reveals completion status and allows advance

### Profile

- user holds to speak on phone mic
- onboarding records local WAV audio
- `AsrService` transcribes the audio
- transcript becomes the user bubble
- onboarding extraction service requests strict JSON only
- parsed result becomes:
  - acknowledgement bubble
  - extraction card
- profile data is persisted only when the user taps the CTA

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
- ASR failure
- consultation reply failure
- extraction failure
- save failure

Policy:

- consultation: retry until success
- profile: retry or skip save
- skip save advances onboarding without mutating the profile
- failure UI stays calm and non-panic red

## Ownership Boundary

- onboarding interaction owns only the pre-pairing intro voice lane
- pairing ownership still begins at `HARDWARE_WAKE`
- no onboarding interaction state should leak into pairing domain contracts
