# Walkthrough: Audio Hub Redesign

**Goal**: Transform `AudioCard` into a "Hub" (Consumption) and "Portal" (Interaction) system.

## Changes Created

### 1. Refactored AudioCard
Transformed the monolithic `AudioCard.kt` into two distinct states/composables:

*   **`CompactAudioCard`**: Minimalist list item.
    *   **Visual**: Icon, Filename, Time, 1-Line Truncated Summary.
    *   **Action**: Click to Expand.

*   **`ExpandedAudioHub`**: Feature-rich consumption view.
    *   **Player Header**: Top-pinned player with waveform visualization.
    *   **Portal Button**: "Ask AI" button (Primary Action) to launch Analyst Chat.
    *   **Accordions**: Collapsible sections for Summary, Transcript, Chapters, Highlights.

### 2. State Hoisting in AudioDrawer
*   Moved expansion state to `AudioDrawer` level (`expandedCardId`).
*   Ensures consistent "One Hub Open at a Time" behavior.

### 3. Portal Navigation
*   Wired "Ask AI" button to a callback `onAskAi: (String) -> Unit`.
*   (Currently prints to stdout, pending `PrismNavigation` implementation).

## Visual Verification (ASCII)

### Compact View
```
┌───────────────────────────────────────────────────────────┐
│  [★] Q4_年度预算会议.wav                           14:20  │
│  财务部关于Q4预算的最终审核意见，重点讨论了SaaS...            │
└───────────────────────────────────────────────────────────┘
```

### Hub View
```
┌───────────────────────────────────────────────────────────┐
│   [ ▼ Collapse ]                                          │
│  ═══════════════════════════════════════════════════════  │
│  [ ▶  00:12 / 14:20    |||||·|||||||·||||    🔊 ]        │
│  ═══════════════════════════════════════════════════════  │
│                                                           │
│  [ ✨ 问AI (Portal) ]                                     │
│                                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ [ > ] 📝 摘要 (Summary)                             │  │
│  └─────────────────────────────────────────────────────┘  │
│  ... (Transcripts, Chapters) ...                          │
└───────────────────────────────────────────────────────────┘
```

## Review Conference Outcomes
*   **Approved**: Hub (Read) vs Portal (Talk) architecture.
*   **Adjusted**: Simplified implementation (Inlined accordions, callback navigation).

## Verification Results
*   **Build**: ✅ SUCCESS (`./gradlew :app-prism:assembleDebug`)
*   **Coupling**: ✅ Low (1 caller in `AudioDrawer`).
*   **Assets**: ✅ Updated with `VolumeUp`, `AutoAwesome` icons.
