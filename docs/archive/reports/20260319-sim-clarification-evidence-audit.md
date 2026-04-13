# SIM Clarification Evidence Audit

> **Date**: 2026-03-19
> **Scope**: Audit the user's latest SIM clarification against current docs and current code reality
> **Method**: `Core Flow -> Cerb Spec -> Code -> Tests`
> **Primary Inputs**:
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/core-flow/sim-audio-artifact-chat-flow.md`
> - `docs/cerb/sim-shell/spec.md`
> - `docs/cerb/sim-audio-chat/spec.md`
> - `docs/cerb/sim-connectivity/spec.md`

---

## 1. Audit Question

Which parts of the clarified SIM product model are already supported by current docs and code seams, and which parts remain docs-only intent or implementation drift?

---

## 2. High-Signal Findings

### Finding 1: The shell-level support surfaces are materially evidenced in code, not just in docs

Supported evidence:

- The current shell already exposes history, new session, connectivity, audio drawer, and settings/profile entry points in one place:
  `AgentShell` routes menu -> history, new session -> `startNewSession()`, device -> connectivity, audio -> audio drawer, profile -> user center.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt:89-98`.
- The same shell renders History Drawer, Audio Drawer, Connectivity Modal, and User Center as top-level surfaces.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt:115-206`.
- The main chat input attach action already routes to the audio drawer rather than a native picker.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt:370-375`.

Audit conclusion:

- Your clarification that SIM should preserve ordinary shell practices is evidence-backed.
- The support surfaces already exist as reusable seams.
- The contamination risk remains real because this behavior currently lives inside `AgentShell`, not a SIM-only shell root.

### Finding 2: The audio -> Ask AI -> discussion continuation path is already partially real

Supported evidence:

- `AudioDrawer` delegates `Ask AI` through `AudioViewModel.onAskAi()` and returns `(sessionId, isNew)` to the shell for navigation.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt:186-199`.
- `AudioViewModel.onAskAi()` first checks `getBoundSessionId(audioId)` and reuses the existing session if present.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt:89-94`.
- If there is no existing session, `AudioViewModel` creates one with `linkedAudioId = audioId`, injects an overview assistant message when artifacts exist, then binds the audio and session.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt:99-125`.
- `AgentViewModel.switchSession()` reloads `linkedAudioId`, fetches artifacts, and loads summary/transcript into document context for the chat session.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt:180-225`.

Audit conclusion:

- Your “audio drawer is the entrance, chat is the continuation surface” model is supported by existing code seams.
- This is not zero-work, because the current continuation path still uses the smart chat runtime.

### Finding 3: “Already transcribed means load existing result, do not rerun Tingwu” is already supported at the data/session seam

Supported evidence:

- `RealAudioRepository` persists artifacts to `${audioId}_artifacts.json` on successful completion.
  See `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt:203-223`.
- `RealAudioRepository.getArtifacts(audioId)` reloads those persisted artifacts from disk.
  See `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt:320-329`.
- `AudioViewModel.onAskAi()` does not call `startTranscription()`; it reads bound session/artifacts and builds discussion context.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt:89-125`.
- Compact transcribed cards already show preview summary text and only become expandable when status is `TRANSCRIBED`.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt:74-75`, `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt:231-245`.

Audit conclusion:

- This clarified rule is evidence-backed.
- The repo already has the core persistence/session behavior needed for SIM.

### Finding 4: Connectivity is a strong hard-migration candidate, and that is evidence-based

Supported evidence:

- The existing connectivity layer is already formalized in `docs/specs/connectivity-spec.md` and `docs/cerb/connectivity-bridge/spec.md`.
- `RealAudioRepository` already consumes `ConnectivityBridge` for badge sync/download/delete rather than talking to BLE/HTTP directly.
  See `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt:45-49`, `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt:68-118`, `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt:293-305`.
- The current shell already exposes `ConnectivityModal` as a separate modal surface.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt:177-187`.
- The history drawer header already shows badge battery/connection state through `ConnectivityViewModel`.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt:51-66`, `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt:81-99`, `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt:103-153`.

Audit conclusion:

- Your claim that connectivity can take a hard migration path is well supported by the codebase.
- The remaining work is shell isolation, not connectivity reinvention.

### Finding 5: The “settings metadata feeds system persona” claim is only weakly supported in current code

Supported evidence:

- `UserCenterViewModel` edits metadata fields such as `displayName`, `role`, `industry`, `experienceYears`, and `communicationPlatform`.
  See `app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterViewModel.kt:31-49`.
- `AgentViewModel` uses user profile data for `currentDisplayName` and `heroGreeting`.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt:107-121`.
- `RealMascotService` reads `userProfileRepository.profile.value.displayName` when generating its idle prompt.
  See `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealMascotService.kt:84-102`.

Missing evidence:

- No inspected code path shows `role`, `industry`, `experienceYears`, or `communicationPlatform` being compiled into the main agent/system prompt.
- No inspected code path shows User Center metadata becoming the general SIM chat persona contract.

Audit conclusion:

- The repo supports metadata editing and lightweight personalization.
- Your stronger claim that metadata is part of the system prompt is not yet evidenced as a general runtime truth.

### Finding 6: The current code does not yet support the new Tingwu-polisher layer you described

Supported evidence:

- `RealAudioRepository` writes raw `TingwuJobArtifacts` to disk and maps only `smartSummary.summary` into the drawer summary field.
  See `app-core/src/main/java/com/smartsales/prism/data/audio/RealAudioRepository.kt:203-223`.
- `ExpandedAudioHub` renders `transcriptMarkdown`, `smartSummary.summary`, `chapters`, and a locally parsed `meetingAssistanceRaw` section.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt:326-391`, `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt:510-560`.

Missing evidence:

- No inspected code path performs a second-model or second-prompt polish pass over Tingwu output before final display.
- No inspected code path stores or distinguishes a “raw buffer” result from a separately polished final result.

Audit conclusion:

- The current repo supports source-led rendering of Tingwu output.
- Your new readability-polisher layer remains product intent, not current implementation reality.

### Finding 7: The current code does not yet support the full transparent-thinking presentation model you described

Supported evidence:

- Compact cards show a transcribing progress bar for in-flight jobs.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt:276-310`.
- Expanded accordions show a shimmer buffer when content is missing or the card is still transcribing.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt:510-560`.

Missing evidence:

- No inspected code path shows transcript streaming that unfolds and then collapses.
- No inspected code path shows pseudo-streaming fallback.
- No inspected code path shows staged activity labels such as summarizing, matching chapters, finding speakers, or highlighting tied specifically to the Tingwu flow.

Audit conclusion:

- A minimal “processing” presentation exists.
- The richer transparent-thinking layer is not yet backed by code.

### Finding 8: History grouping is real, but rename/delete/pin are not yet evidenced as visible drawer controls

Supported evidence:

- `HistoryViewModel` supports `togglePin`, `renameSession`, and `deleteSession`.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryViewModel.kt:31-45`.
- `AgentShell` passes pin/rename/delete callbacks into `HistoryDrawer`.
  See `app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt:136-144`.
- `HistoryDrawer` renders grouped sections and session items.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt:205-355`.

Missing evidence:

- In the inspected `HistoryDrawer` rendering code, the pin/rename/delete callbacks are accepted as parameters but are not wired into visible session-item actions.
  See `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt:51-62`, `app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt:305-355`.

Audit conclusion:

- Grouped history is real.
- The stronger claim that rename/delete/pin are already ordinary visible practices is only partially supported.

### Finding 9: Test coverage is materially behind the clarified SIM model

Supported evidence:

- The only directly found repository-level audio tests cover concurrency safety and invalid deletion.
  See `app-core/src/test/java/com/smartsales/prism/data/audio/RealAudioRepositoryBreakItTest.kt:68-88`.
- No `AudioViewModel` tests were found in `app-core/src/test`.

Missing evidence:

- No tests were found for:
  - `already transcribed -> no rerun`
  - `Ask AI` session reuse
  - audio drawer -> chat continuation
  - chat attach -> audio drawer reselection
  - Tingwu polisher layer
  - transparent-thinking presentation

Audit conclusion:

- The clarified SIM design is under-tested in current code reality.
- Any implementation wave should add acceptance coverage for these branches.

---

## 3. Verified Clarifications

The following user clarifications are evidence-backed by the current repo:

- connectivity is mature, decoupled, and suitable for hard migration
- the shell already has history/new session/connectivity/settings style support surfaces
- the chat attach route already maps to the audio drawer instead of a native picker
- audio cards already encode the distinction between pending/transcribing/transcribed
- already-transcribed audio can load stored artifacts instead of rerunning Tingwu
- `Ask AI` already behaves like an audio-linked continuation seam at the data/session level

---

## 4. Clarifications Not Yet Proven by Code

The following clarifications are now represented in docs, but are not yet evidenced as current implementation truth:

- a dedicated post-Tingwu readability-polisher model/prompt layer
- transcript unfold/fold streaming or pseudo-streaming
- rich transparent-thinking states specific to the Tingwu pipeline
- settings metadata as a general system-prompt/persona input for the main chat runtime
- visible pin/rename/delete controls inside the rendered history drawer UI

---

## 5. Minimal-Work Impact

This audit lowers risk in three ways:

1. The shell support-surface model is not speculative; it already exists.
2. The audio-to-chat continuation seam is not speculative; it already exists.
3. Connectivity hard migration is not speculative; the bridge and modal path already exist.

The highest new work implied by the clarification is not scheduler or connectivity.
It is:

- SIM shell isolation
- replacing smart chat runtime ownership
- the Tingwu post-processing/polisher layer
- the transparent-thinking presentation layer

---

## 6. Recommendation

Treat the following as the implementation baseline:

- hard-migrate connectivity
- hard-migrate scheduler Path A behavior through a SIM runtime seam
- reuse the existing audio/session seam for `Ask AI`
- do **not** assume the polisher layer or transparent-thinking layer already exist
- do **not** assume user metadata already powers the system persona beyond lightweight personalization

The next document should be a T0 implementation brief that explicitly splits:

- already available reuse seams
- required new SIM runtime ownership
- required new audio-polisher work
- required new acceptance coverage
