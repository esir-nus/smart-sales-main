# Home UI & Title Pipeline Sanity Check Report

**Date:** 2025-12-08  
**Scope:** Read-only inspection of Home chat UI and title pipeline against UX sources of truth  
**Status:** Diagnostics only, no code changes

---

## UX Rules (from docs)

From `assistant-ux-contract.md`, `style-guide.md`, `Orchestrator-MetadataHub-Mvp-V3.md`:

1. **Top bar layout:** Left hamburger (history), center dynamic session title (bound to `currentSession.title`), right side: HUD dot toggle, "+" new-chat button (`home_new_chat_button`), profile icon — in that order.

2. **Home body:** Chat-first layout. No device/audio cards (`EntryCards`, "设备管理", "音频库") in the chat body. Empty state shows hero + single quick-skill row. Non-empty state: quick-skill row near input, not at top of scroll.

3. **HUD:** Top bar control is a dot icon (`debug_toggle_metadata`), not a text button. When enabled, bottom overlay panel (`DebugSessionMetadataHud`) with capped height, scrollable content, copy-all and close actions.

4. **Session title pipeline:** One-time auto rename from metadata/resolver when title is still placeholder (e.g., "新的聊天" or "通话分析 – ..."). Once user renames, never overwrite. Top bar title and history list title must always match.

5. **History drawer:** Uses `SessionListItemUi` / `sessionList` from VM as single source of truth. Sorted by `updatedAtMillis` descending. Title displayed matches the same `summary.title` that feeds current-session header.

6. **Keyboard/IME:** Top bar stays visible when keyboard opens. Content resizes (adjustResize), no panning. Device/audio overlay gestures disabled while typing.

---

## Findings by Area

### 1. Home top bar

**MISMATCH – `HomeScreen.kt:731-734`:** Title text is hard-coded `"AI 助手"` instead of bound to `state.currentSession.title`.

```kotlin
Text(
    text = "AI 助手",  // Should be: state.currentSession.title
    style = MaterialTheme.typography.titleLarge
)
```

**OK:** Left icon is a hamburger (`Icons.Filled.History`) that toggles the history drawer. Test tag `HISTORY_TOGGLE` is present (line 749).

**MISMATCH – `HomeScreen.kt:747-766`:** Right side order is incorrect. Current order: history icon, HUD toggle (if enabled), profile icon. Missing: "+" new-chat button (`home_new_chat_button`). Expected order: HUD dot toggle, "+" new-chat button, profile icon.

**MISMATCH – `HomeScreen.kt:754-759`:** HUD toggle is a `TextButton` with text "开启调试 HUD" / "关闭调试 HUD", not a dot icon. Test tag `debug_toggle_metadata` is present.

---

### 2. Home body layout

**MISMATCH – `HomeScreen.kt:396-402`:** `EntryCards` (device manager / audio sync cards) are rendered in the Home chat body at lines 396-402, inside the main content `Column`. This violates the chat-first principle.

```kotlin
Column(modifier = Modifier.fillMaxSize()) {
    EntryCards(  // Should NOT be here
        deviceSnapshot = state.deviceSnapshot,
        audioSummary = state.audioSummary,
        ...
    )
    Spacer(modifier = Modifier.height(8.dp))
    Box(...) { /* chat list / empty state */ }
}
```

**OK:** Empty state shows hero section (logo, greeting, assistant role, bullets, "让我们开始吧") plus a single quick-skill row under the hero (lines 420-425, 609-616).

**OK:** Non-empty state: quick-skill row is present only near the input (line 1147: `if (showQuickSkills)` inside `HomeInputArea`), and does NOT appear at the very top of the scroll content.

**OK:** There is no pull-to-refresh spinner or global sync icon wired into Home. The `PullRefreshIndicator` at line 494 is for device/audio refresh only, not chat content.

---

### 3. HUD

**MISMATCH – `HomeScreen.kt:754-759`:** Top bar HUD control is a `TextButton` with text "开启调试 HUD" / "关闭调试 HUD", not a dot icon. Test tag `debug_toggle_metadata` is present.

**MISMATCH – `HomeScreen.kt:839-894`:** `DebugSessionMetadataHud` panel exists but:
- Missing capped height (no `heightIn` or `maxHeight` constraint)
- Missing scrollable content wrapper (no `verticalScroll` or `LazyColumn`)
- Missing "copy all" action button
- Missing close action button (which should toggle the same HUD flag)

Current implementation is a simple `Column` with fixed text lines, no actions.

**OK:** HUD panel has a stable test tag `"debug_metadata_hud"` (line 851), though the test tag constant `DEBUG_HUD_PANEL` is defined but not used (line 686).

---

### 4. Session title pipeline

**OK:** There is exactly one place in the VM responsible for applying automatic titles: `maybeGenerateSessionTitle` (line 2510) and `updateTitleFromMetadata` (line 2543).

**OK:** `maybeGenerateSessionTitle` only runs when:
- `request.isFirstAssistantReply == true` (line 2511)
- `request.quickSkillId == null` (line 2512)
- Title is still placeholder: `existingSummary.title == DEFAULT_SESSION_TITLE` (line 2514)

**OK:** It can take a suggestion from MetaHub (`SessionMetadata`) OR from `sessionTitleResolver` (lines 2515-2529), and applies it once.

**OK:** It updates both:
- `currentSession.title` in UI state (lines 2532-2538)
- The persisted `AiSessionRepository` summary via `sessionRepository.updateTitle` (line 2530)

**OK:** `updateTitleFromMetadata` follows the same contract (lines 2543-2559), checking for placeholder before updating.

**OK:** Calls to `sessionRepository.updateTitle` go through these helpers (lines 2530, 2549). No rogue hard-coded "AI 助手" titles found in title update paths.

---

### 5. History drawer

**OK:** History drawer uses `SessionListItemUi` / `sessionList` from VM as single source of truth (line 214: `historySessions = state.sessionList.take(10)`, passed to `HistoryPanel` at line 538).

**OK:** `applySessionList()` (line 940) sorts by `updatedAtMillis` descending before mapping to `SessionListItemUi`. The sorting happens implicitly via `sessionRepository.summaries.collectLatest` (line 931), which should provide pre-sorted summaries. The mapping preserves order (lines 941-950).

**OK:** The title displayed for each history item is exactly `session.title` (line 942, 1483), which comes from `summary.title` (line 942), the same source that feeds `currentSession.title` (via `applySessionList()` → `currentSession` update in VM).

**OK:** There is no separate legacy history shell that could still be showing a different title or layout. `HistoryPanel` is the only history UI component (lines 1407-1510).

---

### 6. Keyboard / gesture guardrails (sanity only)

**OK:** Home content (chat area) applies IME-aware padding. The `Scaffold` with `bottomBar` (line 366) handles keyboard insets via `navigationBarsPadding()` in `HomeInputArea` (line 1143).

**OK:** Input focus state from `HomeInputArea` is surfaced via `onInputFocusChanged` callback (line 380-383), which is passed up to `HomeScreenRoute` (line 221) and can be used to disable gestures in the shell.

**OK:** `dragDismissModifier` (lines 331-346) respects `isInputFocused` and only dismisses keyboard when typing (`typingAtStart && dragAmount > 0f`).

---

## Likely Causes of the Observed Behavior

1. **Top bar still showing "AI 助手":**
   - `HomeTopBar` composable (line 717) hard-codes the title string instead of accepting `state.currentSession.title` as a parameter.
   - The VM correctly maintains `currentSession.title` and updates it via `maybeGenerateSessionTitle` / `updateTitleFromMetadata`, but the UI does not consume it.

2. **HUD still using text button:**
   - `HomeTopBar` uses `TextButton` with conditional text (lines 754-759) instead of an `IconButton` with a dot icon.
   - The UX contract specifies a "simple dot or subtle icon" but the implementation uses text.

3. **New chat "+" missing:**
   - `HomeTopBar` does not include a "+" new-chat button in the right-side `Row` (lines 735-767).
   - The `SessionHeader` composable (line 1291) has a "新建对话" button, but it's not used in the top bar.
   - There is an `onNewChatClicked` callback available (line 240) and `NEW_CHAT_BUTTON` test tag constant (line 671), but no corresponding button in the top bar.

4. **EntryCards in Home body:**
   - `EntryCards` are explicitly rendered in the main content `Column` (lines 396-402), before the chat list/empty state.
   - The UX contract states "Device Manager and Audio Library live in their own pages or drawers, not as big cards inside the chat stream."

5. **HUD panel missing copy/close actions:**
   - `DebugSessionMetadataHud` (line 839) is a simple `Column` with text, no action buttons.
   - The UX contract requires "Always includes Copy and Close actions" but these are not implemented.

---

## Summary

**Total mismatches:** 5  
**OK items:** 11

**Critical issues:**
1. Top bar title hard-coded to "AI 助手" instead of dynamic session title
2. HUD toggle using text button instead of dot icon
3. Missing "+" new-chat button in top bar
4. EntryCards (device/audio) still rendered in Home body
5. HUD panel missing copy/close actions and height/scroll constraints

**Working correctly:**
- Session title pipeline (one-time auto rename, placeholder check, MetaHub integration)
- History drawer (single source of truth, correct sorting, title consistency)
- Quick skills placement (empty state hero, non-empty near input)
- Keyboard/IME handling (focus state, gesture guards)
