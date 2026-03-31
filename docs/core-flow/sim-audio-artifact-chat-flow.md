# Core Flow: SIM Audio Artifact Chat

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: Audio drawer informational mode, Tingwu-backed transcription/artifact display, readability polishing, transparent-state presentation, and optional audio-context attachment inside the general SIM chat flow.
> **Testing Directive**: Validate one audio/chat route or one safety branch per run. Do not mix unrelated audio and shell branches in a single PU run.

---

## How To Read This Doc

This document defines **what the SIM audio/chat lane must do**, not the final repository class names or DTO details.

Authority note:

- treat this flow plus shared audio-management and Tingwu docs as the active non-Mono audio/chat truth
- deprecated SIM audio/chat shards are migration memory only and must not be used as the active spec layer

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or off-contract first.
- If a PU test does not cover a branch defined here, the validation surface is incomplete.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the behavior is encoded | `docs/cerb/audio-management/spec.md`, `docs/cerb/audio-management/interface.md`, `docs/cerb/tingwu-pipeline/spec.md`, and `docs/cerb/tingwu-pipeline/interface.md` |
| **Code** | Delivered behavior | SIM audio drawer wiring, namespaced audio repository behavior, artifact display, and chat-side audio-context attachment |
| **PU Test** | Behavioral validation | future SIM audio/chat tests, drawer-to-chat tests, storage-isolation tests |

---

## Non-Negotiable Invariants

1. **Audio cards are the entry surface**: audio browsing starts in the audio drawer, not in a hidden background workflow.
2. **Transcribed-card open implies artifact availability**: opening a transcribed card means artifacts already exist or are synchronously fetchable from the persisted result.
3. **Audio drawer is the informational conversation surface**: it should feel like the read-only variant of the chat interface rather than a separate file-manager product.
4. **Tingwu artifacts remain source-led**: transcript, summary, chapters, highlights, speaker data, questions/answers, and similar structures come from Tingwu/API-backed artifacts, not locally invented replacements.
5. **Final display may be polished, but not rewritten into fiction**: SIM may use a readability-polisher prompt over Tingwu output, but it must not invent unsupported facts or sections.
6. **Already-transcribed selection must reuse existing results**: selecting an already-transcribed audio loads stored artifacts instead of rerunning Tingwu.
7. **There is one SIM-owned transcription/artifact pipeline with two entry surfaces**: drawer-origin transcription and chat-origin audio reselection are two ways to enter the same underlying Tingwu/artifact path rather than two separate processing systems.
8. **General SIM chat exists before audio**: blank/new SIM chat is a real conversation surface using system persona, user metadata, and local SIM session history even when no audio is attached yet.
9. **`Ask AI` or chat-side attach binds audio into chat**: the selected audio item and its artifacts or in-flight transcription job become context for an existing or newly opened SIM chat session.
10. **No smart-agent memory system is required**: SIM chat may keep immediate chat state, but it must not require the smart app's wider memory architecture.
11. **Chat-side audio selection returns through the audio drawer**: this flow must not default to Android file picker for the real product path.
12. **Pending audio selected from chat continues inside chat transparency**: if the user picks an untranscribed audio item from the chat-side drawer path, the same SIM transcription pipeline continues from chat and the waiting experience is owned by chat-side transparent processing states.
13. **Pending chat-side selection binds immediately**: when the user selects pending audio from chat-side reselection, SIM binds that discussion session to the chosen audio immediately before Tingwu finishes so the waiting experience already belongs to the correct discussion thread.
14. **Audio persistence must not contaminate the smart app**: if runtime/storage is shared, the audio/chat storage must be namespaced or otherwise isolated.
15. **Badge sync is the real product ingress**: production SIM audio inventory comes from physical badge synchronization via BLE/Wi-Fi transfer rather than arbitrary phone-local uploads.
16. **Phone-local import is test-only**: if a local file picker exists, it is only a testing convenience surface and must not be treated as the product-default audio ingress.
17. **Completed chat-side audio must become durable chat history**: once a pending chat-side audio run completes, the finished artifact content must be appended as durable chat history rather than remaining only in transient thinking/progress state.

## Badge Inventory Sync Rule

Active rule:

- drawer-side badge sync is manual and UI-owned
- opening the drawer must not auto-run badge sync as current product truth
- successful badge-pipeline completions may still appear in the drawer automatically because pipeline completion ingests them into the same SIM audio inventory

Historical rule:

- any older browse-open auto-sync wording belongs to deprecated SIM audio/chat migration memory only and must not override the manual drawer-sync contract

---

## Artifact Surface Rule

The expanded audio card is the artifact surface and the informational counterpart of the discussion chat UI.

Drawer inventory behavior must stay stable:

- transcribed cards should start collapsed by default in the drawer list
- opening a card expands it into the artifact surface
- manual expand/collapse state must remain stable while the drawer stays open and must not reset just because the list scrolls or recomposes

It may display:

- transcript
- summary
- chapters
- highlights
- talker/speaker information when returned
- questions/answers or adjacent provider-returned sections when returned

It may polish:

- raw provider wording
- noisy structure
- readability of final section layout

It must not:

- fabricate absent sections
- recast provider output as if locally authored intelligence produced it
- imply smart-agent reasoning provenance for Tingwu-generated structure
- imply plugin/task-board ownership
- turn the drawer into a generalized memory console

---

## Chat Scope Rule

SIM audio/chat remains SIM-scoped, but chat is no longer audio-entry-only.

It is:

- a directly available SIM conversation surface even before audio is selected
- grounded in system persona, user metadata, and local SIM session history
- able to accept audio context later from `Ask AI` or chat-side attach/reselect
- a question-answer surface over one selected audio
- the transparent waiting surface when chat-side reselection chooses pending audio
- allowed to load transcript and structured artifacts as context
- expected to preserve completed artifact content as part of the discussion history once the run finishes

It is not:

- a generalized smart agent
- a plugin execution cockpit
- a memory-hub shell

---

## Canonical Valves

These valve names are the behavioral checkpoints for downstream specs and tests.
The exact telemetry names in code may differ today, but they should converge toward this model.

- `AUDIO_DRAWER_VISIBLE`
- `AUDIO_LIST_LOADED`
- `LOCAL_AUDIO_ADDED`
- `BADGE_AUDIO_SYNCED`
- `TRANSCRIPTION_REQUESTED`
- `TRANSCRIPTION_PROGRESS_UPDATED`
- `TRANSCRIPTION_COMPLETED`
- `TRANSCRIPTION_FAILED`
- `RAW_TINGWU_RESULT_READY`
- `ARTIFACT_POLISH_STARTED`
- `ARTIFACT_POLISH_COMPLETED`
- `TRANSCRIBED_CARD_OPENED`
- `ARTIFACTS_RENDERED`
- `TRANSCRIPT_STREAM_STARTED`
- `TRANSCRIPT_STREAM_COLLAPSED`
- `ASK_AI_REQUESTED`
- `AUDIO_CHAT_CONTEXT_BOUND`
- `AUDIO_CHAT_VISIBLE`
- `AUDIO_RESELECT_REQUESTED`
- `AUDIO_RESELECT_RETURNED`

---

## Master Routing Flow

This is the top-level routing model for SIM audio/chat.

```text
                    +----------------------+
                    | Audio Drawer Visible |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | Audio Cards Loaded   |
                    +----+-------------+---+
                         |             |
                         v             v
                [Pending Audio]   [Transcribed Audio]
                         |             |
                         v             v
              +----------------+   +-------------------+
              | Transcribe     |   | Load Existing     |
              +-------+--------+   +---------+---------+
                      |                      |
                      v                      v
           +----------------------+  +----------------------+
           | Tingwu in Progress   |  | Informational Audio  |
           |                      |  | View Loaded          |
           +----------+-----------+  +----------+-----------+
                      |                         |
                      v                         v
          +-----------------------+    +--------------------+
          | Raw Result or Failed  |    | Ask AI Requested   |
          +-----+-------------+---+    +----------+---------+
                |             |                    |
                v             v                    v
      [Polish + Render] [Retry / Error] +--------------------+
                |                        | Audio Chat Visible |
                v                        +----------+---------+
      +----------------------+                      |
      | Informational Audio  |                      v
      | View Loaded          |           +--------------------+
      +----------+-----------+           | Reselect Audio     |
                 |                       +----------+---------+
                 v                                  |
           [Ask AI Requested]                       v
                                                    [Reopen Audio Drawer]
```

---

## Safety Branches

### Branch-S1: Transcription Failure

If Tingwu/API-backed transcription fails:

- the audio card must surface failure or retry-ready state
- the system must not fake a transcribed result

### Branch-S2: Missing Artifact on Transcribed Card

If the card says transcribed but artifact fetch fails:

- the UI must surface explicit failure or empty-state feedback
- it must not fabricate chapters, summary, or speaker structure

### Branch-S3: Polisher Failure

If Tingwu returns artifacts but the readability-polisher step fails:

- the app may fall back to lightly formatted source-led output
- the app must not drop the provider result silently
- the app must not claim a polished interpretation exists when it does not

### Branch-S4: Audio Reselect from Chat

When the user requests another audio inside chat:

- the app must reopen the audio drawer
- the user must be able to select one audio item
- selecting an already-transcribed item must bind chat to stored artifacts without rerunning Tingwu
- selecting a pending item must bind chat immediately to that audio and continue the same SIM Tingwu pipeline inside chat with transparent progress states
- once that pending-item run completes, chat must append a durable artifact-backed discussion turn rather than leaving only a lightweight completion notice
- while that audio is already pending/transcribing, the drawer must lock its transcribe trigger for that item so the user cannot start a duplicate run from swipe or button actions
- when that pending-item run completes, the resulting artifact/state must already be reflected back in the audio drawer without requiring a separate drawer-origin rerun
- Android file manager must not become the default path for this branch

### Branch-S6: Test-Only Local Import

If SIM exposes phone-local audio import for QA or developer testing:

- it must remain explicitly secondary to the badge-sync product path
- it may open Android file picker only as an intentional test/import action
- imported files must enter the same SIM-owned transcription/artifact pipeline once ingested
- specs and validation must not mistake this testing affordance for the production ingress model

### Branch-S5: Transparent-State Presentation

When native streaming or provider-native trace is unavailable:

- SIM may use pseudo-streaming and cosmetic activity states
- these states must remain presentational rather than fake backend truth
- for chat-side durable artifact messages, transcript pseudo-streaming may play only once on the first reveal of that message
- that one-time reveal rule applies to both already-transcribed reuse and pending-to-completion artifact turns in chat history
- if the rendered transcript exceeds 4 lines during that first reveal, the UI may classify it as a long transcript immediately, but it must hold the first reveal open long enough to be readable before collapsing it
- the default SIM timing rule for that readable first reveal is about 1 second before collapse is allowed
- reopening the same history session later must not replay that reveal animation; long transcripts should reopen collapsed by default

---

## Done-When Definition

The SIM audio/chat lane is behaviorally ready only when:

- audio drawer browsing works
- transcription runs through Tingwu-backed behavior
- transcribed cards render source-led artifacts with readability polishing
- already-transcribed audio loads existing artifacts without rerunning Tingwu
- blank/new SIM chat is available without audio first
- `Ask AI` attaches one selected audio into chat as a fast path
- chat-side durable artifact messages use one-time transcript reveal behavior and do not replay long transcript streaming on history reentry
- audio re-selection from chat returns to the audio drawer
- persistence stays isolated from the smart app
