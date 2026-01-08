# UX Experience Spec

> **📝 UX-OWNED DOCUMENT**
>
> This file defines **HOW** things are presented—states, flows, microcopy, timing, and layout.
> The UX Specialist persona (`/8-ux-specialist`) may modify this file during polish sessions.
>
> For **WHAT** the system presents (data contracts, pipelines, boundaries), see [`ux-contract.md`](ux-contract.md).

---

## Quick Reference

| Section | Purpose |
|---------|---------|
| [State Inventories](#state-inventories) | Every state users can experience, per flow |
| [Layout Invariants](#layout-invariants) | Component placement rules |
| [Timing & Feedback](#timing--feedback) | Response latency and progress rules |
| [Microcopy](#microcopy) | Text strings for system messages |
| [Changelog](#changelog) | History of experience changes |

---

## State Inventories

### Chat Flow

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `idle` | default | Input field ready | — |
| `composing` | user types | Text in input | — |
| `sending` | tap send | Bubble appears (pending) | — |
| `streaming` | LLM responds | Text streams into bubble | — |
| `complete` | stream ends | Full response in bubble | — |
| `error:network` | connection fail | Error indicator | "Couldn't send. Check your connection." |
| `error:llm` | LLM error | Error indicator | "Something went wrong. Try again." |

### Audio Upload Flow

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `idle` | default | "+" button visible | — |
| `picking` | tap + | System file picker | — |
| `uploading` | file selected | Progress bar | "Uploading... {progress}%" |
| `transcribing` | upload complete | Spinner in chat | "Transcribing..." |
| `complete` | transcription done | Transcript in chat bubble | — |
| `error:upload` | network fail | Error card | "Upload failed. Retry?" |
| `error:transcription` | Tingwu error | Error card | "Couldn't transcribe. Try again?" |

### Transcription Flow

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `loading` | enter transcript view | Skeleton/spinner | "Loading transcript..." |
| `partial` | batches arriving | Growing transcript | — |
| `complete` | all batches received | Full transcript | — |
| `chapters_loading` | analysis pending | Chapter skeleton | "Analyzing..." |
| `chapters_ready` | analysis complete | Chapter list | — |
| `error` | API fail | Error state | "Couldn't load transcript." |

### L3 SmartAnalysis Card

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `collapsed` | default | Summary line + expand button | — |
| `expanded` | tap card | Highlights, Action items, Entities, Pointers | — |

---

## Layout Invariants

| Component | Rule | Visual Spec Ref |
|-----------|------|-----------------|
| **Home Hero** | Visible ONLY when session is empty (no messages, no imported transcripts). Never rendered as chat bubble. | §6.2 |
| **Quick Skill Row** | Under hero when empty; above text field when active. **Never more than one row visible.** | §6.3 |
| **History Drawer** | Device-status card at top, profile entry at bottom. Both use full drawer width. | §6.6 |

---

## Timing & Feedback

| Rule | Target | Verification |
|------|--------|--------------|
| Action acknowledgment | < 200ms | Stopwatch test: tap → visual change |
| Progress update interval | Every 2 seconds | Manual timing during upload |
| Loading states | Always show indicator | UI review: no "frozen" states |
| Error display duration | Until user acknowledges | Cannot auto-dismiss |

---

## Microcopy

### System Messages

| Context | Message |
|---------|---------|
| Upload progress | "Uploading... {progress}%" |
| Transcription pending | "Transcribing..." |
| Transcription complete | — (transcript renders inline) |
| Network error (chat) | "Couldn't send. Check your connection." |
| Network error (upload) | "Upload failed. Retry?" |
| Transcription error | "Couldn't transcribe. Try again?" |
| Generic error | "Something went wrong. Try again." |

### Empty States

| Screen | Message |
|--------|---------|
| Chat (no history) | [Hero + Quick Skills visible] |
| Transcript view (loading) | "Loading transcript..." |
| History drawer (no sessions) | "No previous conversations" |

---

## Open Questions

Track unresolved UX decisions here for Product/Eng review:

- [ ] Should error states auto-dismiss after N seconds?
- [ ] Maximum transcription duration before timeout warning?
- [ ] Should "Cancel" be available during transcription (after upload complete)?

---

## Changelog

| Date | Flow | Change | Reason |
|------|------|--------|--------|
| 2026-01-08 | — | Initial creation | Split from ux-contract.md |
