# UI Element Registry

> **Purpose**: Contract between **Legacy UI** (functional reference) and **Target UI** (cosmetic redesign).
>
> **Owner**: UX Specialist (`/08-ux-specialist`)
>
> **Last Updated**: 2026-01-10

---

## Core Concepts

### The Two-UI Model

| Term | Definition |
|------|------------|
| **Legacy UI** | Current app implementation. Ugly but functional. Represents *what elements exist*. |
| **Target UI** | Cosmetic redesign (Web Prototype). Beautiful. Represents *what elements should look like*. |
| **Registry** | Translation layer. Maps Legacy → Target with authority rules. |

```
Legacy UI (Feature Dev)  →  Registry (Contract)  →  Target UI (Web Prototype)
      ↓                           ↓                         ↓
   "What exists"           "What's allowed"           "How it looks"
```

---

## Authority Model

### Cosmetic Authority (UI Designer Can Unilaterally Change)

| Category | Examples | No Approval Needed |
|----------|----------|-------------------|
| **Colors** | Gradients, opacity, tints, dark mode | ✅ |
| **Spacing** | Padding, margins, gaps | ✅ |
| **Typography** | Font weights, sizes, line heights | ✅ |
| **Shapes** | Border radius, shadows, elevation | ✅ |
| **Icons** | Swap icons (same semantic meaning) | ✅ |
| **Animation** | Transitions, easing, duration | ✅ |
| **Proportions** | Card sizes, button widths, layout ratios | ✅ |

### Proposal Required (Needs User Approval)

| Category | Examples | Requires Proposal |
|----------|----------|------------------|
| **Add Element** | New button, new section, new card | 🔄 Proposal |
| **Remove Element** | Delete a button, hide a section | 🔄 Proposal |
| **Rename Label** | "Summarize" → "Quick Summary" | 🔄 Proposal |
| **Change Flow** | Reorder steps, merge screens | 🔄 Proposal |
| **Add State** | New loading state, new error type | 🔄 Proposal |

---

## Alignment Check Workflow

> **Trigger**: Invoke when comparing Legacy UI to Target UI before implementation.

### Step 1: Element Inventory Audit

For each element in the Registry, verify:

```markdown
| Element | In Legacy? | In Target? | Alignment |
|---------|------------|------------|-----------|
| TopBar  | ✅ Yes     | ✅ Yes     | ✅ Aligned |
| ActionGrid | ✅ Yes  | ✅ Yes     | ✅ Aligned |
| QuickSkillRow | ✅ Yes | ❌ No    | 🔄 Deprecated in Target |
| NewCard | ❌ No      | ✅ Yes     | 🔄 UI Proposal Needed |
```

### Step 2: Cosmetic Diff

| Element | Legacy Look | Target Look | Change Type |
|---------|-------------|-------------|-------------|
| `InputBar` | Square corners | Pill shape | ✅ Cosmetic |
| `ActionGrid` | Text buttons | Card grid | ✅ Cosmetic |
| `TopBar` | White bg | Blur glass | ✅ Cosmetic |

### Step 3: Functional Diff (Requires Proposal)

| Element | Legacy Behavior | Target Difference | Status |
|---------|-----------------|-------------------|--------|
| — | — | — | — |

---

## Registry Tables

### HomeScreen

| Element | States | In Legacy | Target Status | Notes |
|---------|--------|-----------|---------------|-------|
| `TopBar` | visible | ✅ | ✅ Styled | Menu + Title + Profile |
| `HeroSection` | visible (empty), hidden (active) | ✅ | ✅ Styled | Only when no messages |
| `KnotSymbol` | idle | ❌ | ✅ Styled | NEW in Target (approved) |
| `HeroGreeting` | visible | ✅ | ✅ Styled | "你好, User" |
| `HeroSubtitle` | visible | ✅ | ✅ Styled | — |
| `ActionGrid` | visible (empty) | ❌ | ✅ Styled | Replaces Legacy bullet list |
| `QuickSkillRow` | visible | ✅ | ⏳ Pending | Keep for functionality, restyle |
| `InputBar` | idle, focused, sending, disabled | ✅ | ⏳ Pending | Floating pill |
| `HistoryDrawer` | open, closed | ✅ | ❌ Not Started | |

---

### ChatScreen

| Element | States | In Legacy | Target Status | Notes |
|---------|--------|-----------|---------------|-------|
| `UserBubble` | sent, error | ✅ | ⏳ Pending | Right-aligned |
| `AssistantBubble` | streaming, complete, error | ✅ | ⏳ Pending | Typewriter effect |
| `TypingIndicator` | visible, hidden | ✅ | ❌ Not Started | |
| `RetryButton` | visible, hidden | ❌ | ❌ Not Started | |
| `SmartAnalysisCard` | collapsed, expanded | ✅ | ❌ Not Started | |

---

### TranscriptionFlow

| Element | States | In Legacy | Target Status | Notes |
|---------|--------|-----------|---------------|-------|
| `UploadProgressBar` | uploading, complete, error | ✅ | ❌ Not Started | |
| `TranscriptionProgress` | submitted, in_progress, complete, error | ✅ | ❌ Not Started | |
| `TranscriptBubble` | streaming, complete | ✅ | ❌ Not Started | |

---

### BadgeFlow (GIF/WAV)

| Element | States | In Legacy | Target Status | Notes |
|---------|--------|-----------|---------------|-------|
| `GifUploadProgress` | preparing, uploading:N/M, complete, error:* | ✅ | ❌ Not Started | |
| `WavDownloadList` | scanning, downloading, complete, error:* | ✅ | ❌ Not Started | |
| `BadgeStatusCard` | connected, disconnected, syncing | ✅ | ❌ Not Started | |

---

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Styled | Target UI complete, ready for Kotlin |
| ⏳ Pending | In Registry, not yet designed in Target |
| ❌ Not Started | Exists in Legacy, no Target work |
| 🔄 Proposed | UI proposes change, awaiting approval |
| 🗑️ Deprecated | Legacy-only, being removed in Target |

---

## UI Proposals Queue

| Element | Proposal | By | Date | Status |
|---------|----------|-----|------|--------|
| `KnotSymbol` | Add brand mark to Hero | UI | 2026-01-10 | ✅ Approved |
| `ActionGrid` | Replace bullet list with cards | UI | 2026-01-10 | ✅ Approved |

---

## Changelog

| Date | Change | By |
|------|--------|-----|
| 2026-01-10 | Initial registry | Agent |
| 2026-01-10 | Added Typewriter, role=alert | Audit |
| 2026-01-10 | Added Legacy ↔ Target model, Alignment Check | UX+UI+Senior Review |
