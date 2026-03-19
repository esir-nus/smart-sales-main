# Core Flow: SIM Audio Artifact Chat

> **Role**: Core Flow
> **Authority**: Behavioral North Star
> **Status**: Ahead of Spec and Code
> **Development Chain**: Core Flow -> Spec -> Code -> PU Test -> Fix Spec -> Fix Code
> **Scope**: Audio drawer informational mode, Tingwu-backed transcription/artifact display, readability polishing, transparent-state presentation, and simple audio-grounded chat inside the SIM standalone app.
> **Testing Directive**: Validate one audio/chat route or one safety branch per run. Do not mix unrelated audio and shell branches in a single PU run.

---

## How To Read This Doc

This document defines **what the SIM audio/chat lane must do**, not the final repository class names or DTO details.

- If this document conflicts with lower specs, treat the lower specs as drift candidates first.
- If this document conflicts with code, treat the code as behind or off-contract first.
- If a PU test does not cover a branch defined here, the validation surface is incomplete.

This file is allowed to be ahead of the codebase.

---

## Downstream Sync Targets

| Layer | Responsibility | Must Eventually Reflect This Core Flow |
|------|----------------|-----------------------------------------|
| **Spec** | How the behavior is encoded | `docs/cerb/sim-audio-chat/spec.md`, `docs/cerb/sim-audio-chat/interface.md` |
| **Code** | Delivered behavior | SIM audio drawer wiring, namespaced audio repository behavior, artifact display, simple audio-grounded chat |
| **PU Test** | Behavioral validation | future SIM audio/chat tests, drawer-to-chat tests, storage-isolation tests |

---

## Non-Negotiable Invariants

1. **Audio cards are the entry surface**: audio browsing starts in the audio drawer, not in a hidden background workflow.
2. **Transcribed-card open implies artifact availability**: opening a transcribed card means artifacts already exist or are synchronously fetchable from the persisted result.
3. **Audio drawer is the informational conversation surface**: it should feel like the read-only variant of the chat interface rather than a separate file-manager product.
4. **Tingwu artifacts remain source-led**: transcript, summary, chapters, highlights, speaker data, questions/answers, and similar structures come from Tingwu/API-backed artifacts, not locally invented replacements.
5. **Final display may be polished, but not rewritten into fiction**: SIM may use a readability-polisher prompt over Tingwu output, but it must not invent unsupported facts or sections.
6. **Already-transcribed selection must reuse existing results**: selecting an already-transcribed audio loads stored artifacts instead of rerunning Tingwu.
7. **`Ask AI` is audio-grounded**: the chat session is bound to one selected audio item and its artifacts.
8. **No smart-agent memory system is required**: SIM chat may keep immediate chat state, but it must not require the smart app's wider memory architecture.
9. **Chat-side audio selection returns through the audio drawer**: this flow must not default to Android file picker.
10. **Audio persistence must not contaminate the smart app**: if runtime/storage is shared, the audio/chat storage must be namespaced or otherwise isolated.

---

## Artifact Surface Rule

The expanded audio card is the artifact surface and the informational counterpart of the discussion chat UI.

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

SIM chat is intentionally narrow.

It is:

- a continuation surface for the transcription discussion started from the audio drawer
- a question-answer surface over one selected audio
- allowed to load transcript and structured artifacts as context

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
- Android file manager must not become the default path for this branch

### Branch-S5: Transparent-State Presentation

When native streaming or provider-native trace is unavailable:

- SIM may use pseudo-streaming and cosmetic activity states
- these states must remain presentational rather than fake backend truth

---

## Done-When Definition

The SIM audio/chat lane is behaviorally ready only when:

- audio drawer browsing works
- transcription runs through Tingwu-backed behavior
- transcribed cards render source-led artifacts with readability polishing
- already-transcribed audio loads existing artifacts without rerunning Tingwu
- `Ask AI` opens a simple audio-grounded chat
- audio re-selection from chat returns to the audio drawer
- persistence stays isolated from the smart app
