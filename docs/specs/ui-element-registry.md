# UI Element Registry

> **Purpose**: The **contract** between UX (who defines elements) and UI (who styles them).
>
> **Owner**: UX Specialist (`/08-ux-specialist`)
>
> **Last Updated**: 2026-01-10

---

## How This Works

| Role | Can Do | Cannot Do |
|------|--------|-----------|
| **UX Specialist** | Add/remove rows, define states, update requirements | Style elements |
| **UI Designer** | Style listed elements, update UI Status | Add new elements, invent features |
| **Web Prototype** | Render listed elements only | Create elements not in registry |
| **User** | Add Notes, requirements, approve UX proposals | — |

---

## Registry

### HomeScreen

| Element | States | UX Ref | UI Status | Notes |
|---------|--------|--------|-----------|-------|
| `TopBar` | visible | [Layout Invariants](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#layout-invariants) | ✅ Styled | Menu + Title + Profile |
| `HeroSection` | visible (empty session), hidden (active session) | [Layout Invariants](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#layout-invariants) | ✅ Styled | Only shown when no messages |
| `KnotSymbol` | idle | [style-guide.md](file:///home/cslh-frank/main_app/docs/guides/style-guide.md) | ✅ Styled | Infinity/Lemniscate logo |
| `HeroGreeting` | visible | — | ✅ Styled | "你好, User" |
| `HeroSubtitle` | visible | — | ✅ Styled | "我是您的智能销售助手" |
| `ActionGrid` | visible (empty session) | [Layout Invariants](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#layout-invariants) | ✅ Styled | 2x2 grid: New Task, Summarize, Ideas, Schedule |
| `InputBar` | idle, focused, sending, disabled | [Chat Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#chat-flow) | ⏳ Pending | Floating pill with + button |
| `HistoryDrawer` | open, closed | [Layout Invariants](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#layout-invariants) | ❌ Not Started | |

---

### ChatScreen

| Element | States | UX Ref | UI Status | Notes |
|---------|--------|--------|-----------|-------|
| `UserBubble` | sent, error | [Chat Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#chat-flow) | ⏳ Pending | Right-aligned, accent color. Error requires `role=alert`. |
| `AssistantBubble` | streaming:waiting, streaming:active, streaming:stalled, complete, error | [Chat Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#chat-flow) | ⏳ Pending | Left-aligned, surface color. **Typewriter Effect** required for streaming (token-by-token). Error requires `role=alert`. |
| `TypingIndicator` | visible, hidden | [Chat Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#chat-flow) | ❌ Not Started | Shown during streaming:waiting |
| `RetryButton` | visible (recoverable error), hidden | [Chat Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#chat-flow) | ❌ Not Started | |
| `SmartAnalysisCard` | collapsed, expanded | [L3 SmartAnalysis](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#l3-smartanalysis-card) | ❌ Not Started | |

---

### TranscriptionFlow

| Element | States | UX Ref | UI Status | Notes |
|---------|--------|--------|-----------|-------|
| `UploadProgressBar` | uploading, complete, error | [Audio Upload](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#audio-upload-flow) | ❌ Not Started | |
| `TranscriptionProgress` | submitted, in_progress, batches_arriving, complete, error | [Transcription Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#transcription-flow) | ❌ Not Started | |
| `TranscriptBubble` | streaming, complete | — | ❌ Not Started | |

---

### BadgeFlow (GIF/WAV)

| Element | States | UX Ref | UI Status | Notes |
|---------|--------|--------|-----------|-------|
| `GifUploadProgress` | preparing, connecting, uploading:N/M, complete, error:ble, error:network, error:frame, error:timeout | [GIF Upload Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#gif-upload-flow-badge) | ❌ Not Started | Error states require `role=alert` for accessibility. |
| `WavDownloadList` | scanning, files_found, downloading, complete, empty, error:ble, error:network, error:sd | [WAV Download Flow](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md#wav-download-flow-badge) | ❌ Not Started | Error states require `role=alert` for accessibility. |
| `BadgeStatusCard` | connected, disconnected, syncing | — | ❌ Not Started | In HistoryDrawer |

---

## UI Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Styled | Element is designed and matches style-guide |
| ⏳ Pending | Element is defined but not yet styled |
| ❌ Not Started | Element is in UX spec but no UI work done |
| 🔄 Proposed | UI persona proposed a change, awaiting UX approval |

---

## UX → UI Handoff Protocol

```
1. UX adds element to this registry (with states + UX Ref)
2. User (Frank) adds Notes if needed
3. UI Designer reads registry, styles element
4. UI Designer updates UI Status to ✅
5. If UI proposes a change → set status to 🔄 and add comment in Notes
6. User approves/rejects → UX updates registry accordingly
```

---

## UI Proposals Queue

When UI Designer wants to propose a UX change (e.g., "This button should be in a different place"):

| Element | Proposal | Proposed By | Date | Status |
|---------|----------|-------------|------|--------|
| *Example: InputBar* | *Move + button to right side* | *UI Designer* | *2026-01-10* | *Pending Review* |

> **Rule**: UI cannot implement proposals. Only after User approves and UX updates the registry.

---

## Changelog

| Date | Change | By |
|------|--------|-----|
| 2026-01-10 | Initial registry created | Agent |
| 2026-01-10 | Added audit findings: Typewriter Effect for streaming, `role=alert` for error states | UI/UX Pro Max Audit |
