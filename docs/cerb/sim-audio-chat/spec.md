# SIM Audio Chat Spec

> **Scope**: Audio drawer, Tingwu artifact display, readability polishing, transparent-state presentation, and simple audio-grounded chat for the standalone SIM prototype
> **Status**: SPEC_ONLY
> **Behavioral Authority Above This Doc**:
> - `docs/core-flow/sim-audio-artifact-chat-flow.md`
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/cerb/audio-management/spec.md`
> - `docs/cerb/tingwu-pipeline/spec.md`
> - `docs/specs/modules/AudioDrawer.md`
> **Audit Evidence**:
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`

---

## 1. Purpose

`SIM Audio Chat` defines the second major feature lane of the standalone prototype:

- browse audio in the Audio Drawer
- transcribe through Tingwu-backed services
- open a transcribed card and view artifacts in an informational conversation-style interface
- enter a simple audio-grounded chat as continuation of that discussion

This shard intentionally replaces the smart-agent interpretation of chat with a narrow audio-grounded product.

---

## 2. Included Scope

### Audio Drawer

- list audio cards
- support local/manual audio inventory
- optionally support SmartBadge sync if the boundary freeze keeps it in V1
- support transcribe action
- support opening transcribed cards
- act as the informational, non-chat variant of the chat experience

### Artifact Display

- transcript
- summary
- chapters
- highlights
- speaker/talker-related Tingwu sections when present
- questions/answers or adjacent provider-returned sections when present
- readability polishing over Tingwu output when needed
- transcript streaming or pseudo-streaming presentation when needed

### Simple Chat

- `Ask AI` starts a plain chat session for one selected audio
- chat answers are grounded in the chosen audio's artifacts
- audio re-selection from chat reopens the Audio Drawer
- if an audio is already transcribed, SIM loads stored artifacts instead of rerunning Tingwu

---

## 3. Excluded Scope

- smart-agent memory system
- plugin task board
- mascot behavior
- generalized autonomous tool execution
- native file manager jump from chat-side audio selection

---

## 4. Ownership

`SIM Audio Chat` owns:

- the standalone audio drawer interaction contract
- the informational-audio-view to discussion-chat relationship
- the standalone audio-to-chat handoff
- the simple chat context rule for one chosen audio

`SIM Audio Chat` may reuse:

- `AudioDrawer`
- most of `AudioViewModel` behavior
- most of `RealAudioRepository` behavior

`SIM Audio Chat` must not assume:

- current smart chat/session orchestration is safe to reuse unchanged
- current storage names are safe for the prototype

---

## 5. Storage Rule

The standalone prototype must namespace its audio persistence if it shares the same runtime/storage container as the smart app.

Minimum namespace targets:

- metadata file
- local audio blob naming
- artifact file naming
- audio/chat binding records if persisted

If SIM receives a separate application ID and file space, this requirement may be satisfied by that app boundary instead.

---

## 6. Chat Context Rule

The chat for SIM is narrow.

It is allowed to:

- continue discussion that begins from the informational audio drawer
- load one audio's transcript and structured artifacts as context
- answer follow-up questions about that audio

It is not allowed to:

- behave like the current agent OS
- accumulate broad session memory beyond the immediate audio-grounded conversation
- use the plugin/tool board as its primary interaction model

---

## 7. Reuse Direction

### Strong Reuse Candidate

- `AudioDrawer`

### Conditional Reuse Candidate

- `AudioViewModel`
  - only after reviewing which parts are safe to keep versus replace for SIM chat/session rules

### Storage Reuse Candidate

- `RealAudioRepository`
  - only after namespacing is explicit

### Do Not Reuse Directly

- `AgentViewModel` as the audio chat runtime

---

## 8. Human Reality

### Organic UX

Users do not care whether the shell is using the same hidden runtime.
They care that:

- the audio drawer feels familiar
- opening a transcribed card is immediate and legible
- `Ask AI` feels direct and focused

### Data Reality

Tingwu artifacts arrive as structured external-service output.
SIM should remain source-led instead of pretending to synthesize them locally.
SIM may still apply a readability-polisher prompt to reduce noisy provider output before final display.

### Legacy Reality

Current code already provides partial audio-to-chat continuation seams and artifact persistence.
Those seams are reuse candidates, not proof that the current smart chat runtime should remain the SIM owner.

### Transparent State Reality

The UI may show transcript streaming, pseudo-streaming, and agent-like activity labels such as summarizing or finding speakers.

These are presentation states unless directly backed by provider-native signals.
The spec does not require fake backend stage fidelity.

### Failure Gravity

The worst failure is silently reintroducing the smart-agent chat runtime because `Ask AI` seems "close enough."

That would destroy the product boundary.

---

## 9. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | Audio/chat contract freeze | PLANNED | SIM audio/chat spec/interface |
| 2 | Storage namespace decision | PLANNED | prototype-safe repository behavior |
| 3 | Audio drawer and artifact render | PLANNED | transcribed-card informational flow |
| 4 | Polisher and transparent-state layer | PLANNED | readability + presentation behavior |
| 5 | Simple chat and reselection | PLANNED | `Ask AI` and drawer-based audio reselection |

---

## 10. Done-When Definition

SIM audio/chat is ready only when:

- audio drawer works in the standalone app
- transcribed audio cards open and show source-led, readability-polished artifacts
- already-transcribed audio loads existing artifacts without rerunning Tingwu
- `Ask AI` opens a simple audio-grounded chat continuation surface
- chat-side audio selection returns to the drawer rather than Android file manager
- audio/session persistence does not contaminate the smart app
