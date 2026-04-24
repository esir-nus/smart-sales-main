# Audio Drawer Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped (semantics) · 🚧 Refined visual/motion contract in effect 2026-04-21
> **Source**: Extracted from prism-ui-ux-contract.md L707-849
> **Visual prototype**: [`docs/inbox/audio-drawer-vibes.html`](../../inbox/audio-drawer-vibes.html) — HTML source of truth for the refined direction below
> **Visual/motion authority**: §R (Refined Visual & Motion Contract) supersedes the table/ASCII chrome notes in §Layout Structure, §Interactions Spring row, §Audio Card States, and §Transcription States. Behavioural contract (Browse vs Select modes, gesture semantics, sync flow) is unchanged and remains authoritative above.

---

## Overview

Bottom-up drawer for managing audio recordings. Pull up from bottom edge to open.

The drawer now has two distinct interaction modes:

- **Browse Mode**: opened directly as an audio gallery and informational artifact surface
- **Select Mode**: reopened from chat attach/upload as a static audio picker for the current chat session

The same audio inventory is reused in both modes, but the interaction language must change clearly so users do not confuse gallery gestures with selection behavior.

---

## Mode Variants

### Browse Mode

Use Browse Mode when the user opens the drawer directly.

- behaves like the spec-aligned audio gallery
- pending items keep the swipe-right-to-transcribe interaction
- transcribed items can be tapped to expand into the artifact surface
- expanded cards may expose `问AI`
- swipe and expansion affordances remain visible

### Select Mode

Use Select Mode when the drawer is opened from the chat input attach/upload affordance.

- behaves like a focused picker, not a gallery
- cards are the action surface; no dedicated bottom CTA button is required
- swipe actions are suppressed
- card expansion is suppressed
- `问AI` is suppressed
- helper copy must explain tap-to-select behavior
- header copy should clearly frame selection, such as `选择要讨论的录音`
- helper copy should clearly frame the rule, such as `点击录音卡片切换当前聊天`

---

## Layout Structure

```
┌────────────────────────────────────────────────────────────┐
│  ══════════════════ PILL HANDLE ══════════════════════════ │
│                         ───                                │
├────────────────────────────────────────────────────────────┤
│  [ ↻ 同步中... ]                                           │ ← Header (sync auto on open)
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ Q4_年度预算会议_Final.wav              14:20   [★]    ││ ← Row 1: Name + Time + Star
│  │ 财务部关于Q4预算的最终审核意见...                      ││ ← Row 2: Preview / state copy
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ 客户拜访_张总_20260124.wav                2 days  [☆]  ││
│  │ ░░░░░░░░░ 右滑开始转写 >>> ░░░░░░░░░                    ││ ← Browse pending hint
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [+] 上传本地音频                                       ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
└────────────────────────────────────────────────────────────┘
```

---

## Interactions

| Action | Trigger | Notes |
|--------|---------|-------|
| **Open** | Pull up from bottom edge (>50px) OR button tap | — |
| **Dismiss** | Drag down (>100px) OR tap scrim OR tap pill handle | — |
| **Height** | `95vh` | Leaves 5% gap at top |
| **Spring Animation** | `damping: 25, stiffness: 200` | Consistent with Scheduler |

---

## Audio Card States

```
┌───────────────────────────────────────────────────────────┐
│  COLLAPSED (Default)                                      │
├───────────────────────────────────────────────────────────┤
│  Q4_年度预算会议.wav                           14:20  [★] │
│  财务部关于Q4预算的最终审核意见...                        │
└───────────────────────────────────────────────────────────┘
        │
        │ tap card body (ONLY if transcribed)
        ▼
┌───────────────────────────────────────────────────────────┐
│  EXPANDED                                                 │
├───────────────────────────────────────────────────────────┤
│  Q4_年度预算会议.wav                           14:20  [★] │
│  ───────────────────────────────────────────────────────  │
│      📅 2026-01-23 15:30                 ┌──────────────┐ │
│                                          │    问AI      │ │
│      ─────────────────────────────────── └──────────────┘ │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ [转写内容 - 可折叠]                            [∧]  │  │
│  │ ─────────────────────────────────────────────────── │  │
│  │ 财务部关于Q4预算的最终审核意见...                   │  │
│  │                                            [查看全部]│ │
│  └─────────────────────────────────────────────────────┘ │
│  [ ▶ 00:00 / 14:20   ●───────────────       🔊 ]        │ ← Audio Player Bar
└───────────────────────────────────────────────────────────┘
```

---

## Browse-Mode Gestures

| Gesture | Action | Notes |
|---------|--------|-------|
| **Swipe RIGHT →** | Instant Transcribe | Only if not transcribed. Shimmer: "转写 >>>" |
| **Swipe LEFT ←** | Reveal tray: `[Play]` `[Delete]` `[Rename]` | Quick actions |
| **Tap Body** | Expand Card | Only if transcribed |
| **Tap [问AI]** | Open Coach with transcript as context | Located in expanded card |

Browse Mode should stay aligned with the existing gallery UX contract. Gesture hints are valid only in this mode.

---

## Select-Mode Interaction Contract

Select Mode is intentionally simpler than Browse Mode.

| Interaction | Result | Notes |
|------------|--------|-------|
| **Tap Card** | Select audio for current chat | Whole card is the tap target |
| **Tap Current Audio Card** | No-op | Disabled style; marked as current discussion |
| **Swipe RIGHT →** | Disabled | No transcribe swipe affordance in this mode |
| **Swipe LEFT ←** | Disabled | No quick-action tray in this mode |
| **Tap Body to Expand** | Disabled | Card stays collapsed; this is not the artifact-reading surface |
| **Tap [问AI]** | Hidden | `问AI` belongs to Browse Mode expanded cards only |

Select Mode cards must feel self-explanatory through state and copy rather than through button chrome.

---

## Transcription States

| State | User Sees | Microcopy |
|-------|-----------|-----------|
| `not_transcribed` | Shimmer-Placeholder | "右滑转写 >>>" (Shimmering) |
| `transcribing` | Progress bar | "转写中... {%}" |
| `transcribed` | Preview text (120 chars) | — |
| `error` | Error badge | "转写失败，请重试" |

These transcription-state microcopies apply to Browse Mode. Select Mode uses different, picker-oriented language.

---

## Select-Mode Card States

| State | User Sees | Microcopy / Behavior |
|-------|-----------|----------------------|
| `current` | Disabled card with visible preview | Inline current marker only, such as `当前讨论中 · ...`; no extra status pill |
| `transcribed` | Tappable card with truncated transcript preview | Transcript-first preview should be truncated to 1-2 lines with ellipsis and be self-explanatory without `已转写` chrome |
| `pending` | Tappable card with compact continuation copy | Use row-body copy such as `选择后在当前聊天中继续处理`; no separate `待处理` pill |
| `transcribing` | Tappable card with progress or in-flight label | Use row-body copy plus progress bar, such as `转写中，选择后将在当前聊天继续处理`; no separate `转写中` pill |
| `error` | Retry-capable or explicitly blocked card | `转写失败`; only promise chat-side retry if that route is truly supported |

For already-transcribed cards in Select Mode, the preview should help the user recognize the audio content quickly without opening the full artifact surface. Compact cards should not differentiate badge/phone origin through extra icon or source-label chrome in this mode.

---

## Sync Flow (Auto on Open, Browse Mode Only)

```
Browse-Mode Drawer Opens
    │
    ▼
[↻ 同步中...] ──auto───▶ Check Badge for new recordings
    │
    ├── New files found → Add cards to list
    │
    └── Complete → [✓] (icon changes to checkmark, fades)
```

---

## Component Registry

| Component | States |
|-----------|--------|
| **Audio Drawer** | `browse`, `select` |
| **Pill Handle** | `idle`, `dragging` |
| **Sync Indicator** | `idle`, `syncing`, `done`, `error` |
| **Audio Card** | `collapsed`, `expanded`, `playing`, `transcribing`, `current_discussion`, `selectable` |
| **Transcript Box** | `folded`, `unfolded`, `streaming` |
| **Upload Button** | `idle`, `picking`, `uploading` |

---

## R. Refined Visual & Motion Contract (2026-04-21)

> **Authority**: This section governs the **visual layer and motion** of the Audio Drawer. The behavioural contract above (Browse/Select semantics, gestures, sync flow, mode-specific microcopy) is unchanged. Where this section conflicts with the older ASCII chrome diagrams or `Spring Animation: damping 25, stiffness 200` row, this section wins.
> **Reference prototype**: `docs/inbox/audio-drawer-vibes.html` — element tree, tokens, and scenarios are mirrored verbatim from that file.
> **Vibe**: refined glass + aurora — keep the frosted sheet and aurora wash, strip one layer of visual noise per surface, replace shimmer-text hints with a single aurora chip, unify all springs to critically-damped (no overshoot), and move state out of pill chrome into typography weight + aurora-dot color.

### R.1 Layer & surface treatment

| Surface | Treatment |
|---------|-----------|
| Sheet | `rgba(22,24,32,0.62)` + `backdrop-filter: blur(28px) saturate(140%)` · top corners radius 22 · 1px hairline border `white/8%` on top edge |
| Aurora wash | Top 140px of sheet · radial-gradient layer (`#4a7cff` 28% top-left, `#8a5cff` 22% top-right) masked by linear fade to transparent at 100% · pointer-events none |
| Scrim | `rgba(10,12,18,0.38)` · fades with sheet via critically-damped opacity tween |
| Card (collapsed) | `white/4%` background · 1px hairline `white/6%` · radius 14 |
| Card (hover/focus) | `white/6%` |
| Card (expanded) | `white/7%` + border `white/10%` |
| Card (current in Select Mode) | aurora edge-bar (2px wide, top-to-bottom, `linear-gradient(#4a7cff → #8a5cff)`) on left edge — replaces any `当前讨论中` pill chrome |
| Hairline | `white/6%` everywhere (hub divider, accordion separators) |

Z-order remains Drawer (Z=4.0) per `ui_element_registry.md` §0. Handle remains Touchable (Z=5.0).

### R.2 Element tree (canonical)

```
sheet
├ handle             (36×4, ink/22%)
├ sheet-header
│  ├ title-block
│  │  ├ sheet-title  (15/600, -0.005em)            "Recordings" or "选择要讨论的录音"
│  │  └ sheet-sub    (10, uppercase, ink/45%)      "browse · 12 items" / "select · tap to attach" / "browse · syncing"
│  └ sync-pill       (glass chip · aurora dot encodes state · text label)
└ list (gutter 14, gap 8)
   ├ card[PENDING]
   │  ├ row(star, filename, meta)
   │  └ pending-hint                                aurora chip "→ transcribe" + static hint copy
   ├ card[TRANSCRIBING]
   │  ├ row(star, filename, meta)
   │  └ progress(track 2px · aurora gradient fill · numeric % label)
   ├ card[TRANSCRIBED, COLLAPSED]
   │  ├ row(★ active when starred, filename, meta)
   │  └ summary (max 2 lines)
   ├ card[TRANSCRIBED, EXPANDED]                    Browse Mode only
   │  ├ row + summary
   │  └ hub
   │       ├ player    (aurora play-btn · tabular numerals · waveform mask)
   │       ├ ask-ai    (aurora glass button, "✧ Ask AI")
   │       └ accordion (Transcript / Summary / Chapters / AI Insights)
   └ card[ERROR]
       ├ row(star, filename, meta)
       └ pending-hint   (warn-gradient chip "retry" + "transcription failed")
```

### R.3 Token table

| Token | Value |
|-------|-------|
| `surface.sheet` | `rgba(22,24,32,0.62)` + blur 28 sat 140 |
| `surface.card` / `.hover` / `.expanded` | `white/4%` · `white/6%` · `white/7%` |
| `hairline` | `white/6%` |
| `aurora.primary` | `#4a7cff → #8a5cff` (chip, ask-ai, edge-bar) |
| `aurora.active` | `#4a7cff → #3ad6c8` (progress fill, playing indicator) |
| `aurora.warn` / `aurora.error` | `#ffb86b` / `#ff6b7a` |
| `ink.100 / 70 / 45 / 25` | primary / body / meta / disabled |
| `radius.sheet / .card / .btn / .pill` | 22 / 14 / 10 / 999 |
| `type.title / .body / .meta / .micro` | 15/600 · 13.5/500 · 11 tabular · 10 uppercase |
| `space.list-gutter / .card-pad / .card-gap` | 14 · 12×14 · 8 |
| `motion.sheet / .card / .micro` | 320 / 240 / 180 ms · cubic-bezier(0.2,0,0,1) |
| `motion.spring` (Compose) | `dampingRatio = 0.9`, `stiffness = 500` |

### R.4 State → visual mapping

| Surface | State | Treatment (replaces older chrome) |
|---------|-------|-----------------------------------|
| Sync pill | `idle` | dot = `aurora.active` (mint), label = "Synced" |
| Sync pill | `syncing` | dot = `aurora.primary` (cool blue), pulses 1.4s ease, label = "Syncing", `sheet-sub` shows "browse · syncing" |
| Sync pill | `done` | identical to `idle`; transition fades over 180ms |
| Sync pill | `error` | dot = `aurora.error`, label = "Retry" (tap = re-trigger sync), `sheet-sub` = "browse · sync failed" |
| Card | `pending` (Browse) | row + `pending-hint` (aurora chip "→ transcribe" + "swipe right to start") |
| Card | `pending` (Select) | row + `pending-hint` ("pending · tap to select"), no chip |
| Card | `transcribing` | row + `progress` (aurora gradient fill, % label). No "正在转写..." text. |
| Card | `transcribed, collapsed` | row + `summary` (2 lines max). No `已转写` pill. |
| Card | `transcribed, expanded` | as above + `hub` (player → ask-ai → accordion). Browse Mode only. |
| Card | `current_discussion` (Select) | aurora edge-bar (left, 2px) + `summary` ("当前讨论中 · …" inline). No pill. |
| Card | `error` | `pending-hint` with warn-gradient chip "retry" + "transcription failed". Replaces "转写失败，请重试" badge. |

### R.5 Motion contract (critically damped, quick)

All motion uses one curve and three durations. **No `MediumBouncy`, `LowBouncy`, or visible overshoot anywhere.** This replaces the `damping: 25, stiffness: 200` row in §Interactions and the bouncy springs at `AudioDrawer.kt:93–103` and `AudioCard.kt:158–162`.

| Surface | Compose spring | CSS equivalent | Notes |
|---------|----------------|----------------|-------|
| Sheet open | `spring(dampingRatio = 0.9f, stiffness = 500f)` | `cubic-bezier(0.2, 0, 0, 1) 320ms` | Slide up from bottom edge |
| Sheet close | `spring(dampingRatio = 0.9f, stiffness = 500f)` | `cubic-bezier(0.2, 0, 0, 1) 320ms` | Same curve in reverse |
| Card expand / collapse | `spring(dampingRatio = 0.9f, stiffness = 500f)` | `cubic-bezier(0.2, 0, 0, 1) 240ms` | Hub fades in with 4px Y-translation |
| Accordion row | `tween(180ms, easing = FastOutSlowInEasing)` | `cubic-bezier(0.2, 0, 0, 1) 180ms` | Chevron rotates 90° |
| Sync dot pulse (syncing only) | `infiniteRepeatable(tween(1400ms, ease))` opacity 0.5 ↔ 1.0 | n/a | The only infinite transition that survives the refresh |
| Disallowed-tap feedback | `tween(120ms)` opacity 1.0 → 0.6 → 1.0 | n/a | **Replaces** the shake keyframes at `AudioCard.kt:171–179` |

**Removed motion primitives** (do not reintroduce):
- `DampingRatioMediumBouncy`, `DampingRatioLowBouncy` — overshoot reads as toy.
- Shimmer infinite transitions on PENDING hint and buffer placeholders — replaced by aurora chip + static hint.
- Shake keyframes on reject — replaced by a single 120ms opacity dip.

### R.6 Interaction affordances under the refined chrome

Behaviour from §Interactions, §Browse-Mode Gestures, §Select-Mode Interaction Contract is unchanged. Only the **visual signal** changes:

| Gesture | Old signal | Refined signal |
|---------|-----------|----------------|
| Swipe right (PENDING, Browse) | shimmer text "右滑开始转写 >>>" | aurora chip "→ transcribe" + static hint "swipe right to start" |
| Swipe left (Browse) | reveal tray | unchanged tray, hairline-bordered glass surface, aurora-error tint on Delete |
| Tap body (TRANSCRIBED) | expand | unchanged; hub fades in via card spring |
| Tap body (non-TRANSCRIBED) | shake | 120ms opacity dip |
| Long-press | rename dialog | unchanged dialog; surface uses sheet-glass tokens |
| Tap card (Select Mode) | ripple | aurora edge-bar slides in over 180ms on the selected card; previous selection's edge-bar fades out |
| Tap [问AI] / Ask AI | navigate | unchanged; button is the aurora glass `ask-ai` element with "✧ Ask AI" glyph prefix |

### R.7 Browse vs Select preserved

The dual-mode contract from §Mode Variants and §Select-Mode Interaction Contract is preserved verbatim. Only the **chrome** changes per the tables above. In particular:

- Select Mode still suppresses swipe, expansion, and `问AI`.
- Select Mode header copy still uses "选择要讨论的录音" with the `sheet-sub` micro-label "select · tap to attach".
- Current discussion is now signaled by the aurora edge-bar + inline `当前讨论中 · …` copy in `summary`, **not** a status pill.
- Compact transcript preview rules from §Select-Mode Card States are unchanged.

### R.8 Implementation pointers

| File | Change |
|------|--------|
| `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt` :93–103 | Replace `DampingRatioMediumBouncy` / `DampingRatioNoBouncy` with `spring(dampingRatio = 0.9f, stiffness = 500f)` for both open and close |
| `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt` :158–162 | Replace `DampingRatioLowBouncy / StiffnessLow` with `spring(dampingRatio = 0.9f, stiffness = 500f)` for card expand/collapse |
| `app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioCard.kt` :171–179 | Replace shake `keyframes` with `animateFloatAsState` for opacity dip 1.0 → 0.6 → 1.0 over 120ms |
| `AudioCard.kt` (PENDING shimmer) | Remove infinite shimmer transition; render `pending-hint` composable: aurora chip + static text |
| `AudioCard.kt` (status pills) | Remove `已转写` / `转写中` / `待处理` pill composables; rely on body treatment from §R.4 |
| `AudioDrawer.kt` (sheet-header) | Add `sheet-sub` text below title (10sp, uppercase, ink/45%); reflect sync state in sub copy |
| Glass surface | Confirm `Modifier.background(...).blur(...)` or equivalent renders sheet at `surface.sheet` token. If current implementation uses a non-blurred translucent fill, switch to `androidx.compose.ui.graphics.BlurEffect` or platform `RenderEffect.createBlurEffect` |

Implementation lands through the declared Android review path from `develop` per `docs/specs/ship-time-checks.md`. This contract itself ships on the `docs` lane.

### R.9 Verification

1. Open `docs/inbox/audio-drawer-vibes.html` in any browser. Each scenario tab (Browse / Select / Syncing / Error) should match the §R.4 mapping. The "Replay motion" button should produce a single, smooth 320ms slide-up with no overshoot.
2. After Kotlin implementation: drag drawer up → confirm critically-damped feel (no bounce). Tap a TRANSCRIBED card → hub fades in over ~240ms with no overshoot. Swipe-right on a PENDING card in Browse Mode → aurora chip activates and transcription begins.
3. `./gradlew :app:assembleDebug` then `./gradlew :app:installDebug`; `./gradlew testDebugUnitTest` and `./gradlew lint` clean.
4. Cross-doc: `docs/specs/ui_element_registry.md` §2 (Audio Card spring rows) and `docs/specs/prism-ui-ux-contract.md` §1.8 reference the refined contract via this document; no behavioural rules duplicated.
